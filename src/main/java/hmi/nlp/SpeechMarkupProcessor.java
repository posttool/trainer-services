package hmi.nlp;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import hmi.data.*;

import java.util.Iterator;
import java.util.List;

public class SpeechMarkupProcessor {
    private NLPipeline pipeline;

    public SpeechMarkupProcessor(NLPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public SpeechMarkup from(String t) {
        Annotation document = pipeline.annotate(t);
        SpeechMarkup sm = new SpeechMarkup();
        Paragraph pp = new Paragraph();
        sm.addParagraph(pp);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            Sentence s = new Sentence();
            Phrase p = new Phrase(1);
            s.addPhrase(p);
            List<CoreLabel> labels = sentence.get(TokensAnnotation.class);
            int sentenceOffsetStart = sentence.get(CharacterOffsetBeginAnnotation.class);
            if (sentenceOffsetStart > 1 && t.substring(sentenceOffsetStart - 2, sentenceOffsetStart).equals("\n\n")
                    && !pp.isEmpty()) {
                pp = new Paragraph();
                sm.addParagraph(pp);
            }
            pp.addSentence(s);
            int ws = labels.size();
            Word lastWord = null;
            for (int i = 0; i < ws; i++) {
                CoreLabel token = labels.get(i);
                if (isPartOfPrevious(token.word())) {
                    lastWord.setText(lastWord.getText() + token.word());
                    lastWord.setPartOfSpeech(lastWord.getPartOfSpeech() + " " + token.tag());
                } else {
                    Word w = new Word(token.word());
                    w.setPartOfSpeech(token.tag());
                    w.setEntity(token.get(NamedEntityTagAnnotation.class));
                    p.addWord(w);
                    lastWord = w;
                }
                // time for a phrase?
                if (token.tag().equals(":")) {
                    p = new Phrase(2);
                    s.addPhrase(p);
                } else if (token.tag().equals(",")) {
                    p = new Phrase(1);
                    s.addPhrase(p);
                }
            }
            Tree tree = sentence.get(TreeAnnotation.class);
            getTree(s.getWords().iterator(), tree.getChild(0), 0);
            // TODO join 1 word phrases w/ previous phrase

        }

        return sm;
    }

    public void getTree(Iterator<Word> words, Tree tree, int depth) {
        //System.out.println(depth + " " + tree);
        if (tree.numChildren() == 0) {
            String ts = tree.toString();
            if (words.hasNext() && !isPartOfPrevious(ts)) {
                Word w = words.next();
                w.setDepth(depth);
                // TODO get the next when ts != w.text
            }
        } else {
            for (Tree child : tree.getChildrenAsList())
                getTree(words, child, depth + 1);
        }
    }

    public boolean isPartOfPrevious(String s) {
        return s.indexOf("'") != -1;
    }
}
