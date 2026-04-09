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
    private int rectCount = 0;
    private int ovalCount = 0;

    public ClientGUI(SExpressionClient client) {
        this.client = client;
        setTitle("S-Expression IDE - Client");
        setSize(1000, 600); // Agrandis pour accueillir le dessin
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- PARTIE GAUCHE (MENU) ---
        JPanel menuPanel = new JPanel(new BorderLayout());
        
        commandArea = new JTextArea("(space add r1 (Rect new 10 10 100 50))\n(r1 setColor red)");
        menuPanel.add(new JScrollPane(commandArea), BorderLayout.CENTER);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton btnRect = new JButton("Ajouter Rect");
        btnRect.addActionListener(e -> {
            rectCount++; // On incrémente le numéro
            String name = "r" + rectCount; // On génère le nom r1, r2...
            String script = "(space add " + name + " (Rect new " + (10 * rectCount) + " " + (10 * rectCount) + " 100 60))";
            
            commandArea.setText(script);
            client.runScript(script);
        });
        toolbar.add(btnRect);

        JButton btnOval = new JButton("Ajouter Oval");
        btnOval.addActionListener(e -> {
            ovalCount++; // On incrémente le numéro
            String name = "o" + ovalCount; // On génère le nom o1, o2...
            String script = "(space add " + name + " (Oval new " + (50 + 10 * ovalCount) + " 150 80 80))";
            
            commandArea.setText(script);
            client.runScript(script);
        });
        toolbar.add(btnOval);
        
        addQuickButton(toolbar, "Nettoyer", "(space clear)");
        
        JButton captureBtn = new JButton("📷 Capture Serveur");
        captureBtn.addActionListener(e -> takeScreenshot());
        toolbar.add(captureBtn);
        menuPanel.add(toolbar, BorderLayout.NORTH);

        sendButton = new JButton("Exécuter sur le Serveur");
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(e -> client.runScript(commandArea.getText()));
        menuPanel.add(sendButton, BorderLayout.SOUTH);

	     // --- PARTIE DROITE (DESSIN) ---
	     // On récupère le composant de dessin à l'intérieur de GSpace
	     // Si GSpace est lui-même le container, on l'utilise directement.
	     Component drawingView;
	     if (client.getSpace() instanceof Component) {
	         drawingView = (Component) client.getSpace();
	     } else {
	         // Si GSpace n'est pas un composant, on essaie de voir s'il a des composants enfants
	         // C'est une sécurité au cas où la lib est structurée différemment
	         drawingView = new JPanel(); 
	         ((JPanel)drawingView).add(new JLabel("Impossible de récupérer le Canvas"));
	     }
	
	     // --- ASSEMBLAGE ---
	     // menuPanel est le panneau qui contient ton éditeur et ta toolbar
	     JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuPanel, drawingView);
	     splitPane.setDividerLocation(400);
	     add(splitPane, BorderLayout.CENTER);
	     
	  
	     this.validate(); 
	     this.repaint();  

	     SwingUtilities.invokeLater(() -> {
	         client.getSpace().repaint();
	     });
    }

    private void addQuickButton(JToolBar toolbar, String label, String sExpression) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> {
            commandArea.setText(sExpression);
            client.runScript(sExpression);
        });
        toolbar.add(btn);
    }

    // --- LA MÉTHODE DE CAPTURE MISE À JOUR ---
    private void takeScreenshot() {
        try {
            // Utilisation de URI pour construire l'URL de manière sécurisée
            URL url = java.net.URI.create(client.getServerUrl() + "/screenshot").toURL();
            
            BufferedImage img = ImageIO.read(url);
            
            if (img != null) {
                JFrame frame = new JFrame("Capture d'écran du Serveur");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(new JLabel(new ImageIcon(img)));
                frame.pack();
                frame.setLocationRelativeTo(this);
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