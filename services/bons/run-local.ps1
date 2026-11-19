$ErrorActionPreference = 'Stop'

$moduleDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $moduleDirectory '.env'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Fichier de configuration privé absent : $envFile (copier .env.example vers .env et renseigner les valeurs)."
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $separator = $line.IndexOf('=')
        if ($separator -gt 0) {
            $name = $line.Substring(0, $separator).Trim()
            $value = $line.Substring($separator + 1)
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

if (-not $env:BONS_CLIENT_SECRET) {
    throw 'Définir BONS_CLIENT_SECRET dans .env avec le secret du client bons-service.'
}

Push-Location $moduleDirectory
try {
    mvn spring-boot:run
}
finally {
    Pop-Location
}
