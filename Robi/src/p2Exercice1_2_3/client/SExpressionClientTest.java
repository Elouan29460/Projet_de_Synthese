package p2Exercice1_2_3.client;

import org.junit.jupiter.api.Test;

import p2Exercice1_2_3.shared.SNodeSerializer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import stree.parser.SNode;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du client sans démarrer le vrai serveur HTTP.
 * On teste la logique de désérialisation et d'exécution locale.
 */
class SExpressionClientTest {

    /**
     * Client bouchon qui court-circuite l'appel HTTP
     * en retournant directement du JSON pré-calculé.
     */
    private static class MockClient extends SExpressionClient {
        private String mockResponse;
        private String lastSentExpression;

        MockClient(String mockResponse) {
            super("http://mock-server");
            this.mockResponse = mockResponse;
        }

        @Override
        String sendToServer(String sExpression) throws IOException {
            this.lastSentExpression = sExpression;
            return mockResponse;
        }

        String getLastSentExpression() {
            return lastSentExpression;
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Le client envoie bien la S-Expression au serveur")
    void testClientSendsExpression() throws IOException {
        // Prépare une réponse JSON valide (liste vide)
        MockClient client = new MockClient("[]");
        client.runScript("(space setColor red)");
        assertEquals("(space setColor red)", client.getLastSentExpression());
    }

    @Test
    @DisplayName("Le client désérialise correctement un nœud simple")
    void testClientDeserializesNode() throws IOException {
        // Construction du JSON représentant (space setColor red)
        String json = "[{\"leaf\":false,\"quote\":0,\"children\":["
                + "{\"leaf\":true,\"quote\":0,\"contents\":\"space\"},"
                + "{\"leaf\":true,\"quote\":0,\"contents\":\"setColor\"},"
                + "{\"leaf\":true,\"quote\":0,\"contents\":\"red\"}"
                + "]}]";

        List<SNode> nodes = SNodeSerializer.fromJson(json);
        assertEquals(1, nodes.size());
        SNode node = nodes.get(0);
        assertFalse(node.isLeaf());
        assertEquals("space",    node.get(0).contents());
        assertEquals("setColor", node.get(1).contents());
        assertEquals("red",      node.get(2).contents());
    }

    @Test
    @DisplayName("Le client gère une réponse JSON vide sans planter")
    void testClientHandlesEmptyResponse() {
        MockClient client = new MockClient("[]");
        assertDoesNotThrow(() -> client.runScript(""));
    }

    @Test
    @DisplayName("Le client gère une erreur réseau gracieusement")
    void testClientHandlesNetworkError() {
        SExpressionClient client = new SExpressionClient("http://localhost:9") {
            @Override
            String sendToServer(String sExpression) throws IOException {
                throw new IOException("Connection refused");
            }
        };
        // Ne doit pas lever d'exception non gérée
        assertDoesNotThrow(() -> client.runScript("(space setColor red)"));
    }

    @Test
    @DisplayName("Le client gère un JSON invalide gracieusement")
    void testClientHandlesInvalidJson() {
        MockClient client = new MockClient("NOT_VALID_JSON");
        // Ne doit pas lever d'exception non gérée
        assertDoesNotThrow(() -> client.runScript("(space setColor red)"));
    }
}