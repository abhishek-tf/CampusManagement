-- ===========================================================================
-- CampusPay : seed.sql
-- Dummy data for every table. Insert order is FK-safe (parents first);
-- explicit PK ids keep subtype/child references deterministic.
-- Safe to re-run: child-first DELETE block clears prior seed data.
-- ===========================================================================
USE campuspay;

-- ---------------------------------------------------------------------------
-- Clean prior seed data (child -> parent) so this script is idempotent.
-- ---------------------------------------------------------------------------
DELETE FROM fraud_flag;
DELETE FROM expense_split;
DELETE FROM group_expense;
DELETE FROM group_member;
DELETE FROM expense_group;
DELETE FROM transfer_transaction;
DELETE FROM campus_payment;
DELETE FROM transaction;
DELETE FROM wallet;
DELETE FROM student;

-- ===========================================================================
-- student
-- ===========================================================================
INSERT INTO student (student_id, name, email, department, phone, created_at) VALUES
  ('S001','Aarav Sharma',  'aarav.sharma@campus.edu',  'CSE',   '9000000001','2026-01-10 09:15:00'),
  ('S002','Diya Patel',    'diya.patel@campus.edu',    'ECE',   '9000000002','2026-01-10 09:20:00'),
  ('S003','Rohan Mehta',   'rohan.mehta@campus.edu',   'MECH',  '9000000003','2026-01-11 10:05:00'),
  ('S004','Ananya Iyer',   'ananya.iyer@campus.edu',   'CSE',   '9000000004','2026-01-11 10:30:00'),
  ('S005','Karthik Reddy', 'karthik.reddy@campus.edu', 'IT',    '9000000005','2026-01-12 11:00:00'),
  ('S006','Sneha Nair',    'sneha.nair@campus.edu',    'EEE',   '9000000006','2026-01-12 11:45:00'),
  ('S007','Vikram Singh',  'vikram.singh@campus.edu',  'CIVIL', '9000000007','2026-01-13 08:50:00'),
  ('S008','Priya Gupta',   'priya.gupta@campus.edu',   'CSE',   '9000000008','2026-01-13 09:10:00'),
  ('S009','Arjun Das',     'arjun.das@campus.edu',     'ECE',   '9000000009','2026-01-14 14:20:00'),
  ('S010','Meera Joshi',   'meera.joshi@campus.edu',   'IT',    '9000000010','2026-01-14 14:55:00');

