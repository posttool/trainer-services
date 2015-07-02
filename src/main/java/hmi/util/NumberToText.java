package hmi.util;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberToText {

    public static void main(String[] args) {
        System.out.println(NumberToText.number("-1"));
        System.out.println(NumberToText.number("23"));
        System.out.println(NumberToText.number("3.14"));
        System.out.println(NumberToText.number("44m"));
        System.out.println(NumberToText.number("23-27"));
        System.out.println(NumberToText.number("1400s"));
    }

    static String numberWithLettersReg = "(\\d+)([a-zA-Z]+)";
    static Pattern numberWithLetters = Pattern.compile(numberWithLettersReg);

    public static String number(String word) {
        StringBuilder r = new StringBuilder();
        word = word.replace(",", "");
        if (word.matches("-?\\d+")) {
            // 7 -1 1330222
            if (word.indexOf("-") == 0) {
                word = word.substring(1);
                r.append("negative ");
            }
            r.append(NumberToText.convert(word));
        } else if (word.matches("-?\\d+\\.\\d+")) {
            // 3.1415
            if (word.indexOf("-") == 0) {
                word = word.substring(1);
                r.append("negative ");
            }
            String[] p = word.split("\\.");
            r.append(NumberToText.convert(p[0]));
            r.append(" point ");
            r.append(NumberToText.each(p[1]));
        } else if (word.matches(numberWithLettersReg)) {
            // 1990s 44mm 3in
            Matcher matcher = numberWithLetters.matcher(word);
            if (matcher.matches()) {
                String ns = matcher.group(1);
                String ls = matcher.group(2);
                String nt = NumberToText.convert(ns);
                int nn = Integer.parseInt(ns);
                String s = nn == 1 ? "" : "s";
                if (ls.equals("s")) {
                    // get last word of nt and pluralize
                    r.append(nt);
                    r.append("s");
                } else {
                    r.append(nt);
                    r.append(" ");
                    if (ls.equals("mm"))
                        r.append("milimeter" + s);
                    else if (ls.equals("m"))
                        r.append("meter" + s);
                    else if (ls.equals("in"))
                        r.append(nn == 1 ? "inch" : "inches");
                    else if (ls.equals("ft"))
                        r.append(nn == 1 ? "foot" : "feet");
                    else
                        r.append(ls);
                }
            }
        } else if (word.indexOf("-") != -1) {
            // 1990-1993
            String[] p = word.split("-");
            int converted = 0;
            for (int i = 0; i < p.length; i++) {
                String a = number(p[i]);
                if (!a.equals(p[i]))
                    converted++;
                p[i] = a;
            }
            if (converted == 2) {
                r.append(p[0]);
                r.append(" to ");
                r.append(p[1]);
            }
        } else {
            r.append(word);
        }

        return r.toString();
    }

    public static String year(String word) {
        String nword = null;
        try {
            int i = Integer.parseInt(word);
            int t = i / 100;
            int w = i - t * 100;
            if (w == 0)
                nword = NumberToText.convert(i);
            else if (t == 0)
                nword = NumberToText.convert(w);
            else
                nword = NumberToText.convert(t) + " " + NumberToText.convert(w);
        } catch (Exception e) {
        }
        if (nword == null)
            return word;
        else
            return nword;
    }

    // //

    private static final String[] tensNames = { "", " ten", " twenty", " thirty", " forty", " fifty", " sixty",
            " seventy", " eighty", " ninety" };

    private static final String[] numNames = { "", " one", " two", " three", " four", " five", " six", " seven",
            " eight", " nine", " ten", " eleven", " twelve", " thirteen", " fourteen", " fifteen", " sixteen",
            " seventeen", " eighteen", " nineteen" };

    private NumberToText() {
    }

    private static String convertLessThanOneThousand(int number) {
        String soFar;

        if (number % 100 < 20) {
            soFar = numNames[number % 100];
            number /= 100;
        } else {
            soFar = numNames[number % 10];
            number /= 10;

            soFar = tensNames[number % 10] + soFar;
            number /= 10;
        }
        if (number == 0)
            return soFar;
        return numNames[number] + " hundred" + soFar;
    }

    public static String convert(String longString) {
        try {
            return convert(Long.parseLong(longString));
        } catch (Exception e) {
            return each(longString);
        }
    }

    public static String each(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c > 47 && c < 58) {
                b.append(convert(c - 48));
                b.append(" ");
            }
        }
        return b.toString();
    }

    public static String convert(long number) {
        // 0 to 999 999 999 999
        if (number == 0) {
            return "zero";
        }

        String snumber = Long.toString(number);

        // pad with "0"
        String mask = "000000000000";
        DecimalFormat df = new DecimalFormat(mask);
        snumber = df.format(number);

        // XXXnnnnnnnnn
        int billions = Integer.parseInt(snumber.substring(0, 3));
        // nnnXXXnnnnnn
        int millions = Integer.parseInt(snumber.substring(3, 6));
        // nnnnnnXXXnnn
        int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
        // nnnnnnnnnXXX
        int thousands = Integer.parseInt(snumber.substring(9, 12));

        String tradBillions;
        switch (billions) {
        case 0:
            tradBillions = "";
            break;
        case 1:
            tradBillions = convertLessThanOneThousand(billions) + " billion ";
            break;
        default:
            tradBillions = convertLessThanOneThousand(billions) + " billion ";
        }
        String result = tradBillions;

        String tradMillions;
        switch (millions) {
        case 0:
            tradMillions = "";
            break;
        case 1:
            tradMillions = convertLessThanOneThousand(millions) + " million ";
            break;
        default:
            tradMillions = convertLessThanOneThousand(millions) + " million ";
        }
        result = result + tradMillions;

        String tradHundredThousands;
        switch (hundredThousands) {
        case 0:
            tradHundredThousands = "";
            break;
        case 1:
            tradHundredThousands = "one thousand ";
            break;
        default:
            tradHundredThousands = convertLessThanOneThousand(hundredThousands) + " thousand ";
        }
        result = result + tradHundredThousands;

        String tradThousand;
        tradThousand = convertLessThanOneThousand(thousands);
        result = result + tradThousand;

        // remove extra spaces!
        return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
    }

}
