package ungp.scripts.campaign.specialist.challenges;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.getMilestoneColor;
import static ungp.scripts.utils.Constants.rules_i18n;

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

    public boolean isRulesRequiredIncluded(List<String> rules) {
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

    public boolean shouldBeCancelled(int positiveRuleAmount, List<String> currentRuleIDs) {
        return isAbovePositiveLimitation(positiveRuleAmount) || !isRulesRequiredIncluded(currentRuleIDs);
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

    public String getConnectedRuleNamesReplacedWithEscapeCharacter() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rulesRequired.size(); i++) {
            sb.append(" %s ");
            sb.append("+");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public String[] getRuleNames() {
        String[] array = new String[rulesRequired.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = rulesRequired.get(i).getName();
        }
        return array;
    }

    public Color[] getRuleColors() {
        Color[] array = new Color[rulesRequired.size()];
        for (int i = 0; i < array.length; i++) {
            URule rule = rulesRequired.get(i);
            array[i] = rule.getCorrectColor().darker();
        }
        return array;
    }

    public void createTooltip(TooltipMakerAPI tooltip, float pad, int elapsedMonth) {
        URule unlockRule = milestoneToUnlock;
        if (unlockRule != null) {
            TooltipMakerAPI imageTooltip = tooltip.beginImageWithText(unlockRule.getSpritePath(), 64f);
            imageTooltip.addPara(name, getMilestoneColor(), 0);

            Color grayColor = Misc.getGrayColor();
            showChallengeDetails(imageTooltip, grayColor);
            //            imageTooltip.addPara(getConnectedRuleNames(), grayColor, 5f);
            //
            //            if (durationByMonth != -1) {
            //                imageTooltip.addPara(Constants.rules_i18n.format("challenge_tip_desc0_0", "" + (durationByMonth - elapsedMonth)), grayColor, 5f);
            //            } else {
            //                imageTooltip.addPara(Constants.rules_i18n.get("challenge_tip_desc0_1"), grayColor, 5f);
            //            }
            //            if (!canReselectRules) {
            //                imageTooltip.addPara(Constants.rules_i18n.get("challenge_tip_desc3"), grayColor, 5f);
            //            }
            tooltip.addImageWithText(pad);
        }
    }

    public void showChallengeDetails(TooltipMakerAPI tooltip, Color color) {
        tooltip.addPara(rules_i18n.get("challenge_tip_rules_prefix")
                                + getConnectedRuleNamesReplacedWithEscapeCharacter(),
                        5f,
                        color,
                        color,
                        getRuleNames()).setHighlightColors(getRuleColors());
        // 所有要求
        if (getDurationByMonth() == -1) {
            tooltip.addPara(rules_i18n.get("challenge_tip_desc0_1"), color, 0f);
        } else {
            tooltip.addPara(rules_i18n.get("challenge_tip_desc0_0"), color, 0f);
            if (isNeedMaxLevel()) {
                tooltip.addPara(rules_i18n.get("challenge_tip_desc1"), color, 0f);
            }
        }
        if (getPositiveLimitation() >= 0) {
            tooltip.addPara(rules_i18n.format("challenge_tip_desc2", "" + getPositiveLimitation()), color, 0f);
        }
        if (!canReselectRules()) {
            tooltip.addPara(rules_i18n.get("challenge_tip_desc3"), color, 0f);
        }
    }

    public boolean canReselectRules() {
        return canReselectRules;
    }

    public boolean isValid() {
        return isValid;
    }
}
