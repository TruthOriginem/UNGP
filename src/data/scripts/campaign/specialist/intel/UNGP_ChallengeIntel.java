package data.scripts.campaign.specialist.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.UNGP_Settings;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.intel.UNGP_ChallengeIntel.UNGP_ChallengeProgress.ProgressState;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import ungp.ui.UIRect;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.IntelSortTier.TIER_0;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.rules_i18n;

public class UNGP_ChallengeIntel extends BaseIntelPlugin {
    public static final class UNGP_ChallengeProgress {
        public enum ProgressState {
            RUNNING,
            CANCELLED,
            COMPLETED
        }

        private String challenge_id;
        private int elapsedMonth = 0;
        private ProgressState state = ProgressState.RUNNING;

        public UNGP_ChallengeProgress(String challenge_id) {
            this.challenge_id = challenge_id;
        }
    }

    private List<UNGP_ChallengeProgress> underGoingChallenge;
    private int monthChecker;
    private int dayChecker;

    public UNGP_ChallengeIntel(List<UNGP_ChallengeProgress> progresses) {
        Global.getSector().addScript(this);
        monthChecker = Global.getSector().getClock().getMonth();
        dayChecker = Global.getSector().getClock().getDay();
        this.underGoingChallenge = progresses;
    }

    @Override
    protected void notifyEnded() {
        super.notifyEnded();
        Global.getSector().removeScript(this);
    }

    /**
     * 更新挑战
     * Update the challenge progress
     *
     * @param inGameData
     */
    public void updateChallengeProgress(UNGP_InGameData inGameData) {
        List<String> activeRuleIds = new ArrayList<>();
        int positiveRuleAmount = 0;
        List<URule> activatedRules = inGameData.getActivatedRules();
        for (URule rule : activatedRules) {
            activeRuleIds.add(rule.getId());
            if (rule.isBonus()) {
                positiveRuleAmount++;
            }
        }
        boolean shouldLockRepick = false;
        for (UNGP_ChallengeProgress progress : underGoingChallenge) {
            // 只对正在进行中的挑战进行更新
            if (progress.state != ProgressState.RUNNING) continue;
            UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(progress.challenge_id);

            // 如果可以重选，才考虑问题
            if (challengeInfo.canReselectRules()) {
                // 如果需要被取消
                if (challengeInfo.isAbovePositiveLimitation(positiveRuleAmount) || !challengeInfo.isRulesContainRequired(activeRuleIds)) {
                    progress.state = ProgressState.CANCELLED;
                }
            }
            // 如果被取消了，发送更新信息
            if (progress.state == ProgressState.CANCELLED) {
                sendUpdateIfPlayerHasIntel(progress, false);
                continue;
            }

            // 检测是否完成
            if (challengeInfo.getDurationByMonth() <= progress.elapsedMonth) {
                if (challengeInfo.isNeedMaxLevel()) {
                    if (UNGP_Settings.reachMaxLevel()) {
                        progress.state = ProgressState.COMPLETED;
                    }
                } else {
                    progress.state = ProgressState.COMPLETED;
                }
            }
            // 如果成功挑战
            if (progress.state == ProgressState.COMPLETED) {
                inGameData.completeChallenge(progress.challenge_id);
                sendUpdateIfPlayerHasIntel(progress, false);
                continue;
            }

            if (!challengeInfo.canReselectRules())
                shouldLockRepick = true;
        }
        UNGP_ChallengeManager.setRepickLock(shouldLockRepick);
    }

