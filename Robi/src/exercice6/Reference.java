package exercice6;

import java.util.HashMap;
import java.util.Map;

import stree.parser.SNode;

public class Reference {
	Object receiver;
	Map<String, Command> primitives;

	public Reference(Object receiver) {
		this.receiver = receiver;
		primitives = new HashMap<String, Command>();
	}

	public void addCommand(String name, Command cmd) {
		primitives.put(name, cmd);
	}

	public Command getCommandByName(String name) {
		return primitives.get(name);
	}

	public Object getReceiver() {
		return receiver;
	}

	public Reference run(SNode expr) {
		String commandName = expr.get(1).contents();
		Command cmd = getCommandByName(commandName);
		if (cmd == null) {
			throw new RuntimeException("Commande inconnue : " + commandName);
		}
		return cmd.run(this, expr);
	}
}
