@echo off

:: Always ensure we are running from the project root directory
cd /d "%~dp0"

:: Forward to the actual development script
call scripts\dev.cmd
