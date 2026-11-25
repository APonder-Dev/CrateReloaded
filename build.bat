@echo off
rem Windows entry point for build.sh: builds every CrateReloaded build family into dist\.
rem   build.bat               all families (mc26, mc121, mc120)
rem   build.bat mc26 mc121    only the named families
rem Needs Git for Windows (bash) and Python on PATH. Set JAVA25_HOME / JAVA21_HOME / JAVA17_HOME if the
rem JDKs are not in "C:\Program Files\Java" or "C:\Program Files\Eclipse Adoptium".
setlocal
where bash >nul 2>nul
if errorlevel 1 (
  echo bash not found. Install Git for Windows or run build.sh from WSL.
  exit /b 1
)
bash "%~dp0build.sh" %*
exit /b %errorlevel%
