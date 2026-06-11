# setup-maquina-colaborador.ps1
# Execute pelo PowerShell como Administrador na maquina do colaborador

$ErrorActionPreference = "Stop"

function Escrever-Sucesso($msg)  { Write-Host "[OK] $msg" -ForegroundColor Green }
function Escrever-Info($msg)     { Write-Host "[->] $msg" -ForegroundColor Cyan }
function Escrever-Aviso($msg)    { Write-Host "[!!] $msg" -ForegroundColor Yellow }
function Escrever-Erro($msg)     { Write-Host "[XX] $msg" -ForegroundColor Red }

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Escrever-Erro "Execute este script como Administrador!"
    Read-Host "Pressione Enter para fechar"
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor White
Write-Host "   Setup - Maquina do Colaborador"          -ForegroundColor White
Write-Host "============================================" -ForegroundColor White
Write-Host ""

# ------------------------------------------------------------------------------
# PASSO 1: JDK 21
# ------------------------------------------------------------------------------
Write-Host "[ 1/4 ] JDK 21" -ForegroundColor White

$jdkInstalado = Get-ChildItem "C:\Program Files\Eclipse Adoptium\" -Filter "jdk-21*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -eq $jdkInstalado) {
    $jdkInstalado = Get-ChildItem "C:\Program Files\Java\" -Filter "jdk-21*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
}

if ($null -ne $jdkInstalado) {
    Escrever-Aviso "JDK 21 ja esta instalado. Pulando..."
} else {
    Escrever-Info "Baixando JDK 21 (Temurin)..."
    $jdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.3_9.msi"
    $jdkInstaller = "$env:TEMP\jdk21.msi"
    try {
        Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkInstaller -UseBasicParsing
        Escrever-Sucesso "Download concluido"
        Escrever-Info "Instalando JDK 21 (pode demorar 2 minutos)..."
        Start-Process msiexec.exe -Wait -ArgumentList "/i `"$jdkInstaller`" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"
        Remove-Item $jdkInstaller -Force
        Escrever-Sucesso "JDK 21 instalado"
    } catch {
        Escrever-Aviso "Nao foi possivel instalar o JDK: $_"
        Escrever-Info "O JDK e opcional para o colaborador. Continuando..."
    }
}

# ------------------------------------------------------------------------------
# PASSO 2: OPENSSH SERVER
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[ 2/4 ] OpenSSH Server" -ForegroundColor White

$sshCapability = Get-WindowsCapability -Online | Where-Object { $_.Name -like "OpenSSH.Server*" }

if ($sshCapability.State -eq "Installed") {
    Escrever-Aviso "OpenSSH Server ja esta instalado. Pulando..."
} else {
    Escrever-Info "Instalando OpenSSH Server..."
    Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
    Escrever-Sucesso "OpenSSH Server instalado"
}

# ------------------------------------------------------------------------------
# PASSO 3: INICIAR SSH
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[ 3/4 ] Configurando servico SSH" -ForegroundColor White

Escrever-Info "Iniciando o servico SSH..."
Start-Service sshd

Escrever-Info "Configurando inicializacao automatica..."
Set-Service -Name sshd -StartupType Automatic

$servicoStatus = Get-Service sshd
if ($servicoStatus.Status -eq "Running") {
    Escrever-Sucesso "Servico SSH rodando e configurado para iniciar automaticamente"
} else {
    Escrever-Erro "O servico SSH nao iniciou. Status: $($servicoStatus.Status)"
}

# ------------------------------------------------------------------------------
# PASSO 4: FIREWALL
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[ 4/4 ] Firewall - liberando porta 22" -ForegroundColor White

$regraExistente = Get-NetFirewallRule -Name "OpenSSH-Server-In-TCP" -ErrorAction SilentlyContinue

if ($null -ne $regraExistente) {
    Escrever-Aviso "Regra de firewall para SSH ja existe. Pulando..."
} else {
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

# ------------------------------------------------------------------------------
# DESCOBRIR IP E USUARIO
# ------------------------------------------------------------------------------
$ips = Get-NetIPAddress -AddressFamily IPv4 |
       Where-Object { $_.IPAddress -ne "127.0.0.1" -and $_.PrefixOrigin -ne "WellKnown" } |
       Select-Object -ExpandProperty IPAddress

$usuarioAtual = $env:USERNAME

# ------------------------------------------------------------------------------
# RESUMO
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "   Configuracao concluida!"                  -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Status:" -ForegroundColor White
Write-Host "  OpenSSH Server    -> rodando" -ForegroundColor Gray
Write-Host "  Inicializacao     -> automatica" -ForegroundColor Gray
Write-Host "  Firewall porta 22 -> liberada" -ForegroundColor Gray
Write-Host ""
Write-Host "Passe estas informacoes para quem vai conectar:" -ForegroundColor Cyan
Write-Host ""
foreach ($ip in $ips) {
    Write-Host "  IP:      $ip" -ForegroundColor Yellow
}
Write-Host "  Usuario: $usuarioAtual" -ForegroundColor Yellow
Write-Host "  Senha:   (senha de login do Windows desta maquina)" -ForegroundColor Yellow
Write-Host ""
Write-Host "Este script nao precisa ser rodado novamente." -ForegroundColor DarkGray
Write-Host ""
Read-Host "Pressione Enter para fechar"
