package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;
import data.scripts.utils.UNGP_BaseBuff;

public class UNGP_AncientThreat extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {
    private class ThreatBuff extends UNGP_BaseBuff {

        public ThreatBuff(String id, float dur) {
            super(id, dur);
        }

        @Override
        public void apply(FleetMemberAPI member) {
            decreaseMaxCR(member.getStats(), id, reduction, rule.getName());
        }
    }

    private float reduction;

    @Override
    public void updateDifficultyCache(int difficulty) {
        reduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.08f + 0.02f * (float) Math.pow(difficulty, 0.598);
        return 0;
    }


    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        if (fleet.getContainingLocation().hasTag(Tags.THEME_REMNANT)) {
            String buffId = rule.getBuffID();
            float buffDur = 0.1f;
            boolean needsSync = false;
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                BuffManagerAPI.Buff test = member.getBuffManager().getBuff(buffId);
                if (test instanceof ThreatBuff) {
                    ThreatBuff buff = (ThreatBuff) test;
                    buff.setDur(buffDur);
                } else {
                    member.getBuffManager().addBuff(new ThreatBuff(buffId, buffDur));
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
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(reduction * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
