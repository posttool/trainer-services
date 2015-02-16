package hmi.data;

public enum Stress {
	NONE(""), PRIMARY("'"), SECONDARY(",");

	String ch;

	Stress(String ch) {
		this.ch = ch;
	}

	public String text() {
		return ch;
	}
}
