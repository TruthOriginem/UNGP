package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

public class UNGP_MoreLogistics extends UNGP_MemberBuffRuleEffect {
    private float bonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return (int) (difficulty.getLinearValue(1f, 1));
        return 0;
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getDynamic().getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD).modifyFlat(buffID, bonus);
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
