package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;

public class UNGP_Salvaging extends UNGP_BaseRuleEffect {
    private float bonus;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    //15~30%
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.05f + 0.1f * (float) Math.pow(difficulty, 0.3059);
        return 0;
    }


    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        fleet.getStats().getDynamic().getStat(Stats.SALVAGE_VALUE_MULT_FLEET_INCLUDES_RARE).modifyFlat(rule.getBuffID(), bonus, rule.getName());
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {
        fleet.getStats().getDynamic().getStat(Stats.SALVAGE_VALUE_MULT_FLEET_INCLUDES_RARE).unmodify(rule.getBuffID());
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(bonus * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
