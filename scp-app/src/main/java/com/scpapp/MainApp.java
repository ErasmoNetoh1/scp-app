package com.scpapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainApp - Interface grafica com JavaFX.
 *
 * Melhorias nesta versao:
 *   - ScrollPane envolve todo o conteudo: a janela pode ser qualquer tamanho
 *     e o usuario consegue rolar para ver tudo
 *   - Download agora tem botao "Escolher pasta..." para selecionar o destino
 *     graficamente, sem precisar digitar o caminho manualmente
 *   - Janela redimensionavel: o usuario pode arrastar para o tamanho que quiser
 */
public class MainApp extends Application {

    // Campos de conexao
    private TextField campHost;
    private TextField campUsuario;
    private PasswordField campSenha;

    // Labels de status
    private Label labelStatus;
    private Label labelConexao;

    // Gerenciador SSH (null se nao conectado)
    private SSHManager ssh;

    // Roda operacoes de rede em background para nao travar a janela
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        // ------------------------------------------------------------------
        // CONFIGURACAO DA JANELA
        // Agora e redimensionavel e tem tamanho minimo para nao ficar pequena
        // ------------------------------------------------------------------
        stage.setTitle("SCP App");
        stage.setWidth(520);
        stage.setHeight(620);
        stage.setMinWidth(420);   // largura minima - nao deixa ficar menor que isso
        stage.setMinHeight(400);  // altura minima
        stage.setResizable(true); // usuario pode redimensionar arrastando a borda

        // ------------------------------------------------------------------
        // LAYOUT PRINCIPAL
        // ------------------------------------------------------------------
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f8f9fa;");

        // ------------------------------------------------------------------
        // TITULO
        // ------------------------------------------------------------------
        Label titulo = new Label("SCP File Transfer");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#1a1a2e"));

