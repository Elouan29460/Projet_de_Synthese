package exercice6;

import graphicLayer.GContainer;
import graphicLayer.GSpace;
import stree.parser.SNode;

public class Clear implements Command {
	@Override
	public Reference run(Reference receiver, SNode method) {
		Object obj = receiver.getReceiver();
		if (obj instanceof GSpace) {
			((GSpace) obj).clear();
		} else if (obj instanceof GContainer) {
			((GContainer) obj).clear();
		}
		return receiver;
	}
}
