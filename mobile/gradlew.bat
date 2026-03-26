@echo off
set SCRIPT_DIR=%~dp0
gradle -p "%SCRIPT_DIR%" %*
