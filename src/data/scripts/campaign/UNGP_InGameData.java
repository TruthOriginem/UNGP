package data.scripts.campaign;

import com.fs.starfarer.api.Global;

import java.util.ArrayList;
import java.util.List;

import static data.scripts.campaign.hardmode.UNGP_RulesManager.ALL_RULES;
import static data.scripts.campaign.hardmode.UNGP_RulesManager.URule;

/**
 * 记录在游戏中
 */
public class UNGP_InGameData {
    int curCycle = 1;
    int difficultyLevel = 0;
    boolean isRecorded = false;//是否用这个记录了重生点
    boolean inherited = false;//是否已经继承了上个重生点
    boolean passedInheritTime = false;
    private List<String> activatedRuleIDs = new ArrayList<>();
    public boolean isHardMode = false;
    private int timesToChangeSpecialistMode = 0;

    /**
     * 一年一度可以获得一次专家规则更改的能力
     * @return
     */
    public int getTimesToChangeSpecialistMode() {
        return timesToChangeSpecialistMode;
    }

    public void reduceTimesToChangeSpecialistMode() {
        timesToChangeSpecialistMode--;
    }

    public void addTimesToChangeSpecialistMode() {
        timesToChangeSpecialistMode++;
    }

    public UNGP_InGameData() {
    }


    public void loadActivatedRules(List<URule> rules) {
        activatedRuleIDs.clear();
        for (URule rule : rules) {
            activatedRuleIDs.add(rule.getId());
        }
    }

    public List<URule> loadActivatedRules() {
        List<URule> results = new ArrayList<>();
        for (URule rule : ALL_RULES) {
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
        if (!isRecorded) {
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
}
