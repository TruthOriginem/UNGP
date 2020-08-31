package data.scripts.campaign.hardmode;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.UNGP_CampaignPlugin;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.hardmode.UNGP_RuleInfos.UNGP_RuleInfo;
import data.scripts.campaign.items.UNGP_RuleItem;
import data.scripts.ungprules.UNGP_RuleEffectAPI;
import data.scripts.utils.SimpleI18n.I18nSection;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class UNGP_RulesManager {
    public static final I18nSection rules_i18n = new I18nSection("UNGP_rules", "", false);
    private static final Color BONUS_COLOR = new Color(50, 255, 50);
    private static final Color NOT_BONUS_COLOR = new Color(255, 50, 50);

    public static final List<URule> ALL_RULES = new ArrayList<>();
    public static final List<URule> ACTIVATED_RULES_IN_THIS_GAME = new ArrayList<>();
    public static int ShownDifficultyLevel = 1;
    public static boolean IsSpecialistMode = false;

    public static void initOrReloadRules() {
        loadAllRules(UNGP_RuleInfos.LoadAllInfos());
    }


    /**
     * 刷新Rule当前缓存，游戏载入时需要调用
     * 1.通过当前游戏启用的规则，刷新 启用状态 和 内部数值
     * 2.刷新玩家状态
     */
    public static void refreshRulesCache() {
        UNGP_InGameData inGameData = UNGP_CampaignPlugin.getInGameData();
        if (inGameData != null) {
            //清理已生效的Rules
            ACTIVATED_RULES_IN_THIS_GAME.clear();
            List<URule> activatedRules = inGameData.loadActivatedRules();
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
            for (URule rule : ALL_RULES) {
                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                if (playerFleet != null) effect.unapplyPlayerFleetStats(playerFleet);
                if (playerStats != null) effect.unapplyPlayerCharacterStats(playerStats);
                effect.unapplyGlobalStats();
                rule.setSelected(false);
            }
            for (URule rule : activatedRules) {
                ACTIVATED_RULES_IN_THIS_GAME.add(rule);
                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                effect.refreshDifficultyCache(inGameData.getDifficultyLevel());
                if (playerFleet != null) effect.applyPlayerFleetStats(playerFleet);
                if (playerStats != null) effect.applyPlayerCharacterStats(playerStats);
                effect.applyGlobalStats();
                rule.setSelected(true);
            }
            ShownDifficultyLevel = inGameData.getDifficultyLevel();
            IsSpecialistMode = inGameData.isHardMode;
        }
    }

    public static class URule {
        private String buffID;
        private UNGP_RuleInfo ruleInfo;
        private boolean isSelected;

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

        public boolean isBonus() {
            return ruleInfo.isBonus();
        }

        public EnumSet<GameState> getUseStates() {
            return getRuleEffect().getEffectiveState();
        }

        public void addName(TooltipMakerAPI tooltip, float pad) {
            tooltip.addPara(getName(), getBorderColor(), pad);
        }

        public void addPreDesc(TooltipMakerAPI tooltip, float pad) {
            tooltip.addPara(getBonusString(isBonus()), getBorderColor(), pad * 0.5f);
            tooltip.addPara(rules_i18n.get("front_desc"), pad * 0.5f, Misc.getHighlightColor(), ShownDifficultyLevel + "");
        }

        public Color getBorderColor() {
            return UNGP_RulesManager.getBonusColor(isBonus());
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad, String prefix, int difficulty) {
            String[] values = new String[]{
                    getRuleEffect().getDescriptionParams(0, difficulty),
                    getRuleEffect().getDescriptionParams(1, difficulty),
                    getRuleEffect().getDescriptionParams(2, difficulty),
                    getRuleEffect().getDescriptionParams(3, difficulty),
                    getRuleEffect().getDescriptionParams(4, difficulty),
                    getRuleEffect().getDescriptionParams(5, difficulty),
                    getRuleEffect().getDescriptionParams(6, difficulty),
                    getRuleEffect().getDescriptionParams(7, difficulty),
                    getRuleEffect().getDescriptionParams(8, difficulty),
                    getRuleEffect().getDescriptionParams(9, difficulty),
            };
            Color highlightColor = isBonus() ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
            tooltip.addPara(prefix + ruleInfo.getDesc(), pad, highlightColor, values);
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad, String prefix) {
            addDesc(tooltip, pad, prefix, ShownDifficultyLevel);
        }

        public void addDesc(TooltipMakerAPI tooltip, float pad) {
            addDesc(tooltip, pad, "");
        }

        public void addShortDesc(TooltipMakerAPI tooltip, float pad) {
            tooltip.addPara(ruleInfo.getShortDesc(), pad);
        }

        public String getDesc(int difficultyLevel) {
            String[] values = new String[]{
                    getRuleEffect().getDescriptionParams(0, difficultyLevel),
                    getRuleEffect().getDescriptionParams(1, difficultyLevel),
                    getRuleEffect().getDescriptionParams(2, difficultyLevel),
                    getRuleEffect().getDescriptionParams(3, difficultyLevel),
                    getRuleEffect().getDescriptionParams(4, difficultyLevel),
                    getRuleEffect().getDescriptionParams(5, difficultyLevel),
                    getRuleEffect().getDescriptionParams(6, difficultyLevel),
                    getRuleEffect().getDescriptionParams(7, difficultyLevel),
                    getRuleEffect().getDescriptionParams(8, difficultyLevel),
                    getRuleEffect().getDescriptionParams(9, difficultyLevel),
            };
            return String.format(ruleInfo.getDesc(), values);
        }

        public void addCost(TooltipMakerAPI tooltip, float pad) {
            int cost = getCost();
            if (cost > 0) {
                tooltip.addPara(rules_i18n.get("cost_point"), pad, BONUS_COLOR, cost + "");
            } else {
                tooltip.addPara(rules_i18n.get("cost_point"), pad, NOT_BONUS_COLOR, cost + "");
            }
        }

        public int getCost() {
            return ruleInfo.getCost();
        }

        public UNGP_RuleEffectAPI getRuleEffect() {
            return ruleInfo.getEffectPlugin();
        }

        public UNGP_RuleInfo getRuleInfo() {
            return ruleInfo;
        }

        public static URule getByID(String id) {
            for (URule rule : ALL_RULES) {
                if (rule.getId().equals(id)) {
                    return rule;
                }
            }
            return null;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
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

    public static String getBonusString(boolean isBonus) {
        if (isBonus) {
            return rules_i18n.get("isBonus");
        } else {
            return rules_i18n.get("notBonus");
        }
    }

    public static Color getBonusColor(boolean isBonus) {
        if (isBonus) {
            return BONUS_COLOR;
        } else {
            return NOT_BONUS_COLOR;
        }
    }

    public static void setShownDifficultyLevel(int level) {
        ShownDifficultyLevel = level;
    }

    public static CargoAPI createAllRulesCargo() {
        CargoAPI cargo = Global.getFactory().createCargo(true);
        List<URule> sortedRules = new ArrayList<>(ALL_RULES);
        Collections.sort(sortedRules, new UNGP_RuleSorter());
        for (URule rule : sortedRules) {
            cargo.addSpecial(new SpecialItemData(UNGP_RuleItem.ID, rule.getId()), 1f);
        }
        return cargo;
    }
}
