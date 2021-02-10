package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

public class UNGP_MoreLogistics extends UNGP_MemberBuffRuleEffect {
    private float bonus;

    @Override
    public void updateDifficultyCache(int difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return (int) (Math.pow(difficulty, 0.235));
        return 0;
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getDynamic().getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD).modifyFlat(rule.getBuffID(), bonus);
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(bonus);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
