package ungp.impl.rules.member;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import ungp.api.rules.UNGP_MemberBuffRuleEffect;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;

public class UNGP_WorseForUse extends UNGP_MemberBuffRuleEffect {
    private static final float PENALTY_PER_D_MOD = 5f;

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return PENALTY_PER_D_MOD;
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        int numDMods = DModManager.getNumDMods(member.getVariant());
        float penaltyMultiplier = Math.max(0.01f, (100f - numDMods * PENALTY_PER_D_MOD) * 0.01f);
        MutableShipStatsAPI stats = member.getStats();
        String id = getBuffID();
        stats.getArmorBonus().modifyMult(id, penaltyMultiplier);
        stats.getHullBonus().modifyMult(id, penaltyMultiplier);
        stats.getFluxCapacity().modifyMult(id, penaltyMultiplier);
        stats.getFluxDissipation().modifyMult(id, penaltyMultiplier);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(PENALTY_PER_D_MOD);
        return super.getDescriptionParams(index, difficulty);
    }
}
