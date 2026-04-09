package exercice4;

import java.awt.Point;

import graphicLayer.GElement;
import stree.parser.SNode;

public class Translate implements Command {
	@Override
	public Reference run(Reference receiver, SNode method) {
		int dx = Integer.parseInt(method.get(2).contents());
		int dy = Integer.parseInt(method.get(3).contents());
		((GElement) receiver.getReceiver()).translate(new Point(dx, dy));
		return receiver;
	}
}
