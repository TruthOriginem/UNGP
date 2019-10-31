package data.scripts.utils.langCoreTest;

import com.fs.starfarer.api.Global;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainLanguageModule {
    private static final String DEFAULT = "default";
    private static String language = DEFAULT;
    private static JSONObject languageJson;

    public static void Load() {
        try {
            String langCode = Global.getSettings().getString("switchLang");
            if (langCode.isEmpty() || langCode.equals(DEFAULT)) {
                language = DEFAULT;
                return;
            }
            languageJson = Global.getSettings().getMergedJSONForMod("data/strings/" + langCode + ".json", "ungp");
            language = langCode;
        } catch (IOException | JSONException e) {
            language = DEFAULT;
        }
    }

    public static String getString(String category, String id) {
        if (language.equals(DEFAULT)) {
            return Global.getSettings().getString(category, id);
        } else {
            JSONObject categoryContent = languageJson.optJSONObject(category);
            if (categoryContent != null) {
                return categoryContent.optString(id, Global.getSettings().getString(category, id));
            }
        }
        return Global.getSettings().getString(category, id);
    }
}
