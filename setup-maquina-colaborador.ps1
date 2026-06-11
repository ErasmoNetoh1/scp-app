# ==============================================================================
# setup-maquina-colaborador.ps1
# Script de configuração para a máquina do COLABORADOR (quem você vai acessar)
#
# O QUE ESSE SCRIPT FAZ:
#   1. Instala o JDK 21 (necessário para rodar arquivos Java se precisar)
#   2. Habilita o OpenSSH Server (para você conseguir acessar remotamente)
#   3. Configura o SSH para iniciar automaticamente com o Windows
#   4. Libera a porta 22 no Firewall do Windows
#   5. Exibe o IP da máquina para você usar na conexão
#
# COMO RODAR (na máquina do colaborador):
#   1. Copie este arquivo para a máquina do colaborador (pode mandar por e-mail/Teams)
#   2. Clique com botão direito → "Executar com PowerShell" como Administrador
#   OU no PowerShell como Admin:
#   Set-ExecutionPolicy Bypass -Scope Process; .\setup-maquina-colaborador.ps1
#
# IMPORTANTE: este script precisa rodar apenas UMA VEZ na máquina do colaborador.
# Depois disso, você consegue acessá-la remotamente sempre que precisar.
# ==============================================================================

$ErrorActionPreference = "Stop"

function Escrever-Sucesso($msg)  { Write-Host "✓ $msg" -ForegroundColor Green }
function Escrever-Info($msg)     { Write-Host "→ $msg" -ForegroundColor Cyan }
function Escrever-Aviso($msg)    { Write-Host "⚠ $msg" -ForegroundColor Yellow }
function Escrever-Erro($msg)     { Write-Host "✗ $msg" -ForegroundColor Red }

# Verifica se é Administrador
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Escrever-Erro "Execute este script como Administrador!"
    Escrever-Info "Clique com botão direito no arquivo → 'Executar com PowerShell' como Admin"
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor White
Write-Host "   Setup - Máquina do Colaborador"          -ForegroundColor White
Write-Host "============================================" -ForegroundColor White
Write-Host ""

# ==============================================================================
# PASSO 1: INSTALAR JDK 21
# ==============================================================================
Write-Host "[ 1/4 ] JDK 21" -ForegroundColor White

# Verifica se alguma versão do Java 21 já está instalada
$javaExistente = Get-Command java -ErrorAction SilentlyContinue
$javaVersion = if ($javaExistente) { (java -version 2>&1 | Select-String "21") } else { $null }

