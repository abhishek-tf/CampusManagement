CREATE DATABASE IF NOT EXISTS campus_payment_db;
USE campus_payment_db;

-- Students Table
CREATE TABLE IF NOT EXISTS students (
    student_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15) NOT NULL,
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
);

-- Wallets Table
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT UNIQUE NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0,
    daily_transfer_limit DECIMAL(15,2) DEFAULT 100000,
    daily_transfer_spent DECIMAL(15,2) DEFAULT 0,
    transfer_reset_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    INDEX idx_student (student_id)
);

-- Transaction History Table
CREATE TABLE IF NOT EXISTS transaction_history (
    transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    wallet_id BIGINT,
    txn_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id),
    INDEX idx_student (student_id),
    INDEX idx_created (created_at)
);

-- Transfer Transactions Table
CREATE TABLE IF NOT EXISTS transfer_transactions (
    transfer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_student_id BIGINT NOT NULL,
    to_student_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (from_student_id) REFERENCES students(student_id),
    FOREIGN KEY (to_student_id) REFERENCES students(student_id),
    INDEX idx_from (from_student_id),
    INDEX idx_to (to_student_id)
);

-- Campus Payments Table
CREATE TABLE IF NOT EXISTS campus_payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    invoice_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    INDEX idx_student (student_id),
    INDEX idx_category (category)
);

-- Expense Groups Table
CREATE TABLE IF NOT EXISTS expense_groups (
    group_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_by_student_id BIGINT NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by_student_id) REFERENCES students(student_id)
);

-- Group Members Table
CREATE TABLE IF NOT EXISTS group_members (
    member_expense_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    is_paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (group_id) REFERENCES expense_groups(group_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    UNIQUE KEY unique_group_member (group_id, student_id)
);

-- Expense Splits Table
CREATE TABLE IF NOT EXISTS expense_splits (
    split_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    share_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES expense_groups(group_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    INDEX idx_group (group_id),
    INDEX idx_student (student_id)
);

-- Fraud Flags Table
CREATE TABLE IF NOT EXISTS fraud_flags (
    flag_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    suspicious_transaction_count INT DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    review_notes VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    flagged_at TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    UNIQUE KEY unique_student_flag (student_id),
    INDEX idx_status (status)
);

CREATE INDEX idx_transactions_date ON transaction_history(created_at);
CREATE INDEX idx_payments_date ON campus_payments(created_at);
CREATE INDEX idx_transfers_date ON transfer_transactions(created_at);
