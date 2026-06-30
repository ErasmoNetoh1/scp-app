# SCP App

## O que e e o que resolve

No trabalho, eu precisava constantemente acessar a maquina de um colaborador para buscar ou enviar arquivos. O processo era manual, demorado e dependia de ferramentas externas como compartilhamento de tela, e-mail e pendrive. Cada transferencia virava uma tarefa por si so.

O SCP App resolve isso. É uma aplicacao desktop feita em Java que abre uma janela simples onde voce informa o IP e a senha da maquina do colaborador, e a partir dai faz upload e download de arquivos diretamente entre as duas maquinas via SSH, sem intermediarios, sem ferramentas extras e sem depender de ninguem.

---

## O que voce vai precisar

Nao precisa instalar nada manualmente. O script de setup instala tudo automaticamente.

| Programa      | Para que serve              |
| ------------- | --------------------------- |
| JDK 21        | Roda o codigo Java          |
| Apache Maven  | Compila e executa o projeto |
| JavaFX SDK 21 | Desenha a janela do app     |

---

## Configuracao inicial — faca isso uma unica vez

### Passo 1 — Baixar o repositorio

1. Acesse https://github.com/ErasmoNetoh1/ssh-app
2. Clique em **Code** -> **Download ZIP**
3. Extraia o ZIP baixado diretamente em `C:\`
4. Renomeie a pasta extraida para `ssh-app`

A estrutura deve ficar assim:

```
C:\ssh-app\
    ssh-app.zip
    setup-minha-maquina.ps1
    setup-maquina-colaborador.ps1
    README.md
```

---

### Passo 2 — Rodar o script de setup

1. Pesquise **PowerShell** no menu Iniciar
2. Clique com botao direito -> **Executar como Administrador**
3. Cole o comando abaixo e pressione Enter:

```powershell
Set-ExecutionPolicy Bypass -Scope CurrentUser -Force; & "C:\ssh-app\setup-minha-maquina.ps1"
```

4. Aguarde o script terminar. Ele vai instalar tudo e mostrar o progresso na tela.
5. Ao final pressione Enter para fechar e **abra um novo PowerShell**.

---

### Passo 3 — Preparar a maquina do colaborador

Faca isso **uma unica vez** na maquina de quem voce vai acessar remotamente.

1. Copie o arquivo `setup-maquina-colaborador.ps1` para a maquina dele (e-mail, Teams ou pendrive)
2. Abra o **PowerShell como Administrador** na maquina dele
3. Cole o comando abaixo trocando pelo caminho onde salvou o script:

```powershell
Set-ExecutionPolicy Bypass -Scope CurrentUser -Force; & "C:\caminho\setup-maquina-colaborador.ps1"
```

4. No final do script vai aparecer o **IP** e o **usuario** da maquina dele — anote os dois.

O colaborador nao precisa fazer mais nada depois disso. O SSH fica configurado para iniciar automaticamente toda vez que o PC ligar.

---

## Como usar o app no dia a dia

### 1. Abrir o PowerShell

Pesquise **PowerShell** no menu Iniciar e abra (nao precisa ser como Admin desta vez).

### 2. Entrar na pasta do projeto e abrir o app

```powershell
cd C:\ssh-app\ssh-app
mvn javafx:run
```

A janela do **SCP File Transfer** vai abrir em alguns segundos.

> Na primeira vez pode demorar mais porque o Maven baixa as dependencias do projeto. Da segunda vez em diante e rapido.

### 3. Conectar na maquina do colaborador

Na janela do app preenche:

- **Host:** o IP da maquina do colaborador (ex: `192.168.1.45`)
- **Usuario:** o usuario Windows dele
- **Senha:** a senha de login do Windows dele

Clica em **Conectar**. Se ficar verde esta pronto.

### 4. Transferir arquivos

**Para enviar um arquivo para ele (Upload):**

1. Seção Upload -> clica em **Escolher arquivo...**
2. Seleciona o arquivo na sua máquina
3. No campo destino digita onde salvar no PC dele, ex: `C:/Users/joao/Desktop/relatorio.xlsx`
4. Clica em **Fazer Upload**

**Para baixar um arquivo dele (Download):**

1. Seção Download -> digita o caminho do arquivo no PC dele, ex: `C:/Users/joao/Documents/planilha.xlsx`
2. Digita onde salvar na sua maquina, ex: `C:/Users/voce/Downloads/planilha.xlsx`
3. Clica em **Fazer Download**

---

## Problemas comuns

| O que aconteceu                     | O que fazer                                                                              |
| ----------------------------------- | ---------------------------------------------------------------------------------------- |
| Script fecha sozinho sem fazer nada | Abra o PowerShell como Admin e rode o comando do Passo 2 manualmente                     |
| `ping` não responde                 | Acione o TI, ha bloqueio de rede entre as maquinas                                       |
| "Auth fail" ao conectar             | Usuario ou senha incorretos, confirma com o colaborador                                  |
| "Connection refused"                | SSH não esta rodando na maquina dele, peca para rodar o script novamente                 |
| IP do colaborador mudou             | Peca para ele abrir o PowerShell e rodar `ipconfig`, o novo IP estara em "Endereco IPv4" |
| `mvn` não reconhecido               | Feche e reabra o PowerShell apos rodar o script de setup                                 |

---

## Estrutura do repositorio

```
ssh-app.zip                        — o app completo
setup-minha-maquina.ps1           — rode na SUA maquina uma unica vez
setup-maquina-colaborador.ps1     — rode na maquina do colaborador uma unica vez
README.md                          — este arquivo
```

- Os scripts de setup só precisam ser rodados **uma vez** em cada máquina.
- O SSH do colaborador está configurado para iniciar automaticamente ele não precisa fazer nada toda vez.
- Se precisar acessar uma máquina diferente no futuro, basta rodar o `setup-maquina-colaborador.ps1` nela e repetir o processo.
