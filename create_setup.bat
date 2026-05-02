@echo off
echo === Smart ePark SETUP Installer Script ===

echo [1] Checking for JDK jpackage...
where jpackage >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: jpackage tool not found!
    pause
    exit /b
)

echo [2] Preparing staging directory...
if exist staging rmdir /s /q staging
mkdir staging
mkdir staging\lib

echo [3] Copying files...
copy SmartEPark.jar staging\ >nul

echo [4] Running jpackage to create SETUP.EXE...
echo (NOTE: This requires WiX Toolset installed on your computer!)
if exist SmartEPark_Installer rmdir /s /q SmartEPark_Installer
mkdir SmartEPark_Installer

jpackage ^
  --type exe ^
  --name "ParkNova" ^
  --input staging ^
  --main-jar SmartEPark.jar ^
  --main-class main.Main ^
  --dest SmartEPark_Installer ^
  --java-options "-Xmx512m" ^
  --icon resources\logo.ico ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --app-version 1.0.0 ^
  --description "SmartEPark Management System"

if %errorlevel% equ 0 (
    echo.
    echo === SUCCESS! ===
    echo Your proper installer Setup.exe is ready in the "SmartEPark_Installer" folder!
) else (
    echo.
    echo === PACKAGING FAILED ===
    echo You probably don't have WiX Toolset installed.
    echo JPackage requires WiX to build .exe installers.
    echo Please download it from: https://wixtoolset.org/releases/ (Version 3.11 is recommended)
)

pause
