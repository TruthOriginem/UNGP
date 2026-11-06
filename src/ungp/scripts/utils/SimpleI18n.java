package ungp.scripts.utils;

import com.fs.starfarer.api.Global;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleI18n {
    private static final Set<String> REPORTED_KEYS = ConcurrentHashMap.newKeySet();

    private SimpleI18n() {
    }

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
            return SimpleI18n.get(category, keyPrefix + key);
        }

        public String nm_format(String key, @Nullable Object... args) {
            return SimpleI18n.format(category, key, args);
        }

    }

    /**
     * 读取一条本地化文本。缺少 key 时记一次错误日志并返回 key 本身，
     * 而不是把 null 交给调用方 —— 后者会在 TooltipMakerAPI.addPara 里变成
     * 指向 UI 代码的 NPE，把真正缺失的位置掩盖掉。
     */
    public static String get(String category, String id) {
        String result;
        try {
            result = Global.getSettings().getString(category, id);
        } catch (Exception e) {
            reportOnce(category + "/" + id, "Failed to read i18n entry: " + category + "/" + id, e);
            return id;
        }
        // 空串可以是合法翻译（例如中文 middle_space），只有 null 才视为缺失。
        if (result == null) {
            reportOnce(category + "/" + id, "Missing i18n entry: " + category + "/" + id, null);
            return id;
        }
        return result;
    }

    /**
     * 读取并填充占位符。模板缺失时返回 key；参数与占位符对不上时返回模板原文
     * （文本里会带着 %s），让界面至少显示得出东西，而不是抛出异常。
     */
    public static String format(String category, String id, Object... args) {
        String template = get(category, id);
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(template, args);
        } catch (Exception e) {
            reportOnce(category + "/" + id, "Failed to format i18n entry: " + category + "/" + id + ", template: " + template, e);
            return template;
        }
    }

    // 同一 key 只报一次，避免每帧刷日志
    private static void reportOnce(String key, String message, @Nullable Exception cause) {
        if (!REPORTED_KEYS.add(key)) {
            return;
        }
        if (cause == null) {
            Global.getLogger(SimpleI18n.class).error(message);
        } else {
            Global.getLogger(SimpleI18n.class).error(message, cause);
        }
    }
}
