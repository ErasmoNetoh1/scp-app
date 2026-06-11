package com.scpapp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * SSHManager é responsável por TODA a comunicação SSH/SCP com o servidor.
 *
 * Conceito importante — o que é uma "classe"?
 *   Uma classe é como uma "planta baixa" de um objeto.
 *   Aqui, SSHManager é o projeto de um objeto que sabe conectar em servidores SSH.
 *   Você cria um objeto a partir dessa classe com: new SSHManager("ip", "user", "senha")
 */
public class SSHManager {

    // -------------------------------------------------------------------------
    // CAMPOS (atributos do objeto)
    // Guardam as informações de conexão. "private" significa que só esta
    // classe pode acessá-los diretamente — boa prática de encapsulamento.
    // -------------------------------------------------------------------------

    private final String host;     // IP ou hostname do servidor
    private final String username; // Usuário SSH (ex: "root", "ubuntu")
    private final String password; // Senha do usuário

    // SSHClient é o objeto da lib SSHJ que representa a conexão em si
    private SSHClient ssh;

    // -------------------------------------------------------------------------
    // CONSTRUTOR
    // Método especial chamado quando você faz: new SSHManager(...)
    // Recebe os parâmetros de conexão e os guarda nos campos acima.
    // -------------------------------------------------------------------------

    public SSHManager(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }

    // -------------------------------------------------------------------------
    // CONECTAR
    // -------------------------------------------------------------------------

    /**
     * Abre a conexão SSH com o servidor.
     * Deve ser chamado antes de qualquer upload, download ou comando.
     *
     * "throws IOException" significa que esse método pode lançar um erro
     * (ex: se o servidor estiver offline). Quem chamar esse método precisa
     * tratar esse erro com try/catch.
     */
    public void conectar() throws IOException {
        ssh = new SSHClient();

        // PromiscuousVerifier aceita qualquer chave do servidor sem verificar.
        // Em produção você usaria um arquivo known_hosts, mas para aprendizado
        // isso simplifica bastante.
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        // Abre a conexão TCP na porta 22 (porta padrão do SSH)
        ssh.connect(host);

        // Faz a autenticação com usuário e senha
        ssh.authPassword(username, password);

        System.out.println("✓ Conectado em " + host + " como " + username);
    }

    // -------------------------------------------------------------------------
    // DESCONECTAR
    // -------------------------------------------------------------------------

    /**
     * Fecha a conexão com o servidor.
     * Sempre chame isso ao terminar — é como fechar um arquivo depois de usar.
     */
    public void desconectar() throws IOException {
        if (ssh != null && ssh.isConnected()) {
            ssh.disconnect();
            System.out.println("✓ Desconectado de " + host);
        }
    }

    // -------------------------------------------------------------------------
    // UPLOAD (SCP: local → servidor)
    // -------------------------------------------------------------------------

    /**
     * Envia um arquivo da sua máquina para o servidor remoto.
     *
     * @param caminhoLocal   Caminho do arquivo na sua máquina. Ex: "C:/foto.jpg"
     * @param caminhoRemoto  Onde salvar no servidor.       Ex: "/home/user/foto.jpg"
     */
    public void upload(String caminhoLocal, String caminhoRemoto) throws IOException {
        // SFTPClient é o protocolo que roda sobre SSH para transferência de arquivos
        // O "try-with-resources" garante que o sftp.close() é chamado automaticamente
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.put(caminhoLocal, caminhoRemoto);
            System.out.println("✓ Upload concluído: " + caminhoLocal + " → " + caminhoRemoto);
        }
    }

    // -------------------------------------------------------------------------
    // DOWNLOAD (SCP: servidor → local)
    // -------------------------------------------------------------------------

    /**
     * Baixa um arquivo do servidor para a sua máquina.
     *
     * @param caminhoRemoto  Caminho do arquivo no servidor. Ex: "/var/log/app.log"
     * @param caminhoLocal   Onde salvar na sua máquina.    Ex: "C:/Downloads/app.log"
     */
    public void download(String caminhoRemoto, String caminhoLocal) throws IOException {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.get(caminhoRemoto, caminhoLocal);
            System.out.println("✓ Download concluído: " + caminhoRemoto + " → " + caminhoLocal);
        }
    }

    // -------------------------------------------------------------------------
    // EXECUTAR COMANDO REMOTO (usado na etapa 6 para o "unzip")
    // -------------------------------------------------------------------------

    /**
     * Executa um comando no terminal do servidor remoto e retorna o resultado.
     * Exemplo de uso: executarComando("unzip arquivo.zip arquivo_interno.txt")
     *
     * @param comando  O comando shell que será executado no servidor
     * @return         A saída do comando (o que apareceria no terminal)
     */
    public String executarComando(String comando) throws IOException {
        // Session representa uma sessão de terminal no servidor
        try (Session session = ssh.startSession()) {
            // Executa o comando
            Command cmd = session.exec(comando);

            // Lê a saída do comando linha por linha
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(cmd.getInputStream())
            );

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

    // -------------------------------------------------------------------------
    // GETTER (método para ler um campo privado de fora da classe)
    // -------------------------------------------------------------------------

    /**
     * Retorna o host atual. A GUI usa isso para exibir o status da conexão.
     */
    public String getHost() {
        return host;
    }
}
