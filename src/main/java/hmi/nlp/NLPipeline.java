package hmi.nlp;

import java.util.Properties;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NLPipeline {
    private StanfordCoreNLP pipeline;

    public NLPipeline(String lng) {
        // TODO lng
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse"); //TODO optional ner
        pipeline = new StanfordCoreNLP(props);

    }

    public Annotation annotate(String t) {
        Annotation document = new Annotation(t);
        pipeline.annotate(document);
        return document;
    }
}
