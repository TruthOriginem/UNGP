package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

import java.awt.*;

import static data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel.RuleMessage;

public class UNGP_WorkHard extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    private static final String MEM_CHECK_WORK = "$UNGP_WorkHard_Warning";
    private static final float STRIKE_CHANCE_PER_DAY = 0.08f;
    private static final float CR_LOSS = 0.75f;
    private static final float CREW_LOSS = 0.50f;
    private float ratio;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        ratio = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.15f, 0.15f);
        return 0;
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            float crew = fleet.getCargo().getCrew();
            crew = crew < 1 ? 1f : crew;
            float marine = fleet.getCargo().getMarines();
            float neededMarine = (float) Math.ceil(crew * ratio - marine);
            if (neededMarine > 0) {
                if (!fleet.getMemoryWithoutUpdate().contains(MEM_CHECK_WORK)) {
                    fleet.getMemoryWithoutUpdate().set(MEM_CHECK_WORK, true, 3f);
                    String neededMarineString = (int) neededMarine + "";
                    Color negative = Misc.getNegativeHighlightColor();
                    Color highlight = Misc.getHighlightColor();
                    Global.getSector().getCampaignUI().addMessage(String.format(rule.getExtra2(), neededMarineString),
                                                                  negative, neededMarineString, "", highlight, highlight);
                }

                if (Math.random() < STRIKE_CHANCE_PER_DAY) {
                    WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<>();
                    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                        if (!member.isMothballed()) {
                            picker.add(member, member.getRepairTracker().getCR());
                        }
                    }
                    FleetMemberAPI notWorkHard = picker.pickAndRemove();
                    int deadCrew = notWorkHard.getCrewComposition().getCrewInt();
                    while (deadCrew < 1) {
                        if (picker.isEmpty()) {
                            return;
                        }
                        notWorkHard = picker.pickAndRemove();
                        deadCrew = notWorkHard.getCrewComposition().getCrewInt();
                    }
                    deadCrew = Math.max(1, (int) (deadCrew * CREW_LOSS));
                    // Avoid zero crew
                    if (crew - deadCrew <= 0) {
                        return;
                    }
                    notWorkHard.getRepairTracker().applyCREvent(-CR_LOSS, rule.getName());
                    fleet.getCargo().removeCrew(deadCrew);
                    RuleMessage message = new RuleMessage(rule, rule.getExtra1(), notWorkHard.getShipName(), deadCrew + "");
                    message.send();
                }
            }
        }
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return (int) (STRIKE_CHANCE_PER_DAY * 100f) + "%";
        if (index == 2) return (int) (CR_LOSS * 100f) + "%";
        if (index == 3) return (int) (CREW_LOSS * 100f) + "%";
        return null;
    }
}
