package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.UNGP_CampaignPlugin;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.UNGP_RepickRulesDialog;
import data.scripts.campaign.hardmode.UNGP_RuleSorter;
import data.scripts.campaign.hardmode.UNGP_RulesManager;
import data.scripts.campaign.hardmode.UNGP_SpecialistSettings;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.IntelSortTier.TIER_0;
import static data.scripts.campaign.UNGP_Settings.d_i18n;
import static data.scripts.campaign.hardmode.UNGP_RulesManager.*;

public class UNGP_SpecialistIntel extends BaseIntelPlugin {
    private static final String KEY = "UNGP_SI";

    public static class RuleMessage {
        URule rule;
        String text;
        String[] highlights;

        public RuleMessage(URule rule, String text, String... highlights) {
            this.rule = rule;
            this.text = text;
            this.highlights = highlights;
        }

        public void send() {
            getInstance().sendUpdateIfPlayerHasIntel(this, false);
        }
    }

    public UNGP_SpecialistIntel() {
        this.setImportant(true);
    }

    public static UNGP_SpecialistIntel getInstance() {
        IntelInfoPlugin intel = Global.getSector().getIntelManager().getFirstIntel(UNGP_SpecialistIntel.class);
        if (intel == null) {
            intel = new UNGP_SpecialistIntel();
            Global.getSector().getIntelManager().addIntel(intel);
        }
        return (UNGP_SpecialistIntel) intel;
    }


