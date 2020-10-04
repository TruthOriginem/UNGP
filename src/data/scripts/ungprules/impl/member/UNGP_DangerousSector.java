package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;

public class UNGP_DangerousSector extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetMemberTag {
    private float multiplier;

    @Override
    public void updateDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    //1.5~2
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 1f + 0.5f * (float) Math.pow(difficulty, 0.23137);
        return 0;
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT).modifyMult(rule.getBuffID(), multiplier);
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(multiplier);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
