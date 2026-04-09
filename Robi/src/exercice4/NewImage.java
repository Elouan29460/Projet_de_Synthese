package exercice4;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import graphicLayer.GImage;
import stree.parser.SNode;

public class NewImage implements Command {
	@Override
	public Reference run(Reference reference, SNode method) {
		try {
			String filename = method.get(2).contents();
			Image image = ImageIO.read(new File(filename));
			GImage gImage = new GImage(image);
			Reference ref = new Reference(gImage);
			ref.addCommand("setColor", new SetColor());
			ref.addCommand("translate", new Translate());
			return ref;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
