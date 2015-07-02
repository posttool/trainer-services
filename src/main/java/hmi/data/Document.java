package hmi.data;

import java.util.ArrayList;
import java.util.List;

public class Document implements Container {
    List<Paragraph> paragraphs;
    Paragraph currentParagraph;

    public Document() {
        paragraphs = new ArrayList<Paragraph>();
    }

}
