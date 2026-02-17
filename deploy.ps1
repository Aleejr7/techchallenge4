# ============================================================================
# Script de Deploy Unificado para Todas as Funções Lambda
# ============================================================================
# Este script faz deploy de TODAS as funções Lambda do projeto usando
# o mesmo pacote (function.zip), mas com handlers diferentes
# ============================================================================

param(
    [string]$FunctionName = "all",  # Nome específico ou "all" para todas
    [switch]$SkipBuild = $false,     # Pular build se já foi feito
    [switch]$CreateIfNotExists = $false  # Criar função se não existir
)

# Configurações
$CONFIG_FILE = "lambda-config.json"
$REGION = "us-east-2"
$ZIP_FILE = "target/function.zip"

# ============================================================================
# Funções Auxiliares
# ============================================================================

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERRO] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host ">> $Message" -ForegroundColor Yellow
}

# ============================================================================
# Carregar Configuração
# ============================================================================

Write-Header "Deploy de Funções Lambda - Tech Challenge 4"

if (-not (Test-Path $CONFIG_FILE)) {
    Write-Error "Arquivo de configuração não encontrado: $CONFIG_FILE"
    exit 1
}

$config = Get-Content $CONFIG_FILE | ConvertFrom-Json
$REGION = $config.region

# ============================================================================
# Build do Projeto
# ============================================================================

if (-not $SkipBuild) {
    Write-Header "Build do Projeto"
    Write-Info "Executando Maven build..."

    .\mvnw clean package -DskipTests

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build falhou!"
        exit 1
    }

    Write-Success "Build concluído com sucesso!"
} else {
    Write-Info "Build pulado (usando pacote existente)"
}

# Verificar se o ZIP existe
if (-not (Test-Path $ZIP_FILE)) {
    Write-Error "Arquivo $ZIP_FILE não encontrado!"
    exit 1
}

# ============================================================================
# Deploy das Funções
# ============================================================================

Write-Header "Deploy das Funções Lambda"

$functionsToeDeploy = if ($FunctionName -eq "all") {
    $config.functions
} else {
    $config.functions | Where-Object { $_.name -eq $FunctionName }
}

if ($functionsToeDeploy.Count -eq 0) {
    Write-Error "Nenhuma função encontrada com o nome: $FunctionName"
    exit 1
}

$deployCount = 0
$errorCount = 0

foreach ($func in $functionsToeDeploy) {
    Write-Host ""
    Write-Host "------------------------------------------------------------" -ForegroundColor DarkCyan
    Write-Info "Processando: $($func.name)"
    Write-Host "------------------------------------------------------------" -ForegroundColor DarkCyan

    # Verificar se a função existe
    $functionExists = aws lambda get-function --function-name $func.name --region $REGION 2>$null

    if ($functionExists) {
        # Atualizar APENAS o código da função (sem mexer em configurações)
        Write-Info "Atualizando código da função..."

        aws lambda update-function-code `
            --function-name $func.name `
            --zip-file fileb://$ZIP_FILE `
            --region $REGION | Out-Null

        if ($LASTEXITCODE -eq 0) {
            Write-Success "Código atualizado: $($func.name)"

            $deployCount++
        } else {
            Write-Error "Falha ao atualizar: $($func.name)"
            $errorCount++
        }
    } else {
        if ($CreateIfNotExists) {
            Write-Info "Função não existe. Criação manual necessária."
            Write-Host "Use o comando:" -ForegroundColor Yellow
            Write-Host "aws lambda create-function --function-name $($func.name) ..." -ForegroundColor White
        } else {
            Write-Error "Função não encontrada: $($func.name)"
            Write-Info "Use -CreateIfNotExists para ver comando de criação"
            $errorCount++
        }
    }
}

# ============================================================================
# Resumo
# ============================================================================

Write-Header "Resumo do Deploy"

Write-Host "Total de funções processadas: $($functionsToeDeploy.Count)" -ForegroundColor White
Write-Host "Sucesso: $deployCount" -ForegroundColor Green
Write-Host "Erros: $errorCount" -ForegroundColor $(if ($errorCount -eq 0) { "Green" } else { "Red" })

if ($deployCount -gt 0) {
    Write-Host ""
    Write-Success "Deploy concluído!"
    Write-Host ""
    Write-Host "Próximos passos:" -ForegroundColor Yellow
    Write-Host "1. Verifique as funções no console AWS Lambda" -ForegroundColor White
    Write-Host "2. Teste as funções com eventos de exemplo" -ForegroundColor White
    Write-Host "3. Monitore os logs no CloudWatch" -ForegroundColor White
}

Write-Host ""

exit $errorCount


