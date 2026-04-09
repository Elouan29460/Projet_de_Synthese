package exercice6;

import graphicLayer.GContainer;
import graphicLayer.GElement;
import stree.parser.SNode;

public class DelElement implements Command {
	private Environment environment;

	public DelElement(Environment env) {
		this.environment = env;
	}

	@Override
	public Reference run(Reference receiver, SNode method) {
		String receiverName = method.get(0).contents();
		String name = method.get(2).contents();
		String fullName = receiverName + "." + name;

		Reference ref = environment.getReferenceByName(fullName);
		if (ref == null) {
			throw new RuntimeException("Référence inconnue : " + fullName);
		}
		Object container = receiver.getReceiver();
		Object element = ref.getReceiver();
		if (container instanceof GContainer && element instanceof GElement) {
			((GContainer) container).removeElement((GElement) element);
			((GContainer) container).repaint();
		}
		environment.removeReference(fullName);
		environment.removeReferencesStartingWith(fullName + ".");
		return receiver;
	}
}
