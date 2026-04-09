package p2Exercice1_2_3.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import stree.parser.SNode;
import stree.parser.SParser;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du sérialiseur SNode ↔ JSON.
 */
class SNodeSerializerTest {

    private List<SNode> parse(String src) throws IOException {
        return new SParser<SNode>().parse(src);
    }

    // -------------------------------------------------------------------------
    // Sérialisation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sérialisation d'un atome simple")
    void testSerializeLeaf() throws IOException {
        List<SNode> nodes = parse("hello");
        String json = SNodeSerializer.toJson(nodes);
        assertTrue(json.contains("\"leaf\":true"), "Doit contenir leaf:true");
        assertTrue(json.contains("\"contents\":\"hello\""), "Doit contenir le contenu");
    }

    @Test
    @DisplayName("Sérialisation d'un nœud (S-Expression)")
    void testSerializeNode() throws IOException {
        List<SNode> nodes = parse("(space setColor red)");
        String json = SNodeSerializer.toJson(nodes);
        assertTrue(json.contains("\"leaf\":false"), "Doit contenir leaf:false pour le nœud");
        assertTrue(json.contains("\"children\""), "Doit contenir des enfants");
        assertTrue(json.contains("space"), "Doit contenir 'space'");
        assertTrue(json.contains("setColor"), "Doit contenir 'setColor'");
        assertTrue(json.contains("red"), "Doit contenir 'red'");
    }

    @Test
    @DisplayName("Sérialisation d'une expression imbriquée")
    void testSerializeNested() throws IOException {
        List<SNode> nodes = parse("(space add r1 (Rect new 10 20 30 40))");
        String json = SNodeSerializer.toJson(nodes);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("Rect"));
    }

    @Test
    @DisplayName("Sérialisation de plusieurs expressions")
    void testSerializeMultiple() throws IOException {
        List<SNode> nodes = parse("(space setColor red)(space setDim 200 100)");
        String json = SNodeSerializer.toJson(nodes);
        // Le JSON doit être un tableau avec 2 éléments
        assertEquals('[', json.charAt(0));
        assertEquals(']', json.charAt(json.length() - 1));
    }

    @Test
    @DisplayName("Sérialisation d'une chaîne avec caractères spéciaux")
    void testSerializeStringWithSpecialChars() throws IOException {
        List<SNode> nodes = parse("(space add r \"hello world\")");
        String json = SNodeSerializer.toJson(nodes);
        assertNotNull(json);
    }

    // -------------------------------------------------------------------------
    // Désérialisation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Round-trip : atome simple")
    void testRoundTripLeaf() throws IOException {
        List<SNode> original = parse("hello");
        String json = SNodeSerializer.toJson(original);
        List<SNode> restored = SNodeSerializer.fromJson(json);

        assertEquals(1, restored.size());
        assertTrue(restored.get(0).isLeaf());
        assertEquals("hello", restored.get(0).contents());
    }

    @Test
    @DisplayName("Round-trip : nœud simple")
    void testRoundTripSimpleNode() throws IOException {
        List<SNode> original = parse("(space setColor red)");
        String json = SNodeSerializer.toJson(original);
        List<SNode> restored = SNodeSerializer.fromJson(json);

        assertEquals(1, restored.size());
        SNode node = restored.get(0);
        assertFalse(node.isLeaf());
        assertEquals(3, node.size());
        assertEquals("space", node.get(0).contents());
        assertEquals("setColor", node.get(1).contents());
        assertEquals("red", node.get(2).contents());
    }

    @Test
    @DisplayName("Round-trip : expression imbriquée")
    void testRoundTripNested() throws IOException {
        List<SNode> original = parse("(space add r1 (Rect new 10 20 30 40))");
        String json = SNodeSerializer.toJson(original);
        List<SNode> restored = SNodeSerializer.fromJson(json);

        assertEquals(1, restored.size());
        SNode node = restored.get(0);
        assertFalse(node.isLeaf());
        assertEquals(4, node.size());

        // Le 4e enfant doit être un nœud (Rect new 10 20 30 40)
        SNode inner = node.get(3);
        assertFalse(inner.isLeaf());
        assertEquals("Rect", inner.get(0).contents());
    }

    @Test
    @DisplayName("Round-trip : plusieurs expressions")
    void testRoundTripMultiple() throws IOException {
        List<SNode> original = parse("(space setColor red)(space setDim 300 200)");
        String json = SNodeSerializer.toJson(original);
        List<SNode> restored = SNodeSerializer.fromJson(json);

        assertEquals(2, restored.size());
    }

    @Test
    @DisplayName("Désérialisation d'un tableau vide")
    void testDeserializeEmptyArray() {
        List<SNode> nodes = SNodeSerializer.fromJson("[]");
        assertNotNull(nodes);
        assertEquals(0, nodes.size());
    }

    @Test
    @DisplayName("Désérialisation : quote préservé")
    void testRoundTripQuote() throws IOException {
        List<SNode> original = parse("'hello");
        String json = SNodeSerializer.toJson(original);
        List<SNode> restored = SNodeSerializer.fromJson(json);

        assertEquals(1, restored.size());
        assertEquals(1, restored.get(0).quote());
        assertEquals("hello", restored.get(0).contents());
    }
}