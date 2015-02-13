package hmi.data;

import java.util.ArrayList;
import java.util.List;

public class Phrase implements Container, IsContained {
	Sentence container;
	List<Word> words;
	Boundary boundary;
	String text;
	String prosodyRange;
	String prosodyPitch;

	public Phrase() {
		words = new ArrayList<Word>();
	}

	public Sentence getContainer() {
		return container;
	}

	public void addWord(Word w) {
		w.container = this;
		words.add(w);
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("  Phrase");
		if (text != null) {
			b.append("[" + text + "]");
		}
		b.append("\n");
		for (Word word : words) {
			b.append(word.toString());
		}
		return b.toString();
	}

}
