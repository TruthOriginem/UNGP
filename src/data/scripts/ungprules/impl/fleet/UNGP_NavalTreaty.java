package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;
import data.scripts.utils.UNGP_BaseBuff;

import java.util.ArrayList;
import java.util.List;

public class UNGP_NavalTreaty extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {
    private static final int THRESHOLD_DESIGN_TYPE_AMOUNT = 2;
    private static final float MAX_CR_REDUCTION_PER_TYPE = 0.15f;

    private class CRDeBuff extends UNGP_BaseBuff {

        private int extraType;

        public CRDeBuff(String id, int extraType) {
            super(id);
            this.extraType = extraType;
        }

        @Override
        public void apply(FleetMemberAPI member) {
            decreaseMaxCR(member.getStats(), id, extraType * MAX_CR_REDUCTION_PER_TYPE, rule.getName());
//            member.getStats().getMaxCombatReadiness().modifyFlat(id, -extraType * MAX_CR_REDUCTION_PER_TYPE, rule.getName());
        }

        public void setExtraType(int extraType) {
            this.extraType = extraType;
        }
    }

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }


    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        List<String> designTypes = new ArrayList<>();
        final List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : members) {
            String designType = member.getHullSpec().getManufacturer();
            if (member.isMothballed()) continue;
            if (!designTypes.contains(designType)) {
                designTypes.add(designType);
            }
        }
        int typeAmount = designTypes.size();
        if (typeAmount > THRESHOLD_DESIGN_TYPE_AMOUNT) {
            typeAmount -= THRESHOLD_DESIGN_TYPE_AMOUNT;
            String buffId = buffID;
            boolean needsSync = false;
            for (FleetMemberAPI member : members) {
                BuffManagerAPI.Buff test = member.getBuffManager().getBuff(buffId);
                if (test instanceof CRDeBuff) {
                    CRDeBuff buff = (CRDeBuff) test;
                    buff.refresh();
                    buff.setExtraType(typeAmount);
                } else {
                    member.getBuffManager().addBuff(new CRDeBuff(buffId, typeAmount));
                    needsSync = true;
                }
            }
            if (needsSync) {
                fleet.forceSync();
            }
        }
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return THRESHOLD_DESIGN_TYPE_AMOUNT + "";
        if (index == 1) return (int) (MAX_CR_REDUCTION_PER_TYPE * 100f) + "%";
        return super.getDescriptionParams(index, difficulty);
    }
}