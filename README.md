# Smart Wallet Transaction Logger with Real-Time Anomaly Detection

CS F213 OOP Project — Group 2
Antariksha Deb (2023A7PS0004H) | Deep Mitra (2023B5AA0670H)

## Tech Stack

- Java 21
- SQLite (dev) / PostgreSQL (prod) via JDBC
- Maven
- JUnit 5
- Java Swing (GUI)

## Project Structure

| Package             | Contents                                                                                                          |
| -------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `model`              | Transaction, DebitTransaction, CreditTransaction, RefundTransaction, AnomalyAlert, UserProfile, TransactionType    |
| `dao`                | GenericDAO, TransactionDAO, AnomalyDAO, DatabaseConnectionPool, SchemaInitialiser                                  |
| `exceptions`         | TransactionPersistenceException, InvalidTransactionException, AnomalyConfigException                               |
| `util`               | CsvLoader                                                                                                          |
| `anomaly`            | AnomalyRule, AmountSpikeRule, HighVelocityRule, DormantSpikeRule, DuplicateAttemptRule, UnusualHourRule, AnomalyConfig, AnomalyDetectionEngine, ProfileStore |
| `pipeline`           | TransactionProducer                                                                                                |
| `gui`                | MainFrame, WalletLoggerApp, TransactionTableModel, AlertTableModel, CsvIngestWorker                                |
| `test/dao`           | TransactionDAOTest, AnomalyDAOTest                                                                                 |
| `test/anomaly`       | AnomalyConfigTest, AnomalyRulesTest, AnomalyDetectionEngineTest                                                    |
| `test/integration`   | PipelineIntegrationTest                                                                                            |
| `resources`          | sample_transactions.csv, anomaly_config.properties                                                                |

## How to Run

1. Open in VS Code with the Extension Pack for Java installed
2. Let Maven download dependencies (automatic on open)
3. Run `Main.java` for the Week 1 CLI smoke test (schema + CSV load + DAO round-trip) —
   easiest via the "Run" CodeLens above `main()` in VS Code, or:
   ```
   mvn compile exec:java
   ```
   (defaults to `com.walletlogger.Main`)
4. Run the Swing desktop app (`com.walletlogger.gui.WalletLoggerApp`) the same way from
   VS Code, or:
   ```
   mvn compile exec:java "-Dexec.mainClass=com.walletlogger.gui.WalletLoggerApp"
   ```
   Quote the **whole** `-Dexec.mainClass=...` argument (not just the class name) if you're
   on PowerShell — quoting only the value can get the argument mis-split at the `=`.
5. Run `mvn test` for all unit and integration tests

### Resetting / erasing stored data

Two ways to start with a clean slate:

- **In-app (recommended):** File > **Reset All Data...** in the GUI. This stops the engine,
  deletes `wallet.db` and `data/user_profiles.ser`, then reinitialises everything fresh
  without needing to restart the application.
- **Manually:** close the app, delete `wallet.db` (the SQLite database) and the `data/`
  folder (the serialized user-profile history) from the project root, then run it again.

### Using the GUI

- **File > Load Sample CSV** streams `sample_transactions.csv` into the live anomaly-detection
  pipeline (with a small delay per row so you can watch it happen) — see it land in the
  **Live Feed** tab as each transaction is processed.
- **File > Load CSV...** does the same for any CSV file you pick, in the same
  `transaction_id,user_id,vendor_id,amount,timestamp,type` format.
- **File > Simulate Single Transaction...** lets you manually inject one transaction to trigger
  a specific rule on demand (e.g. submit the same amount/vendor twice quickly to trigger
  `DUPLICATE_ATTEMPT`).
- **All Transactions** tab supports filtering by user, amount range, type, and flagged-only.
- **Anomaly Alerts** tab shows every alert raised, filterable by user, most recent first, with
  a color-coded severity badge (LOW/MEDIUM/HIGH/CRITICAL).
- Flagged rows are highlighted in the transaction tables.
- Closing the window gracefully stops the engine and serializes all `UserProfile` state to
  `data/user_profiles.ser`, so running averages/velocity history survive a restart.

The GUI uses [FlatLaf](https://www.formdev.com/flatlaf/) for a modern flat theme, plus a
custom indigo dashboard header with live stat cards (transactions processed, alerts raised,
flagged rate, active users), striped/badge-styled tables, and a proper modal form for
simulating transactions.

## Anomaly Detection Engine (Week 2)

A single-threaded `AnomalyDetectionEngine` consumes `Transaction` objects from a shared
`BlockingQueue` (the classic producer/consumer pattern — `TransactionProducer` or the GUI's
`CsvIngestWorker` are the producers). For every transaction it runs five independent rules
against the user's running `UserProfile` (mean/stddev via Welford's online algorithm, a
rolling timestamp window, and last vendor/amount):

| Rule                | Fires when...                                                                 |
| -------------------- | ------------------------------------------------------------------------------ |
| `AMOUNT_SPIKE`       | Amount is more than N standard deviations above the user's historical mean     |
| `HIGH_VELOCITY`      | Too many transactions from the same user within a short rolling window        |
| `DORMANT_SPIKE`      | A large transaction follows a long stretch of user inactivity                 |
| `DUPLICATE_ATTEMPT`  | Same vendor + amount as the immediately preceding transaction, within seconds |
| `UNUSUAL_HOUR`       | Transaction occurs outside normal daytime hours                              |

All thresholds are tunable in `src/main/resources/anomaly_config.properties` and are validated
at startup by `AnomalyConfig`, throwing `AnomalyConfigException` on anything missing or invalid
— failing fast rather than silently using bad numbers. Fired alerts are ranked by severity
through a `PriorityQueue` (`AnomalyAlert` implements `Comparable`) before being persisted via
`AnomalyDAO` and surfaced to any registered `AnomalyDetectionEngine.AlertListener` (the GUI is
one such listener). Shutdown uses a poison-pill sentinel pushed onto the queue so the consumer
thread exits cleanly. Per-user profile state is serialized to disk via `ProfileStore` so restart
doesn't lose historical context.

## Week Progress

- [x] Week 1 — Domain models, JDBC DAO layer, DB schema, CSV loader, unit tests
- [x] Week 2 — Anomaly detection engine, BlockingQueue pipeline, serialisation
- [x] Week 3 — Swing GUI, SwingWorker, integration testing, demo
