# ParkNova 🚗🚙

**ParkNova (Smart E-Parking System)** is a professional, enterprise-grade Parking Management System built with **Java (Swing/AWT)**. It features a modern, responsive UI with real-time analytics, automated check-in/check-out workflows, PDF receipt generation, and a fully standalone SQLite database.

## 🌟 Key Features

* **Modern Dashboard & UI:** Clean, intuitive interface with real-time statistics, smooth animations, and a responsive sidebar.
* **Streamlined Check-In / Check-Out:** Fast vehicle entry and exit processing with automated slot assignment.
* **Dynamic Slot Management:** Automatically tracks available, occupied, and maintenance slots for both Cars and Bikes.
* **Automated PDF Receipts:** Generates professional PDF entry slips and exit receipts using Apache PDFBox.
* **Data Persistence & Security:** Fully embedded SQLite database stored securely in the user's home directory.
* **Advanced Reporting & Analytics:** Visual charts (JFreeChart) and CSV export for revenue and capacity tracking.
* **Standalone Windows Installer:** One-click packaging to generate a fully portable Fat JAR or a professional `Setup.exe` using `jpackage`.

## 💻 Technology Stack

* **Language:** Java 17+
* **GUI Framework:** Java Swing & AWT
* **Database:** SQLite (JDBC)
* **PDF Generation:** Apache PDFBox (Custom Type1 Font implementation)
* **Charting:** JFreeChart
* **Logging:** SLF4J
* **Packaging:** Batch scripting (`.bat`), WiX Toolset, `jpackage`

## 📁 Project Structure

```text
SmartEParking/
├── src/                # Java source code (UI, DAO, Utils, Models)
├── lib/                # Dependencies (SQLite, SLF4J)
├── resources/          # Icons, logos, and UI assets
├── build.bat           # Script to compile and create a "Fat JAR"
├── create_setup.bat    # Script to create a Windows Setup.exe
├── create_installer.bat# Script to create a standalone portable app
└── .gitignore          # Git ignore configuration
```

## 🚀 Installation & Setup

You do not need an IDE to build or run this project. It is fully self-contained!

### 1. Build the Application
Double-click `build.bat` or run it from the command line:
```cmd
.\build.bat
```
This will compile the Java source code, extract all library dependencies, and generate a self-contained `SmartEPark.jar`.

### 2. Create the Installer
To generate a professional Windows installer (requires [WiX Toolset v3.11](https://wixtoolset.org/releases/)):
```cmd
.\create_setup.bat
```
This will create a `SmartEPark_Installer` folder containing your `Setup.exe`.

*Note: If you don't have WiX Toolset installed, you can run `create_installer.bat` instead to generate a portable App Image folder.*

## 🔒 Default Credentials
* **Username:** `admin`
* **Password:** `admin123`

---
*Developed by Muhammad Rehan*
