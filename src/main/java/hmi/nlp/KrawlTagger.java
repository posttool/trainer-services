package hmi.nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import edu.stanford.nlp.ling.CoreAnnotations.AfterAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

class KrawlTagger {
    private StanfordCoreNLP pipeline;
    private String basepath;

    private KrawlTagger(String outfile) throws IOException {
        basepath = outfile;
    }

    public void init() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        pipeline = new StanfordCoreNLP(props);
    }

    public void processRow(BufferedWriter bw, BufferedWriter bw1, String text) throws Exception {

        if (pipeline == null)
            throw new Exception("init pipeline first");

        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            StringBuilder b = new StringBuilder();
            String nerType = null;
            StringBuilder nerB = null;
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                // String pos = token.get(PartOfSpeechAnnotation.class);
                String ne = token.get(NamedEntityTagAnnotation.class);
                // String bf = token.get(BeforeAnnotation.class);
                String af = token.get(AfterAnnotation.class);

                // TODO add phrase boundary markers

                if (ne.equals(nerType)) {
                    word = processNer(ne, word);
                    nerB.append(word);
                    nerB.append(af);
                } else {
                    if (nerB != null && !nerType.equals("O")) {
                        bw1.write("> " + nerType + " " + nerB.toString().trim());
                        bw1.newLine();
                    }
                    nerType = ne;
                    nerB = new StringBuilder();
                    word = processNer(ne, word);
                    nerB.append(word);
                    nerB.append(af);
                }

                if (word.equals("&")) {
                    word = "and";
                } else if (word.equals("%")) {
                    word = "percent";
                } else if (word.startsWith("#")) {
                    word = "hashtag " + word.substring(1);
                } else if (word.equals("``") || word.equals("''") || word.equals("-LRB-") || word.equals("-RRB-")
                        || word.equals("`") || word.equals("″")) {
                    word = "";
                }
                b.append(word);
                b.append(af);
            }
            if (nerB != null && !nerType.equals("O")) {
                bw1.write("> " + nerType + " " + nerB.toString().trim());
                bw1.newLine();
            }
            String bb = b.toString().trim();
            if (!bb.isEmpty()) {
                bw.write(bb);
                bw.newLine();
            }
        }
    }

    private String processNer(String ne, String word) {
        if (ne.equals("O")) {
            return word;
        } else if (ne.equals("PERSON") || ne.equals("LOCATION") || ne.equals("ORGANIZATION") || ne.equals("MISC")) {
            return word;
        } else if (ne.equals("NUMBER") || ne.equals("DURATION") || ne.equals("ORDINAL") || ne.equals("PERCENT")) {
            return NumberToText.number(word);
        } else if (ne.equals("MONEY")) {
            // TODO
            if (word.equals("$")) {
                return "";
            } else {
                return NumberToText.number(word);
            }
        } else if (ne.equals("DATE")) {
            return NumberToText.year(word);
        } else if (ne.equals("TIME")) {
            return word;
        } else if (ne.equals("SET")) {
            return word;
        } else {
            System.out.println("?? " + ne);
            return word;
        }
    }

    public void processData() throws Exception {
        init();
        Mongo mongoClient = new MongoClient("192.155.87.239");
        DB db = mongoClient.getDB("hmi");
        DBCollection channels = db.getCollection("channels");
        DBCollection feeds = db.getCollection("readerfeeds");
        DBCollection contents = db.getCollection("readercontents");
        DBCursor channelsCursor = channels.find();
        try {
            while (channelsCursor.hasNext()) {
                DBObject ch = channelsCursor.next();
                String name = ((String) ch.get("name")).toLowerCase();
                System.out.println("> " + name);
                DBCursor feedsCursor = feeds.find(new BasicDBObject("channel", ch.get("_id")));
                BufferedWriter sentencesOut = getWriter("/gen-" + name + "-s.txt");
                BufferedWriter entitiesOut = getWriter("/gen-" + name + "-e.txt");
                BufferedWriter titlesOut = getWriter("/gen-" + name + "-t.txt");
                while (feedsCursor.hasNext()) {
                    DBObject fe = feedsCursor.next();
                    System.out.println(".    " + fe.get("name"));
                    DBCursor contentsCursor = contents.find(new BasicDBObject("feed", fe.get("_id")));
                    int c = 0;
                    while (contentsCursor.hasNext()) {
                        DBObject co = contentsCursor.next();
                        String title = name + "\t" + fe.get("name") + "\t" + co.get("title");
                        System.out.println(title);
                        titlesOut.write(title);
                        titlesOut.newLine();
                        processRow(sentencesOut, entitiesOut, (String) co.get("text"));
                        c++;
                        if (c % 10 == 0)
                            System.out.println(c);
                    }
                }
                sentencesOut.close();
                entitiesOut.close();
                titlesOut.close();
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
