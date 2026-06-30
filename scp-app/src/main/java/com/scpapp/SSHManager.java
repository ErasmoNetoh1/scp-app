package com.scpapp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SSHManager {

    // CAMPOS (atributos do objeto)
    private final String host; // IP ou hostname do servidor
    private final String username; // Usuário SSH
    private final String password; // Senha do usuário

    // SSHClient representa a conexão em si
    private SSHClient ssh;

    // CONSTRUTOR
    // Recebe os parâmetros de conexão e os guarda nos campos acima.

    public SSHManager(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }

    // CONECTAR

    public void conectar() throws IOException {
        ssh = new SSHClient();

        // PromiscuousVerifier aceita qualquer chave do servidor sem verificar.
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        // Abre a conexão TCP na porta 22
        ssh.connect(host);

        // Faz a autenticação
        ssh.authPassword(username, password);

        System.out.println("Conectado em " + host + " como " + username);
    }

    // DESCONECTAR
    public void desconectar() throws IOException {
        if (ssh != null && ssh.isConnected()) {
            ssh.disconnect();
            System.out.println("Desconectado de " + host);
        }
    }

    // UPLOAD (SCP: local → servidor)
    public void upload(String caminhoLocal, String caminhoRemoto) throws IOException {
        // SFTPClient é o protocolo que roda sobre SSH para transferência de arquivos
        // O "try-with-resources" garante que o sftp.close() é chamado automaticamente
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.put(caminhoLocal, caminhoRemoto);
            System.out.println("Upload concluído: " + caminhoLocal + " → " + caminhoRemoto);
        }
    }

    // DOWNLOAD (SCP: servidor → local)
    public void download(String caminhoRemoto, String caminhoLocal) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.get(caminhoRemoto, caminhoLocal);
            System.out.println("Download concluído: " + caminhoRemoto + " → " + caminhoLocal);
        }
    }

    // EXECUTAR COMANDO REMOTO
    public String executarComando(String comando) throws IOException {
        // Session representa uma sessão de terminal no servidor
        try (Session session = ssh.startSession()) {
            // Executa o comando
            Command cmd = session.exec(comando);

            // Lê a saída do comando linha por linha
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(cmd.getInputStream()));

            StringBuilder resultado = new StringBuilder();
            String linha;
            while ((linha = reader.readLine()) != null) {
                resultado.append(linha).append("\n");
            }

            // Espera o comando terminar antes de retornar
            cmd.join();

            return resultado.toString();
        }
    }

    // GETTER (método para ler um campo privado de fora da classe)

    public String getHost() {
        return host;
    }
}
