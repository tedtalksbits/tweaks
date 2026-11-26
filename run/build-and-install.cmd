@echo off
rem Stops the local test server, builds and tests Tweaks, installs the plugin on the server as plugins\Tweaks.jar,
rem then offers to start the server. Options: see run\DevInstall.java (for example: build-and-install.cmd --start).
rem Settings: TWEAKS_SERVER_DIR, TWEAKS_GRADLE_ARGS, JAVA_HOME.
setlocal
cd /d "%~dp0.."
set "JAVA_EXE=java"
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    for /d %%D in ("%~dp0..\..\blueprints-mc\.tools\jdk\*") do if exist "%%D\bin\java.exe" set "JAVA_EXE=%%D\bin\java.exe"
)
"%JAVA_EXE%" "%~dp0DevInstall.java" %*
pause
