package data.scripts.campaign.hardmode;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.UNGP_modPlugin;
import data.scripts.campaign.UNGP_CampaignPlugin;
import data.scripts.campaign.hardmode.economy.UNGP_EconomyListener;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.hardmode.UNGP_RuleInfos.UNGP_RuleInfo;
import data.scripts.campaign.items.UNGP_RuleItem;
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

    private static List<URule> ALL_RULES = new ArrayList<>();
    public static List<URule> ACTIVATED_RULES_IN_THIS_GAME = new ArrayList<>();

    public static List<URule> COMBAT_RULES_IN_THIS_GAME = new ArrayList<>();
    public static List<URule> CAMPAIGN_RULES_IN_THIS_GAME = new ArrayList<>();
    public static List<UNGP_EconomyTag> ECONOMY_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_CampaignTag> CAMPAIGN_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_PlayerFleetTag> PLAYER_FLEET_TAGS_ITG = new ArrayList<>();
    public static List<UNGP_PlayerFleetMemberTag> PLAYER_FLEET_MEMBER_TAGS_ITG = new ArrayList<>();
    private static boolean IsSpecialistMode = false;
    private static int CurrentDifficultyLevel = 1;

    /**
     * Called on {@link UNGP_modPlugin}
     */
    public static void initOrReloadRules() {
        loadAllRules(UNGP_RuleInfos.LoadAllInfos());
    }


    /**
     * ***主要入口***
     * 刷新Rule当前缓存，游戏载入时需要调用
     * 1.通过当前游戏启用的规则，刷新 启用状态 和 内部数值
     * 2.刷新玩家状态
     */
    public static void updateRulesCache() {
        UNGP_InGameData inGameData = UNGP_CampaignPlugin.getInGameData();
        if (inGameData != null) {
            //清理已生效的Rules
            ACTIVATED_RULES_IN_THIS_GAME.clear();
            COMBAT_RULES_IN_THIS_GAME.clear();
            CAMPAIGN_RULES_IN_THIS_GAME.clear();
            CAMPAIGN_TAGS_ITG.clear();
            ECONOMY_TAGS_ITG.clear();
            PLAYER_FLEET_TAGS_ITG.clear();
            List<URule> activatedRules = inGameData.loadActivatedRules();
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
            //unapply All stats
            for (URule rule : ALL_RULES) {
                UNGP_RuleEffectAPI effect = rule.getRuleEffect();
                effect.unapplyGlobalStats();
                if (effect instanceof UNGP_PlayerFleetTag) {
                    if (playerFleet != null)
                        ((UNGP_PlayerFleetTag) effect).unapplyPlayerFleetStats(playerFleet);
                }
                if (effect instanceof UNGP_CharacterTag) {
                    if (playerStats != null)
                        ((UNGP_CharacterTag) effect).unapplyPlayerCharacterStats(playerStats);
                }
                if (effect instanceof UNGP_EconomyTag) {
                    UNGP_EconomyListener.unapplyMarkets((UNGP_EconomyTag) effect);
                }
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
            setSpecialistMode(inGameData.isHardMode);
        }
    }

    public static int getCurrentDifficultyLevel() {
        return CurrentDifficultyLevel;
    }

    public static boolean isSpecialistMode() {
        return IsSpecialistMode;
    }

    public static void setSpecialistMode(boolean isSpecialistMode) {
        IsSpecialistMode = isSpecialistMode;
    }

    public static List<URule> getAllRules() {
        return ALL_RULES;
    }


    public static class URule {
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

        public boolean isBonus() {
            return ruleInfo.isBonus();
        }

        public void addName(TooltipMakerAPI tooltip, float pad) {
            tooltip.addPara(getName(), getBorderColor(), pad);
        }

        public void addPreDesc(TooltipMakerAPI tooltip, float pad) {
            tooltip.addPara(getBonusString(isBonus()), getBorderColor(), pad * 0.5f);
            tooltip.addPara(rules_i18n.get("front_desc"), pad * 0.5f, Misc.getHighlightColor(), getCurrentDifficultyLevel() + "");
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
            addDesc(tooltip, pad, prefix, getCurrentDifficultyLevel());
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

    public static void setDifficultyLevel(int level) {
        CurrentDifficultyLevel = level;
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
            cargo.addSpecial(new SpecialItemData(UNGP_RuleItem.ID, rule.getId()), 1f);
        }
        return cargo;
    }
}
