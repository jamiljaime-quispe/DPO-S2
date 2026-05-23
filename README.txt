# Parking System — DPO Phase 2

A small Java/Swing parking management application built on top of a MySQL database. It lets administrators manage parking spaces and reservations, lets regular users sign up, book spaces, drive in and out, and lets everyone consult the current parking status and a live occupancy chart for the last hour.

## Group members
- William Alberto Avendaño Acevedo
- Mathias Felipe Jon Anderson
- Jamil Jaime Quispe Espinoza
- Lucas Vilamitjana Schroeder
- Romà Sardá Casellas

## Requirements
- **JDK 25** (the project was compiled and tested with this version).
- **XAMPP** with both **MySQL Database** and **Apache Web Server** running, so that phpMyAdmin is reachable at `http://localhost/phpmyadmin`.
- **IntelliJ IDEA** to open and run the project.
- The MySQL JDBC driver, which is already included in the `lib/` folder of the project (`mysql-connector-j-8.3.0.jar`), so there is nothing extra to download.

## Database setup
1. Open the **XAMPP Control Panel** and click *Start* on **Apache** and on **MySQL**.
2. Go to `http://localhost/phpmyadmin` in the browser.
3. On the left panel click **New**, type `parking_db` as the database name, and click **Create**. Leave the empty database selected.
4. Click the **Import** tab at the top, choose the file **`parking_db.sql`** from the project root, and click **Go** at the bottom of the page. This creates all five tables (`user`, `vehicle`, `parking_space`, `reservation`, `occupancy_log`), all the foreign keys, and inserts the seed data we use for testing.

That is the only SQL file the grader needs to import. The other `.sql` files in the project root are kept only as a history of the previous database state and are not needed.

## Configuration (`config.json`)
The application reads `config.json` from the project root at startup. The values that ship with the project are:

```json
{
  "dbIP": "localhost",
  "dbPort": 3306,
  "dbName": "parking_db",
  "dbUser": "root",
  "dbPassword": "",
  "adminPassword": "admin",
  "simulatedVehicleDelay": 5
}
```

## How to run the project
1. Open the code folder in **IntelliJ IDEA** 
2. Make sure the project SDK is set to **JDK 25** 
3. Make sure every JAR inside the `lib/` folder
4. Open `src/Main.java` and run it

The application should now show the login window.

## Default credentials
Two accounts are already seeded in the database so the grader does not have to sign up to test things:

1. Admin account -> Username: admin    Password: admin
2. User account -> Username: test      Password: Testtest1

The admin password is also the one declared in `config.json`, as the spec requires. It is also possible to sign up a brand new user from the sign-up screen and log in with that one instead.

