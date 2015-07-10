package hmi.service;

import hmi.annotate.SpeechMarkupAnnotater;
import hmi.data.SpeechMarkup;
import hmi.data.VoiceRepo;
import hmi.util.HandlebarsTemplateEngine;
import hmi.util.SparkAccess;
import spark.ModelAndView;
import spark.utils.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.staticFileLocation;

public class HMIServices {

    public static void main(String[] args) throws Exception {
        final SpeechMarkupAnnotater annotater = null;//new SpeechMarkupAnnotater("en_US");

        staticFileLocation("/public");

        SparkAccess.setAccessControl();

        get("/", (req, res) -> new ModelAndView(new HashMap<>(), "index.html"), new HandlebarsTemplateEngine());

        get("/annotate", "application/json", (req, res) -> {
            SpeechMarkup sm = annotater.annotate(req.queryParams("s"));
            return sm.toJSON();
        });

        get("/view", (req, res) -> {
            Map<String, Object> data = new HashMap<>();
            String vid = req.queryParams("vid");
            String uid = req.queryParams("uid");
            if (vid == null || uid == null) {
                data.put("error", "Requires vid and uid.");
                return new ModelAndView(data, "error.html");
            }
            try {
                VoiceRepo root = new VoiceRepo(vid);
                SpeechMarkup sm = new SpeechMarkup();
                sm.readJSON(root.path("sm", uid + ".json"));
                data.put("speechMarkupJson", sm.toJSON().toJSONString());
                return new ModelAndView(data, "view.html");
            } catch (Exception e) {
                e.printStackTrace();
                data.put("error", e.getMessage());
                return new ModelAndView(data, "error.html");
            }
        }, new HandlebarsTemplateEngine());

        get("/wav", (req, res) -> {
            String vid = req.queryParams("vid");
            String uid = req.queryParams("uid");
            if (vid == null || uid == null) {
                Map<String, Object> data = new HashMap<>();
                data.put("error", "Requires vid and uid.");
                return new ModelAndView(data, "error.html");
            }
            try {
                VoiceRepo root = new VoiceRepo(vid);
                InputStream inputStream = new FileInputStream(root.path("wav", uid + ".wav"));
                if (inputStream != null) {
                    res.type("audio/x-wav");
                    res.status(200);
                    byte[] buf = new byte[1024];
                    OutputStream os = res.raw().getOutputStream();
                    OutputStreamWriter outWriter = new OutputStreamWriter(os);
                    int count = 0;
                    while ((count = inputStream.read(buf)) >= 0) {
                        os.write(buf, 0, count);
                    }
                    inputStream.close();
                    outWriter.close();
                    return "";
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, Object> data = new HashMap<>();
                data.put("error", e.getMessage());
                return new ModelAndView(data, "error.html");
            }
        });

        get("/work", (req, res) -> new ModelAndView(req.session().attribute("work"), "work.html"), new HandlebarsTemplateEngine());

        get("/work/info", "application/json", (req, res) -> {
            return "no work yet";
        });
    }


}
