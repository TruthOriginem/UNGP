package data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.campaign.UNGP_CampaignPlugin;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import data.scripts.utils.SimpleI18n;
import data.scripts.utils.UNGPUtils;

import java.util.ArrayList;
import java.util.List;

import static data.scripts.campaign.hardmode.UNGP_RulesManager.COMBAT_RULES_IN_THIS_GAME;
import static data.scripts.campaign.hardmode.UNGP_RulesManager.URule;

public class UNGP_SpecialistCombatPlugin extends BaseEveryFrameCombatPlugin {
    private static final String KEY = "ungp_hmc";
    private static final SimpleI18n.I18nSection i18n = new SimpleI18n.I18nSection("UNGP", "hm", true);

    private CombatEngineAPI engine;
    private boolean init = false;
    private int difficultyLevel = 0;
    private boolean isHardMode = false;

    private List<URule> combatRules = new ArrayList<>();

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        if (engine.isInCampaign() || engine.isInCampaignSim()) {
            //处理
            UNGP_InGameData inGameData = UNGP_CampaignPlugin.getInGameData();
            if (inGameData != null && inGameData.isHardMode) {
                isHardMode = true;
                difficultyLevel = inGameData.getDifficultyLevel();
                for (URule rule : COMBAT_RULES_IN_THIS_GAME) {
                    if (rule.getRuleEffect() instanceof UNGP_CombatTag) {
                        combatRules.add(rule);
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
                for (URule rule : combatRules) {
                    engine.getCombatUI().addMessage(0, rule.getDesc(difficultyLevel));
                }
                engine.getCombatUI().addMessage(0, i18n.get("start"));
            }
        }

        if (!isHardMode) return;
        for (URule rule : combatRules) {
            ((UNGP_CombatTag) rule.getRuleEffect()).advanceInCombat(engine, amount);
        }

        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive()) continue;
            if (UNGPUtils.isPlayerShip(ship)) {
                for (URule rule : combatRules) {
                    ((UNGP_CombatTag) rule.getRuleEffect()).applyPlayerShipInCombat(amount, engine, ship);
                }
            } else {
                for (URule rule : combatRules) {
                    ((UNGP_CombatTag) rule.getRuleEffect()).applyEnemyShipInCombat(amount, ship);
                }
            }
        }
    }
}
