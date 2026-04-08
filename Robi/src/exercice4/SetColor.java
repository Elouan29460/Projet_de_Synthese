package exercice4;

import java.awt.Color;

import graphicLayer.GElement;
import graphicLayer.GSpace;
import stree.parser.SNode;
import tools.Tools;

public class SetColor implements Command {
	@Override
	public Reference run(Reference receiver, SNode method) {
		String colorName = method.get(2).contents();
		Color color = Tools.getColorByName(colorName);
		if (color == null) {
			throw new RuntimeException("Couleur inconnue : " + colorName);
		}
		Object obj = receiver.getReceiver();
		if (obj instanceof GSpace) {
			((GSpace) obj).setColor(color);
		} else if (obj instanceof GElement) {
			((GElement) obj).setColor(color);
		}
		return receiver;
	}
}
