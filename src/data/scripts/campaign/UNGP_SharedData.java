package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import data.scripts.UNGP_modPlugin;

import java.util.HashMap;
import java.util.Map;

public class UNGP_SharedData {
    private static final String SHARED_DATA_KEY = "UNGP_sharedData";
    private static UNGP_SharedData instance;

    private final Map<String, Object> ruleDataMap = new HashMap<>();

    public static void saveRuleData(String key, Object data) {
        if (instance != null) {
            instance.ruleDataMap.put(key, data);
        }
    }

    public static void clearRuleData(String key) {
        if (instance != null) {
            instance.ruleDataMap.remove(key);
        }
    }

    /**
     * Might be null.
     *
     * @param key
     * @param <T>
     * @return
     */
    public static <T> T loadRuleData(String key) {
        if (instance != null) {
            return (T) instance.ruleDataMap.get(key);
        }
        return null;
    }


    /**
     * Called on {@link UNGP_modPlugin#onGameLoad(boolean)}
     */
    public static void initialize() {
        instance = (UNGP_SharedData) Global.getSector().getPersistentData().get(SHARED_DATA_KEY);
        if (instance == null) {
            instance = new UNGP_SharedData();
            Global.getSector().getPersistentData().put(SHARED_DATA_KEY, instance);
        }
    }
}
