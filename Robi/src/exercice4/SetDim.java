package exercice4;

import java.awt.Dimension;

import graphicLayer.GBounded;
import stree.parser.SNode;

public class SetDim implements Command {
	@Override
	public Reference run(Reference receiver, SNode method) {
		int width = Integer.parseInt(method.get(2).contents());
		int height = Integer.parseInt(method.get(3).contents());
		((GBounded) receiver.getReceiver()).setDimension(new Dimension(width, height));
		return receiver;
	}
}
