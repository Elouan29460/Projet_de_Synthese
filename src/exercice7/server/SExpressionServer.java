package exercice7.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import stree.parser.SNode;
import stree.parser.SParser;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;

import graphicLayer.*;
import exercice7.shared.*;

/**
 * Serveur HTTP qui expose le parseur de S-Expressions.
 *
 * - POST /parse  : reçoit une S-Expression en texte brut,
 *                  retourne la liste des SNodes sérialisés en JSON
 *                  ET effectue un rendu côté serveur.
 *
 * Le port par défaut est 8080 (surchargeable via la propriété système
 * -Dserver.port=XXXX ou le premier argument de main()).
 */
public class SExpressionServer {

    public static final int DEFAULT_PORT = 4444;

    private final int port;
    private HttpServer httpServer;

    // Rendu côté serveur (pour la comparaison)
    private final ServerSideRenderer serverRenderer;

    public SExpressionServer(int port) {
        this.port = port;
        this.serverRenderer = new ServerSideRenderer();
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/parse", new ParseHandler());
        httpServer.createContext("/health", new HealthHandler());
        httpServer.createContext("/screenshot", new ScreenshotHandler());
        httpServer.setExecutor(null); // exécuteur par défaut
        httpServer.start();
        System.out.println("[Server] Démarré sur le port " + port);
        System.out.println("[Server] POST http://localhost:" + port + "/parse");
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            System.out.println("[Server] Arrêté.");
        }
    }

    // -------------------------------------------------------------------------
    // Handler principal : parsing + rendu serveur
    // -------------------------------------------------------------------------
    private class ParseHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS basique pour faciliter les tests depuis un navigateur
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            // Lecture du corps de la requête (la S-Expression)
            String sExpression;
            try (InputStream is = exchange.getRequestBody()) {
                sExpression = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            if (sExpression.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Empty body\"}");
                return;
            }

            System.out.println("[Server] Reçu : " + sExpression);

            // --- 1. Parsing ---
            SParser<SNode> parser = new SParser<>();
            List<SNode> nodes;
            try {
                nodes = parser.parse(sExpression);
            } catch (Exception e) {
                String err = "{\"error\":\"Parse error: " + escapeJson(e.getMessage()) + "\"}";
                sendResponse(exchange, 422, err);
                return;
            }

            // --- 2. Rendu côté serveur (pour la comparaison) ---
            serverRenderer.render(sExpression);

            // --- 3. Sérialisation des SNodes en JSON ---
            String json = SNodeSerializer.toJson(nodes);
            System.out.println("[Server] Réponse JSON : " + json);
            sendResponse(exchange, 200, json);
        }
    }

    // -------------------------------------------------------------------------
    // Handler de santé
    // -------------------------------------------------------------------------
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "{\"status\":\"ok\"}");
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------
    private static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
    
    class ScreenshotHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Ajouter ceci pour éviter des blocages si testé via navigateur
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // On récupère l'image depuis le renderer
            BufferedImage image = serverRenderer.capture();
            
            // On écrit l'image en PNG dans un flux d'octets
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();

            // Envoi de l'image au client
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Point d'entrée
    // -------------------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        String sysProp = System.getProperty("server.port");
        if (sysProp != null) {
            try { port = Integer.parseInt(sysProp); } catch (NumberFormatException ignored) {}
        }

        SExpressionServer server = new SExpressionServer(port);
        server.start();

        // Arrêt propre sur CTRL+C
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}