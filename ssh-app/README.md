# SSH App

Aplicação Java para transferência de arquivos via SSH/SFTP.
Contém interface de linha de comando (CLI) e interface gráfica (JavaFX).

---

## Pré-requisitos

Instale tudo isso antes de começar:

| O que                   | Link                                                         | Observação                             |
| ----------------------- | ------------------------------------------------------------ | -------------------------------------- |
| JDK 21                  | https://adoptium.net                                         | Escolha "Temurin 21 LTS"               |
| Maven                   | https://maven.apache.org/download.cgi                        | Ou instale pelo gerenciador de pacotes |
| VSCode                  | https://code.visualstudio.com                                |                                        |
| Extension Pack for Java | No VSCode: Ctrl+Shift+X → pesquise "Extension Pack for Java" | Instala suporte a Java no VSCode       |
| JavaFX SDK 21           | https://openjfx.io → Download → SDK                          | Só para a GUI (etapa 5)                |

---

## Como verificar se as instalações funcionaram

Abra o terminal e rode:

```bash
java -version
# Deve aparecer: openjdk version "21..."

mvn -version
# Deve aparecer: Apache Maven 3.x.x
```

---

## Estrutura do projeto

```
ssh-app/
├── pom.xml                          ← Configuração do Maven e dependências
├── .vscode/
│   ├── launch.json                  ← Como rodar no VSCode (CLI e GUI)
│   └── settings.json                ← Configurações do VSCode para Java
└── src/main/java/com/sshapp/
    ├── SSHManager.java              ← Toda a lógica SSH/SFTP (etapas 1-4)
    ├── MainCLI.java                 ← Interface de linha de comando (etapas 2-3)
    └── MainApp.java                 ← Interface gráfica JavaFX (etapa 5)
```

---

## Passo a passo para rodar

### 1. Abrir o projeto no VSCode

```bash
# No terminal, entre na pasta do projeto e abra o VSCode
cd ssh-app
code .
```

Aguarde o VSCode detectar o projeto Maven (aparece uma notificação no canto inferior direito).
Pode demorar 1-2 minutos na primeira vez — ele está baixando as dependências.

---

### 2. Rodar a versão CLI (interface de terminal)

**Opção A — pelo terminal integrado do VSCode:**

```bash
mvn compile exec:java -Dexec.mainClass="com.sshapp.MainCLI"
```

**Opção B — pelo Run and Debug do VSCode:**

1. Pressione `Ctrl+Shift+D`
2. Selecione "Rodar CLI (Terminal)" no menu suspenso
3. Clique no botão verde ▶ ou pressione `F5`

> ⚠️ Certifique-se de que o terminal integrado está selecionado (não o "Debug Console"),
> pois o Scanner precisa do terminal para receber o que você digita.

---

### 3. Rodar a versão GUI (JavaFX)

#### 3a. Baixar o JavaFX SDK

1. Acesse https://openjfx.io
2. Clique em "Download" → selecione versão 21, tipo SDK, seu sistema operacional
3. Extraia o arquivo baixado em um local fixo (ex: `C:/javafx-sdk-21` ou `/opt/javafx-sdk-21`)

#### 3b. Configurar a variável de ambiente PATH_TO_FX

**Windows (PowerShell):**

```powershell
# Troque pelo caminho real onde você extraiu o JavaFX
[System.Environment]::SetEnvironmentVariable("PATH_TO_FX", "C:\javafx-sdk-21\lib", "User")
# Feche e reabra o VSCode depois de rodar isso
```

**Mac/Linux (bash/zsh):**

```bash
# Adicione esta linha ao seu ~/.bashrc ou ~/.zshrc
export PATH_TO_FX=/opt/javafx-sdk-21/lib
# Depois rode: source ~/.bashrc
```

#### 3c. Rodar a GUI

**Opção A — pelo Maven:**

```bash
mvn javafx:run
```

**Opção B — pelo Run and Debug do VSCode:**

1. `Ctrl+Shift+D` → selecione "Rodar GUI (JavaFX)"
2. Pressione `F5`

---

## Testar localmente (conectar na própria máquina)

Para testar sem uma segunda máquina, conecte no `localhost`.

**No Linux/Mac — o servidor SSH já vem instalado. Apenas inicie:**

```bash
sudo systemctl start ssh    # Linux
sudo systemsetup -setremotelogin on  # Mac
```

**No Windows — habilite o OpenSSH Server:**

1. Configurações → Sistema → Recursos opcionais
2. Adicionar recurso → "OpenSSH Server"
3. No PowerShell como administrador:

```powershell
Start-Service sshd
Set-Service -Name sshd -StartupType Automatic
```

**Credenciais para teste local:**

- Host: `localhost` (ou `127.0.0.1`)
- Usuário: seu usuário do sistema operacional
- Senha: sua senha de login

---

## Rodar contra outra máquina (etapa 4)

Não é preciso mudar nada no código.
Basta informar o IP, usuário e senha da outra máquina quando o app pedir.

Exemplo: a outra máquina está em `192.168.1.100` com usuário `ubuntu`.
Digite esses dados na tela de conexão — a aplicação funciona igual.

---

## Problemas comuns

| Erro                  | Causa provável                | Solução                                       |
| --------------------- | ----------------------------- | --------------------------------------------- |
| `Connection refused`  | Servidor SSH não está rodando | Inicie o serviço SSH (ver acima)              |
| `Auth fail`           | Usuário ou senha errados      | Verifique as credenciais                      |
| `No JavaFX runtime`   | PATH_TO_FX não configurado    | Configure a variável de ambiente              |
| `Scanner não lê nada` | Rodando no "Debug Console"    | Mude para "integratedTerminal" no launch.json |
| `Build falhou`        | Dependências não baixadas     | Rode `mvn dependency:resolve`                 |
