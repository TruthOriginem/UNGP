package ungp.scripts.campaign.specialist.rules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import ungp.api.rules.UNGP_RuleEffectAPI;
import ungp.api.rules.tags.*;
import ungp.scripts.UNGP_modPlugin;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import ungp.scripts.campaign.specialist.economy.UNGP_EconomyListener;
import ungp.scripts.campaign.specialist.items.UNGP_RuleItem;
import ungp.scripts.campaign.specialist.rules.UNGP_RuleInfoLoader.UNGP_RuleInfo;
import ungp.scripts.utils.UNGPUtils;
import ungp.scripts.utils.UNGP_Feedback;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.*;

import static ungp.scripts.utils.Constants.rules_i18n;

public class UNGP_RulesManager {
    private static final Color POSITIVE_COLOR = new Color(50, 255, 50);
    private static final Color NEGATIVE_COLOR = new Color(255, 50, 50);
    private static final Color GOLDEN_COLOR = new Color(255, 215, 0);
    private static final Color MILESTONE_COLOR = new Color(158, 41, 255);
    private static final Logger LOGGER = Global.getLogger(UNGP_RulesManager.class);
    // 全规则
    private static List<URule> ALL_RULES = new ArrayList<>();

    /**
     * Suggest using {@link UNGP_InGameData#getActivatedRules()} to get real active rules.
     */
    public static List<URule> ACTIVATED_RULES_IN_THIS_GAME = new ArrayList<>();

    public static List<URule> COMBAT_RULES_IN_THIS_GAME = new ArrayList<>();
    public static List<URule> CAMPAIGN_RULES_IN_THIS_GAME = new ArrayList<>();
    public static List<UNGP_EconomyTag> ECONOMY_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_CampaignTag> CAMPAIGN_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_PlayerFleetTag> PLAYER_FLEET_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_PlayerFleetMemberTag> PLAYER_FLEET_MEMBER_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_PlayerShipSkillTag> PLAYER_SHIP_SKILL_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_PlayerCharacterStatsSkillTag> PLAYER_CHARACTER_SKILL_TAGS_ITG = new ArrayList<>();
    private static boolean needUpdateCache = false;
    // 2 static values that avoids calling inGameData occasionally
    private static boolean isSpecialistMode = false;
    private static Difficulty globalDifficulty = null;

    /**
     * Called on {@link UNGP_modPlugin}
     */
    public static void initOrReloadRules() {
        loadAllRules(UNGP_RuleInfoLoader.loadAllRuleInformation());
    }


    /**
     * Should be called after {@link UNGP_ChallengeManager#initOrReloadChallengeInfos()} to tag all the rules which provides the challenge
     */
    public static void tagAllChallengeProviders() {
        final List<UNGP_ChallengeInfo> challengesCopy = UNGP_ChallengeManager.getChallengeInfosCopy();
        for (UNGP_ChallengeInfo challengeInfo : challengesCopy) {
            for (URule rule : challengeInfo.getRulesRequired()) {
                rule.isMilestoneProvider = true;
            }
        }
    }

    /**
     * Used when you need to update rules in rule plugin.
     */
    public static void updateCacheNextFrame() {
        needUpdateCache = true;
    }

    public static boolean isCacheNeedUpdate() {
        return needUpdateCache;
    }

