package ungp.scripts.campaign;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

public class UNGP_Settings {
    private static boolean NO_LEVEL_LIMIT = false;
    private static boolean NO_TIMES_LIMIT = false;
    private static int SHOW_MENU_KEY_1 = Keyboard.KEY_LCONTROL;
    private static int SHOW_MENU_KEY_2 = Keyboard.KEY_P;
    private static boolean NO_LEFT_TOP_SPECIALIST_WIDGET = false;

    public static boolean isNoLevelLimit() {
        return NO_LEVEL_LIMIT;
    }

    public static boolean isNoTimesLimit() {
        return NO_TIMES_LIMIT;
    }

    public static int getShowMenuKey1() {
        return SHOW_MENU_KEY_1;
    }

    public static int getShowMenuKey2() {
        return SHOW_MENU_KEY_2;
    }

    public static boolean isNoLeftTopSpecialistWidget() {
        return NO_LEFT_TOP_SPECIALIST_WIDGET;
    }

    public static void loadSettings() {
        try {
            JSONObject jsonObject = Global.getSettings().loadJSON("UNGP_OPTIONS.ini");
            NO_LEVEL_LIMIT = jsonObject.optBoolean("noLevelLimit", false);
            NO_TIMES_LIMIT = jsonObject.optBoolean("noTimesLimit", false);
            SHOW_MENU_KEY_1 = jsonObject.optInt("showMenuKey1", Keyboard.KEY_LCONTROL);
            SHOW_MENU_KEY_2 = jsonObject.optInt("showMenuKey2", Keyboard.KEY_P);
            NO_LEFT_TOP_SPECIALIST_WIDGET = jsonObject.optBoolean("noLeftTopSpecialistWidget", false);
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
