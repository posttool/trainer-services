package hmi.service;

import hmi.annotate.Annotater;
import hmi.data.SpeechMarkup;
import hmi.util.HandlebarsTemplateEngine;
import hmi.util.SparkAccess;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import spark.ModelAndView;
import spark.Request;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static spark.Spark.*;

public class HMIServices {
    static final String tmp = System.getProperty("java.io.tmpdir");
    static final String dir = "/Users/posttool/Documents/hmi-repo";

    public static void main(String[] args) throws Exception {
        final Annotater annotater = null;//new Annotater("en_US");

        staticFileLocation("/public");

        SparkAccess.setAccessControl();

        get("/", (req, res) -> new ModelAndView(new HashMap<>(), "index.html"), new HandlebarsTemplateEngine());

        get("/annotate", "application/json", (req, res) -> {
            SpeechMarkup sm = annotater.annotate(req.queryParams("s"));
            return sm.toJSON();
        });

        get("/create", (req, res) -> new ModelAndView(new HashMap<>(), "create.html"), new HandlebarsTemplateEngine());

        post("/create", (req, res) -> {
            Map<String, String> workInfo = new HashMap<>();
            workInfo.put("message", "processing file");
            FileItem item = getFileItem(req, "zip");
            doWork(() -> {
                // get uuid for new repo
                String uid = UUID.randomUUID().toString();
                String fileName = item.getName();
                File repoDir = new File(dir, uid);
                repoDir.mkdir();
                File f = new File(repoDir, fileName);
                item.write(f);
                workInfo.put("message", "checked in");
                // unzip
                try {
                    ZipFile zipFile = new ZipFile(f);
                    zipFile.extractAll(repoDir.getAbsolutePath());
                } catch (ZipException e) {
                    e.printStackTrace();
                    workInfo.put("message", e.getMessage());
                    f.delete();
                    repoDir.delete();
                    return false;
                }
                workInfo.put("message", "unzipped");
                // validate
                File text = new File(repoDir, "text");
                File wav = new File(repoDir, "wav");
                if (text.exists() && wav.exists()) {
                    workInfo.put("message", "validated");
                    workInfo.put("uid", uid.toString());
                    return true;
                } else {
                    workInfo.put("message", "no text/wav files");
                    return false;
                }
            });
            req.session().attribute("work", workInfo);
            res.redirect("/work");
            return null;
        });

        get("/work", (req, res) -> new ModelAndView(req.session().attribute("work"), "work.html"), new HandlebarsTemplateEngine());
    }

    public static void doWork(Work w) throws Exception {
        new Thread(() -> {
            try {
                w.work();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    interface Work {
        boolean work() throws Exception;
    }

    public static FileItem getFileItem(Request req, String fieldName) throws FileUploadException {
        final File upload = new File(tmp);
        if (!upload.exists() && !upload.mkdirs()) {
            throw new RuntimeException("Failed to create directory " + upload.getAbsolutePath());
        }
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(upload);
        ServletFileUpload fileUpload = new ServletFileUpload(factory);
        List<FileItem> items = fileUpload.parseRequest(req.raw());
        return items.stream()
                .filter(e -> fieldName.equals(e.getFieldName()))
                .findFirst().get();

    }


}
