# Validation of the delivered source

Date: 2026-10-08.

## Executed successfully

- Java compilation using OpenJDK/Temurin 17 and Maven 3.9.9.
- `mvn clean verify`: **BUILD SUCCESS**.
- **7 integration tests, 0 failures, 0 errors, 0 skipped**.
- Real HTTP requests to embedded Tomcat 9.0.122 with H2 in MySQL compatibility mode.
- JSP compilation/rendering for login, dashboard, employees, absences, messages, schedule and profile.
- Employee create/update/delete, duplicate email conflict, negative salary rejection and escaped user content.
- Absence creation/deletion and restriction of employee views to their own data.
- Two distinct inbox messages, reply recipient selection, message ownership and deletion.
- Weekly planning regeneration, offset changes, preservation of unrelated weeks and admin-only generation.
- Password change, password verification, logout, session protection and CSRF rejection.
- WAR packaging and inspection of controller/filter classes, schema, JSP views and deployment descriptor.
- README development launcher started successfully against a persistent local H2 file database.

## Not executed here

- Docker image build and Docker Compose stack.
- A real MySQL server connection or schema execution on MySQL.
- Deployment to a separately installed standalone Tomcat.
- Browser screenshot / visual layout inspection.
- GitHub Actions execution on a published repository.
- Load testing, penetration testing, or a full dependency vulnerability scan.

The delivered MySQL/Docker configuration is provided for local verification, not reported as tested. H2 MySQL compatibility is useful for tests but is not a substitute for a real MySQL integration run.

## Reproduce

```sh
mvn clean verify
mvn test-compile exec:java -Dexec.mainClass=hr.LocalServer -Dexec.classpathScope=test
```

For MySQL, populate `.env` and run `docker compose up --build`, then manually check both employee administration and message/absence/planning operations. The non-demo stack starts with one administrator; create a second employee to exercise employee permissions.
