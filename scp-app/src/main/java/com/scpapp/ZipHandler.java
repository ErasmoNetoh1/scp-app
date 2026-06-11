package com.scpapp;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZipHandler cuida de tudo relacionado a arquivos .zip.
 *
 * Implementa a lógica das duas alternativas da etapa 6:
 *   - Zip leve (< 10MB): baixa o zip inteiro e extrai localmente
 *   - Zip pesado (> 10MB): extrai no servidor via SSH, baixa só o arquivo, deleta do servidor
 */
public class ZipHandler {

    // Limite em bytes para decidir a estratégia (10 MB = 10 * 1024 * 1024)
    private static final long LIMITE_ZIP_LEVE = 10 * 1024 * 1024;

    private final SSHManager ssh;

    /**
     * ZipHandler precisa de um SSHManager já conectado para funcionar.
     */
    public ZipHandler(SSHManager ssh) {
        this.ssh = ssh;
    }

    // -------------------------------------------------------------------------
    // MÉTODO PRINCIPAL: escolhe a estratégia automaticamente
    // -------------------------------------------------------------------------

    /**
     * Extrai um arquivo específico de dentro de um .zip remoto.
     * Escolhe a estratégia (leve ou pesada) baseado no tamanho do zip.
     *
     * @param caminhoZipRemoto   Caminho do .zip no servidor. Ex: "/dados/backup.zip"
     * @param arquivoDentroDoZip Nome do arquivo que você quer extrair. Ex: "relatorio.pdf"
     * @param destinoLocal       Onde salvar na sua máquina. Ex: "C:/Downloads/relatorio.pdf"
     */
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

    // -------------------------------------------------------------------------
    // ALTERNATIVA 1: Zip leve — baixa e extrai localmente
    // -------------------------------------------------------------------------

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
            // Limpa o arquivo temporário, mesmo se der erro acima
            // "finally" executa SEMPRE, com sucesso ou com erro
            new File(zipTemp).delete();
        }
    }

    /**
     * Extrai um arquivo específico de um .zip local.
     * Usa ZipInputStream — biblioteca nativa do Java, sem dependência extra.
     */
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

    // -------------------------------------------------------------------------
    // ALTERNATIVA 2: Zip pesado — extrai no servidor, baixa só o que precisa
    // -------------------------------------------------------------------------

    private void estrategiaZipPesado(
            String caminhoZipRemoto,
            String arquivoDentroDoZip,
            String destinoLocal) throws IOException {

        // Pasta temporária no servidor onde o arquivo será extraído
        String pastaTemp = "/tmp/scp_app_temp";
        String arquivoExtraido = pastaTemp + "/" + arquivoDentroDoZip;

        // Passo 1: cria a pasta temporária no servidor
        ssh.executarComando("mkdir -p " + pastaTemp);

        // Passo 2: extrai APENAS o arquivo desejado no servidor
        // O comando "unzip -j" extrai sem criar subpastas (-j = "junk paths")
        String resultadoUnzip = ssh.executarComando(
            "unzip -j " + caminhoZipRemoto + " " + arquivoDentroDoZip + " -d " + pastaTemp
        );
        System.out.println("Saída do unzip: " + resultadoUnzip);

        // Passo 3: baixa o arquivo extraído para a máquina local
        ssh.download(arquivoExtraido, destinoLocal);

        // Passo 4: deleta o arquivo temporário do servidor (limpeza)
        ssh.executarComando("rm -rf " + pastaTemp);

        System.out.println("✓ Arquivo extraído do zip pesado: " + arquivoDentroDoZip);
    }

    // -------------------------------------------------------------------------
    // UTILITÁRIO: descobre o tamanho de um arquivo no servidor
    // -------------------------------------------------------------------------

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
