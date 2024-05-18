package ungp.scripts.campaign.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import ungp.scripts.campaign.specialist.intel.UNGP_SpecialistBackgroundUI;
import ungp.scripts.campaign.specialist.rules.UNGP_RulePickHelper;
import ungp.scripts.campaign.specialist.rules.UNGP_RulePickPresetManager;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import ungp.scripts.ui.CheckBoxGroup;
import ungp.scripts.ui.UIRect;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static ungp.scripts.campaign.ui.UNGP_RulePickItemPanelPlugin.ITEM_SIZE;
import static ungp.scripts.utils.Constants.root_i18n;
import static ungp.scripts.utils.Constants.rules_i18n;

public class UNGP_RulePickPanelPlugin extends BaseCustomUIPanelPlugin {
    private static final Logger LOGGER = Global.getLogger(UNGP_RulePickPanelPlugin.class);
    protected CustomPanelAPI panel;
    protected InteractionDialogAPI dialog;
    protected DialogCallbacks callbacks;

    private PositionAPI pos;
    private List<ButtonAPI> allRuleButtons = new ArrayList<>();
    private CheckBoxGroup presetCheckboxGroup = new CheckBoxGroup();
    private ButtonAPI presetSaveBtn;
    private ButtonAPI presetLoadBtn;
    private ButtonAPI confirmBtn;
    private ButtonAPI cancelBtn;

    private Difficulty difficulty;
    private List<URule> pickedRules;
    private List<String> completedChallenges;

    private LabelAPI positiveRuleCount;
    private LabelAPI negativeRuleCount;

    private LabelAPI l_curCost;
    private LabelAPI l_curRuleCount;

    private UIRect positiveRuleSummaryContentSect;
    private UIRect negativeRuleSummaryContentSect;
    private UIRect challengeContentSect;
    private UIRect reasonUnmetSect;

    private Script onConfirm;
    private Script onCancel;

    private boolean isRuleInit = false;

    //    private LabelAPI


    public UNGP_RulePickPanelPlugin(Difficulty difficulty, List<URule> pickedRules, List<String> completedChallenges, Script onConfirm, Script onCancel) {
        this.difficulty = difficulty;
        this.pickedRules = pickedRules;
        this.completedChallenges = completedChallenges;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public static UNGP_RulePickPanelPlugin createPlugin(Difficulty difficulty, List<URule> previousSelectedRules, List<String> completedChallenges, Script onConfirm, Script onCancel) {
        return new UNGP_RulePickPanelPlugin(difficulty, previousSelectedRules, completedChallenges, onConfirm, onCancel);
    }

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks, InteractionDialogAPI dialog) {
        this.panel = panel;
        this.callbacks = callbacks;
        this.dialog = dialog;
        UNGP_ChallengeManager.setTemporaryCompletedChallenges(completedChallenges);
        // 初始化后增加基本内容
        createContent();


        // 添加背景
        CustomPanelAPI customPanel = panel.createCustomPanel(pos.getWidth(), pos.getHeight(),
                                                             new UNGP_SpecialistBackgroundUI(0.8f));
        UNGP_SpecialistBackgroundUI.stopTicking();
        panel.addComponent(customPanel);

        // 添加音乐
        Global.getSoundPlayer().playCustomMusic(1, 1, "UNGP_rulepicker_bgm", true);
    }

