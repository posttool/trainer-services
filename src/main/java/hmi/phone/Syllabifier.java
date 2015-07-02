package hmi.phone;

import hmi.data.Phone;
import hmi.data.Stress;
import hmi.data.Syllable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class Syllabifier {
    public static List<Syllable> syllabify(PhoneSet ps, String phoneString) {
        // Before we process, a sanity check:
        if (phoneString.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot syllabify empty phone string");
        }

        // First, split phoneString into a List of phone Strings...
        List<String> phoneStrings = ps.splitIntoPhoneList(phoneString, true);
        // ...and create from it a List of generic Objects
        List<Object> phonesAndSyllables = new ArrayList<Object>(phoneStrings);

        // Create an iterator
        ListIterator<Object> iterator = phonesAndSyllables.listIterator();

        // First iteration (left-to-right):
        // CSP (a): Associate each [+syllabic] segment to a syllable node.
        Syllable currentSyllable = null;
        while (iterator.hasNext()) {
            String phone = (String) iterator.next();
            try {
                // either it's an phone
                PhoneEl phoneel = ps.getPhone(phone);
                PhoneEl curlastph = currentSyllable == null ? null : ps.getPhone(currentSyllable.getLastPhone());
                if (phoneel.isSyllabic()) {
                    // if /6/ immediately follows a non-diphthong vowel, it
                    // should be appended instead of forming its own syllable
                    if (phoneel.getFeature("ctype").equals("r") && curlastph != null && !curlastph.isDiphthong()) {
                        iterator.remove();
                        currentSyllable.addPhone(new Phone(phone));
                    } else {
                        currentSyllable = new Syllable(new Phone(phone));
                        iterator.set(currentSyllable);
                    }
                }
            } catch (IllegalArgumentException e) {
                // or a stress or boundary marker
                if (!ps.getIgnoreChars().contains(phone)) {
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
                    // it's an phone -- prepend to the Syllable
                    PhoneEl phonel = ps.getPhone(phone);
                    PhoneEl curfirstph = currentSyllable == null ? null : ps.getPhone(currentSyllable.getFirstPhone());
                    if (phonel.sonority() < curfirstph.sonority()) {
                        iterator.remove();
                        currentSyllable.insertPhone(0, new Phone(phone));
                    }
                } catch (IllegalArgumentException e) {
                    // it's a provided stress marker -- assign it to the
                    // Syllable
                    switch (phone) {
                    case "0":
                        iterator.remove();
                        break;
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
                    // it's an phone -- append to the Syllable
                    PhoneEl phoneel = ps.getPhone(phone);
                    if (currentSyllable == null) {
                        // haven't seen a Syllable yet in this iteration
                        iterator.remove();
                        initialSyllable.insertPhone(0, new Phone(phone));
                    } else {
                        // append it to the last seen Syllable
                        iterator.remove();
                        currentSyllable.addPhone(new Phone(phone));
                    }
                } catch (IllegalArgumentException e) {
                    // throw e;
                }
            }
        }

        // if primary stress was not provided, assign it to initial syllable
        if (!foundPrimaryStress && initialSyllable != null) {
            initialSyllable.setStress(Stress.PRIMARY);
        }

        List<Syllable> syls = new ArrayList<Syllable>();
        for (Object o : phonesAndSyllables) {
            if (o instanceof Syllable) {
                syls.add((Syllable) o);
            } else {
                //System.out.println(o + " is not a Syllable");
            }
        }
        return syls;
    }
}
