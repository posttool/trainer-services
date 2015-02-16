package hmi.data;

import java.util.ArrayList;
import java.util.List;

public class Document implements Container {
    List<Paragraph> paragraphs;
    Paragraph currentParagraph;

    public Document() {
        paragraphs = new ArrayList<Paragraph>();
        currentParagraph = new Paragraph();
        currentParagraph.container = this;
        paragraphs.add(currentParagraph);
    }

    public void add(Sentence s) {
        currentParagraph.add(s);
    }

}
