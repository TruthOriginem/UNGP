package data.scripts.campaign;

import com.fs.starfarer.api.Global;

public class UNGP_InGameData {
    private static final String KEY = "UNGP_igd";
    int curCycle = 1;
    boolean isRecorded = false;//是否用这个记录了重生点
    boolean inherited = false;//是否已经继承了上个重生点
    boolean passedInheritTime = false;
    public boolean isHardMode = false;
    public boolean shouldDeleteRecordNextSave = false;

    public UNGP_InGameData() {
        Global.getSector().getPersistentData().put(KEY, this);
    }

    public static UNGP_InGameData getInstance() {
        return (UNGP_InGameData) Global.getSector().getPersistentData().get(KEY);
    }

    /***
     * 是否能被继承
     * @return 满级返回true
     */
    public boolean couldBeRecorded() {
        if (!isRecorded) {
            if (Global.getSettings().getBoolean("noLevelLimit")) {
                return true;
            }
            return reachMaxLevel();
        }
        return false;
    }

    public boolean reachMaxLevel() {
        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        int maxLevel = Global.getSettings().getLevelupPlugin().getMaxLevel();
        return playerLevel >= maxLevel;
    }

    public float getDamageBuffFactor() {
        return 1f + curCycle * 0.1f;
    }

    public float getDamageTakenBuffFactor() {
        float factor = Math.max(0.5f, 1f - curCycle * 0.05f);
        return factor;
    }

}
