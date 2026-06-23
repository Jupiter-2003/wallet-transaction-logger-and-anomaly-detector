cat > ~/Desktop/wallet-transaction-logger-and-anomaly-detector/README.md << 'OUTER'

# Smart Wallet Transaction Logger with Real-Time Anomaly Detection (in progress)

CS F213 OOP Project — Group 2  
Antariksha Deb (2023A7PS0004H) | Deep Mitra (2023B5AA0670H)

## Tech Stack

- Java 21
- SQLite (dev) / PostgreSQL (prod) via JDBC
- Maven
- JUnit 5
- Java Swing (GUI — Week 3)

## Project Structure

src/
├── main/java/com/walletlogger/
│ ├── model/ # Transaction, AnomalyAlert, UserProfile, enums
│ ├── dao/ # GenericDAO, TransactionDAO, AnomalyDAO, ConnectionPool
│ ├── exceptions/ # Custom checked exceptions
│ ├── util/ # CsvLoader
│ └── Main.java
├── test/java/com/walletlogger/
│ └── dao/ # JUnit 5 tests
└── resources/
└── sample_transactions.csv

## How to Run

1. Open in VS Code with the Extension Pack for Java installed
2. Let Maven download dependencies (automatic on open)
3. Run `Main.java` for the smoke test
4. Run `mvn test` for all unit tests

## Week Progress

- [x] Week 1 — Domain models, JDBC DAO layer, DB schema, CSV loader, unit tests
- [ ] Week 2 — Anomaly detection engine, BlockingQueue pipeline, serialisation
- [ ] Week 3 — Swing GUI, SwingWorker, integration testing, demo
