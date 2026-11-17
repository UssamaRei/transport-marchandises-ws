param(
    [string]$CredentialsDirectory = (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Neon-WS-Credentials'),
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
foreach ($module in @('camions', 'chargements')) {
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
        & mvn.cmd -B -f services/flotte/pom.xml package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'La construction Camions a echoue.' }
        Push-Location (Join-Path $projectRoot 'services/chargements')
        try { & .\mvnw.cmd -B package -DskipTests }
        finally { Pop-Location }
        if ($LASTEXITCODE -ne 0) { throw 'La construction Chargements a echoue.' }
    }
    foreach ($jar in @('services/flotte/target/flotte-0.1.0-SNAPSHOT.jar',
            'services/chargements/target/chargements-0.0.1-SNAPSHOT.jar')) {
        if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw "JAR absent : $jar. Relancer sans -SkipBuild." }
    }
    & docker compose --env-file .secrets/auth.env -f compose.auth.yaml -f compose.local.yaml up -d
    if ($LASTEXITCODE -ne 0) { throw 'Les backends ne peuvent pas demarrer.' }
    Push-Location (Join-Path $projectRoot 'frontend')
    try {
        if (-not (Test-Path -LiteralPath 'node_modules')) {
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw "L'installation du frontend a echoue." }
        }
        Write-Host 'Frontend : http://localhost:5173 (les backends peuvent prendre une minute a demarrer)'
        & npm.cmd run dev
    } finally { Pop-Location }
} finally { Pop-Location }
