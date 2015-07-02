package hmi.annotate;

import hmi.data.SpeechMarkup;
import hmi.data.Syllable;
import hmi.data.Word;
import hmi.nlp.NLPipeline;
import hmi.nlp.SpeechMarkupProcessor;
import hmi.phone.PhoneSet;
import hmi.phone.Phonetizer;
import hmi.phone.Syllabifier;
import hmi.util.HandlebarsTemplateEngine;
import spark.ModelAndView;
import spark.Spark;

import java.util.HashMap;
import java.util.List;

import static spark.Spark.*;

public class AnnotationService {
    static {
//        Spark.before((request, response) -> {
//            String method = request.requestMethod();
//            if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
//                String authentication = request.headers("Authentication");
//                if (!"PASSWORD".equals(authentication)) {
//                    Spark.halt(401, "User Unauthorized");
//                }
//            }
//        });
        Spark.options("/*", (request,response)->{
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if(accessControlRequestMethod != null){
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        Spark.before((request,response)->{
            response.header("Access-Control-Allow-Origin", "*");
        });
    }


    public static void main(String[] args) throws Exception {
        String BP = "/Users/posttool/Documents/github/la/src/main/resources";
        NLPipeline nlp = new NLPipeline("en_US");
        SpeechMarkupProcessor markup = new SpeechMarkupProcessor(nlp);
        Phonetizer phonetizer = new Phonetizer(nlp, BP + "/en_US/dict.txt");
        PhoneSet phoneSet = new PhoneSet(BP + "/en_US/phones.xml");

        get("/", (req, res) -> new ModelAndView(new HashMap<>(), "index.html"), new HandlebarsTemplateEngine());

        get("/annotate", "application/json", (req, res) -> {
            SpeechMarkup sm = markup.process(req.queryParams("s"));
            for (Word w : sm.getWords()) {
                phonetizer.addTranscript(w);
                if (w.isVoiced()) {
                    List<Syllable> syllables = Syllabifier.syllabify(phoneSet, w.getPh().toLowerCase());
                    for (Syllable syl : syllables)
                        w.addSyllable(syl);
                }
            }
            return sm.toJSON();
        });
    }

}