    @Override
    protected void advanceImpl(float amount) {
        CampaignClockAPI clock = Global.getSector().getClock();
        final int curMonth = clock.getMonth();
        final int curDay = clock.getDay();
        // 每月提高一次
        if (curMonth != monthChecker) {
            monthChecker = curMonth;
            for (UNGP_ChallengeProgress progress : underGoingChallenge) {
                if (progress.state == ProgressState.RUNNING) {
                    progress.elapsedMonth++;
                }
            }
        }
        // 每天检查一次挑战是否完成
        if (curDay != dayChecker) {
            dayChecker = curDay;
            UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
            updateChallengeProgress(inGameData);
        }
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        UNGP_SpecialistBackgroundUI.resumeTicking();
        CustomPanelAPI customPanel = panel.createCustomPanel(width, height, new UNGP_SpecialistBackgroundUI());
        panel.addComponent(customPanel);
        UIRect fullScreenRect = new UIRect(0, 0, width, height).shrink(40f);
        TooltipMakerAPI tooltip =
                fullScreenRect.beginTooltip(panel, true);

        List<UNGP_ChallengeProgress> runningProgress = new ArrayList<>();
        List<UNGP_ChallengeProgress> cancelledProgress = new ArrayList<>();
        List<UNGP_ChallengeProgress> completedProgress = new ArrayList<>();
        for (UNGP_ChallengeProgress progress : underGoingChallenge) {
            switch (progress.state) {
                case RUNNING:
                    runningProgress.add(progress);
                    break;
                case CANCELLED:
                    cancelledProgress.add(progress);
                    break;
                case COMPLETED:
                    completedProgress.add(progress);
                    break;
            }
        }
        Color grayColor = Misc.getGrayColor();
        if (!runningProgress.isEmpty()) {
            tooltip.setParaOrbitronLarge();
            tooltip.addPara(rules_i18n.get("challenge_intel_running"), Misc.getPositiveHighlightColor(), 0f);
            tooltip.setParaFontDefault();
            for (UNGP_ChallengeProgress progress : runningProgress) {
                UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(progress.challenge_id);
                challengeInfo.createTooltip(tooltip, 10f, progress.elapsedMonth);
            }
        }
        if (!cancelledProgress.isEmpty()) {
            tooltip.addSpacer(20f);
            tooltip.setParaOrbitronLarge();
            tooltip.addPara(rules_i18n.get("challenge_intel_cancelled"), Misc.getNegativeHighlightColor(), 0f);
            tooltip.setParaFontDefault();
            for (UNGP_ChallengeProgress progress : cancelledProgress) {
                UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(progress.challenge_id);
                URule unlockRule = challengeInfo.getMilestoneToUnlock();
                if (unlockRule != null) {
                    TooltipMakerAPI imageTooltip = tooltip.beginImageWithText(unlockRule.getSpritePath(), 64f);
                    imageTooltip.addPara(challengeInfo.getName(), grayColor, 0);
                    tooltip.addImageWithText(10f);
                }
            }
        }
        if (!completedProgress.isEmpty()) {
            tooltip.addSpacer(20f);
            tooltip.setParaOrbitronLarge();
            tooltip.addPara(rules_i18n.get("challenge_intel_completed"), Misc.getHighlightColor(), 0f);
            tooltip.setParaFontDefault();
            for (UNGP_ChallengeProgress progress : completedProgress) {
                UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(progress.challenge_id);
                URule unlockRule = challengeInfo.getMilestoneToUnlock();
                if (unlockRule != null) {
                    TooltipMakerAPI imageTooltip = tooltip.beginImageWithText(unlockRule.getSpritePath(), 64f);
                    imageTooltip.addPara(challengeInfo.getName(), Misc.getHighlightColor(), 0);
                    tooltip.addImageWithText(10f);
                }
            }
        }
        fullScreenRect.addTooltip();
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color c = getTitleColor(mode);
        float pad = 3f;
        float opad = 10f;

        if (listInfoParam instanceof UNGP_ChallengeProgress) {
            UNGP_ChallengeProgress progress = (UNGP_ChallengeProgress) listInfoParam;
            UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(progress.challenge_id);
            Color nameColor = c;
            String desc = "";
            switch (progress.state) {
                case RUNNING:
                    nameColor = UNGP_RulesManager.getMilestoneColor();
                    desc = rules_i18n.get("challenge_intel_running");
                    break;
                case CANCELLED:
                    nameColor = Misc.getNegativeHighlightColor();
                    desc = rules_i18n.get("challenge_intel_cancelled");
                    break;
                case COMPLETED:
                    nameColor = h;
                    desc = rules_i18n.get("challenge_intel_completed");
                    break;
            }
            info.addPara(challengeInfo.getName(), nameColor, 0f);
            bullet(info);
            info.addPara(desc, g, pad);
            unindent(info);
        } else {
            info.addPara(getName(), Misc.getNegativeHighlightColor(), 0f);
            bullet(info);
            info.addPara(getDesc(), g, pad);
            unindent(info);
        }

    }

    public String getDesc() {
        return rules_i18n.get("challenge_intel_desc");
    }

    @Override
    public String getName() {
        return rules_i18n.get("challenge_intel_name");
    }

    @Override
    public String getIcon() {
        if (listInfoParam instanceof UNGP_ChallengeProgress) {
            UNGP_ChallengeProgress progress = (UNGP_ChallengeProgress) listInfoParam;
            UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(progress.challenge_id);
            URule rule = challengeInfo.getMilestoneToUnlock();
            if (rule != null) {
                return rule.getSpritePath();
            }
        }
        return Global.getSettings().getSpriteName("icons", "UNGP_hmlogo");
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("ungp");
        return tags;
    }

    @Override
    public IntelSortTier getSortTier() {
        return TIER_0;
    }
}
