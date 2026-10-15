# Online Job Portal (Java Swing + MySQL)

A teaching-oriented desktop job portal built with Java 17+, Swing, JDBC, and MySQL. It has role-specific Admin, Employer, and Job Seeker workspaces and stores its records in MySQL.

## Implemented features

- Email/password sign-in, registration, SHA-256 password digests, account status checks, and role-specific workspaces.
- Admin views for users, jobs, applications, recent activity, and editable system settings; user activation/suspension and job approval/rejection/closure.
- Employer job posting, application review/status changes, per-job application counts, and internal messaging.
- Job Seeker approved-job search, duplicate-checked applications, application tracking, profile editing, recommendations using a skill match, and messages.
- Prepared statements for user-provided database values, foreign keys, unique constraints, indexes, and activity events.

## Requirements

- Java Development Kit 17 or later
- MySQL Server 8.0 or later
- Maven 3.9+ (or an IDE that imports Maven projects)

Install a JDK from a trusted OpenJDK distribution and MySQL Server from the MySQL Community downloads. Ensure `java` and `mvn` are on PATH. MySQL 8 supports the `utf8mb4_0900_ai_ci` collation used in the script.

## Database setup

1. Start MySQL Server.
2. Import `database/job_portal.sql` using MySQL Workbench's **Open SQL Script** and **Execute**, or run `mysql -u root -p < database/job_portal.sql` in a terminal.
3. By default, the program connects to `jdbc:mysql://localhost:3306/job_portal` as `root` with an empty password. If your MySQL account has a password, set these environment variables on that computer. On Windows, search **Edit environment variables for your account**, add both under **User variables**, then restart VS Code:
   - `JOBPORTAL_DB_URL` (optional full JDBC URL)
   - `JOBPORTAL_DB_USER`
   - `JOBPORTAL_DB_PASSWORD`

The application account needs SELECT, INSERT, UPDATE, and DELETE privileges on `job_portal`. Do not use an unrestricted production root account.

## Build and run

From this project directory:

```sh
mvn clean package
java -jar target/online-job-portal-1.0.0.jar
```

Maven downloads the MySQL JDBC driver as declared in `pom.xml`. In an IDE, import the folder as a Maven project and run `portal.Main` after Maven dependencies resolve. If your environment cannot download Maven dependencies, add the MySQL Connector/J 9.4.0 JAR to the IDE classpath.

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Admin | admin@jobportal.com | admin123 |
| Employer | employer@techcorp.com | employer123 |
| Job Seeker | seeker@gmail.com | seeker123 |

These are classroom demo application accounts. Each person imports the SQL into their own local database and configures their own MySQL connection credentials; do not send your MySQL password with the project.

## Structure

```text
OnlineJobPortal/
├── database/job_portal.sql
├── pom.xml
├── README.md
└── src/main/java/portal/Main.java
```

`Main` is the Swing entry point, holds the small Account session record, and routes to role-specific tabs. Its JDBC helper opens connections using environment-based configuration. Each tab reads its table through JDBC; forms execute parameterized create/update/delete operations. The SQL schema separates users from seeker/employer profile data, relates employer-owned jobs to seeker applications, and tracks messages, settings, and activity. Methods are kept explicit so students can follow the JDBC flow and Swing event handlers.

## ER diagram description

`users` has one optional one-to-one row in `job_seekers` or `employers`; the role column distinguishes account types. One employer owns many jobs. A job has many applications and each job seeker can submit at most one application per job (`UNIQUE(job_id,seeker_id)`). Users send and receive messages. Activity rows optionally reference a user, and system settings are key/value rows. Foreign keys cascade profile, job, application, and message data when their owner is deleted; activity user references become null so the audit record remains.

## Viva walkthrough

- **Encapsulation:** model data is held in database tables; the `Account` record carries authenticated session identity.
- **Abstraction:** JDBC connection and dialog operations are hidden behind helper methods; Swing presents the workflow.
- **Interfaces/polymorphism/inheritance:** the implementation favors the Java standard Swing component hierarchy and JDBC interfaces (`Connection`, `PreparedStatement`, `ResultSet`); it intentionally avoids artificial subclass layers that do not help explain the project.
- **JDBC and SQL:** each action uses try-with-resources and prepared statements; joins combine jobs, employers, seekers, and applications.
- **Authorization:** login selects the role and the UI exposes the corresponding dashboard. Data-changing employer queries also constrain ownership by the signed-in user ID.
- **Validation and constraints:** registration validates email/password; SQL enforces uniqueness and relationships; duplicate applications are prevented in the database.

## Current scope notes

This is a compact runnable university-project implementation. Resume management stores a resume path/text field rather than providing a binary upload workflow. Search filtering is a table text filter over the loaded approved-job list; salary/type/experience filters can be extended in the same screen. Passwords use a SHA-256 digest for a dependency-free demonstration; a production system should use a salted, slow password hash (Argon2id, scrypt, or bcrypt). UI error dialogs intentionally avoid showing raw SQL messages.

## Sample viva questions

1. **Why use JDBC?** It provides Java's standard API for executing SQL and mapping result sets.
2. **How is SQL injection reduced?** User-controlled values are passed through `PreparedStatement` parameters.
3. **How is a duplicate application prevented?** The application table has a unique constraint on job and seeker, in addition to UI feedback.
4. **What is a foreign key?** A database constraint that ensures a reference points to an existing parent row.
5. **How does job approval work?** Employer submissions start as `PENDING`; an administrator updates the status; seeker search selects only approved, unexpired jobs.
6. **How are roles handled?** Login retrieves the database role and opens its corresponding dashboard; employer actions also filter by owner ID.
7. **What does try-with-resources do?** It closes JDBC resources automatically, including when exceptions occur.
8. **How do recommendations work?** The first comma-separated seeker skill is compared against job skills and requirements.
9. **Why separate profile tables from users?** Each role has role-specific data, while shared credentials and account state live in one table.
10. **How could authentication be improved?** Use a unique random salt and a slow password hashing function such as Argon2id or bcrypt.
