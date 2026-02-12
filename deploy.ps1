# Script simples de deploy para Lambda

Write-Host "Deploy da funcao Lambda feedback-ingestao" -ForegroundColor Cyan
Write-Host ""

# Build
Write-Host "Fazendo build..." -ForegroundColor Green
.\mvnw clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build concluido!" -ForegroundColor Green
    Write-Host ""

    # Deploy
    Write-Host "Fazendo deploy para AWS..." -ForegroundColor Green
    aws lambda update-function-code --function-name feedback-ingestao --zip-file fileb://target/function.zip --region us-east-2

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Deploy concluido com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "Erro no deploy!" -ForegroundColor Red
    }
} else {
    Write-Host "Erro no build!" -ForegroundColor Red
}


