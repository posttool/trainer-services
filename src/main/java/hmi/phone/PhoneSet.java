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

public class PhoneSet {
    private static Map<String, PhoneSet> phoneSets = new HashMap<String, PhoneSet>();

    public static PhoneSet getPhoneSet(String filename) throws Exception {
        InputStream fis = null;
        try {
            fis = new FileInputStream(filename);
        } catch (IOException e) {
            throw new Exception("Problem reading phone file " + filename, e);
        }
        assert fis != null;
        return getPhoneSet(fis, filename);
    }

    public static boolean hasPhoneSet(String identifier) {
        return phoneSets.containsKey(identifier);
    }

    public static PhoneSet getPhoneSetById(String identifier) {
        return phoneSets.get(identifier);
    }

    public static PhoneSet getPhoneSet(InputStream inStream, String identifier) throws Exception {
        PhoneSet as = phoneSets.get(identifier);
        if (as == null) {
            try {
                as = new PhoneSet(inStream);
            } catch (Exception e) {
                throw new Exception("Problem loading phone set from " + identifier, e);
            }
            phoneSets.put(identifier, as);
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
    private Map<String, PhoneEl> _phones = null;
    private Map<String, String[]> featureValueMap = null;

    private PhoneEl silence = null;
    private String ignore_chars = null;
    private int maxPhoneSymbolLength = 1;

    private PhoneSet(InputStream inputStream) throws Exception {
        _phones = new TreeMap<String, PhoneEl>();
        Document document;
        try {
            document = parseDocument(inputStream);
        } catch (Exception e) {
            throw new Exception("Cannot parse phone file", e);
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
            PhoneEl ap = new PhoneEl(a, featureNames);
            if (_phones.containsKey(ap.name()))
                throw new Exception("File contains duplicate definition of phone '" + ap.name() + "'!");
            _phones.put(ap.name(), ap);
            if (ap.isPause()) {
                if (silence != null)
                    throw new Exception("File contains more than one silence symbol: '" + silence.name() + "' and '"
                            + ap.name() + "'!");
                silence = ap;
            }
            int len = ap.name().length();
            if (len > maxPhoneSymbolLength) {
                maxPhoneSymbolLength = len;
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
            for (PhoneEl ap : _phones.values()) {
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

    public PhoneEl getPhone(String ph) {
        PhoneEl phone = _phones.get(ph);
        if (phone == null) {
            throw new IllegalArgumentException(String.format(
                    "Phone `%s' could not be found in PhoneSet `%s' (Locale: %s)", ph, name, locale));
        }
        return phone;
    }

    public PhoneEl getSilence() {
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
        PhoneEl a = _phones.get(ph);
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

    public Set<String> getPhoneNames() {
        Iterator<String> it = _phones.keySet().iterator();
        Set<String> phoneKeySet = new TreeSet<String>();
        while (it.hasNext()) {
            String keyString = it.next();
            if (!_phones.get(keyString).isTone()) {
                phoneKeySet.add(keyString);
            }
        }
        return phoneKeySet;
    }

    public PhoneEl[] splitIntoPhones(String phoneString) {
        List<String> phones = splitIntoPhoneList(phoneString, false);
        PhoneEl[] phs = new PhoneEl[phones.size()];
        for (int i = 0; i < phones.size(); i++) {
            try {
                phs[i] = getPhone(phones.get(i));
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }
        return phs;
    }

    public String splitPhoneString(String phoneString) {
        List<String> phones = splitIntoPhoneList(phoneString, true);
        StringBuilder pronunciation = new StringBuilder();
        for (String a : phones) {
            if (pronunciation.length() > 0)
                pronunciation.append(" ");
            pronunciation.append(a);
        }
        return pronunciation.toString();
    }

    public List<String> splitIntoPhoneList(String phonesString) {
        return splitIntoPhoneList(phonesString, true);
    }

    public List<String> splitIntoPhoneList(String phoneString, boolean includeStressAndSyllableMarkers) {
        List<String> phones = new ArrayList<String>();
        for (int i = 0; i < phoneString.length(); i++) {
            String one = phoneString.substring(i, i + 1);

            // Allow modification of ignore characters in phones.xml
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
            for (int l = maxPhoneSymbolLength; l >= 1; l--) {
                if (i + l <= phoneString.length()) {
                    ph = phoneString.substring(i, i + l);
                    // look up in phone map:
                    if (_phones.containsKey(ph)) {
                        // OK, found a symbol of length l.
                        i += l - 1; // together with the i++ in the for loop,
                                    // move by l
                        break;
                    }
                }
            }
            if (ph != null && _phones.containsKey(ph)) {
                // have found a valid phone
                phones.add(ph);
            } else {
                throw new IllegalArgumentException("Found unknown symbol `" + phoneString.charAt(i)
                        + "' in phonetic string `" + phoneString + "' -- ignoring.");
            }
        }
        return phones;
    }

    public boolean checkPhoneSyntax(String phoneString) {
        try {
            splitIntoPhoneList(phoneString, false);
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
