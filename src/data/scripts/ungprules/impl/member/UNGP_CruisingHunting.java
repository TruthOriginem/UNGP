package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatInitTag;
import data.scripts.ungprules.tags.UNGP_PlayerShipSkillTag;

public class UNGP_CruisingHunting extends UNGP_BaseRuleEffect implements UNGP_CombatInitTag, UNGP_PlayerShipSkillTag {
    private static final int DP_REDUCTION_FLAT = 2;
    private float dpReductionFactor;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        dpReductionFactor = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.08f, 0.04f);
        return super.getValueByDifficulty(index, difficulty);
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return DP_REDUCTION_FLAT + "";
        return super.getDescriptionParams(index, difficulty);
    }


    @Override
    public void init(CombatEngineAPI engine) {
        // do nothing, just for tag
    }

    @Override
    public void apply(FleetDataAPI fleetData, FleetMemberAPI member, MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        if (hullSize == ShipAPI.HullSize.CRUISER) {
            // How vanilla works
            float dp = stats.getSuppliesToRecover().getBaseValue();
            int reduction = (int) Math.ceil(Math.max(DP_REDUCTION_FLAT, dp * dpReductionFactor));
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(buffID, -reduction);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(buffID);
    }
}