    /**
     * ***主要入口***
     * 刷新Rule当前缓存，游戏载入时需要调用
     * 1.通过当前游戏启用的规则，刷新 启用状态 和 内部数值
     * 2.刷新玩家状态
     */
    public static void updateRulesCache() {
        UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
        if (inGameData != null) {
            LOGGER.info("Start updating rule caches...");
            //清理已生效的Rules
            ACTIVATED_RULES_IN_THIS_GAME.clear();
            COMBAT_RULES_IN_THIS_GAME.clear();
            CAMPAIGN_RULES_IN_THIS_GAME.clear();
            CAMPAIGN_TAGS_ITG.clear();
            ECONOMY_TAGS_ITG.clear();
            PLAYER_FLEET_TAGS_ITG.clear();
            PLAYER_FLEET_MEMBER_TAGS_ITG.clear();
            PLAYER_SHIP_SKILL_TAGS_ITG.clear();
            PLAYER_CHARACTER_SKILL_TAGS_ITG.clear();
            // 已生效的规则，在最后会重新保存
            List<URule> activatedRules = inGameData.getActivatedRules();
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
            //unapply All stats
            for (URule rule : ALL_RULES) {
                // 首先锁住成就规则的随机pick
                rule.isMilestoneRollLocked = true;
                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                effect.unapplyGlobalStats();

                // 清理listener
                if (effect instanceof UNGP_CampaignListenerTag) {
                    Class<?> listenerClass = ((UNGP_CampaignListenerTag<?>) effect).getClassOfListener();
                    Global.getSector().getListenerManager().removeListenerOfClass(listenerClass);
                }

                // 清理舰队rule
                if (effect instanceof UNGP_PlayerFleetTag) {
                    if (playerFleet != null)
                        ((UNGP_PlayerFleetTag) effect).unapplyPlayerFleetStats(playerFleet);
                }
                // 清理经济rule
                if (effect instanceof UNGP_EconomyTag) {
                    UNGP_EconomyListener.unapplyMarkets((UNGP_EconomyTag) effect);
                }
            }
            // 生效前的规则，只生效一次，不会被记录
            List<UNGP_TweakBeforeApplyTag> beforeApplyTags = new ArrayList<>();
            for (URule rule : activatedRules) {
                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                if (effect instanceof UNGP_TweakBeforeApplyTag) {
                    beforeApplyTags.add((UNGP_TweakBeforeApplyTag) effect);
                }
            }
            // 不可更改的原记录内容
            List<URule> originalActivatedRules = Collections.unmodifiableList(new ArrayList<>(activatedRules));
            for (UNGP_TweakBeforeApplyTag tag : beforeApplyTags) {
                tag.tweakBeforeApply(activatedRules, originalActivatedRules);
            }
            // 根据当前完成挑战，让那些成就规则可被roll
            updateRollableMilestoneRules(inGameData);
            // Clean the rule list
            Set<URule> activatedRulesSet = new HashSet<>(activatedRules);
            activatedRules.clear();
            activatedRules.addAll(activatedRulesSet);
            //apply stats
            Difficulty difficulty = inGameData.getDifficulty();
            for (URule rule : activatedRules) {
                ACTIVATED_RULES_IN_THIS_GAME.add(rule);

                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                effect.updateDifficultyCache(difficulty);
                effect.applyGlobalStats();

                if (effect instanceof UNGP_CampaignListenerTag) {
                    Global.getSector().getListenerManager().addListener(((UNGP_CampaignListenerTag<?>) effect).getListener(),
                                                                        true);
                }

                //如果不是战斗效果，那就是生涯效果
                if (effect instanceof UNGP_CombatTag || effect instanceof UNGP_CombatInitTag) {
                    COMBAT_RULES_IN_THIS_GAME.add(rule);
                } else {
                    CAMPAIGN_RULES_IN_THIS_GAME.add(rule);
                }

                if (effect instanceof UNGP_PlayerFleetTag) {
                    if (playerFleet != null) {
                        ((UNGP_PlayerFleetTag) effect).applyPlayerFleetStats(playerFleet);
                        PLAYER_FLEET_TAGS_ITG.add((UNGP_PlayerFleetTag) effect);
                    }
                }
                if (effect instanceof UNGP_PlayerFleetMemberTag) {
                    PLAYER_FLEET_MEMBER_TAGS_ITG.add((UNGP_PlayerFleetMemberTag) effect);
                }
                if (effect instanceof UNGP_EconomyTag) {
                    ECONOMY_TAGS_ITG.add((UNGP_EconomyTag) effect);
                }
                if (effect instanceof UNGP_CampaignTag) {
                    CAMPAIGN_TAGS_ITG.add((UNGP_CampaignTag) effect);
                }
                if (effect instanceof UNGP_PlayerShipSkillTag) {
                    PLAYER_SHIP_SKILL_TAGS_ITG.add((UNGP_PlayerShipSkillTag) effect);
                }
                if (effect instanceof UNGP_PlayerCharacterStatsSkillTag) {
                    PLAYER_CHARACTER_SKILL_TAGS_ITG.add((UNGP_PlayerCharacterStatsSkillTag) effect);
                }
            }
            if (!ECONOMY_TAGS_ITG.isEmpty()) {
                UNGP_EconomyListener.addListener();
                UNGP_EconomyListener.applyMarkets();
            }
            setStaticDifficulty(difficulty);
            setStaticSpecialistMode(inGameData.isHardMode());
            inGameData.saveActivatedRules(activatedRules);
            UNGP_ChallengeManager.updateChallengeProgress(inGameData);
            updateSpecialistSkills(playerStats);
            if (playerFleet != null)
                playerFleet.forceSync();
            LOGGER.info("Rule caches update completed.");
        }
        needUpdateCache = false;
    }

