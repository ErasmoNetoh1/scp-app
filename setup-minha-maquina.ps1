# setup-minha-maquina.ps1
# Execute pelo PowerShell como Administrador

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
Write-Host "   Setup - SCP App (Sua Maquina)"           -ForegroundColor White
Write-Host "============================================" -ForegroundColor White
Write-Host ""

# ------------------------------------------------------------------------------
# PASSO 1: JDK 21
# ------------------------------------------------------------------------------
Write-Host "[ 1/5 ] JDK 21" -ForegroundColor White

$jdkInstalado = Get-ChildItem "C:\Program Files\Eclipse Adoptium\" -Filter "jdk-21*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -eq $jdkInstalado) {
    $jdkInstalado = Get-ChildItem "C:\Program Files\Java\" -Filter "jdk-21*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
}

if ($null -ne $jdkInstalado) {
    Escrever-Aviso "JDK 21 ja esta instalado em $($jdkInstalado.FullName). Pulando..."
    $javaHome = $jdkInstalado.FullName
} else {
    Escrever-Info "Baixando JDK 21 (Temurin)..."
    $jdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.3_9.msi"
    $jdkInstaller = "$env:TEMP\jdk21.msi"
    Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkInstaller -UseBasicParsing
    Escrever-Sucesso "Download concluido"
    Escrever-Info "Instalando JDK 21..."
    Start-Process msiexec.exe -Wait -ArgumentList "/i `"$jdkInstaller`" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"
    Remove-Item $jdkInstaller -Force
    Escrever-Sucesso "JDK 21 instalado"
    $jdkInstalado = Get-ChildItem "C:\Program Files\Eclipse Adoptium\" -Filter "jdk-21*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    $javaHome = if ($null -ne $jdkInstalado) { $jdkInstalado.FullName } else { "" }
}

# ------------------------------------------------------------------------------
# PASSO 2: JAVA_HOME
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[ 2/5 ] Variavel JAVA_HOME" -ForegroundColor White

if ($javaHome -ne "") {
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")
    $pathAtual = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $javaBin = $javaHome + "\bin"
    if ($pathAtual -notlike ("*" + $javaBin + "*")) {
        [System.Environment]::SetEnvironmentVariable("Path", $pathAtual + ";" + $javaBin, "Machine")
    }
    Escrever-Sucesso "JAVA_HOME configurado: $javaHome"
} else {
    Escrever-Aviso "Nao foi possivel localizar o JDK. Configure JAVA_HOME manualmente."
}

# ------------------------------------------------------------------------------
# PASSO 3: MAVEN
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[ 3/5 ] Apache Maven 3.9.16" -ForegroundColor White

$mavenPath = "C:\maven"
$mavenBin  = $mavenPath + "\bin\mvn.cmd"

if (Test-Path $mavenBin) {
    Escrever-Aviso "Maven ja esta instalado em $mavenPath. Pulando..."
} else {
    Escrever-Info "Baixando Apache Maven 3.9.16..."
    $mavenUrl = "https://downloads.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip"
    $mavenZip = "$env:TEMP\maven.zip"
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
    Escrever-Sucesso "Download concluido"
    Escrever-Info "Extraindo Maven..."
    Expand-Archive -Path $mavenZip -DestinationPath "$env:TEMP\maven_extract" -Force
    $pastaExtraida = Get-ChildItem "$env:TEMP\maven_extract" -Directory | Select-Object -First 1
    Move-Item $pastaExtraida.FullName $mavenPath
    Remove-Item $mavenZip -Force
    Remove-Item "$env:TEMP\maven_extract" -Recurse -Force -ErrorAction SilentlyContinue
    $pathAtual = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $mavenBinPath = $mavenPath + "\bin"
    if ($pathAtual -notlike ("*" + $mavenBinPath + "*")) {
        [System.Environment]::SetEnvironmentVariable("Path", $pathAtual + ";" + $mavenBinPath, "Machine")
    }
    Escrever-Sucesso "Maven instalado em $mavenPath"
}

# ------------------------------------------------------------------------------
# PASSO 4: JAVAFX
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[ 4/5 ] JavaFX SDK 21" -ForegroundColor White

$javafxPath = "C:\javafx-sdk-21"
$javafxLib  = $javafxPath + "\lib"

if (Test-Path $javafxLib) {
    Escrever-Aviso "JavaFX SDK ja esta em $javafxPath. Pulando download..."
} else {
    Escrever-Info "Baixando JavaFX SDK 21..."
    $javafxUrl = "https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_windows-x64_bin-sdk.zip"
    $javafxZip = "$env:TEMP\javafx.zip"
    Invoke-WebRequest -Uri $javafxUrl -OutFile $javafxZip -UseBasicParsing
    Escrever-Sucesso "Download concluido"
    Escrever-Info "Extraindo JavaFX SDK..."
    Expand-Archive -Path $javafxZip -DestinationPath "$env:TEMP\javafx_extract" -Force
    $pastaExtraida = Get-ChildItem "$env:TEMP\javafx_extract" -Directory | Select-Object -First 1
    Move-Item $pastaExtraida.FullName $javafxPath
    Remove-Item $javafxZip -Force
    Remove-Item "$env:TEMP\javafx_extract" -Recurse -Force -ErrorAction SilentlyContinue
    Escrever-Sucesso "JavaFX SDK instalado em $javafxPath"
}

[System.Environment]::SetEnvironmentVariable("PATH_TO_FX", $javafxLib, "User")
Escrever-Sucesso "Variavel PATH_TO_FX configurada: $javafxLib"

# ------------------------------------------------------------------------------
# PASSO 5: EXTRAIR O APP E BAIXAR DEPENDENCIAS
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[ 5/5 ] Extraindo o app e baixando dependencias" -ForegroundColor White

$zipApp = "C:\scp-app\scp-app.zip"
$destinoApp = "C:\scp-app"

if (Test-Path $zipApp) {
    Escrever-Info "Extraindo scp-app.zip..."
    Expand-Archive -Path $zipApp -DestinationPath $destinoApp -Force
    Escrever-Sucesso "App extraido em $destinoApp"
} else {
    Escrever-Aviso "scp-app.zip nao encontrado em C:\scp-app. Pulando extracao..."
}

# Procura o pom.xml dentro de C:\scp-app
$pom = Get-ChildItem "C:\scp-app" -Filter "pom.xml" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1

if ($null -ne $pom) {
    $pomDir = $pom.DirectoryName
    Escrever-Info "Projeto encontrado em: $pomDir"
    Escrever-Info "Baixando dependencias (pode demorar alguns minutos na primeira vez)..."
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    Push-Location $pomDir
    try {
        & "$mavenPath\bin\mvn.cmd" dependency:resolve -q
        Escrever-Sucesso "Dependencias baixadas com sucesso"
    } catch {
        Escrever-Aviso "Nao foi possivel baixar as dependencias agora."
        Escrever-Info "Rode manualmente: cd $pomDir && mvn dependency:resolve"
    } finally {
        Pop-Location
    }
    Write-Host ""
    Write-Host "Para abrir o app, feche este terminal e abra um novo PowerShell, depois rode:" -ForegroundColor White
    Write-Host "  cd $pomDir" -ForegroundColor Yellow
    Write-Host "  mvn javafx:run" -ForegroundColor Yellow
} else {
    Escrever-Aviso "pom.xml nao encontrado. Certifique-se de que o scp-app.zip esta em C:\scp-app."
}

# ------------------------------------------------------------------------------
# RESUMO
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "   Instalacao concluida!"                    -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "O que foi configurado:" -ForegroundColor White
Write-Host "  JDK 21        -> instalado" -ForegroundColor Gray
Write-Host "  Maven         -> C:\maven" -ForegroundColor Gray
Write-Host "  JavaFX SDK 21 -> C:\javafx-sdk-21" -ForegroundColor Gray
Write-Host "  JAVA_HOME     -> configurado" -ForegroundColor Gray
Write-Host "  PATH_TO_FX    -> C:\javafx-sdk-21\lib" -ForegroundColor Gray
Write-Host ""
Read-Host "Pressione Enter para fechar"
