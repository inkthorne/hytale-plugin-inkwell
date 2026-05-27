@echo off
echo Building inkwell plugin...
call ./gradlew.bat build
if %ERRORLEVEL% EQU 0 (
    echo Build successful! JAR located at: build\libs\inkwell.jar
) else (
    echo Build failed!
)
