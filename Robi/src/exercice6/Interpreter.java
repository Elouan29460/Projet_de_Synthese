package exercice6;

import stree.parser.SNode;

public class Interpreter {
	public Reference compute(Environment env, SNode expr) {
		String receiverName = expr.get(0).contents();
		Reference receiver = env.getReferenceByName(receiverName);
		if (receiver == null) {
			throw new RuntimeException("Référence inconnue : " + receiverName);
		}
		return receiver.run(expr);
	}
}
