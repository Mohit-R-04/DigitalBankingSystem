# Digital Banking System — Microservices

## Services Overview

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | Single entry point, Rate limiting, CORS |
| account-service | 8081 | Account management, Balance |
| transaction-service | 8082 | Money transfers, Transaction history |
| payment-service | 8083 | Razorpay integration, Webhooks |
| fraud-detection-service | 8084 | Real time fraud detection via Redis |
| notification-service | 8085 | Transaction and fraud alerts |

---

## Architecture Flow

```
User → API Gateway (rate limiting)
             ↓
    Account / Transaction / Payment Service
             ↓
        Apache Kafka
             ↓
    ┌────────────────────────┐
    │                        │
Fraud Detection      Notification Service
(Redis patterns)     (alerts via email/SMS)
    │
Account Service
(block if fraud)
```

---

## Kafka Topics

| Topic | Publisher | Consumer |
|---|---|---|
| transaction.initiated | Transaction Service | Fraud Detection |
| fraud.check.result | Fraud Detection | Transaction Service |
| transaction.completed | Transaction Service | Account Service, Notification |
| fraud.detected | Fraud Detection | Account Service, Notification |
| payment.completed | Payment Service | Notification |
| payment.failed | Payment Service | Notification |
| transaction.refunded | Transaction Service | Notification |
| transaction.otp.generated | Transaction Service | Notification |
| verification.required | Fraud Detection | Transaction Service |
| fraud.check.clean | Fraud Detection | Transaction Service |

---

## How To Run

### Prerequisites
- Docker Desktop
- Java 21 (`JAVA_HOME` must point to Java 21)
- Maven 3.9+
- Node.js 18+

### Step 1: Start Infrastructure
```bash
docker-compose up -d
```

### Step 2: Start All Services
```bash
# Set Java 21 for all services
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home

# Terminal 1
cd account-service && mvn spring-boot:run

# Terminal 2
cd transaction-service && mvn spring-boot:run

# Terminal 3
cd payment-service && mvn spring-boot:run

# Terminal 4
cd fraud-detection-service && mvn spring-boot:run

# Terminal 5
cd notification-service && mvn spring-boot:run

# Terminal 6
cd api-gateway && mvn spring-boot:run
```

### Step 3: Start Frontend
```bash
cd frontend && npm install && npm run dev
# Opens at http://localhost:3000
```

### Stop All Services
```bash
# Stop frontend (port 3000)
lsof -ti:3000 | xargs kill -9 2>/dev/null

# Stop all Java Spring Boot services (ports 8080-8085)
pkill -f 'mvn spring-boot:run' 2>/dev/null
pkill -f 'java.*banking' 2>/dev/null

# Stop Docker infrastructure (MySQL, Redis, Kafka)
docker-compose down

# Optional: Remove all data (MySQL volume, etc.)
docker-compose down -v
```

---

## Frontend Features

| Page | Description |
|---|---|
| Dashboard | Quick actions, account lookup |
| Create Account | Open new savings/current/FD account |
| Transfer | SAGA-based money transfer with OTP |
| Transactions | View full transaction history |
| Add Money | Simulate deposit (Razorpay flow) |
| Account Details | View account, block account |

Features: Dark/Light mode toggle, fully responsive for all screen sizes.

---

## Useful Commands

### View All Accounts (MySQL)
```bash
docker exec mysql mysql -uroot -proot -e \
  'SELECT account_number, account_holder_name, email, balance, status FROM account_db.accounts;'
```

### View All Transactions
```bash
docker exec mysql mysql -uroot -proot -e \
  'SELECT * FROM transaction_db.transactions;'
```

### View All Payments
```bash
docker exec mysql mysql -uroot -proot -e \
  'SELECT * FROM payment_db.payments;'
```

### Check Redis (fraud counters, OTPs)
```bash
docker exec -it redis redis-cli keys '*'
```

### Check Kafka Topics
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### View Service Logs
```bash
# MySQL
docker logs mysql

# Kafka
docker logs kafka

# Redis
docker logs redis
```

---

## Test Account

```
Account: 378669440034
Holder:  Mohit Reddy
Balance: ₹50,000
```

