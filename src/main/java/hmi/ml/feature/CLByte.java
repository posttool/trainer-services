package hmi.ml.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CLByte {
	public static final int MAXNUM = 255;

	private ArrayList<String> list;
	private Map<String, Byte> map;

	public CLByte() {
		list = new ArrayList<String>();
		map = new HashMap<String, Byte>();
	}

	public CLByte(int initialRange) {
		int range = initialRange & 0xFF;
		list = new ArrayList<String>(range);
		map = new HashMap<String, Byte>();
	}

	public CLByte(String[] strings) {
		if (strings.length > MAXNUM) {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < strings.length; i++) {
				buf.append("\"" + strings[i] + "\" ");
			}
			throw new IllegalArgumentException("Too many strings for a byte-string translator: \n" + buf.toString()
					+ "(" + strings.length + " strings)");
		}
		list = new ArrayList<String>(Arrays.asList(strings));
		map = new HashMap<String, Byte>();
		for (int i = 0; i < strings.length; i++) {
			map.put(strings[i], (byte) i);
		}

	}

	public void set(byte b, String s) {
		int index = b & 0xFF; // make sure we treat the byte as an unsigned byte
								// for position
		list.add(index, s);
		map.put(s, b);
	}

	public boolean contains(String s) {
		return map.containsKey(s);
	}

	public boolean contains(byte b) {
		int index = b & 0xFF;
		if (index < 0 || index >= list.size())
			return false;
		return true;
	}

	public byte get(String s) {
		Byte b = map.get(s);
		if (b == null)
			throw new IllegalArgumentException("No byte value known for string [" + s + "]");
		return b.byteValue();
	}

	public String get(byte b) {
		int index = b & 0xFF;
		if (index < 0 || index >= list.size())
			throw new IndexOutOfBoundsException("Byte value out of range: " + index);
		return list.get(index);
	}

	public String[] getStringValues() {
		return list.toArray(new String[0]);
	}

	public int getNumberOfValues() {
		return list.size();
	}

}
