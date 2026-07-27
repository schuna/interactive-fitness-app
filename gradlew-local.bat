@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "GRADLE_USER_HOME=%PROJECT_DIR%.gradle-user"
set "JDK_HOME_FILE=%PROJECT_DIR%.tools\jdk17-home.txt"

if exist "%JDK_HOME_FILE%" set /p JAVA_HOME=<"%JDK_HOME_FILE%"
if not defined JAVA_HOME set "JAVA_HOME=%PROJECT_DIR%.tools\jdk17\jdk-17.0.19+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JDK 17 is missing. Run: powershell -ExecutionPolicy Bypass -File scripts\bootstrap-jdk.ps1
  exit /b 1
)

call "%PROJECT_DIR%gradlew.bat" %*
exit /b %ERRORLEVEL%
