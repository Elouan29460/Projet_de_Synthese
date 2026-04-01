package exercice3;

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

public class Exercice3_0 {
	GSpace space = new GSpace("Exercice 3", new Dimension(200, 100));
	GRect robi = new GRect();
	String script = "" +
	"   (space setColor black) " +
	"   (robi setColor yellow)" +
	"   (space sleep 1000)" +
	"   (space setColor white)\n" + 
	"   (space sleep 1000)" +
	"	(robi setColor red) \n" + 
	"   (space sleep 1000)" +
	"	(robi translate 100 0)\n" + 
	"	(space sleep 1000)\n" + 
	"	(robi translate 0 50)\n" + 
	"	(space sleep 1000)\n" + 
	"	(robi translate -100 0)\n" + 
	"	(space sleep 1000)\n" + 
	"	(robi translate 0 -40)";

	public Exercice3_0() {
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
		Command cmd = getCommandFromExpr(expr);
		if (cmd == null)
			throw new Error("unable to get command for: " + expr);
		cmd.run();
	}

	Command getCommandFromExpr(SNode expr) {
		if (expr.children() == null || expr.children().size() < 2) return null;

		String receiver = expr.get(0).contents();
		String command = expr.get(1).contents();

		if ("space".equals(receiver)) {
			if ("setColor".equals(command)) {
				Color color = Tools.getColorByName(expr.get(2).contents());
				return new SpaceChangeColor(color);
			} else if ("sleep".equals(command)) {
				int millis = Integer.parseInt(expr.get(2).contents());
				return new SpaceSleep(millis);
			}
		} else if ("robi".equals(receiver)) {
			if ("setColor".equals(command)) {
				Color color = Tools.getColorByName(expr.get(2).contents());
				return new RobiChangeColor(color);
			} else if ("translate".equals(command)) {
				int dx = Integer.parseInt(expr.get(2).contents());
				int dy = Integer.parseInt(expr.get(3).contents());
				return new RobiTranslate(dx, dy);
			}
		}
		return null;
	}

	public static void main(String[] args) {
		new Exercice3_0();
	}

	public interface Command {
		abstract public void run();
	}

	public class SpaceChangeColor implements Command {
		Color newColor;

		public SpaceChangeColor(Color newColor) {
			this.newColor = newColor;
		}

		@Override
		public void run() {
			space.setColor(newColor);
		}
	}

	public class SpaceSleep implements Command {
		int millis;

		public SpaceSleep(int millis) {
			this.millis = millis;
		}

		@Override
		public void run() {
			Tools.sleep(millis);
		}
	}

	public class RobiChangeColor implements Command {
		Color newColor;

		public RobiChangeColor(Color newColor) {
			this.newColor = newColor;
		}

		@Override
		public void run() {
			robi.setColor(newColor);
		}
	}

	public class RobiTranslate implements Command {
		int dx, dy;

		public RobiTranslate(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}

		@Override
		public void run() {
			robi.translate(new Point(dx, dy));
		}
	}
}