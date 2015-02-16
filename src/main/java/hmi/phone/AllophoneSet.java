package hmi.phone;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AllophoneSet {
    private static Map<String, AllophoneSet> allophoneSets = new HashMap<String, AllophoneSet>();

    public static AllophoneSet getAllophoneSet(String filename) throws Exception {
        InputStream fis = null;
        try {
            fis = new FileInputStream(filename);
        } catch (IOException e) {
            throw new Exception("Problem reading allophone file " + filename, e);
        }
        assert fis != null;
        return getAllophoneSet(fis, filename);
    }

    public static boolean hasAllophoneSet(String identifier) {
        return allophoneSets.containsKey(identifier);
    }

    public static AllophoneSet getAllophoneSetById(String identifier) {
        return allophoneSets.get(identifier);
    }

    public static AllophoneSet getAllophoneSet(InputStream inStream, String identifier) throws Exception {
        AllophoneSet as = allophoneSets.get(identifier);
        if (as == null) {
            try {
                as = new AllophoneSet(inStream);
            } catch (Exception e) {
                throw new Exception("Problem loading allophone set from " + identifier, e);
            }
            allophoneSets.put(identifier, as);
        } else {
            try {
                inStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        assert as != null;
        return as;
    }

    // //////////////////////////////////////////////////////////////////

    private String name;
    private Locale locale;
    private Map<String, Allophone> allophones = null;
    private Map<String, String[]> featureValueMap = null;

    private Allophone silence = null;
    private String ignore_chars = null;
    private int maxAllophoneSymbolLength = 1;

    private AllophoneSet(InputStream inputStream) throws Exception {
        allophones = new TreeMap<String, Allophone>();
        Document document;
        try {
            document = parseDocument(inputStream);
        } catch (Exception e) {
            throw new Exception("Cannot parse allophone file", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
        Element root = document.getDocumentElement();
        name = root.getAttribute("name");
        String xmlLang = root.getAttribute("xml:lang");
        locale = string2locale(xmlLang);
        String[] featureNames = root.getAttribute("features").split(" ");

        if (root.hasAttribute("ignore_chars")) {
            ignore_chars = root.getAttribute("ignore_chars");
        }

        NodeIterator ni = createNodeIterator(document, root, "vowel", "consonant", "silence", "tone");
        Element a;
        while ((a = (Element) ni.nextNode()) != null) {
            Allophone ap = new Allophone(a, featureNames);
            if (allophones.containsKey(ap.name()))
                throw new Exception("File contains duplicate definition of allophone '" + ap.name() + "'!");
            allophones.put(ap.name(), ap);
            if (ap.isPause()) {
                if (silence != null)
                    throw new Exception("File contains more than one silence symbol: '" + silence.name() + "' and '"
                            + ap.name() + "'!");
                silence = ap;
            }
            int len = ap.name().length();
            if (len > maxAllophoneSymbolLength) {
                maxAllophoneSymbolLength = len;
            }
        }
        if (silence == null)
            throw new Exception("File does not contain a silence symbol");
        // Fill the list of possible values for all features
        // such that "0" comes first and all other values are sorted
        // alphabetically
        featureValueMap = new TreeMap<String, String[]>();
        for (String feature : featureNames) {
            Set<String> featureValueSet = new TreeSet<String>();
            for (Allophone ap : allophones.values()) {
                featureValueSet.add(ap.getFeature(feature));
            }
            if (featureValueSet.contains("0"))
                featureValueSet.remove("0");
            String[] featureValues = new String[featureValueSet.size() + 1];
            featureValues[0] = "0";
            int i = 1;
            for (String f : featureValueSet) {
                featureValues[i++] = f;
            }
            featureValueMap.put(feature, featureValues);
        }
        // Special "vc" feature:
        featureValueMap.put("vc", new String[] { "0", "+", "-" });
    }

    public Locale getLocale() {
        return locale;
    }

    public Allophone getAllophone(String ph) {
        Allophone allophone = allophones.get(ph);
        if (allophone == null) {
            throw new IllegalArgumentException(String.format(
                    "Allophone `%s' could not be found in AllophoneSet `%s' (Locale: %s)", ph, name, locale));
        }
        return allophone;
    }

    public Allophone getSilence() {
        return silence;
    }

    public String getIgnoreChars() {
        if (ignore_chars == null) {
            return "',-";
        } else {
            return ignore_chars;
        }
    }

    public String getPhoneFeature(String ph, String featureName) {
        if (ph == null)
            return null;
        Allophone a = allophones.get(ph);
        if (a == null)
            return null;
        return a.getFeature(featureName);
    }

    public Set<String> getPhoneFeatures() {
        return Collections.unmodifiableSet(featureValueMap.keySet());
    }

    public String[] getPossibleFeatureValues(String featureName) {
        String[] vals = featureValueMap.get(featureName);
        if (vals == null)
            throw new IllegalArgumentException("No such feature: " + featureName);
        return vals;
    }

    public Set<String> getAllophoneNames() {
        Iterator<String> it = allophones.keySet().iterator();
        Set<String> allophoneKeySet = new TreeSet<String>();
        while (it.hasNext()) {
            String keyString = it.next();
            if (!allophones.get(keyString).isTone()) {
                allophoneKeySet.add(keyString);
            }
        }
        return allophoneKeySet;
    }

    public Allophone[] splitIntoAllophones(String allophoneString) {
        List<String> phones = splitIntoAllophoneList(allophoneString, false);
        Allophone[] allos = new Allophone[phones.size()];
        for (int i = 0; i < phones.size(); i++) {
            try {
                allos[i] = getAllophone(phones.get(i));
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }
        return allos;
    }

    public String splitAllophoneString(String allophoneString) {
        List<String> phones = splitIntoAllophoneList(allophoneString, true);
        StringBuilder pronunciation = new StringBuilder();
        for (String a : phones) {
            if (pronunciation.length() > 0)
                pronunciation.append(" ");
            pronunciation.append(a);
        }
        return pronunciation.toString();
    }

    public List<String> splitIntoAllophoneList(String allophonesString) {
        return splitIntoAllophoneList(allophonesString, true);
    }

    public List<String> splitIntoAllophoneList(String allophoneString, boolean includeStressAndSyllableMarkers) {
        List<String> phones = new ArrayList<String>();
        for (int i = 0; i < allophoneString.length(); i++) {
            String one = allophoneString.substring(i, i + 1);

            // Allow modification of ignore characters in allophones.xml
            if (getIgnoreChars().contains(one)) {
                if (includeStressAndSyllableMarkers)
                    phones.add(one);
                continue;
            } else if (one.equals(" ")) {
                continue;
            }
            // Try to cut off individual segments,
            // starting with the longest prefixes:
            String ph = null;
            for (int l = maxAllophoneSymbolLength; l >= 1; l--) {
                if (i + l <= allophoneString.length()) {
                    ph = allophoneString.substring(i, i + l);
                    // look up in allophone map:
                    if (allophones.containsKey(ph)) {
                        // OK, found a symbol of length l.
                        i += l - 1; // together with the i++ in the for loop,
                                    // move by l
                        break;
                    }
                }
            }
            if (ph != null && allophones.containsKey(ph)) {
                // have found a valid phone
                phones.add(ph);
            } else {
                throw new IllegalArgumentException("Found unknown symbol `" + allophoneString.charAt(i)
                        + "' in phonetic string `" + allophoneString + "' -- ignoring.");
            }
        }
        return phones;
    }

    public boolean checkAllophoneSyntax(String allophoneString) {
        try {
            splitIntoAllophoneList(allophoneString, false);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // MISC UTIL
    public static Document parseDocument(InputStream inputStream) throws ParserConfigurationException, SAXException,
            IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(true);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(inputStream));
    }

    public static Locale string2locale(String localeString) {
        Locale locale = null;
        StringTokenizer localeST = new StringTokenizer(localeString, "_-");
        String language = localeST.nextToken();
        String country = "";
        String variant = "";
        if (localeST.hasMoreTokens()) {
            country = localeST.nextToken();
            if (localeST.hasMoreTokens()) {
                variant = localeST.nextToken();
            }
        }
        locale = new Locale(language, country, variant);
        return locale;
    }

    private class NameNodeFilter implements NodeFilter {
        private String[] names;

        public NameNodeFilter(String... names) {
            if (names == null)
                throw new NullPointerException("Cannot filter on null names");
            this.names = names;
            for (int i = 0; i < names.length; i++) {
                if (names[i] == null)
                    throw new NullPointerException("Cannot filter on null name");
            }
        }

        public short acceptNode(Node n) {
            String name = n.getNodeName();
            for (int i = 0; i < names.length; i++) {
                if (name.equals(names[i]))
                    return NodeFilter.FILTER_ACCEPT;
            }
            return NodeFilter.FILTER_SKIP;
        }
    }

    private NodeIterator createNodeIterator(Document doc, Node root, String... tagNames) {
        return ((DocumentTraversal) doc).createNodeIterator(root, NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(tagNames), false);
    }

    // private NodeIterator createNodeIterator(Node root, String... tagNames) {
    // return createNodeIterator(root.getNodeType() == Node.DOCUMENT_NODE ?
    // (Document) root : root.getOwnerDocument(),
    // root, tagNames);
    // }

}
