package hmi.data;

import org.json.simple.JSONArray;

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

    public Syllable(Phone... phones) {
        this();
        for (Phone p : phones)
            addPhone(p);
    }

    public Word getContainer() {
        return container;
    }

    public void insertPhone(int i, Phone p) {
        p.container = this;
        phones.add(i, p);
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
        b.append("      Syllable");
        if (stress != null) {
            b.append(" " + stress.text() + "");
        }
        b.append("\n");
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

    public JSONArray toJSON() {
        JSONArray a = new JSONArray();
        for (Phone p : phones) {
            a.add(p.toJSON());
        }
        return a;
    }
}