    protected void createContent() {
        // 前期准备
        float panelWidth = pos.getWidth();
        float panelHeight = pos.getHeight();
        Color boarderColor = Misc.getDarkPlayerColor();
        float sectionHeadingHeight = 25f;

        List<URule> allRulesCopy = UNGP_RulesManager.getAvailableRules(completedChallenges);
        List<URule> positiveRules = new ArrayList<>();
        List<URule> negativeRules = new ArrayList<>();

        for (URule rule : allRulesCopy) {
            if (rule.isPositive()) {
                positiveRules.add(rule);
            } else {
                negativeRules.add(rule);
            }
        }
        Collections.sort(positiveRules, new UNGP_RulesManager.UNGP_RuleSorter());
        Collections.sort(negativeRules, new UNGP_RulesManager.UNGP_RuleSorter());

        // UI构筑
        UIRect[] temp;
        UIRect parentPanelRect = new UIRect(0, 0, panelWidth, panelHeight);
        UIRect shrinkPanelRect = parentPanelRect.shrink(20f);

        // 分左右
        // 左边规则区，右边说明及当前一览
        temp = shrinkPanelRect.splitHorizontallyReverse(512f);

        UIRect ruleRect = temp[0];
        UIRect readMeAndDetailsRect = temp[1];

        temp = ruleRect.splitVertically(0.46f, 0.46f, 0.08f);

        UIRect positiveRuleRect = temp[0].shrink(5f);
        UIRect negativeRuleRect = temp[1].shrink(5f);
        UIRect presetRect = temp[2].shrink(5f);

        createRuleRect(ruleRect, positiveRuleRect, positiveRules, negativeRuleRect, negativeRules);
        // 预设
        presetRect.addBoarder(panel, boarderColor, 2f);
        //        TooltipMakerAPI presetRectTooltip = presetRect.beginTooltip(panel, false);
        //        UIComponentAPI presetRectBoarder = presetRectTooltip.createRect(boarderColor, 2f);
        //        presetRectTooltip.addCustomDoNotSetPosition(presetRectBoarder).getPosition().inTL(0f, 0f)
        //                .setSize(presetRect.getWidth(), presetRect.getHeight());
        //        presetRect.addTooltip();

        temp = presetRect.shrink(10f).splitHorizontally(1f, 1f, 1f, 1f, 1f, 1f);

        for (int i = 0; i < 4; i++) {
            UIRect presetItemRect = temp[i];
            TooltipMakerAPI presetItemTooltip = presetItemRect.beginTooltip(panel, false);
            String presetId = "preset_" + i;
            ButtonAPI presetItemCheckbox = presetItemTooltip.addCheckbox(presetItemRect.getWidth(), presetItemRect.getHeight(),
                                                                         rules_i18n.get("rule_preset_prefix") + " " + (i + 1), presetId, ButtonAPI.UICheckboxSize.SMALL, 0);
            if (i == 0) presetItemCheckbox.setChecked(true);
            presetCheckboxGroup.addCheckBox(presetItemCheckbox, presetId);
            presetItemRect.addTooltip();
        }

        // 保存预设
        UIRect presetSaveRect = temp[4].shrink(10f);
        TooltipMakerAPI presetSaveTooltip = presetSaveRect.beginTooltip(panel, false);
        presetSaveBtn = presetSaveTooltip.addButton(rules_i18n.get("rule_preset_button_save"), "preset_save", presetSaveRect.getWidth(), presetSaveRect.getHeight(), 0f);
        presetSaveRect.addTooltip();
        // 读取预设
        UIRect presetLoadRect = temp[5].shrink(10f);
        TooltipMakerAPI presetLoadTooltip = presetLoadRect.beginTooltip(panel, false);
        presetLoadBtn = presetLoadTooltip.addButton(rules_i18n.get("rule_preset_button_load"), "preset_load", presetLoadRect.getWidth(), presetLoadRect.getHeight(), 0f);
        presetLoadRect.addTooltip();

        // 右侧
        temp = readMeAndDetailsRect.splitVertically(0.35f, 0.5f, 0.15f);

        UIRect readMeSect = temp[0].shrink(10f);
        UIRect detailSect = temp[1];
        UIRect confirmSect = temp[2];

        // 用于显示规则的界面
        readMeSect.addBoarder(panel, boarderColor, 2f);

        readMeSect = readMeSect.shrink(10f);
        TooltipMakerAPI readMeTooltip = readMeSect.beginTooltip(panel, true);
        TooltipMakerAPI imageTooltip = readMeTooltip.beginImageWithText(difficulty.spritePath, 64f);
        imageTooltip.setParaOrbitronLarge();
        if (difficulty == Difficulty.OMEGA) {
            imageTooltip.addPara(root_i18n.get("rulepick_level"), 0f, Misc.getNegativeHighlightColor(), difficulty.color, difficulty.name);
        } else {
            imageTooltip.addPara(root_i18n.get("rulepick_level"), 0f, difficulty.color, difficulty.name);
        }
        imageTooltip.setParaFontDefault();
        imageTooltip.setBulletedListMode("  - ");
        imageTooltip.addPara(rules_i18n.get("pick_rule_min_rule_count") + ": %s", 10f, Misc.getBasePlayerColor(),
                             Misc.getHighlightColor(), difficulty.minRules + "");
        imageTooltip.addPara(rules_i18n.get("pick_rule_max_rule_count") + ": %s", 0f, Misc.getBasePlayerColor(),
                             Misc.getHighlightColor(), difficulty.maxRules + "");
        imageTooltip.setBulletedListMode(null);
        readMeTooltip.addImageWithText(0);

        // 展示规则
        readMeTooltip.setParaOrbitronVeryLarge();
        readMeTooltip.addPara(rules_i18n.get("pick_rule_basic"), Misc.getBasePlayerColor(), 10f);
        readMeTooltip.setParaOrbitronLarge();
        readMeTooltip.setBulletedListMode("  - ");
        readMeTooltip.addPara(rules_i18n.get("pick_rule_basic1"), 10f, UNGP_RulesManager.getPNColor(true),
                              UNGP_RulesManager.getPNRuleString(true));
        readMeTooltip.addPara(rules_i18n.get("pick_rule_basic2"), 0f, UNGP_RulesManager.getPNColor(false),
                              UNGP_RulesManager.getPNRuleString(false));
        readMeTooltip.addPara(rules_i18n.get("pick_rule_basic3"), 0f, UNGP_RulesManager.getGoldenColor(),
                              UNGP_RulesManager.getGoldenRuleString());
        readMeTooltip.addPara(rules_i18n.get("pick_rule_basic4"), 0f, UNGP_RulesManager.getMilestoneColor(),
                              UNGP_RulesManager.getRuleTypeName(true, false, true),
                              rules_i18n.get("challenge_name"));
        readMeTooltip.addPara(rules_i18n.get("pick_rule_basic5"), 0f, Misc.getHighlightColor(),
                              "0");
        readMeTooltip.setBulletedListMode(null);

        readMeTooltip.setParaOrbitronVeryLarge();
        readMeTooltip.addPara(rules_i18n.get("pick_rule_challenge"), Misc.getBasePlayerColor(), 10f);
        readMeTooltip.setParaOrbitronLarge();
        readMeTooltip.setBulletedListMode("  - ");
        readMeTooltip.addPara(rules_i18n.get("pick_rule_challenge1"), 10f);
        readMeTooltip.addPara(rules_i18n.get("pick_rule_challenge2"), 0f, Difficulty.ALPHA.color, Difficulty.ALPHA.name);
        readMeTooltip.setBulletedListMode(null);
        readMeSect.addTooltip();

        // 细节
        temp = detailSect.splitVerticallyReverse(100f);

        UIRect ruleSummaryRect = temp[0].shrink(10f);
        UIRect ruleConditionSect = temp[1].shrink(0f, 0f, 10f, 10f);


        // 正负面规则/挑战模块UI分割
        ruleSummaryRect.addBoarder(panel, boarderColor, 2f);

        temp = ruleSummaryRect.splitVertically(0.6f, 0.4f);
        UIRect pnRulesSect = temp[0];
        UIRect challengeSect = temp[1];

        // 正负面分割
        temp = pnRulesSect.splitHorizontally(0.5f, 0.5f);
        UIRect positiveRuleSummarySect = temp[0];
        UIRect negativeRuleSummarySect = temp[1];

        // 正面归纳
        temp = positiveRuleSummarySect.splitVertically(sectionHeadingHeight);
        UIRect positiveRuleSummaryHeadingSect = temp[0];

        positiveRuleSummaryContentSect = temp[1].shrink(2f, 0f);
        //        positiveRuleSummaryContentSect.setRootPanel(panel);

        TooltipMakerAPI positiveRuleSummaryHeadingTooltip = positiveRuleSummaryHeadingSect.beginTooltip(panel, false);
        positiveRuleSummaryHeadingSect.syncPositionSize(
                positiveRuleSummaryHeadingTooltip.addSectionHeading(UNGP_RulesManager.getPNRuleString(true),
                                                                    UNGP_RulesManager.getPNColor(true),
                                                                    boarderColor, Alignment.MID, 0f)
                        .getPosition());
        positiveRuleSummaryHeadingSect.addTooltip();
        // 负面归纳
        temp = negativeRuleSummarySect.splitVertically(sectionHeadingHeight);
        UIRect negativeRuleSummaryHeadingSect = temp[0];

        negativeRuleSummaryContentSect = temp[1].shrink(2f, 0f);
        //        negativeRuleSummaryContentSect.setRootPanel(panel);

        TooltipMakerAPI negativeRuleSummaryHeadingTooltip = negativeRuleSummaryHeadingSect.beginTooltip(panel, false);
        negativeRuleSummaryHeadingSect.syncPositionSize(
                negativeRuleSummaryHeadingTooltip.addSectionHeading(UNGP_RulesManager.getPNRuleString(false),
                                                                    UNGP_RulesManager.getPNColor(false),
                                                                    boarderColor, Alignment.MID, 0f)
                        .getPosition());
        negativeRuleSummaryHeadingSect.addTooltip();
        // 专家挑战
        temp = challengeSect.splitVertically(sectionHeadingHeight);
        UIRect challengeHeadingSect = temp[0];

        challengeContentSect = temp[1].shrink(2f, 0f);
        //        challengeContentSect.setRootPanel(panel);

        TooltipMakerAPI challengeHeadingTooltip = challengeHeadingSect.beginTooltip(panel, false);
        challengeHeadingSect.syncPositionSize(
                challengeHeadingTooltip.addSectionHeading(rules_i18n.get("challenge_name"),
                                                          UNGP_RulesManager.getMilestoneColor(),
                                                          boarderColor, Alignment.MID, 0f)
                        .getPosition());
        challengeHeadingSect.addTooltip();


        // 规则规则
        ruleConditionSect.addBoarder(panel, boarderColor, 2f);

        //        ruleConditionSect = ruleConditionSect.shrink(10f);
        temp = ruleConditionSect.splitVertically(sectionHeadingHeight);
        UIRect ruleConditionHeadingSect = temp[0];
        TooltipMakerAPI ruleConditionHeadingTooltip = ruleConditionHeadingSect.beginTooltip(panel, false);
        ruleConditionHeadingSect.syncPositionSize(ruleConditionHeadingTooltip.addSectionHeading(rules_i18n.get("pick_rule_condition_heading"), Alignment.MID, 0f)
                                                          .getPosition());
        ruleConditionHeadingSect.addTooltip();

        UIRect ruleConditionContentSect = temp[1].shrink(10f);

        TooltipMakerAPI ruleConditionTooltip = ruleConditionContentSect.beginTooltip(panel, false);
        ruleConditionTooltip.setParaOrbitronLarge();
        l_curCost = ruleConditionTooltip.addPara("", Misc.getBasePlayerColor(), 5f);
        l_curRuleCount = ruleConditionTooltip.addPara("", Misc.getBasePlayerColor(), 5f);
        ruleConditionTooltip.setParaFontDefault();
        ruleConditionContentSect.addTooltip();

        // 确认处
        temp = confirmSect.splitHorizontally(0.6f, 0.4f);
        UIRect reasonSect = temp[0].shrink(10f);
        UIRect okOrNotSect = temp[1].shrink(10f);

        reasonSect.addBoarder(panel, boarderColor, 2f);

        temp = reasonSect.splitVertically(sectionHeadingHeight);

        UIRect reasonUnmetHeading = temp[0];
        TooltipMakerAPI reasonUnmetHeadingTooltip = reasonUnmetHeading.beginTooltip(panel, false);
        reasonUnmetHeading.syncPositionSize(reasonUnmetHeadingTooltip.addSectionHeading(rules_i18n.get("unmet_heading"), Alignment.MID, 0f).getPosition());
        reasonUnmetHeading.addTooltip();

        reasonUnmetSect = temp[1].shrink(10f);
        //        reasonUnmetSect.setRootPanel(panel);
        // 按钮
        temp = okOrNotSect.splitVertically(0.5f, 0.5f);
        UIRect confirmBtnSect = temp[0].shrink(0f, 5f);
        TooltipMakerAPI confirmBtnTooltip = confirmBtnSect.beginTooltip(panel, false);
        confirmBtnTooltip.setButtonFontOrbitron20Bold();
        confirmBtn = confirmBtnTooltip.addButton(rules_i18n.get("pick_rule_confirm"), "confirm", confirmBtnSect.getWidth(), confirmBtnSect.getHeight(), 0f);
        confirmBtnSect.addTooltip();

        UIRect cancelBtnSect = temp[1].shrink(0f, 5f);
        TooltipMakerAPI cancelBtnTooltip = cancelBtnSect.beginTooltip(panel, false);
        cancelBtnTooltip.setButtonFontOrbitron20Bold();
        cancelBtn = cancelBtnTooltip.addButton(rules_i18n.get("pick_rule_cancel"), "cancel", cancelBtnSect.getWidth(), cancelBtnSect.getHeight(), 0f);
        cancelBtn.setShortcut(Keyboard.KEY_ESCAPE, true);
        cancelBtnSect.addTooltip();

        //规则选择初始化
        if (!isRuleInit) {
            isRuleInit = true;
            for (ButtonAPI ruleButton : allRuleButtons) {
                URule rule = (URule) ruleButton.getCustomData();
                if (pickedRules.contains(rule)) {
                    ruleButton.setChecked(true);
                }
            }
        }
        // 更新
        triggerPresetEvents(null);
        updateRuleThings();
    }

