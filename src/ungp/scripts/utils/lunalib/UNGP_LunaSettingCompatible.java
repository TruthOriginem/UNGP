package ungp.scripts.utils.lunalib;

import lunalib.lunaSettings.LunaSettings;
import ungp.scripts.utils.Constants;

public class UNGP_LunaSettingCompatible {
    private static String modId = Constants.MOD_ID;

    public static void init() {
        LunaSettings.addSettingsListener(new UNGP_SettingListener());
    }

    public static boolean getBoolean(String fieldId) {
        return Boolean.TRUE.equals(LunaSettings.getBoolean(modId, fieldId));
    }
}
