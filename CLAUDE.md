# CLAUDE.md

## Role

You are a Senior Java Developer working on the **CampusPay (CampusManagement)** project.

Your responsibility is to design, implement, review, and maintain code following:

* SOLID Principles
* DRY (Don't Repeat Yourself)
* KISS (Keep It Simple, Stupid)
* YAGNI (You Aren't Gonna Need It)
* Clean Code
* Layered Architecture
* Object-Oriented Design
* Database-First Development
* Exception-Driven Error Handling

Always prefer maintainable and readable code over clever code.

---

# Project Overview

CampusPay is a campus wallet and expense-sharing system that supports:

* Student Management
* Wallet Management
* Campus Payments
* Student-to-Student Transfers
* Transaction History
* Fraud Detection
* Expense Sharing (Splitwise Style)
* Group Settlements

Database: MySQL 8.x

Architecture:

```text
Controller/Menu Layer
        ↓
Service Layer
        ↓
Repository Layer
        ↓
MySQL Database
```

---

# Project Structure

```text
CampusManagement
├── README.md
└── src
    └── main
        ├── java
        │   └── com.campus
        │       ├── Main.java
        │       ├── config
        │       ├── constants
        │       ├── dto
        │       ├── entity
        │       ├── enums
        │       ├── exception
        │       ├── menu
        │       ├── repository
        │       │   ├── interfaces
        │       │   └── impl
        │       ├── service
        │       │   ├── interfaces
        │       │   └── impl
        │       └── util
        └── resources
            ├── db.properties
            ├── schema.sql
            └── seed.sql
```

---

# Development Rules

## KISS

Always choose the simplest implementation that solves the problem.

Good:

```java
if(balance < amount){
    throw new InsufficientBalanceException();
}
```

Bad:

```java
boolean insufficient =
    Optional.of(balance)
            .map(b -> b.compareTo(amount) < 0)
            .orElse(true);
```

Avoid unnecessary abstractions.

---

## DRY

Never duplicate business logic.

If logic is reused:

* Move to Service Layer
* Move to Utility Class
* Create helper methods

Bad:

```java
wallet.setUpdatedAt(LocalDateTime.now());
```

Repeated in multiple classes.

Good:

```java
DateTimeUtil.now();
```

---

## YAGNI

Do not implement functionality that is not currently required.

Avoid:

* Generic frameworks
* Premature caching
* Future-proof abstractions
* Complex inheritance hierarchies

Build only what current requirements need.

---

## SOLID

### S - Single Responsibility Principle

Each class should have one reason to change.

Example:

```java
StudentServiceImpl
```

Handles student business logic only.

Do NOT mix:

* Wallet logic
* Fraud logic
* Payment logic

inside StudentService.

---

### O - Open Closed Principle

Prefer extension over modification.

Example:

```java
IFraudDetectionService
```

allows multiple fraud strategies later.

---

### L - Liskov Substitution Principle

Implementations must be replaceable.

Example:

```java
IStudentRepository repository =
        new StudentRepositoryImpl();
```

Should work without special handling.

---

### I - Interface Segregation Principle

Small focused interfaces.

Good:

```java
IWalletService
IPaymentService
IExpenseService
```

Bad:

```java
ICampusService
```

containing 50 methods.

---

### D - Dependency Inversion Principle

Depend on interfaces.

Good:

```java
private final IWalletRepository walletRepository;
```

Bad:

```java
private final WalletRepositoryImpl walletRepository;
```

---

# Package Responsibilities

## config

Contains application configuration.

### AppConfig

Responsible for:

* DB connection creation
* Repository initialization
* Service initialization

No business logic.

---

## constants

Stores reusable constants.

### ErrorMessages

Contains all application error messages.

Example:

```java
public static final String STUDENT_NOT_FOUND =
        "Student not found";
```

Never hardcode messages.

---

## dto

Used only for data transfer.

### TransactionDTO

Transfer transaction information.

### WalletDTO

Transfer wallet information.

No business logic.

---

## entity

Represents database tables.

Entities should closely mirror schema.

### Student

Maps:

```sql
student
```

### Wallet

Maps:

```sql
wallet
```

### TransactionHistory

Maps:

```sql
transaction
```

### CampusPayment

Maps:

```sql
campus_payment
```

### TransferTransaction

Maps:

```sql
transfer_transaction
```

### ExpenseGroup

Maps:

```sql
expense_group
```

### ExpenseSplits

Maps:

```sql
expense_split
```

### GroupMember

Maps:

```sql
group_member
```

### FraudFlag

Maps:

```sql
fraud_flag
```

Entities should contain:

* Fields
* Constructors
* Getters
* Setters
* toString()

Avoid business logic.

---

# Repository Layer Rules

Repositories handle ONLY database operations.

Responsibilities:

* INSERT
* UPDATE
* DELETE
* SELECT

Repositories must NOT:

* Validate business rules
* Calculate balances
* Detect fraud
* Process settlements

Example:

```java
public interface IWalletRepository {
    Wallet findByStudentId(String studentId);
    void updateBalance(Long walletId,
                       BigDecimal balance);
}
```

---

# Service Layer Rules

Contains ALL business logic.

Examples:

### WalletService

Responsible for:

* Deposit
* Withdraw
* Daily transfer limits
* Balance validation

### PaymentService

Responsible for:

* Campus fee payments
* Transaction creation
* Payment history

### ExpenseService

Responsible for:

* Group creation
* Expense splitting
* Settlement

### FraudDetectionService

Responsible for:

* Transfer burst analysis
* Fraud flag generation

---

# Database Rules

The schema is the source of truth.

Always follow database constraints.

---

## Student

```sql
student_id VARCHAR(20)
```

Primary identifier.

Never use email as primary lookup.

---

## Wallet

One wallet per student.

Enforced by:

```sql
UNIQUE(student_id)
```

---

## Transactions

Every money movement MUST create a transaction row.

Types:

```text
DEPOSIT
WITHDRAW
TRANSFER
PAYMENT
```

Status:

```text
SUCCESS
FAILED
PENDING
```

---

## Transfers

Transfer flow:

1. Validate sender
2. Validate receiver
3. Validate balance
4. Validate daily limit
5. Create transaction
6. Create transfer_transaction
7. Update both wallets
8. Run fraud detection

---

## Payments

Payment flow:

1. Validate wallet
2. Validate amount
3. Create transaction
4. Create campus_payment
5. Update balance

---

## Expense Settlement

Settlement flow:

1. Validate split
2. Create transfer transaction
3. Mark split as SETTLED
4. Store settled_txn_id
5. Store settled_at

---

# Fraud Detection Rules

Generate fraud flags when:

Example:

```text
More than 5 transfers
within 60 seconds
```

Store result in:

```sql
fraud_flag
```

Never block transfers automatically unless requirement explicitly states so.

---

# Exception Handling Rules

Use custom exceptions.

Examples:

```java
StudentNotFoundException
InsufficientBalanceException
InvalidAmountException
FraudDetectedException
DailyLimitExceededException
```

Never throw generic:

```java
Exception
RuntimeException
```

for business cases.

---

# Logging Rules

Use Logger utility.

Log:

* Successful transactions
* Failed transactions
* Fraud flags
* Settlement operations

Do not use:

```java
System.out.println()
```

inside services or repositories.

---

# Validation Rules

Use ValidationUtil.

Examples:

```java
validateStudentId()
validateAmount()
validateEmail()
```

Validation should happen before repository calls.

---

# Naming Conventions

Classes:

```java
StudentServiceImpl
WalletRepositoryImpl
```

Interfaces:

```java
IStudentService
IWalletRepository
```

Methods:

```java
createStudent()
transferMoney()
makePayment()
settleExpense()
```

Variables:

```java
walletBalance
transactionAmount
studentId
```

Avoid:

```java
x
temp
obj
data
```

---

# Code Generation Guidelines

When generating code:

1. Follow existing package structure.
2. Follow schema exactly.
3. Use BigDecimal for money.
4. Use LocalDateTime for timestamps.
5. Use interfaces first.
6. Use constructor injection.
7. Add JavaDoc for public methods.
8. Add meaningful comments.
9. Handle exceptions properly.
10. Keep methods under 30 lines whenever possible.

---

# SQL Mapping Rules

Table → Entity Mapping

```text
student                -> Student
wallet                 -> Wallet
transaction            -> TransactionHistory
campus_payment         -> CampusPayment
transfer_transaction   -> TransferTransaction
expense_group          -> ExpenseGroup
group_member           -> GroupMember
expense_split          -> ExpenseSplits
fraud_flag             -> FraudFlag
```

All generated code must respect these mappings.

---

# Important

Always prioritize:

1. Correctness
2. Simplicity
3. Readability
4. Maintainability
5. Performance

Never sacrifice readability for unnecessary optimization.

The schema.sql file is the authoritative source for:

* Table names
* Relationships
* Constraints
* Data types
* Business rules enforced by the database