    /**
     * Add/Remove the skill if didn't pick related rules.
     *
     * @param stats
     */
    private static void updateSpecialistSkills(MutableCharacterStatsAPI stats) {
        if (stats == null) return;
        if (PLAYER_SHIP_SKILL_TAGS_ITG.isEmpty()) {
            stats.setSkillLevel("UNGP_specialist_player_ship_skill", 0);
        } else {
            stats.setSkillLevel("UNGP_specialist_player_ship_skill", 1);
        }
        if (PLAYER_CHARACTER_SKILL_TAGS_ITG.isEmpty()) {
            stats.setSkillLevel("UNGP_specialist_player_character_skill", 0);
        } else {
            stats.setSkillLevel("UNGP_specialist_player_character_skill", 1);
        }
        stats.refreshCharacterStatsEffects();
    }

    public static Difficulty getGlobalDifficulty() {
        return globalDifficulty;
    }

    public static boolean isSpecialistMode() {
        return isSpecialistMode;
    }

    public static void setStaticSpecialistMode(boolean isSpecialistMode) {
        UNGP_RulesManager.isSpecialistMode = isSpecialistMode;
    }

    public static List<URule> getAllRulesCopy() {
        return new ArrayList<>(ALL_RULES);
    }

    /**
     * 根据当前inGameData完成的挑战来使成就规则可被roll到
     *
     * @return
     */
    public static void updateRollableMilestoneRules(UNGP_InGameData dataInSave) {
        for (String completedChallenge : dataInSave.getCompletedChallenges()) {
            UNGP_ChallengeInfo info = UNGP_ChallengeManager.getChallengeInfo(completedChallenge);
            if (info != null) {
                URule rule = info.getMilestoneToUnlock();
                if (rule != null)
                    rule.isMilestoneRollLocked = false;
            }
        }
    }

    /**
     * 规则的封装类，每个规则应该有且仅有一个对象
     * The sealed class of single rule, each rule should only have one object of this class
     */
    public static final class URule {
        public void setPickedTimes(int pickedTimes) {
            this.pickedTimes = pickedTimes;
        }

        public int getPickedTimes() {
            return pickedTimes;
        }

        public enum Tags {
            NO_ROLL("no_roll"),
            MILESTONE("milestone");
            private final String id;

            Tags(String id) {
                this.id = id;
            }
        }

        private String buffID;
        private UNGP_RuleInfo ruleInfo;
        private boolean isMilestoneRollLocked = true;
        /**
         * If this rule is one of the conditions of some challenges
         */
        private boolean isMilestoneProvider = false;
        /**
         * Would be initialized only when the rule is loaded
         */
        private boolean isDescAffectedByLevel;
        /**
         * Only used for deep sync feedback.
         */
        private int pickedTimes = -1;

        URule(UNGP_RuleInfo info) {
            this.buffID = "ungp_" + info.getId();
            this.ruleInfo = info;
            this.ruleInfo.getEffectPlugin().setRule(this);
            this.isDescAffectedByLevel = isDescAffectedByLevel();
        }

