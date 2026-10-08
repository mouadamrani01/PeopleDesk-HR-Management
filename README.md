# PeopleDesk — HR Management System

PeopleDesk is a web application for managing employees, absences, internal messages and weekly work assignments.

This project was developed as part of my academic studies to apply **Java EE concepts using Eclipse**, including Servlets, JSP, JDBC and relational database management. It demonstrates how a Java web application connects user interfaces, business rules and persistent data through an MVC architecture.

## Features

### Employee management
- Create, view, edit and delete employee records.
- Manage contact details, positions and monthly salaries.
- Assign administrator or employee access.

### Absence tracking
- Record an employee's absence date, duration and reason.
- Allow administrators to review team absences.
- Allow employees to view their own absence records.

### Internal messaging
- Send messages to other employees.
- View received messages and reply to their sender.
- Delete messages from the recipient's inbox.

### Weekly planning
- Generate Monday–Friday task assignments for employees.
- Adjust the task rotation using an offset.
- View assignments for a selected week.
- Allow employees to consult their own planning.

### Account and interface
- Sign in and sign out with session-based authentication.
- Change a password after verifying the current password.
- Navigate through a responsive interface with a sidebar and dashboard.
- View team, absence and inbox counters drawn from the database.

## Technologies

| Area | Technologies |
| --- | --- |
| Language | Java; compilation target: Java 17 |
| Web layer | Java EE 8, Servlets, JSP and JSTL |
| Data access | JDBC |
| Database | H2 for local demonstration; MySQL configuration available |
| Server | Apache Tomcat 9 |
| Build | Apache Maven |
| Interface | HTML and CSS |
| Testing | JUnit 5 and HTTP integration tests |
| Development environment | Eclipse for the academic project; current Maven layout can also be opened in VS Code |
| Automation | GitHub Actions build and test workflow |

## Architecture

The application follows an MVC structure:

- **Views:** JSP pages display data and provide forms.
- **Controller:** Servlets process requests, validate inputs and enforce access rules.
- **Data access:** JDBC repositories read and update the database.
- **Security:** A filter handles authentication checks, CSRF tokens and response headers.

```text
src/main/java/hr/
  AppServlet.java       Routing, validation and authorization
  SecurityFilter.java   Authentication and CSRF checks
  Repository.java       JDBC queries and transactional planning
  Database.java         Configuration and database initialization
  Passwords.java        Password hashing and verification
src/main/webapp/
  assets/               Application styles
  WEB-INF/views/        JSP pages
src/main/resources/
  schema.sql            Database tables and constraints
src/test/java/hr/       HTTP integration tests and local server launcher
```

## Run locally

### Requirements

- JDK 17 or 21.
- Maven 3.9 or later.
- Internet access for the first dependency download.

Open a terminal in the directory containing `pom.xml`:

```sh
mvn clean verify
```

Start the application:

```sh
mvn test-compile exec:java "-Dexec.mainClass=hr.LocalServer" "-Dexec.classpathScope=test"
```

Open **http://localhost:8080/hr-management/** in your browser.

Keep the terminal open while using the application. Press **Ctrl+C** to stop the server.

### Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Administrator | admin@example.test | DemoAdmin!2026 |
| Employee | employee@example.test | DemoEmployee!2026 |

These accounts are intended for local demonstration. They are created when the database is empty.

Local data is stored in the `data/` directory and persists between restarts. The directory is excluded from Git. Tests use a separate in-memory database.

## MySQL configuration

The repository includes a Docker Compose configuration for MySQL and Tomcat.

1. Copy `.env.example` to `.env`.
2. Set new database passwords and the administrator credentials.
3. Use an administrator password of at least 12 characters.
4. Start the application:

```sh
docker compose up --build
```

On Windows PowerShell, the copy command is:

```powershell
Copy-Item .env.example .env
```

The application creates its tables and initial administrator account in an empty database. Changing the startup administrator password in `.env` does not reset an existing account.

| Environment variable | Purpose |
| --- | --- |
| APP_DEMO | Set to `false` to disable demo accounts. |
| DB_URL | JDBC database URL. |
| DB_USER | Database username. |
| DB_PASSWORD | Database password. |
| ADMIN_EMAIL | Initial administrator email outside demo mode. |
| ADMIN_PASSWORD | Initial administrator password outside demo mode. |
| PORT | Local embedded-server port; default: `8080`. |

## WAR deployment

Build the application:

```sh
mvn clean verify
```

Copy `target/hr-management.war` to the `webapps` directory of **Tomcat 9**, then start the server.

The application uses the `javax.servlet` API. Deploying to Tomcat 10 or later requires migration to Jakarta packages.

## Security and validation

- Passwords are stored as salted PBKDF2-HMAC-SHA256 hashes.
- Administrator actions require server-side authorization.
- Employees can access only their own absences, planning and received messages.
- POST forms include CSRF tokens.
- JDBC queries use bound parameters.
- JSP pages escape user-provided values.
- Database connections and query resources are closed after use.

## Tests

```sh
mvn clean verify
```

The integration suite starts Tomcat and exercises the application over HTTP. It covers authentication, permissions, employee CRUD, input validation, absence ownership, messaging, weekly planning, password changes and logout.

A GitHub Actions workflow runs the Maven build and tests on pushes and pull requests. Details of the checks performed are available in [docs/VALIDATION.md](docs/VALIDATION.md).

## Scope

The project focuses on employee administration, communication and simple weekly task rotation. Planning does not account for absences or staffing capacity. Payroll, password recovery and an audit trail are outside the current scope.

## Learning objectives

- Apply Java EE web development concepts.
- Handle HTTP requests using Servlets.
- Render dynamic pages using JSP and JSTL.
- Implement relational data persistence with JDBC.
- Organize code using MVC principles.
- Manage sessions, permissions and form validation.
- Package a web application and verify its behavior with automated tests.
