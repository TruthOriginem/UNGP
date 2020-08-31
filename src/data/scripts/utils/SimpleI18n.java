package data.scripts.utils;

import com.fs.starfarer.api.Global;
import de.unkrig.commons.nullanalysis.Nullable;

public class SimpleI18n {
    public static class I18nSection {
        private String category;
        private String keyPrefix;

        public I18nSection(String category, String keyPrefix, boolean add_ToPrefix) {
            this.category = category;
            this.keyPrefix = keyPrefix + (add_ToPrefix ? "_" : "");
        }

        public String format(String keyMainBody, @Nullable Object... args) {
            if (args != null && args.length > 0) {
                return SimpleI18n.format(category, keyPrefix + keyMainBody, args);
            }
            return get(keyMainBody);
        }

        public String get(String key) {
            return Global.getSettings().getString(category, keyPrefix + key);
        }

        public String nm_format(String key, @Nullable Object... args) {
            return SimpleI18n.format(category, key, args);
        }

    }

    public static String format(String category, String id, Object... args) {
        String result;
        try {
            result = String.format(Global.getSettings().getString(category, id), args);
        } catch (Exception e) {
            return null;
        }
        return result;
    }
}
