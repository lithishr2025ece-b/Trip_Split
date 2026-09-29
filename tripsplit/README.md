# TripSplit — Group Travel Expense Settlement Tracker

A beginner-friendly full-stack Java 17 Spring Boot application for recording group trip expenses and calculating who should pay whom.

## 1. Features

- Create, view, edit and delete trips.
- Add, edit, view and delete participants.
- Add, edit, view and delete shared expenses.
- Split an expense equally among selected participants.
- Calculate total paid, total owed and net balance.
- Generate practical settlement transactions using a simple greedy matching algorithm.
- Service-layer checks make sure every participant belongs to the trip and the total balance is zero.
- Clear JSON error responses through a global exception handler.
- Simple Thymeleaf + HTML + CSS + JavaScript frontend.

## 2. Technologies

- Java 17
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA
- Spring Validation
- MySQL
- Maven
- Thymeleaf
- HTML5, CSS3 and Vanilla JavaScript

No React, Angular, Vue, Bootstrap, Tailwind, Node.js or PHP is used.

## 3. Requirements

Install:

1. JDK 17
2. MySQL 8.x
3. IntelliJ IDEA
4. Maven 3.9+ (or use IntelliJ's Maven support)

## 4. MySQL setup

MySQL 8.x must be installed and running on port 3306. The project is already configured for user `root` (see `application.properties`). The `tripsplit` database and all tables are created automatically on first start, so no SQL needs to be run manually.

## 5. Run in IntelliJ IDEA

1. Extract the zip and open the `tripsplit` folder in IntelliJ IDEA (File > Open).
2. Wait for Maven to import (JDK 17 or newer).
3. Click the green Run button (or run `TripSplitApplication`).
4. Your browser opens `http://localhost:8080/` automatically.

## 7. Main REST API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/trips` | Create trip |
| GET | `/api/trips` | List trips |
| GET | `/api/trips/{id}` | Get trip |
| PUT | `/api/trips/{id}` | Edit trip |
| DELETE | `/api/trips/{id}` | Delete trip |
| POST | `/api/trips/{tripId}/participants` | Add participant |
| GET | `/api/trips/{tripId}/participants` | List participants |
| PUT | `/api/participants/{id}` | Edit participant |
| DELETE | `/api/participants/{id}` | Delete participant |
| POST | `/api/trips/{tripId}/expenses` | Add expense |
| GET | `/api/trips/{tripId}/expenses` | Expense history |
| PUT | `/api/expenses/{id}` | Edit expense |
| DELETE | `/api/expenses/{id}` | Delete expense |
| GET | `/api/trips/{tripId}/balances` | Calculate balances |
| POST | `/api/trips/{tripId}/settlements/generate` | Generate settlement |
| GET | `/api/trips/{tripId}/settlements` | View saved settlement |

## 8. Frontend pages

- `/` — Dashboard
- `/trips` — Trips list
- `/trips/create` — Create trip
- `/trips/{id}` — Trip details
- `/trips/{id}/participant` — Add participant
- `/trips/{id}/expense` — Add expense
- `/trips/{id}/balance` — Balances
- `/trips/{id}/settlement` — Settlement

## 9. Example API requests

### Create trip

```json
{
  "name": "Ooty Trip",
  "description": "College friends trip",
  "startDate": "2026-10-01",
  "endDate": "2026-10-03"
}
```

### Add participant

```json
{
  "name": "Arun",
  "email": "arun@example.com"
}
```

### Add expense

```json
{
  "description": "Hotel",
  "amount": 3000.00,
  "payerId": 1,
  "expenseDate": "2026-10-01",
  "participantIds": [1, 2, 3]
}
```

## 10. Example calculation

If Arun pays ₹3000 and Arun, Bala and Charlie share it:

- Arun paid = ₹3000
- Arun owed = ₹1000
- Arun net = +₹2000
- Bala owed = ₹1000
- Bala net = -₹1000
- Charlie owed = ₹1000
- Charlie net = -₹1000

Total net = `2000 - 1000 - 1000 = 0`.

Settlement:

- Bala pays Arun ₹1000
- Charlie pays Arun ₹1000

The application splits money in cents, so an amount such as ₹100 divided by three is stored as ₹33.34, ₹33.33 and ₹33.33 instead of losing a cent to rounding.

## 11. Business-rule tests

Try these cases in the frontend or Postman:

1. Create a trip.
2. Add three participants.
3. Add hotel, food and fuel expenses.
4. View expense history.
5. View balances.
6. Generate settlement.
7. Confirm the settlement clears the balances.
8. Try amount `0` — it must fail.
9. Use a participant ID from another trip — it must fail.
10. Send an empty `participantIds` list — it must fail.
11. Use an invalid trip ID — it must return 404.
12. Try a payer who is not included in the shared participants — it must fail.

## 12. Postman examples

For all POST/PUT JSON requests, use:

`Content-Type: application/json`

### Create trip

`POST http://localhost:8080/api/trips`

### Add participant

`POST http://localhost:8080/api/trips/1/participants`

### Add expense

`POST http://localhost:8080/api/trips/1/expenses`

### Get balances

`GET http://localhost:8080/api/trips/1/balances`

### Generate settlement

`POST http://localhost:8080/api/trips/1/settlements/generate`

## 13. Common errors and fixes

### Cannot connect to MySQL

- Make sure MySQL is running.
- Check the username and password in `application.properties`.
- Make sure the `tripsplit` database exists.
- Check that MySQL is using port 3306.

### Access denied for root

Your MySQL password is different from the value in `application.properties`. Update `spring.datasource.password` in `application.properties`.

### Port 8080 already in use

Stop the other application using port 8080, or change `server.port` in `application.properties`.

### Participant does not belong to this trip

The expense request contains a participant ID belonging to another trip. Use only participant IDs from the selected trip.

### Payer should be included among the expense participants

Select the payer in the "Shared by" list too.

### No participants yet

Add participants before adding an expense.

## 14. Project structure

```text
tripsplit/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/example/tripsplit/
    │   │   ├── TripSplitApplication.java
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/
    │       └── static/
    │           ├── css/style.css
    │           └── js/app.js
    └── test/
```

## 15. Important note

This project is intentionally written with ordinary Java classes, explicit getters/setters, simple services, simple controllers, and straightforward JavaScript so it is easier for a beginner to understand and modify.
