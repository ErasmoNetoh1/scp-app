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

        // configuração da janela
        stage.setTitle("SCP App");
        stage.setWidth(520);
        stage.setHeight(620);
        stage.setMinWidth(420);
        stage.setMinHeight(400);
        stage.setResizable(true);

        // layout
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f8f9fa;");

        // titulo
        Label titulo = new Label("SCP File Transfer");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#1a1a2e"));

        // conexao SSH
        GridPane gridConexao = new GridPane();
        gridConexao.setHgap(10);
        gridConexao.setVgap(8);
        gridConexao.setPadding(new Insets(16));
        gridConexao.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");

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

        // Seção Upload (Local -> Servidor)
        VBox boxUpload = criarCardSecao("Upload (Local -> Servidor)");

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

        Button btnEscolherPasta = new Button("Escolher pasta...");
        btnEscolherPasta.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Selecionar pasta de destino");
            File pasta = dc.showDialog(stage);
            if (pasta != null) {
                campDestinoRemoto.setText(pasta.getAbsolutePath());
            }
        });

        Button btnUpload = criarBotao("Fazer Upload", "#198754");
        btnUpload.setMaxWidth(Double.MAX_VALUE);
        btnUpload.setOnAction(e -> acaoUpload(campArquivoLocal.getText(), campDestinoRemoto.getText()));

        // HBox coloca o campo de texto e o botao lado a lado
        HBox linhaArquivoLocal = new HBox(8, campArquivoLocal, btnEscolherArquivo);
        HBox.setHgrow(campArquivoLocal, Priority.ALWAYS);

        HBox linhaArquivoDestino = new HBox(8, campDestinoRemoto, btnEscolherPasta);
        HBox.setHgrow(campDestinoRemoto, Priority.ALWAYS);

        boxUpload.getChildren().addAll(linhaArquivoLocal, linhaArquivoDestino, btnUpload);

        // Seção Download (Servidor -> Local)
        VBox boxDownload = criarCardSecao("Download (Servidor -> Local)");

        // Campo para o caminho do arquivo no servidor (ainda e texto pois e remoto)
        TextField campOrigemRemota = new TextField();
        campOrigemRemota.setPromptText("Caminho no servidor: C:/Users/usuario/arquivo.txt");

        Button btnEscolherArquivoRemoto = new Button("Escolher arquivo...");
        btnEscolherArquivoRemoto.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Selecionar arquivo no servidor");
            File arquivo = fc.showOpenDialog(stage);
            if (arquivo != null) {
                campOrigemRemota.setText(arquivo.getAbsolutePath());
            }
        });

        HBox linhaOrigemRemota = new HBox(8, campOrigemRemota, btnEscolherArquivoRemoto);
        HBox.setHgrow(campOrigemRemota, Priority.ALWAYS);

        // Campo que mostra a pasta de destino escolhida
        TextField campDestinoLocal = new TextField();
        campDestinoLocal.setPromptText("Pasta de destino na sua maquina");

        Button btnEscolherPastaD = new Button("Escolher pasta...");
        btnEscolherPastaD.setOnAction(e -> {
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

        HBox linhaDestinoDLocal = new HBox(8, campDestinoLocal, btnEscolherPastaD);
        HBox.setHgrow(campDestinoLocal, Priority.ALWAYS);

        boxDownload.getChildren().addAll(linhaOrigemRemota, linhaDestinoDLocal, btnDownload);

        // barra de status
        labelStatus = new Label("Pronto.");
        labelStatus.setWrapText(true);
        labelStatus.setStyle(
                "-fx-background-color: #212529;" +
                        "-fx-text-fill: #adb5bd;" +
                        "-fx-padding: 10;" +
                        "-fx-font-family: monospace;" +
                        "-fx-background-radius: 6;");
        labelStatus.setMaxWidth(Double.MAX_VALUE);
        labelStatus.setMinHeight(50);

        // junta todas as secoes no layout principal
        root.getChildren().addAll(titulo, gridConexao, boxUpload, boxDownload, labelStatus);

        // faz o root crescer verticalmente quando a janela é redimensionada
        VBox.setVgrow(boxUpload, Priority.NEVER);
        VBox.setVgrow(boxDownload, Priority.NEVER);

        // envolve todo o conteudo com scroll
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true); // o conteudo se ajusta a largura da janela
        scroll.setFitToHeight(false); // altura livre para o scroll funcionar
        scroll.setStyle("-fx-background-color: #f8f9fa; -fx-background: #f8f9fa;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // sem scroll horizontal
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // scroll vertical so quando precisar

        // Cria a cena com o ScrollPane como raiz
        Scene scene = new Scene(scroll);
        stage.setScene(scene);
        stage.show();
    }

    // ações dos botões
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
        if (!verificarConectado())
            return;
        if (caminhoLocal.isEmpty() || caminhoRemoto.isEmpty()) {
            atualizarStatus("[ERRO] Escolha o arquivo e preencha o destino.");
            return;
        }

        atualizarStatus("Enviando " + obterNomeArquivo(caminhoLocal) + "...");

        executor.submit(() -> {
            try {
                ssh.upload(caminhoLocal, caminhoRemoto);
                Platform.runLater(() -> atualizarStatus("[OK] Upload concluido: " + obterNomeArquivo(caminhoLocal)));
            } catch (Exception e) {
                Platform.runLater(() -> atualizarStatus("[ERRO] Upload falhou: " + e.getMessage()));
            }
        });
    }

    private void acaoDownload(String caminhoRemoto, String caminhoLocal) {
        if (!verificarConectado())
            return;
        if (caminhoRemoto.isEmpty() || caminhoLocal.isEmpty()) {
            atualizarStatus("[ERRO] Preencha o arquivo de origem e escolha a pasta de destino.");
            return;
        }

        atualizarStatus("Baixando " + obterNomeArquivo(caminhoRemoto) + "...");

        executor.submit(() -> {
            try {
                ssh.download(caminhoRemoto, caminhoLocal);
                Platform.runLater(() -> atualizarStatus("[OK] Download concluido: " + obterNomeArquivo(caminhoRemoto)));
            } catch (Exception e) {
                Platform.runLater(() -> atualizarStatus("[ERRO] Download falhou: " + e.getMessage()));
            }
        });
    }

    // METODOS AUXILIARES

    // Extrai apenas o nome do arquivo de um caminho completo.
    private String obterNomeArquivo(String caminho) {
        if (caminho == null || caminho.isEmpty())
            return "";
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
                        "-fx-cursor: hand;");
        return btn;
    }

    private VBox criarCardSecao(String titulo) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");
        card.getChildren().add(criarLabelSecao(titulo));
        return card;
    }

    @Override
    public void stop() throws Exception {
        executor.shutdown();
        if (ssh != null)
            ssh.desconectar();
    }
}
