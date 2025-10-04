package ungp.scripts.utils.lunalib;

import lunalib.lunaSettings.LunaSettings;
import ungp.scripts.utils.Constants;

public class UNGP_LunaSettingCompatible {

    public static void init() {
        LunaSettings.addSettingsListener(new UNGP_SettingListener());
    }

    /**
     * Get boolean value from LunaSettings.
     *
     * @param fieldId The field id in LunaSettings.
     * @return The boolean value.
     */
    public static boolean getBoolean(String fieldId) {
        return Boolean.TRUE.equals(LunaSettings.getBoolean(Constants.MOD_ID, fieldId));
    }
}
