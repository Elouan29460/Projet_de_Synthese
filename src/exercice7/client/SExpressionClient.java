package exercice7.client;

import java.awt.Dimension;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.swing.SwingUtilities;

import stree.parser.SNode;
import graphicLayer.*;
import exercice6.*;
import exercice7.shared.SNodeSerializer;
import tools.Tools;

/**
 * Client de l'architecture client–serveur de l'exercice 7.
 *
 * Fonctionnement :
 *  1. Lit une S-Expression depuis l'entrée utilisateur.
 *  2. L'envoie en POST au serveur (http://localhost:8080/parse).
 *  3. Reçoit en retour les SNodes sérialisés en JSON.
 *  4. Désérialise les SNodes et les exécute localement avec l'interpréteur.
 *
 * Le rendu client s'affiche dans une fenêtre "[Client]" ;
 * le rendu serveur s'affiche simultanément dans une fenêtre "[Server]"
 * pour permettre la comparaison visuelle.
 *
 * L'URL du serveur est configurable via :
 *   -Dserver.url=http://host:port
 * ou le premier argument de main().
 */
public class SExpressionClient {

    public static final String DEFAULT_SERVER_URL = "http://localhost:4444";

    private final String serverUrl;
    private final Environment environment;
    private final GSpace space;

    public SExpressionClient(String serverUrl) {
        this.serverUrl = serverUrl;

        // --- Fenêtre de rendu côté client ---
        space = new GSpace("Exercice 7 - Rendu Client", new Dimension(400, 300));
        space.open();

        // --- Environnement de l'interpréteur ---
        environment = new Environment();

        Reference spaceRef       = new Reference(space);
        Reference rectClassRef   = new Reference(GRect.class);
        Reference ovalClassRef   = new Reference(GOval.class);
        Reference imageClassRef  = new Reference(GImage.class);
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

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Envoie une S-Expression au serveur, récupère les SNodes et les exécute.
     *
     * @param sExpression La S-Expression à traiter.
     */
    public void runScript(String sExpression) {
        // 1. Envoyer au serveur et récupérer le JSON
        String json;
        try {
            json = sendToServer(sExpression);
        } catch (IOException e) {
            System.err.println("[Client] Erreur réseau : " + e.getMessage());
            return;
        }

        // 2. Désérialiser les SNodes
        List<SNode> nodes;
        try {
            nodes = SNodeSerializer.fromJson(json);
        } catch (Exception e) {
            System.err.println("[Client] Erreur de désérialisation : " + e.getMessage());
            return;
        }

        System.out.println("[Client] " + nodes.size() + " node(s) reçu(s) du serveur.");

        // 3. Exécuter chaque SNode côté client
        for (SNode node : nodes) {
            try {
                new Interpreter().compute(environment, node);
            } catch (Exception e) {
                System.err.println("[Client] Erreur d'exécution : " + e.getMessage());
            }
        }
    }

    /**
     * Boucle interactive : lit des S-Expressions depuis le clavier et les exécute.
     */
    public void mainLoop() {
        System.out.println("[Client] Connecté à " + serverUrl);
        System.out.println("[Client] Entrez une S-Expression (ex: (space setColor red))");
        while (true) {
            System.out.print("> ");
            String input = Tools.readKeyboard();
            if (input == null || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                System.out.println("[Client] Fin.");
                break;
            }
            runScript(input);
        }
    }

    // -------------------------------------------------------------------------
    // Communication HTTP
    // -------------------------------------------------------------------------

    /**
     * Envoie la S-Expression au serveur en POST et retourne la réponse JSON brute.
     */
    String sendToServer(String sExpression) throws IOException {
        URL url = new URL(serverUrl + "/parse");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);

        // Envoi
        try (OutputStream os = connection.getOutputStream()) {
            os.write(sExpression.getBytes(StandardCharsets.UTF_8));
        }

        // Lecture de la réponse
        int code = connection.getResponseCode();
        InputStream responseStream = (code >= 200 && code < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String response = sb.toString();
            if (code != 200) {
                throw new IOException("Serveur a répondu " + code + " : " + response);
            }
            return response;
        }
    }
    
    public String getServerUrl() {
        return this.serverUrl;
    }

    // -------------------------------------------------------------------------
    // Point d'entrée
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        String serverUrl = DEFAULT_SERVER_URL;
        if (args.length > 0) serverUrl = args[0];

        SExpressionClient client = new SExpressionClient(serverUrl);
        
        // Au lieu de client.mainLoop(), on lance l'interface Swing
        SwingUtilities.invokeLater(() -> {
            ClientGUI gui = new ClientGUI(client);
            gui.setVisible(true);
        });
    }
}