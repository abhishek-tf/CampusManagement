CREATE DATABASE IF NOT EXISTS campuspay
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campuspay;

-- ---------------------------------------------------------------------------
-- Drop in FK-safe order (children first) for clean re-runs.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS fraud_flag;
DROP TABLE IF EXISTS expense_split;
DROP TABLE IF EXISTS group_expense;
DROP TABLE IF EXISTS group_member;
DROP TABLE IF EXISTS expense_group;
DROP TABLE IF EXISTS transfer_transaction;
DROP TABLE IF EXISTS campus_payment;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS wallet;
DROP TABLE IF EXISTS student;

-- ===========================================================================
-- Core identity & wallet
-- ===========================================================================

-- student : identity. UNIQUE email drives duplicate detection.
CREATE TABLE student (
  student_id  VARCHAR(20)  PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  email       VARCHAR(150) NOT NULL UNIQUE,
  department  VARCHAR(60)  NOT NULL,
  phone       VARCHAR(15)  NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- wallet : one per student (enforced by UNIQUE student_id).
--   daily_transfer_used + transfer_reset_date let the app reset the daily
--   counter once per calendar day without scanning the transaction table.
CREATE TABLE wallet (
  wallet_id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
  student_id           VARCHAR(20)   NOT NULL UNIQUE,
  balance              DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  daily_transfer_used  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  transfer_reset_date  DATE          NULL,
  max_balance_cap      DECIMAL(12,2) NOT NULL DEFAULT 100000.00,
  daily_transfer_limit DECIMAL(12,2) NOT NULL DEFAULT 50000.00,
  updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_wallet_student FOREIGN KEY (student_id)
    REFERENCES student(student_id) ON DELETE CASCADE,
  CONSTRAINT chk_wallet_balance_nonneg CHECK (balance             >= 0),
  CONSTRAINT chk_wallet_used_nonneg    CHECK (daily_transfer_used >= 0)
) ENGINE=InnoDB;

-- ===========================================================================
-- Transactions — supertype + subtypes
-- ===========================================================================

-- transaction : every money movement is one row here (the audit backbone).
--   txn_type : DEPOSIT | WITHDRAW | TRANSFER | PAYMENT
--   status   : SUCCESS | FAILED | PENDING   (failure_reason set when FAILED)
CREATE TABLE transaction (
  txn_id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
  wallet_id      BIGINT        NOT NULL,
  txn_type       VARCHAR(30)   NOT NULL,
  amount         DECIMAL(12,2) NOT NULL,
  status         VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
  failure_reason VARCHAR(255)  NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_txn_wallet  FOREIGN KEY (wallet_id) REFERENCES wallet(wallet_id),
  CONSTRAINT chk_txn_amount CHECK (amount   > 0),
  CONSTRAINT chk_txn_type   CHECK (txn_type IN ('DEPOSIT','WITHDRAW','TRANSFER','PAYMENT')),
  CONSTRAINT chk_txn_status CHECK (status   IN ('SUCCESS','FAILED','PENDING'))
) ENGINE=InnoDB;

CREATE INDEX idx_txn_wallet_time ON transaction (wallet_id, created_at);
CREATE INDEX idx_txn_type        ON transaction (txn_type);

-- campus_payment : subtype detail for txn_type = PAYMENT.
--   One-to-one with transaction (UNIQUE txn_id). CASCADE keeps it in sync.
CREATE TABLE campus_payment (
  payment_id BIGINT        AUTO_INCREMENT PRIMARY KEY,
  txn_id     BIGINT        NOT NULL UNIQUE,
  student_id VARCHAR(20)   NOT NULL,
  category   VARCHAR(30)   NOT NULL,
  amount     DECIMAL(12,2) NOT NULL,
  paid_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pay_txn      FOREIGN KEY (txn_id)     REFERENCES transaction(txn_id) ON DELETE CASCADE,
  CONSTRAINT fk_pay_student  FOREIGN KEY (student_id) REFERENCES student(student_id),
  CONSTRAINT chk_pay_amount  CHECK (amount > 0),
  CONSTRAINT chk_pay_category CHECK (category IN
    ('CANTEEN','LIBRARY_FINE','HACKATHON_FEE','WORKSHOP_FEE','HOSTEL_FEE'))
) ENGINE=InnoDB;

CREATE INDEX idx_pay_student ON campus_payment (student_id);

-- transfer_transaction : subtype detail for txn_type = TRANSFER.
--   Records both parties. One-to-one with transaction (UNIQUE txn_id).
CREATE TABLE transfer_transaction (
  transfer_id     BIGINT      AUTO_INCREMENT PRIMARY KEY,
  txn_id          BIGINT      NOT NULL UNIQUE,
  from_student_id VARCHAR(20) NOT NULL,
  to_student_id   VARCHAR(20) NOT NULL,
  CONSTRAINT fk_tt_txn      FOREIGN KEY (txn_id)          REFERENCES transaction(txn_id) ON DELETE CASCADE,
  CONSTRAINT fk_tt_from     FOREIGN KEY (from_student_id) REFERENCES student(student_id),
  CONSTRAINT fk_tt_to       FOREIGN KEY (to_student_id)   REFERENCES student(student_id),
  CONSTRAINT chk_tt_distinct CHECK (from_student_id <> to_student_id)
) ENGINE=InnoDB;

CREATE INDEX idx_tt_from ON transfer_transaction (from_student_id);

-- ===========================================================================
-- Expense sharing (Splitwise-style)
-- ===========================================================================

-- expense_group : a named group of students who share expenses.
CREATE TABLE expense_group (
  group_id   BIGINT       AUTO_INCREMENT PRIMARY KEY,
  group_name VARCHAR(100) NOT NULL,
  created_by VARCHAR(20)  NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_group_creator FOREIGN KEY (created_by) REFERENCES student(student_id)
) ENGINE=InnoDB;

-- group_member : pure membership — who belongs to which group.
--   Composite PK prevents duplicate membership.
--   CASCADE: removing a group removes all member rows automatically.
CREATE TABLE group_member (
  group_id   BIGINT      NOT NULL,
  student_id VARCHAR(20) NOT NULL,
  joined_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id, student_id),
  CONSTRAINT fk_gm_group   FOREIGN KEY (group_id)   REFERENCES expense_group(group_id) ON DELETE CASCADE,
  CONSTRAINT fk_gm_student FOREIGN KEY (student_id) REFERENCES student(student_id)
) ENGINE=InnoDB;

