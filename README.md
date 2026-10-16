# Invoice & Billing REST API

A REST API that small businesses can use to manage customers, create invoices with automatic tax and discount calculation, record payments, and track what is owed.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Data JPA · MySQL 8 · Spring Security + JWT · Swagger/OpenAPI · JUnit 5 + Mockito + Testcontainers · Docker

## Features

- **Customers:** create, update, search by name/email/phone/GSTIN, deactivate (soft delete).
- **Invoices:** line items with per-line discount % and tax %. Totals are calculated on the server with `BigDecimal` and HALF_UP rounding.
- **Lifecycle:** `DRAFT → SENT → PARTIALLY_PAID → PAID`, plus `OVERDUE` (a nightly job) and `CANCELLED`.
- **Payments:** partial payments, overpayment protection, duplicate-reference protection, and optimistic locking.
- **Search & pagination:** optional filters, whitelisted sort fields, and a page-size cap.
- **Reports:** outstanding balance per customer, invoice totals by status, and cash collected in a date range.
- **Security:** stateless JWT authentication with two roles, `ADMIN` and `ACCOUNTANT`.
- **Errors:** consistent RFC 7807 Problem Details JSON.

## Prerequisites

- **JDK 21**
- **Docker Desktop**, for MySQL and the Testcontainers tests
- An IDE: **Eclipse** (2024-03 or newer), **IntelliJ IDEA**, or **VS Code**

You don't need to install Maven yourself, because every IDE below has it built in.

## Opening the project in an IDE

### Eclipse

1. **Install Lombok (one time only).** The code uses Lombok to generate getters and setters, and Eclipse needs a plugin to understand it.
   - Download `lombok.jar` from https://projectlombok.org/download.
   - Run `java -jar lombok.jar`, select your Eclipse installation, click **Install/Update**, then restart Eclipse.
   - If you skip this step, Eclipse will show errors like "The method getName() is undefined", even though the Maven build works fine.
2. Go to **File → Import → Maven → Existing Maven Projects**, then select the `billing-api` folder (the one that contains `pom.xml`) and click **Finish**.
3. Wait for Maven to download the dependencies (shown at the bottom right). If errors remain afterwards, right-click the project and choose **Maven → Update Project** (Alt+F5).
4. Make sure the project uses JDK 21. Check under **Project → Properties → Java Build Path → Libraries**.

### IntelliJ IDEA

Go to **File → Open**, select `pom.xml`, and choose **Open as Project**. The Lombok plugin is bundled. If IntelliJ prompts you, enable annotation processing.

### VS Code

Install the **Extension Pack for Java**, which includes Lombok support, then open the `billing-api` folder.

## Running locally

**1. Start MySQL in Docker**

```bash
docker compose up -d mysql
```

**2. Run the app**

Run `BillingApplication` from your IDE (right-click → **Run As → Java Application** in Eclipse). Tables are created automatically on the first start, along with an admin user: `admin` / `admin123`.

**3. Open Swagger UI**

Go to http://localhost:8080/swagger-ui.html, then:

- Call `POST /api/auth/login` with `{"username":"admin","password":"admin123"}`.
- Copy the `accessToken` from the response.
- Click **Authorize** and paste the token. Every endpoint is now callable from the browser.

### Running everything in Docker

```bash
docker compose up --build
```

This builds the app inside Docker, so Maven and a JDK aren't needed on the host.

### Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/billing` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `billing` / `billing` | DB credentials |
| `JWT_SECRET` | dev-only value | Base64 key, at least 32 bytes. **Set this outside local dev:** `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS` | `3600000` | Token lifetime (1 hour) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin123` | First admin, created on startup if missing |

## Tests

```bash
mvn test
```

Or right-click `src/test/java` → **Run As → JUnit Test** in Eclipse. The suite has three parts:

- **`InvoiceCalculatorTest`:** plain JUnit tests of the money math.
- **`InvoiceServiceTest`:** Mockito unit tests of invoice creation, editing rules, payments and status changes.
- **`CustomerRepositoryTest`:** search and pagination against a real MySQL in Docker. It is skipped automatically if Docker isn't running.

## API overview

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Get a JWT |
| POST | `/api/customers` | Create customer |
| GET | `/api/customers?q=&active=&page=&size=&sort=` | Search customers |
| GET | `/api/customers/balances?sort=outstanding,desc` | Outstanding balance per customer |
| GET / PUT | `/api/customers/{id}` | Get / update customer |
| PATCH | `/api/customers/{id}/deactivate` · `/activate` | Soft delete / restore |
| DELETE | `/api/customers/{id}` | Delete a never-invoiced customer (ADMIN) |
| POST | `/api/invoices` | Create draft invoice |
| GET | `/api/invoices?customerId=&status=&from=&to=&invoiceNumber=` | Search invoices |
| GET | `/api/invoices/summary` | Count and value per status |
| GET / PUT | `/api/invoices/{id}` | Get / edit draft |
| POST | `/api/invoices/{id}/send` | Mark as sent |
| POST | `/api/invoices/{id}/cancel` | Cancel an invoice with no payments (ADMIN) |
| DELETE | `/api/invoices/{id}` | Delete draft |
| POST / GET | `/api/invoices/{id}/payments` | Record payment / payment history |
| GET | `/api/payments?from=&to=` | Payments received in a date range |
| GET | `/api/payments/total?from=&to=` | Total collected in a date range |

### Example: create an invoice

```json
POST /api/invoices
{
  "customerId": 1,
  "notes": "Thank you for your business",
  "items": [
    { "description": "Web hosting", "quantity": 3, "unitPrice": 199.99, "discountPct": 10, "taxRate": 18 },
    { "description": "Support hours", "quantity": 2, "unitPrice": 500.00, "taxRate": 5 }
  ]
}
```

This produces subtotal **1599.97**, discount **60.00**, tax **147.19** and grand total **1687.16**. Tax is applied after the discount, and amounts are rounded per line.

## Project structure

```
src/main/java/com/example/billing
├── BillingApplication.java
├── config        SecurityConfig, OpenApiConfig, TimeConfig, DataInitializer
├── security      JwtService, JwtAuthFilter, CustomUserDetailsService, JsonSecurityErrorHandler
├── controller    Auth, Customer, Invoice, Payment
├── service       InvoiceCalculator, InvoiceService, CustomerService, PaymentService
├── repository    JPA repositories + Specifications for dynamic search
├── entity        Customer, Invoice, InvoiceItem, Payment, User + enums
├── dto           Request/response records, projections, PageResponse
├── exception     GlobalExceptionHandler + custom exceptions
└── web           Pageables (sort whitelist)
```

## Production checklist

- **JWT secret:** set a strong `JWT_SECRET` and change the admin password.
- **Schema management:** replace `ddl-auto: update` with Flyway migrations and `validate`.
- **CORS:** configure it if a browser frontend on another origin will call the API.
- **HTTPS:** put the API behind HTTPS, for example with a reverse proxy.
