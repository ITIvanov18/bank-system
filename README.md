# Bank Management System

A full-stack web application designed to manage bank clients, bank accounts, and credit services. This project implements a robust monolithic Spring Boot backend and a modern React with TypeScript frontend, utilizing Docker for seamless database management.

## 🚀 Features

### Client & Account Management
* **Register Clients:** Support for both Physical Persons (Name, EGN) and Legal Entities (Company Name, EIK, Representative).
* **Open Bank Accounts:** Generate and assign IBANs to clients, track account balances, and manage account statuses (Active/Closed).
* **Relationships:** A single client can hold multiple bank accounts and multiple credits.

### Credit Services & Installment Plans
* **Grant Credits:** Issue Consumer and Mortgage credits with specific interest rates, maximum amounts, and duration limits.
* **Annuity Installment Plans:** Automatically generate a month-by-month repayment schedule using the annuity formula.
* **Payment Tracking:** Mark specific monthly installments as paid.
* **Credit Status:** Real-time monitoring of remaining balances and overall credit status (Active/Paid).

---

## 🛠️ Tech Stack

**Backend**
* **Framework:** Spring Boot 3.x (Java 21)
* **Build Tool:** Gradle (Groovy)
* **Database Access:** Spring Data JPA / Hibernate
* **Validation:** Spring Boot Starter Validation
* **Utilities:** Lombok, Spring Boot DevTools

**Frontend**
* **Library:** React 18
* **Language:** TypeScript
* **Build Tool:** Vite
* **Routing:** React Router DOM
* **HTTP Client:** Axios

**Infrastructure & Database**
* **Database:** MySQL 8.0
* **Containerization:** Docker & Docker Compose

---

## 📂 Project Structure

The repository is structured as a monorepo containing three main directories:

```text
bank-system-project/
├── bank-backend/           # Spring Boot REST API (Java)
├── bank-frontend/          # React + TypeScript UI
└── docker/                 # Infrastructure configuration
    └── docker-compose.yml

## Temporary Password Email Delivery

Customer onboarding now sends the generated temporary password directly to the customer email.
The API no longer returns plaintext temporary passwords.

### Required backend environment variables

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_SMTP_AUTH`
- `MAIL_SMTP_STARTTLS`
- `APP_MAIL_FROM` (optional, defaults to `no-reply@bankai.com`)

You can store these in `bank-system/.env` (auto-loaded by Spring) instead of setting IDE environment variables.

Setup for new developers:

```powershell
Copy-Item "bank-system/.env.example" "bank-system/.env"
```

Default local development values are configured for SMTP on `localhost:1025` (MailHog/Mailpit).

### Local mail testing with MailHog

```powershell
docker run -d --name bank-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

Mail inbox UI:

- `http://localhost:8025`

When onboarding succeeds, customers receive an email with their temporary password and first-login instructions.

### Real SMTP delivery (custom domain sender)

Set environment variables before starting backend:

```powershell
$env:MAIL_HOST="smtp.your-provider.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="no-reply@bankai.com"
$env:MAIL_PASSWORD="your_provider_smtp_password"
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_SMTP_STARTTLS="true"
$env:MAIL_SMTP_SSL="false"
$env:APP_MAIL_FROM="no-reply@bankai.com"
```

Prerequisites for real delivery:

- `bankai.com` must be verified in your SMTP provider.
- SPF and DKIM DNS records must be configured for `bankai.com`.
- The sender identity `no-reply@bankai.com` must be allowed by the provider.

The onboarding response now includes `emailDeliveryChannel` and `emailRelay` so you can verify whether emails go to local MailHog or external SMTP.

