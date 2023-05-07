package ungp.impl.rules.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerFleetTag;
import ungp.scripts.utils.UNGP_BaseBuff;

import java.util.ArrayList;
import java.util.List;

public class UNGP_NavalTreaty extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {
    private static final int THRESHOLD_DESIGN_TYPE_AMOUNT = 2;
    private static final float MAX_CR_REDUCTION = 0.3f;

    private class CRDeBuff extends UNGP_BaseBuff {

        private float crReduction;

        public CRDeBuff(String id, float crReduction) {
            super(id);
            this.crReduction = crReduction;
        }

        @Override
        public void apply(FleetMemberAPI member) {
            decreaseMaxCR(member.getStats(), id, crReduction, rule.getName());
        }

        public void setCrReduction(float crReduction) {
            this.crReduction = crReduction;
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
            if (member.isMothballed()) continue;
            if (isCivilian(member)) continue;
            String designType = member.getHullSpec().getManufacturer();
            if (!designTypes.contains(designType)) {
                designTypes.add(designType);
            }
        }
        float typeAmount = designTypes.size();
        if (typeAmount > THRESHOLD_DESIGN_TYPE_AMOUNT) {
            String buffId = buffID;
            boolean needsSync = false;
            float crReduction = MAX_CR_REDUCTION * (1f - THRESHOLD_DESIGN_TYPE_AMOUNT / typeAmount);
            for (FleetMemberAPI member : members) {
                CRDeBuff buff = (CRDeBuff) member.getBuffManager().getBuff(buffId);
                if (buff != null) {
                    buff.refresh();
                    buff.setCrReduction(crReduction);
                } else {
                    member.getBuffManager().addBuff(new CRDeBuff(buffId, crReduction));
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
        if (index == 1) return (int) (MAX_CR_REDUCTION * 100f) + "%";
        return super.getDescriptionParams(index, difficulty);
    }
}