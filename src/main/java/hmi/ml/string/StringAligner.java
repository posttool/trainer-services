package hmi.ml.string;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This trains an alignment model between Strings.
 * 
 * The basic idea is to perform a Levenshtein search for the cheapest path and
 * read alignment from that. The costs used in the distance computation are
 * estimated in an iterative process, according to the log of the relative
 * frequencies of the respective operations in the previous iteration. Perform
 * several iterations (e.g. 4) of aligning in order to get stable estimates of
 * the costs (and a good alignment in turn).
 * 
 * The algorithm is implemented as it can be found in Wikipedia:
 * http://en.wikipedia.org/w/index.php?title=Levenshtein_distance&oldid=
 * 349201802#Computing_Levenshtein_distance
 *
 */
public class StringAligner {

    private static final double logOf2 = Math.log(2.0);

    // cost of translating first element of the pair into the second
    private HashMap<StringPair, Integer> aligncost;

    private int defaultcost = 10;
    // cost of deleting an element
    private int skipcost;

    // input side (eg. graphemes) split into symbols
    protected List<String[]> inSplit;
    // output side (eg. phones) split into symbols
    protected List<String[]> outSplit;

    // optional info, eg. part-of-speech
    protected List<String> optInfo;

    // protected Logger logger;

    private boolean inIsOut;

    /**
     * @param inIsOutAlphabet
     *            boolean indicating as input and output strings should be
     *            considered as belonging to the same symbol sets (alignment
     *            between identical symbol is then cost-free)
     */
    public StringAligner(boolean inIsOutAlphabet, boolean hasOptInfo) {

        this.skipcost = this.defaultcost;
        this.aligncost = new HashMap<StringPair, Integer>();

        this.inSplit = new ArrayList<String[]>();
        this.outSplit = new ArrayList<String[]>();

        this.inIsOut = inIsOutAlphabet;
        if (hasOptInfo) {
            this.optInfo = new ArrayList<String>();
        }
    }

    public StringAligner() {
        this(false, false);
    }

    public void add(List<String> inStr, List<String> outStr) {
        inSplit.add(inStr.toArray(new String[] {}));
        outSplit.add(outStr.toArray(new String[] {}));
    }

    public void add(String[] inStr, String[] outStr) {
        inSplit.add(inStr);
        outSplit.add(outStr);
    }

    public int size() {
        return inSplit.size();
    }

    /**
     * One iteration of alignment, using adapted Levenshtein distance. After the
     * iteration, the costs between a grapheme and a phone are set by the log
     * probability of the phone given the grapheme. Analogously, the deletion
     * cost is set by the log of deletion probability. In the first iteration,
     * all operations cost maxCost.
     */
    public void alignIteration() {

        // this counts how many times a symbol is mapped to symbols
        Map<String, Integer> symMapCount = new HashMap<String, Integer>();

        // this counts how often particular mappings from one symbol to another
        // occurred
        Map<StringPair, Integer> sym2symCount = new HashMap<StringPair, Integer>();

        // how many symbols are on input side
        int symCount = 0;

        // how many symbols are deleted
        int symDels = 0;

        // for every alignment pair collect counts
        for (int i = 0; i < outSplit.size(); i++) {

            String[] in = inSplit.get(i);
            String[] out = outSplit.get(i);
            int[] alignment = getAlignment(in, out);

            symCount += in.length;

            int pre = 0;

            // for every input symbol...
            for (int inNr = 0; inNr < in.length; inNr++) {

                if (alignment[inNr] == pre) {
                    // is mapped to empty string
                    symDels++;
                } else {
                    // mapped to one or several symbols

                    // increase count of overall mappings for this symbol
                    Integer c = symMapCount.get(in[inNr]);
                    if (null == c) {
                        symMapCount.put(in[inNr], alignment[inNr] - pre);
                    } else {
                        symMapCount.put(in[inNr], c + alignment[inNr] - pre);
                    }

                    // for every corresponding output symbol
                    for (int outNr = pre; outNr < alignment[inNr]; outNr++) {

                        // get key for mapping symbol to symbol
                        StringPair key = new StringPair(in[inNr], out[outNr]);

                        Integer mapC = sym2symCount.get(key);
                        if (null == mapC) {
                            sym2symCount.put(key, 1);
                        } else {
                            sym2symCount.put(key, 1 + mapC);
                        }
                    } // ...for each output-symbol
                } // ...if > 0 output-symbols
                pre = alignment[inNr];
            } // ...for each input symbol
        } // ...for each input string

        // now build fractions, to estimate the new costs

        // first reset skip costs
        double delFraction = (double) symDels / symCount;
        skipcost = (int) -log2(delFraction);

        // now reset aligncosts
        aligncost.clear();

        for (StringPair mapping : sym2symCount.keySet()) {

            String firstSym = mapping.getString1();

            double fraction = (double) sym2symCount.get(mapping) / symMapCount.get(firstSym);
            int cost = (int) -log2(fraction);

            if (cost < defaultcost) {
                aligncost.put(mapping, cost);
            }
        }
    }

