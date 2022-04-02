package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.utils.UNGPUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

/**
 * 记录在游戏中,基本为存档信息
 */
public final class UNGP_InGameData {
    private int curCycle = 1;
    private Difficulty difficulty = null;
    private boolean isRecorded = false;//是否用这个记录了重生点
    private boolean inherited = false;//是否已经继承了上个重生点
    private boolean passedInheritTime = false;
    private boolean isHardMode = false;
    private float timesToChangeSpecialistMode = 0f; // 更换专家模式的次数
    private MutableStat changeTimeStat = new MutableStat(1f); // 用于标明每年可以获得多少次重启次数
    private List<String> activatedRuleIDs = new ArrayList<>();
    private List<String> completedChallenges = new ArrayList<>();

    public UNGP_InGameData() {
    }

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


    public void completeChallenge(String id) {
        completedChallenges.add(id);
        UNGPUtils.clearDuplicatedIdsInList(completedChallenges);
    }

    /**
     * 保存现有规则，应该在之后更新缓存{@link UNGP_RulesManager}
     * Save rule ids
     *
     * @param newRules
     */
    public void saveActivatedRules(List<URule> newRules) {
        List<String> oldRules = new ArrayList<>(activatedRuleIDs);
        activatedRuleIDs.clear();
        Set<String> ruleSet = new HashSet<>();
        // Avoid duplicated
        for (URule rule : newRules) {
            String ruleId = rule.getId();
            ruleSet.add(ruleId);
            // remove the current activated rules
            oldRules.remove(ruleId);
        }
        // Clean up the old rules
        for (String ruleId : oldRules) {
            URule rule = URule.getByID(ruleId);
            if (rule != null) {
                rule.getRuleEffect().cleanUp();
            }
        }
        activatedRuleIDs.addAll(ruleSet);
    }

    /**
     * Load rule ids, modify the list will not affect the true one
     *
     * @return
     */
    public List<URule> getActivatedRules() {
        List<URule> results = new ArrayList<>();
        for (URule rule : UNGP_RulesManager.getAllRulesCopy()) {
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
    public boolean couldStartRecord() {
        if (!isRecorded()) {
            if (Global.getSettings().getBoolean("noLevelLimit")) {
                return true;
            }
            return UNGP_Settings.reachMaxLevel();
        }
        return Global.getSettings().getBoolean("noTimesLimit");
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
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

    public List<String> getCompletedChallenges() {
        return completedChallenges;
    }

    public void setCompletedChallenges(List<String> completedChallenges) {
        this.completedChallenges = new ArrayList<>(completedChallenges);
    }
}