        public String getId() {
            return ruleInfo.getId();
        }

        /**
         * Should just use buffID in Rule plugin.
         *
         * @return
         */
        @Deprecated
        public String getBuffID() {
            return buffID;
        }

        public String getName() {
            return ruleInfo.getName();
        }

        public String getSpritePath() {
            return ruleInfo.getSpritePath();
        }

        public boolean isDefaultSource() {
            return UNGPUtils.isEmpty(ruleInfo.getSource());
        }

        @Deprecated
        public boolean isBonus() {
            return ruleInfo.isBonus();
        }

        public boolean isPositive() {
            return ruleInfo.isBonus();
        }

        public boolean isGolden() {
            return ruleInfo.isGolden();
        }


        /**
         * 一般来说是规则名颜色
         * The color of rules' name in default.
         *
         * @return
         */
        public Color getCorrectColor() {
            if (isMilestone()) {
                if (isGolden()) return getGoldenColor();
                return getMilestoneColor();
            }
            if (isGolden()) {
                return getGoldenColor();
            } else {
                return UNGP_RulesManager.getPNColor(isBonus());
            }
        }

        public Color getCostColor() {
            return getCost() >= 0 ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
        }

        public String getRuleTypeName() {
            return UNGP_RulesManager.getRuleTypeName(isMilestone(), isGolden(), isBonus());
        }

        public String getRuleTypeCharacter() {
            if (isGolden()) return "G";
            if (isMilestone()) return "M";
            return isBonus() ? "P" : "N";
        }

        public void addPreDesc(TooltipMakerAPI tooltip, float pad) {
            TooltipMakerAPI image = tooltip.beginImageWithText(getRuleIconSpriteName(isMilestone(), isGolden(), isBonus()), 16f);
            image.addPara(getRuleTypeName(), getCorrectColor(), 0f);
            tooltip.addImageWithText(pad * 0.5f);
            if (!isDefaultSource()) {
                tooltip.setParaFontVictor14();
                tooltip.addPara(rules_i18n.get("rule_source") + ruleInfo.getSource(), Misc.getGrayColor(), pad * 0.5f);
                tooltip.setParaFontDefault();
            }
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad, String prefix, Difficulty difficulty) {
            String[] values;
            if (difficulty == null) {
                values = getDescriptionParams(Difficulty.GAMMA);
            } else {
                values = getDescriptionParams(difficulty);
            }
            Color highlightColor = isBonus() ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
            tooltip.addPara(prefix + ruleInfo.getDesc(), pad, highlightColor, values);
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad, String prefix) {
            addDesc(tooltip, pad, prefix, getGlobalDifficulty());
        }

        public void addDescToItem(TooltipMakerAPI tooltip, float pad, String prefix, boolean isExpanded) {
            if (!isDescAffectedByLevel) {
                tooltip.addPara(rules_i18n.get("front_desc"), pad * 0.5f, Misc.getBasePlayerColor(), Misc.getHighlightColor(), rules_i18n.get("any"));
                addDesc(tooltip, pad, prefix, null);
            } else {
                Difficulty difficulty = UNGP_RulesManager.getGlobalDifficulty();
                if (isExpanded) {
                    for (Difficulty itemDifficulty : Difficulty.values()) {
                        tooltip.addPara(rules_i18n.get("front_desc"), pad * 0.5f,
                                        difficulty == itemDifficulty ? Misc.getGrayColor() : Misc.getBasePlayerColor(),
                                        itemDifficulty.color, itemDifficulty.name);
                        addDesc(tooltip, pad, prefix, itemDifficulty);
                        tooltip.addSpacer(pad * 0.5f);
                    }
                } else {
                    tooltip.addPara(rules_i18n.get("front_desc"), pad * 0.5f, Misc.getBasePlayerColor(), difficulty.color, difficulty.name);
                    addDesc(tooltip, pad, prefix, difficulty);
                }
            }
        }

