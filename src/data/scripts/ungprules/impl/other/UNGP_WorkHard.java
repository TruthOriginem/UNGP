package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.UNGP_CampaignPlugin.TempCampaignParams;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;

import java.awt.*;

import static data.scripts.campaign.intel.UNGP_SpecialistIntel.RuleMessage;

public class UNGP_WorkHard extends UNGP_BaseRuleEffect {
    private static final String MEM_CHECK_WORK = "$UNGP_WorkHard";
    private static final float STRIKE_CHANCE_PER_DAY = 0.05f;
    private static final float CR_LOSS = 0.50f;
    private float ratio;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        ratio = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.16f + 0.04f * (float) Math.pow(difficulty, 0.5981);
        return 0;
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            float crew = fleet.getCargo().getCrew();
            crew = crew < 1 ? 1f : crew;
            float marine = fleet.getCargo().getMarines();
            float neededMarine = crew * ratio - marine;
            if (neededMarine > 0) {
                if (Math.random() < STRIKE_CHANCE_PER_DAY) {
                    WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<>();
                    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                        if (!member.isMothballed()) {
                            picker.add(member, member.getRepairTracker().getCR());
                        }
                    }
                    FleetMemberAPI notWorkHard = picker.pick();
                    notWorkHard.getRepairTracker().applyCREvent(-CR_LOSS, rule.getName());
                    RuleMessage message = new RuleMessage(rule, rule.getRuleInfo().getExtra1(), notWorkHard.getShipName());
                    message.send();
                }
                if (!fleet.getMemoryWithoutUpdate().contains(MEM_CHECK_WORK)) {
                    fleet.getMemoryWithoutUpdate().set(MEM_CHECK_WORK, true, 3f);
                    String neededMarineString = (int) neededMarine + "";
                    Color negative = Misc.getNegativeHighlightColor();
                    Color highlight = Misc.getHighlightColor();
                    Global.getSector().getCampaignUI().addMessage(String.format(rule.getRuleInfo().getExtra2(), neededMarineString),
                            negative, neededMarineString, "", highlight, highlight);
                }

            }
        }
    }


    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(ratio * 100f);
        if (index == 1) return (int) (STRIKE_CHANCE_PER_DAY * 100f) + "%";
        if (index == 2) return (int) (CR_LOSS * 100f) + "%";
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return getDescriptionParams(index);
    }
}
