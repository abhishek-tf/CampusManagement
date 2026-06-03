# Campus Payment Platform

## Overview
**Campus Digital Payment & Expense Management Platform** - A comprehensive Java-based payment system for campus environments with proper folder structure following SOLID principles and Lombok annotations.

## ER Diagram Entities
Based on the provided database schema:
- **Student**: Core student profiles
- **Wallet**: Student accounts with daily limits
- **TransactionHistory**: Complete transaction logs
- **TransferTransaction**: P2P transfers
- **CampusPayment**: Institutional payments
- **ExpenseGroup**: Shared expense groups
- **GroupMember**: Group memberships
- **ExpenseSplits**: Expense calculations
- **FraudFlag**: Fraud detection flags

## Project Structure (Per ER Diagram)

```
campus-payment-platform/
├── src/main/java/com/campus/
│   ├── entity/                    # Entity classes (9 classes with @Data @Builder)
│   ├── enums/                     # Enum types (4 classes)
│   ├── exception/                 # Custom exceptions (6 classes)
│   ├── repository/
│   │   ├── interfaces/            # Repository contracts (8 interfaces - ISP)
│   │   └── impl/                  # Repository implementations (8 classes)
│   ├── service/
│   │   ├── interfaces/            # Service contracts (5 interfaces)
│   │   └── impl/                  # Service implementations (5 classes - SRP)
│   ├── dto/                       # Data transfer objects
│   ├── config/                    # Configuration classes
│   ├── util/                      # Utility classes
│   ├── constants/                 # Constants
│   ├── controller/                # API controllers
│   ├── menu/                      # Console UI
│   └── Main.java
├── src/main/resources/
│   ├── schema.sql                 # Complete MySQL schema
│   ├── seed.sql                   # Sample data
│   └── db.properties              # DB configuration
├── src/test/java/
└── pom.xml
```

## SOLID Principles Implementation

### 1. Single Responsibility (SRP)
Each service handles ONE domain:
- **StudentService**: Student registration, search, update
- **WalletService**: Wallet operations (topup, withdrawal, transfer)
- **PaymentService**: Payment processing
- **ExpenseService**: Group and split management
- **FraudDetectionService**: Fraud monitoring

### 2. Open/Closed (OCP)
- Service interfaces allow new implementations
- Repository interfaces for different persistence layers

### 3. Liskov Substitution (LSP)
- All implementations follow their interfaces
- Repository implementations interchangeable

### 4. Interface Segregation (ISP)
Focused repository interfaces:
- `IStudentRepository`: Student CRUD operations
- `IWalletRepository`: Wallet operations only
- `ITransactionRepository`: Transaction queries
- `ITransferRepository`: Transfer operations
- `IPaymentRepository`: Payment records
- `IExpenseRepository`: Group & member management
- `ISplitRepository`: Expense splits
- `IFraudRepository`: Fraud flags

### 5. Dependency Inversion (DIP)
- Services depend on repository interfaces
- Constructor injection for loose coupling
- No tight coupling to implementations

## Lombok Annotations

All entities use Lombok to eliminate boilerplate:
```java
@Data              // getters, setters, equals, hashCode, toString
@Builder           // fluent builder pattern
@NoArgsConstructor // default constructor
@AllArgsConstructor // constructor with all fields
@EqualsAndHashCode // custom equals/hashCode (excluding timestamps)
@ToString          // custom toString
```

## Entities Implemented

| Entity | Fields | Repository | Service |
|--------|--------|------------|---------|
| Student | studentId, name, email, phone, department | ✓ | ✓ |
| Wallet | walletId, studentId, balance, limits | ✓ | ✓ |
| TransactionHistory | transactionId, studentId, type, amount | ✓ | ✓ |
| TransferTransaction | transferId, from/toStudentId, amount | ✓ | ✓ |
| CampusPayment | paymentId, category, amount, status | ✓ | ✓ |
| ExpenseGroup | groupId, name, members | ✓ | ✓ |
| GroupMember | memberExpenseId, groupId, studentId | ✓ | ✓ |
| ExpenseSplits | splitId, shareAmount, paidAmount | ✓ | ✓ |
| FraudFlag | flagId, reason, transactionCount | ✓ | ✓ |

## Key Features

✅ Student Management (register, search, update)
✅ Wallet Management (topup, withdraw, transfer, limits)
✅ Transaction History & Tracking
✅ P2P Money Transfers
✅ Campus Payments (multiple categories)
✅ Expense Splitting (group-based)
✅ Fraud Detection (suspicious activity flagging)
✅ Daily Transfer Limits
✅ MySQL Integration

## Technologies

- **Language**: Java 11+
- **Build**: Maven
- **Database**: MySQL 8.0+
- **Code Generation**: Lombok
- **Persistence**: JDBC

## Setup

```bash
# 1. Create database
mysql -u root < src/main/resources/schema.sql

# 2. Load sample data
mysql -u root campus_payment_db < src/main/resources/seed.sql

# 3. Configure connection
# Edit src/main/resources/db.properties

# 4. Build
mvn clean install

# 5. Run
java -cp target/* com.campus.Main
```

## Design Patterns Used

1. **Repository Pattern** - Data access abstraction
2. **Service Layer** - Business logic encapsulation
3. **Factory Pattern** - Dynamic object creation
4. **Singleton Pattern** - Configuration & DB connection
5. **Builder Pattern** - Entity construction (via Lombok)
6. **Dependency Injection** - Loose coupling

## Exception Hierarchy

Base: `CampusPaymentException`
- `InsufficientBalanceException`
- `DailyLimitExceededException`
- `StudentNotFoundException`
- `InvalidAmountException`
- `FraudDetectedException`

---

**Version**: 1.0.0 | **Buildathon-1** | **SOLID + Lombok**
