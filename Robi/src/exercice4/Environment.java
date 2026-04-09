package exercice4;

import java.util.HashMap;

public class Environment {
	HashMap<String, Reference> variables;

	public Environment() {
		variables = new HashMap<String, Reference>();
	}

	public void addReference(String name, Reference ref) {
		variables.put(name, ref);
	}

	public Reference getReferenceByName(String name) {
		return variables.get(name);
	}

	public void removeReference(String name) {
		variables.remove(name);
	}
}
