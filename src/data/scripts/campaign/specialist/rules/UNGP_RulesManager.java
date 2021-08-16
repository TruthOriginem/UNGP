package data.scripts.campaign.specialist.rules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.UNGP_modPlugin;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.specialist.economy.UNGP_EconomyListener;
import data.scripts.campaign.specialist.items.UNGP_RuleItem;
import data.scripts.campaign.specialist.rules.UNGP_RuleInfoLoader.UNGP_RuleInfo;
import data.scripts.ungprules.UNGP_RuleEffectAPI;
import data.scripts.ungprules.tags.*;
import data.scripts.utils.SimpleI18n.I18nSection;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UNGP_RulesManager {
    public static final I18nSection rules_i18n = new I18nSection("UNGP_rules", "", false);
    private static final Color BONUS_COLOR = new Color(50, 255, 50);
    private static final Color NOT_BONUS_COLOR = new Color(255, 50, 50);
    private static final Color GOLDEN_COLOR = new Color(255, 215, 0);
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
    public static boolean needUpdateCache = false;
    private static boolean isSpecialistMode = false;
    private static int globalDifficultyLevel = 1;

    /**
     * Called on {@link UNGP_modPlugin}
     */
    public static void initOrReloadRules() {
        loadAllRules(UNGP_RuleInfoLoader.LoadAllInfos());
    }

    /**
     * Used when you need to update rules in rule plugin.
     */
    public static void updateCacheNextFrame() {
        needUpdateCache = true;
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
            //清理已生效的Rules
            ACTIVATED_RULES_IN_THIS_GAME.clear();
            COMBAT_RULES_IN_THIS_GAME.clear();
            CAMPAIGN_RULES_IN_THIS_GAME.clear();
            CAMPAIGN_TAGS_ITG.clear();
            ECONOMY_TAGS_ITG.clear();
            PLAYER_FLEET_TAGS_ITG.clear();
            PLAYER_FLEET_MEMBER_TAGS_ITG.clear();
            // 已生效的规则，在最后会重新保存
            List<URule> activatedRules = inGameData.getActivatedRules();
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
            //unapply All stats
            for (URule rule : ALL_RULES) {
                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                effect.unapplyGlobalStats();
                // 清理舰队rule
                if (effect instanceof UNGP_PlayerFleetTag) {
                    if (playerFleet != null)
                        ((UNGP_PlayerFleetTag) effect).unapplyPlayerFleetStats(playerFleet);
                }
                // 清理角色rule
                if (effect instanceof UNGP_CharacterTag) {
                    if (playerStats != null)
                        ((UNGP_CharacterTag) effect).unapplyPlayerCharacterStats(playerStats);
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

            //apply stats
            for (URule rule : activatedRules) {
                ACTIVATED_RULES_IN_THIS_GAME.add(rule);

                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                effect.applyGlobalStats();
                effect.updateDifficultyCache(inGameData.getDifficultyLevel());

                //如果不是战斗效果，那就是生涯效果
                if (effect instanceof UNGP_CombatTag) {
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
                if (effect instanceof UNGP_CharacterTag) {
                    if (playerStats != null) {
                        ((UNGP_CharacterTag) effect).applyPlayerCharacterStats(playerStats);
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
            }
            if (!ECONOMY_TAGS_ITG.isEmpty()) {
                UNGP_EconomyListener.addListener();
                UNGP_EconomyListener.applyMarkets();
            }
            setDifficultyLevel(inGameData.getDifficultyLevel());
            setSpecialistMode(inGameData.isHardMode());
            inGameData.saveActivatedRules(activatedRules);
        }
    }

    public static int getGlobalDifficultyLevel() {
        return globalDifficultyLevel;
    }

    public static boolean isSpecialistMode() {
        return isSpecialistMode;
    }

    public static void setSpecialistMode(boolean isSpecialistMode) {
        UNGP_RulesManager.isSpecialistMode = isSpecialistMode;
    }

    public static List<URule> getAllRules() {
        return ALL_RULES;
    }


    /**
     * 规则的封装类
     */
    public static final class URule {
        public enum Tags {
            NO_ROLL("no_roll");
            private final String id;

            Tags(String id) {
                this.id = id;
            }
        }

        private String buffID;
        private UNGP_RuleInfo ruleInfo;

        URule(UNGP_RuleInfo info) {
            this.buffID = "ungp_" + info.getId();
            this.ruleInfo = info;
            this.ruleInfo.getEffectPlugin().setRule(this);
        }

        public String getId() {
            return ruleInfo.getId();
        }

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
            return UNGP_RuleInfoLoader.isEmpty(ruleInfo.getSource());
        }

        public boolean isBonus() {
            return ruleInfo.isBonus();
        }

        public boolean isGolden() {
            return ruleInfo.isGolden();
        }


        public Color getBorderColor() {
            return UNGP_RulesManager.getBonusColor(isBonus());
        }

        public Color getCorrectColor() {
            if (isGolden()) {
                return getGoldenColor();
            } else {
                return getBorderColor();
            }
        }

        public Color getCostColor() {
            return getCost() >= 0 ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
        }

        public String getRuleTypeName() {
            if (isGolden()) {
                return getGoldenString();
            } else {
                return getBonusString(isBonus());
            }
        }

        public String getRuleTypeCharacter() {
            return isGolden() ? "G" : (isBonus() ? "P" : "N");
        }

        public void addPreDesc(TooltipMakerAPI tooltip, float pad) {
            TooltipMakerAPI image = tooltip.beginImageWithText(getRuleIconSpriteName(isBonus(), isGolden()), 16f);
            image.addPara(getRuleTypeName(), getCorrectColor(), 0f);
            tooltip.addImageWithText(pad * 0.5f);
            if (!isDefaultSource()) {
                tooltip.setParaFontVictor14();
                tooltip.addPara(rules_i18n.get("rule_source") + ruleInfo.getSource(), Misc.getGrayColor(), pad * 0.5f);
                tooltip.setParaFontDefault();
            }
            tooltip.addPara(rules_i18n.get("front_desc"), pad * 0.5f, Misc.getBasePlayerColor(), Misc.getHighlightColor(), getGlobalDifficultyLevel() + "");
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad, String prefix, int difficulty) {
            String[] values = new String[10];
            for (int i = 0; i < 10; i++) {
                values[i] = getRuleEffect().getDescriptionParams(i, difficulty);
            }
            Color highlightColor = isBonus() ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
            tooltip.addPara(prefix + ruleInfo.getDesc(), pad, highlightColor, values);
            if (!isRollable()) {
                tooltip.addPara(prefix + rules_i18n.get("not_rollable"), Misc.getGrayColor(), 10f);
            }
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad, String prefix) {
            addDesc(tooltip, pad, prefix, getGlobalDifficultyLevel());
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad) {
            addDesc(tooltip, pad, "");
        }

        public void addShortDesc(TooltipMakerAPI tooltip, float pad) {
            tooltip.addPara(ruleInfo.getShortDesc(), pad);
        }

        public String getDesc(int difficulty) {
            String[] values = new String[10];
            for (int i = 0; i < 10; i++) {
                values[i] = getRuleEffect().getDescriptionParams(i, difficulty);
            }
            return String.format(ruleInfo.getDesc(), values);
        }

        public void addCost(TooltipMakerAPI tooltip, float pad) {
            int cost = getCost();
            tooltip.setParaOrbitronLarge();
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
            tooltip.addPara(rules_i18n.get("cost_point"), pad, baseColor, highlight, costString);
            tooltip.setParaFontDefault();
        }

        public int getCost() {
            return ruleInfo.getCost();
        }

        public String getCostString() {
            int cost = getCost();
            return cost > 0 ? "+" + cost : "" + cost;
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
                if (rule.getId().equals(id)) {
                    return rule;
                }
            }
            return null;
        }

        public boolean isRollable() {
            return !hasTag(Tags.NO_ROLL);
        }

    }

    /**
     * 设置可见信息，是最初始的
     */
    private static void loadAllRules(List<UNGP_RuleInfo> ruleInfos) {
        ALL_RULES.clear();
        for (UNGP_RuleInfo info : ruleInfos) {
            ALL_RULES.add(new URule(info));
        }
    }

    /**
     * @param isBonus
     * @return 返回 "正面/负面规则"
     */
    public static String getBonusString(boolean isBonus) {
        if (isBonus) {
            return rules_i18n.get("isBonus");
        } else {
            return rules_i18n.get("notBonus");
        }
    }

    /**
     * @return "黄金规则"
     */
    public static String getGoldenString() {
        return rules_i18n.get("golden_rule");
    }

    /**
     * 获得标志性小图标的路径
     *
     * @param isBonus
     * @param isGolden
     * @return
     */
    public static String getRuleIconSpriteName(boolean isBonus, boolean isGolden) {
        String type;
        if (isGolden) {
            type = "golden";
        } else {
            type = isBonus ? "positive" : "negative";
        }
        return "graphics/icons/" + type + "_icon.png";
    }

    public static Color getBonusColor(boolean isBonus) {
        if (isBonus) {
            return BONUS_COLOR;
        } else {
            return NOT_BONUS_COLOR;
        }
    }

    public static Color getGoldenColor() {
        return GOLDEN_COLOR;
    }


    public static Color getHighlightColor(boolean isPositive) {
        return isPositive ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor();
    }

    public static void setDifficultyLevel(int level) {
        globalDifficultyLevel = level;
    }

    /**
     * 创建包含所有规则的货舱
     *
     * @return
     */
    public static CargoAPI createAllRulesCargo() {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        List<URule> sortedRules = new ArrayList<>(ALL_RULES);
        Collections.sort(sortedRules, new UNGP_RuleSorter());
        for (URule rule : sortedRules) {
            cargo.addSpecial(new SpecialItemData(UNGP_RuleItem.getSpecialItemID(rule), rule.getId()), 1f);
        }
        return cargo;
    }
}