    /**
     * 更新规则区域UI
     *
     * @param ruleRect
     * @param positiveRuleRect
     * @param positiveRules
     * @param negativeRuleRect
     * @param negativeRules
     */
    private void createRuleRect(UIRect ruleRect, UIRect positiveRuleRect, List<URule> positiveRules, UIRect negativeRuleRect, List<URule> negativeRules) {
        UIRect[] temp;
        // 规则变量初设
        float rulePadding = 10f;
        float ruleYPadding = rulePadding * 2.5f;
        int itemPerRow = (int) (ruleRect.getWidth() / (ITEM_SIZE + rulePadding));

        // 正面规则区域
        temp = positiveRuleRect.splitVertically(32f);

        UIRect positiveRuleTitleRect = temp[0];
        UIRect positiveRuleContentRect = temp[1];

        TooltipMakerAPI positiveRuleTitleTooltip = positiveRuleTitleRect.beginTooltip(panel, false);

        TooltipMakerAPI positiveRuleTitleImageTooltip = positiveRuleTitleTooltip.beginImageWithText(
                UNGP_RulesManager.getRuleIconSpriteName(false, false, true), 16f);
        positiveRuleTitleImageTooltip.setParaOrbitronLarge();
        positiveRuleTitleImageTooltip.addPara(UNGP_RulesManager.getPNRuleString(true), UNGP_RulesManager.getPNColor(true), 0f);
        positiveRuleTitleTooltip.addImageWithText(0f);

        // 正面规则当前数量
        positiveRuleCount = positiveRuleTitleTooltip.createLabel("", Color.white);
        positiveRuleCount.setAlignment(Alignment.RMID);
        PositionAPI positiveRuleCountPos = positiveRuleTitleTooltip.addCustomDoNotSetPosition((UIComponentAPI) positiveRuleCount).getPosition();
        positiveRuleCountPos.setSize(positiveRuleTitleRect.getWidth() - 110f, positiveRuleTitleRect.getHeight());
        positiveRuleCountPos.inTL(0f, 0f);
        // 取消选择全部按钮
        positiveRuleTitleTooltip.addButton(rules_i18n.get("pick_rule_clear"), "clear_positive_rules", Misc.getBasePlayerColor(),
                                           Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BL_TR,
                                           100f, positiveRuleTitleRect.getHeight() * 0.8f, 0)
                .getPosition().inTL(positiveRuleTitleRect.getWidth() - 100f, 0f);

        positiveRuleTitleRect.addTooltip();

        TooltipMakerAPI positiveRuleContentTooltip = positiveRuleContentRect.beginTooltip(panel, true);
        for (int i = 0; i < positiveRules.size(); i++) {
            URule curRule = positiveRules.get(i);
            UNGP_RulePickItemPanelPlugin plugin = new UNGP_RulePickItemPanelPlugin(curRule, new UNGP_RulePickItemPanelPlugin.ButtonPressListener() {
                @Override
                public void notifyPressed(ButtonAPI button) {
                    LOGGER.info("Pressed:" + button.getCustomData());
                    buttonPressed(button);
                }
            });
            CustomPanelAPI custom = Global.getSettings().createCustom(ITEM_SIZE, ITEM_SIZE, plugin);
            plugin.init(custom);

            int x_index = i % itemPerRow;
            int y_index = i / itemPerRow;
            float itemX = x_index * ITEM_SIZE + x_index * rulePadding;
            float itemY = y_index * ITEM_SIZE + y_index * ruleYPadding;
            // 设置该规则位置，因为是自动不占面积的所以需要addSpacer来提供滚动
            positiveRuleContentTooltip.addCustomDoNotSetPosition(custom).getPosition().inTL(itemX, itemY);
            // 用于给 Spoiler 增加滚动空间
            positiveRuleContentTooltip.addSpacer((1f / itemPerRow) * (ITEM_SIZE + ruleYPadding));

            allRuleButtons.add(plugin.getLinkedButton());
        }
        // 预留一行
        positiveRuleContentTooltip.addSpacer(ITEM_SIZE);
        positiveRuleContentRect.addTooltip();

        // 负面规则区域
        temp = negativeRuleRect.splitVertically(32f);

        UIRect negativeRuleTitleRect = temp[0];
        UIRect negativeRuleContentRect = temp[1];

        TooltipMakerAPI negativeRuleTitleTooltip = negativeRuleTitleRect.beginTooltip(panel, false);
        TooltipMakerAPI negativeRuleTitleImageTooltip = negativeRuleTitleTooltip.beginImageWithText(
                UNGP_RulesManager.getRuleIconSpriteName(false, false, false), 16f);
        negativeRuleTitleImageTooltip.setParaOrbitronLarge();
        negativeRuleTitleImageTooltip.addPara(UNGP_RulesManager.getPNRuleString(false), UNGP_RulesManager.getPNColor(false), 0f);
        negativeRuleTitleTooltip.addImageWithText(0f);

        // 负面规则当前数量
        negativeRuleCount = negativeRuleTitleTooltip.createLabel("", Color.white);
        negativeRuleCount.setAlignment(Alignment.RMID);
        PositionAPI negativeRuleCountPos = negativeRuleTitleTooltip.addCustomDoNotSetPosition((UIComponentAPI) negativeRuleCount).getPosition();
        negativeRuleCountPos.setSize(negativeRuleTitleRect.getWidth() - 110f, negativeRuleTitleRect.getHeight());
        negativeRuleCountPos.inTL(0f, 0f);
        // 取消选择全部按钮
        negativeRuleTitleTooltip.addButton(rules_i18n.get("pick_rule_clear"), "clear_negative_rules", Misc.getBasePlayerColor(),
                                           Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BL_TR,
                                           100f, negativeRuleTitleRect.getHeight() * 0.8f, 0)
                .getPosition().inTL(negativeRuleTitleRect.getWidth() - 100f, 0f);

        negativeRuleTitleRect.addTooltip();

        TooltipMakerAPI negativeRuleContentTooltip = negativeRuleContentRect.beginTooltip(panel, true);
        for (int i = 0; i < negativeRules.size(); i++) {
            URule curRule = negativeRules.get(i);
            UNGP_RulePickItemPanelPlugin plugin = new UNGP_RulePickItemPanelPlugin(curRule, new UNGP_RulePickItemPanelPlugin.ButtonPressListener() {
                @Override
                public void notifyPressed(ButtonAPI button) {
                    buttonPressed(button);
                }
            });
            CustomPanelAPI custom = Global.getSettings().createCustom(ITEM_SIZE, ITEM_SIZE, plugin);
            plugin.init(custom);

            int x_index = i % itemPerRow;
            int y_index = i / itemPerRow;
            float itemX = x_index * ITEM_SIZE + x_index * rulePadding;
            float itemY = y_index * ITEM_SIZE + y_index * ruleYPadding;
            // 设置该规则位置，因为是自动不占面积的所以需要addSpacer来提供滚动
            negativeRuleContentTooltip.addCustomDoNotSetPosition(custom).getPosition().inTL(itemX, itemY);
            // 用于给 Spoiler 增加滚动空间
            negativeRuleContentTooltip.addSpacer((1f / itemPerRow) * (ITEM_SIZE + ruleYPadding));

            allRuleButtons.add(plugin.getLinkedButton());
        }
        // 预留一行
        negativeRuleContentTooltip.addSpacer(ITEM_SIZE);
        negativeRuleContentRect.addTooltip();
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.pos = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (pos != null && difficulty != null) {
            // 简单背景
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            Color darkerColor = Misc.scaleColor(difficulty.color, 0.1f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor3ub((byte) darkerColor.getRed(), (byte) darkerColor.getGreen(), (byte) darkerColor.getBlue());
            GL11.glVertex2f(pos.getX(), pos.getY() + pos.getHeight());
            GL11.glVertex2f(pos.getX() + pos.getWidth(), pos.getY() + pos.getHeight());
            GL11.glColor3ub((byte) 0, (byte) 0, (byte) 0);
            GL11.glVertex2f(pos.getX() + pos.getWidth(), pos.getY());
            GL11.glVertex2f(pos.getX(), pos.getY());
            GL11.glEnd();
            GL11.glPopMatrix();
        }
    }

    @Override
    public void buttonPressed(Object buttonId) {
        LOGGER.info("Pressed:" + buttonId);
        triggerPresetEvents(buttonId);
        String btnId = buttonId.toString();
        switch (btnId) {
            case "clear_positive_rules": {
                resetSpecificRuleButtons(true);
                break;
            }
            case "clear_negative_rules": {
                resetSpecificRuleButtons(false);
                break;
            }
            default:
                break;
        }
        triggerRuleEvents(null);
        switch (btnId) {
            case "confirm": {
                pickedRules.clear();
                for (ButtonAPI ruleButton : allRuleButtons) {
                    if (!ruleButton.isChecked()) continue;
                    URule rule = (URule) ruleButton.getCustomData();
                    if (rule != null) {
                        pickedRules.add(rule);
                    }
                }
                onConfirm.run();
                exit();
                break;
            }
            case "cancel": {
                exit();
                break;
            }
            default:
                break;
        }
    }

    /**
     * 每次按規則都会触发这个（接口在单独的按钮插件）
     *
     * @param ruleButton
     */
    public void buttonPressed(ButtonAPI ruleButton) {
        triggerRuleEvents(ruleButton);
    }

    private void resetAllRuleButtons() {
        for (ButtonAPI ruleButton : allRuleButtons) {
            ruleButton.setChecked(false);
            ruleButton.setEnabled(true);
        }
    }

    private void resetSpecificRuleButtons(boolean isPositive) {
        for (ButtonAPI ruleButton : allRuleButtons) {
            URule rule = (URule) ruleButton.getCustomData();
            if (rule != null && rule.isPositive() == isPositive) {
                ruleButton.setChecked(false);
                ruleButton.setEnabled(true);
            }
        }
    }

    private void triggerPresetEvents(Object buttonId) {
        presetCheckboxGroup.updateCheck();
        String presetSlot = (String) presetCheckboxGroup.getCheckedValue();
        if (presetSlot != null) {
            presetLoadBtn.setEnabled(UNGP_RulePickPresetManager.isPresetExists(presetSlot));
            if ("preset_save".equals(buttonId)) {
                List<URule> pickedRules = new ArrayList<>();
                for (ButtonAPI ruleButton : allRuleButtons) {
                    if (ruleButton.isChecked()) {
                        URule rule = (URule) ruleButton.getCustomData();
                        if (rule != null)
                            pickedRules.add(rule);
                    }
                }
                UNGP_RulePickPresetManager.save(presetSlot, pickedRules);
            } else if ("preset_load".equals(buttonId)) {
                List<URule> rules = UNGP_RulePickPresetManager.load(presetSlot);
                if (!rules.isEmpty()) {
                    resetAllRuleButtons();
                    for (ButtonAPI ruleButton : allRuleButtons) {
                        URule rule = (URule) ruleButton.getCustomData();
                        if (rule != null)
                            ruleButton.setChecked(rules.contains(rule));
                    }
                    triggerRuleEvents(null);
                    triggerPresetEvents(null);
                }
            }
        }
    }

    private void triggerRuleEvents(ButtonAPI pressedRuleButton) {
        if (pressedRuleButton != null) {
            updateGoldenRuleButtonStatus(pressedRuleButton);
        } else {
            ButtonAPI checkedGoldenBtn = null;
            for (ButtonAPI ruleButton : allRuleButtons) {
                URule rule = (URule) ruleButton.getCustomData();
                if (ruleButton.isChecked() && rule != null && rule.isGolden()) {
                    checkedGoldenBtn = ruleButton;
                    break;
                }
            }
            if (checkedGoldenBtn != null) {
                updateGoldenRuleButtonStatus(checkedGoldenBtn);
            }
        }
        updateRuleThings();
    }

    /**
     * 会进行UI方面的更新
     */
    private void updateRuleThings() {
        int positiveCount = 0, negativeCount = 0;
        int totalCount = 0;
        int cost = 0;

        List<URule> curPickedRules = new ArrayList<>();
        positiveRuleSummaryContentSect.removeLatestSubPanel();
        negativeRuleSummaryContentSect.removeLatestSubPanel();
        reasonUnmetSect.removeLatestSubPanel();
        CustomPanelAPI positivePanel = positiveRuleSummaryContentSect.createSubPanel(panel, null).getPanel();
        CustomPanelAPI negativePanel = negativeRuleSummaryContentSect.createSubPanel(panel, null).getPanel();
        CustomPanelAPI reasonUnmetPanel = reasonUnmetSect.createSubPanel(panel, null).getPanel();
        TooltipMakerAPI positiveTooltip = positivePanel.createUIElement(positiveRuleSummaryContentSect.getWidth(), positiveRuleSummaryContentSect.getHeight(), true);
        TooltipMakerAPI negativeTooltip = negativePanel.createUIElement(negativeRuleSummaryContentSect.getWidth(), negativeRuleSummaryContentSect.getHeight(), true);
        TooltipMakerAPI reasonUnmetTooltip = reasonUnmetPanel.createUIElement(reasonUnmetSect.getWidth(), reasonUnmetSect.getHeight(), true);
        positiveTooltip.addSpacer(5f);
        negativeTooltip.addSpacer(5f);
        for (ButtonAPI ruleButton : allRuleButtons) {
            if (ruleButton.isChecked()) {
                URule rule = (URule) ruleButton.getCustomData();
                if (rule != null) {
                    if (rule.isPositive()) {
                        positiveCount++;
                        TooltipMakerAPI imageMaker = positiveTooltip.beginImageWithText(rule.getSpritePath(), 32f);
                        imageMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                        rule.addShortDesc(imageMaker, 0f);
                        positiveTooltip.addImageWithText(2f);
                    } else {
                        negativeCount++;
                        TooltipMakerAPI imageMaker = negativeTooltip.beginImageWithText(rule.getSpritePath(), 32f);
                        imageMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                        rule.addShortDesc(imageMaker, 0f);
                        negativeTooltip.addImageWithText(2f);
                    }
                    cost += rule.getCost();
                    curPickedRules.add(rule);
                }
            }
        }
        positivePanel.addUIElement(positiveTooltip);
        negativePanel.addUIElement(negativeTooltip);
        totalCount = positiveCount + negativeCount;

        // 更新专家挑战统计
        List<UNGP_ChallengeInfo> runnableChallenges = UNGP_ChallengeManager.getRunnableChallenges(difficulty, curPickedRules, completedChallenges);
        challengeContentSect.removeLatestSubPanel();
        if (!runnableChallenges.isEmpty()) {
            CustomPanelAPI challengeCP = challengeContentSect.createSubPanel(panel, null).getPanel();
            TooltipMakerAPI challengeTooltip = challengeCP.createUIElement(challengeContentSect.getWidth(), challengeContentSect.getHeight(), true);
            challengeTooltip.addSpacer(5f);
            for (UNGP_ChallengeInfo runnableChallenge : runnableChallenges) {
                runnableChallenge.createTooltip(challengeTooltip, 0f, 0);
            }
            challengeCP.addUIElement(challengeTooltip);
        } else {
            if (!UNGP_ChallengeManager.isDifficultyEnough(difficulty)) {
                CustomPanelAPI challengeCP = challengeContentSect.createSubPanel(panel, null).getPanel();
                TooltipMakerAPI challengeTooltip = challengeCP.createUIElement(challengeContentSect.getWidth(), challengeContentSect.getHeight(), true);
                challengeTooltip.setParaOrbitronLarge();
                challengeTooltip.addPara(rules_i18n.format("challenge_tip_difficultyNotMeet", Difficulty.ALPHA.name, Difficulty.OMEGA.name), Misc.getNegativeHighlightColor(), 10f
                                        ).setAlignment(Alignment.MID);
                challengeCP.addUIElement(challengeTooltip);
            }
        }

        // 更新规则数量
        positiveRuleCount.setText(rules_i18n.format("pick_rule_count", positiveCount + "", UNGP_RulesManager.getPNRuleString(true)));
        positiveRuleCount.setHighlight(positiveCount + "", UNGP_RulesManager.getPNRuleString(true));
        positiveRuleCount.setHighlightColors(Misc.getHighlightColor(), UNGP_RulesManager.getPNColor(true));
        negativeRuleCount.setText(rules_i18n.format("pick_rule_count", negativeCount + "", UNGP_RulesManager.getPNRuleString(false)));
        negativeRuleCount.setHighlight(negativeCount + "", UNGP_RulesManager.getPNRuleString(false));
        negativeRuleCount.setHighlightColors(Misc.getHighlightColor(), UNGP_RulesManager.getPNColor(false));

        // 更新总结合计
        l_curCost.setText(String.format(rules_i18n.get("pick_rule_cur_cost") + ": %s", cost + ""));
        l_curCost.setHighlight(cost + "");
        l_curCost.setHighlightColor(cost >= 0 ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor());
        String extraString = "";
        if (totalCount < difficulty.minRules) {
            extraString = " ( < " + difficulty.minRules + " )";
        }
        if (totalCount > difficulty.maxRules) {
            extraString = " ( >" + difficulty.maxRules + " )";
        }
        String resultString = totalCount + extraString;
        l_curRuleCount.setText(String.format(rules_i18n.get("pick_rule_cur_rule_count") + ": %s", resultString));
        l_curRuleCount.setHighlight(resultString);
        l_curRuleCount.setHighlightColor(extraString.isEmpty() ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor());
        // 无法确认的原因
        boolean isConfirmBtnEnabled = true;
        reasonUnmetTooltip.setParaOrbitronLarge();
        for (UNGP_RulePickHelper.UnmetReason unmetReason : UNGP_RulePickHelper.generateUnmetReasons(curPickedRules, difficulty)) {
            isConfirmBtnEnabled = false;
            reasonUnmetTooltip.addPara(rules_i18n.get(unmetReason.getType().getI18nKey()), Misc.getNegativeHighlightColor(), 5f);
        }
        if (isConfirmBtnEnabled) {
            reasonUnmetTooltip.addPara(rules_i18n.get("unmet_none"), Misc.getPositiveHighlightColor(), 5f);
        }
        reasonUnmetPanel.addUIElement(reasonUnmetTooltip);
        confirmBtn.setEnabled(isConfirmBtnEnabled);
    }

    private void updateGoldenRuleButtonStatus(ButtonAPI pressedRuleButton) {
        URule pressedRule = (URule) pressedRuleButton.getCustomData();
        if (pressedRule.isGolden()) {
            if (pressedRuleButton.isChecked()) {
                for (ButtonAPI button : allRuleButtons) {
                    URule rule = (URule) button.getCustomData();
                    if (rule != null && pressedRule != rule && rule.isGolden()) {
                        button.setChecked(false);
                        button.setEnabled(false);
                    }
                }
            } else {
                for (ButtonAPI button : allRuleButtons) {
                    URule rule = (URule) button.getCustomData();
                    if (rule != null && pressedRule != rule && rule.isGolden()) {
                        button.setChecked(false);
                        button.setEnabled(true);
                    }
                }
            }
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (pos == null) return;
        //        for (InputEventAPI event : events) {
        //            if (event.isConsumed()) continue;
        //            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
        //                event.consume();
        //                exit();
        //                return;
        //            }
        //        }
    }

    /**
     * 退出
     */
    public void exit() {
        callbacks.dismissDialog();
        UNGP_ChallengeManager.getTemporaryCompletedChallenges().clear();
        Global.getSoundPlayer().playCustomMusic(1, 1, null);
        if (onCancel != null)
            onCancel.run();
    }
}
