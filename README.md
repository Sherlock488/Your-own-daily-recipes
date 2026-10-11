# Daily Menu

Daily Menu is a JavaFX desktop application for planning meals, exploring recipe suggestions, and keeping meal plans organized by account.

## Features

- Create a local account or sign in to an existing account.
- Optionally remember the signed-in account on the current device.
- View a home page with the current date, time, greeting, daily plan, and saved meals.
- Use the calendar to plan meals for a selected date.
- Use Quick Start to choose meal preferences and receive a recipe suggestion.
- Browse recipe suggestions in the Kitchen page using food and meal preference tags.
- Review recipes and add selected meals to a plan.
- Manage profile details and application preferences.
- Store account and meal-plan data locally on the computer.

## Technology

- Java 17
- JavaFX 21.0.2
- Maven

## Run from the Source Project on Windows

### Requirements

- JDK 17
- Maven
- The project files, including `pom.xml`

Open PowerShell in the project directory (the directory containing `pom.xml`) and run:

```powershell
mvn javafx:run
```

If PowerShell reports that `mvn` is not recognized, Maven is not on the system `PATH`. You can either add Maven's `bin` directory to `PATH` or use the full path to `mvn.cmd`. For the Maven installation used in this setup:

```powershell
& "D:\homework\Tools\apache-maven-3.10.0\bin\mvn.cmd" -f "D:\homework\DailyMenu\pom.xml" javafx:run
```

## Build a Windows App Package

From the project directory, run:

```powershell
mvn clean javafx:jlink
```

If `mvn` is not on `PATH`, use the full Maven path instead:

```powershell
& "D:\homework\Tools\apache-maven-3.10.0\bin\mvn.cmd" -f "D:\homework\DailyMenu\pom.xml" clean javafx:jlink
```

After Maven reports `BUILD SUCCESS`, the package ZIP is created at:

```text
target\DailyMenu.zip
```

## Start the Packaged App

1. Extract the **entire** `DailyMenu.zip` archive to a folder, such as `D:\Apps\DailyMenu`.
2. Keep the extracted folder structure intact. Do not copy only the `bin` folder or an individual runtime file.
3. Open the extracted app folder, then open `bin`.
4. Double-click `DailyMenu.bat` to start the app.

The packaged image includes its Java runtime, so Maven and a separately installed Java runtime are not needed to launch this package. The package is a portable Windows app image, not a setup installer. To share it, distribute the ZIP or the complete extracted app folder.

## Local Data

Daily Menu stores account and meal-plan information locally on the computer. It does not provide cloud synchronization in this version. Keep the application data on the same computer if you want to continue using the same local accounts and plans.

## Troubleshooting

- **`mvn` is not recognized:** Use the full path to `mvn.cmd` shown above, or add Maven's `bin` directory to the Windows `PATH`.
- **The app package does not start:** Confirm that the entire ZIP was extracted and run `bin\DailyMenu.bat` from the extracted package. Do not launch `java.exe` directly.
- **Maven build fails:** Check that JDK 17 is installed and that the PowerShell terminal is in the directory containing `pom.xml`.

