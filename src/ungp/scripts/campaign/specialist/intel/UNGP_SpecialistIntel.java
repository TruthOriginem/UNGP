package ungp.scripts.campaign.specialist.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import ungp.scripts.campaign.specialist.dialog.UNGP_RepickRulesDialog;
import ungp.scripts.campaign.specialist.items.UNGP_RuleItem;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.utils.Constants;
import ungp.scripts.utils.UNGP_Feedback;
import ungp.scripts.ui.HorizontalButtonGroup;
import ungp.scripts.ui.UIRect;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.IntelSortTier.TIER_0;
import static com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import static com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;
import static ungp.scripts.utils.Constants.root_i18n;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.*;

public class UNGP_SpecialistIntel extends BaseIntelPlugin {
    private static final String KEY = "UNGP_SI";
    private static final String FEED_BACK = "UNGP_feedBack";

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
        return (UNGP_SpecialistIntel) intel;
    }

    private static final Object OPTION_ID_DETAILS = new Object();
    private static final Object OPTION_ID_TIPS = new Object();
    private static Object checkedButton = OPTION_ID_DETAILS;
    private transient ButtonAPI simulationCheckBox = null;

    @Override
    public void notifyPlayerAboutToOpenIntelScreen() {
        UNGP_SpecialistBackgroundUI.cleanBGUI();
        checkedButton = OPTION_ID_DETAILS;
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        UNGP_SpecialistBackgroundUI.resumeTicking();
        CustomPanelAPI customPanel = panel.createCustomPanel(width, height, new UNGP_SpecialistBackgroundUI());
        panel.addComponent(customPanel);
        // 数据处理
        TooltipMakerAPI tooltip;
        Color positiveColor = Misc.getHighlightColor();
        boolean showDetails = checkedButton == OPTION_ID_DETAILS;

        UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
        List<URule> bonusRules = new ArrayList<>();
        List<URule> notBonusRules = new ArrayList<>();
        for (URule rule : ACTIVATED_RULES_IN_THIS_GAME) {
            if (rule.isBonus()) {
                bonusRules.add(rule);
            } else {
                notBonusRules.add(rule);
            }
        }
        Collections.sort(bonusRules, new UNGP_RulesManager.UNGP_RuleSorter());
        Collections.sort(notBonusRules, new UNGP_RulesManager.UNGP_RuleSorter());

        // 状态
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

        // UI生成
        float contentShrink = 20f;
        UIRect fullScreen = new UIRect(0, 0, width, height);
        UIRect[] fullScreenSplits = fullScreen.splitVertically(120f);
        UIRect titleRect = fullScreenSplits[0];
        // title
        {
            UIRect[] titleRectSplits = titleRect.splitHorizontally(0.35f, 0.35f, 0.3f);
            UIRect levelTitle = titleRectSplits[0].shrink(contentShrink);
            {
                tooltip = levelTitle.beginTooltip(panel, false);
                UNGP_SpecialistSettings.Difficulty difficulty = inGameData.getDifficulty();
                TooltipMakerAPI imageMaker = tooltip.beginImageWithText(difficulty.spritePath, 80f);
                imageMaker.setParaOrbitronLarge();
                imageMaker.addPara(root_i18n.get("rulepick_level"), 0, difficulty.color, difficulty.name);
                imageMaker.setParaFontDefault();
                tooltip.addImageWithText(0f);
                levelTitle.addTooltip();
            }
            UIRect checkBoxRect = titleRectSplits[1].shrink(contentShrink);
            {
                tooltip = checkBoxRect.beginTooltip(panel, false);
                final float checkBoxRectWidth = checkBoxRect.getWidth();
                final float checkBoxRectHeight = checkBoxRect.getHeight();
                ButtonAPI button_details = tooltip.addAreaCheckbox(Constants.rules_i18n.get("button_details"), OPTION_ID_DETAILS,
                                                                   Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                                   checkBoxRectWidth, checkBoxRectHeight * 0.4f, 0f);
                tooltip.addTooltipToPrevious(new TooltipCreator() {
                    @Override
                    public boolean isTooltipExpandable(Object tooltipParam) {
                        return false;
                    }

                    @Override
                    public float getTooltipWidth(Object tooltipParam) {
                        return checkBoxRectWidth * 0.4f;
                    }

                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addPara(Constants.rules_i18n.get("button_details_tooltip"), 0f);
                    }
                }, TooltipLocation.LEFT);
                button_details.setChecked(checkedButton == OPTION_ID_DETAILS);
                ButtonAPI button_tips = tooltip.addAreaCheckbox(Constants.rules_i18n.get("button_tips"), OPTION_ID_TIPS,
                                                                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                                checkBoxRectWidth, checkBoxRectHeight * 0.4f, checkBoxRectHeight * 0.2f);
                tooltip.addTooltipToPrevious(new TooltipCreator() {
                    @Override
                    public boolean isTooltipExpandable(Object tooltipParam) {
                        return false;
                    }

                    @Override
                    public float getTooltipWidth(Object tooltipParam) {
                        return checkBoxRectWidth * 0.4f;
                    }

                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addPara(Constants.rules_i18n.get("button_tips_tooltip"), 0f);
                    }
                }, TooltipLocation.LEFT);
                button_tips.setChecked(checkedButton == OPTION_ID_TIPS);
                checkBoxRect.addTooltip();
            }
            UIRect repickRect = titleRectSplits[2].shrink(contentShrink);
            {
                tooltip = repickRect.beginTooltip(panel, false);
                tooltip.addPara(Constants.rules_i18n.get("current_cycle") + "%s", 0f, positiveColor, Global.getSector().getClock().getCycle() + "").setAlignment(Alignment.RMID);
                boolean lockedBecauseOfChallenges = UNGP_ChallengeManager.isRepickLockedByChallenges();
                if (lockedBecauseOfChallenges) {
                    tooltip.addPara(Constants.rules_i18n.get("repick_blocked"), Misc.getNegativeHighlightColor(), 0f).setAlignment(Alignment.RMID);
                } else {
                    tooltip.addPara(Constants.rules_i18n.get("current_times_to_refresh") + "%s", 0f, positiveColor, Constants.rules_i18n.get("repick_rules"), inGameData.getTimesToChangeSpecialistMode() + "").setAlignment(Alignment.RMID);
                }
                Color buttonBase = Misc.getBasePlayerColor();
                Color buttonDark = Misc.getDarkPlayerColor();

                HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
                float repickRectWidth = repickRect.getWidth();
                float buttonPad = 10f;
                ButtonAPI uploadButton = tooltip.addButton(Constants.rules_i18n.get("button_feedback"), FEED_BACK, Misc.getStoryOptionColor(), Misc.getStoryDarkColor(), Alignment.MID, CutStyle.C2_MENU, repickRectWidth * 0.3f, repickRect.getHeight() * 0.35f, 20f);
                uploadButton.setEnabled(!UNGP_Feedback.isFeedbackSent());
                ButtonAPI repickButton = tooltip.addButton(Constants.rules_i18n.get("repick_rules"), KEY, buttonBase, buttonDark, Alignment.MID, CutStyle.C2_MENU, repickRectWidth * 0.7f - buttonPad, repickRect.getHeight() * 0.35f, 20f);
                // 设置重选
                repickButton.setEnabled(!lockedBecauseOfChallenges && inGameData.getTimesToChangeSpecialistMode() > 0);
                buttonGroup.addButton(uploadButton);
                buttonGroup.addButton(repickButton);

                buttonGroup.updateTooltip(tooltip, buttonPad);
                repickRect.addTooltip();
            }
        }


        // content
        UIRect contentRect = fullScreenSplits[1];
        {
            UIRect[] contentRectSplits = contentRect.splitHorizontally(0.35f, 0.35f, 0.3f);
            UIRect positiveRect = contentRectSplits[0];
            {
                UIRect[] positiveRectSplits = positiveRect.splitVertically(30f);
                UIRect positiveRectTitle = positiveRectSplits[0].shrink(contentShrink);
                {
                    // 正面规则标题
                    tooltip = positiveRectTitle.beginTooltip(panel, false);
                    tooltip.setParaOrbitronLarge();
                    tooltip.addPara(UNGP_RulesManager.getPNRuleString(true), UNGP_RulesManager.getPNColor(true), 0f);
                    tooltip.setParaFontDefault();
                    addLine(tooltip, positiveRectTitle.getWidth() - 5f, 3f);
                    positiveRectTitle.addTooltip();
                }
                UIRect positiveRectContent = positiveRectSplits[1].shrink(contentShrink);
                {
                    //正面规则内容
                    tooltip = positiveRectContent.beginTooltip(panel, true);
                    for (URule rule : bonusRules) {
                        TooltipMakerAPI iconMaker = tooltip.beginImageWithText(rule.getSpritePath(), 64f);
                        iconMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                        if (showDetails) {
                            rule.addDesc(iconMaker, 0f);
                            tooltip.addImageWithText(10f);
                            tooltip.addTooltipToPrevious(UNGP_RuleItem.createRuleItemTooltip(rule), TooltipLocation.BELOW);
                        } else if (rule.getRuleEffect().addIntelTips(iconMaker)) {
                            tooltip.addImageWithText(10f);
                        }
                    }
                    positiveRectContent.addTooltip();
                }
            }
            UIRect negativeRect = contentRectSplits[1];
            {
                UIRect[] negativeRectSplits = negativeRect.splitVertically(30f);
                UIRect negativeRectTitle = negativeRectSplits[0].shrink(contentShrink);
                {
                    // 负面规则标题
                    tooltip = negativeRectTitle.beginTooltip(panel, false);
                    tooltip.setParaOrbitronLarge();
                    tooltip.addPara(UNGP_RulesManager.getPNRuleString(false), UNGP_RulesManager.getPNColor(false), 0f);
                    tooltip.setParaFontDefault();
                    addLine(tooltip, negativeRectTitle.getWidth() - 5f, 3f);
                    negativeRectTitle.addTooltip();
                }
                UIRect negativeRectContent = negativeRectSplits[1].shrink(contentShrink);
                {
                    //负面规则内容
                    tooltip = negativeRectContent.beginTooltip(panel, true);
                    for (URule rule : notBonusRules) {
                        TooltipMakerAPI iconMaker = tooltip.beginImageWithText(rule.getSpritePath(), 64f);
                        iconMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                        if (showDetails) {
                            rule.addDesc(iconMaker, 0f);
                            tooltip.addImageWithText(10f);
                            tooltip.addTooltipToPrevious(UNGP_RuleItem.createRuleItemTooltip(rule), TooltipLocation.BELOW);
                        } else if (rule.getRuleEffect().addIntelTips(iconMaker)) {
                            tooltip.addImageWithText(10f);
                        }
                    }
                    negativeRectContent.addTooltip();
                }
            }
            UIRect gameStateRect = contentRectSplits[2];
            {
                UIRect[] gameStateRectSplits = gameStateRect.splitVertically(30f);
                UIRect gameStateRectTitle = gameStateRectSplits[0].shrink(contentShrink);
                {
//                    UIRect[] gameStateRectTitleSplits = gameStateRectTitle.splitHorizontally(0.5f, 0.5f);
//                    gameStateRectTitleSplits
                    // 状态标题
                    tooltip = gameStateRectTitle.beginTooltip(panel, false);
                    tooltip.setParaOrbitronLarge();
                    tooltip.addPara(Constants.rules_i18n.get("suited_state"), Misc.getButtonTextColor(), 0f);
                    float titleWidth = tooltip.computeStringWidth(Constants.rules_i18n.get("suited_state")) + 10;
                    PositionAPI titlePos = tooltip.getPrev().getPosition();
                    tooltip.setParaFontDefault();
                    simulationCheckBox = tooltip.addCheckbox(20f, titlePos.getHeight(), Constants.rules_i18n.get("enable_in_simulation"), ButtonAPI.UICheckboxSize.SMALL, 0f);
                    simulationCheckBox.getPosition().inTL(titleWidth, 0);
                    simulationCheckBox.setChecked(UNGP_SpecialistSettings.isRulesEnabledInSimulation());
                    tooltip.addSpacer(0f).getPosition().inTL(titlePos.getX(), titlePos.getHeight());
                    addLine(tooltip, gameStateRectTitle.getWidth() - 5f, 3f);
                    gameStateRectTitle.addTooltip();
                }
                UIRect gameStateRectContent = gameStateRectSplits[1].shrink(contentShrink);
                {
                    tooltip = gameStateRectContent.beginTooltip(panel, true);
                    tooltip.addSpacer(5f);
                    if (!campaignRules.isEmpty()) {
                        tooltip.setParaOrbitronLarge();
                        tooltip.addPara(Constants.rules_i18n.get("campaign_state"), 0f);
                        tooltip.setParaFontDefault();
                        tooltip.addSpacer(5f);
                        for (URule rule : campaignRules) {
                            TooltipMakerAPI image = tooltip.beginImageWithText(rule.getSpritePath(), 16f);
                            image.addPara(rule.getName(), rule.getCorrectColor(), 0);
                            tooltip.addImageWithText(2f);
                        }
                        tooltip.setBulletedListMode(null);
                    }
                    if (!combatRules.isEmpty()) {
                        tooltip.setParaOrbitronLarge();
                        tooltip.addPara(Constants.rules_i18n.get("combat_state"), 10f);
                        tooltip.setParaFontDefault();
                        tooltip.addSpacer(5f);
                        for (URule rule : combatRules) {
                            TooltipMakerAPI image = tooltip.beginImageWithText(rule.getSpritePath(), 16f);
                            image.addPara(rule.getName(), rule.getCorrectColor(), 0);
                            tooltip.addImageWithText(2f);
                        }
                        tooltip.setBulletedListMode(null);
                    }
                    gameStateRectContent.addTooltip();
                }
            }
        }
    }

    private void addLine(TooltipMakerAPI tooltip, float width, float pad) {
        Color color = new Color(77, 150, 154, 200);
        LabelAPI heading = tooltip.addSectionHeading("", color, color, Alignment.MID, pad);
        heading.getPosition().setSize(width, 1);
    }

    @Override
    public void reportPlayerClickedOn() {
        if (simulationCheckBox != null) {
            UNGP_SpecialistSettings.setRulesEnabledInSimulation(simulationCheckBox.isChecked());
        }
    }

    @Override
    public boolean doesButtonHaveConfirmDialog(Object buttonId) {
        if (buttonId == FEED_BACK) return true;
        return super.doesButtonHaveConfirmDialog(buttonId);
    }

    @Override
    public void createConfirmationPrompt(Object buttonId, TooltipMakerAPI prompt) {
        if (buttonId == FEED_BACK) {
            prompt.addPara(Constants.rules_i18n.get("button_feedback_tips"), 0f, Misc.getStoryOptionColor(), "1");
            prompt.addPara(Constants.rules_i18n.get("button_feedback_tips_detailed"), Misc.getGrayColor(), 20f).italicize();
        }
        super.createConfirmationPrompt(buttonId, prompt);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId == KEY) {
            ui.showDialog(null, new UNGP_RepickRulesDialog(ui, this));
            UNGP_SpecialistBackgroundUI.stopTicking();
        }
        if (buttonId == FEED_BACK) {
            UNGP_Feedback.sendPlayerRulesToServer(UNGP_InGameData.getDataInSave().getActivatedRules());
            UNGP_Feedback.setFeedBackSent();
            Global.getSoundPlayer().playUISound("UNGP_deep_sync", 1f, 1f);
            Global.getSector().getPlayerStats().addStoryPoints(1);
            ui.updateUIForItem(this);
        }
        if (buttonId == OPTION_ID_DETAILS || buttonId == OPTION_ID_TIPS) {
            checkedButton = buttonId;
            ui.updateUIForItem(this);
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
        } else {
            RuleMessage message = (RuleMessage) listInfoParam;
            info.addPara(message.rule.getName(), c, 0f);
            bullet(info);
            info.addPara(message.text, pad, g, h, message.highlights);
        }
        unindent(info);
    }

    public String getDesc() {
        return Constants.rules_i18n.get("mode_desc");
    }

    public String getName() {
        return Constants.rules_i18n.get("mode_name");
    }

    @Override
    public String getIcon() {
        if (listInfoParam == null) {
            return UNGP_SpecialistSettings.getSpecialistModeIconPath();
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
        if (isSendingUpdate()) {
            return getSoundStandardUpdate();
        }
        return "ui_specialist_on";
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
