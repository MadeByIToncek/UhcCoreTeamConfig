package space.itoncek.uctc.cfg;

import java.util.HashMap;
import java.util.Map;

public class Lang {
    Map<Translation, String> translationMap = new HashMap<>();

    public Lang() {

    }

    public String getTranslation(Translation translation) {
        if (translationMap.get(translation) == null) return translation.getDefault();
        return translationMap.get(translation);
    }
}