    public static void sendUpdate(URule rule) {

    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float marginTopBottom = height * 0.05f;
        float marginLeft = width * 0.05f;
        float contentWidth = width - marginLeft * 2f;
        //0.325 + 0.025 + 0.325 +0.025 + 0.3
        float ruleListWidth = contentWidth * 0.325f;
        float ruleWidthMargin = contentWidth * 0.025f;
        float titleHeight = 80f;
        float titleWidth = width * 0.3f;
        float contentHeightMargin = height * 0.05f;
        float contentHeight = height - marginTopBottom * 2 - titleHeight - contentHeightMargin;
        float detailedWidth = width - marginLeft * 2 - ruleListWidth * 2 - ruleWidthMargin * 2;

        Color positiveColor = Misc.getHighlightColor();

        UNGP_InGameData inGameData = UNGP_CampaignPlugin.getInGameData();
        List<URule> bonusRules = new ArrayList<>();
        List<URule> notBonusRules = new ArrayList<>();
        for (URule rule : ACTIVATED_RULES_IN_THIS_GAME) {
            if (rule.isBonus()) {
                bonusRules.add(rule);
            } else {
                notBonusRules.add(rule);
            }
        }
        Collections.sort(bonusRules, new UNGP_RuleSorter());
        Collections.sort(notBonusRules, new UNGP_RuleSorter());

        TooltipMakerAPI title = panel.createUIElement(titleWidth, titleHeight, false);
        TooltipMakerAPI imageMaker = title.beginImageWithText(UNGP_SpecialistSettings.getSpecialistModeIconPath(), titleHeight);
        imageMaker.setParaOrbitronLarge();
        imageMaker.addPara(d_i18n.get("rulepick_level"), 0, positiveColor, inGameData.getDifficultyLevel() + "");
        imageMaker.setParaFontDefault();
        title.addImageWithText(0f);
        panel.addUIElement(title).inTMid(marginTopBottom);

        float ruleTitleHeight = 20f;
        float ruleListHeight = contentHeight - ruleTitleHeight;
        float ruleTitleTopMargin = marginTopBottom + titleHeight + contentHeightMargin;

        TooltipMakerAPI bonusTitle = panel.createUIElement(ruleListWidth, ruleTitleHeight, false);
        bonusTitle.setParaOrbitronLarge();
        bonusTitle.addPara(UNGP_RulesManager.getBonusString(true), UNGP_RulesManager.getBonusColor(true), 0f);
        bonusTitle.setParaFontDefault();
        panel.addUIElement(bonusTitle).inTL(marginLeft, ruleTitleTopMargin);


        float ruleListTopMargin = ruleTitleTopMargin + ruleTitleHeight;
        TooltipMakerAPI bonusContent = panel.createUIElement(ruleListWidth, ruleListHeight, true);
        for (URule rule : bonusRules) {
            TooltipMakerAPI iconMaker = bonusContent.beginImageWithText(rule.getSpritePath(), 64f);
            iconMaker.addPara(rule.getName(), rule.getBorderColor(), 0f);
            rule.addDesc(iconMaker, 0f);
            bonusContent.addImageWithText(10f);
        }
        panel.addUIElement(bonusContent).inTL(marginLeft, ruleListTopMargin);


        float notBonusLeftMargin = marginLeft + ruleListWidth + ruleWidthMargin;
        TooltipMakerAPI notBonusTitle = panel.createUIElement(ruleListWidth, ruleTitleHeight, false);
        notBonusTitle.setParaOrbitronLarge();
        notBonusTitle.addPara(UNGP_RulesManager.getBonusString(false), UNGP_RulesManager.getBonusColor(false), 0f);
        notBonusTitle.setParaFontDefault();
        panel.addUIElement(notBonusTitle).inTL(notBonusLeftMargin, ruleTitleTopMargin);


        TooltipMakerAPI notbonusContent = panel.createUIElement(ruleListWidth, ruleListHeight, true);
        for (URule rule : notBonusRules) {
            TooltipMakerAPI iconMaker = notbonusContent.beginImageWithText(rule.getSpritePath(), 64f);
            iconMaker.addPara(rule.getName(), rule.getBorderColor(), 0f);
            rule.addDesc(iconMaker, 0f);
            notbonusContent.addImageWithText(10f);
        }
        panel.addUIElement(notbonusContent).inTL(notBonusLeftMargin, ruleListTopMargin);


        float detailedHeight = contentHeight * 0.8f - ruleTitleHeight;
        float detailedRightMargin = marginLeft;

        TooltipMakerAPI detailedTitle = panel.createUIElement(detailedWidth, ruleTitleHeight, false);
        detailedTitle.setParaOrbitronLarge();
        detailedTitle.addPara(rules_i18n.get("suited_state"), Misc.getButtonTextColor(), 0f);
        detailedTitle.setParaFontDefault();
        panel.addUIElement(detailedTitle).inTR(detailedRightMargin, ruleTitleTopMargin);


        List<URule> combatRules = new ArrayList<>();
        List<URule> campaignRules = new ArrayList<>();
        for (URule rule : bonusRules) {
            if (CAMPAIGN_RULES_IN_THIS_GAME.contains(rule)) {
                campaignRules.add(rule);
            }
            if (COMBAT_RULES_IN_THIS_GAME.contains(rule)) {
                combatRules.add(rule);
            }
        }
        for (URule rule : notBonusRules) {
            if (CAMPAIGN_RULES_IN_THIS_GAME.contains(rule)) {
                campaignRules.add(rule);
            }
            if (COMBAT_RULES_IN_THIS_GAME.contains(rule)) {
                combatRules.add(rule);
            }
        }

        float detailedTopMargin = ruleListTopMargin + ruleTitleHeight;
        TooltipMakerAPI gameStateTooltip = panel.createUIElement(detailedWidth, detailedHeight, true);
        if (!campaignRules.isEmpty()) {
            gameStateTooltip.setParaOrbitronLarge();
            gameStateTooltip.addPara(rules_i18n.get("campaign_state"), 0f);
            gameStateTooltip.setParaFontDefault();
            for (URule rule : campaignRules) {
                gameStateTooltip.addPara(rule.getName(), rule.getBorderColor(), 3f);
            }
        }
        if (!combatRules.isEmpty()) {
            gameStateTooltip.setParaOrbitronLarge();
            gameStateTooltip.addPara(rules_i18n.get("combat_state"), 10f);
            gameStateTooltip.setParaFontDefault();
            for (URule rule : combatRules) {
                gameStateTooltip.addPara(rule.getName(), rule.getBorderColor(), 3f);
            }
        }

        panel.addUIElement(gameStateTooltip).inTR(detailedRightMargin, detailedTopMargin);

        float buttonHeight = contentHeight * 0.2f;
        TooltipMakerAPI buttonTooltip = panel.createUIElement(detailedWidth, buttonHeight, false);
        buttonTooltip.addPara(rules_i18n.get("current_cycle") + "%s", 0f, positiveColor, Global.getSector().getClock().getCycle() + "");
        buttonTooltip.addPara(rules_i18n.get("current_times_to_refresh") + "%s", 0f, positiveColor, rules_i18n.get("repick_rules"), inGameData.getTimesToChangeSpecialistMode() + "");
        ButtonAPI button = buttonTooltip.addButton(rules_i18n.get("repick_rules"), KEY, detailedWidth * 0.5f, buttonHeight * 0.3f, 10f);
        button.setEnabled(inGameData.getTimesToChangeSpecialistMode() > 0);
        panel.addUIElement(buttonTooltip).inBR(detailedRightMargin, marginTopBottom);


    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId == KEY) {
            ui.showDialog(null, new UNGP_RepickRulesDialog(ui, this));
        }
    }


    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color c = getTitleColor(mode);
        float pad = 3f;
        float opad = 10f;

        if (listInfoParam == null) {
            info.addPara(getName(), c, 0f);
            bullet(info);
            info.addPara(getDesc(), g, pad);
            unindent(info);
        } else {
            RuleMessage message = (RuleMessage) listInfoParam;
            info.addPara(message.rule.getName(), c, 0f);
            bullet(info);
            info.addPara(message.text, pad, g, h, message.highlights);
            unindent(info);
        }
    }

    public String getDesc() {
        return rules_i18n.get("mode_desc");
    }

    public String getName() {
        return rules_i18n.get("mode_name");
    }

    @Override
    public String getIcon() {
        if (listInfoParam == null) {
            return Global.getSettings().getSpriteName("icons", "UNGP_hmlogo");
        } else {
            RuleMessage message = (RuleMessage) listInfoParam;
            return message.rule.getSpritePath();
        }
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("ungp");
        return tags;
    }

    @Override
    public String getCommMessageSound() {
        return super.getCommMessageSound();
    }

    @Override
    public IntelSortTier getSortTier() {
        return TIER_0;
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }
}
