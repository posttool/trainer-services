package hmi.data;

import java.util.ArrayList;
import java.util.List;

public class Paragraph implements Container, IsContained {
    Document container;
    List<Sentence> sentences;

    public Paragraph() {
        sentences = new ArrayList<Sentence>();
    }

    public Document getContainer() {
        return container;
    }

    public void add(Sentence s) {
        s.container = this;
        sentences.add(s);
    }

}
