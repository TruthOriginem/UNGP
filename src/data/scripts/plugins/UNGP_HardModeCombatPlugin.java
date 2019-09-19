package data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.campaign.UNGP_InGameData;

import java.util.List;

public class UNGP_HardModeCombatPlugin extends BaseEveryFrameCombatPlugin {
    private static final String KEY = "ungp_hmc";

    private CombatEngineAPI engine;
    private UNGP_InGameData inGameData;
    private boolean init = false;
    private float damageFactor;
    private float damageTakenFactor;

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        this.inGameData = UNGP_InGameData.getInstance();
        damageFactor = inGameData.getDamageBuffFactor();
        damageTakenFactor = inGameData.getDamageTakenBuffFactor();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!init) {
            init = true;

        }

        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive()) continue;
            if (ship.isAlly() || ship.getOwner() == 0) continue;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getArmorDamageTakenMult().modifyMult(KEY, damageTakenFactor);
            stats.getEmpDamageTakenMult().modifyMult(KEY, damageTakenFactor);
            stats.getHullDamageTakenMult().modifyMult(KEY, damageTakenFactor);
            stats.getShieldDamageTakenMult().modifyMult(KEY, damageTakenFactor);

            stats.getEnergyWeaponDamageMult().modifyMult(KEY, damageFactor);
            stats.getBallisticWeaponDamageMult().modifyMult(KEY, damageFactor);
            stats.getMissileWeaponDamageMult().modifyMult(KEY, damageFactor);
        }
    }
}
