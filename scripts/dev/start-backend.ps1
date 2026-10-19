# one terminal for the whole backend: mysql (if not running), kafka (docker, if available), the 5 services as jars.
# logs go to logs\<service>.out.log; Ctrl+C stops everything this script started.
# usage (repo root):  .\scripts\dev\start-backend.ps1            -Build = rebuild the jars first
#                     .\scripts\dev\start-backend.ps1 -Only core,gateway   (lighter: just these services)
param(
    [switch]$Build,
    [string[]]$Only = @('core', 'notification', 'payment', 'ai', 'gateway'),
    [string]$MysqlHome = $(if ($env:GRIDPULSE_MYSQL_HOME) { $env:GRIDPULSE_MYSQL_HOME } else { "$env:USERPROFILE\devtools\mysql" }),
    [string]$MysqlData = $(if ($env:GRIDPULSE_MYSQL_DATA) { $env:GRIDPULSE_MYSQL_DATA } else { "$env:USERPROFILE\devtools\mysql-data\data" })
)
$ErrorActionPreference = 'Stop'
$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root
New-Item -ItemType Directory -Force logs | Out-Null
$started = @()

function Test-Port([int]$port) {
    try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', $port); $c.Close(); $true } catch { $false }
}

function Wait-Healthy([string]$name, [int]$port) {
    $deadline = (Get-Date).AddMinutes(4)
    while ((Get-Date) -lt $deadline) {
        try {
            $h = Invoke-RestMethod "http://localhost:$port/actuator/health" -TimeoutSec 3
            if ($h.status -eq 'UP') { Write-Host "  $name is up on :$port" -ForegroundColor Green; return }
        } catch { }
        Start-Sleep 3
    }
    Write-Host "  $name did not come up - see logs\$name.out.log" -ForegroundColor Red
}

# 1. mysql
if (Test-Port 3306) {
    Write-Host "mysql already running on :3306"
} elseif (Test-Path "$MysqlHome\bin\mysqld.exe") {
    Write-Host "starting mysql..."
    $null = Start-Process "$MysqlHome\bin\mysqld.exe" -WindowStyle Hidden -PassThru `
        -ArgumentList "--basedir=`"$MysqlHome`"", "--datadir=`"$MysqlData`"", '--port=3306' `
        -RedirectStandardError logs\mysql.err.log -RedirectStandardOutput logs\mysql.out.log
    while (-not (Test-Port 3306)) { Start-Sleep 1 }
    Write-Host "  mysql is up on :3306" -ForegroundColor Green
} else {
    throw "mysql not found at $MysqlHome (set GRIDPULSE_MYSQL_HOME) and nothing listens on :3306"
}

# 2. kafka (optional: without it the site works, emails / refunds / labels wait)
if (Test-Port 9092) {
    Write-Host "kafka already running on :9092"
} else {
    try { docker info *> $null; if ($LASTEXITCODE -ne 0) { throw 'no docker' }
        Write-Host "starting kafka (docker)..."; docker compose up -d kafka | Out-Null
    } catch { Write-Host "docker is not running: no kafka (emails, refunds and AI labels will wait)" -ForegroundColor Yellow }
}

# 3. jars
$services = [ordered]@{
    core         = @{ module = 'core-service';         port = 8082; env = @{ CORS_ENABLED = 'false'; CACHE_TYPE = 'none' } }
    notification = @{ module = 'notification-service'; port = 8084; env = @{ MAIL_TO_FILE = 'true'; GRIDPULSE_MAIL_FILEDIR = 'notification-service/target/test-mails' } }
    payment      = @{ module = 'payment-service';      port = 8085; env = @{ } }
    ai           = @{ module = 'ai-service';           port = 8086; env = @{ CACHE_TYPE = 'none' } }
    gateway      = @{ module = 'gateway';              port = 8080; env = @{ } }
}
$missing = $services.Values | Where-Object { -not (Test-Path "$($_.module)\target\$($_.module)-0.0.1-SNAPSHOT.jar") }
if ($Build -or $missing) {
    Write-Host "building the jars (1-3 min)..."
    & .\mvnw.cmd -q clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'maven build failed' }
}

# 4. services, in order, each waits until healthy
try {
    foreach ($name in $services.Keys) {
        if ($Only -notcontains $name) { continue }
        $s = $services[$name]
        if (Test-Port $s.port) { Write-Host "${name}: port $($s.port) already in use, skipped" -ForegroundColor Yellow; continue }
        Write-Host "starting $name..."
        foreach ($k in $s.env.Keys) { Set-Item "env:$k" $s.env[$k] }
        $started += Start-Process java -WindowStyle Hidden -PassThru `
            -ArgumentList '-Xmx384m', '-XX:+UseSerialGC', '-jar', "$($s.module)\target\$($s.module)-0.0.1-SNAPSHOT.jar" `
            -RedirectStandardOutput "logs\$name.out.log" -RedirectStandardError "logs\$name.err.log"
        foreach ($k in $s.env.Keys) { Remove-Item "env:$k" }
        Wait-Healthy $name $s.port
    }
    Write-Host "`nbackend ready: gateway http://localhost:8080   (frontend: cd frontend; npm start)" -ForegroundColor Cyan
    Write-Host "Ctrl+C stops everything this script started.`n"
    while ($true) { Start-Sleep 5 }
} finally {
    Write-Host "`nstopping..."
    $started | Where-Object { -not $_.HasExited } | ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }
    Write-Host "stopped (mysql and kafka keep running for next time)"
}
