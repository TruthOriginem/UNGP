package data.scripts.campaign.specialist.rules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoPickerListener;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.everyframe.UNGP_UITimeScript;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.items.UNGP_RuleItemConnectBGManager;
import org.lwjgl.input.Keyboard;
import ungp.ui.UNGPFont;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static data.scripts.campaign.UNGP_Settings.d_i18n;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.*;

public class UNGP_RulePickListener implements CargoPickerListener {
    private List<URule> pickedRules;
    private List<String> completedChallenges;
    private Script onPicked = null;
    private Script onCancelled = null;
    private Difficulty difficulty;
    private boolean isPressedShift = false;
    private boolean detailedMode = false;
    private boolean isAPressed = false;
    private boolean isDPressed = false;
    private int rulePage = 0;

    public UNGP_RulePickListener(List<URule> pickedRules, List<String> completedChallenges, Difficulty difficulty, Script onPicked, Script onCancelled) {
        this.pickedRules = pickedRules;
        this.completedChallenges = completedChallenges;
        this.onPicked = onPicked;
        this.onCancelled = onCancelled;
        this.difficulty = difficulty;
        initUIData();
    }

    @Override
    public void pickedCargo(CargoAPI cargo) {
        List<URule> ruleToAdd = new ArrayList<>();
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (stack.isSpecialStack()) {
                URule rule = URule.getByID(stack.getSpecialDataIfSpecial().getData());
                if (rule != null) {
                    ruleToAdd.add(rule);
                }
            }
        }
        pickedRules.addAll(ruleToAdd);
        Collections.sort(pickedRules, new UNGP_RulesManager.UNGP_RuleSorter());
        if (onPicked != null)
            onPicked.run();
        clearUIData();
    }

    @Override
    public void cancelledCargoSelection() {
        if (onCancelled != null)
            onCancelled.run();
        clearUIData();
    }

    private void initUIData() {
        UNGP_RuleItemConnectBGManager.clear();
        Global.getSoundPlayer().playCustomMusic(1, 1, "UNGP_rulepicker_bgm", true);
        UNGP_UITimeScript.addInterval("2secs", new IntervalUtil(2f, 2f));
        UNGP_UITimeScript.addInterval("6secs", new IntervalUtil(6f, 6f));
    }

    private void clearUIData() {
        UNGP_RuleItemConnectBGManager.clear();
        Global.getSoundPlayer().playCustomMusic(1, 1, null);
        UNGP_UITimeScript.removeInterval("2secs");
        UNGP_UITimeScript.removeInterval("6secs");
        UNGPFont.clearDynamicDrawable();
    }

    @Override
    public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {
        float pad = 3f;
        float small = 5f;
        float opad = 10f;
        Color highlight = Misc.getHighlightColor();
        Color negative = Misc.getNegativeHighlightColor();
        // 满足挑战开启条件(专20)
        if (UNGP_ChallengeManager.isDifficultyEnough(difficulty)) {
            CargoAPI copy = combined.createCopy();
            if (pickedUp != null) {
                copy.addFromStack(pickedUp);
            }
            UNGP_RuleItemConnectBGManager.refresh(copy, completedChallenges);
        }
        // 专家等级:X
        TooltipMakerAPI imageTooltip = panel.beginImageWithText(difficulty.spritePath, 64f);
        imageTooltip.setParaOrbitronLarge();
        if (difficulty == Difficulty.OMEGA) {
            imageTooltip.addPara(d_i18n.get("rulepick_level"), opad, negative, difficulty.color, difficulty.name);
        } else {
            imageTooltip.addPara(d_i18n.get("rulepick_level"), opad, difficulty.color, difficulty.name);
        }
        imageTooltip.setParaFontDefault();
        panel.addImageWithText(0);

        // 展示规则
        LabelAPI desc = panel.addPara(d_i18n.get("rulepick_desc"), opad, highlight,
                                      UNGP_RulesManager.getBonusString(true),
                                      UNGP_RulesManager.getBonusString(false),
                                      UNGP_RulesManager.getGoldenString());
        desc.setHighlightColors(UNGP_RulesManager.getBonusColor(true),
                                UNGP_RulesManager.getBonusColor(false),
                                UNGP_RulesManager.getGoldenColor());

        int cost = 0;
        int amount = 0;
        List<URule> bonusRules = new ArrayList<>();
        List<URule> notBonusRules = new ArrayList<>();
        for (CargoStackAPI stack : combined.getStacksCopy()) {
            if (stack.isSpecialStack()) {
                URule rule = URule.getByID(stack.getSpecialDataIfSpecial().getData());
                if (rule != null) {
                    if (rule.isBonus())
                        bonusRules.add(rule);
                    else
                        notBonusRules.add(rule);
                    cost += rule.getCost();
                    amount++;
                }
            }
        }
        // 剩余cost点
        panel.setParaOrbitronLarge();
        Color color = cost >= 0 ? highlight : negative;
        Color textColor = Misc.getBasePlayerColor();
        panel.addPara(d_i18n.get("rulepick_cost"), pad, textColor, color, cost + "");
        int maxAmount = difficulty.maxRules;
        int minAmount = difficulty.minRules;
        // 规则数目
        color = (amount >= minAmount && amount <= maxAmount) ? highlight : negative;
        LabelAPI para = panel.addPara(d_i18n.get("rulepick_ruleAmount"), pad, textColor, color, amount + "", maxAmount + "", minAmount + "");
        para.setHighlightColors(color, highlight, highlight);
        panel.setParaFontDefault();

        Collections.sort(bonusRules, new UNGP_RulesManager.UNGP_RuleSorter());
        Collections.sort(notBonusRules, new UNGP_RulesManager.UNGP_RuleSorter());
        List<URule> rules = new ArrayList<>();
        rules.addAll(bonusRules);
        rules.addAll(notBonusRules);

        boolean currentPress = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        if (currentPress) {
            if (!isPressedShift) {
                isPressedShift = true;
                detailedMode = !detailedMode;
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
            }
        } else {
            isPressedShift = false;
        }

        // 最高页数
        final int bonusRuleSize = bonusRules.size();
        final int notBonusRuleSize = notBonusRules.size();
        final int bonusPage = Math.max(0, bonusRuleSize - 1) / 5;
        final int notBonusPage = Math.max(0, notBonusRuleSize - 1) / 5;

        final int maxPage = Math.max(bonusPage, notBonusPage);
        if (rulePage > maxPage) {
            rulePage = maxPage;
        }
        // A键换页
        boolean isAPressedNow = Keyboard.isKeyDown(Keyboard.KEY_A);
        if (isAPressedNow) {
            if (!isAPressed) {
                isAPressed = true;
                if (rulePage > 0) {
                    rulePage--;
                }
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
            }
        } else {
            isAPressed = false;
        }
        boolean isDPressedNow = Keyboard.isKeyDown(Keyboard.KEY_D);
        if (isDPressedNow) {
            if (!isDPressed) {
                isDPressed = true;
                if (rulePage < maxPage) {
                    rulePage++;
                }
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
            }
        } else {
            isDPressed = false;
        }


        Color sectionColor = new Color(40, 40, 40);
        // 详情情况

        if (detailedMode) {
            panel.addSectionHeading(getBonusString(true) + String.format("(%d/%d)", rulePage + 1, maxPage + 1), getBonusColor(true), sectionColor, Alignment.MID, pad);
            for (int i = rulePage * 5; i < (rulePage + 1) * 5; i++) {
                if (i < bonusRuleSize) {
                    URule rule = bonusRules.get(i);
                    rule.addDesc(panel, 0f, "-");
                } else {
                    panel.addPara("----------------", Misc.getGrayColor(), 0f);
                }
            }
            panel.addSectionHeading(getBonusString(false) + String.format("(%d/%d)", rulePage + 1, maxPage + 1), getBonusColor(false), sectionColor, Alignment.MID, pad);
            for (int i = rulePage * 5; i < (rulePage + 1) * 5; i++) {
                if (i < notBonusRuleSize) {
                    URule rule = notBonusRules.get(i);
                    rule.addDesc(panel, 0f, "-");
                } else {
                    panel.addPara("----------------", Misc.getGrayColor(), 0f);
                }
            }
        } else {
            panel.addSectionHeading(getBonusString(true) + String.format("(%d/%d)", rulePage + 1, maxPage + 1), getBonusColor(true), sectionColor, Alignment.MID, pad);
            addRuleSmallIconGroup(panel, bonusRules);
            panel.addSectionHeading(getBonusString(false) + String.format("(%d/%d)", rulePage + 1, maxPage + 1), getBonusColor(false), sectionColor, Alignment.MID, pad);
            addRuleSmallIconGroup(panel, notBonusRules);
        }
        if (!UNGP_SpecialistSettings.rulesMeetCondition(rules, this.difficulty)) {
            panel.setParaOrbitronLarge();
            panel.addPara(d_i18n.get("rulepick_notMeet"), negative, small).setAlignment(Alignment.MID);
            panel.setParaFontDefault();
        }
    }

    private void addRuleSmallIconGroup(TooltipMakerAPI panel, List<URule> rules) {
        for (int i = rulePage * 5; i < (rulePage + 1) * 5; i++) {
            if (i < rules.size()) {
                URule rule = rules.get(i);
                TooltipMakerAPI imageMaker = panel.beginImageWithText(rule.getSpritePath(), 32f);
                imageMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                rule.addShortDesc(imageMaker, 0f);
            } else {
                TooltipMakerAPI imageMaker = panel.beginImageWithText(Global.getSettings().getSpriteName("icons", "rule_default"), 32f);
                imageMaker.addPara("----------------", Misc.getGrayColor(), 0f);
            }
            panel.addImageWithText(2f);
        }
    }

    /**
     * Add cargo picker to dialog.
     *
     * @param baseDialog
     */
    public void showCargoPickerDialog(InteractionDialogAPI baseDialog) {
        float width = Global.getSettings().getScreenWidthPixels() * 0.8f;
        float height = Global.getSettings().getScreenHeightPixels() * 0.8f;
        baseDialog.showCargoPickerDialog(d_i18n.get("rulepick_title"), d_i18n.get("confirm"), d_i18n.get("cancel"), false,
                                         Math.max(280f, width * 0.2f),
                                         width,
                                         height,
                                         UNGP_RulesManager.createRulesCargoBasedOnChallenges(completedChallenges),
                                         this);
    }
}
