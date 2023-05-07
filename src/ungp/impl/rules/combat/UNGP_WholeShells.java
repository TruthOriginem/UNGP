package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;
import ungp.scripts.utils.UNGPUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UNGP_WholeShells extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final String LIST_ID = "UNGP_WholeShells";
    private static final IntervalUtil checkInterval = new IntervalUtil(0.1f, 0.1f);
    private static final float DAMAGE_MULTIPLIER = 0.5f;
    private float chance = 0f;
    private int decrease_amount = 0;
    private float total_damageReduction = 0f;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        chance = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.05f, 0.05f);
        return 1f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

        checkInterval.advance(amount);
        if (checkInterval.intervalElapsed()) {
            List<DamagingProjectileAPI> recordedProjectile = getDataInEngine(engine, LIST_ID);
            if (recordedProjectile == null) {
                recordedProjectile = new ArrayList<>(50);
                decrease_amount = 0;
                total_damageReduction = 0f;
                putDataInEngine(engine, LIST_ID, recordedProjectile);
            }
            for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
                if (projectile.getOwner() != UNGPUtils.PLAYER) continue;
                if (RANDOM.nextFloat() < chance) {
                    if (recordedProjectile.contains(projectile)) continue;
                    if (projectile.getSpawnType() == ProjectileSpawnType.BALLISTIC) {
                        float baseDamage = projectile.getBaseDamageAmount();
                        projectile.setDamageAmount(baseDamage * DAMAGE_MULTIPLIER);
                        decrease_amount++;
                        total_damageReduction += baseDamage * (1 - DAMAGE_MULTIPLIER);
                    }
                }
                recordedProjectile.add(projectile);
            }
            Iterator<DamagingProjectileAPI> recordIter = recordedProjectile.iterator();
            while (recordIter.hasNext()) {
                DamagingProjectileAPI proj = recordIter.next();
                if (!engine.isEntityInPlay(proj) || proj.isExpired()) {
                    recordIter.remove();
                }
            }
        }
        ShipAPI player = engine.getPlayerShip();
        if (player != null) {
            engine.maintainStatusForPlayerShip(buffID,
                                               "graphics/icons/hullsys/ammo_feeder.png",
                                               rule.getName() + "(" + decrease_amount + ")",
                                               rule.getExtra1() + (int) (total_damageReduction),
                                               true);
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return getFactorString(DAMAGE_MULTIPLIER);
        return null;
    }
}
