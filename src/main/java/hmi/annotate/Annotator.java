package hmi.annotate;

import hmi.data.SpeechMarkup;

public interface Annotator {
	public SpeechMarkup annotate(SpeechMarkup sm);
}