        public String[] getDescriptionParams(Difficulty difficulty) {
            String[] values = new String[10];
            for (int i = 0; i < 10; i++) {
                values[i] = getRuleEffect().getDescriptionParams(i, difficulty);
            }
            return values;
        }

        /**
         * @return true if the difficulty level description won't change at any level, by checking description params
         */
        public boolean isDescAffectedByLevel() {
            String lastCompared = null;
            for (Difficulty difficulty : Difficulty.values()) {
                String[] values = getDescriptionParams(difficulty);
                StringBuilder sb = new StringBuilder();
                for (String value : values) {
                    sb.append(value);
                }
                if (lastCompared == null) {
                    lastCompared = sb.toString();
                } else {
                    String compared = sb.toString();
                    if (!lastCompared.contentEquals(compared)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * For roll tag
         *
         * @param tooltip
         * @param pad
         * @param prefix
         */
        public void addRollDesc(TooltipMakerAPI tooltip, float pad, String prefix) {
            if (!isBasicallyRollable()) {
                tooltip.addPara(prefix + rules_i18n.get("not_rollable"), Misc.getGrayColor(), pad);
            }
        }

        public void addChallengeRelatedDesc(TooltipMakerAPI tooltip, float pad, String detailPrefix, boolean showMore) {
            Color grayColor = Misc.getGrayColor();
            // when the rule is milestone rule
            if (isMilestone()) {
                final List<UNGP_ChallengeInfo> challengesCopy = UNGP_ChallengeManager.getChallengeInfosCopy();
                List<UNGP_ChallengeInfo> provider = new ArrayList<>();
                for (UNGP_ChallengeInfo challengeInfo : challengesCopy) {
                    if (challengeInfo.getMilestoneToUnlock().equals(this)) {
                        provider.add(challengeInfo);
                    }
                }
                if (!provider.isEmpty()) {
                    tooltip.addPara(rules_i18n.get("milestone_tip"), Misc.getBasePlayerColor(), pad);
                    tooltip.addSpacer(10f);
                    tooltip.setBulletedListMode(detailPrefix);
                    for (UNGP_ChallengeInfo challengeInfo : provider) {
                        String sb = challengeInfo.getName() +
                                " : " +
                                challengeInfo.getConnectedRuleNames();
                        tooltip.addPara(sb, grayColor, 5f);
                    }
                }
            } else {
                final List<UNGP_ChallengeInfo> challengesCopy = UNGP_ChallengeManager.getChallengeInfosCopy();
                List<UNGP_ChallengeInfo> provider = new ArrayList<>();
                for (UNGP_ChallengeInfo challengeInfo : challengesCopy) {
                    if (challengeInfo.getRulesRequired().contains(this)) {
                        provider.add(challengeInfo);
                    }
                }
                if (!provider.isEmpty()) {
                    boolean hasUncompletedChallenges = false;
                    boolean difficultyEnough = UNGP_ChallengeManager.isDifficultyEnough(getGlobalDifficulty());
                    // 专家挑战
                    tooltip.addPara(rules_i18n.get("challenge_tip"), Misc.getBasePlayerColor(), pad);
                    tooltip.addSpacer(10f);
                    for (UNGP_ChallengeInfo challengeInfo : provider) {
                        tooltip.setBulletedListMode(detailPrefix);
                        boolean isChallengeCompleted = !UNGP_ChallengeManager.getTemporaryCompletedChallenges().isEmpty() && UNGP_ChallengeManager.getTemporaryCompletedChallenges().contains(challengeInfo.getId());
                        tooltip.addSpacer(5f);
                        // 完成/未完成？
                        if (isChallengeCompleted) {
                            tooltip.addPara(challengeInfo.getName() + " - " + rules_i18n.get("challenge_tip_completed"),
                                            grayColor, 0);
                        } else {
                            tooltip.addPara(challengeInfo.getName() + " - %s", 0, getMilestoneColor(), Misc.getNegativeHighlightColor(), rules_i18n.get("challenge_tip_uncompleted"));
                            hasUncompletedChallenges = true;
                        }
                        // F1 信息
                        if (showMore) {
                            tooltip.setBulletedListMode(detailPrefix + "  -");
                            challengeInfo.showChallengeDetails(tooltip, grayColor);
                        }
                    }
                    // 如果存在未完成挑战的同时，难度等级不够
                    if (hasUncompletedChallenges && !difficultyEnough) {
                        tooltip.setBulletedListMode(detailPrefix);
                        tooltip.addPara(rules_i18n.get("challenge_tip_difficultyNotMeet"), 5f, Misc.getNegativeHighlightColor(), grayColor,
                                        Difficulty.ALPHA.name, Difficulty.OMEGA.name).setHighlightColors(Difficulty.ALPHA.color, Difficulty.OMEGA.color);
                    }
                }
            }
            tooltip.setBulletedListMode(null);
        }

        public boolean isTooltipExpandable() {
            return isMilestoneProvider || isDescAffectedByLevel;
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad) {
            addDesc(tooltip, pad, "");
        }

        public void addShortDesc(TooltipMakerAPI tooltip, float pad) {
            tooltip.addPara(ruleInfo.getShortDesc(), pad);
        }


        /**
         * Would be shown at the beginning of the battle
         *
         * @param difficulty
         * @return
         */
        public Object[] generateCombatTips(Difficulty difficulty) {
            List<Object> messageList = new ArrayList<>();
            String originDesc = ruleInfo.getDesc();
            String[] unformulatedDesc = originDesc.split("%s");
            Color baseColor = getPNColor(isBonus());
            Color hlColor = isBonus() ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
            for (int i = 0; i < unformulatedDesc.length; i++) {
                String baseString = unformulatedDesc[i];
                if (baseString != null && !baseString.isEmpty()) {
                    messageList.add(baseColor);
                    messageList.add(baseString);
                }
                String hlString = getRuleEffect().getDescriptionParams(i, difficulty);
                if (hlString != null && !hlString.isEmpty()) {
                    messageList.add(hlColor);
                    messageList.add(getRuleEffect().getDescriptionParams(i, difficulty));
                }
            }
            return messageList.toArray();
        }

        public void addCostAndPickTimes(TooltipMakerAPI tooltip, float pad) {
            UIComponentAPI spacer = tooltip.addSpacer(pickedTimes > 0 ? pad - 12f : pad);
            LabelAPI para = null;
            if (pickedTimes > 0) {
                tooltip.setParaFontVictor14();
                para = tooltip.addPara(rules_i18n.get("pick_times_desc"), 0f, Misc.getGrayColor(), Misc.getStoryOptionColor(),
                                       rules_i18n.get("button_feedback"), "" + pickedTimes);
                para.setAlignment(Alignment.RMID);
                para.setOpacity(0.8f);
                para.getPosition().inBR(0f, 5f);
                tooltip.setParaFontDefault();
            }
            int cost = getCost();
            String costString;
            Color baseColor = Misc.getBasePlayerColor();
            Color highlight;
            if (cost > 0) {
                highlight = getHighlightColor(true);
                costString = "+" + cost;
            } else if (cost == 0) {
                highlight = Misc.getHighlightColor();
                costString = "" + cost;
            } else {
                highlight = getHighlightColor(false);
                costString = "" + cost;
            }
            tooltip.setParaOrbitronLarge();
            LabelAPI costLabel = tooltip.addPara(rules_i18n.get("cost_point"), 0f, baseColor, highlight, costString);
            if (para != null) {
                costLabel.getPosition().belowLeft(spacer, costLabel.getPosition().getHeight() - 8f);
            }
            tooltip.setParaFontDefault();
        }

        public int getCost() {
            return ruleInfo.getCost();
        }

        public String getCostString() {
            int cost = getCost();
            return cost > 0 ? "+" + cost : String.valueOf(cost);
        }

        public UNGP_RuleEffectAPI getRuleEffect() {
            return ruleInfo.getEffectPlugin();
        }

        public UNGP_RuleInfo getRuleInfo() {
            return ruleInfo;
        }

        public String getExtra1() {
            return ruleInfo.getExtra1();
        }

        public String getExtra2() {
            return ruleInfo.getExtra2();
        }

        public boolean hasTag(String tag) {
            return ruleInfo.getTags().contains(tag);
        }

        public boolean hasTag(Tags tag) {
            return ruleInfo.getTags().contains(tag.id);
        }


        public static URule getByID(String id) {
            for (URule rule : ALL_RULES) {
                if (rule.getId().contentEquals(id)) {
                    return rule;
                }
            }
            return null;
        }

        /**
         * 默认情况下成就规则是不能被roll到的
         *
         * @return
         */
        public boolean isRollable() {
            if (!isBasicallyRollable()) {
                return false;
            }
            if (isMilestone()) {
                return !isMilestoneRollLocked;
            }
            return true;
        }

        private boolean isBasicallyRollable() {
            return !hasTag(Tags.NO_ROLL);
        }

        public boolean isMilestone() {
            return hasTag(Tags.MILESTONE);
        }

    }

    /**
     * 获取"**规则"字符串.
     * Get the "** Rule" String.
     *
     * @return
     */
    public static String getRuleTypeName(boolean isMilestone, boolean isGolden, boolean isPositive) {
        StringBuilder sb = new StringBuilder();
        if (isMilestone) {
            if (isGolden) {
                sb.append(rules_i18n.get("prefix_golden"));
                sb.append(rules_i18n.get("middle_space"));
            }
            sb.append(rules_i18n.get("prefix_milestone"));
        } else {
            if (isGolden) {
                sb.append(rules_i18n.get("prefix_golden"));
            } else {
                if (isPositive) {
                    sb.append(rules_i18n.get("prefix_positive"));
                } else {
                    sb.append(rules_i18n.get("prefix_negative"));
                }
            }
        }
        sb.append(rules_i18n.get("middle_space"));
        sb.append(rules_i18n.get("suffix_rule"));
        return sb.toString();
    }

    /**
     * 设置可见信息，是最初始的
     */
    private static void loadAllRules(List<UNGP_RuleInfo> ruleInfos) {
        ALL_RULES.clear();
        for (UNGP_RuleInfo info : ruleInfos) {
            ALL_RULES.add(new URule(info));
        }
        loadPickTimes();
    }

    /**
     * Load from server
     */
    public static void loadPickTimes() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String url = rules_i18n.get("pick_times_collection_url");
                try {
                    URL connURL = new URL(url);
                    HttpsURLConnection conn = (HttpsURLConnection) connURL.openConnection();
                    UNGP_Feedback.setHttpProtocol(conn);
                    InputStream stream = conn.getInputStream();
                    Scanner scanner = (new Scanner(stream, "utf-8")).useDelimiter("\\A");
                    if (scanner.hasNext()) {
                        String collectionText = scanner.next();
                        try {
                            JSONObject jsonObject = new JSONObject(collectionText);
                            for (UNGP_RulesManager.URule rule : ALL_RULES) {
                                rule.setPickedTimes(jsonObject.optInt(rule.getId(), -1));
                            }
                            LOGGER.info("Successfully loaded the pick times of rules from deep sync...");
                        } catch (JSONException e) {
                            LOGGER.warn("Error loading json...");
                        }
                    }
                    stream.close();
                } catch (IOException e) {
                    LOGGER.warn("Error loading rules' pick times, but it doesn't matter...\n" + e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * PN: positive-negative
     *
     * @param isPositive
     * @return 返回 "正面/负面规则"
     */
    public static String getPNRuleString(boolean isPositive) {
        return getRuleTypeName(false, false, isPositive);
    }

    /**
     * @return "黄金规则"
     */
    public static String getGoldenRuleString() {
        return getRuleTypeName(false, true, true);
    }

    /**
     * 获得标志性小图标的路径
     *
     * @param isGolden
     * @param isPositive
     * @return
     */
    public static String getRuleIconSpriteName(boolean isMilestone, boolean isGolden, boolean isPositive) {
        StringBuilder sb = new StringBuilder("graphics/ungp/icons/");
        if (isMilestone) {
            if (isGolden) {
                sb.append("golden");
                sb.append("_");
            }
            sb.append("milestone");
        } else {
            if (isGolden) {
                sb.append("golden");
            } else {
                sb.append(isPositive ? "positive" : "negative");
            }
        }
        sb.append("_icon.png");
        return sb.toString();
    }

    public static Color getPNColor(boolean isPositive) {
        if (isPositive) {
            return POSITIVE_COLOR;
        } else {
            return NEGATIVE_COLOR;
        }
    }

    public static Color getGoldenColor() {
        return GOLDEN_COLOR;
    }

    public static Color getMilestoneColor() {
        return MILESTONE_COLOR;
    }

    public static Color getHighlightColor(boolean isPositive) {
        return isPositive ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor();
    }

    public static void setStaticDifficulty(Difficulty difficulty) {
        globalDifficulty = difficulty;
    }

    /**
     * 基于完成的挑战创建规则货舱
     * Create rules cargo based on completed challenges.
     *
     * @param completedChallenges
     * @return
     */
    public static CargoAPI createRulesCargoBasedOnChallenges(List<String> completedChallenges) {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        List<String> unlockedRuleIds = new ArrayList<>();
        // 检测完成的专家挑战
        for (String completedChallenge : completedChallenges) {
            UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(completedChallenge);
            if (challengeInfo != null) {
                String mileStoneRuleId = challengeInfo.getMilestoneToUnlock().getId();
                if (!mileStoneRuleId.isEmpty()) {
                    unlockedRuleIds.add(mileStoneRuleId);
                }
            }
        }
        List<URule> sortedRules = new ArrayList<>();
        for (URule rule : ALL_RULES) {
            // 如果是成就规则
            if (rule.isMilestone()) {
                // 如果已解锁
                if (unlockedRuleIds.contains(rule.getId())) {
                    sortedRules.add(rule);
                }
            } else {
                sortedRules.add(rule);
            }
        }

        Collections.sort(sortedRules, new UNGP_RuleSorter());
        for (URule rule : sortedRules) {
            cargo.addSpecial(new SpecialItemData(UNGP_RuleItem.getSpecialItemID(rule), rule.getId()), 1f);
        }
        return cargo;
    }

    public static List<URule> getAvailableRules(List<String> completedChallenges) {
        List<String> unlockedRuleIds = new ArrayList<>();
        // 检测完成的专家挑战
        for (String completedChallenge : completedChallenges) {
            UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(completedChallenge);
            if (challengeInfo != null) {
                String mileStoneRuleId = challengeInfo.getMilestoneToUnlock().getId();
                if (!mileStoneRuleId.isEmpty()) {
                    unlockedRuleIds.add(mileStoneRuleId);
                }
            }
        }
        List<URule> availableRules = new ArrayList<>();
        for (URule rule : ALL_RULES) {
            // 如果是成就规则
            if (rule.isMilestone()) {
                // 如果已解锁
                if (unlockedRuleIds.contains(rule.getId())) {
                    availableRules.add(rule);
                }
            } else {
                availableRules.add(rule);
            }
        }
        return availableRules;
    }

    public static class UNGP_RuleSorter implements Comparator<URule> {
        /**
         * 负面规则在前，正面在后
         * cost高的在前，低的在后
         * 根据id在排
         *
         * @param o1
         * @param o2
         * @return
         */
        @Override
        public int compare(URule o1, URule o2) {
            int compare = Boolean.compare(!o1.isBonus(), !o2.isBonus());
            if (compare == 0) {
                compare = Boolean.compare(o1.isGolden(), o2.isGolden());
                if (compare == 0) {
                    compare = Boolean.compare(!o1.isMilestone(), !o2.isMilestone());
                    if (compare == 0) {
                        compare = Integer.compare(Math.abs(o1.getCost()), Math.abs(o2.getCost()));
                        if (compare == 0)
                            compare = o1.getId().compareTo(o2.getId());
                    }
                }
            }
            compare = -compare;
            return compare;
        }
    }
}
