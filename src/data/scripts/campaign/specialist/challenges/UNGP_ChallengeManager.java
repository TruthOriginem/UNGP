package data.scripts.campaign.specialist.challenges;

import com.fs.starfarer.api.Global;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.campaign.specialist.intel.UNGP_ChallengeIntel;
import data.scripts.campaign.specialist.intel.UNGP_ChallengeIntel.UNGP_ChallengeProgress;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UNGP_ChallengeManager {
    private static final Map<String, UNGP_ChallengeInfo> ID_TO_CHALLENGE_INFO_MAP = new HashMap<>();
    public static final String LOCK_REPICK_KEY = "UNGP_challenge_lockRepick";

    /**
     * 当存在挑战时：
     * 每次更新规则{@link UNGP_RulesManager#updateRulesCache()}的时候都需要调用，删除、更新挑战进度
     */
    public static void updateChallengeProgress(UNGP_InGameData inGameData) {
        if (Global.getSector().getIntelManager().hasIntelOfClass(UNGP_ChallengeIntel.class)) {
            UNGP_ChallengeIntel challengeIntel = (UNGP_ChallengeIntel) Global.getSector().getIntelManager().getFirstIntel(UNGP_ChallengeIntel.class);
            challengeIntel.updateChallengeProgress(inGameData);
        }
    }

    /**
     * 应该在规则被加载之后调用。
     * Should be called after rules are loaded.
     */
    public static void initOrReloadChallengeInfos() {
        ID_TO_CHALLENGE_INFO_MAP.clear();
        ID_TO_CHALLENGE_INFO_MAP.putAll(UNGP_ChallengeLoader.loadAllInfos());
    }


    /**
     * 确认并生成挑战，在更新缓存之前调用(即在随机类规则生效前调用)
     * Called After the first pick of rules. Called before the random rules take effect.
     */
    public static UNGP_ChallengeIntel confirmChallenges(UNGP_InGameData inGameData) {
        // 低于专20无法开启挑战
        if (inGameData.getDifficultyLevel() < UNGP_SpecialistSettings.getMinDifficultyLevelOfChallenge()) return null;
        Global.getSector().getPersistentData().put("UNGP_challenge_start", true);
        if (!Global.getSector().getIntelManager().hasIntelOfClass(UNGP_ChallengeIntel.class)) {
            List<UNGP_ChallengeProgress> challengeProgresses = new ArrayList<>();
            List<UNGP_ChallengeInfo> runnableChallenges = getRunnableChallenges(inGameData);
            for (UNGP_ChallengeInfo challenge : runnableChallenges) {
                UNGP_ChallengeProgress progress = new UNGP_ChallengeProgress(challenge.getId());
                challengeProgresses.add(progress);
            }
            // 判断是否存在挑战
            if (!challengeProgresses.isEmpty()) {
                UNGP_ChallengeIntel challengeIntel = new UNGP_ChallengeIntel(challengeProgresses);
                Global.getSector().getIntelManager().addIntel(challengeIntel);
                return challengeIntel;
            }
        }
        return null;
    }


    /**
     * Get challenge infos that meet the condition
     *
     * @param inGameData
     * @return
     */
    public static List<UNGP_ChallengeInfo> getRunnableChallenges(UNGP_InGameData inGameData) {
        return getRunnableChallenges(inGameData.getDifficultyLevel(), inGameData.getActivatedRules(), inGameData.getCompletedChallenges());
    }

    /**
     * Get challenge infos that meet the condition
     *
     * @return
     */
    public static List<UNGP_ChallengeInfo> getRunnableChallenges(int difficultyLevel, List<URule> rules, List<String> completedChallenges) {
        rules = new ArrayList<>(rules);
        // 低于专20无法开启挑战
        if (difficultyLevel < UNGP_SpecialistSettings.getMinDifficultyLevelOfChallenge())
            return new ArrayList<>();
        List<String> activeRuleIds = new ArrayList<>();
        int positiveRuleAmount = 0;
        for (URule rule : rules) {
            activeRuleIds.add(rule.getId());
            if (rule.isBonus()) {
                positiveRuleAmount++;
            }
        }
        List<UNGP_ChallengeInfo> challengeInfosCopy = getChallengeInfosCopy();
        List<UNGP_ChallengeInfo> runnableChallenges = new ArrayList<>();
        for (UNGP_ChallengeInfo challengeInfo : challengeInfosCopy) {
            // 已经完成过的挑战不会再继续
            if (completedChallenges.contains(challengeInfo.getId())) {
                continue;
            }
            // 高于限制
            if (challengeInfo.isAbovePositiveLimitation(positiveRuleAmount)
                    || !challengeInfo.isRulesContainRequired(activeRuleIds)) {
                continue;
            }
            runnableChallenges.add(challengeInfo);
        }
        return runnableChallenges;
    }

    /**
     * 判断挑战是否开始，如果挑战已经开始，那无法中途变更
     *
     * @return
     */
    public static boolean isChallengesStarted() {
        return Global.getSector().getPersistentData().containsKey("UNGP_challenge_start");
    }

    /**
     * 是否重选规则被锁
     *
     * @return
     */
    public static boolean isRepickLockedByChallenges() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        Boolean locked = (Boolean) persistentData.get(LOCK_REPICK_KEY);
        return locked != null && locked;
    }

    /**
     * 设置是否锁重选
     *
     * @param lock
     */
    public static void setRepickLock(boolean lock) {
        Global.getSector().getPersistentData().put(LOCK_REPICK_KEY, lock);
    }

    public static UNGP_ChallengeInfo getChallengeInfo(String id) {
        return ID_TO_CHALLENGE_INFO_MAP.get(id);
    }

    public static List<UNGP_ChallengeInfo> getChallengeInfosCopy() {
        return new ArrayList<>(ID_TO_CHALLENGE_INFO_MAP.values());
    }
}
