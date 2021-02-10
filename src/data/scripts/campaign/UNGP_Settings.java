package data.scripts.campaign;

import com.fs.starfarer.api.Global;

import static data.scripts.utils.SimpleI18n.I18nSection;

public class UNGP_Settings {
    public static final I18nSection d_i18n = new I18nSection("UNGP", "d", true);

    public static boolean reachMaxLevel() {
        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        int maxLevel = Global.getSettings().getLevelupPlugin().getMaxLevel();
        return playerLevel >= maxLevel;
    }
}
