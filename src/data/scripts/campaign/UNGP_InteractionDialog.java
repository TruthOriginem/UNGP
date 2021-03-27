package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.inherit.UNGP_InheritData;
import data.scripts.campaign.inherit.UNGP_InheritManager;
import data.scripts.campaign.specialist.rules.UNGP_RulePickListener;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static data.scripts.campaign.UNGP_Settings.d_i18n;

public class UNGP_InteractionDialog implements InteractionDialogPlugin {
    private enum OptionID {
        CHECK_INHERIT_DATA,
        CHECK_RECORD,
        HELP,

        CHOOSE_INHERIT_SLOT_0,
        CHOOSE_INHERIT_SLOT_1,
        CHOOSE_INHERIT_SLOT_2,
        SWITCH_MODE,
        PICK_RULES,
        INHERIT,

        //        START_RECORD,
        CHOOSE_RECORD_SLOT_0,
        CHOOSE_RECORD_SLOT_1,
        CHOOSE_RECORD_SLOT_2,

        BACK_TO_MENU,
        LEAVE
    }

    private InteractionDialogAPI dialog;
    private TextPanelAPI textPanel;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;
    private SectorAPI sector;

    private UNGP_InGameData inGameData;

    private UNGP_InheritData lastInheritData;
    private OptionID choseInheritSlotOptionID = null;

    private UNGP_InheritData toRecordInheritData;
    private boolean isHardMode = false;

    private String inheritCreditsSelector = Misc.genUID();
    private String inheritBPSelector = Misc.genUID();
    private String inheritDifficultySelector = Misc.genUID();
    private boolean lockedDifficultyValue = false;
    private float creditsSelecterValue;
    private float bpSelecterValue;
    private int difficultyValue;
    private List<URule> selectedRules = new ArrayList<>();

    public UNGP_InteractionDialog(UNGP_InGameData inGameData) {
        this.inGameData = inGameData;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        sector = Global.getSector();
        //lastInheritData = UNGP_InheritData.Load();

        toRecordInheritData = UNGP_InheritData.CreateInheritData(inGameData);
        UNGP_InheritManager.LoadAllSlots();
        initMenu();
        dialog.setOptionOnEscape(null, OptionID.LEAVE);
        dialog.setBackgroundDimAmount(0.4f);
        visual.showCustomPanel(400f, 300f, new CustomUIPanelPlugin() {
            private SpriteAPI sprite = Global.getSettings().getSprite("illustrations", "UNGP_logo");

            @Override
            public void positionChanged(PositionAPI position) {

            }

            @Override
            public void renderBelow(float alphaMult) {

            }

            @Override
            public void render(float alphaMult) {
                sprite.setAlphaMult(alphaMult);
                float screenWidth = Global.getSettings().getScreenWidth() / Display.getPixelScaleFactor();
                float screenHeight = Global.getSettings().getScreenHeight() / Display.getPixelScaleFactor();
                float preferX = screenWidth * 0.55f;
                float preferY = screenHeight * 0.5f;
                if (preferX + sprite.getWidth() > screenWidth) {
                    preferX = screenWidth - sprite.getWidth();
                }
                if (preferY + sprite.getHeight() > screenHeight) {
                    preferY = screenHeight - sprite.getHeight();
                }
                sprite.render(preferX, preferY);
            }

            @Override
            public void advance(float amount) {

            }

            @Override
            public void processInput(List<InputEventAPI> events) {

            }
        });
    }

    private void initMenu() {
        textPanel.addPara(d_i18n.get("menu"));
        options.addOption(d_i18n.get("checkInherit"), OptionID.CHECK_INHERIT_DATA);
        options.addOption(d_i18n.get("checkRecord"), OptionID.CHECK_RECORD);
        if (!inGameData.isInherited() && inGameData.isPassedInheritTime()) {
            textPanel.addPara(d_i18n.get("hasPassedTime"), Misc.getNegativeHighlightColor());
        }
        if (!inGameData.couldBeRecorded()) {
            options.setEnabled(OptionID.CHECK_RECORD, false);
            if (inGameData.isRecorded()) {
                textPanel.addPara(d_i18n.get("hasRecorded"), Misc.getNegativeHighlightColor());
            }
            if (!UNGP_Settings.reachMaxLevel()) {
                textPanel.addPara(d_i18n.get("notMaxLevel"), Misc.getNegativeHighlightColor());
            }
        }
        textPanel.addPara(d_i18n.get("inGameData"),
                          Misc.getHighlightColor(),
                          "" + (inGameData.getCurCycle()),
                          Misc.getDGSCredits(toRecordInheritData.inheritCredits),
                          "" + toRecordInheritData.ships.size(),
                          "" + toRecordInheritData.fighters.size(),
                          "" + toRecordInheritData.weapons.size(),
                          "" + toRecordInheritData.hullmods.size(),
                          toRecordInheritData.isHardMode ? "Yes" : "No");
        if (inGameData.isHardMode()) {
            textPanel.addPara(d_i18n.get("hardmodeLevel") + ": %s", Misc.getHighlightColor(), inGameData.getDifficultyLevel() + "");
        }
        options.addOption(d_i18n.get("help"), OptionID.HELP);
        addLeaveButton();
    }

