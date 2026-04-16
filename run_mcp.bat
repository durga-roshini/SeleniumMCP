@echo off
REM run_mcp.bat - Build (if mvn present) and start the MCP Selenium server jar
REM Usage: run_mcp.bat [headless]

REM Change to script directory (project root)
cd /d "%~dp0"

necho Project root: %CD%

nREM Try to build with mvn if available
where mvn >nul 2>&1
if %ERRORLEVEL%==0 (
    echo Building project with Maven...
    mvn clean package
) else (
    echo Maven not found on PATH. If you haven't built the project yet, please install Maven or build from IntelliJ.
)

nset JAR=target\mcp-server.jar
nif not exist "%JAR%" (
    echo ERROR: JAR not found at %JAR%
    echo Please run "mvn clean package" or build via IntelliJ and try again.
    pause
    exit /b 1
)

nREM Decide headless mode if first arg equals headless (case-insensitive)
set HEADLESS_ARG=
set MCP_PROP=
if /I "%1"=="headless" (
    set HEADLESS_ARG=--headless
    set MCP_PROP=-Dmcp.headless=true
)

necho Starting MCP server (jar=%JAR%)
java %MCP_PROP% -jar "%JAR%" %HEADLESS_ARG%
pause

