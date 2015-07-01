package hmi.phone;

import hmi.data.Stress;
import hmi.data.Syllable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class Syllabifier {

    /**
     * Syllabify a string of allophones. If stress markers are provided, they
     * are preserved; otherwise, primary stress will be assigned to the initial
     * syllable.
     * <p>
     * The syllabification algorithm itself follows the <i>Core Syllabification
     * Principle (CSP)</i> from <blockquote>G.N. Clements (1990)
     * "The role of the sonority cycle in core syllabification." In: J. Kingston
     * & M.E. Beckman (Eds.),
     * <em>Papers in Laboratory Phonology I: Between the Grammar and Physics of Speech</em>
     * , Ch. 17, pp. 283-333, Cambridge University Press.</blockquote>
     */
    public static List<Object> syllabify(PhoneSet phoneSet, String phoneString) {
        // First, split phoneString into a List of allophone Strings...
        List<String> phones = phoneSet.splitIntoPhoneList(phoneString, true);
        // ...and create from it a List of generic Objects
        List<Object> phonesAndSyllables = new ArrayList<Object>(phones);

        // Create an iterator
        ListIterator<Object> iterator = phonesAndSyllables.listIterator();

        // First iteration (left-to-right):
        // CSP (a): Associate each [+syllabic] segment to a syllable node.
        Syllable currentSyllable = null;
        while (iterator.hasNext()) {
            String phone = (String) iterator.next();
            try {
                // either it's an Allophone
                PhoneEl ph = phoneSet.getPhone(phone);
                if (ph.isSyllabic()) {
                    // if /6/ immediately follows a non-diphthong vowel, it
                    // should be appended instead of forming its own syllable
                    PhoneEl lastph = (PhoneEl) currentSyllable.getLastPhone();
                    if (ph.getFeature("ctype").equals("r") && currentSyllable != null && !lastph.isDiphthong()) {
                        iterator.remove();
                        currentSyllable.addPhone(ph);
                    } else {
                        currentSyllable = new Syllable();
                        currentSyllable.addPhone(ph);
                        iterator.set(currentSyllable);
                    }
                }
            } catch (IllegalArgumentException e) {
                // or a stress or boundary marker
                if (!phoneSet.getIgnoreChars().contains(phone)) {
                    throw e;
                }
            }
        }

        // Second iteration (right-to-left):
        // CSP (b): Given P (an unsyllabified segment) preceding Q (a
        // syllabified segment), adjoin P to the syllable containing Q
        // iff P has lower sonority rank than Q (iterative).
        currentSyllable = null;
        boolean foundPrimaryStress = false;
        iterator = phonesAndSyllables.listIterator(phonesAndSyllables.size());
        while (iterator.hasPrevious()) {
            Object phoneOrSyllable = iterator.previous();
            if (phoneOrSyllable instanceof Syllable) {
                currentSyllable = (Syllable) phoneOrSyllable;
            } else if (currentSyllable == null) {
                // haven't seen a Syllable yet in this iteration
                continue;
            } else {
                String phone = (String) phoneOrSyllable;
                try {
                    // it's an Allophone -- prepend to the Syllable
                    PhoneEl allophone = phoneSet.getPhone(phone);
                    PhoneEl firstph = (PhoneEl) currentSyllable.getFirstPhone();
                    if (allophone.sonority() < firstph.sonority()) {
                        iterator.remove();
                        currentSyllable.prependPhone(allophone);
                    }
                } catch (IllegalArgumentException e) {
                    // it's a provided stress marker -- assign it to the
                    // Syllable
                    switch (phone) {
                    case "1":
                        iterator.remove();
                        currentSyllable.setStress(Stress.PRIMARY);
                        foundPrimaryStress = true;
                        break;
                    case "2":
                        iterator.remove();
                        currentSyllable.setStress(Stress.SECONDARY);
                        break;
                    case "-":
                        iterator.remove();
                        // TODO handle syllable boundaries
                        break;
                    default:
                        throw e;
                    }
                }
            }
        }

        // Third iteration (left-to-right):
        // CSP (c): Given Q (a syllabified segment) followed by R (an
        // unsyllabified segment), adjoin R to the syllable containing
        // Q iff has a lower sonority rank than Q (iterative).
        Syllable initialSyllable = currentSyllable;
        currentSyllable = null;
        iterator = phonesAndSyllables.listIterator();
        while (iterator.hasNext()) {
            Object phoneOrSyllable = iterator.next();
            if (phoneOrSyllable instanceof Syllable) {
                currentSyllable = (Syllable) phoneOrSyllable;
            } else {
                String phone = (String) phoneOrSyllable;
                try {
                    // it's an Allophone -- append to the Syllable
                    PhoneEl allophone = phoneSet.getPhone(phone);
                    if (currentSyllable == null) {
                        // haven't seen a Syllable yet in this iteration
                        iterator.remove();
                        initialSyllable.prependPhone(allophone);
                    } else {
                        // append it to the last seen Syllable
                        iterator.remove();
                        currentSyllable.addPhone(allophone);
                    }
                } catch (IllegalArgumentException e) {
                    throw e;
                }
            }
        }

        // if primary stress was not provided, assign it to initial syllable
        if (!foundPrimaryStress) {
            initialSyllable.setStress(Stress.PRIMARY);
        }

        return phonesAndSyllables;
    }
}
