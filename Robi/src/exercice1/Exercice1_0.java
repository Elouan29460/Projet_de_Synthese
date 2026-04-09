package exercice1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Random;

import graphicLayer.GRect;
import graphicLayer.GSpace;
import tools.Tools;

public class Exercice1_0 {
	GSpace space = new GSpace("Exercice 1", new Dimension(200, 150));
	GRect robi = new GRect();
	int sleepDuration = 10;

	public Exercice1_0() {
		space.addElement(robi);
		space.open();

		while (true) {
			int spaceWidth = space.getWidth();
			int spaceHeight = space.getHeight();
			int robiWidth = robi.getWidth();
			int robiHeight = robi.getHeight();

			while (robi.getX() + robiWidth < spaceWidth) {
				robi.translate(new Point(1, 0));
				Tools.sleep(sleepDuration);
			}

			while (robi.getY() + robiHeight < spaceHeight) {
				robi.translate(new Point(0, 1));
				Tools.sleep(sleepDuration);
			}

			while (robi.getX() > 0) {
				robi.translate(new Point(-1, 0));
				Tools.sleep(sleepDuration);
			}

			while (robi.getY() > 0) {
				robi.translate(new Point(0, -1));
				Tools.sleep(sleepDuration);
			}

			Random rand = new Random();
			robi.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
		}
	}

	public static void main(String[] args) {
		new Exercice1_0();
	}
}