        // ------------------------------------------------------------------
        // SECAO: Conexao SSH
        // ------------------------------------------------------------------
        GridPane gridConexao = new GridPane();
        gridConexao.setHgap(10);
        gridConexao.setVgap(8);
        gridConexao.setPadding(new Insets(16));
        gridConexao.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #dee2e6;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;"
        );

        // Faz a coluna dos campos de texto crescer com a janela
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(70);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS); // esta coluna se expande
        gridConexao.getColumnConstraints().addAll(col0, col1);

        gridConexao.add(criarLabelSecao("Conexao SSH"), 0, 0, 2, 1);

        gridConexao.add(new Label("Host:"), 0, 1);
        campHost = new TextField("localhost");
        campHost.setPromptText("IP ou hostname");
        campHost.setMaxWidth(Double.MAX_VALUE);
        gridConexao.add(campHost, 1, 1);

        gridConexao.add(new Label("Usuario:"), 0, 2);
        campUsuario = new TextField();
        campUsuario.setPromptText("seu_usuario");
        campUsuario.setMaxWidth(Double.MAX_VALUE);
        gridConexao.add(campUsuario, 1, 2);

        gridConexao.add(new Label("Senha:"), 0, 3);
        campSenha = new PasswordField();
        campSenha.setMaxWidth(Double.MAX_VALUE);
        gridConexao.add(campSenha, 1, 3);

        labelConexao = new Label("Desconectado");
        labelConexao.setTextFill(Color.web("#dc3545"));
        gridConexao.add(labelConexao, 0, 4, 2, 1);

        Button btnConectar = criarBotao("Conectar", "#0d6efd");
        btnConectar.setMaxWidth(Double.MAX_VALUE);
        btnConectar.setOnAction(e -> acaoConectar());
        gridConexao.add(btnConectar, 0, 5, 2, 1);

        // ------------------------------------------------------------------
        // SECAO: Upload
        // Upload tem "Escolher arquivo..." para selecionar o arquivo local
        // e campo de texto para o caminho de destino no servidor
        // ------------------------------------------------------------------
        VBox boxUpload = criarCardSecao("Upload (sua maquina -> servidor)");

        TextField campArquivoLocal = new TextField();
        campArquivoLocal.setPromptText("Caminho do arquivo local");

        // Botao que abre o explorador de arquivos para escolher o arquivo
        Button btnEscolherArquivo = new Button("Escolher arquivo...");
        btnEscolherArquivo.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Selecionar arquivo para upload");
            File arquivo = fc.showOpenDialog(stage);
            if (arquivo != null) {
                campArquivoLocal.setText(arquivo.getAbsolutePath());
            }
        });

        TextField campDestinoRemoto = new TextField();
        campDestinoRemoto.setPromptText("Destino no servidor: C:/Users/usuario/arquivo.txt");

        Button btnUpload = criarBotao("Fazer Upload", "#198754");
        btnUpload.setMaxWidth(Double.MAX_VALUE);
        btnUpload.setOnAction(e -> acaoUpload(campArquivoLocal.getText(), campDestinoRemoto.getText()));

        // HBox coloca o campo de texto e o botao lado a lado
        HBox linhaArquivoLocal = new HBox(8, campArquivoLocal, btnEscolherArquivo);
        HBox.setHgrow(campArquivoLocal, Priority.ALWAYS);

        boxUpload.getChildren().addAll(linhaArquivoLocal, campDestinoRemoto, btnUpload);

        // ------------------------------------------------------------------
        // SECAO: Download
        // Download agora tem dois seletores graficos:
        //   1. Campo de texto para o caminho do arquivo no servidor remoto
        //   2. "Escolher pasta..." para selecionar onde salvar na sua maquina
        // ------------------------------------------------------------------
        VBox boxDownload = criarCardSecao("Download (servidor -> sua maquina)");

        // Campo para o caminho do arquivo no servidor (ainda e texto pois e remoto)
        TextField campOrigemRemota = new TextField();
        campOrigemRemota.setPromptText("Caminho no servidor: C:/Users/usuario/arquivo.txt");

        // Campo que mostra a pasta de destino escolhida
        TextField campDestinoLocal = new TextField();
        campDestinoLocal.setPromptText("Pasta de destino na sua maquina");

        // Botao que abre o explorador de PASTAS para escolher onde salvar
        // Usamos DirectoryChooser (nao FileChooser) pois o usuario escolhe a pasta,
        // e o nome do arquivo vem automaticamente do servidor
        Button btnEscolherPasta = new Button("Escolher pasta...");
        btnEscolherPasta.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Selecionar pasta de destino");

            // Tenta abrir na pasta Downloads do usuario por padrao
            File pastaDownloads = new File(System.getProperty("user.home") + "/Downloads");
            if (pastaDownloads.exists()) {
                dc.setInitialDirectory(pastaDownloads);
            }

            File pastaSelecionada = dc.showDialog(stage);
            if (pastaSelecionada != null) {
                // Monta o caminho completo: pasta escolhida + nome do arquivo do servidor
                String nomeArquivo = obterNomeArquivo(campOrigemRemota.getText());
                campDestinoLocal.setText(pastaSelecionada.getAbsolutePath() + File.separator + nomeArquivo);
            }
        });

        Button btnDownload = criarBotao("Fazer Download", "#6f42c1");
        btnDownload.setMaxWidth(Double.MAX_VALUE);
        btnDownload.setOnAction(e -> acaoDownload(campOrigemRemota.getText(), campDestinoLocal.getText()));

        HBox linhaDestinoLocal = new HBox(8, campDestinoLocal, btnEscolherPasta);
        HBox.setHgrow(campDestinoLocal, Priority.ALWAYS);

        boxDownload.getChildren().addAll(campOrigemRemota, linhaDestinoLocal, btnDownload);

        // ------------------------------------------------------------------
        // BARRA DE STATUS
        // Mostra mensagens de progresso e erro na parte de baixo
        // ------------------------------------------------------------------
        labelStatus = new Label("Pronto.");
        labelStatus.setWrapText(true);
        labelStatus.setStyle(
            "-fx-background-color: #212529;" +
            "-fx-text-fill: #adb5bd;" +
            "-fx-padding: 10;" +
            "-fx-font-family: monospace;" +
            "-fx-background-radius: 6;"
        );
        labelStatus.setMaxWidth(Double.MAX_VALUE);
        labelStatus.setMinHeight(50);

        // ------------------------------------------------------------------
        // MONTAGEM: junta todas as secoes no layout principal
        // ------------------------------------------------------------------
        root.getChildren().addAll(titulo, gridConexao, boxUpload, boxDownload, labelStatus);

        // VBox.setVgrow faz o root crescer verticalmente quando a janela e redimensionada
        VBox.setVgrow(boxUpload, Priority.NEVER);
        VBox.setVgrow(boxDownload, Priority.NEVER);

        // ------------------------------------------------------------------
        // SCROLLPANE: envolve todo o conteudo com scroll
        // Isso resolve o problema da tela pequena - o usuario pode rolar
        // para ver as secoes que ficam fora da area visivel
        // ------------------------------------------------------------------
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);   // o conteudo se ajusta a largura da janela
        scroll.setFitToHeight(false); // altura livre para o scroll funcionar
        scroll.setStyle("-fx-background-color: #f8f9fa; -fx-background: #f8f9fa;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);   // sem scroll horizontal
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // scroll vertical so quando precisar

        // Cria a cena com o ScrollPane como raiz
        Scene scene = new Scene(scroll);
        stage.setScene(scene);
        stage.show();
    }

    // -------------------------------------------------------------------------
    // ACOES DOS BOTOES
    // -------------------------------------------------------------------------

    private void acaoConectar() {
        String host = campHost.getText().trim();
        String usuario = campUsuario.getText().trim();
        String senha = campSenha.getText();

        if (host.isEmpty() || usuario.isEmpty()) {
            atualizarStatus("[ERRO] Preencha o host e o usuario.");
            return;
        }

        atualizarStatus("Conectando...");

        executor.submit(() -> {
            try {
                SSHManager novoSsh = new SSHManager(host, usuario, senha);
                novoSsh.conectar();
                ssh = novoSsh;

                Platform.runLater(() -> {
                    labelConexao.setText("Conectado em " + host);
                    labelConexao.setTextFill(Color.web("#198754"));
                    atualizarStatus("[OK] Conectado em " + host + " como " + usuario);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    ssh = null;
                    labelConexao.setText("Desconectado");
                    labelConexao.setTextFill(Color.web("#dc3545"));
                    atualizarStatus("[ERRO] " + e.getMessage());
                });
            }
        });
    }

    private void acaoUpload(String caminhoLocal, String caminhoRemoto) {
        if (!verificarConectado()) return;
        if (caminhoLocal.isEmpty() || caminhoRemoto.isEmpty()) {
            atualizarStatus("[ERRO] Escolha o arquivo e preencha o destino.");
            return;
        }

        atualizarStatus("Enviando " + obterNomeArquivo(caminhoLocal) + "...");

        executor.submit(() -> {
            try {
                ssh.upload(caminhoLocal, caminhoRemoto);
                Platform.runLater(() ->
                    atualizarStatus("[OK] Upload concluido: " + obterNomeArquivo(caminhoLocal))
                );
            } catch (Exception e) {
                Platform.runLater(() ->
                    atualizarStatus("[ERRO] Upload falhou: " + e.getMessage())
                );
            }
        });
    }

    private void acaoDownload(String caminhoRemoto, String caminhoLocal) {
        if (!verificarConectado()) return;
        if (caminhoRemoto.isEmpty() || caminhoLocal.isEmpty()) {
            atualizarStatus("[ERRO] Preencha o arquivo de origem e escolha a pasta de destino.");
            return;
        }

        atualizarStatus("Baixando " + obterNomeArquivo(caminhoRemoto) + "...");

        executor.submit(() -> {
            try {
                ssh.download(caminhoRemoto, caminhoLocal);
                Platform.runLater(() ->
                    atualizarStatus("[OK] Download concluido: " + obterNomeArquivo(caminhoRemoto))
                );
            } catch (Exception e) {
                Platform.runLater(() ->
                    atualizarStatus("[ERRO] Download falhou: " + e.getMessage())
                );
            }
        });
    }

    // -------------------------------------------------------------------------
    // METODOS AUXILIARES
    // -------------------------------------------------------------------------

    /**
     * Extrai apenas o nome do arquivo de um caminho completo.
     * Ex: "C:/Users/joao/relatorio.xlsx" -> "relatorio.xlsx"
     * Ex: "/home/ubuntu/dados.zip"       -> "dados.zip"
     */
    private String obterNomeArquivo(String caminho) {
        if (caminho == null || caminho.isEmpty()) return "";
        // Substitui barras invertidas por normais para tratar Windows e Linux igual
        String normalizado = caminho.replace("\\", "/");
        int ultimaBarra = normalizado.lastIndexOf("/");
        return ultimaBarra >= 0 ? normalizado.substring(ultimaBarra + 1) : normalizado;
    }

    private boolean verificarConectado() {
        if (ssh == null) {
            atualizarStatus("[ERRO] Conecte-se primeiro.");
            return false;
        }
        return true;
    }

    private void atualizarStatus(String mensagem) {
        labelStatus.setText(mensagem);
    }

    private Label criarLabelSecao(String texto) {
        Label label = new Label(texto);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#495057"));
        return label;
    }

    private Button criarBotao(String texto, String corHex) {
        Button btn = new Button(texto);
        btn.setStyle(
            "-fx-background-color: " + corHex + ";" +
            "-fx-text-fill: white;" +
            "-fx-padding: 8 16;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    private VBox criarCardSecao(String titulo) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #dee2e6;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;"
        );
        card.getChildren().add(criarLabelSecao(titulo));
        return card;
    }

    @Override
    public void stop() throws Exception {
        executor.shutdown();
        if (ssh != null) ssh.desconectar();
    }
}
