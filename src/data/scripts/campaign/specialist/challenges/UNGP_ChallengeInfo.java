package data.scripts.campaign.specialist.challenges;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.*;

public final class UNGP_ChallengeInfo {
    private String id;
    private String name;
    private List<URule> rulesRequired;
    private int positiveLimitation;
    private int durationByMonth; //Month
    private boolean needMaxLevel;
    private boolean canReselectRules;
    private URule milestoneToUnlock;
    private boolean isValid = true;

    public UNGP_ChallengeInfo(String id, String name, List<String> rulesRequired, int positiveLimitation, int durationByMonth, boolean needMaxLevel, boolean canReselectRules, String milestoneToUnlock) {
        this.id = id;
        this.name = name;
        this.positiveLimitation = positiveLimitation;
        this.durationByMonth = durationByMonth;
        this.needMaxLevel = needMaxLevel;
        this.canReselectRules = canReselectRules;
        this.rulesRequired = new ArrayList<>();
        for (String ruleID : rulesRequired) {
            URule rule = URule.getByID(ruleID);
            if (rule != null) {
                this.rulesRequired.add(rule);
            } else {
                isValid = false;
                return;
            }
        }
        URule mileStoneUnlock = URule.getByID(milestoneToUnlock);
        if (mileStoneUnlock != null) {
            this.milestoneToUnlock = mileStoneUnlock;
        } else {
            isValid = false;
            return;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<URule> getRulesRequired() {
        return rulesRequired;
    }

    public boolean isRulesContainRequired(List<String> rules) {
        List<String> rulesRequiredIDs = new ArrayList<>();
        for (URule rule : rulesRequired) {
            rulesRequiredIDs.add(rule.getId());
        }
        return rules.containsAll(rulesRequiredIDs);
    }

    public int getPositiveLimitation() {
        return positiveLimitation;
    }

    public boolean isPositiveLimited() {
        return positiveLimitation != -1;
    }

    public boolean isAbovePositiveLimitation(int ruleAmount) {
        return isPositiveLimited() && ruleAmount > positiveLimitation;
    }


    public int getDurationByMonth() {
        return durationByMonth;
    }

    public boolean isNeedMaxLevel() {
        return needMaxLevel;
    }

    public URule getMilestoneToUnlock() {
        return milestoneToUnlock;
    }

    /**
     * 获取所需规则的链接字符串。例如：xxx+xxx
     * Get the connected rules names.
     *
     * @return
     */
    public String getConnectedRuleNames() {
        StringBuilder sb = new StringBuilder();
        for (URule rule : rulesRequired) {
            sb.append(rule.getName()).append("+");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public void createTooltip(TooltipMakerAPI tooltip, float pad, int elapsedMonth) {
        URule unlockRule = milestoneToUnlock;
        if (unlockRule != null) {
            TooltipMakerAPI imageTooltip = tooltip.beginImageWithText(unlockRule.getSpritePath(), 64f);
            imageTooltip.addPara(name, getMilestoneColor(), 0);

            Color grayColor = Misc.getGrayColor();
            imageTooltip.addPara(getConnectedRuleNames(), grayColor, 5f);

            if (durationByMonth != -1) {
                imageTooltip.addPara(rules_i18n.format("challenge_tip_desc0_0", "" + (durationByMonth - elapsedMonth)), grayColor, 5f);
            } else {
                imageTooltip.addPara(rules_i18n.get("challenge_tip_desc0_1"), grayColor, 5f);
            }
            if (!canReselectRules) {
                imageTooltip.addPara(rules_i18n.get("challenge_tip_desc3"), grayColor, 5f);
            }
            tooltip.addImageWithText(pad);
        }
    }

    public boolean canReselectRules() {
        return canReselectRules;
    }

    public boolean isValid() {
        return isValid;
    }
}
