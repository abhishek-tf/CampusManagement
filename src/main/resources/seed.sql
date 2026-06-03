-- Sample Data

INSERT INTO students (name, email, phone, department) VALUES
('Rajesh Kumar', 'rajesh@campus.edu', '9876543210', 'Computer Science'),
('Priya Singh', 'priya@campus.edu', '9876543211', 'Electronics'),
('Amit Patel', 'amit@campus.edu', '9876543212', 'Mechanical'),
('Neha Sharma', 'neha@campus.edu', '9876543213', 'Civil');

INSERT INTO wallets (student_id, balance, daily_transfer_limit) VALUES
(1, 50000.00, 100000),
(2, 75000.00, 100000),
(3, 30000.00, 100000),
(4, 100000.00, 100000);