-- ===========================================================================
-- wallet  (one per student; wallet_id = student index for readability)
-- ===========================================================================
INSERT INTO wallet (wallet_id, student_id, balance, daily_transfer_used, transfer_reset_date,
                    max_balance_cap, daily_transfer_limit, updated_at) VALUES
  ( 1,'S001', 5850.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 2,'S002', 2200.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 3,'S003', 8300.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 4,'S004', 2700.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 5,'S005', 6000.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 6,'S006', 5300.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 7,'S007', 2000.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 8,'S008', 5000.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  ( 9,'S009', 3500.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00'),
  (10,'S010', 4500.00, 0.00, NULL, 100000.00, 50000.00, '2026-05-21 10:00:00');

-- ===========================================================================
-- transaction  (audit backbone; explicit txn_id referenced by subtypes)
--   1-17  : deposits / withdraws / payments / one FAILED withdraw
--   18-19 : ad-hoc transfers
--   20-22 : expense settlements (debtor -> payer)
--   23-28 : burst of 6 transfers by S005 within 60s (backs fraud_flag)
-- ===========================================================================
INSERT INTO transaction (txn_id, wallet_id, txn_type, amount, status, failure_reason, created_at) VALUES
  ( 1, 1,'DEPOSIT',  5000.00,'SUCCESS', NULL,                 '2026-05-01 09:00:00'),
  ( 2, 1,'PAYMENT',   150.00,'SUCCESS', NULL,                 '2026-05-02 12:30:00'),
  ( 3, 2,'DEPOSIT',  3000.00,'SUCCESS', NULL,                 '2026-05-01 09:10:00'),
  ( 4, 2,'WITHDRAW',  500.00,'SUCCESS', NULL,                 '2026-05-03 18:00:00'),
  ( 5, 3,'DEPOSIT', 10000.00,'SUCCESS', NULL,                 '2026-05-01 09:20:00'),
  ( 6, 3,'PAYMENT',  2000.00,'SUCCESS', NULL,                 '2026-05-04 10:15:00'),
  ( 7, 4,'DEPOSIT',  4000.00,'SUCCESS', NULL,                 '2026-05-01 09:30:00'),
  ( 8, 4,'PAYMENT',   300.00,'SUCCESS', NULL,                 '2026-05-05 16:45:00'),
  ( 9, 5,'DEPOSIT',  8000.00,'SUCCESS', NULL,                 '2026-05-01 09:40:00'),
  (10, 2,'PAYMENT',   500.00,'SUCCESS', NULL,                 '2026-05-06 11:00:00'),
  (11, 6,'DEPOSIT',  6000.00,'SUCCESS', NULL,                 '2026-05-01 09:50:00'),
  (12, 6,'PAYMENT',  1000.00,'SUCCESS', NULL,                 '2026-05-07 13:20:00'),
  (13, 7,'DEPOSIT',  2000.00,'SUCCESS', NULL,                 '2026-05-01 10:00:00'),
  (14, 8,'DEPOSIT',  5000.00,'SUCCESS', NULL,                 '2026-05-01 10:10:00'),
  (15, 9,'DEPOSIT',  3500.00,'SUCCESS', NULL,                 '2026-05-01 10:20:00'),
  (16,10,'DEPOSIT',  4500.00,'SUCCESS', NULL,                 '2026-05-01 10:30:00'),
  (17, 7,'WITHDRAW', 5000.00,'FAILED',  'Insufficient balance','2026-05-08 19:05:00'),
  (18, 1,'TRANSFER', 1000.00,'SUCCESS', NULL,                 '2026-05-10 14:00:00'),
  (19, 5,'TRANSFER', 1500.00,'SUCCESS', NULL,                 '2026-05-11 15:30:00'),
  (20, 2,'TRANSFER', 1000.00,'SUCCESS', NULL,                 '2026-05-12 17:00:00'),
  (21, 4,'TRANSFER', 1000.00,'SUCCESS', NULL,                 '2026-05-12 17:05:00'),
  (22, 6,'TRANSFER',  300.00,'SUCCESS', NULL,                 '2026-05-13 18:10:00'),
  (23, 5,'TRANSFER',  200.00,'SUCCESS', NULL,                 '2026-05-20 09:00:05'),
  (24, 5,'TRANSFER',  200.00,'SUCCESS', NULL,                 '2026-05-20 09:00:12'),
  (25, 5,'TRANSFER',  200.00,'SUCCESS', NULL,                 '2026-05-20 09:00:20'),
  (26, 5,'TRANSFER',  200.00,'SUCCESS', NULL,                 '2026-05-20 09:00:31'),
  (27, 5,'TRANSFER',  200.00,'SUCCESS', NULL,                 '2026-05-20 09:00:42'),
  (28, 5,'TRANSFER',  200.00,'SUCCESS', NULL,                 '2026-05-20 09:00:48');

-- ===========================================================================
-- campus_payment  (subtype for txn_type = PAYMENT)
-- ===========================================================================
INSERT INTO campus_payment (txn_id, student_id, category, amount, paid_at) VALUES
  ( 2,'S001','CANTEEN',       150.00,'2026-05-02 12:30:00'),
  ( 6,'S003','HOSTEL_FEE',   2000.00,'2026-05-04 10:15:00'),
  ( 8,'S004','LIBRARY_FINE',  300.00,'2026-05-05 16:45:00'),
  (10,'S002','WORKSHOP_FEE',  500.00,'2026-05-06 11:00:00'),
  (12,'S006','HACKATHON_FEE',1000.00,'2026-05-07 13:20:00');

-- ===========================================================================
-- transfer_transaction  (subtype for txn_type = TRANSFER; from <> to)
-- ===========================================================================
INSERT INTO transfer_transaction (txn_id, from_student_id, to_student_id) VALUES
  (18,'S001','S002'),  -- ad-hoc
  (19,'S005','S003'),  -- ad-hoc
  (20,'S002','S001'),  -- settles exp1 split (debtor S002 -> payer S001)
  (21,'S004','S001'),  -- settles exp1 split (debtor S004 -> payer S001)
  (22,'S006','S005'),  -- settles exp3 split (debtor S006 -> payer S005)
  (23,'S005','S002'),  -- burst
  (24,'S005','S006'),  -- burst
  (25,'S005','S007'),  -- burst
  (26,'S005','S002'),  -- burst
  (27,'S005','S006'),  -- burst
  (28,'S005','S007');  -- burst

-- ===========================================================================
-- expense_group
-- ===========================================================================
INSERT INTO expense_group (group_id, group_name, created_by, created_at) VALUES
  (1,'Goa Trip',     'S001','2026-04-01 20:00:00'),
  (2,'Flat 3B',      'S005','2026-04-05 21:00:00'),
  (3,'Project Team', 'S004','2026-04-10 18:30:00');

-- ===========================================================================
-- group_member
-- ===========================================================================
INSERT INTO group_member (group_id, student_id, joined_at) VALUES
  (1,'S001','2026-04-01 20:00:00'),
  (1,'S002','2026-04-01 20:05:00'),
  (1,'S003','2026-04-01 20:06:00'),
  (1,'S004','2026-04-01 20:07:00'),
  (2,'S005','2026-04-05 21:00:00'),
  (2,'S006','2026-04-05 21:02:00'),
  (2,'S007','2026-04-05 21:03:00'),
  (3,'S004','2026-04-10 18:30:00'),
  (3,'S008','2026-04-10 18:32:00'),
  (3,'S009','2026-04-10 18:33:00'),
  (3,'S010','2026-04-10 18:34:00');

-- ===========================================================================
-- group_expense
-- ===========================================================================
INSERT INTO group_expense (expense_id, group_id, paid_by, description, total_amount, split_type, created_at) VALUES
  (1,1,'S001','Hotel booking',  4000.00,'EQUAL',  '2026-04-02 10:00:00'),
  (2,1,'S002','Group dinner',   1200.00,'EQUAL',  '2026-04-03 21:30:00'),
  (3,2,'S005','Monthly groceries',900.00,'EXACT', '2026-04-06 19:00:00'),
  (4,3,'S004','Cloud credits',  1000.00,'PERCENT','2026-04-11 12:00:00');

-- ===========================================================================
-- expense_split  (one share per debtor; payer is not their own debtor)
--   SETTLED rows point settled_txn_id at the matching TRANSFER above.
-- ===========================================================================
INSERT INTO expense_split (expense_id, debtor_id, share_amount, share_percent, status, settled_txn_id, settled_at) VALUES
  -- exp1 EQUAL 4000 / 4 = 1000 each (payer S001)
  (1,'S002',1000.00, NULL,'SETTLED',  20,'2026-05-12 17:00:00'),
  (1,'S003',1000.00, NULL,'PENDING',  NULL, NULL),
  (1,'S004',1000.00, NULL,'SETTLED',  21,'2026-05-12 17:05:00'),
  -- exp2 EQUAL 1200 / 4 = 300 each (payer S002)
  (2,'S001', 300.00, NULL,'PENDING',  NULL, NULL),
  (2,'S003', 300.00, NULL,'PENDING',  NULL, NULL),
  (2,'S004', 300.00, NULL,'PENDING',  NULL, NULL),
  -- exp3 EXACT 900 (payer S005)
  (3,'S006', 300.00, NULL,'SETTLED',  22,'2026-05-13 18:10:00'),
  (3,'S007', 600.00, NULL,'PENDING',  NULL, NULL),
  -- exp4 PERCENT 1000 (payer S004)
  (4,'S008', 400.00,40.00,'PENDING',  NULL, NULL),
  (4,'S009', 300.00,30.00,'PENDING',  NULL, NULL),
  (4,'S010', 300.00,30.00,'PENDING',  NULL, NULL);

-- ===========================================================================
-- fraud_flag  (S005 burst: 6 transfers within a 60s window)
-- ===========================================================================
INSERT INTO fraud_flag (student_id, transfer_count, threshold, window_seconds, window_start, window_end, status, flagged_at) VALUES
  ('S005', 6, 5, 60, '2026-05-20 09:00:00', '2026-05-20 09:01:00', 'OPEN', '2026-05-20 09:01:05');
