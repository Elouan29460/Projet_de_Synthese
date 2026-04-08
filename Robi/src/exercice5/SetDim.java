package exercice5;

import java.awt.Dimension;

import graphicLayer.GBounded;
import graphicLayer.GSpace;
import stree.parser.SNode;

public class SetDim implements Command {
	@Override
	public Reference run(Reference receiver, SNode method) {
		int width = Integer.parseInt(method.get(2).contents());
		int height = Integer.parseInt(method.get(3).contents());
		Object obj = receiver.getReceiver();
		if (obj instanceof GSpace) {
			((GSpace) obj).changeWindowSize(new Dimension(width, height));
		} else {
			((GBounded) obj).setDimension(new Dimension(width, height));
		}
		return receiver;
	}
}
