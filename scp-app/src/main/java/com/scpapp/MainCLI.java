package com.scpapp;

import java.util.Scanner;

/**
 * MainCLI — Interface de linha de comando (etapas 2 e 3).
 *
 * Esta classe mostra um menu no terminal e deixa o usuário escolher
 * o que fazer: conectar, fazer upload, download, etc.
 *
 * O método main() é o PONTO DE ENTRADA de qualquer programa Java.
 * Quando você roda o programa, o Java procura e executa main() primeiro.
 *
 * Para rodar esta versão (sem GUI):
 *   mvn compile exec:java -Dexec.mainClass="com.scpapp.MainCLI"
 */
public class MainCLI {

    public static void main(String[] args) {
        // Scanner lê o que o usuário digita no terminal (entrada padrão = System.in)
        Scanner scanner = new Scanner(System.in);

        // Variável que vai guardar o SSHManager após a conexão ser feita
        SSHManager ssh = null;

        System.out.println("=== SCP App — Interface de Linha de Comando ===\n");

        // Loop principal: fica rodando até o usuário escolher "Sair"
        boolean rodando = true;
        while (rodando) {

            // Exibe o menu
            System.out.println("\nEscolha uma opção:");
            System.out.println("1. Conectar ao servidor");
            System.out.println("2. Upload (local → servidor)");
            System.out.println("3. Download (servidor → local)");
            System.out.println("4. Executar comando remoto");
            System.out.println("5. Sair");
            System.out.print("\nOpção: ");

            // Lê a opção do usuário como texto (nextLine é mais seguro que nextInt)
            String opcao = scanner.nextLine().trim();

            // switch/case: executa um bloco diferente dependendo do valor de "opcao"
            switch (opcao) {

                case "1":
                    // -------------------------------------------------------
                    // CONECTAR
                    // -------------------------------------------------------
                    System.out.print("Host (IP ou hostname): ");
                    String host = scanner.nextLine().trim();

                    System.out.print("Usuário: ");
                    String usuario = scanner.nextLine().trim();

                    System.out.print("Senha: ");
                    String senha = scanner.nextLine().trim();

                    try {
                        // Cria o SSHManager e abre a conexão
                        ssh = new SSHManager(host, usuario, senha);
                        ssh.conectar();
                    } catch (Exception e) {
                        System.out.println("✗ Erro ao conectar: " + e.getMessage());
                        ssh = null; // Garante que ssh só fica não-nulo se a conexão foi bem
                    }
                    break; // Sai do case, volta para o topo do while

                case "2":
                    // -------------------------------------------------------
                    // UPLOAD
                    // -------------------------------------------------------
                    if (!verificarConectado(ssh)) break;

                    System.out.print("Caminho local do arquivo: ");
                    String caminhoLocal = scanner.nextLine().trim();

                    System.out.print("Caminho remoto de destino: ");
                    String caminhoRemoto = scanner.nextLine().trim();

                    try {
                        ssh.upload(caminhoLocal, caminhoRemoto);
                    } catch (Exception e) {
                        System.out.println("✗ Erro no upload: " + e.getMessage());
                    }
                    break;

                case "3":
                    // -------------------------------------------------------
                    // DOWNLOAD
                    // -------------------------------------------------------
                    if (!verificarConectado(ssh)) break;

                    System.out.print("Caminho remoto do arquivo: ");
                    String remoto = scanner.nextLine().trim();

                    System.out.print("Caminho local de destino: ");
                    String local = scanner.nextLine().trim();

                    try {
                        ssh.download(remoto, local);
                    } catch (Exception e) {
                        System.out.println("✗ Erro no download: " + e.getMessage());
                    }
                    break;

                case "4":
                    // -------------------------------------------------------
                    // EXECUTAR COMANDO REMOTO
                    // -------------------------------------------------------
                    if (!verificarConectado(ssh)) break;

                    System.out.print("Comando a executar no servidor: ");
                    String comando = scanner.nextLine().trim();

                    try {
                        String resultado = ssh.executarComando(comando);
                        System.out.println("Saída:\n" + resultado);
                    } catch (Exception e) {
                        System.out.println("✗ Erro ao executar comando: " + e.getMessage());
                    }
                    break;

                case "5":
                    // -------------------------------------------------------
                    // SAIR
                    // -------------------------------------------------------
                    try {
                        if (ssh != null) ssh.desconectar();
                    } catch (Exception e) {
                        System.out.println("Aviso ao desconectar: " + e.getMessage());
                    }
                    rodando = false; // Para o loop principal
                    System.out.println("Até mais!");
                    break;

                default:
                    // Opção inválida
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }

        scanner.close(); // Boa prática: fecha o Scanner ao terminar
    }

    // -------------------------------------------------------------------------
    // MÉTODO AUXILIAR
    // -------------------------------------------------------------------------

    /**
     * Verifica se o usuário já conectou antes de tentar usar SSH.
     * Retorna true se está conectado, false se não.
     *
     * "private static" significa:
     *   - private: só usado dentro desta classe
     *   - static: pertence à classe, não a um objeto específico
     *             (por isso pode ser chamado sem criar um "new MainCLI()")
     */
    private static boolean verificarConectado(SSHManager ssh) {
        if (ssh == null) {
            System.out.println("✗ Você precisa conectar primeiro (opção 1).");
            return false;
        }
        return true;
    }
}
