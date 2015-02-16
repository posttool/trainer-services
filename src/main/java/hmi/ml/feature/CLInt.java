package hmi.ml.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CLInt {
    ArrayList<String> list;
    Map<String, Integer> map;

    public CLInt() {
        list = new ArrayList<String>();
        map = new HashMap<String, Integer>();
    }

    public CLInt(int initialRange) {
        list = new ArrayList<String>(initialRange);
        map = new HashMap<String, Integer>();
    }

    public CLInt(String[] strings) {
        list = new ArrayList<String>(Arrays.asList(strings));
        map = new HashMap<String, Integer>();
        for (int i = 0; i < strings.length; i++) {
            map.put(strings[i], i);
        }
    }

    public void set(int i, String s) {
        list.add(i, s);
        map.put(s, i);
    }

    public boolean contains(String s) {
        return map.containsKey(s);
    }

    public boolean contains(int b) {
        int index = b;
        if (index < 0 || index >= list.size())
            return false;
        return true;
    }

    public int get(String s) {
        Integer index = map.get(s);
        if (index == null)
            throw new IllegalArgumentException("No int value known for string [" + s + "]");
        return index.intValue();
    }

    public String get(int i) {
        if (i < 0 || i >= list.size())
            throw new IndexOutOfBoundsException("Int value out of range: " + i);
        return list.get(i);
    }

    public String[] getStringValues() {
        return list.toArray(new String[0]);
    }

    public int getHighestValue() {
        return list.size();
    }
}
