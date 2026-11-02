@echo off
rem keep-awake wrapper: only bypasses execution policy for this single script
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0keep-awake.ps1" %*
exit /b %ERRORLEVEL%