if ($null -ne $javaVersion) {
    Escrever-Aviso "Java 21 já está instalado. Pulando..."
} else {
    Escrever-Info "Baixando JDK 21 (Temurin)..."

    $jdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.3_9.msi"
    $jdkInstaller = "$env:TEMP\jdk21.msi"

    try {
        Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkInstaller -UseBasicParsing
        Escrever-Sucesso "Download concluído"

        Escrever-Info "Instalando JDK 21 (pode demorar ~2 minutos)..."
        Start-Process msiexec.exe -Wait -ArgumentList "/i `"$jdkInstaller`" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"

        Remove-Item $jdkInstaller -Force
        Escrever-Sucesso "JDK 21 instalado"
    } catch {
        Escrever-Aviso "Não foi possível instalar o JDK automaticamente: $_"
        Escrever-Info "O JDK é opcional para o colaborador. Continuando com o restante..."
    }
}

# ==============================================================================
# PASSO 2: HABILITAR OPENSSH SERVER
# ==============================================================================
Write-Host ""
Write-Host "[ 2/4 ] OpenSSH Server" -ForegroundColor White

# Verifica se o OpenSSH Server já está instalado
# Get-WindowsCapability lista funcionalidades opcionais do Windows
$sshCapability = Get-WindowsCapability -Online | Where-Object { $_.Name -like "OpenSSH.Server*" }

if ($sshCapability.State -eq "Installed") {
    Escrever-Aviso "OpenSSH Server já está instalado. Pulando instalação..."
} else {
    Escrever-Info "Instalando OpenSSH Server (recurso nativo do Windows)..."

    # Add-WindowsCapability instala recursos opcionais do Windows
    # Isso é o equivalente a ir em Configurações → Recursos Opcionais manualmente
    Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

    Escrever-Sucesso "OpenSSH Server instalado"
}

# ==============================================================================
# PASSO 3: INICIAR SSH E CONFIGURAR INICIALIZAÇÃO AUTOMÁTICA
# ==============================================================================
Write-Host ""
Write-Host "[ 3/4 ] Configurando serviço SSH" -ForegroundColor White

# Start-Service inicia o serviço agora
Escrever-Info "Iniciando o serviço SSH..."
Start-Service sshd

# Set-Service configura o serviço para iniciar automaticamente toda vez que o Windows ligar
# Sem isso, o colaborador precisaria iniciar manualmente toda vez que reiniciar o PC
Escrever-Info "Configurando inicialização automática..."
Set-Service -Name sshd -StartupType Automatic

# Verifica se o serviço está rodando
$servicoStatus = Get-Service sshd
if ($servicoStatus.Status -eq "Running") {
    Escrever-Sucesso "Serviço SSH rodando e configurado para iniciar automaticamente"
} else {
    Escrever-Erro "O serviço SSH não iniciou corretamente. Status: $($servicoStatus.Status)"
}

# ==============================================================================
# PASSO 4: LIBERAR PORTA 22 NO FIREWALL
# ==============================================================================
Write-Host ""
Write-Host "[ 4/4 ] Firewall — liberando porta 22" -ForegroundColor White

# Verifica se a regra já existe para não criar duplicada
$regraExistente = Get-NetFirewallRule -Name "OpenSSH-Server-In-TCP" -ErrorAction SilentlyContinue

if ($null -ne $regraExistente) {
    Escrever-Aviso "Regra de firewall para SSH já existe. Pulando..."
} else {
    # New-NetFirewallRule cria uma regra no Firewall do Windows
    # -Direction Inbound = tráfego que chega na máquina (alguém conectando nela)
    # -Protocol TCP -LocalPort 22 = porta padrão do SSH
    # -Action Allow = permitir (não bloquear)
    New-NetFirewallRule `
        -Name "OpenSSH-Server-In-TCP" `
        -DisplayName "OpenSSH Server (entrada)" `
        -Description "Permite conexoes SSH de entrada na porta 22" `
        -Enabled True `
        -Direction Inbound `
        -Protocol TCP `
        -Action Allow `
        -LocalPort 22 | Out-Null

    Escrever-Sucesso "Porta 22 liberada no Firewall"
}

# ==============================================================================
# DESCOBRIR O IP DA MÁQUINA
# ==============================================================================

# Get-NetIPAddress pega todas as configurações de IP da máquina
# Filtramos por IPv4 e excluímos o loopback (127.0.0.1, que é só interno)
$ips = Get-NetIPAddress -AddressFamily IPv4 |
       Where-Object { $_.IPAddress -ne "127.0.0.1" -and $_.PrefixOrigin -ne "WellKnown" } |
       Select-Object -ExpandProperty IPAddress

# ==============================================================================
# RESUMO FINAL
# ==============================================================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "   Configuração concluída!"                  -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Status da máquina do colaborador:" -ForegroundColor White
Write-Host "  • JDK 21          → instalado" -ForegroundColor Gray
Write-Host "  • OpenSSH Server  → rodando" -ForegroundColor Gray
Write-Host "  • Inicialização   → automática (não precisa reiniciar manualmente)" -ForegroundColor Gray
Write-Host "  • Firewall porta 22 → liberada" -ForegroundColor Gray
Write-Host ""
Write-Host "IP(s) desta máquina:" -ForegroundColor White
foreach ($ip in $ips) {
    Write-Host "  → $ip" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "Anote o IP acima — você vai usar ele na tela de conexão do SCP App." -ForegroundColor Cyan
Write-Host ""
Write-Host "Para conectar, use:" -ForegroundColor White
Write-Host "  Host:   (um dos IPs acima)" -ForegroundColor Gray

# Pega o nome de usuário atual (quem está logado na máquina do colaborador)
$usuarioAtual = $env:USERNAME
Write-Host "  Usuário: $usuarioAtual" -ForegroundColor Gray
Write-Host "  Senha:   (senha de login do Windows do colaborador)" -ForegroundColor Gray
Write-Host ""
Write-Host "Observação: se o colaborador reiniciar o PC, o SSH volta sozinho." -ForegroundColor DarkGray
Write-Host "Não é necessário rodar este script novamente." -ForegroundColor DarkGray
Write-Host ""
