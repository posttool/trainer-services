package hmi.data;

public class Boundary extends Segment implements IsContained {
	Phrase container;
	int breakIndex;
	String tone;

	public Boundary() {
	}

	public Phrase getContainer() {
		return container;
	}

	public String getPhone() {
		return "_"; // should come from somewhere
	}
}
