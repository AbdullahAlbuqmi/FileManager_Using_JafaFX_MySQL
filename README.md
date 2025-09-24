\# JavaFX File Manager – README

\# Created by: Abdullah Albuqmi

\# KACST - Digital Health Institute 



\## 📋 Overview ############################################

This is a JavaFX-based File Manager application that supports user authentication, file operations (create, delete, rename, append), and logging activities into a MySQL database. It uses a login/sign-up interface with password hashing, and features a responsive UI for authenticated users.



---



\## 🛠️ Requirements #######################################



\### ✅ Software Requirements

\- Java Development Kit (JDK): OpenJDK 17.0.4 or later

\- MySQL Database Server: version 8.0 or later (8.0.42.0 is recommanded)

\- JavaFX SDK: version 21.0 or later (JavaFX 8 JARs must be included in the classpath, version 21.0.7 is recommanded)

\- IDE: Eclipse / IntelliJ / NetBeans (recommended for JavaFX support)



---



\## 📦 External Libraries (JARs Required) ####################



1\. JavaFX 8 JARs

&nbsp;  - e.g., `javafx-controls.jar`, `javafx-fxml.jar`, `javafx-base.jar`, etc.

2\. MySQL JDBC Connector

&nbsp;  - `mysql-connector-java-9.3.0.jar`

3\. HikariCP (Connection Pooling)

&nbsp;  - `HikariCP-5.1.0.jar`

4\. SLF4J (Logging)

&nbsp;  - `slf4j-api-2.0.9.jar`

&nbsp;  - `slf4j-simple-2.0.9.jar`



---



\## 📁 Database Setup ########################################



&nbsp;1. Create the Database

```sql

CREATE DATABASE users;



&nbsp;2. use the database

```sql

USE users;



&nbsp;3. Create the users table

```sql

CREATE TABLE users (

&nbsp;   id INT AUTO\_INCREMENT PRIMARY KEY,

&nbsp;   username VARCHAR(50) NOT NULL UNIQUE,

&nbsp;   password\_hash VARCHAR(100) NOT NULL,

&nbsp;   role ENUM('REGULAR', 'ADMIN') NOT NULL,

&nbsp;   email VARCHAR(100)

);



&nbsp;4. Create the file activities table

```sql

CREATE TABLE file\_activities (

&nbsp;   activity\_id INT NOT NULL AUTO\_INCREMENT PRIMARY KEY,

&nbsp;   user\_id INT NOT NULL,

&nbsp;   file\_name VARCHAR(255),

&nbsp;   file\_path VARCHAR(500),

&nbsp;   action VARCHAR(50),

&nbsp;   details TEXT,

&nbsp;   timestamp TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,

&nbsp;   INDEX (user\_id),

&nbsp;   INDEX (timestamp),

&nbsp;   FOREIGN KEY (user\_id) REFERENCES users(id)

);
```
\## 🔧 Configuration ########################################

1\. Database Configuration

Ensure the DatabaseConnection.java class is set to use the correct:



JDBC URL (e.g., jdbc:mysql://localhost:3306/users)



Database username/password

In src > database > DatabaseConnection.java ( Lines 9,10 and 11 )



\#############################################################

