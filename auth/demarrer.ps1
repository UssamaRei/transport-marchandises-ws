$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$privateDirectory = Join-Path $projectRoot '.secrets'
$envFile = Join-Path $privateDirectory 'auth.env'
New-Item -ItemType Directory -Force -Path $privateDirectory | Out-Null

if (-not (Test-Path -LiteralPath $envFile)) {
    $lines = @('AUTH_BASE_URL=http://localhost:8090')
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        foreach ($name in @('KEYCLOAK_ADMIN_PASSWORD', 'CHARGEMENTS_CLIENT_SECRET',
                'CAMIONS_CLIENT_SECRET', 'BONS_CLIENT_SECRET', 'TRANSPORTS_CLIENT_SECRET',
                'WS_ADMIN_PASSWORD', 'WS_AGENT_PASSWORD', 'WS_LECTEUR_PASSWORD')) {
            $bytes = New-Object byte[] 32
            $generator.GetBytes($bytes)
            $secret = [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
            $lines += "$name=$secret"
        }
    } finally { $generator.Dispose() }
    [System.IO.File]::WriteAllLines($envFile, $lines, (New-Object System.Text.UTF8Encoding $false))
}

& docker compose --env-file $envFile -f (Join-Path $projectRoot 'compose.auth.yaml') up -d
if ($LASTEXITCODE -ne 0) { throw 'Echec du demarrage de Keycloak. Verifier Docker Desktop.' }
Write-Host 'Keycloak : http://localhost:8090'
Write-Host "Identifiants et secrets prives : $envFile"
Write-Host 'Comptes de test : admin-ws, agent-ws, lecteur-ws. Voir auth/README.md.'
