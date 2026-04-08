package exercice4;

import graphicLayer.GContainer;
import graphicLayer.GElement;
import stree.parser.SNode;

public class AddElement implements Command {
	private Environment environment;

	public AddElement(Environment env) {
		this.environment = env;
	}

	@Override
	public Reference run(Reference receiver, SNode method) {
		String name = method.get(2).contents();

		SNode newExpr = method.get(3);
		String className = newExpr.get(0).contents();

		Reference classRef = environment.getReferenceByName(className);
		Reference newRef = classRef.run(newExpr);

		Object container = receiver.getReceiver();
		Object element = newRef.getReceiver();
		if (container instanceof GContainer && element instanceof GElement) {
			((GContainer) container).addElement((GElement) element);
		}

		environment.addReference(name, newRef);

		return newRef;
	}
}
