package hmi.nlp;

import hmi.data.Sentence;
import hmi.data.SpeechMarkup;
import hmi.data.Word;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class PosTagger {
	private StanfordCoreNLP pipeline;

	public PosTagger() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos");
		pipeline = new StanfordCoreNLP(props);
	}

	public SpeechMarkup process(String t) {
		// TODO process paragraph breaks
		Annotation document = new Annotation(t);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		SpeechMarkup sm = new SpeechMarkup();
		for (CoreMap sentence : sentences) {
			Sentence s = new Sentence(true);
			sm.addSentence(s);
			for (CoreLabel word : sentence.get(TokensAnnotation.class)) {
				Word w = new Word(word.word());
				w.setPos(word.tag());
				s.addWord(w);
			}
		}
		return sm;
	}

	public static void main(String[] args) {
		PosTagger t = new PosTagger();
		SpeechMarkup sm = t.process("This is one. Tis is two.");
		System.out.println(sm);
	}

}
