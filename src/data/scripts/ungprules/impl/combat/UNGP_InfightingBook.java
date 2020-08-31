package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.ungprules.UNGP_BaseRuleEffect;
import data.scripts.utils.UNGPUtils;

import java.util.EnumSet;

public class UNGP_InfightingBook extends UNGP_BaseRuleEffect {
    private float multiplier;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.13f + 0.02f * (float) Math.pow(difficulty, 0.598);
        return 1f;
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        if (UNGPUtils.isPlayerShip(ship)) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.isDecorative()) continue;
                if (weapon.isBeam()) continue;
                if (weapon.isBurstBeam()) continue;
                if (weapon.getRange() < 550) {
                    weapon.getDamage().setDamage(weapon.getDerivedStats().getDamagePerShot() * (1f + multiplier));
                }
            }
        }
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(multiplier * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }

    @Override
    public EnumSet<GameState> getEffectiveState() {
        return EnumSet.of(GameState.COMBAT);
    }
}
