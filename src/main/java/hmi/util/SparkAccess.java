package hmi.util;


import spark.Spark;

public class SparkAccess {
//    static {
//        Spark.before((request, response) -> {
//            String method = request.requestMethod();
//            if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
//                String authentication = request.headers("Authentication");
//                if (!"PASSWORD".equals(authentication)) {
//                    Spark.halt(401, "User Unauthorized");
//                }
//            }
//        });
//    }
    public static void setAccessControl() {
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
        });
    }
}
