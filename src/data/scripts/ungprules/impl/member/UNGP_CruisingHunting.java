package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatInitTag;
import data.scripts.ungprules.tags.UNGP_CombatTag;

import java.util.List;

public class UNGP_CruisingHunting extends UNGP_MemberBuffRuleEffect implements UNGP_CombatTag, UNGP_CombatInitTag {
    private static final int DP_REDUCTION_FLAT = 2;
    private float dpReductionFactor;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        dpReductionFactor = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.1f, 0.05f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        float dp = member.getDeploymentPointsCost();
        int reduction = (int) Math.ceil(Math.max(DP_REDUCTION_FLAT, dp * dpReductionFactor));
        member.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(buffID, -reduction);
    }

    @Override
    public boolean canApply(FleetMemberAPI member) {
        return member.isCruiser();
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return DP_REDUCTION_FLAT + "";
        return super.getDescriptionParams(index, difficulty);
    }

    private int skipFrames = 0;

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        skipFrames++;
        if (skipFrames >= 60) {
            skipFrames = 0;
        } else {
            return;
        }
        CombatFleetManagerAPI fleetManager = engine.getFleetManager(FleetSide.PLAYER);
        List<FleetMemberAPI> members = fleetManager.getReservesCopy();
        members.addAll(fleetManager.getDeployedCopy());
        for (FleetMemberAPI member : members) {
            if (canApply(member)) {
                StatBonus mod = member.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD);
                if (!mod.getFlatBonuses().containsKey(buffID)) {
                    float dp = member.getDeploymentPointsCost();
                    int reduction = (int) Math.ceil(Math.max(DP_REDUCTION_FLAT, dp * dpReductionFactor));
                    mod.modifyFlat(buffID, -reduction);
                }
            }
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }

    @Override
    public void init(CombatEngineAPI engine) {
        skipFrames = 70;
        advanceInCombat(engine, 0);
        skipFrames = 70;
    }
}
