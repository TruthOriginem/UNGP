package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.Description;

import java.util.ArrayList;
import java.util.List;

public class UNGP_LoadingChecker {
    private static final String ATTENTION_INFO = "Attention: Any unauthorized modifications to the mods will be considered as a blatant violation of the mod author's rights.";

    public static void checkLoad() {
        boolean shouldThrow = check1();
        if (shouldThrow) {
            throw new RuntimeException("Not compatible with Total Version Mod: Fairy Empire.\n" + ATTENTION_INFO);
        }
    }

    public static boolean check1() {
        if (Global.getSettings().getModManager().isModEnabled("fairyempire")) {
            return true;
        }
        Description description = Global.getSettings().getDescription("fairyempire", Description.Type.FACTION);
        if (description.hasText1() || description.hasText2() || description.hasText3()) {
            return true;
        }
        description = Global.getSettings().getDescription("N127S", Description.Type.CUSTOM);
        if (description.hasText1() || description.hasText2() || description.hasText3()) {
            return true;
        }
        List<String> classNames = new ArrayList<>();
        classNames.add("data.scripts.FairyEmpireModPlugin");
        classNames.add("data.scripts.world.FairyEmpireGen");
        ClassLoader classLoader = Global.getSettings().getScriptClassLoader();
        for (String className : classNames) {
            if (isClassFound(className, classLoader)) {
                return true;
            }
        }
        List<String> hulls = new ArrayList<>();
        hulls.add("FYL_cruiser");
        hulls.add("FYL_heavycruiser");
        hulls.add("HGN_battlecruiser");
        hulls.add("FYL_battleship09A");
        hulls.add("FYL_supershipMK6");
        hulls.add("VGR_supershipMK3");
        for (String hullID : hulls) {
            try {
                if (Global.getSettings().getHullSpec(hullID) != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean isClassFound(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
