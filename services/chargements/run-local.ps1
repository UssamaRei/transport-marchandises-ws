param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '.env')
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
    throw 'Fichier prive introuvable. Fournir -EnvFile avec le chemin vers chargements_app.env.'
}

foreach ($line in Get-Content -LiteralPath $EnvFile) {
    $line = $line.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith('#')) { continue }
    $parts = $line.Split('=', 2)
    if ($parts.Length -ne 2) { continue }
    $name = $parts[0].Trim()
    if ($name -notmatch '^(SPRING_DATASOURCE_[A-Z_]+|CAMIONS_SERVICE_URL|SERVER_PORT)$') { continue }
    $value = $parts[1].Trim().Trim('"').Trim("'")
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

foreach ($name in @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
        throw "Variable manquante : $name"
    }
}

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd spring-boot:run
    $runExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $runExitCode
