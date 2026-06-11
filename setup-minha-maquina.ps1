# ==============================================================================
# setup-minha-maquina.ps1
# Script de configuração para a SUA máquina (quem vai usar a aplicação)
#
# O QUE ESSE SCRIPT FAZ:
#   1. Instala o JDK 21 (Temurin/Adoptium)
#   2. Instala o Apache Maven 3.9.16
#   3. Instala o JavaFX SDK 21
#   4. Configura as variáveis de ambiente (JAVA_HOME, PATH, PATH_TO_FX)
#   5. Baixa as dependências do projeto Maven
#
# COMO RODAR:
#   1. Clique com botão direito neste arquivo
#   2. Selecione "Executar com PowerShell" (como Administrador)
#   OU no PowerShell como Admin:
#   Set-ExecutionPolicy Bypass -Scope Process; .\setup-minha-maquina.ps1
# ==============================================================================

# Faz o script parar imediatamente se qualquer comando falhar
$ErrorActionPreference = "Stop"

# Cores para deixar as mensagens mais claras no terminal
function Escrever-Sucesso($msg)  { Write-Host "✓ $msg" -ForegroundColor Green }
function Escrever-Info($msg)     { Write-Host "→ $msg" -ForegroundColor Cyan }
function Escrever-Aviso($msg)    { Write-Host "⚠ $msg" -ForegroundColor Yellow }
function Escrever-Erro($msg)     { Write-Host "✗ $msg" -ForegroundColor Red }

# Verifica se está rodando como Administrador (necessário para instalar programas)
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Escrever-Erro "Execute este script como Administrador!"
    Escrever-Info "Clique com botão direito no arquivo → 'Executar com PowerShell' como Admin"
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor White
Write-Host "   Setup - SCP App (Sua Máquina)"           -ForegroundColor White
Write-Host "============================================" -ForegroundColor White
Write-Host ""

# ==============================================================================
# PASSO 1: INSTALAR JDK 21
# ==============================================================================
Write-Host "[ 1/5 ] JDK 21" -ForegroundColor White

$jdkPath = "C:\Program Files\Eclipse Adoptium\jdk-21"
$jdkInstalado = Test-Path $jdkPath

