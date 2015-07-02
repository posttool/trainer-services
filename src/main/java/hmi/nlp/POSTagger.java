package hmi.nlp;

import hmi.data.Paragraph;
import hmi.data.Phrase;
import hmi.data.Sentence;
import hmi.data.SpeechMarkup;
import hmi.data.Word;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class POSTagger {
    private NLP pipeline;

    public POSTagger(NLP pipeline) {
        this.pipeline = pipeline;
    }

    public SpeechMarkup process(String t) {
        Annotation document = pipeline.annotate(t);
        SpeechMarkup sm = new SpeechMarkup();
        Paragraph pp = new Paragraph();
        sm.addParagraph(pp);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            Sentence s = new Sentence();
            Phrase p = new Phrase();
            s.addPhrase(p);
            List<CoreLabel> words = sentence.get(TokensAnnotation.class);
            int sentenceOffsetStart = sentence.get(CharacterOffsetBeginAnnotation.class);
            if (sentenceOffsetStart > 1 && t.substring(sentenceOffsetStart - 2, sentenceOffsetStart).equals("\n\n")
                    && !pp.isEmpty()) {
                pp = new Paragraph();
                sm.addParagraph(pp);
            }
            pp.addSentence(s);
            int ws = words.size();
            for (int i = 0; i < ws; i++) {
                CoreLabel word = words.get(i);
                Word w = new Word(word.word());
                w.setPos(word.tag());
                p.addWord(w);
                if (word.word().equals("--") || word.word().equals(",")) {
                    p = new Phrase();
                    s.addPhrase(p);
                }
            }
            // Tree tree = sentence.get(TreeAnnotation.class);
            // StringBuilder b = new StringBuilder();
            // getTree(b, tree.getChild(0), 0);
            // System.out.println(b);
        }

        return sm;
    }

    public void getTree(StringBuilder b, Tree tree, int depth) {
        if (tree.numChildren() == 0) {
            for (int i = 2; i < depth; i++)
                b.append("  ");
            b.append(tree);
            b.append("\n");
        } else {
            for (Tree child : tree.getChildrenAsList())
                getTree(b, child, depth + 1);
        }
    }

}
