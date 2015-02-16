package hmi.phone;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import hmi.data.Phone;

public class Allophone extends Phone {

	Map<String, String> features;

	public Allophone(Element a, String[] featureNames) {
		String text = a.getAttribute("ph");
		String vc;
		String isTone;
		if (text.equals(""))
			throw new IllegalArgumentException("Element must have a 'ph' attribute");
		setPhone(text);
		if (a.getTagName().equals("consonant")) {
			vc = "-";
			isTone = "-";
		} else if (a.getTagName().equals("vowel")) {
			vc = "+";
			isTone = "-";
		} else if (a.getTagName().equals("silence")) {
			vc = "0";
			isTone = "-";
		} else if (a.getTagName().equals("tone")) {
			vc = "0";
			isTone = "+";
		} else {
			throw new IllegalArgumentException("Element must be one of <vowel>, <consonant> and <silence>, but is <"
					+ a.getTagName() + ">");
		}
		Map<String, String> feats = new HashMap<String, String>();
		feats.put("vc", vc);
		feats.put("isTone", isTone);
		for (String f : featureNames) {
			feats.put(f, getAttribute(a, f));
		}
		features = Collections.unmodifiableMap(feats);
	}

	private String getAttribute(Element e, String att) {
		String val = e.getAttribute(att);
		if (val.equals(""))
			return "0";
		return val;
	}

	public Map<String, String> getFeatures() {
		return features;
	}

	public String getFeature(String name) {
		return features.get(name);
	}

	public boolean isVowel() {
		return "+".equals(features.get("vc"));
	}

	public boolean isDiphthong() {
		assert isVowel();
		return "d".equals(features.get("vlng"));
	}

	public boolean isSyllabic() {
		return isVowel();
	}

	public boolean isConsonant() {
		return "-".equals(features.get("vc"));
	}

	public boolean isVoiced() {
		return isVowel() || "+".equals(features.get("cvox"));
	}

	public boolean isSonorant() {
		return "lnr".contains(features.get("ctype"));
	}

	public boolean isLiquid() {
		return "l".equals(features.get("ctype"));
	}

	public boolean isNasal() {
		return "n".equals(features.get("ctype"));
	}

	public boolean isGlide() {
		return "r".equals(features.get("ctype")) && !isVowel();
	}

	public boolean isFricative() {
		return "f".equals(features.get("ctype"));
	}

	public boolean isPlosive() {
		return "s".equals(features.get("ctype"));
	}

	public boolean isAffricate() {
		return "a".equals(features.get("ctype"));
	}

	public boolean isPause() {
		return "0".equals(features.get("vc")) && "-".equals(features.get("isTone"));
	}

	public boolean isTone() {
		return "+".equals(features.get("isTone"));
	}

	public int sonority() {
		if (isVowel()) {
			String vlng = features.get("vlng");
			if (vlng == null)
				return 5; // language doesn't make a distinction between vowels
							// of different length
			if ("ld".contains(vlng))
				return 6;
			else if ("s".equals(vlng))
				return 5;
			else if ("a".equals(vlng))
				return 4;
			else
				return 5; // unknown vowel length
		} else if (isSonorant())
			return 3;
		else if (isFricative())
			return 2;
		return 1;
	}
}
