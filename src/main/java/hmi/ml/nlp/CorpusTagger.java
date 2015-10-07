package hmi.ml.nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import edu.stanford.nlp.ling.CoreAnnotations.AfterAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

class CorpusTagger {
    private StanfordCoreNLP pipeline;
    private String basepath;

    private CorpusTagger(String outfile) throws IOException {
        basepath = outfile;
    }

    public void init() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, ner");
        pipeline = new StanfordCoreNLP(props);
    }

    @SuppressWarnings("unchecked")
    public void processRow(BufferedWriter bw, BufferedWriter entityOut, String text) throws Exception {

        if (pipeline == null)
            throw new Exception("init pipeline first");

        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            JSONArray words = new JSONArray();

            String nerType = null;
            StringBuilder nerB = null;
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                String pos = token.get(PartOfSpeechAnnotation.class);
                String ne = token.get(NamedEntityTagAnnotation.class);
                String af = token.get(AfterAnnotation.class);

                JSONObject sentO = new JSONObject();
                sentO.put("word", word);
                if (!ne.equals("O"))
                    sentO.put("ne", ne);
                sentO.put("af", af);
                sentO.put("pos", pos);
                words.add(sentO);

                if (ne.equals(nerType)) {
                    word = processNer(ne, word);
                    nerB.append(word);
                    nerB.append(af);
                } else {
                    if (nerB != null && !nerType.equals("O")) {
                        entityOut.write("> " + nerType + " " + nerB.toString().trim());
                        entityOut.newLine();
                    }
                    nerType = ne;
                    nerB = new StringBuilder();
                    word = processNer(ne, word);
                    nerB.append(word);
                    nerB.append(af);
                }
            }
            if (nerB != null && !nerType.equals("O")) {
                entityOut.write("> " + nerType + " " + nerB.toString().trim());
                entityOut.newLine();
            }

            // Tree tree = sentence.get(TreeAnnotation.class);
            // System.out.println(tree);

            bw.write(words.toJSONString());
            bw.newLine();
        }
    }

    private String processNer(String ne, String word) {
        return word;
    }

//    public void processRow0(BufferedWriter bw, BufferedWriter entityOut, String text) throws Exception {
//
//        if (pipeline == null)
//            throw new Exception("init pipeline first");
//
//        Annotation document = new Annotation(text);
//        pipeline.annotate(document);
//
//        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
//
//        for (CoreMap sentence : sentences) {
//            StringBuilder b = new StringBuilder();
//            String nerType = null;
//            StringBuilder nerB = null;
//            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
//                String word = token.get(TextAnnotation.class);
//                // String pos = token.get(PartOfSpeechAnnotation.class);
//                String ne = token.get(NamedEntityTagAnnotation.class);
//                // String bf = token.get(BeforeAnnotation.class);
//                String af = token.get(AfterAnnotation.class);
//
//                if (ne.equals(nerType)) {
//                    word = processNer(ne, word);
//                    nerB.append(word);
//                    nerB.append(af);
//                } else {
//                    if (nerB != null && !nerType.equals("O")) {
//                        entityOut.write("> " + nerType + " " + nerB.toString().trim());
//                        entityOut.newLine();
//                    }
//                    nerType = ne;
//                    nerB = new StringBuilder();
//                    word = processNer(ne, word);
//                    nerB.append(word);
//                    nerB.append(af);
//                }
//
//                if (word.equals("&")) {
//                    word = "and";
//                } else if (word.equals("%")) {
//                    word = "percent";
//                } else if (word.startsWith("#")) {
//                    word = "hashtag " + word.substring(1);
//                } else if (word.equals("``") || word.equals("''") || word.equals("-LCB-") || word.equals("-LSB-")
//                        || word.equals("-LRB-") || word.equals("-RCB-") || word.equals("-RSB-") || word.equals("-RRB-")
//                        || word.equals("`") || word.equals("″")) {
//                    word = "";
//                }
//                b.append(word);
//                b.append(af);
//            }
//            if (nerB != null && !nerType.equals("O")) {
//                entityOut.write("> " + nerType + " " + nerB.toString().trim());
//                entityOut.newLine();
//            }
//            String bb = b.toString().trim();
//            if (!bb.isEmpty()) {
//                bw.write(bb);
//                bw.newLine();
//            }
//        }
//    }
//
//    private String processNer0(String ne, String word) {
//        if (ne.equals("O")) {
//            return word;
//        } else if (ne.equals("PERSON") || ne.equals("LOCATION") || ne.equals("ORGANIZATION") || ne.equals("MISC")) {
//            return word;
//        } else if (ne.equals("NUMBER") || ne.equals("DURATION") || ne.equals("ORDINAL") || ne.equals("PERCENT")) {
//            return NumberToText.number(word);
//        } else if (ne.equals("MONEY")) {
//            // TODO
//            if (word.equals("$")) {
//                return "";
//            } else {
//                return NumberToText.number(word);
//            }
//        } else if (ne.equals("DATE")) {
//            return NumberToText.year(word);
//        } else if (ne.equals("TIME")) {
//            return word;
//        } else if (ne.equals("SET")) {
//            return word;
//        } else {
//            System.out.println("?? " + ne);
//            return word;
//        }
//    }

    public void processData() throws Exception {
        init();
        Mongo mongoClient = new MongoClient(); // 192.155.87.239
        DB db = mongoClient.getDB("hmi");
        DBCollection channels = db.getCollection("channels");
        DBCollection feeds = db.getCollection("readerfeeds");
        DBCollection contents = db.getCollection("readercontents");
        DBCursor channelsCursor = channels.find().addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        try {
            while (channelsCursor.hasNext()) {
                DBObject ch = channelsCursor.next();
                String name = ((String) ch.get("name")).toLowerCase();
                System.out.println("> " + name);
                if (ok(name)) {
                    DBCursor feedsCursor = feeds.find(new BasicDBObject("channel", ch.get("_id"))).addOption(
                            Bytes.QUERYOPTION_NOTIMEOUT);
                    BufferedWriter sentencesOut = getWriter("/genb-" + name + "-s.txt");
                    BufferedWriter entitiesOut = getWriter("/genb-" + name + "-e.txt");
                    BufferedWriter titlesOut = getWriter("/genb-" + name + "-t.txt");
                    while (feedsCursor.hasNext()) {
                        DBObject fe = feedsCursor.next();
                        System.out.println(".    " + fe.get("name"));
                        DBCursor contentsCursor = contents.find(new BasicDBObject("feed", fe.get("_id"))).addOption(
                                Bytes.QUERYOPTION_NOTIMEOUT);
                        int c = 0;
                        Set<String> titles = new HashSet<String>();
                        while (contentsCursor.hasNext()) {
                            DBObject co = contentsCursor.next();
                            String title = (String) co.get("title");
                            // TODO add a flag to cms that allows user to toggle
                            // unique key (url* or title)
                            // medium.com changes article urls regularly so
                            // there are lots of dupes.
                            if (titles.contains(title))
                                continue;
                            titles.add(title);
                            System.out.println(name + "\t" + fe.get("name") + "\t" + title);
                            titlesOut.write(name + "\t" + fe.get("name") + "\t" + title);
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
            }
        } finally {
            channelsCursor.close();
        }
    }

    private boolean ok(String name) {
        return !(name.equals("news")  || name.equals("technology"));
    }

    private BufferedWriter getWriter(String p) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(basepath + p))));
    }

    public static void main(String[] args) throws Exception {
        CorpusTagger tagger = new CorpusTagger("/Users/posttool/Documents/github/hmi-www/app/krawl/data/");
        tagger.processData();
    }

}
