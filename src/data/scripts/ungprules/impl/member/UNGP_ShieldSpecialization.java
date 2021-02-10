package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

public class UNGP_ShieldSpecialization extends UNGP_MemberBuffRuleEffect {
    private float commonBonus; // percent
    private float upkeepBonus; // percent

    @Override
    public void updateDifficultyCache(int difficulty) {
        commonBonus = getValueByDifficulty(0, difficulty);
        upkeepBonus = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(5f, 10f, difficulty);
        if (index == 1) return getLinearValue(10f, 20f, difficulty);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(commonBonus);
        if (index == 1) return getPercentString(upkeepBonus);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
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
