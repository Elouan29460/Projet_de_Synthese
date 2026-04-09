package exercice5;

import stree.parser.SNode;

public interface Command {
	abstract public Reference run(Reference receiver, SNode method);
}
