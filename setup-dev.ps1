# setup-dev.ps1 — recree application-dev.yml depuis le template
# A lancer une fois apres un clone ou un checkout si le fichier est absent

$target = "src\main\resources\application-dev.yml"
$example = "src\main\resources\application-dev.yml.example"

if (Test-Path $target) {
    Write-Host "application-dev.yml deja present." -ForegroundColor Green
    exit 0
}

if (-not (Test-Path $example)) {
    Write-Host "Template $example introuvable." -ForegroundColor Red
    exit 1
}

Copy-Item $example $target
Write-Host "application-dev.yml cree depuis le template." -ForegroundColor Yellow
Write-Host "Edite $target et renseigne ton mot de passe PostgreSQL local." -ForegroundColor Yellow
