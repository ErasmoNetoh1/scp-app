package com.scpapp;

import java.util.Scanner;

public class MainCLI {

    public static void main(String[] args) {
        // Scanner lê o que o usuário digita no terminal (entrada padrão = System.in)
        Scanner scanner = new Scanner(System.in);

        // guarda o SSHManager após a conexão ser feita
        SSHManager ssh = null;

        System.out.println("=== SCP App — Interface de Linha de Comando ===\n");

        // Loop principal: roda até o usuário sair
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

            // le a opção do usuário
            String opcao = scanner.nextLine().trim();

            switch (opcao) {

                case "1":

                    // CONECTAR
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
                        ssh = null;
                    }
                    break; // Sai do case, volta para o topo do while

                case "2":

                    // UPLOAD
                    if (!verificarConectado(ssh))
                        break;

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

                    // DOWNLOAD
                    if (!verificarConectado(ssh))
                        break;

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

                    // EXECUTAR COMANDO REMOTO
                    if (!verificarConectado(ssh))
                        break;

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

                    // SAIR
                    try {
                        if (ssh != null)
                            ssh.desconectar();
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
        scanner.close();
    }

    // MÉTODO AUXILIAR

    // Verifica conexão SSH antes de realizar operações
    private static boolean verificarConectado(SSHManager ssh) {
        if (ssh == null) {
            System.out.println("✗ Você precisa conectar primeiro (opção 1).");
            return false;
        }
        return true;
    }
}
