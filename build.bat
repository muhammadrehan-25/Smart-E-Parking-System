@echo off
echo === Smart ePark Build Script ===
if not exist out\classes mkdir out\classes

if exist "lib\sqlite-jdbc-3.45.1.0 (1).jar" (
    ren "lib\sqlite-jdbc-3.45.1.0 (1).jar" "sqlite-jdbc.jar"
)

set "CP=lib\sqlite-jdbc.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar"

echo Compiling...
javac -cp "%CP%" -d out\classes src\dao\*.java src\exceptions\*.java src\interfaces\*.java src\main\*.java src\model\*.java src\ui\*.java src\util\*.java

if %errorlevel% equ 0 (
    echo Build successful!
    echo Creating FAT JAR including dependencies...
    xcopy /E /Y resources out\classes\resources\ >nul 2>&1
    
    cd out\classes
    jar xf ..\..\lib\sqlite-jdbc.jar
    jar xf ..\..\lib\slf4j-api-2.0.9.jar
    jar xf ..\..\lib\slf4j-simple-2.0.9.jar
    cd ..\..
    
    echo Main-Class: main.Main > out\MANIFEST.MF
    jar cfm SmartEPark.jar out\MANIFEST.MF -C out\classes .
    echo.
    echo === BUILD SUCCESSFUL ===
    echo Run: java -jar SmartEPark.jar
) else (
    echo === BUILD FAILED ===
)
