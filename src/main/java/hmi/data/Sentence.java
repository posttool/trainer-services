package hmi.data;

import java.util.ArrayList;
import java.util.List;

public class Sentence implements Container, IsContained {
	Paragraph container;
	List<Phrase> phrases;
	Phrase current;
	String text;

	public Sentence() {
		phrases = new ArrayList<Phrase>();
	}

	public Sentence(boolean addDefaultPhrase) {
		this();
		addPhrase(new Phrase());
	}

	public Paragraph getContainer() {
		return container;
	}

	public void addPhrase(Phrase p) {
		p.container = this;
		phrases.add(p);
	}

	public void addWord(Word w) {
		if (!phrases.isEmpty())
			phrases.get(phrases.size() - 1).addWord(w);
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Sentence");
		if (text != null) {
			b.append("[" + text + "]");
		}
		b.append("\n");
		for (Phrase phrase : phrases) {
			b.append(phrase.toString());
		}
		return b.toString();
	}

	public List<Segment> getSegments() {
		List<Segment> segs = new ArrayList<Segment>();
		for (Phrase phrase : phrases) {
			for (Word w : phrase.words) {
				segs.addAll(w.getSegments());
			}
			if (phrase.boundary != null)
				segs.add(phrase.boundary);
		}
		return segs;
	}

	public List<Syllable> getSyllables() {
		List<Syllable> sylls = new ArrayList<Syllable>();
		for (Phrase phrase : phrases) {
			for (Word w : phrase.words) {
				sylls.addAll(w.syllables);
			}
		}
		return sylls;
	}

}
