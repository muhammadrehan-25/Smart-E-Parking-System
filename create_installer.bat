@echo off
echo === Smart ePark JPackage Installer Script ===

echo [1] Checking for JDK jpackage...
where jpackage >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: jpackage tool not found! Make sure you are using JDK 14 or higher and it is added to your system PATH.
    pause
    exit /b
)

echo [2] Preparing staging directory...
if exist staging rmdir /s /q staging
mkdir staging
mkdir staging\lib

echo [3] Copying files...
copy SmartEPark.jar staging\ >nul

echo [4] Running jpackage to create a standalone App Image...
if exist SmartEPark_App rmdir /s /q SmartEPark_App

jpackage ^
  --type app-image ^
  --name "ParkNova" ^
  --input staging ^
  --main-jar SmartEPark.jar ^
  --main-class main.Main ^
  --dest SmartEPark_App ^
  --java-options "-Xmx512m" ^
  --icon resources\logo.ico

if %errorlevel% equ 0 (
    echo.
    echo === SUCCESS! ===
    echo Your complete, standalone application is ready in the "SmartEPark_App\SmartEPark" folder.
    echo Aap is folder ko kisi bhi computer par copy kar sakte hain, usmein pehle se Java JRE maujood hai!
    echo Agar aapko .exe installer chahiye jaise setup.exe, toh aapke computer par 'Wix Toolset' install hona zaroori hai.
) else (
    echo === PACKAGING FAILED ===
)

pause
