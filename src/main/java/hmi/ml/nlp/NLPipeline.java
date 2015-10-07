package hmi.ml.nlp;

import java.util.Properties;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NLPipeline {
    private StanfordCoreNLP pipeline;
    private String annotators = "tokenize, ssplit, pos, lemma, parse";// ner

    public NLPipeline(String lng) {
        // TODO lng
        // TODO options
        Properties props = new Properties();
        props.setProperty("annotators", annotators);
        pipeline = new StanfordCoreNLP(props);

    }

    public Annotation annotate(String t) {
        Annotation document = new Annotation(t);
        pipeline.annotate(document);
        return document;
    }

    public String getAnnotators() {
        return annotators;
    }
}
