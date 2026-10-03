@echo off
cd /d "%~dp0"
where java >nul 2>nul || (echo ERROR: Java not found. Install a JDK and add it to PATH.&pause&exit /b 1)
where javac >nul 2>nul || (echo ERROR: javac not found. Install a JDK, not only a JRE.&pause&exit /b 1)
where python >nul 2>nul || (echo ERROR: Python not found. Install Python 3.9+ and add it to PATH.&pause&exit /b 1)
echo Compiling Java UI...
javac MainNative.java NativeCpuClient.java SimulatorUINative.java
if errorlevel 1 (echo ERROR: Java compilation failed.&pause&exit /b 1)
echo Build complete. No C compiler, WSL, or Visual Studio is required.
pause
