package p2Exercice1_2_3.shared;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère la sauvegarde et le chargement de l'état graphique
 * sous forme de liste de S-Expressions en JSON.
 *
 * Format du fichier :
 * {
 *   "expressions": [
 *     "(space add robi (Rect new))",
 *     "(robi setColor red)"
 *   ]
 * }
 */
public class PersistenceManager {

    private PersistenceManager() {}

    /**
     * Sauvegarde une liste de S-Expressions dans un fichier JSON.
     * @param expressions la liste des S-expressions à sauvegarder
     * @param filePath    le chemin du fichier de destination
     */
    public static void save(List<String> expressions, String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"expressions\": [\n");
        for (int i = 0; i < expressions.size(); i++) {
            sb.append("    \"").append(escapeJson(expressions.get(i))).append("\"");
            if (i < expressions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        Files.writeString(Path.of(filePath), sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Charge une liste de S-Expressions depuis un fichier JSON.
     * @param filePath le chemin du fichier source
     * @return la liste des S-expressions lues
     */
    public static List<String> load(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        return parseExpressions(content);
    }

    /**
     * Charge depuis une String JSON directement (utile côté serveur).
     */
    public static List<String> loadFromJson(String json) {
        return parseExpressions(json);
    }

    /**
     * Sérialise une liste de S-Expressions en JSON String (utile pour /save).
     */
    public static String toJson(List<String> expressions) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"expressions\": [\n");
        for (int i = 0; i < expressions.size(); i++) {
            sb.append("    \"").append(escapeJson(expressions.get(i))).append("\"");
            if (i < expressions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Parsing JSON manuel (sans librairie externe, cohérent avec SNodeSerializer)
    // -------------------------------------------------------------------------

    private static List<String> parseExpressions(String json) {
        List<String> result = new ArrayList<>();
        // On cherche le tableau "expressions": [...]
        int start = json.indexOf("[");
        int end   = json.lastIndexOf("]");
        if (start < 0 || end < 0) return result;

        String array = json.substring(start + 1, end).trim();
        if (array.isEmpty()) return result;

        // On extrait chaque chaîne entre guillemets
        int i = 0;
        while (i < array.length()) {
            if (array.charAt(i) == '"') {
                StringBuilder sb = new StringBuilder();
                i++; // saute le guillemet ouvrant
                while (i < array.length()) {
                    char c = array.charAt(i);
                    if (c == '\\' && i + 1 < array.length()) {
                        char next = array.charAt(i + 1);
                        switch (next) {
                            case '"':  sb.append('"');  i += 2; continue;
                            case '\\': sb.append('\\'); i += 2; continue;
                            case 'n':  sb.append('\n'); i += 2; continue;
                            default:   sb.append(next); i += 2; continue;
                        }
                    }
                    if (c == '"') break; // guillemet fermant
                    sb.append(c);
                    i++;
                }
                result.add(sb.toString());
            }
            i++;
        }
        return result;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}