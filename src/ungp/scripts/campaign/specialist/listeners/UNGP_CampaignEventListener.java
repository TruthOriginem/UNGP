package ungp.scripts.campaign.specialist.listeners;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistManager;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeProgress;

import java.util.HashSet;

public class UNGP_CampaignEventListener extends BaseCampaignEventListener {
    public UNGP_CampaignEventListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        // 只有在专家模式下才会触发
        if (!UNGP_SpecialistManager.isSpecialistMode()) return;
        EngagementResultForFleetAPI anotherSideResult;
        if (result.didPlayerWin()) {
            anotherSideResult = result.getLoserResult();
        } else {
            anotherSideResult = result.getWinnerResult();
        }
        HashSet<FleetMemberAPI> damagedMembers = new HashSet<>();
        damagedMembers.addAll(anotherSideResult.getDestroyed());
        damagedMembers.addAll(anotherSideResult.getDisabled());
        float damagedDp = 0;
        for (FleetMemberAPI damagedMember : damagedMembers) {
            float dp = damagedMember.getUnmodifiedDeploymentPointsCost();
            damagedDp += Math.min(50f, dp);
        }
        // 自动记录击败舰船总计DP
        for (UNGP_ChallengeProgress progress : UNGP_ChallengeManager.getUndergoingChallenges()) {
            if (progress.getState() == UNGP_ChallengeProgress.ProgressState.RUNNING) progress.addDefeatedDp(damagedDp);
        }
    }
}
