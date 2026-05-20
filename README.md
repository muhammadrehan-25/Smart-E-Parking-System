<div align="center">

<h1>🚗 ParkNova — Smart E-Parking System</h1>

<p>An enterprise-grade, fully standalone Parking Management System built with Java Swing/AWT</p>

[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![SQLite](https://img.shields.io/badge/SQLite-Embedded-blue?style=for-the-badge&logo=sqlite)](https://www.sqlite.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows-0078d4?style=for-the-badge&logo=windows)](https://www.microsoft.com/windows)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge)]()
[![Version](https://img.shields.io/badge/Version-1.0.0-purple?style=for-the-badge)]()

<br/>

> **ParkNova** streamlines vehicle entry, exit, slot tracking, PDF receipts, and analytics — all in one self-contained desktop application. No internet required. No server setup needed.

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Screenshots](#-screenshots)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Prerequisites](#-prerequisites)
- [Project Structure](#-project-structure)
- [Installation & Setup](#-installation--setup)
- [Default Credentials](#-default-credentials)
- [Database Schema](#-database-schema)
- [Data Storage Location](#-data-storage-location)
- [Known Issues & Limitations](#-known-issues--limitations)
- [Roadmap](#-roadmap)
- [Changelog](#-changelog)
- [Contributing](#-contributing)
- [License](#-license)
- [Author](#-author)

---

## 🌐 Overview

ParkNova is a professional, enterprise-grade Smart Parking Management System designed for real-world parking facilities. It handles the full lifecycle of vehicle parking — from entry to exit — with automated slot assignment, PDF receipt generation, and visual analytics. Everything runs locally with zero external dependencies at runtime.

---

## 📸 Screenshots

> ℹ️ Screenshots will be added in the next release. Run the application to see the live UI.

| Dashboard | Check-In | Check-Out | Analytics |
|-----------|----------|-----------|-----------|
| *(Coming Soon)* | *(Coming Soon)* | *(Coming Soon)* | *(Coming Soon)* |

---

## 🌟 Key Features

### 🖥️ Modern Dashboard & UI
A clean, intuitive interface with real-time statistics, smooth animations, and a responsive sidebar. Built entirely with Java Swing/AWT — no external UI frameworks needed.

### ⚡ Streamlined Check-In / Check-Out
Fast vehicle entry and exit processing with automated slot assignment. Supports both Cars and Bikes with separate slot pools.

### 🅿️ Dynamic Slot Management
Automatically tracks available, occupied, and maintenance slots in real time. Slot status updates instantly on every check-in and check-out action.

### 🧾 Automated PDF Receipts
Generates professional PDF entry slips and exit receipts using **Apache PDFBox** with a custom Type1 Font implementation. Receipts include vehicle details, timestamps, duration, and charges.

### 🔒 Data Persistence & Security
Fully embedded **SQLite** database stored securely in the user's home directory. No external database server required. Data persists across all sessions automatically.

### 📊 Advanced Reporting & Analytics
Visual bar and pie charts powered by **JFreeChart**, plus one-click **CSV export** for revenue and capacity tracking over custom date ranges.

### 📦 Standalone Windows Installer
One-click packaging to generate a fully portable Fat JAR or a professional `Setup.exe` using `jpackage` + WiX Toolset. Share the installer with anyone — no Java installation required on the target machine.

---

## 💻 Technology Stack

| Category | Technology | Purpose |
|----------|-----------|---------|
| **Language** | Java 17+ | Core application logic |
| **GUI Framework** | Java Swing & AWT | User interface |
| **Database** | SQLite (via JDBC) | Embedded data storage |
| **PDF Generation** | Apache PDFBox | Entry slips & exit receipts |
| **Charting** | JFreeChart | Analytics & visual reports |
| **Logging** | SLF4J | Application logging |
| **Packaging** | `jpackage` + WiX Toolset | Windows installer creation |
| **Build Scripts** | Batch Scripting (`.bat`) | Compilation & packaging automation |

---

## ✅ Prerequisites

Before building or running ParkNova, make sure you have the following installed:

| Requirement | Version | Required? | Notes |
|-------------|---------|-----------|-------|
| **JDK (Java Development Kit)** | 17 or higher | ✅ Yes | [Download from Oracle](https://www.oracle.com/java/technologies/downloads/) |
| **Windows OS** | Windows 10/11 | ✅ Yes | `.bat` scripts are Windows-only |
| **WiX Toolset** | v3.11 | ⚠️ Optional | Only needed for `Setup.exe` — [Download here](https://wixtoolset.org/releases/) |

> 💡 **No IDE required.** The project is fully self-contained and builds from the command line using `.bat` scripts.

> 💡 **JRE is NOT enough.** You need the full JDK (which includes `javac` and `jpackage`) to build the project.

---

## 📁 Project Structure

```text
SmartEParking/
│
├── src/                        # Java source code
│   ├── ui/                     # All Swing UI panels (Dashboard, CheckIn, CheckOut, Reports)
│   ├── dao/                    # Data Access Objects (database queries & operations)
│   ├── models/                 # Data models (Vehicle, Slot, Transaction, User)
│   └── utils/                  # Helper classes (PDF generator, CSV exporter, chart builder)
│
├── lib/                        # Third-party dependency JARs
│   ├── sqlite-jdbc-*.jar       # SQLite JDBC driver
│   ├── pdfbox-*.jar            # Apache PDFBox for PDF generation
│   ├── jfreechart-*.jar        # JFreeChart for analytics charts
│   └── slf4j-*.jar             # SLF4J logging facade
│
├── resources/                  # Static assets
│   ├── icons/                  # Application icons (.png, .ico)
│   ├── logo/                   # ParkNova branding assets
│   └── fonts/                  # Custom fonts for PDF receipts
│
├── build.bat                   # Compiles source + creates Fat JAR (SmartEPark.jar)
├── create_setup.bat            # Creates Setup.exe using jpackage + WiX Toolset
├── create_installer.bat        # Creates a portable App Image folder (no WiX needed)
└── .gitignore                  # Git ignore configuration
```

---

## 🚀 Installation & Setup

### Step 1 — Build the Application

Double-click `build.bat` or run it from the command line:

```cmd
.\build.bat
```

This will:
1. Compile all Java source files in `src/`
2. Extract all library JARs from `lib/`
3. Package everything into a single self-contained **`SmartEPark.jar`**

### Step 2A — Run the Fat JAR (Quick Start)

After building, run the JAR directly:

```cmd
java -jar SmartEPark.jar
```

### Step 2B — Create a Windows Installer (Optional)

To generate a professional Windows installer (requires [WiX Toolset v3.11](https://wixtoolset.org/releases/)):

```cmd
.\create_setup.bat
```

This will create a `SmartEPark_Installer/` folder containing your `Setup.exe`. Share this with anyone — they don't need Java installed.

### Step 2C — Create a Portable App (No WiX Needed)

If you don't have WiX Toolset installed:

```cmd
.\create_installer.bat
```

This generates a portable `SmartEPark-App/` folder that runs on any compatible Windows machine.

---

## 🔒 Default Credentials

| Field | Value |
|-------|-------|
| **Username** | `admin` |
| **Password** | `admin123` |

> ⚠️ **Security Notice:** Change the default admin password immediately after first login, especially if deploying in a shared or production environment.

---

## 🗄️ Database Schema

ParkNova uses an embedded SQLite database. The main tables are:

| Table | Description |
|-------|-------------|
| `users` | Admin credentials and roles |
| `slots` | Parking slot records (id, type, status) |
| `vehicles` | Vehicle details (plate, owner, type) |
| `transactions` | Check-in/out records with timestamps and charges |

The database is auto-created on first launch. No manual setup is required.

---

## 💾 Data Storage Location

The SQLite database file is stored automatically in your **user home directory**:

```
Windows:   C:\Users\<YourUsername>\ParkNova\parknova.db
```

> You can back up your data by copying this file. To reset the application to a fresh state, delete the file and restart ParkNova.

---

## ⚠️ Known Issues & Limitations

- **Windows Only** — The `.bat` build scripts are Windows-specific. Linux/macOS support is not available yet.
- **Single Admin User** — Currently only one admin account is supported. Multi-user roles are planned.
- **No Network Access** — ParkNova is a fully offline application. Cloud sync or remote access is not supported in v1.0.0.
- **PDF Font Rendering** — On some systems, custom Type1 fonts in PDFs may render slightly differently depending on the PDF viewer.
- **Screen Resolution** — The UI is optimized for 1366×768 and above. Very low-resolution displays may see layout issues.

---

## 🛣️ Roadmap

Planned features for future releases:

- [ ] **v1.1.0** — Multi-user support with role-based access (Admin, Operator, Viewer)
- [ ] **v1.2.0** — Monthly subscription parking with automated billing
- [ ] **v1.3.0** — Multi-floor / multi-zone parking support
- [ ] **v1.4.0** — QR code-based vehicle entry and exit
- [ ] **v2.0.0** — Online dashboard with cloud database sync
- [ ] **v2.1.0** — Mobile app companion for real-time slot monitoring
- [ ] **Future** — License plate recognition (camera integration)

---

## 📝 Changelog

### v1.0.0 — Initial Release
- Modern Java Swing dashboard with real-time statistics
- Automated check-in / check-out with slot assignment
- PDF receipt generation using Apache PDFBox
- Visual analytics with JFreeChart (bar & pie charts)
- CSV export for revenue and occupancy reports
- Embedded SQLite database with auto-setup
- Fat JAR packaging + Windows installer scripts

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. Fork the repository
2. Create a new branch:
   ```cmd
   git checkout -b feature/your-feature-name
   ```
3. Make your changes and commit:
   ```cmd
   git commit -m "Add: description of your change"
   ```
4. Push the branch and open a **Pull Request**

Please keep code style consistent with the existing codebase and add comments for any non-obvious logic.

> Found a bug? Open an [Issue](../../issues) with steps to reproduce.

---

## 📄 License

This project is licensed under the **MIT License**.
You are free to use, modify, and distribute this software with attribution.

See the [LICENSE](LICENSE) file for full details.

---

## 👨‍💻 Author

**Muhammad Rehan**

[![GitHub](https://img.shields.io/badge/GitHub-Profile-black?style=flat-square&logo=github)](https://github.com/)
[![Email](https://img.shields.io/badge/Email-Contact-red?style=flat-square&logo=gmail)](mailto:your-email@example.com)

> *If this project helped you, consider giving it a ⭐ on GitHub!*

---

<div align="center">
  <sub>Built with ❤️ using Java Swing | ParkNova v1.0.0</sub>
</div>
