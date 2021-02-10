package data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.ungprules.UNGP_RuleEffectAPI;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import data.scripts.utils.SimpleI18n.I18nSection;
import data.scripts.utils.UNGPUtils;

import java.util.ArrayList;
import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.COMBAT_RULES_IN_THIS_GAME;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public class UNGP_SpecialistCombatPlugin extends BaseEveryFrameCombatPlugin {
    private static final I18nSection i18n = new I18nSection("UNGP", "hm", true);

    private CombatEngineAPI engine;
    private boolean init = false;
    private boolean isHardMode = false;

    private List<UNGP_CombatTag> tags = new ArrayList<>();
    private List<String> hdStrings = new ArrayList<>();

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        if (engine.isInCampaign() || engine.isInCampaignSim()) {
            //处理
            UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
            if (inGameData != null && inGameData.isHardMode()) {
                isHardMode = true;
                int difficultyLevel = inGameData.getDifficultyLevel();
                for (URule rule : COMBAT_RULES_IN_THIS_GAME) {
                    UNGP_RuleEffectAPI ruleEffect = rule.getRuleEffect();
                    if (ruleEffect instanceof UNGP_CombatTag) {
                        tags.add((UNGP_CombatTag) ruleEffect);
                        hdStrings.add(rule.getDesc(difficultyLevel));
                    }
                }
            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!init) {
            init = true;
            if (isHardMode) {
                for (String string : hdStrings) {
                    engine.getCombatUI().addMessage(0, string);
                }
                engine.getCombatUI().addMessage(0, i18n.get("start"));
                hdStrings.clear();
            }
        }

        if (!isHardMode) return;
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
