package exercice2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import graphicLayer.GRect;
import graphicLayer.GSpace;
import stree.parser.SNode;
import stree.parser.SParser;
import tools.Tools;


public class Exercice2_1_0 {
	GSpace space = new GSpace("Exercice 2_1", new Dimension(200, 100));
	GRect robi = new GRect();
	String script = "(space setColor white) (robi setColor red) "
			+ "(robi translate 10 0) (space sleep 100) "
			+ "(robi translate 0 10) (space sleep 100) "
			+ "(robi translate -10 0) (space sleep 100) "
			+ "(robi translate 0 -10)";

	public Exercice2_1_0() {
		space.addElement(robi);
		space.open();
		this.runScript();
	}

	private void runScript() {
		SParser<SNode> parser = new SParser<>();
		List<SNode> rootNodes = null;
		try {
			rootNodes = parser.parse(script);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Iterator<SNode> itor = rootNodes.iterator();
		while (itor.hasNext()) {
			this.run(itor.next());
		}
	}
	
	private void run(SNode expr) {
		if (expr.children() == null || expr.children().size() < 2) return;
		
		String receiver = expr.get(0).contents();
		String command = expr.get(1).contents();
		
		if ("setColor".equals(command)) {
			String colorName = expr.get(2).contents();
			Color color = Tools.getColorByName(colorName);
			if (color == null) return;
			
			if ("space".equals(receiver)) {
				space.setColor(color);
			} else if ("robi".equals(receiver)) {
				robi.setColor(color);
			}
		} else if ("translate".equals(command)) {
			int dx = Integer.parseInt(expr.get(2).contents());
			int dy = Integer.parseInt(expr.get(3).contents());
			if ("robi".equals(receiver)) {
				robi.translate(new Point(dx, dy));
			}
		} else if ("sleep".equals(command)) {
			int millis = Integer.parseInt(expr.get(2).contents());
			Tools.sleep(millis);
		}
	}

	public static void main(String[] args) {
		new Exercice2_1_0();
	}
}