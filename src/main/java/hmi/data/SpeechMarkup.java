package hmi.data;

import hmi.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// pp
//   sentence
//     phrase
//       text
//         syllable
//           ph

public class SpeechMarkup {
    Document document;
    String text;

    public SpeechMarkup() {
        document = new Document();
    }

    public SpeechMarkup(String text) {
        this();
        this.text = text;
    }

    public List<Paragraph> getParagraphs() {
        return document.paragraphs;
    }

    public void addParagraph(Paragraph p) {
        p.container = document;
        document.paragraphs.add(p);
    }

    public List<Sentence> getSentences() {
        List<Sentence> sentences = new ArrayList<Sentence>();
        for (Paragraph p : document.paragraphs)
            for (Sentence sentence : p.sentences)
                sentences.add(sentence);
        return sentences;
    }

    public List<Word> getWords() {
        List<Word> words = new ArrayList<Word>();
        for (Paragraph p : document.paragraphs)
            for (Sentence sentence : p.sentences)
                words.addAll(sentence.getWords());
        return words;
    }

    public List<Segment> getSegments() {
        List<Segment> segs = new ArrayList<Segment>();
        for (Paragraph p : document.paragraphs)
            for (Sentence sentence : p.sentences)
                segs.addAll(sentence.getSegments());
        return segs;
    }

    public List<Phone> getPhones() {
        List<Phone> phones = new ArrayList<Phone>();
        for (Paragraph p : document.paragraphs)
            for (Sentence sentence : p.sentences)
                for (Phrase ph : sentence.phrases)
                    for (Word w : ph.words)
                        for (Syllable s : w.syllables)
                            for (Phone phone : s.phones)
                                phones.add(phone);
        return phones;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("HMML 0.1\n");
        for (Paragraph p : document.paragraphs) {
            b.append("PP\n");
            for (Sentence sentence : p.sentences) {
                b.append(sentence.toString());
            }
        }
        return b.toString();
    }

    public String getLocale() {
        // TODO
        return "en";
    }

    public String getText() {
        return text;
    }

    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        o.put("hmml", 1);
        o.put("document", document.toJSON());
        return o;
    }

    public void fromJSON(JSONObject o) {
        document.fromJSON((JSONObject) o.get("document"));
    }

    public void readJSON(String filepath) {
        try {
            fromJSON((JSONObject) JSONValue.parse(FileUtils.getFileAsString(new File(filepath), "UTF-8")));
        } catch (IOException e){
            System.err.println("SpeechMarkup.fromJSONFile COULDNT READ "+filepath);
        }
    }
    public void writeJSON(String filepath) {
        FileUtils.writeTextFile(new String[]{this.toJSON().toJSONString()}, filepath);

    }

}
