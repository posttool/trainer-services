package hmi.util;


public class Resource {
    public static String path(String s) {
        return Resource.class.getResource(s).getPath();
    }

}
