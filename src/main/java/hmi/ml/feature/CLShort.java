package hmi.ml.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CLShort {
	ArrayList<String> list;
	Map<String, Short> map;

	public CLShort() {
		list = new ArrayList<String>();
		map = new HashMap<String, Short>();
	}

	public CLShort(short initialRange) {
		list = new ArrayList<String>(initialRange);
		map = new HashMap<String, Short>();
	}

	public CLShort(String[] strings) {
		if (strings.length > Short.MAX_VALUE)
			throw new IllegalArgumentException("Too many strings for a short-string translator");
		list = new ArrayList<String>(Arrays.asList(strings));
		map = new HashMap<String, Short>();
		for (int i = 0; i < strings.length; i++) {
			map.put(strings[i], (short) i);
		}
	}

	public void set(short b, String s) {
		list.add(b, s);
		map.put(s, b);
	}

	public boolean contains(String s) {
		return map.containsKey(s);
	}

	public boolean contains(short b) {
		int index = (int) b;
		if (index < 0 || index >= list.size())
			return false;
		return true;
	}

	public short get(String s) {
		Short index = map.get(s);
		if (index == null)
			throw new IllegalArgumentException("No short value known for string [" + s + "]");
		return index.shortValue();
	}

	public String get(short b) {
		int index = (int) b;
		if (index < 0 || index >= list.size())
			throw new IndexOutOfBoundsException("Short value out of range: " + index);
		return list.get(index);
	}

	public String[] getStringValues() {
		return list.toArray(new String[0]);
	}

	public short getNumberOfValues() {
		return (short) list.size();
	}

}
