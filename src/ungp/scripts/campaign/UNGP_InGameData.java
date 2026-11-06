package ungp.scripts.campaign;

import com.fs.starfarer.api.combat.MutableStat;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.utils.UNGPUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

/**
 * 记录在游戏中,基本为存档信息
 */
public final class UNGP_InGameData {
    private int curCycle = 1;
    private Difficulty difficulty = null;
    private boolean isRecorded = false;//是否用这个记录了重生点
    private boolean inherited = false;//是否已经继承了上个重生点
    private boolean passedInheritTime = false; // 继承是否超时
    private boolean isHardMode = false;
    private float timesToChangeSpecialistMode = 0f; // 更换专家模式的次数
    private MutableStat changeTimeStat = new MutableStat(1f); // 用于标明每年可以获得多少次重启次数
    private List<String> activatedRuleIDs = new ArrayList<>();
    private List<String> completedChallenges = new ArrayList<>();
    // 是否在下一场生涯战斗中屏蔽战斗类专家规则，只生效一次
    private boolean blockCombatRulesInNextBattle = false;
    // 上一次真正写进 stats 的规则 id，卸载时据此判断需要撤回哪些规则
    private List<String> appliedRuleIDs = new ArrayList<>();
    // 上一次刷新规则缓存是否中途失败；为真时下一次刷新会退回全量卸载
    private boolean ruleCacheDirty = false;

    public UNGP_InGameData() {
    }

    /**
     * @return 获取当前存档中的数据
     */
    public static UNGP_InGameData getDataInSave() {
        return UNGP_CampaignPlugin.getInstance().getInGameData();
    }

    /**
     * @return 当前存档中的数据，不在生涯中（如主菜单的战斗任务）时返回 null
     */
    public static UNGP_InGameData getDataInSaveOrNull() {
        UNGP_CampaignPlugin plugin = UNGP_CampaignPlugin.getInstanceOrNull();
        return plugin == null ? null : plugin.getInGameData();
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
        return rulesFromIds(activatedRuleIDs);
    }

    /**
     * 上一次真正写进 stats 的规则。旧存档没有 appliedRuleIDs 字段，此时退回 activatedRuleIDs
     * —— 旧存档里两者本来就是同一批。
     *
     * @return
     */
    public List<URule> getPreviouslyAppliedRules() {
        // 旧存档的 XML 里没有这个字段,而 XStream 不走构造器与字段初始化器,读出来是 null
        List<String> ids = (appliedRuleIDs == null || appliedRuleIDs.isEmpty()) ? activatedRuleIDs : appliedRuleIDs;
        return rulesFromIds(ids);
    }

    /**
     * 规则缓存刷新成功之后调用，记录这一轮真正 apply 的规则。
     * 卸载阶段只撤这批规则，而不是把 86 条规则全扫一遍。
     *
     * @param rules
     */
    public void setAppliedRules(List<URule> rules) {
        if (appliedRuleIDs == null) {
            appliedRuleIDs = new ArrayList<>();
        }
        appliedRuleIDs.clear();
        for (URule rule : rules) {
            appliedRuleIDs.add(rule.getId());
        }
    }

    public boolean isRuleCacheDirty() {
        return ruleCacheDirty;
    }

    public void setRuleCacheDirty(boolean ruleCacheDirty) {
        this.ruleCacheDirty = ruleCacheDirty;
    }

    private List<URule> rulesFromIds(List<String> ids) {
        List<URule> results = new ArrayList<>();
        if (ids == null) {
            return results;
        }
        for (URule rule : UNGP_RulesManager.getAllRulesCopy()) {
            if (ids.contains(rule.getId())) {
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
            if (UNGP_Settings.isNoLevelLimit()) {
                return true;
            }
            return UNGP_Settings.reachMaxLevel();
        }
        return UNGP_Settings.isNoTimesLimit();
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

    /**
     * 是否屏蔽下一场生涯战斗中的战斗类专家规则。
     * 该标记保存在存档中，并且只会被下一场生涯战斗消耗一次。
     */
    public boolean isBlockCombatRulesInNextBattle() {
        return blockCombatRulesInNextBattle;
    }

    /**
     * 设置是否屏蔽下一场生涯战斗中的战斗类专家规则。
     * 一般请使用 {@link ungp.api.UNGP_CombatRulesAPI#blockInNextBattle()}。
     */
    public void setBlockCombatRulesInNextBattle(boolean blockCombatRulesInNextBattle) {
        this.blockCombatRulesInNextBattle = blockCombatRulesInNextBattle;
    }
}
