package hmi.nlp;

import hmi.data.SpeechMarkup;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class PosTagger {
    private StanfordCoreNLP pipeline;

    public PosTagger() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        pipeline = new StanfordCoreNLP(props);
    }

    public SpeechMarkup process(String t) {
        // TODO process paragraph breaks
        Annotation document = new Annotation(t);
        pipeline.annotate(document);
        SpeechMarkup sm = new SpeechMarkup();
        // List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        // for (CoreMap sentence : sentences) {
        // Sentence s = new Sentence(true);
        // sm.addSentence(s);
        // for (CoreLabel word : sentence.get(TokensAnnotation.class)) {
        // Word w = new Word(word.word());
        // w.setPos(word.tag());
        // s.addWord(w);
        // }
        // }
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            Tree tree = sentence.get(TreeAnnotation.class);
            StringBuilder b = new StringBuilder();
            getTree(b, tree.getChild(0), 0);
            System.out.println(b);
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

    public static void main(String[] args) {
        PosTagger t = new PosTagger();
        SpeechMarkup sm = t.process("This is one. Tis is two. How do you do, if you don't mind me asking? "
                + "Furthermore, it stands to reason that I wouldnt use a comma here but would in San Francisco, California. "
                + "Why wouldn't you eat out when you could eat on Mars?");
        System.out.println(sm);
    }

}