if ($jdkInstalado) {
    Escrever-Aviso "JDK 21 já está instalado em $jdkPath. Pulando..."
} else {
    Escrever-Info "Baixando JDK 21 (Temurin)..."

    $jdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.3_9.msi"
    $jdkInstaller = "$env:TEMP\jdk21.msi"

    # Faz o download do instalador
    Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkInstaller -UseBasicParsing
    Escrever-Sucesso "Download concluído"

    Escrever-Info "Instalando JDK 21..."
    # /quiet = instala sem mostrar janelas; /norestart = não reinicia o PC
    Start-Process msiexec.exe -Wait -ArgumentList "/i `"$jdkInstaller`" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"

    # Limpa o instalador temporário
    Remove-Item $jdkInstaller -Force
    Escrever-Sucesso "JDK 21 instalado com sucesso"
}

# ==============================================================================
# PASSO 2: CONFIGURAR JAVA_HOME
# ==============================================================================
Write-Host ""
Write-Host "[ 2/5 ] Variável JAVA_HOME" -ForegroundColor White

# Procura onde o JDK foi instalado (pode variar a versão exata)
$jdkReal = Get-ChildItem "C:\Program Files\Eclipse Adoptium\" -Filter "jdk-21*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1

if ($null -eq $jdkReal) {
    # Tenta outro caminho comum
    $jdkReal = Get-ChildItem "C:\Program Files\Java\" -Filter "jdk-21*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
}

if ($null -ne $jdkReal) {
    $javaHome = $jdkReal.FullName
    # Define a variável de ambiente JAVA_HOME no nível do sistema (permanente)
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")

    # Adiciona o Java ao PATH se ainda não estiver
    $pathAtual = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($pathAtual -notlike "*$javaHome\bin*") {
        [System.Environment]::SetEnvironmentVariable("Path", "$pathAtual;$javaHome\bin", "Machine")
    }
    Escrever-Sucesso "JAVA_HOME configurado: $javaHome"
} else {
    Escrever-Aviso "Não foi possível localizar o JDK automaticamente."
    Escrever-Info "Configure JAVA_HOME manualmente apontando para a pasta do JDK 21."
}

# ==============================================================================
# PASSO 3: INSTALAR MAVEN
# ==============================================================================
Write-Host ""
Write-Host "[ 3/5 ] Apache Maven 3.9.16" -ForegroundColor White

$mavenPath = "C:\maven"
$mavenBin  = "$mavenPath\bin\mvn.cmd"

if (Test-Path $mavenBin) {
    Escrever-Aviso "Maven já está instalado em $mavenPath. Pulando..."
} else {
    Escrever-Info "Baixando Apache Maven 3.9.16..."

    $mavenUrl = "https://downloads.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip"
    $mavenZip = "$env:TEMP\maven.zip"

    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
    Escrever-Sucesso "Download concluído"

    Escrever-Info "Extraindo Maven..."
    # Expand-Archive descompacta o zip
    Expand-Archive -Path $mavenZip -DestinationPath "$env:TEMP\maven_extract" -Force

    # Move para C:\maven (pasta limpa, sem subpasta com versão)
    $pastaExtraida = Get-ChildItem "$env:TEMP\maven_extract" -Directory | Select-Object -First 1
    Move-Item $pastaExtraida.FullName $mavenPath

    Remove-Item $mavenZip -Force
    Remove-Item "$env:TEMP\maven_extract" -Recurse -Force -ErrorAction SilentlyContinue

    # Adiciona o Maven ao PATH do sistema
    $pathAtual = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($pathAtual -notlike "*$mavenPath\bin*") {
        [System.Environment]::SetEnvironmentVariable("Path", "$pathAtual;$mavenPath\bin", "Machine")
    }

    Escrever-Sucesso "Maven instalado em $mavenPath"
}

# ==============================================================================
# PASSO 4: INSTALAR JAVAFX SDK E CONFIGURAR PATH_TO_FX
# ==============================================================================
Write-Host ""
Write-Host "[ 4/5 ] JavaFX SDK 21" -ForegroundColor White

$javafxPath = "C:\javafx-sdk-21"
$javafxLib  = "$javafxPath\lib"

if (Test-Path $javafxLib) {
    Escrever-Aviso "JavaFX SDK já está em $javafxPath. Pulando download..."
} else {
    Escrever-Info "Baixando JavaFX SDK 21 para Windows..."

    $javafxUrl = "https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_windows-x64_bin-sdk.zip"
    $javafxZip = "$env:TEMP\javafx.zip"

    Invoke-WebRequest -Uri $javafxUrl -OutFile $javafxZip -UseBasicParsing
    Escrever-Sucesso "Download concluído"

    Escrever-Info "Extraindo JavaFX SDK..."
    Expand-Archive -Path $javafxZip -DestinationPath "$env:TEMP\javafx_extract" -Force

    $pastaExtraida = Get-ChildItem "$env:TEMP\javafx_extract" -Directory | Select-Object -First 1
    Move-Item $pastaExtraida.FullName $javafxPath

    Remove-Item $javafxZip -Force
    Remove-Item "$env:TEMP\javafx_extract" -Recurse -Force -ErrorAction SilentlyContinue

    Escrever-Sucesso "JavaFX SDK instalado em $javafxPath"
}

# Configura a variável PATH_TO_FX (usada pelo launch.json do VSCode)
[System.Environment]::SetEnvironmentVariable("PATH_TO_FX", $javafxLib, "User")
Escrever-Sucesso "Variável PATH_TO_FX configurada: $javafxLib"

# ==============================================================================
# PASSO 5: BAIXAR DEPENDÊNCIAS DO PROJETO MAVEN
# ==============================================================================
Write-Host ""
Write-Host "[ 5/5 ] Dependências do projeto Maven" -ForegroundColor White

# Tenta encontrar o pom.xml do projeto na pasta atual ou Desktop
$possiveisCaminhos = @(
    ".\pom.xml",
    "$env:USERPROFILE\Desktop\scp-app\pom.xml",
    "$env:USERPROFILE\Downloads\scp-app\pom.xml"
)

$pomEncontrado = $null
foreach ($caminho in $possiveisCaminhos) {
    if (Test-Path $caminho) {
        $pomEncontrado = (Get-Item $caminho).DirectoryName
        break
    }
}

if ($null -ne $pomEncontrado) {
    Escrever-Info "Projeto encontrado em: $pomEncontrado"
    Escrever-Info "Baixando dependências (pode demorar alguns minutos na primeira vez)..."

    # Atualiza o PATH na sessão atual para o mvn funcionar imediatamente
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")

    Push-Location $pomEncontrado
    try {
        & "$mavenPath\bin\mvn.cmd" dependency:resolve -q
        Escrever-Sucesso "Dependências baixadas com sucesso"
    } catch {
        Escrever-Aviso "Não foi possível baixar as dependências automaticamente."
        Escrever-Info "Abra o terminal na pasta do projeto e rode: mvn dependency:resolve"
    } finally {
        Pop-Location
    }
} else {
    Escrever-Aviso "Projeto scp-app não encontrado automaticamente."
    Escrever-Info "Extraia o scp-app.zip e rode 'mvn dependency:resolve' dentro da pasta."
}

# ==============================================================================
# RESUMO FINAL
# ==============================================================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "   Instalação concluída!"                    -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "O que foi instalado/configurado:" -ForegroundColor White
Write-Host "  • JDK 21          → $javaHome" -ForegroundColor Gray
Write-Host "  • Apache Maven    → $mavenPath" -ForegroundColor Gray
Write-Host "  • JavaFX SDK 21   → $javafxPath" -ForegroundColor Gray
Write-Host "  • JAVA_HOME       → configurado" -ForegroundColor Gray
Write-Host "  • PATH_TO_FX      → $javafxLib" -ForegroundColor Gray
Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor White
Write-Host "  1. FECHE e reabra o VSCode para carregar as novas variáveis" -ForegroundColor Yellow
Write-Host "  2. Abra a pasta scp-app no VSCode" -ForegroundColor Yellow
Write-Host "  3. Para rodar a CLI:  mvn compile exec:java -Dexec.mainClass=com.scpapp.MainCLI" -ForegroundColor Yellow
Write-Host "  4. Para rodar a GUI:  mvn javafx:run" -ForegroundColor Yellow
Write-Host ""
