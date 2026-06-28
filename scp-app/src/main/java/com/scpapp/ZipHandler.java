package com.scpapp;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipHandler {

    // Limite em bytes
    private static final long LIMITE_ZIP_LEVE = 10 * 1024 * 1024;

    private final SSHManager ssh;

    // exige conexão SSH para operar
    public ZipHandler(SSHManager ssh) {
        this.ssh = ssh;
    }

    // MÉTODO PRINCIPAL: escolhe a estratégia automaticamente
    public void extrairArquivoDeZip(
            String caminhoZipRemoto,
            String arquivoDentroDoZip,
            String destinoLocal) throws IOException {

        // Verifica o tamanho do zip no servidor antes de decidir
        long tamanhoBytes = obterTamanhoRemoto(caminhoZipRemoto);

        System.out.println("Tamanho do zip: " + (tamanhoBytes / 1024 / 1024) + " MB");

        if (tamanhoBytes < LIMITE_ZIP_LEVE) {
            System.out.println("Estratégia: zip leve — baixando o zip inteiro");
            estrategiaZipLeve(caminhoZipRemoto, arquivoDentroDoZip, destinoLocal);
        } else {
            System.out.println("Estratégia: zip pesado — extraindo no servidor");
            estrategiaZipPesado(caminhoZipRemoto, arquivoDentroDoZip, destinoLocal);
        }
    }

    // Zip leve — baixa e extrai localmente

    private void estrategiaZipLeve(
            String caminhoZipRemoto,
            String arquivoDentroDoZip,
            String destinoLocal) throws IOException {

        // Caminho temporário para salvar o zip baixado
        String zipTemp = destinoLocal + "_temp.zip";

        try {
            // Passo 1: baixa o zip inteiro do servidor
            ssh.download(caminhoZipRemoto, zipTemp);

            // Passo 2: abre o zip baixado e extrai apenas o arquivo desejado
            extrairArquivoDoZipLocal(zipTemp, arquivoDentroDoZip, destinoLocal);

        } finally {
            // Limpa o arquivo temporário
            new File(zipTemp).delete();
        }
    }

    private void extrairArquivoDoZipLocal(
            String caminhoZipLocal,
            String nomeArquivo,
            String destino) throws IOException {

        // ZipInputStream lê um arquivo .zip entrada por entrada
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(caminhoZipLocal))) {

            ZipEntry entrada; // Representa cada arquivo dentro do zip

            // Itera sobre cada arquivo dentro do zip
            while ((entrada = zis.getNextEntry()) != null) {

                // Verifica se este é o arquivo que queremos
                if (entrada.getName().equals(nomeArquivo)) {

                    // Cria o arquivo de destino e copia os bytes
                    try (FileOutputStream fos = new FileOutputStream(destino)) {
                        byte[] buffer = new byte[4096]; // lê 4KB por vez
                        int bytesLidos;
                        while ((bytesLidos = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesLidos);
                        }
                    }

                    System.out.println("✓ Arquivo extraído: " + nomeArquivo);
                    return; // Encontrou o arquivo, pode parar
                }

                zis.closeEntry(); // Fecha a entrada atual antes de ir para a próxima
            }

            // Se chegou aqui, o arquivo não foi encontrado no zip
            throw new IOException("Arquivo '" + nomeArquivo + "' não encontrado dentro do zip");
        }
    }

    // Zip pesado — extrai no servidor, baixa só o que precisa

    private void estrategiaZipPesado(
            String caminhoZipRemoto,
            String arquivoDentroDoZip,
            String destinoLocal) throws IOException {

        // Pasta temporária no servidor onde o arquivo será extraído
        String pastaTemp = "/tmp/scp_app_temp";
        String arquivoExtraido = pastaTemp + "/" + arquivoDentroDoZip;

        // Cria a pasta temporária no servidor
        ssh.executarComando("mkdir -p " + pastaTemp);

        // extrai APENAS o arquivo desejado no servidor
        // O comando "unzip -j" extrai sem criar subpastas (-j = "junk paths")
        String resultadoUnzip = ssh.executarComando(
                "unzip -j " + caminhoZipRemoto + " " + arquivoDentroDoZip + " -d " + pastaTemp);
        System.out.println("Saída do unzip: " + resultadoUnzip);

        // Baixa o arquivo extraído para a máquina local
        ssh.download(arquivoExtraido, destinoLocal);

        // Deleta o arquivo temporário do servidor (limpeza)
        ssh.executarComando("rm -rf " + pastaTemp);

        System.out.println("✓ Arquivo extraído do zip pesado: " + arquivoDentroDoZip);
    }

    // UTILITÁRIO: descobre o tamanho de um arquivo no servidor

    private long obterTamanhoRemoto(String caminhoArquivo) throws IOException {
        // Executa "stat" no servidor — retorna informações do arquivo
        // O formato "%s" retorna só o tamanho em bytes
        String saida = ssh.executarComando("stat -c %s " + caminhoArquivo);
        try {
            return Long.parseLong(saida.trim());
        } catch (NumberFormatException e) {
            // Se não conseguiu ler o tamanho, assume zip pesado (estratégia segura)
            System.out.println("Não foi possível ler o tamanho do arquivo. Usando estratégia pesada.");
            return Long.MAX_VALUE;
        }
    }
}
