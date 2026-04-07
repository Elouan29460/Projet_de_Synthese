package exercice6;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import stree.parser.SNode;
import stree.parser.SParser;

public class Script implements Command {
	private List<String> paramNames;
	private List<SNode> body;
	private Environment environment;

	public Script(List<String> paramNames, List<SNode> body, Environment env) {
		this.paramNames = paramNames;
		this.body = body;
		this.environment = env;
	}

	@Override
	public Reference run(Reference receiver, SNode method) {
		Map<String, String> params = new HashMap<>();
		params.put(paramNames.get(0), method.get(0).contents());
		for (int i = 1; i < paramNames.size(); i++) {
			params.put(paramNames.get(i), method.get(i + 1).contents());
		}

		Reference result = receiver;
		for (SNode bodyExpr : body) {
			String exprStr = nodeToString(bodyExpr, params);
			SParser<SNode> parser = new SParser<>();
			try {
				List<SNode> parsed = parser.parse(exprStr);
				Iterator<SNode> itor = parsed.iterator();
				while (itor.hasNext()) {
					result = new Interpreter().compute(environment, itor.next());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	private String nodeToString(SNode node, Map<String, String> params) {
		if (node.isLeaf()) {
			String substituted = substituteInContent(node.contents(), params);
			if (substituted.contains(" ")) {
				return "\"" + substituted + "\"";
			}
			return substituted;
		}
		StringBuilder sb = new StringBuilder("(");
		for (int i = 0; i < node.size(); i++) {
			if (i > 0) sb.append(" ");
			sb.append(nodeToString(node.get(i), params));
		}
		sb.append(")");
		return sb.toString();
	}

	private String substituteInContent(String content, Map<String, String> params) {
		String[] parts = content.split("\\.", -1);
		for (int i = 0; i < parts.length; i++) {
			if (params.containsKey(parts[i])) {
				parts[i] = params.get(parts[i]);
			}
		}
		return String.join(".", parts);
	}
}
