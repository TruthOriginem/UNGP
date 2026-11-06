@echo off
rem Double-click to run the interactive icon tinting wizard.
rem Requires Python 3 with Pillow (pip install pillow).
set "PY="
where py >nul 2>nul && set "PY=py -3"
if not defined PY (
  where python >nul 2>nul && set "PY=python"
)
if not defined PY (
  echo Python 3 was not found in PATH.
  echo Install Python 3 from https://www.python.org/downloads/ and try again.
  pause
  exit /b 1
)
%PY% "%~dp0icon_tint.py" --interactive
if errorlevel 1 pause
