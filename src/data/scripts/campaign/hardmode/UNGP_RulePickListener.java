package data.scripts.campaign.hardmode;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoPickerListener;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static data.scripts.campaign.UNGP_Settings.d_i18n;
import static data.scripts.campaign.hardmode.UNGP_RulesManager.*;

public class UNGP_RulePickListener implements CargoPickerListener {
    private List<URule> pickedRules;
    private Script onPicked = null;
    private Script onCancelled = null;
    private int difficultyValue;
    private boolean isPressedShift = false;
    private boolean switchMode = false;

    public UNGP_RulePickListener(List<URule> pickedRules, int difficulty, Script onPicked, Script onCancelled) {
        this.pickedRules = pickedRules;
        this.onPicked = onPicked;
        this.onCancelled = onCancelled;
        this.difficultyValue = difficulty;
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
        Collections.sort(pickedRules, new UNGP_RuleSorter());
        if (onPicked != null)
            onPicked.run();
    }

    @Override
    public void cancelledCargoSelection() {
        if (onCancelled != null)
            onCancelled.run();
    }

    @Override
    public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {
        float pad = 3f;
        float small = 5f;
        float opad = 10f;
        Color highlight = Misc.getHighlightColor();
        Color negative = Misc.getNegativeHighlightColor();
        TooltipMakerAPI imageTooltip = panel.beginImageWithText(UNGP_SpecialistSettings.getSpecialistModeIconPath(), 64f);
        imageTooltip.setParaOrbitronLarge();
        imageTooltip.addPara(d_i18n.get("rulepick_level"), opad, highlight, difficultyValue + "");
        imageTooltip.setParaFontDefault();
        panel.addImageWithText(0);

        panel.addPara(d_i18n.get("rulepick_desc"), opad);
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
        panel.setParaOrbitronLarge();
        Color color = cost >= 0 ? highlight : negative;
        panel.addPara(d_i18n.get("rulepick_cost"), pad, color, cost + "");
        int maxAmount = UNGP_SpecialistSettings.getMaxRulesAmount(difficultyValue);
        color = amount <= maxAmount ? highlight : negative;
        panel.addPara(d_i18n.get("rulepick_ruleAmount"), pad, color, amount + "", maxAmount + "");
        panel.setParaFontDefault();

        Collections.sort(bonusRules, new UNGP_RuleSorter());
        Collections.sort(notBonusRules, new UNGP_RuleSorter());
        List<URule> rules = new ArrayList<>();
        rules.addAll(bonusRules);
        rules.addAll(notBonusRules);

        boolean currentPress = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        if (currentPress) {
            if (!isPressedShift) {
                isPressedShift = true;
                switchMode = !switchMode;
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
            }
        } else {
            isPressedShift = false;
        }

        int shownSkill = 0;
        Color sectionColor = new Color(40, 40, 40);
        if (switchMode) {
            if (!bonusRules.isEmpty()) {
                panel.addSectionHeading(getBonusString(true), getBonusColor(true), sectionColor, Alignment.MID, pad);
                for (URule rule : bonusRules) {
                    if (shownSkill >= 10) continue;
                    rule.addDesc(panel, 0f, "-");
                    shownSkill++;
                }
            }
            if (shownSkill < 9) {
                if (!notBonusRules.isEmpty()) {
                    panel.addSectionHeading(getBonusString(false), getBonusColor(false), sectionColor, Alignment.MID, pad);
                    for (URule rule : notBonusRules) {
                        if (shownSkill >= 10) continue;
                        rule.addDesc(panel, 0f, "-");
                        shownSkill++;
                    }
                }
            }
        } else {
            if (!bonusRules.isEmpty()) {
//            panel.addPara(getBonusString(true), getBonusColor(true), opad);
                panel.addSectionHeading(getBonusString(true), getBonusColor(true), sectionColor, Alignment.MID, pad);
                for (URule rule : bonusRules) {
                    if (shownSkill >= 10) continue;
                    TooltipMakerAPI imageMaker = panel.beginImageWithText(rule.getSpritePath(), 32f);
                    imageMaker.addPara(rule.getName(), rule.getBorderColor(), 0f);
                    rule.addShortDesc(imageMaker, 0f);
                    panel.addImageWithText(2f);
                    shownSkill++;
                }
            }
            if (shownSkill < 9) {
                if (!notBonusRules.isEmpty()) {
//                panel.addPara(getBonusString(false), getBonusColor(false), pad);
                    panel.addSectionHeading(getBonusString(false), getBonusColor(false), sectionColor, Alignment.MID, pad);
                    for (URule rule : notBonusRules) {
                        if (shownSkill >= 10) continue;
                        TooltipMakerAPI imageMaker = panel.beginImageWithText(rule.getSpritePath(), 32f);
                        imageMaker.addPara(rule.getName(), rule.getBorderColor(), 0f);
                        rule.addShortDesc(imageMaker, 0f);
                        panel.addImageWithText(2f);
                        shownSkill++;
                    }
                }
            }
        }

        if (!UNGP_SpecialistSettings.rulesMeetCondition(rules, difficultyValue)) {
            panel.setParaOrbitronLarge();
            panel.addPara(d_i18n.get("rulepick_notMeet"), negative, small).setAlignment(Alignment.MID);
            panel.setParaFontDefault();
        }
    }
}
