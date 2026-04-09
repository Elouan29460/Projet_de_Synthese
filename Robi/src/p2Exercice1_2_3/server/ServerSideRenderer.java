package p2Exercice1_2_3.server;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

import stree.parser.SNode;
import stree.parser.SParser;
import graphicLayer.*;
import exercice6.*;

public class ServerSideRenderer {

    private final Environment environment;
    private final GSpace space;
    // --- AJOUT ICI ---
    private final Interpreter interpreter; 

    public ServerSideRenderer() {
        space = new GSpace("Exercice 7 - Rendu Serveur", new Dimension(400, 300));
        space.open();

        environment = new Environment();
        // --- INITIALISATION ICI ---
        interpreter = new Interpreter(); 

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

    public void render(String sExpression) {
        try {
            SParser<SNode> parser = new SParser<>();
            List<SNode> nodes = parser.parse(sExpression);
            for (SNode node : nodes) {
                // Maintenant 'interpreter' existe et peut être utilisé
                interpreter.compute(environment, node);
            }

            // Correction du rafraîchissement
            if (this.space != null) {
                SwingUtilities.invokeLater(() -> {
                    this.space.repaint();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BufferedImage capture() {
        int w = space.getBounds().width;
        int h = space.getBounds().height;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        space.paint(g2d); 
        g2d.dispose();
        return img;
    }
}