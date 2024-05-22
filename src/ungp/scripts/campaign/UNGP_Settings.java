package ungp.scripts.campaign;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;
import ungp.scripts.utils.lunalib.UNGP_LunaSettingCompatible;

public class UNGP_Settings {
    private static boolean NO_LEVEL_LIMIT = false;
    private static boolean NO_TIMES_LIMIT = false;
    private static int SHOW_MENU_KEY_1 = Keyboard.KEY_LCONTROL;
    private static int SHOW_MENU_KEY_2 = Keyboard.KEY_P;
    private static boolean NO_LEFT_TOP_SPECIALIST_WIDGET = false;
    private static boolean NO_RULE_MESSAGE_WHILE_BATTLE_START = false;
    private static boolean LUNALIB_LOADED = false;

    public static boolean isNoLevelLimit() {
        if (LUNALIB_LOADED)
            return UNGP_LunaSettingCompatible.getBoolean("ungp_noLevelLimit");
        return NO_LEVEL_LIMIT;
    }

    public static boolean isNoTimesLimit() {
        if (LUNALIB_LOADED)
            return UNGP_LunaSettingCompatible.getBoolean("ungp_noTimesLimit");
        return NO_TIMES_LIMIT;
    }

    public static int getShowMenuKey1() {
        return SHOW_MENU_KEY_1;
    }

    public static int getShowMenuKey2() {
        return SHOW_MENU_KEY_2;
    }

    public static boolean isLeftTopSpecialistWidgetShown() {
        if (LUNALIB_LOADED)
            return !UNGP_LunaSettingCompatible.getBoolean("ungp_noSpecialistWidget");
        return !NO_LEFT_TOP_SPECIALIST_WIDGET;
    }

    public static boolean isNoRuleMessageWhileBattleStart() {
        if (LUNALIB_LOADED)
            return UNGP_LunaSettingCompatible.getBoolean("ungp_noRuleMessageWhileBattleStart");
        return NO_RULE_MESSAGE_WHILE_BATTLE_START;
    }

    public static void loadSettings() {
        LUNALIB_LOADED = Global.getSettings().getModManager().isModEnabled("lunalib");
        try {
            JSONObject settingIni = Global.getSettings().loadJSON("UNGP_OPTIONS.ini");
            NO_LEVEL_LIMIT = settingIni.optBoolean("noLevelLimit", false);
            NO_TIMES_LIMIT = settingIni.optBoolean("noTimesLimit", false);
            SHOW_MENU_KEY_1 = settingIni.optInt("showMenuKey1", Keyboard.KEY_LCONTROL);
            SHOW_MENU_KEY_2 = settingIni.optInt("showMenuKey2", Keyboard.KEY_P);
            NO_LEFT_TOP_SPECIALIST_WIDGET = settingIni.optBoolean("noLeftTopSpecialistWidget", false);
            NO_RULE_MESSAGE_WHILE_BATTLE_START = settingIni.optBoolean("noRuleMessageWhileBattleStart", false);
            if (LUNALIB_LOADED) {
                UNGP_LunaSettingCompatible.init();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading UNGP_OPTIONS...");
        }
    }


    public static boolean reachMaxLevel() {
        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        int maxLevel = Global.getSettings().getLevelupPlugin().getMaxLevel();
        return playerLevel >= maxLevel;
    }
}
