package exercice4;

import graphicLayer.GContainer;
import graphicLayer.GElement;
import stree.parser.SNode;

public class AddElement implements Command {
	@Override
	public Reference run(Reference receiver, SNode method) {
		// (space add robi (Rect new))
		String name = method.get(2).contents();

		// Execute nested expression to create the element: (Rect new)
		SNode newExpr = method.get(3);
		String className = newExpr.get(0).contents();

		Environment env = getEnvironment(receiver);
		Reference classRef = env.getReferenceByName(className);
		Reference newRef = classRef.run(newExpr);

		// Add the graphic element to the container
		Object container = receiver.getReceiver();
		Object element = newRef.getReceiver();
		if (container instanceof GContainer && element instanceof GElement) {
			((GContainer) container).addElement((GElement) element);
		}

		// Register the new reference in the environment
		env.addReference(name, newRef);

		return newRef;
	}

	private Environment getEnvironment(Reference receiver) {
		return environment;
	}

	private Environment environment;

	public AddElement(Environment env) {
		this.environment = env;
	}
}
