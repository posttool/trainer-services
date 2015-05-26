package hmi.nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.AfterAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.BeforeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TopicAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.UtteranceAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

class KrawlTagger {
    private StanfordCoreNLP pipeline;
    private String basepath;

    private KrawlTagger(String outfile) throws IOException {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        pipeline = new StanfordCoreNLP(props);
        basepath = outfile;
    }

    public void processRow(BufferedWriter bw, BufferedWriter bw1, String text) throws IOException {

        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // String st = sentence.get(TextAnnotation.class);
            // System.out.println(st);
            StringBuilder b = new StringBuilder();
            String nerType = null;
            StringBuilder nerB = null;
            boolean dollar = false;
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                String pos = token.get(PartOfSpeechAnnotation.class);
                String ne = token.get(NamedEntityTagAnnotation.class);
                String lem = token.get(LemmaAnnotation.class);
                String bf = token.get(BeforeAnnotation.class);
                String af = token.get(AfterAnnotation.class);
                Integer ut = token.get(UtteranceAnnotation.class);
                // String ne = token.get(NamedEntityTagAnnotation.class);
                // System.out.println(word + " " + pos + " " + ne + " " + lem +
                // " " + ut + " '" + bf + "' '" + af + "'");
                if (ne.equals("O")) {
                    if (nerB != null) {
                        bw1.write("> " + nerB + " " + nerType);
                        bw1.newLine();
                    }
                    nerType = null;
                    nerB = null;
                } else if (ne.equals("PERSON") || ne.equals("LOCATION") || ne.equals("ORGANIZATION")
                        || ne.equals("MISC")) {
                    if (nerType == null) {
                        nerType = ne;
                        nerB = new StringBuilder();
                    }
                    nerB.append(word);
                    nerB.append(af);
                } else if (ne.equals("NUMBER") || ne.equals("DURATION") || ne.equals("ORDINAL") || ne.equals("PERCENT")) {
                    if (nerType == null) {
                        nerType = ne;
                        nerB = new StringBuilder();
                    }
                    word = getTextForNumber(word);
                    nerB.append(word);
                    nerB.append(af);
                } else if (ne.equals("MONEY")) {
                    if (nerType == null) {
                        nerType = ne;
                        nerB = new StringBuilder();
                    }
                    if (word.equals("$")) {
                        dollar = true;
                        continue;
                    } else {
                        word = getTextForNumber(word);
                        if (dollar) {
                            word += " dollars";
                            dollar = false;
                        }
                        nerB.append(word);
                        nerB.append(af);
                    }
                } else if (ne.equals("DATE")) {
                    if (nerType == null) {
                        nerType = ne;
                        nerB = new StringBuilder();
                    }
                    word = getTextForYear(word);
                    nerB.append(word);
                    nerB.append(af);
                } else {
                    bw1.write("? " + word + " " + ne);
                    bw1.newLine();
                }
                if (word.equals("``") || word.equals("''") || word.equals("-LRB-") || word.equals("-RRB-")) {
                    b.append(af);
                } else {
                    b.append(word);
                    b.append(af);
                }
            }
            if (nerB != null) {
                bw1.write("> " + nerB + " " + nerType);
                bw1.newLine();
            }
            String bb = b.toString().trim();
            if (!bb.isEmpty()) {
                bw.write(bb);
                bw.newLine();
            }

            // Tree tree = sentence.get(TreeAnnotation.class);
            // processChild((LabeledScoredTreeNode) tree, 0);
            //
            // SemanticGraph dependencies =
            // sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            // System.out.println(dependencies);
        }

        // This is the coreference link graph
        // Each chain stores a set of mentions that link to each other,
        // along with a method for getting the most representative mention
        // Both sentence and token offsets start at 1!
        // Map<Integer, CorefChain> graph =
        // document.get(CorefChainAnnotation.class);
        // String topics = document.get(TopicAnnotation.class);
        // System.out.println("!!!" + topics);

        // System.out.println(graph);
    }

    private static String getTextForNumber(String word) {
        // TODO dehyphenate
        word = word.replace(",", "");
        String nword = null;
        try {
            float f = Float.parseFloat(word);
            int i = (int) Math.floor(f);
            nword = NumberToText.convert(i);
            if (i != f) {
                nword += " point ";
                String r = String.valueOf(f - i);
                for (int j = 1; j < r.length(); j++) {
                    nword += NumberToText.convert(Integer.parseInt(r.substring(j, j + 1)));
                }
            }
        } catch (Exception e) {
        }
        if (nword == null)
            return word;
        else
            return nword;
    }

    private static String getTextForYear(String word) {
        String nword = null;
        try {
            int i = Integer.parseInt(word);
            int t = i / 100;
            int w = i - t * 100;
            if (w == 0)
                nword = NumberToText.convert(i);
            else
                nword = NumberToText.convert(t) + " " + NumberToText.convert(w);
        } catch (Exception e) {
        }
        if (nword == null)
            return word;
        else
            return nword;
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
        // System.out.println(b);
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

    public void processData() throws IOException {
        Mongo mongoClient = new MongoClient("192.155.87.239");
        DB db = mongoClient.getDB("hmi");
        DBCollection channels = db.getCollection("channels");
        DBCollection feeds = db.getCollection("readerfeeds");
        DBCollection contents = db.getCollection("readercontents");
        DBCursor channelsCursor = channels.find();
        int c = 0;
        try {
            while (channelsCursor.hasNext()) {
                DBObject ch = channelsCursor.next();
                String name = ((String) ch.get("name")).toLowerCase();
                System.out.println(c + " " + name);
                if (!name.equals("technology")) {
                    DBCursor feedsCursor = feeds.find(new BasicDBObject("channel", ch.get("_id")));
                    BufferedWriter sentencesOut = getWriter("/gen-" + name + "-s.txt");
                    BufferedWriter entitiesOut = getWriter("/gen-" + name + "-e.txt");
                    while (feedsCursor.hasNext()) {
                        DBObject fe = feedsCursor.next();
                        System.out.println(c + " " + fe.get("name"));
                        DBCursor contentsCursor = contents
                                .find(new BasicDBObject("feed", new BasicDBObject("$in", fe)));
                        while (contentsCursor.hasNext()) {
                            DBObject co = contentsCursor.next();
                            // System.out.println(co.get("text"));
                            processRow(sentencesOut, entitiesOut, (String) co.get("text"));
                            c++;
                            if (c % 10 == 0)
                                System.out.println(c);
                        }
                    }
                    sentencesOut.close();
                    entitiesOut.close();
                }
            }
        } finally {
            channelsCursor.close();
        }
    }

    private BufferedWriter getWriter(String p) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(basepath + p))));
    }

    public static void main(String[] args) throws Exception {
        KrawlTagger tagger = new KrawlTagger("/Users/posttool/Documents/github/hmi-www/app/krawl/data/");
        tagger.processData();

    }

}
