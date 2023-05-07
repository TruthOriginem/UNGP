package ungp.impl.rules.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerFleetTag;
import ungp.scripts.utils.UNGP_BaseBuff;

import java.util.List;

public class UNGP_Endurance extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {
    private float duration;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        duration = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(12f, -4f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "50%";
        if (index == 1) return getFactorString(getValueByDifficulty(0, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : members) {
            if (member.isMothballed()) continue;
            if (member.getBuffManager().getBuff(buffID) != null) continue;
            RepairTrackerAPI repairTracker = member.getRepairTracker();
            if (repairTracker.getCR() < repairTracker.getMaxCR() * 0.5f) {
                float crPer = member.getStats().getCRPerDeploymentPercent().computeEffective(member.getVariant().getHullSpec().getCRToDeploy()) / 100f;
                crPer = Math.min(crPer, repairTracker.getMaxCR() * 0.5f);
                repairTracker.applyCREvent(crPer, buffID, rule.getName());
                member.getBuffManager().addBuffOnlyUpdateStat(new UNGP_BaseBuff(buffID, duration));
            }
        }
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {

    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        boolean show = false;
        for (FleetMemberAPI member : members) {
            UNGP_BaseBuff buff = (UNGP_BaseBuff) member.getBuffManager().getBuff(buffID);
            if (buff != null) {
                imageTooltip.addPara(rule.getExtra1(), 0f, Misc.getHighlightColor(), member.getShipName(), (int) buff.getDur() + "");
                show = true;
            }
        }
        return show;
    }
}
