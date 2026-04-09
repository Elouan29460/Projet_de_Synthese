package exercice6;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import graphicLayer.GImage;
import graphicLayer.GOval;
import graphicLayer.GRect;
import graphicLayer.GSpace;
import graphicLayer.GString;
import stree.parser.SNode;
import stree.parser.SParser;
import tools.Tools;

public class Exercice6 {
	Environment environment = new Environment();
	GSpace space;

	public Exercice6() {
		space = new GSpace("Exercice 6", new Dimension(200, 100));
		space.open();

		Reference spaceRef = new Reference(space);
		Reference rectClassRef = new Reference(GRect.class);
		Reference ovalClassRef = new Reference(GOval.class);
		Reference imageClassRef = new Reference(GImage.class);
		Reference stringClassRef = new Reference(GString.class);

		spaceRef.addCommand("setColor", new SetColor());
		spaceRef.addCommand("sleep", new Sleep());
		spaceRef.addCommand("setDim", new SetDim());
		spaceRef.addCommand("add", new AddElement(environment));
		spaceRef.addCommand("del", new DelElement(environment));
		spaceRef.addCommand("clear", new Clear());
		spaceRef.addCommand("addScript", new AddScript(environment));

		rectClassRef.addCommand("new", new NewElement());
		ovalClassRef.addCommand("new", new NewElement());
		imageClassRef.addCommand("new", new NewImage());
		stringClassRef.addCommand("new", new NewString());

		environment.addReference("space", spaceRef);
		environment.addReference("Rect", rectClassRef);
		environment.addReference("Oval", ovalClassRef);
		environment.addReference("Image", imageClassRef);
		environment.addReference("Label", stringClassRef);
	}

	public void oneShot(String script) {
		runScript(script);
	}

	private void runScript(String script) {
		SParser<SNode> parser = new SParser<>();
		List<SNode> compiled = null;
		try {
			compiled = parser.parse(script);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Iterator<SNode> itor = compiled.iterator();
		while (itor.hasNext()) {
			try {
				new Interpreter().compute(environment, itor.next());
			} catch (Exception e) {
				System.out.println("Erreur : " + e.getMessage());
			}
		}
	}

	private void mainLoop() {
		while (true) {
			System.out.print("> ");
			String input = Tools.readKeyboard();
			runScript(input);
		}
	}

	public static void main(String[] args) {
		Exercice6 exo = new Exercice6();
		exo.mainLoop();
	}
}
