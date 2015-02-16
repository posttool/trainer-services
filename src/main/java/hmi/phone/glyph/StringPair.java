package hmi.phone.glyph;

public class StringPair {

	private String s1;
	private String s2;

	public StringPair(String s1, String s2) {
		this.s1 = s1;
		this.s2 = s2;
	}

	public void setString1(String s1) {
		this.s1 = s1;
	}

	public void setString2(String s2) {
		this.s2 = s2;
	}

	public int hashCode() {
		return 31 * s1.hashCode() + s2.hashCode();
	}

	public boolean equals(Object o) {

		if (o instanceof StringPair) {
			StringPair sp = (StringPair) o;
			return sp.getString1().equals(s1) && sp.getString2().equals(s2);
		}
		return false;
	}

	public String getString1() {
		return s1;
	}

	public String getString2() {
		return s2;
	}

	public String toString() {
		return s1 + " " + s2;
	}
}
