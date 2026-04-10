package p2Exercice1_2_3.client;

import java.awt.Dimension;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import stree.parser.SNode;
import stree.parser.SParser;
import graphicLayer.*;
import p2Exercice1_2_3.shared.SNodeSerializer;
import exercice6.*;
import tools.Tools;

public class SExpressionClient {
    public static final String DEFAULT_SERVER_URL = "http://localhost:4444";

    private final String serverUrl;
    private final Environment environment;
    private final GSpace space;
    private long localVersion = 0;

    public SExpressionClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.space = new GSpace("Partie 2 ex1,2,3 - Rendu Client", new Dimension(500, 400));        
        this.environment = new Environment();

        // 1. Initialisation de l'environnement (Commandes)
        setupEnvironment();

        // 2. Lancement de l'unique Thread de synchro (vérifie toutes les secondes)
        startAutoSyncThread();
    }

    private void setupEnvironment() {
        Reference spaceRef = new Reference(space);
        Reference rectClassRef = new Reference(GRect.class);
        Reference ovalClassRef = new Reference(GOval.class);
        Reference imageClassRef = new Reference(GImage.class);
        Reference stringClassRef = new Reference(GString.class);

        spaceRef.addCommand("setColor",  new SetColor());
        spaceRef.addCommand("sleep",     new Sleep());
        spaceRef.addCommand("setDim",    new SetDim());
        spaceRef.addCommand("add",       new AddElement(environment));
        spaceRef.addCommand("del",       new DelElement(environment));
        spaceRef.addCommand("clear",     new Clear());
        spaceRef.addCommand("addScript", new AddScript(environment));

        rectClassRef.addCommand("new",   new NewElement());
        ovalClassRef.addCommand("new",   new NewElement());
        imageClassRef.addCommand("new",  new NewImage());
        stringClassRef.addCommand("new", new NewString());

        environment.addReference("space", spaceRef);
        environment.addReference("Rect",  rectClassRef);
        environment.addReference("Oval",  ovalClassRef);
        environment.addReference("Image", imageClassRef);
        environment.addReference("Label", stringClassRef);
    }

    private void startAutoSyncThread() {
        Thread syncThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); 
                    checkServerVersion();
                } catch (Exception e) {
                    System.err.println("[Sync] Serveur injoignable...");
                }
            }
        });
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void checkServerVersion() throws Exception {
        URL url = java.net.URI.create(serverUrl + "/version").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (conn.getResponseCode() == 200) {
            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            long serverVersion = Long.parseLong(json.replaceAll("[^0-9]", ""));

            if (serverVersion > localVersion) {
                updateFromHistory();
                localVersion = serverVersion;
            }
        }
    }

    private void updateFromHistory() throws Exception {
        URL url = java.net.URI.create(serverUrl + "/history").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (conn.getResponseCode() == 200) {
            String history = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            SwingUtilities.invokeLater(() -> {
                space.clear();
                String[] lines = history.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) executeLocally(line);
                }
                space.repaint();
            });
        }
    }

    private void executeLocally(String sExpression) {
        try {
            List<SNode> nodes = new SParser<SNode>().parse(sExpression);
            for (SNode node : nodes) {
                new Interpreter().compute(environment, node);
            }
        } catch (Exception ignored) {}
    }

    public void runScript(String sExpression) {
        try {
            sendToServer(sExpression);
            localVersion = System.currentTimeMillis(); // On évite de se re-synchro soi-même
            executeLocally(sExpression);
            space.repaint();
        } catch (IOException e) {
            showErrorMessage("Erreur", "Serveur injoignable");
        }
    }

    String sendToServer(String sExpression) throws IOException {
        URL url = java.net.URI.create(serverUrl + "/parse").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(sExpression.getBytes(StandardCharsets.UTF_8));
        }
        return new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    public void saveState(String filePath) {
        try {
            URL url = new URL(serverUrl + "/save");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            int code = connection.getResponseCode();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                String json = sb.toString();
                if (code == 200) {
                    java.nio.file.Files.writeString(
                        java.nio.file.Path.of(filePath), json, StandardCharsets.UTF_8);
                    System.out.println("[Client] État sauvegardé dans : " + filePath);
                }
            }
        } catch (IOException e) {
            System.err.println("[Client] Erreur sauvegarde : " + e.getMessage());
        }
    }

    public void loadState(String filePath) {
        try {
            String json = java.nio.file.Files.readString(
                java.nio.file.Path.of(filePath), StandardCharsets.UTF_8);

            URL url = new URL(serverUrl + "/load");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                String responseJson = sb.toString();
                if (code == 200) {
                    List<SNode> nodes = SNodeSerializer.fromJson(responseJson);
                    System.out.println("[Client] " + nodes.size() + " node(s) chargé(s).");
                    for (SNode node : nodes) {
                        try {
                            new exercice6.Interpreter().compute(environment, node);
                        } catch (Exception e) {
                            System.err.println("[Client] Erreur exécution : " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Client] Erreur chargement : " + e.getMessage());
        }
    }

    private void showErrorMessage(String title, String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE));
    }

    public GSpace getSpace() { return space; }
    public String getServerUrl() { return serverUrl; }

    public static void main(String[] args) {
        String url = (args.length > 0) ? args[0] : DEFAULT_SERVER_URL;
        SExpressionClient client = new SExpressionClient(url);
        SwingUtilities.invokeLater(() -> new ClientGUI(client).setVisible(true));
    }
}