-- group_expense : one bill paid by one group member.
--   split_type drives how expense_split rows were computed.
CREATE TABLE group_expense (
  expense_id   BIGINT        AUTO_INCREMENT PRIMARY KEY,
  group_id     BIGINT        NOT NULL,
  paid_by      VARCHAR(20)   NOT NULL,
  description  VARCHAR(255)  NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  split_type   VARCHAR(10)   NOT NULL DEFAULT 'EQUAL',
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ge_group  FOREIGN KEY (group_id) REFERENCES expense_group(group_id) ON DELETE CASCADE,
  CONSTRAINT fk_ge_payer  FOREIGN KEY (paid_by)  REFERENCES student(student_id),
  CONSTRAINT chk_ge_total CHECK (total_amount > 0),
  CONSTRAINT chk_ge_split CHECK (split_type IN ('EQUAL','EXACT','PERCENT'))
) ENGINE=InnoDB;

CREATE INDEX idx_ge_group ON group_expense (group_id);

-- expense_split : each debtor's share of one bill.
--   UNIQUE (expense_id, debtor_id) — one share per person per bill.
--   When settled: status=SETTLED, settled_txn_id → the wallet TRANSFER, settled_at stamped.
--   share_percent populated only when split_type = PERCENT.
CREATE TABLE expense_split (
  split_id       BIGINT        AUTO_INCREMENT PRIMARY KEY,
  expense_id     BIGINT        NOT NULL,
  debtor_id      VARCHAR(20)   NOT NULL,
  share_amount   DECIMAL(12,2) NOT NULL,
  share_percent  DECIMAL(5,2)  NULL,
  status         VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
  settled_txn_id BIGINT        NULL,
  settled_at     DATETIME      NULL,
  CONSTRAINT fk_es_expense FOREIGN KEY (expense_id)     REFERENCES group_expense(expense_id) ON DELETE CASCADE,
  CONSTRAINT fk_es_debtor  FOREIGN KEY (debtor_id)      REFERENCES student(student_id),
  CONSTRAINT fk_es_txn     FOREIGN KEY (settled_txn_id) REFERENCES transaction(txn_id),
  CONSTRAINT uq_es_once    UNIQUE (expense_id, debtor_id),
  CONSTRAINT chk_es_share  CHECK (share_amount >= 0),
  CONSTRAINT chk_es_status CHECK (status IN ('PENDING','SETTLED'))
) ENGINE=InnoDB;

CREATE INDEX idx_es_debtor ON expense_split (debtor_id, status);

-- ===========================================================================
-- Fraud detection
-- ===========================================================================

-- fraud_flag : persisted result of burst-detection rules.
--   threshold + window_seconds make the rule self-documenting.
--   UNIQUE (student_id, window_start) prevents double-flagging the same window.
CREATE TABLE fraud_flag (
  flag_id        BIGINT      AUTO_INCREMENT PRIMARY KEY,
  student_id     VARCHAR(20) NOT NULL,
  transfer_count INT         NOT NULL,
  threshold      INT         NOT NULL,
  window_seconds INT         NOT NULL,
  window_start   DATETIME    NOT NULL,
  window_end     DATETIME    NOT NULL,
  status         VARCHAR(10) NOT NULL DEFAULT 'OPEN',
  flagged_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ff_student FOREIGN KEY (student_id) REFERENCES student(student_id),
  CONSTRAINT uq_ff_window  UNIQUE (student_id, window_start),
  CONSTRAINT chk_ff_status CHECK (status IN ('OPEN','REVIEWED','DISMISSED'))
) ENGINE=InnoDB;