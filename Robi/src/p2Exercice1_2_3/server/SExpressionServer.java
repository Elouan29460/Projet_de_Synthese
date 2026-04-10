package p2Exercice1_2_3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import stree.parser.SNode;
import stree.parser.SParser;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import p2Exercice1_2_3.shared.*;
import p2Exercice1_2_3.shared.PersistenceManager;

public class SExpressionServer {

    public static final int DEFAULT_PORT = 4444;

    private final int port;
    private HttpServer httpServer;
    private final ServerSideRenderer serverRenderer;
    
    private final List<String> executedExpressions = new ArrayList<>();
        
    // --- SYNCHRONISATION ---
    // Version incrémentée à chaque modification pour notifier les clients
    private final AtomicLong stateVersion = new AtomicLong(System.currentTimeMillis());
    
    // Historique complet des S-Expressions reçues
    private final StringBuilder history = new StringBuilder();

    public SExpressionServer(int port) {
        this.port = port;
        this.serverRenderer = new ServerSideRenderer();
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Enregistrement des contextes (endpoints)
        httpServer.createContext("/parse", new ParseHandler());
        httpServer.createContext("/health", new HealthHandler());
        httpServer.createContext("/screenshot", new ScreenshotHandler());
        httpServer.createContext("/version", new VersionHandler());
        httpServer.createContext("/history", new HistoryHandler());
        
        httpServer.setExecutor(null); 
        httpServer.start();
        httpServer.createContext("/save", new SaveHandler());
        httpServer.createContext("/load", new LoadHandler());
        
        System.out.println("[Server] Démarré sur port " + port);

    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            System.out.println("[Server] Arrêté.");
        }
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private class ParseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String sExpression;
            try (InputStream is = exchange.getRequestBody()) {
                sExpression = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            if (sExpression.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Empty body\"}");
                return;
            }

            try {
                // --- 1. Sauvegarde dans l'historique ---
                synchronized(history) {
                    history.append(sExpression).append("\n");
                }

                // --- 2. Rendu côté serveur ---
                serverRenderer.render(sExpression);
                
                executedExpressions.add(sExpression);
                
                // --- 3. Notification : Mise à jour de la version ---
                stateVersion.set(System.currentTimeMillis());
                
                // --- 4. Réponse JSON des nodes parsés ---
                SParser<SNode> parser = new SParser<>();
                List<SNode> nodes = parser.parse(sExpression);
                String json = SNodeSerializer.toJson(nodes);
                
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                sendResponse(exchange, 200, json);
                
            } catch (Exception e) {
                sendResponse(exchange, 422, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            
            String response;
            synchronized(history) {
                response = history.toString();
            }
            sendResponse(exchange, 200, response);
        }
    }

    private class VersionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            String response = "{\"version\":" + stateVersion.get() + "}";
            sendResponse(exchange, 200, response);
        }
    }

    private class ScreenshotHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            BufferedImage image = serverRenderer.capture();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();

            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
    
    class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            String json = PersistenceManager.toJson(executedExpressions);
            System.out.println("[Server] Sauvegarde de " + executedExpressions.size() + " expression(s).");
            sendResponse(exchange, 200, json);
        }
    }

    class LoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            if (body.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Empty body\"}");
                return;
            }

            List<String> expressions = PersistenceManager.loadFromJson(body);
            System.out.println("[Server] Chargement de " + expressions.size() + " expression(s).");
            executedExpressions.clear();

            SParser<SNode> parser = new SParser<>();
            List<SNode> allNodes = new ArrayList<>();
            for (String expr : expressions) {
                try {
                    List<SNode> nodes = parser.parse(expr);
                    allNodes.addAll(nodes);
                    serverRenderer.render(expr);
                    executedExpressions.add(expr);
                } catch (Exception e) {
                    System.err.println("[Server] Erreur parsing : " + e.getMessage());
                }
            }
            sendResponse(exchange, 200, SNodeSerializer.toJson(allNodes));
        }
    }

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

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        SExpressionServer server = new SExpressionServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}