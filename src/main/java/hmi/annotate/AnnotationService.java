package hmi.annotate;

import hmi.data.SpeechMarkup;
import hmi.util.HandlebarsTemplateEngine;
import hmi.util.SparkAccess;
import spark.ModelAndView;

import java.util.HashMap;

import static spark.Spark.get;

public class AnnotationService {

    public static void main(String[] args) throws Exception {
        SparkAccess.setAccessControl();
        Annotater annotater = new Annotater("en_US");

        get("/", (req, res) -> new ModelAndView(new HashMap<>(), "index.html"), new HandlebarsTemplateEngine());

        get("/annotate", "application/json", (req, res) -> {
            SpeechMarkup sm = annotater.annotate(req.queryParams("s"));
            return sm.toJSON();
        });
    }

}