    public StringPair[] get(int entryNr) {

        String[] in = inSplit.get(entryNr);
        String[] out = outSplit.get(entryNr);
        int[] align = getAlignment(in, out);

        StringPair[] listArray = new StringPair[in.length];

        int pre = 0;
        for (int pos = 0; pos < in.length; pos++) {
            String inStr = in[pos];
            String oStr = "";
            for (int alPos = pre; alPos < align[pos]; alPos++) {
                oStr += out[alPos];
            }
            pre = align[pos];
            listArray[pos] = new StringPair(inStr, oStr);
        }

        return listArray;
    }

    private double log2(double d) {
        return Math.log(d) / logOf2;
    }

    /**
     *
     * This computes the alignment that has the lowest distance between two
     * Strings with three differences to the normal Levenshtein-distance:
     *
     * 1. Only insertions and deletions are allowed, no replacements (i.e. no
     * "diagonal" transitions) 2. insertion costs are dependent on a particular
     * phone on the input side (the one they are aligned to) 3. deletion is
     * equivalent to a symbol on the input side that is not aligned. There are
     * costs associated with that.
     *
     * For each input symbol the method returns the index of the right alignment
     * boundary. For input ['a','b'] and output ['a','a','b'] a correct
     * alignment would be: [2,3]
     *
     */
    public int[] getAlignment(String[] istr, String[] ostr) {

        StringPair key = new StringPair(null, null);

        // distances:
        // 1. previous distance (= previous column in matrix)
        int[] p_d = new int[ostr.length + 1];
        // 2. current distance
        int[] d = new int[ostr.length + 1];
        // 3. dummy array for swapping, when switching to new column
        int[] _d;

        // array indicating if a skip was performed (= if current character has
        // not been aligned) same arrays as for distances
        boolean[] p_sk = new boolean[ostr.length + 1];
        boolean[] sk = new boolean[ostr.length + 1];
        boolean[] _sk;

        // arrays storing the alignment boundaries
        int[][] p_al = new int[ostr.length + 1][istr.length];
        int[][] al = new int[ostr.length + 1][istr.length];
        int[][] _al;

        // initialize
        p_d[0] = 0;
        p_sk[0] = true;

        for (int j = 1; j < ostr.length + 1; j++) {
            // only possibility first is to align the first letter
            // of the input string to everything
            p_al[j][0] = j;

            key.setString1(istr[0]);
            key.setString2(ostr[j - 1]);
            p_d[j] = p_d[j - 1] + getAlignmentCost(key);
            p_sk[j] = false;
        }

        // constant penalty for not aligning a character
        int skConst = skipcost;

        // align
        // can start at 1, since 0 has been treated in initialization
        for (int i = 1; i < istr.length; i++) {

            // zero'st row stands for skipping from the beginning on
            d[0] = p_d[0] + skConst;
            sk[0] = true;

            for (int j = 1; j < ostr.length + 1; j++) {

                // translation cost between symbols ( j-1, because 0 row
                // inserted for not aligning at beginning)
                key.setString1(istr[i]);
                key.setString2(ostr[j - 1]);
                int tr_cost = getAlignmentCost(key);

                // skipping cost greater zero if not yet aligned
                int sk_cost = p_sk[j] ? skConst : 0;

                if (sk_cost + p_d[j] < tr_cost + d[j - 1]) {
                    // skipping cheaper

                    // cost is cost from previous input char + skipping
                    d[j] = sk_cost + p_d[j];
                    // alignment is from prev. input + delimiter
                    al[j] = p_al[j];
                    al[j][i] = j;
                    // yes, we skipped
                    sk[j] = true;

                } else {
                    // aligning cheaper

                    // cost is that from previously aligned output + distance
                    d[j] = tr_cost + d[j - 1];
                    // alignment continues from previously aligned
                    System.arraycopy(al[j - 1], 0, al[j], 0, i);// copy of...
                    al[j][i] = j;

                    // nope, didn't skip
                    sk[j] = false;
                }
            }
            // swapping
            _d = p_d;
            p_d = d;
            d = _d;

            _sk = p_sk;
            p_sk = sk;
            sk = _sk;

            _al = p_al;
            p_al = al;
            al = _al;
        }

        return p_al[ostr.length];

    }

    private int getAlignmentCost(StringPair key) {

        Integer cost = aligncost.get(key);

        if (null == cost) {
            if (inIsOut)
                return (key.getString1().equals(key.getString2())) ? 0 : defaultcost;
            else
                return defaultcost;
        }

        return cost;
    }

}