    /**
     * 选择继承槽位
     */
    private void optionSelectedChooseInherit(OptionID option) {
        int slotID = 0;
        switch (option) {
            case CHOOSE_INHERIT_SLOT_0:
                break;
            case CHOOSE_INHERIT_SLOT_1:
                slotID = 1;
                break;
            case CHOOSE_INHERIT_SLOT_2:
                slotID = 2;
                break;
        }
        lastInheritData = UNGP_InheritManager.Get(slotID);
        if (lastInheritData != null) {
            choseInheritSlotOptionID = option;
            if (!lastInheritData.lastPlayerName.equals(UNGP_InheritData.DEFAULT_NAME)) {
                textPanel.addPara(d_i18n.get("inheritName"), Misc.getHighlightColor(), lastInheritData.lastPlayerName);
            }
            textPanel.addPara(d_i18n.get("inheritData"),
                              Misc.getHighlightColor(),
                              "" + (lastInheritData.cycle - 1),
                              Misc.getDGSCredits(lastInheritData.inheritCredits),
                              "" + lastInheritData.ships.size(),
                              "" + lastInheritData.fighters.size(),
                              "" + lastInheritData.weapons.size(),
                              "" + lastInheritData.hullmods.size());

            //如果没有继承过或者没有超过时限
            if (!inGameData.isPassedInheritTime() && !inGameData.isInherited()) {
                options.addSelector(d_i18n.get("inheritCredits"), inheritCreditsSelector,
                                    Misc.getHighlightColor(), 270f, 30f,
                                    0f, 100f, ValueDisplayMode.PERCENT, null);
                options.setSelectorValue(inheritCreditsSelector, creditsSelecterValue);

                options.addSelector(d_i18n.get("inheritBPs"), inheritBPSelector,
                                    Misc.getHighlightColor(), 270f, 30f,
                                    0f, 100f, ValueDisplayMode.PERCENT, null);
                options.setSelectorValue(inheritBPSelector, bpSelecterValue);

                //专家模式
                if (isHardMode) {
                    options.addSelector(d_i18n.get("hardmodeLevel"), inheritDifficultySelector,
                                        Misc.getNegativeHighlightColor(), 270f, 30f,
                                        1, UNGP_SpecialistSettings.getMaxDifficultyLevel(lastInheritData.cycle + 1), ValueDisplayMode.VALUE, null);
                    options.setSelectorValue(inheritDifficultySelector, difficultyValue);
                }
                options.addOption(d_i18n.get("switchHardMode") + "" + (isHardMode ? d_i18n.get("on") : d_i18n.get("off")), OptionID.SWITCH_MODE);
                if (isHardMode) {
                    options.addOption(d_i18n.get("rulepick_button"), OptionID.PICK_RULES);
                    selectedRules.clear();
//                    setHardModeToolTip(difficultyValue);
                }

                options.addOption(d_i18n.get("startInherit"), OptionID.INHERIT);
                changeInheritConfirmationTooltip();
            } else {
                if (inGameData.isInherited()) {
                    textPanel.addPara(d_i18n.get("hasInherited"), Misc.getNegativeHighlightColor());
                } else {
                    textPanel.addPara(d_i18n.get("hasPassedTime"), Misc.getNegativeHighlightColor());
                }
            }
        } else {
            textPanel.addPara(d_i18n.get("noInherit"), Misc.getNegativeHighlightColor());
        }
    }

