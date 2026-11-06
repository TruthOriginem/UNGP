package ungp.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.UNGP_Settings;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_RuleEffectAPI;
import ungp.api.rules.tags.UNGP_CombatInitTag;
import ungp.api.rules.tags.UNGP_CombatTag;
import ungp.api.rules.tags.UNGP_TweakBeforeApplyCombatTag;
import ungp.scripts.utils.Constants;
import ungp.scripts.utils.UNGPUtils;

import java.util.ArrayList;
import java.util.List;

import static ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.COMBAT_RULES_IN_THIS_GAME;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public class UNGP_SpecialistCombatPlugin extends BaseEveryFrameCombatPlugin {
    private static final Logger LOGGER = Global.getLogger(UNGP_SpecialistCombatPlugin.class);

    private CombatEngineAPI engine;
    private boolean init = false;
    private boolean isSpecialistMode = false;
    private boolean isRulesBlocked = false;

    private List<UNGP_CombatTag> tags = new ArrayList<>();
    private List<Object[]> bonusMessages = new ArrayList<>();
    private List<Object[]> notBonusMessages = new ArrayList<>();

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        if (engine.isInCampaign() || engine.isInCampaignSim()) {
            //处理
            UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
            // 公共 API 设置的“下一场战斗屏蔽战斗规则”：只在真正的生涯战斗中消耗，模拟战不消耗
            if (inGameData != null && engine.isInCampaign() && inGameData.isBlockCombatRulesInNextBattle()) {
                inGameData.setBlockCombatRulesInNextBattle(false);
                if (inGameData.isHardMode()) {
                    isRulesBlocked = true;
                    LOGGER.info("UNGP combat rules are blocked in this campaign battle by API.");
                    return;
                }
            }
            if (inGameData != null && inGameData.isHardMode()) {
                // 如果取消模拟战
                if (engine.isInCampaignSim() && !UNGP_SpecialistSettings.isRulesEnabledInSimulation()) return;
                isSpecialistMode = true;
                Difficulty difficulty = inGameData.getDifficulty();
                List<URule> originalRules = new ArrayList<>(COMBAT_RULES_IN_THIS_GAME);
                List<URule> activatedRules = new ArrayList<>(originalRules);
                for (URule rule : originalRules) {
                    if (rule.getRuleEffect() instanceof UNGP_TweakBeforeApplyCombatTag) {
                        ((UNGP_TweakBeforeApplyCombatTag) rule.getRuleEffect()).tweakBeforeApply(activatedRules, originalRules);
                    }
                }
                for (URule rule : activatedRules) {
                    UNGP_RuleEffectAPI ruleEffect = rule.getRuleEffect();
                    boolean addMessage = false;
                    if (ruleEffect instanceof UNGP_CombatTag) {
                        tags.add((UNGP_CombatTag) ruleEffect);
                        addMessage = true;
                    }
                    if (ruleEffect instanceof UNGP_CombatInitTag) {
                        ((UNGP_CombatInitTag) ruleEffect).init(engine);
                        addMessage = true;
                    }
                    if (addMessage) {
                        if (rule.isPositive()) {
                            bonusMessages.add(rule.generateCombatTips(difficulty));
                        } else {
                            notBonusMessages.add(rule.generateCombatTips(difficulty));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!init) {
            init = true;
            if (isSpecialistMode) {
                if (!UNGP_Settings.isNoRuleMessageWhileBattleStart()){
                    for (Object[] messageArray : bonusMessages) {
                        engine.getCombatUI().addMessage(0, messageArray);
                    }
                    for (Object[] messageArray : notBonusMessages) {
                        engine.getCombatUI().addMessage(0, messageArray);
                    }
                    engine.getCombatUI().addMessage(0, Misc.getHighlightColor(), Constants.root_i18n.get("specialist_mode_combat_loading"));
                }
                bonusMessages.clear();
                notBonusMessages.clear();
            } else if (isRulesBlocked && !UNGP_Settings.isNoRuleMessageWhileBattleStart()) {
                engine.getCombatUI().addMessage(0, Misc.getHighlightColor(), Constants.root_i18n.get("specialist_mode_combat_blocked"));
            }
        }

        if (!isSpecialistMode) return;
        if (engine.isPaused())
            amount = 0;
        for (UNGP_CombatTag tag : tags) {
            tag.advanceInCombat(engine, amount);
        }

        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI ship : ships) {
            if (!ship.isAlive()) continue;
            if (UNGPUtils.isPlayerShip(ship)) {
                for (UNGP_CombatTag tag : tags) {
                    tag.applyPlayerShipInCombat(amount, engine, ship);
                }
            } else {
                for (UNGP_CombatTag tag : tags) {
                    tag.applyEnemyShipInCombat(amount, ship);
                }
            }
        }
    }
}
