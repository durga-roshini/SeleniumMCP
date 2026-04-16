# Start-server script: builds and runs the MCP server jar if possible.
# Usage: .\start-server.ps1 [-Headless]
param(
    [switch]$Headless
)

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $projectRoot

Write-Host "Project root: $projectRoot"

# Try to build with mvn if available
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn) {
    Write-Host "Building project with Maven..."
    mvn clean package
} else {
    Write-Host "Maven not found in PATH. If you haven't built the project yet, please install Maven or build from IntelliJ."
}

$jar = Join-Path $projectRoot "target\mcp-server.jar"
if (-Not (Test-Path $jar)) {
    Write-Host "JAR not found at $jar"
    exit 1
}

$headlessArg = ''
if ($Headless) { $headlessArg = '--headless' }

Write-Host "Starting MCP server (jar=$jar)"
java -Dmcp.headless=$($Headless.IsPresent.ToString()) -jar $jar $headlessArg

