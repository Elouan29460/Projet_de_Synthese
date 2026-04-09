package exercice6;

import java.util.ArrayList;
import java.util.List;

import stree.parser.SNode;

public class AddScript implements Command {
	private Environment environment;

	public AddScript(Environment env) {
		this.environment = env;
	}

	@Override
	public Reference run(Reference receiver, SNode method) {
		String scriptName = method.get(2).contents();
		SNode scriptDef = method.get(3);

		SNode paramNode = scriptDef.get(0);
		List<String> paramNames = new ArrayList<>();
		for (int i = 0; i < paramNode.size(); i++) {
			paramNames.add(paramNode.get(i).contents());
		}

		List<SNode> body = new ArrayList<>();
		for (int i = 1; i < scriptDef.size(); i++) {
			body.add(scriptDef.get(i));
		}

		receiver.addCommand(scriptName, new Script(paramNames, body, environment));

		return receiver;
	}
}