    private void optionSelectedChooseRecord(UNGP_InheritData dataToRecord, OptionID option) {
        int slotID = 0;
        switch (option) {
            case CHOOSE_RECORD_SLOT_0:
                break;
            case CHOOSE_RECORD_SLOT_1:
                slotID = 1;
                break;
            case CHOOSE_RECORD_SLOT_2:
                slotID = 2;
                break;
        }
        UNGP_InheritManager.Save(dataToRecord, slotID);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        OptionID selectedOption = (OptionID) optionData;
        if (selectedOption != OptionID.PICK_RULES) {
            options.clearOptions();
            textPanel.clear();
        }
        switch (selectedOption) {
            case CHECK_INHERIT_DATA: {
                textPanel.addPara(d_i18n.get("checkInherit"));
                textPanel.addPara(d_i18n.get("checkInheritSlot"));
                UNGP_InheritData curSlot = UNGP_InheritManager.InheritData_slot0;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_INHERIT_SLOT_0);
                    options.setEnabled(OptionID.CHOOSE_INHERIT_SLOT_0, false);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle - 1 + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_INHERIT_SLOT_0);
                }
                curSlot = UNGP_InheritManager.InheritData_slot1;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_INHERIT_SLOT_1);
                    options.setEnabled(OptionID.CHOOSE_INHERIT_SLOT_1, false);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle - 1 + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_INHERIT_SLOT_1);
                }
                curSlot = UNGP_InheritManager.InheritData_slot2;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_INHERIT_SLOT_2);
                    options.setEnabled(OptionID.CHOOSE_INHERIT_SLOT_2, false);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle - 1 + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_INHERIT_SLOT_2);
                }
                addBackButton(OptionID.BACK_TO_MENU);
            }
            break;
            case CHOOSE_INHERIT_SLOT_0:
            case CHOOSE_INHERIT_SLOT_1:
            case CHOOSE_INHERIT_SLOT_2:
                optionSelectedChooseInherit(selectedOption);
                addBackButton(OptionID.CHECK_INHERIT_DATA);
                break;
            case CHECK_RECORD: {
                textPanel.addPara(d_i18n.get("recordInfo"));
                textPanel.addPara(d_i18n.get("inGameData"),
                                  Misc.getHighlightColor(),
                                  "" + inGameData.getCurCycle(),
                                  Misc.getDGSCredits(toRecordInheritData.inheritCredits),
                                  "" + toRecordInheritData.ships.size(),
                                  "" + toRecordInheritData.fighters.size(),
                                  "" + toRecordInheritData.weapons.size(),
                                  "" + toRecordInheritData.hullmods.size(),
                                  toRecordInheritData.isHardMode ? "Yes" : "No");
                textPanel.addPara(d_i18n.get("checkRecordSlot"));
                UNGP_InheritData curSlot = UNGP_InheritManager.InheritData_slot0;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_0);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_0);
                }
                curSlot = UNGP_InheritManager.InheritData_slot1;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_1);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_1);
                }
                curSlot = UNGP_InheritManager.InheritData_slot2;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_2);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_2);
                }
                options.addOptionConfirmation(OptionID.CHOOSE_RECORD_SLOT_0, d_i18n.get("recordConfirmInfo"), d_i18n.get("confirm"), d_i18n.get("cancel"));
                options.addOptionConfirmation(OptionID.CHOOSE_RECORD_SLOT_1, d_i18n.get("recordConfirmInfo"), d_i18n.get("confirm"), d_i18n.get("cancel"));
                options.addOptionConfirmation(OptionID.CHOOSE_RECORD_SLOT_2, d_i18n.get("recordConfirmInfo"), d_i18n.get("confirm"), d_i18n.get("cancel"));
                addBackButton(OptionID.BACK_TO_MENU);
            }
            break;
            case HELP:
                textPanel.addPara(d_i18n.get("helpInfo"));
                addBackButton(OptionID.BACK_TO_MENU);
                break;
            case SWITCH_MODE:
                isHardMode = !isHardMode;
                if (!isHardMode) {
                    selectedRules.clear();
                    lockedDifficultyValue = false;
                }
                optionSelected(null, choseInheritSlotOptionID);
                break;
            case PICK_RULES:
                selectedRules.clear();
                UNGP_RulesManager.setDifficultyLevel(difficultyValue);
                UNGP_RulePickListener pickListener = new UNGP_RulePickListener(selectedRules, difficultyValue, new Script() {
                    @Override
                    public void run() {
                        setHardModeToolTip();
                        textPanel.addPara(d_i18n.get("hardmodeDes"));
                        TooltipMakerAPI tooltip = textPanel.beginTooltip();
                        for (URule rule : selectedRules) {
                            TooltipMakerAPI imageMaker = tooltip.beginImageWithText(rule.getSpritePath(), 32f);
                            imageMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                            rule.addDesc(imageMaker, 0f);
                            tooltip.addImageWithText(3f);
                        }
                        if (!UNGP_SpecialistSettings.rulesMeetCondition(selectedRules, difficultyValue)) {
                            tooltip.addPara(d_i18n.get("rulepick_notMeet"), Misc.getNegativeHighlightColor(), 5f);
                        }
                        textPanel.addTooltip();
                        lockedDifficultyValue = true;
                    }
                }, new Script() {
                    @Override
                    public void run() {
                        optionSelected(null, choseInheritSlotOptionID);
                    }
                });
                dialog.showCargoPickerDialog(d_i18n.get("rulepick_title"), d_i18n.get("confirm"), d_i18n.get("cancel"), false,
                                             280, UNGP_RulesManager.createAllRulesCargo(), pickListener);
                options.setEnabled(inheritDifficultySelector, false);
                break;
            case INHERIT:
                inherit();
                addLeaveButton();
                break;
            case CHOOSE_RECORD_SLOT_0:
            case CHOOSE_RECORD_SLOT_1:
            case CHOOSE_RECORD_SLOT_2:
                inGameData.setRecorded(true);
                optionSelectedChooseRecord(toRecordInheritData, selectedOption);
                Global.getSoundPlayer().playUISound("ui_rep_raise", 1, 1);
                textPanel.addPara(d_i18n.get("recordSuccess"));
                addLeaveButton();
                break;
            case BACK_TO_MENU:
                initMenu();
                break;
            case LEAVE:
                UNGP_InheritManager.ClearSlots();
                dialog.dismiss();
                break;
            default:
                break;
        }

    }

    /**
     * 继承重生点
     */
    private void inherit() {
        int creditsInherited = (int) (lastInheritData.inheritCredits * creditsSelecterValue * 0.01f);
        sector.getPlayerFleet().getCargo().getCredits().add(creditsInherited);
        AddRemoveCommodity.addCreditsGainText(creditsInherited, textPanel);
        FactionAPI player = sector.getPlayerFaction();
        float inheritBPPercent = bpSelecterValue * 0.01f;
        int shipAmount = (int) (inheritBPPercent * lastInheritData.ships.size());
        int fighterAmount = (int) (inheritBPPercent * lastInheritData.fighters.size());
        int weaponAmount = (int) (inheritBPPercent * lastInheritData.weapons.size());
        int hullmodAmount = (int) (inheritBPPercent * lastInheritData.hullmods.size());
        List<String> curShipBps = new ArrayList<>();
        List<String> curFighterBps = new ArrayList<>();
        List<String> curWeaponBps = new ArrayList<>();
        List<String> curHullmodBps = new ArrayList<>();
        SettingsAPI setting = Global.getSettings();

        for (ShipHullSpecAPI spec : setting.getAllShipHullSpecs()) {
            curShipBps.add(spec.getHullId());
        }
        for (FighterWingSpecAPI spec : setting.getAllFighterWingSpecs()) {
            curFighterBps.add(spec.getId());
        }
        for (WeaponSpecAPI spec : setting.getAllWeaponSpecs()) {
            curWeaponBps.add(spec.getWeaponId());
        }
        for (HullModSpecAPI spec : setting.getAllHullModSpecs()) {
            curHullmodBps.add(spec.getId());
        }


        int inheritedShip = 0;
        int inheritedFighter = 0;
        int inheritedWeapon = 0;
        int inheritedHullmod = 0;

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.ships) {
            if (curShipBps.contains(bp) && !player.knowsShip(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < shipAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownShip(bpId, true);
            inheritedShip++;
        }
        picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.fighters) {
            if (curFighterBps.contains(bp) && !player.knowsFighter(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < fighterAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownFighter(bpId, true);
            inheritedFighter++;
        }
        picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.weapons) {
            if (curWeaponBps.contains(bp) && !player.knowsWeapon(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < weaponAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownWeapon(bpId, true);
            inheritedWeapon++;
        }
        picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.hullmods) {
            if (curHullmodBps.contains(bp) && !player.knowsHullMod(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < hullmodAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownHullMod(bpId);
            inheritedHullmod++;
        }

        textPanel.setFontSmallInsignia();
        textPanel.addPara(d_i18n.get("inheritedBP"), Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                          "" + inheritedShip,
                          "" + inheritedFighter,
                          "" + inheritedWeapon,
                          "" + inheritedHullmod);

        // Add points: skill points/story points
        int addSkillPoints = (int) Math.sqrt(lastInheritData.cycle - 1);
        int addStoryPoints = addSkillPoints * 2;
        textPanel.addPara(d_i18n.get("inheritedPoints"), Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                          addSkillPoints + "");
        sector.getPlayerStats().addPoints(addSkillPoints);
        sector.getPlayerStats().addStoryPoints(addStoryPoints, textPanel, true);


        textPanel.setFontInsignia();

        if (isHardMode)
            textPanel.addPara(d_i18n.get("hardModeYes"), Misc.getNegativeHighlightColor());


        Global.getSoundPlayer().playUISound("ui_acquired_blueprint", 1, 1);

        inGameData.setCurCycle(lastInheritData.cycle);
        inGameData.setInherited(true);
        inGameData.setHardMode(isHardMode);
        if (isHardMode) {
            inGameData.setDifficultyLevel(difficultyValue);
            inGameData.saveActivatedRules(selectedRules);
            UNGP_SpecialistIntel intel = UNGP_SpecialistIntel.getInstance();
            Global.getSector().getIntelManager().addIntelToTextPanel(intel, textPanel);
            UNGP_RulesManager.updateRulesCache();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    private void setHardModeToolTip() {
//        String enemyDamageMultiplier = String.format("%.2f", inGameData.getEnemyDamageMultiplier(difficultyValue));
//        String enemyDamageTakenMultiplier = String.format("%.2f", inGameData.getEnemyDamageTakenMultiplier(difficultyValue));
        if (options.hasOption(OptionID.INHERIT)) {
            if (!selectedRules.isEmpty()) {
                String[] ruleNames = new String[selectedRules.size()];
                Color[] ruleColors = new Color[selectedRules.size()];
                StringBuilder result = new StringBuilder(d_i18n.get("hardmodeDes"));
                for (int i = 0; i < selectedRules.size(); i++) {
                    URule rule = selectedRules.get(i);
                    result.append("\n  ");
                    result.append(rule.getName());
                    ruleNames[i] = rule.getName();
                    ruleColors[i] = rule.getCorrectColor();
                }
                options.setTooltip(OptionID.INHERIT, result.toString());
                options.setTooltipHighlights(OptionID.INHERIT, ruleNames);
                options.setTooltipHighlightColors(OptionID.INHERIT, ruleColors);
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (options.hasSelector(inheritCreditsSelector)) {
            float newValue = options.getSelectorValue(inheritCreditsSelector);
            if (newValue != creditsSelecterValue) {
                creditsSelecterValue = options.getSelectorValue(inheritCreditsSelector);
                changeInheritConfirmationTooltip();
            }
        }
        if (options.hasSelector(inheritBPSelector)) {
            float newValue = options.getSelectorValue(inheritBPSelector);
            if (newValue != bpSelecterValue) {
                bpSelecterValue = options.getSelectorValue(inheritBPSelector);
                changeInheritConfirmationTooltip();
            }
        }
        if (options.hasSelector(inheritDifficultySelector)) {
            if (!lockedDifficultyValue) {
                int newDifficultyValue = Math.round(options.getSelectorValue(inheritDifficultySelector));
                if (newDifficultyValue != difficultyValue) {
                    difficultyValue = Math.round(options.getSelectorValue(inheritDifficultySelector));
//                setHardModeToolTip(difficultyValue);
                    changeInheritConfirmationTooltip();
                }
            } else {
                options.setSelectorValue(inheritDifficultySelector, difficultyValue);
            }
        }
        if (isHardMode && options.hasOption(OptionID.INHERIT)) {
            options.setEnabled(OptionID.INHERIT, UNGP_SpecialistSettings.rulesMeetCondition(selectedRules, difficultyValue));
        }
    }

    private void changeInheritConfirmationTooltip() {
        if (options.hasOption(OptionID.INHERIT)) {
            int creditsInherited = (int) (lastInheritData.inheritCredits * creditsSelecterValue * 0.01f);
            int bpInherited = (int) ((lastInheritData.ships.size() + lastInheritData.fighters.size() + lastInheritData.weapons.size() + lastInheritData.hullmods.size()) * bpSelecterValue * 0.01f);
            String confirmText = d_i18n.format("inheritConfirmInfo0", "" + creditsInherited, "" + bpInherited);
            if (isHardMode) {
                confirmText += d_i18n.get("inheritConfirmInfo1");
            }
            options.addOptionConfirmation(OptionID.INHERIT, confirmText, d_i18n.get("confirm"), d_i18n.get("cancel"));
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    private void addLeaveButton() {
        options.addOption(d_i18n.get("leave"), OptionID.LEAVE);
    }

    private void addBackButton(OptionID warpOption) {
        options.addOption(d_i18n.get("back"), warpOption);
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
