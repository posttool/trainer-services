package hmi.service;


import hmi.util.HandlebarsTemplateEngine;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import spark.ModelAndView;
import spark.Request;

import static spark.Spark.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RepoService {

    public static String TMP_DIR = System.getProperty("java.io.tmpdir");

    public static void addRoutes() {
        get("/create", (req, res) -> new ModelAndView(new HashMap<>(), "create.html"), new HandlebarsTemplateEngine());

        post("/create", (req, res) -> {
            Map<String, String> workInfo = new HashMap<>();
            workInfo.put("message", "processing file");
            FileItem item = getFileItem(req, "zip");
            doWork(() -> {
                // get uuid for new repo
                String uid = UUID.randomUUID().toString();
                File repoDir = new File("uploadDir=user bucket", uid);//TODO use net file system
                String fileName = item.getName();
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
    }

    public static void doWork(ServiceWork w) throws Exception {
        new Thread(() -> {
            try {
                w.work();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    public static FileItem getFileItem(Request req, String fieldName) throws FileUploadException {
        final File upload = new File(TMP_DIR);
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

    public static void main() {
        addRoutes();
    }
}
