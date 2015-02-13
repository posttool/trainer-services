package hmi.nlp;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TopicAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

class Tagger {
	private StanfordCoreNLP pipeline;

	private Tagger() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = new StanfordCoreNLP(props);
	}

	public void tagEverything(String text) {

		Annotation document = new Annotation(text);
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(TextAnnotation.class);
				String pos = token.get(PartOfSpeechAnnotation.class);
				String ne = token.get(NamedEntityTagAnnotation.class);
				System.out.println(word + " " + pos + " " + ne);
			}

			Tree tree = sentence.get(TreeAnnotation.class);
			processChild((LabeledScoredTreeNode) tree, 0);

			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			System.out.println(dependencies);
		}

		// This is the coreference link graph
		// Each chain stores a set of mentions that link to each other,
		// along with a method for getting the most representative mention
		// Both sentence and token offsets start at 1!
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		String topics = document.get(TopicAnnotation.class);
		System.out.println("!!!" + topics);

		System.out.println(graph);
	}

	private void processChild(LabeledScoredTreeNode ltree, int d) {
		List<Tree> c = ltree.getChildrenAsList();
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < d; i++) {
			b.append("  ");
		}
		CoreLabel l = (CoreLabel) ltree.label();
		String w = l.word();
		if (w != null) {
			b.append(w);
		} else {
			String t = l.value();
			b.append(t);
		}
		for (Tree t : c) {
			processChild((LabeledScoredTreeNode) t, d + 1);
		}
		System.out.println(b);
	}

	public List<List<CoreLabel>> pos(String text) {
		List<List<CoreLabel>> tagged = new ArrayList<List<CoreLabel>>();
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			tagged.add(sentence.get(TokensAnnotation.class));
		}
		return tagged;
	}

	public void processData() throws UnknownHostException {
		Mongo mongoClient = new Mongo("localhost");
		DB db = mongoClient.getDB("hmi");
		DBCollection content = db.getCollection("readercontents");
		DBCursor cursor = content.find();
		try {
			int c = 0;
			while (cursor.hasNext() && c < 10) {
				DBObject o = cursor.next();
				processRow((String) o.get("text"));
				c++;
			}
		} finally {
			cursor.close();
		}
	}

	public void processRowPOS(String t) {
		List<List<CoreLabel>> tagged = pos(t);
		for (List<CoreLabel> sentences : tagged) {
			for (CoreLabel taggedWord : sentences) {
				String tag = taggedWord.tag(); // same as
												// taggedWord.get(CoreAnnotations.PartOfSpeechAnnotation.class);
				String text = taggedWord.word();
				System.out.print(tag + " ");
			}
			System.out.println();
		}
	}

	public void processRow(String t) {
		tagEverything(t);

	}

	public static void main(String[] args) throws Exception {
		Tagger tagger = new Tagger();
		tagger.processData();

	}

}
