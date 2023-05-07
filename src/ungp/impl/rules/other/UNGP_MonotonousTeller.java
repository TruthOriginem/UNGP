package ungp.impl.rules.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignTag;

public class UNGP_MonotonousTeller extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

    private int daysToLostStoryPoint;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        daysToLostStoryPoint = (int) getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return Math.round(difficulty.getLinearValue(120f, -60f));
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            Integer sinceLastLost = getDataInCampaign(0);
            if (sinceLastLost == null) {
                sinceLastLost = 0;
            }

            sinceLastLost++;
            if (sinceLastLost > daysToLostStoryPoint) {
                PersonAPI player = Global.getSector().getPlayerPerson();
                int storyPoints = player.getStats().getStoryPoints();
                if (storyPoints > 0) {
                    player.getStats().setStoryPoints(storyPoints - 1);
                    sinceLastLost = 0;

                    UNGP_SpecialistIntel.RuleMessage message = new UNGP_SpecialistIntel.RuleMessage(rule, rule.getExtra1(), "1");
                    message.send();
                }
            }
            saveDataInCampaign(0, sinceLastLost);
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        if (index == 1) return "1";
        return null;
    }
}
