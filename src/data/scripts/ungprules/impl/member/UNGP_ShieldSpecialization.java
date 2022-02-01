package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

public class UNGP_ShieldSpecialization extends UNGP_MemberBuffRuleEffect {
    private float commonBonus; // percent
    private float upkeepBonus; // percent

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        commonBonus = getValueByDifficulty(0, difficulty);
        upkeepBonus = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(5f, 5f);
        if (index == 1) return difficulty.getLinearValue(10f, 10f);
        return 0;
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0 || index == 1) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        final MutableShipStatsAPI stats = member.getStats();
        final String id = getBuffID();
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - commonBonus * 0.01f);
        stats.getShieldTurnRateMult().modifyPercent(id, commonBonus);
        stats.getShieldUnfoldRateMult().modifyPercent(id, commonBonus);
        stats.getShieldUpkeepMult().modifyMult(id, 1f - upkeepBonus * 0.01f);
    }
}
