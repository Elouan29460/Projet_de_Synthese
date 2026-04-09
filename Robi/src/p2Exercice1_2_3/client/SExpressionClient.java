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