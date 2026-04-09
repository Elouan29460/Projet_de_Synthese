package p2Exercice1_2_3.shared;

import stree.parser.SNode;
import java.util.List;

/**
 * Sérialise et désérialise des SNodes en JSON.
 *
 * Format JSON utilisé :
 * <pre>
 * // Feuille (atom)
 * { "leaf": true, "contents": "monAtom", "quote": 0 }
 *
 * // Nœud (liste)
 * { "leaf": false, "quote": 0, "children": [ ... ] }
 * </pre>
 *
 * Ce format est volontairement simple pour rester lisible et
 * ne pas introduire de dépendance à une bibliothèque JSON externe.
 */
public class SNodeSerializer {

    private SNodeSerializer() {}

    // -------------------------------------------------------------------------
    // Sérialisation : SNode → JSON
    // -------------------------------------------------------------------------

    public static String toJson(List<SNode> nodes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(nodeToJson(nodes.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String nodeToJson(SNode node) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"leaf\":").append(node.isLeaf()).append(",");
        sb.append("\"quote\":").append(node.quote()).append(",");

        if (node.isLeaf()) {
            sb.append("\"contents\":\"").append(escapeJson(node.contents())).append("\"");
        } else {
            sb.append("\"children\":[");
            List<SNode> children = node.children();
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(nodeToJson(children.get(i)));
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Désérialisation : JSON → SNode
    // -------------------------------------------------------------------------

    /**
     * Désérialise un tableau JSON de SNodes (tel que produit par {@link #toJson}).
     * Implémentation sans dépendance externe (parsing manuel).
     */
    public static List<SNode> fromJson(String json) {
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("JSON invalide : tableau attendu");
        }
        // Retire les crochets extérieurs
        String inner = json.substring(1, json.length() - 1).trim();
        java.util.List<SNode> result = new java.util.ArrayList<>();
        if (inner.isEmpty()) return result;

        // Découpe les objets JSON de premier niveau
        java.util.List<String> items = splitTopLevelObjects(inner);
        for (String item : items) {
            result.add(parseNode(item.trim()));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Parsing JSON manuel (sans librairie externe)
    // -------------------------------------------------------------------------

    private static SNode parseNode(String json) {
        stree.parser.SDefaultNode node = new stree.parser.SDefaultNode();

        boolean isLeaf = extractBoolean(json, "leaf");
        int quote = extractInt(json, "quote");
        node.quote(quote);

        if (isLeaf) {
            String contents = extractString(json, "contents");
            node.setContents(contents);
        } else {
            String childrenJson = extractArray(json, "children");
            if (childrenJson != null && !childrenJson.isEmpty()) {
                java.util.List<String> childItems = splitTopLevelObjects(childrenJson);
                for (String childJson : childItems) {
                    SNode child = parseNode(childJson.trim());
                    node.addChild(child);
                }
            }
        }
        return node;
    }

    /** Extrait la valeur booléenne d'une clé JSON simple. */
    private static boolean extractBoolean(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return false;
        int start = idx + pattern.length();
        return json.startsWith("true", start);
    }

    /** Extrait la valeur entière d'une clé JSON simple. */
    private static int extractInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return 0;
        int start = idx + pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
    }

    /** Extrait la valeur chaîne d'une clé JSON simple (gère les échappements). */
    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int start = idx + pattern.length();
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case 'n':  sb.append('\n'); i += 2; continue;
                    case 'r':  sb.append('\r'); i += 2; continue;
                    case 't':  sb.append('\t'); i += 2; continue;
                    default:   sb.append(next); i += 2; continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** Extrait le contenu d'un tableau JSON pour une clé donnée. */
    private static String extractArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int start = idx + pattern.length();
        int depth = 1;
        int i = start;
        while (i < json.length() && depth > 0) {
            char c = json.charAt(i);
            if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') depth--;
            if (depth > 0) i++;
        }
        return json.substring(start, i);
    }

    /**
     * Découpe une chaîne en objets JSON de premier niveau (séparés par des virgules),
     * en respectant les accolades/crochets imbriqués et les chaînes entre guillemets.
     */
    private static java.util.List<String> splitTopLevelObjects(String s) {
        java.util.List<String> result = new java.util.ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && inString) { i++; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                String item = s.substring(start, i).trim();
                if (!item.isEmpty()) result.add(item);
                start = i + 1;
            }
        }
        String last = s.substring(start).trim();
        if (!last.isEmpty()) result.add(last);
        return result;
    }

    // -------------------------------------------------------------------------
    // Utilitaire
    // -------------------------------------------------------------------------

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}