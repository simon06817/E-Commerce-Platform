@echo off
setlocal

set "DEPS=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies"
set "NODE_BIN=%DEPS%\node\bin"
set "PNPM_BIN=%DEPS%\bin\fallback"

if not exist "%NODE_BIN%\node.exe" (
    echo Node.js runtime was not found:
    echo %NODE_BIN%\node.exe
    exit /b 1
)

if not exist "%PNPM_BIN%\pnpm.cmd" (
    echo pnpm was not found:
    echo %PNPM_BIN%\pnpm.cmd
    exit /b 1
)

set "PATH=%NODE_BIN%;%PNPM_BIN%;%PATH%"
cd /d "%~dp0"

echo Starting frontend at http://localhost:5173
call pnpm run dev
