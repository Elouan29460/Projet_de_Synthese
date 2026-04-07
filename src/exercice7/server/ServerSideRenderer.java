package exercice7.server;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import stree.parser.SNode;
import stree.parser.SParser;
import graphicLayer.*;
import exercice6.*;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
/**
 * Effectue un rendu graphique côté serveur à partir d'une S-Expression.
 * Réutilise l'interpréteur de l'exercice 6 sans modification.
 *
 * Ce rendu s'affiche dans une fenêtre "[Server]" afin de pouvoir
 * comparer visuellement avec le rendu client.
 */
public class ServerSideRenderer {

    private final Environment environment;
    private final GSpace space;
    


    public BufferedImage capture() {
        // On récupère les dimensions définies à la création
        int w = space.getBounds().width;
        int h = space.getBounds().height;
        
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        // On demande au space de se dessiner directement sur notre image
        space.paint(g2d); 
        
        g2d.dispose();
        return img;
    }

    public ServerSideRenderer() {
        space = new GSpace("Exercice 7 - Rendu Serveur", new Dimension(400, 300));
        space.open();

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

    /**
     * Parse et exécute une S-Expression sur le rendu serveur.
     *
     * @param script La S-Expression à interpréter.
     */
    public void render(String script) {
        SParser<SNode> parser = new SParser<>();
        List<SNode> compiled;
        try {
            compiled = parser.parse(script);
        } catch (IOException e) {
            System.err.println("[ServerRenderer] Erreur de parsing : " + e.getMessage());
            return;
        }

        Iterator<SNode> itor = compiled.iterator();
        while (itor.hasNext()) {
            try {
                new Interpreter().compute(environment, itor.next());
            } catch (Exception e) {
                System.err.println("[ServerRenderer] Erreur d'exécution : " + e.getMessage());
            }
        }
    }
}