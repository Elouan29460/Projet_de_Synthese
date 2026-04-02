package exercice4;

import graphicLayer.GString;
import stree.parser.SNode;

public class NewString implements Command {
	@Override
	public Reference run(Reference reference, SNode method) {
		// (label.class new "Hello world")
		String text = method.get(2).contents();
		GString gString = new GString(text);
		Reference ref = new Reference(gString);
		ref.addCommand("setColor", new SetColor());
		ref.addCommand("translate", new Translate());
		ref.addCommand("setDim", new SetDim());
		return ref;
	}
}
