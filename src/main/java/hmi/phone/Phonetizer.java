package hmi.phone;

import hmi.data.Word;
import hmi.nlp.NLPipeline;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Phonetizer {
    private HashMap<String, String> lex = new HashMap<String, String>();
    private HashMap<String, String> g2pcache = new HashMap<String, String>();
    private NLPipeline pipeline;
    private G2P g2p;

    public Phonetizer(NLPipeline pipeline) {
        this.pipeline = pipeline;
        try {
            this.g2p = new G2P();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Phonetizer(NLPipeline pipeline, String lexfile) throws IOException {
        this(pipeline);
        addLexFile(lexfile);
    }

    private int addLexFile(String lexfile) throws FileNotFoundException, IOException {
        int c = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(lexfile))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (!line.startsWith(";;;")) {
                    int i = line.indexOf(" ");
                    String w = line.substring(0, i);
                    String t = line.substring(i + 2);
                    lex.put(w, t);
                    c++;
                }
            }
        }
        return c;
    }

    Pattern puncpatt = Pattern.compile("\\p{Punct}+");

    public List<Word> getTranscript(String t) {
        List<Word> ss = new ArrayList<Word>();
        Annotation document = pipeline.annotate(t);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel word : sentence.get(TokensAnnotation.class)) {
                Word w = new Word(word.word());
                addTranscript(w);
                ss.add(w);
            }
        }
        return ss;
    }

    public Word addTranscript(Word word) {
        String W = word.getText().toUpperCase();
        String phones = lex.get(W);
        String src = "lex";
        if (phones == null) {
            if (isPunctuation(word)) {
                phones = word.getText();
                src = "";
            } else if (!processSpecialCase(word)) {
                if (this.g2p != null) {
                    phones = g2p_get(W);
                    src = "g2p";
                } else {
                    phones = "?";
                    src = "";
                }
            }
        }
        word.setPh(phones);
        word.setG2P(src);
        return word;
    }

    private String g2p_get(String W) {
        String transcript = g2pcache.get(W);
        if (transcript == null) {
            try {
                transcript = g2p.get(W);
                g2pcache.put(W, transcript);
            } catch (Exception e) {
                transcript = "?";
                e.printStackTrace();
            }
        }
        return transcript;
    }

    public boolean isPunctuation(Word word) {
        return puncpatt.matcher(word.getText()).matches();
    }

    public boolean processSpecialCase(Word word) {
        // if word is
        // number
        // number w/ letters
        // $ amount
        // % amount
        // phone number, etc
        return false;
    }


    public class G2P {
        String hostName = "10.11.12.25";
        int portNumber = 8111;
        String appId = "app-id";

        Socket socket;
        DataOutputStream out;
        DataInputStream in;

        public G2P() throws IOException {
            socket = new Socket(hostName, portNumber);
            socket.setSoTimeout(2000);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        }

        public String get(String word) throws IOException {
            String s = "{\"model\":\"" + appId + "\", \"params\": {\"words\":[\"" + word + "\"],\"nbest\":1,\"band\":500,\"prune\":10}}";
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(s.length());
            byte[] bs = b.array();
            out.write(bs);
            out.write(s.getBytes());

            StringBuilder sb = new StringBuilder();
            int c = in.readInt();
            for (int i = 0; i < c; i++)
                sb.append((char) in.read());
            JSONObject obj = (JSONObject) JSONValue.parse(sb.toString());
            JSONArray a0 = (JSONArray) obj.get(appId);
            for (int i = 0; i < a0.size(); i++) {
                JSONArray a1 = (JSONArray) a0.get(i);
                for (int j = 0; j < a1.size(); j++) {
                    JSONObject o = (JSONObject) a1.get(j);
                    return (String) o.get("pron");
                }
            }
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        NLPipeline nlp = new NLPipeline("en_US");
        Phonetizer p = new Phonetizer(nlp, "/Users/posttool/Documents/github/la/src/main/resources/en_US/dict.txt");
        List<Word> words = p.getTranscript("This is a sentence, about nothing but biazibeetri -- I think.");
        for (Word w : words)
            System.out.println(w.getText() + " / " + w.getPh() + " / " + w.getG2P());
        words = p.getTranscript("Blonkity blobity bipity bap. I think that a smaterbash could be all that.");
        for (Word w : words)
            System.out.println(w.getText() + " / " + w.getPh() + " / " + w.getG2P());
    }
}
