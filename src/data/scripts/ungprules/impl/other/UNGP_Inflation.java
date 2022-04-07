package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

public class UNGP_Inflation extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

    private int reducePercent;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        reducePercent = (int) getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return Math.round(difficulty.getLinearValue(1.5f, 1f));
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        if (params.isOneMonthPassed()) {
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            float credits = player.getCargo().getCredits().get();
            int toReduce = (int) (credits * reducePercent * 0.01f);
            if (toReduce > 0) {
                player.getCargo().getCredits().subtract(toReduce);

                UNGP_SpecialistIntel.RuleMessage message = new UNGP_SpecialistIntel.RuleMessage(rule, rule.getExtra1(), "" + toReduce);
                message.send();
            }
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
