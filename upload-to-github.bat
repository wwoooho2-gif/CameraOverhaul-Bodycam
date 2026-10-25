@echo off
cd /d "%~dp0"

where git >nul 2>nul
if errorlevel 1 (
  echo Git is not installed. Install it from https://git-scm.com then run this again.
  pause
  exit /b 1
)

if not exist "src" (
  echo Run this file from inside the CameraOverhaul-Bodycam folder, next to the src folder.
  pause
  exit /b 1
)

git init

git config user.name >nul 2>nul || git config user.name "T2ws"
git config user.email >nul 2>nul || git config user.email "wwoooho2-gif@users.noreply.github.com"

git add .
git commit -m "First release: CameraOverhaul Bodycam Edition by T2ws"
git branch -M main

git remote remove origin >nul 2>nul
git remote add origin https://github.com/wwoooho2-gif/CameraOverhaul-Bodycam.git

git push -u origin main
if errorlevel 1 (
  echo.
  echo Push failed. Make sure the repo CameraOverhaul-Bodycam exists on GitHub and is empty, then run this again.
) else (
  echo.
  echo Done! https://github.com/wwoooho2-gif/CameraOverhaul-Bodycam
)
pause
