package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;

import java.util.ArrayList;
import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

/**
 * 记录在游戏中,基本为存档信息,不建议随时调用
 */
public final class UNGP_InGameData {
    private int curCycle = 1;
    private int difficultyLevel = 0;
    private boolean isRecorded = false;//是否用这个记录了重生点
    private boolean inherited = false;//是否已经继承了上个重生点
    private boolean passedInheritTime = false;
    private boolean isHardMode = false;
    private float timesToChangeSpecialistMode = 0f;
    private MutableStat changeTimeStat = new MutableStat(1f); // 用于标明每年可以获得多少次重启次数
    private List<String> activatedRuleIDs = new ArrayList<>();


    /**
     * @return 获取当前存档中的数据
     */
    public static UNGP_InGameData getDataInSave() {
        return UNGP_CampaignPlugin.getInstance().getInGameData();
    }

    /**
     * 一年一度可以获得一次专家规则更改的能力，
     *
     * @return
     */
    public int getTimesToChangeSpecialistMode() {
        return (int) timesToChangeSpecialistMode;
    }


    public void reduceTimesToChangeSpecialistMode() {
        timesToChangeSpecialistMode -= 1f;
    }

    /**
     * 每年加一次
     */
    public void addTimesToChangeSpecialistMode() {
        float timeToAdd = changeTimeStat.getModifiedValue();
        timesToChangeSpecialistMode += timeToAdd;
    }

    public UNGP_InGameData() {
    }

    /**
     * Save rule ids
     *
     * @param rules
     */
    public void saveActivatedRules(List<URule> rules) {
        activatedRuleIDs.clear();
        for (URule rule : rules) {
            activatedRuleIDs.add(rule.getId());
        }
    }

    /**
     * Load rule ids
     *
     * @return
     */
    public List<URule> getActivatedRules() {
        List<URule> results = new ArrayList<>();
        for (URule rule : UNGP_RulesManager.getAllRules()) {
            if (activatedRuleIDs.contains(rule.getId())) {
                results.add(rule);
            }
        }
        return results;
    }

    /***
     * 是否能被继承
     * @return 满级返回true
     */
    public boolean couldBeRecorded() {
        if (!isRecorded()) {
            if (Global.getSettings().getBoolean("noLevelLimit")) {
                return true;
            }
            return UNGP_Settings.reachMaxLevel();
        }
        return Global.getSettings().getBoolean("noTimesLimit");
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public int getCurCycle() {
        return curCycle;
    }

    public boolean isHardMode() {
        return isHardMode;
    }

    public void setHardMode(boolean hardMode) {
        isHardMode = hardMode;
    }

    public boolean isRecorded() {
        return isRecorded;
    }

    public void setRecorded(boolean recorded) {
        isRecorded = recorded;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public boolean isPassedInheritTime() {
        return passedInheritTime;
    }

    public void setPassedInheritTime(boolean passedInheritTime) {
        this.passedInheritTime = passedInheritTime;
    }

    public void setCurCycle(int curCycle) {
        this.curCycle = curCycle;
    }

    public MutableStat getChangeTimeStat() {
        return changeTimeStat;
    }
}
