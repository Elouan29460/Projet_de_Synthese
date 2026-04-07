package p2Exercice1_2_3.client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;

public class ClientGUI extends JFrame {
    private JTextArea commandArea;
    private JButton sendButton;
    private SExpressionClient client;

    public ClientGUI(SExpressionClient client) {
        this.client = client;
        setTitle("S-Expression IDE - Client");
        setSize(600, 500); // Un peu plus grand pour l'ergonomie
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Zone de texte (Centre) ---
        commandArea = new JTextArea("(space add r1 (Rect new 10 10 100 50))\n(r1 setColor red)");
        add(new JScrollPane(commandArea), BorderLayout.CENTER);

        // --- Barre d'outils (Nord) ---
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        
        addQuickButton(toolbar, "Ajouter Rect", "(space add r1 (Rect new 50 50 100 60))");
        addQuickButton(toolbar, "Ajouter Oval", "(space add o1 (Oval new 200 50 80 80))");
        addQuickButton(toolbar, "Nettoyer", "(space clear)");
        
        // --- AJOUT DU BOUTON CAPTURE ---
        JButton captureBtn = new JButton("📷 Capture Serveur");
        captureBtn.addActionListener(e -> takeScreenshot());
        toolbar.add(captureBtn);
        
        add(toolbar, BorderLayout.NORTH);

        // --- Bouton d'envoi (Sud) ---
        sendButton = new JButton("Exécuter sur le Serveur");
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(e -> {
            client.runScript(commandArea.getText());
        });
        add(sendButton, BorderLayout.SOUTH);
    }

    private void addQuickButton(JToolBar toolbar, String label, String sExpression) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> {
            commandArea.setText(sExpression);
            client.runScript(sExpression);
        });
        toolbar.add(btn);
    }

    // --- LA MÉTHODE DE CAPTURE ---
    private void takeScreenshot() {
        try {
            // On demande l'image au serveur (port 4444 /screenshot)
            URL url = new URL(client.getServerUrl() + "/screenshot");
            BufferedImage img = ImageIO.read(url);
            
            if (img != null) {
                // Création d'une fenêtre popup pour afficher l'image reçue
                JFrame frame = new JFrame("Capture d'écran du Serveur");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(new JLabel(new ImageIcon(img)));
                frame.pack();
                frame.setLocationRelativeTo(this); // Centre la popup
                frame.setVisible(true);
            } else {
                throw new Exception("L'image reçue est vide.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Erreur de capture : " + ex.getMessage(), 
                "Erreur Réseau", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}