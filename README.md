# trainer-services

Tools for building a voice from text and wavs. 

### Basic use
```java
// open the voice repository and phoneme set for the appropriate language
VoiceRepo repo = new VoiceRepo("jbw-voc");
PhoneSet phoneSet = new PhoneSet(Resource.path("/en_US/phones.xml"));
// convert the text files into SpeechMarkup
ASpeechMarkup asm = new ASpeechMarkup(repo);
asm.compute();
// generate label alignment and integrate with the SpeechMarkup
BAlign aligner = new BAlign(repo);
aligner.compute(phoneSet);
aligner.copyToSpeechMarkup();
// initialize the HTS build process
CInitHTS hts = new CInitHTS(repo);
hts.init();
hts.compute();
// make questions, labels and extract audio features
DDataAll data = new DDataAll(repo);
data.compute(phoneSet);
// create the voice
EVoice voice = new EVoice(repo);
voice.compute();
```
