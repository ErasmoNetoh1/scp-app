# SCP App

## O que é e o que resolve

No trabalho, eu precisava constantemente acessar a máquina de um colaborador para buscar ou enviar arquivos. O processo era manual, demorado e dependia de ferramentas externas — compartilhamento de tela, e-mail, pendrive. Cada transferência virava uma tarefa por si só.

O SCP App resolve isso. É uma aplicação desktop feita em Java que abre uma janela simples onde você informa o IP e a senha da máquina do colaborador, e a partir daí faz upload e download de arquivos diretamente entre as duas máquinas via SSH — sem intermediários, sem ferramentas extras, sem depender de ninguém.

A ideia é que a transferência de arquivos seja tão rápida quanto arrastar um arquivo para uma pasta.

---

## O que você vai precisar

Antes de qualquer coisa, você precisa ter esses programas instalados na sua máquina. Se já tiver, pode pular. Se não tiver, o script de setup instala tudo automaticamente.

| Programa | Para que serve |
|---|---|
| JDK 21 | Roda o código Java |
| Apache Maven | Gerencia e compila o projeto |
| JavaFX SDK 21 | Desenha a janela do app |

> **Não sabe se tem instalado?** Não se preocupe — o script de setup verifica isso e instala só o que estiver faltando.

---

## Configuração inicial — faça isso uma única vez

### Passo 1 — Preparar a sua máquina

1. Baixe o arquivo `setup-minha-maquina.ps1` deste repositório
2. Clique com **botão direito** no arquivo
3. Selecione **"Executar com PowerShell"**
4. Se aparecer uma janela perguntando sobre permissão de administrador, clique em **Sim**
5. Aguarde o script terminar — ele vai mostrar o progresso e um resumo no final
6. **Feche e reabra o PowerShell** depois que terminar

---

### Passo 2 — Preparar a máquina do colaborador

Faça isso **uma única vez** na máquina de quem você vai acessar remotamente.

1. Mande o arquivo `setup-maquina-colaborador.ps1` para o colaborador (pode ser por e-mail, Teams ou pendrive)
2. Ele clica com **botão direito** → **"Executar com PowerShell"** como Administrador
3. No final do script vai aparecer na tela o **IP** e o **usuário** da máquina dele
4. **Anota esses dois dados** — você vai precisar deles toda vez que conectar

> **O colaborador não precisa fazer mais nada depois disso.** O SSH fica configurado para iniciar automaticamente toda vez que o PC dele ligar.

---

### Passo 3 — Colocar o projeto na sua máquina

1. Baixe o arquivo `scp-app.zip` deste repositório
2. Abra o **PowerShell** (pesquise "PowerShell" no menu Iniciar)
3. Rode o comando abaixo para extrair o projeto direto no `C:\`:

```powershell
Expand-Archive -Path "$env:USERPROFILE\Downloads\scp-app.zip" -DestinationPath "C:\" -Force
```

4. Confirma que deu certo rodando:

```powershell
dir C:\scp-app\scp-app\pom.xml
```

Se aparecer o arquivo `pom.xml` na listagem, está no lugar certo.

> **Por que o C:\?** Para facilitar o acesso — o caminho fica curto e sem caracteres especiais como acentos, o que evita erros no terminal.

---

## Como usar o app no dia a dia

Toda vez que precisar transferir arquivos, siga esses passos:

### 1. Abrir o PowerShell

Pesquise **"PowerShell"** no menu Iniciar e abre.

---

### 2. Entrar na pasta do projeto

```powershell
cd C:\scp-app\scp-app
```

Confirma que está no lugar certo rodando:

```powershell
dir
```

Deve aparecer o arquivo `pom.xml` na listagem. Se aparecer, está certo.

---

### 3. Testar se consegue alcançar a máquina do colaborador

Substitui pelo IP que o colaborador te passou e roda:

```powershell
ping 192.168.1.XX
```

**Se aparecer "Resposta de..."** → está tudo certo, pode continuar.

**Se aparecer "Esgotado o tempo"** → há um bloqueio de rede entre as máquinas. Nesse caso acione o TI da empresa.

---

### 4. Abrir o app

```powershell
mvn javafx:run
```

Aguarda alguns segundos — a janela do **SCP File Transfer** vai abrir.

> Na primeira vez pode demorar um pouco mais porque o Maven vai baixar as dependências do projeto. Da segunda vez em diante é rápido.

---

### 5. Conectar na máquina do colaborador

Na janela do app, preenche:

- **Host:** o IP da máquina do colaborador (ex: `192.168.1.45`)
- **Usuário:** o usuário Windows dele (ex: `joao.silva`)
- **Senha:** a senha de login do Windows dele

Clica em **Conectar**.

Se o status ficar **verde** escrito "Conectado" — está pronto para transferir.

---

### 6. Transferir arquivos

**Para enviar um arquivo para ele:**
1. Na seção **Upload**, clica em **Escolher arquivo...**
2. Seleciona o arquivo na sua máquina
3. No campo **"Destino no servidor"**, digita onde salvar no PC dele
   - Exemplo: `C:/Users/joao.silva/Desktop/relatorio.xlsx`
4. Clica em **Fazer Upload**

**Para baixar um arquivo dele:**
1. Na seção **Download**, no primeiro campo digita o caminho do arquivo no PC dele
   - Exemplo: `C:/Users/joao.silva/Documents/planilha.xlsx`
2. No segundo campo digita onde salvar na sua máquina
   - Exemplo: `C:/Users/netod/Downloads/planilha.xlsx`
3. Clica em **Fazer Download**

O status no rodapé da janela mostra se deu certo ou se houve algum erro.

---

## Problemas comuns

| O que aconteceu | O que fazer |
|---|---|
| `ping` não responde | Acione o TI — há bloqueio de rede entre as máquinas |
| "Auth fail" ao conectar | Usuário ou senha incorretos — confirma com o colaborador |
| "Connection refused" | O SSH não está rodando na máquina dele — peça para ele rodar o script novamente |
| O IP do colaborador mudou | Peça para ele abrir o PowerShell e rodar `ipconfig` — o novo IP estará em "Endereço IPv4" |
| `mvn` não é reconhecido | Feche e reabra o PowerShell após rodar o script de setup |

---

## Estrutura do repositório

```
scp-app.zip                          — o app completo (baixe e extraia em C:\)
setup-minha-maquina.ps1             — rode na SUA máquina uma única vez
setup-maquina-colaborador.ps1       — rode na máquina do colaborador uma única vez
README.md                            — este arquivo
```

---

## Observações

- Os scripts de setup só precisam ser rodados **uma vez** em cada máquina.
- O SSH do colaborador está configurado para iniciar automaticamente — ele não precisa fazer nada toda vez.
- Se precisar acessar uma máquina diferente no futuro, basta rodar o `setup-maquina-colaborador.ps1` nela e repetir o processo.
