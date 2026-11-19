param(
    [string]$CredentialsDirectory = (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Neon-WS-Credentials'),
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
function Wait-Application([string]$Url, [int]$ExpectedStatus) {
    $deadline = (Get-Date).AddSeconds(180)
    Write-Host "Attente du service : $Url"
    do {
        $status = 0
        try { $status = [int](Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop).StatusCode }
        catch { if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode } }
        if ($status -eq $ExpectedStatus) { return }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Le service ne repond pas : $Url. Consulter les journaux dans Docker Desktop."
}
foreach ($module in @('camions', 'chargements', 'bons', 'transports')) {
    $file = Join-Path $CredentialsDirectory ($module + '_app.env')
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        throw "Fichier prive introuvable : $file. Utiliser -CredentialsDirectory avec le dossier de connexion."
    }
    [Environment]::SetEnvironmentVariable(('WS_' + $module.ToUpperInvariant() + '_ENV_FILE'),
        (Resolve-Path -LiteralPath $file).Path, 'Process')
}
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'auth/demarrer.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Keycloak ne peut pas demarrer.' }
    if (-not $SkipBuild) {
        & docker compose --env-file .secrets/auth.env -f compose.auth.yaml -f compose.local.yaml stop camions chargements bons transports
        if ($LASTEXITCODE -ne 0) { throw 'Les applications existantes ne peuvent pas etre arretees pour la construction.' }
        foreach ($service in @('flotte', 'chargements', 'bons', 'transports')) {
            & mvn.cmd -B -f ("services/$service/pom.xml") package -DskipTests
            if ($LASTEXITCODE -ne 0) { throw "La construction du service $service a echoue." }
        }
    }
    foreach ($jar in @('services/flotte/target/flotte-0.1.0-SNAPSHOT.jar',
            'services/chargements/target/chargements-0.0.1-SNAPSHOT.jar',
            'services/bons/target/bons-0.0.1-SNAPSHOT.jar',
            'services/transports/target/transports-0.0.1-SNAPSHOT.jar')) {
        if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw "JAR absent : $jar. Relancer sans -SkipBuild." }
    }
    & docker compose --env-file .secrets/auth.env -f compose.auth.yaml -f compose.local.yaml up -d
    if ($LASTEXITCODE -ne 0) { throw 'Les backends ne peuvent pas demarrer.' }
    Wait-Application 'http://127.0.0.1:8090/realms/transport-ws/.well-known/openid-configuration' 200
    foreach ($service in @(@{Port=8081; Route='camions'}, @{Port=8082; Route='chargements'},
            @{Port=8083; Route='bons'}, @{Port=8084; Route='transports'})) {
        Wait-Application ("http://127.0.0.1:$($service.Port)/api/v1/$($service.Route)") 401
    }
    Push-Location (Join-Path $projectRoot 'frontend')
    try {
        if (-not (Test-Path -LiteralPath 'node_modules')) {
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw "L'installation du frontend a echoue." }
        }
        Write-Host 'Frontend : http://localhost:5173'
        & npm.cmd run dev
    } finally { Pop-Location }
} finally { Pop-Location }
