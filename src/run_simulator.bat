
@echo off
cd /d "%~dp0"

start /min "PIC16F72 Logger" cmd /c "python pic_logger.py"
ping 127.0.0.1 -n 2 -w 1000 >nul

start /min "PIC16F72 Python CPU Backend" cmd /c "python pic_cpu_backend.py"
ping 127.0.0.1 -n 2 -w 1000 >nul

java MainNative
