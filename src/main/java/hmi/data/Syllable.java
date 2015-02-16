package hmi.data;

import java.util.ArrayList;
import java.util.List;

public class Syllable implements Container, IsContained {
    Word container;
    List<Phone> phones;
    String text;
    String accent;
    String ph;
    Stress stress;

    public Syllable() {
        phones = new ArrayList<Phone>();
    }

    public Word getContainer() {
        return container;
    }

    public void addPhone(Phone p) {
        p.container = this;
        phones.add(p);
    }

    public void prependPhone(Phone p) {
        p.container = this;
        phones.add(0, p);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("      Syllable [" + text + "]\n");
        for (Phone phone : phones) {
            b.append(phone.toString());
        }
        return b.toString();
    }

    public void setStress(Stress stress) {
        this.stress = stress;
    }

    public Phone getLastPhone() {
        if (phones.isEmpty())
            return null;
        else
            return phones.get(phones.size() - 1);
    }

    public Phone getFirstPhone() {
        if (phones.isEmpty())
            return null;
        else
            return phones.get(0);
    }
}
