@echo off
cd /d "%~dp0"
start "PIC16F72 Python CPU Backend" /min cmd /c "python pic_cpu_backend.py"
timeout /t 1 /nobreak >nul
java MainNative
