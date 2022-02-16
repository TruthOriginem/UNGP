package data.scripts.campaign.specialist.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.UNGP_Settings;
import data.scripts.campaign.inherit.UNGP_InheritData;
import data.scripts.campaign.inherit.UNGP_InheritManager;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.intel.UNGP_ChallengeIntel;
import data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import data.scripts.campaign.specialist.rules.UNGP_RulePickListener;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import data.scripts.utils.UNGP_Feedback;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import ungp.ui.CheckBoxGroup;
import ungp.ui.HorizontalButtonGroup;
import ungp.ui.SettingEntry;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static data.scripts.campaign.UNGP_Settings.d_i18n;
import static data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static data.scripts.campaign.specialist.UNGP_SpecialistSettings.rulesMeetCondition;

public class UNGP_InteractionDialog implements InteractionDialogPlugin {
    private enum OptionID {
        CHECK_INHERIT_DATA,
        CHECK_RECORD,
        HELP,

        CHOOSE_INHERIT_SLOT_0,
        CHOOSE_INHERIT_SLOT_1,
        CHOOSE_INHERIT_SLOT_2,
        PICK_RULES,
        INHERIT,
        INHERIT_SETTINGS,

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
    private boolean isSpecialistMode = false;

    // inherit setting entry
    private SettingEntry<Float> setting_inheritCreditsRatio = new SettingEntry<>(0f);
    private SettingEntry<Float> setting_inheritBPsRatio = new SettingEntry<>(0f);
    private SettingEntry<Difficulty> setting_difficulty = new SettingEntry<>(null);

    private List<URule> pickedRules = new ArrayList<>();


    public UNGP_InteractionDialog(UNGP_InGameData inGameData) {
        this.inGameData = inGameData;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(0.4f);
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        sector = Global.getSector();
        //lastInheritData = UNGP_InheritData.Load();

        toRecordInheritData = UNGP_InheritData.createInheritData(inGameData);
        UNGP_InheritManager.loadAllSlots();
        initMenu();
        dialog.setOptionOnEscape(null, OptionID.LEAVE);
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
        TooltipMakerAPI toRecordInfo = textPanel.beginTooltip();
        Difficulty difficulty = null;
        if (inGameData.isHardMode()) {
            difficulty = inGameData.getDifficulty();
        }
        toRecordInheritData.addRecordTooltip(toRecordInfo, difficulty);
        textPanel.addTooltip();
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
        lastInheritData = UNGP_InheritManager.getDataFromSlot(slotID);
        Color negativeHighlightColor = Misc.getNegativeHighlightColor();
        if (lastInheritData != null) {
            choseInheritSlotOptionID = option;
            TooltipMakerAPI inheritDataInfo = textPanel.beginTooltip();
            lastInheritData.addInheritTooltip(inheritDataInfo);
            textPanel.addTooltip();

            //如果没有继承过或者没有超过时限
            if (!inGameData.isPassedInheritTime() && !inGameData.isInherited()) {

                // 继承选项
                String settingOptionStr = d_i18n.get("startSetting");
                options.addOption(settingOptionStr, OptionID.INHERIT_SETTINGS);
                options.setShortcut(OptionID.INHERIT_SETTINGS, Keyboard.KEY_S, false, false, false, true);

                if (isSpecialistMode) {
                    options.addOption(d_i18n.get("rulepick_button") + (UNGP_ChallengeManager.isDifficultyEnough(setting_difficulty.get()) ?
                                                                       d_i18n.get("rulepick_couldChallenge") : ""), OptionID.PICK_RULES);
                    options.setShortcut(OptionID.PICK_RULES, Keyboard.KEY_R, false, false, false, true);
                    pickedRules.clear();
                }

                options.addOption(d_i18n.get("startInherit"), OptionID.INHERIT);
                options.setShortcut(OptionID.INHERIT, Keyboard.KEY_SPACE, false, false, false, true);
                updateOptionsFromSettings();
            } else {
                if (inGameData.isInherited()) {
                    textPanel.addPara(d_i18n.get("hasInherited"), negativeHighlightColor);
                } else {
                    textPanel.addPara(d_i18n.get("hasPassedTime"), negativeHighlightColor);
                }
            }
        } else {
            textPanel.addPara(d_i18n.get("noInherit"), negativeHighlightColor);
        }
    }

    private void saveRecordByChosenOption(UNGP_InheritData dataToRecord, OptionID option) {
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
        UNGP_InheritManager.saveDataToSlot(dataToRecord, slotID);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        final OptionID selectedOption = (OptionID) optionData;
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
                resetSettings();
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
                TooltipMakerAPI recordInfo = textPanel.beginTooltip();
                textPanel.addPara(d_i18n.get("recordInfo"));
                toRecordInheritData.addRecordTooltip(recordInfo, inGameData.getDifficulty());
                textPanel.addTooltip();
                textPanel.addPara(d_i18n.get("checkRecordSlot"));
                // 三个重生槽位
                UNGP_InheritData curSlot = UNGP_InheritManager.InheritData_slot0;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_0);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle - 1 + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_0);
                }
                curSlot = UNGP_InheritManager.InheritData_slot1;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_1);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle - 1 + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_1);
                }
                curSlot = UNGP_InheritManager.InheritData_slot2;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_2);
                } else {
                    options.addOption(d_i18n.format("slotDes", curSlot.cycle - 1 + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_2);
                }
//                options.addOptionConfirmation(OptionID.CHOOSE_RECORD_SLOT_0, d_i18n.get("recordConfirmInfo"), d_i18n.get("confirm"), d_i18n.get("cancel"));
//                options.addOptionConfirmation(OptionID.CHOOSE_RECORD_SLOT_1, d_i18n.get("recordConfirmInfo"), d_i18n.get("confirm"), d_i18n.get("cancel"));
//                options.addOptionConfirmation(OptionID.CHOOSE_RECORD_SLOT_2, d_i18n.get("recordConfirmInfo"), d_i18n.get("confirm"), d_i18n.get("cancel"));
                addBackButton(OptionID.BACK_TO_MENU);
            }
            break;
            case HELP:
                textPanel.addPara(d_i18n.get("helpInfo"));
                addBackButton(OptionID.BACK_TO_MENU);
                break;
            case PICK_RULES:
                final List<URule> oldList = new ArrayList<>(pickedRules);
                pickedRules.clear();
                final Difficulty difficulty = setting_difficulty.get();
                UNGP_RulesManager.setStaticDifficulty(difficulty);
                UNGP_RulePickListener pickListener = new UNGP_RulePickListener(pickedRules,
                                                                               lastInheritData.completedChallenges,
                                                                               difficulty, new Script() {
                    @Override
                    public void run() {
                        setSpecialistModeToolTip();
                        textPanel.addPara(d_i18n.get("hardmodeDes"));
                        TooltipMakerAPI tooltip = textPanel.beginTooltip();
                        for (URule rule : pickedRules) {
                            TooltipMakerAPI imageMaker = tooltip.beginImageWithText(rule.getSpritePath(), 32f);
                            imageMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                            rule.addDesc(imageMaker, 0f);
                            tooltip.addImageWithText(3f);
                        }
                        // 如果满足规则
                        if (!rulesMeetCondition(pickedRules, difficulty)) {
                            tooltip.addPara(d_i18n.get("rulepick_notMeet"), Misc.getNegativeHighlightColor(), 5f);
                        } else {
                            List<UNGP_ChallengeInfo> runnableChallenges = UNGP_ChallengeManager.getRunnableChallenges(difficulty, pickedRules, lastInheritData.completedChallenges);
                            if (!runnableChallenges.isEmpty()) {
                                tooltip.addPara(d_i18n.get("rulepick_runnableChallenges"), 10f);
                                for (UNGP_ChallengeInfo challenge : runnableChallenges) {
                                    challenge.createTooltip(tooltip, 5f, 0);
                                }
                            }
                        }
                        textPanel.addTooltip();
                    }
                }, new Script() {
                    @Override
                    public void run() {
                        pickedRules.addAll(oldList);
                    }
                });
                pickListener.showCargoPickerDialog(dialog);
                break;
            case INHERIT:
                inherit();
                addLeaveButton();
                break;
            case CHOOSE_RECORD_SLOT_0:
            case CHOOSE_RECORD_SLOT_1:
            case CHOOSE_RECORD_SLOT_2:
                dialog.showCustomDialog(720f, 160f, new RecordDialogDelegate(selectedOption));
                addLeaveButton();
                break;
            case BACK_TO_MENU:
                initMenu();
                break;
            case LEAVE:
                UNGP_InheritManager.clearSlots();
                dialog.dismiss();
                break;
            case INHERIT_SETTINGS:
                dialog.showCustomDialog(720f, 300f, new CustomConfirmDialog());
                break;
            default:
                break;
        }

    }

    /**
     * 继承重生点
     */
    private void inherit() {
        int creditsInherited = (int) (lastInheritData.inheritCredits * setting_inheritCreditsRatio.get());
        sector.getPlayerFleet().getCargo().getCredits().add(creditsInherited);
        AddRemoveCommodity.addCreditsGainText(creditsInherited, textPanel);
        FactionAPI player = sector.getPlayerFaction();
        float inheritBPPercent = setting_inheritBPsRatio.get();
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
        int addSkillPoints = UNGP_Settings.getBonusSkillPoints(lastInheritData.cycle);
        int addStoryPoints = UNGP_Settings.getBonusStoryPoints(lastInheritData.cycle);
        textPanel.addPara(d_i18n.get("inheritedPoints"), Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                          addSkillPoints + "");
        sector.getPlayerStats().addPoints(addSkillPoints);
        sector.getPlayerStats().addStoryPoints(addStoryPoints, textPanel, true);


        textPanel.setFontInsignia();

        if (isSpecialistMode)
            textPanel.addPara(d_i18n.get("hardModeYes"), Misc.getNegativeHighlightColor());

        inGameData.setCurCycle(lastInheritData.cycle);
        inGameData.setInherited(true);
        inGameData.setHardMode(isSpecialistMode);
        inGameData.setCompletedChallenges(lastInheritData.completedChallenges);
        if (isSpecialistMode) {
            inGameData.setDifficulty(setting_difficulty.get());
            inGameData.saveActivatedRules(pickedRules);
            UNGP_Feedback.setFeedBackList(pickedRules);
            UNGP_SpecialistIntel intel = new UNGP_SpecialistIntel();
            Global.getSector().getIntelManager().addIntel(intel, false, textPanel);
            UNGP_ChallengeIntel challengeIntel = UNGP_ChallengeManager.confirmChallenges(inGameData);
            if (challengeIntel != null) {
                Global.getSector().getIntelManager().addIntelToTextPanel(challengeIntel, textPanel);
            }
            UNGP_RulesManager.updateRulesCache();
        }
    }

    /**
     * 记录重生点
     *
     * @param option
     */
    private void record(OptionID option) {
        inGameData.setRecorded(true);
        saveRecordByChosenOption(toRecordInheritData, option);
        Global.getSoundPlayer().playUISound("ui_rep_raise", 1, 1);
        textPanel.addPara(d_i18n.get("recordSuccess"));
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    private void setSpecialistModeToolTip() {
        if (options.hasOption(OptionID.INHERIT)) {
            if (!pickedRules.isEmpty()) {
                String[] ruleNames = new String[pickedRules.size()];
                Color[] ruleColors = new Color[pickedRules.size()];
                StringBuilder result = new StringBuilder(d_i18n.get("hardmodeDes"));
                for (int i = 0; i < pickedRules.size(); i++) {
                    URule rule = pickedRules.get(i);
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
        if (isSpecialistMode && options.hasOption(OptionID.INHERIT)) {
            options.setEnabled(OptionID.INHERIT, rulesMeetCondition(pickedRules, setting_difficulty.get()));
        }
    }

    /**
     * 继承
     */
    private void updateOptionsFromSettings() {
        if (options.hasOption(OptionID.INHERIT)) {
            final int creditsInherited = (int) (lastInheritData.inheritCredits * setting_inheritCreditsRatio.get());
            final int bpInherited = (int) ((lastInheritData.ships.size() +
                    lastInheritData.fighters.size() +
                    lastInheritData.weapons.size() +
                    lastInheritData.hullmods.size())
                    * setting_inheritBPsRatio.get());
            final int addSkillPoints = UNGP_Settings.getBonusSkillPoints(lastInheritData.cycle);
            final int addStoryPoints = UNGP_Settings.getBonusStoryPoints(lastInheritData.cycle);
            final boolean isHd = isSpecialistMode;
            final Difficulty difficulty = setting_difficulty.get();

            textPanel.addPara(d_i18n.get("inheritCredits") + ": %s", Misc.getHighlightColor(), (int) (setting_inheritCreditsRatio.get() * 100f) + "%");
            textPanel.addPara(d_i18n.get("inheritBPs") + ": %s", Misc.getPositiveHighlightColor(), (int) (setting_inheritBPsRatio.get() * 100f) + "%");
            if (difficulty != null) {
                textPanel.addPara(d_i18n.get("hardmodeLevel") + ": %s", difficulty.color, difficulty.name);
            }
            // 确认
            options.addOptionConfirmation(OptionID.INHERIT, new CustomStoryDialogDelegate() {
                @Override
                public String getTitle() {
                    return d_i18n.get("startInherit");
                }

                @Override
                public void createDescription(TooltipMakerAPI info) {
                    float pad = 10f;
                    String credits = Misc.getDGSCredits(creditsInherited);
                    Color hl = Misc.getHighlightColor();
                    Color negative = Misc.getNegativeHighlightColor();
                    info.addPara(d_i18n.get("inheritConfirmInfo0"), 0f, hl, credits, "" + bpInherited);
                    info.addSpacer(pad);
                    info.addPara(d_i18n.get("inheritConfirmTip_p1"), 0f, Misc.getBasePlayerColor(), hl, addSkillPoints + "");
                    info.addPara(d_i18n.get("inheritConfirmTip_p2"), 0f, Misc.getStoryOptionColor(), hl, addStoryPoints + "");
                    if (difficulty != null && isHd) {
                        info.addPara(d_i18n.get("inheritConfirmInfo1"), negative, 0f);
                        info.addSectionHeading(d_i18n.format("rulepick_level", difficulty.name), hl, Misc.scaleAlpha(negative, 0.2f), Alignment.MID, pad * 0.5f);
                        float width = info.getPrev().getPosition().getWidth();
                        int ruleSize = pickedRules.size();
                        int itemsPerRow = (int) (width / 64f);
                        int page = Math.max(0, ruleSize - 1) / itemsPerRow;

                        for (int i = 0; i <= page; i++) {
                            List<String> ruleSprites = new ArrayList<>();
                            for (int j = i * itemsPerRow; j < (i + 1) * itemsPerRow; j++) {
                                if (j < ruleSize) {
                                    ruleSprites.add(pickedRules.get(j).getSpritePath());
                                }
                            }
                            if (!ruleSprites.isEmpty()) {
                                String[] array = ruleSprites.toArray(new String[0]);
                                info.addImages(width, 64f, 0f, 4f, array);
                            }
                        }
                    }
                }
            });
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

    public void resetSettings() {
        setting_difficulty.reset();
        setting_inheritBPsRatio.reset();
        setting_inheritCreditsRatio.reset();
    }

    private class CustomConfirmDialog implements CustomDialogDelegate {

        CheckBoxGroup creditsGroup = new CheckBoxGroup();
        CheckBoxGroup bpsGroup = new CheckBoxGroup();
        CheckBoxGroup difficultyGroup = new CheckBoxGroup();

        @Override
        public void createCustomDialog(CustomPanelAPI panel) {
            creditsGroup.clear();
            bpsGroup.clear();
            difficultyGroup.clear();
            float width = panel.getPosition().getWidth();
            float height = panel.getPosition().getHeight();
            float pad = 5f;

            TooltipMakerAPI tooltip = panel.createUIElement(width, height, true);
            panel.addUIElement(tooltip);

//            tooltip.setForceProcessInput(true);
            tooltip.setParaOrbitronLarge();
            tooltip.setAreaCheckboxFont(Fonts.ORBITRON_24AA);
            tooltip.addPara(d_i18n.get("inheritCredits"), Misc.getHighlightColor(), 0f);
            tooltip.addSpacer(pad);
            float buttonHeight = 30f;
            final float buttonWidth = width / pad - 10f;
            {
                HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
                for (int i = 0; i < 5; i++) {
                    float percentage = Math.min(1, i * 0.25f);
                    ButtonAPI checkBox = tooltip.addAreaCheckbox((int) (percentage * 100f) + "%", null, Misc.getBasePlayerColor(),
                                                                 Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                                 buttonWidth, buttonHeight, 0f);
                    buttonGroup.addButton(checkBox);
                    creditsGroup.addCheckBox(checkBox, percentage);
                }
                buttonGroup.updateTooltip(tooltip, pad);
            }
            tooltip.addSpacer(pad);
            tooltip.addPara(d_i18n.get("inheritBPs"), Misc.getPositiveHighlightColor(), 0f);
            tooltip.addSpacer(pad);
            {
                HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
                for (int i = 0; i < 5; i++) {
                    float percentage = Math.min(1, i * 0.25f);
                    ButtonAPI checkBox = tooltip.addAreaCheckbox((int) (percentage * 100f) + "%", null, Misc.getBasePlayerColor(),
                                                                 Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                                 buttonWidth, buttonHeight, 0f);
                    buttonGroup.addButton(checkBox);
                    bpsGroup.addCheckBox(checkBox, percentage);
                }
                buttonGroup.updateTooltip(tooltip, pad);
            }
            tooltip.addSpacer(40f);
            tooltip.addPara(d_i18n.get("hardmodeLevel"), Misc.getNegativeHighlightColor(), 0f);
            tooltip.addSpacer(pad);
            {
                HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
                Difficulty[] difficulties = Difficulty.values();
                for (int i = 0; i < difficulties.length + 1; i++) {
                    ButtonAPI checkBox;
                    Difficulty difficulty;
                    if (i == 0) {
                        difficulty = null;
                        checkBox = tooltip.addAreaCheckbox("/", null, Misc.getBasePlayerColor(),
                                                           Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                           buttonWidth, buttonHeight, 0f);
                    } else {
                        difficulty = difficulties[i - 1];
                        checkBox = tooltip.addAreaCheckbox(difficulty.name, null, difficulty.color,
                                                           difficulty.color.darker(), difficulty.color.brighter(),
                                                           buttonWidth, buttonHeight, 0f);
                    }
                    tooltip.addTooltipToPrevious(new DifficultyTooltipCreator(difficulty), TooltipMakerAPI.TooltipLocation.BELOW);
                    buttonGroup.addButton(checkBox);
                    difficultyGroup.addCheckBox(checkBox, difficulty);
                }
                buttonGroup.updateTooltip(tooltip, pad);
            }
            creditsGroup.tryCheckValue(setting_inheritCreditsRatio.get());
            bpsGroup.tryCheckValue(setting_inheritBPsRatio.get());
            difficultyGroup.tryCheckValue(setting_difficulty.get());
        }

        private class DifficultyTooltipCreator implements TooltipMakerAPI.TooltipCreator {
            private Difficulty difficulty;

            public DifficultyTooltipCreator(Difficulty difficulty) {
                this.difficulty = difficulty;
            }

            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return 200f;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                if (difficulty == null) {
                    tooltip.addPara(d_i18n.get("difficulty_desc_null"), 0);
                } else {
                    Color hl = Misc.getHighlightColor();
                    tooltip.addPara(d_i18n.get("difficulty_desc_base") + "%s / %s", 0f, hl,
                                    difficulty.minRules + "",
                                    difficulty.maxRules + "");
                    tooltip.addPara(d_i18n.get("difficulty_desc_value") + "%s", 0f, hl,
                                    (int) (difficulty.extraValueMultiplier * 100f) + "%");
                    if (UNGP_ChallengeManager.isDifficultyEnough(difficulty)) {
                        tooltip.addPara(d_i18n.get("difficulty_desc_max"), hl, 5f);
                    }
                }
            }
        }

        @Override
        public boolean hasCancelButton() {
            return true;
        }

        @Override
        public String getConfirmText() {
            return null;
        }

        @Override
        public String getCancelText() {
            return null;
        }

        @Override
        public void customDialogConfirm() {
            setting_inheritCreditsRatio.set((Float) creditsGroup.getCheckedValue());
            setting_inheritBPsRatio.set((Float) bpsGroup.getCheckedValue());
            Difficulty difficulty = (Difficulty) difficultyGroup.getCheckedValue();
            setting_difficulty.set(difficulty);
            isSpecialistMode = difficulty != null;
            optionSelected(null, choseInheritSlotOptionID);
        }

        @Override
        public void customDialogCancel() {
            optionSelected(null, choseInheritSlotOptionID);
        }

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            return new CustomUIPanelPlugin() {
                @Override
                public void positionChanged(PositionAPI position) {

                }

                @Override
                public void renderBelow(float alphaMult) {

                }

                @Override
                public void render(float alphaMult) {

                }

                @Override
                public void advance(float amount) {
                    creditsGroup.updateCheck();
                    bpsGroup.updateCheck();
                    difficultyGroup.updateCheck();
                }

                @Override
                public void processInput(List<InputEventAPI> events) {

                }
            };
        }
    }

    private class CustomStoryDialogDelegate extends BaseStoryPointActionDelegate {
        @Override
        public boolean withDescription() {
            return true;
        }

        @Override
        public boolean withSPInfo() {
            return false;
        }

        @Override
        public String getLogText() {
            return null;
        }

        @Override
        public float getBonusXPFraction() {
            return 0;
        }

        @Override
        public TextPanelAPI getTextPanel() {
            if (dialog == null) return null;
            return textPanel;
        }

        @Override
        public String getConfirmSoundId() {
            return "ui_acquired_blueprint";
        }

        @Override
        public int getRequiredStoryPoints() {
            return 0;
        }
    }

    private class RecordDialogDelegate implements CustomDialogDelegate {
        private ButtonAPI btn_recordCargo;
        private ButtonAPI btn_recordShip;
        private ButtonAPI btn_recordColony;

        private OptionID selectedOption;

        public RecordDialogDelegate(OptionID selectedOption) {
            this.selectedOption = selectedOption;
        }

        @Override
        public void createCustomDialog(CustomPanelAPI panel) {
            float width = panel.getPosition().getWidth();
            float height = panel.getPosition().getHeight();
            float pad = 5f;

            TooltipMakerAPI info = panel.createUIElement(width, height, true);
            panel.addUIElement(info);
            info.setParaOrbitronLarge();
            info.addPara(d_i18n.get("recordConfirmInfo"), Misc.getNegativeHighlightColor(), 0f);
            info.addSpacer(30f);
            info.setAreaCheckboxFont(Fonts.ORBITRON_24AA);
            info.addPara(d_i18n.get("recordExtraCreditsTitle"), Misc.getHighlightColor(), 0f);
            info.addSpacer(pad);
            float buttonWidth = width / 3f - 10f;
            float buttonHeight = 30f;
            HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
            btn_recordCargo = info.addAreaCheckbox(d_i18n.get("recordExtraCredits_cargo"), null, Misc.getBasePlayerColor(),
                                                   Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), buttonWidth, buttonHeight, 0f);
            btn_recordShip = info.addAreaCheckbox(d_i18n.get("recordExtraCredits_ship"), null, Misc.getBasePlayerColor(),
                                                  Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), buttonWidth, buttonHeight, 0f);
            btn_recordColony = info.addAreaCheckbox(d_i18n.get("recordExtraCredits_colony"), null, Misc.getBasePlayerColor(),
                                                    Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), buttonWidth, buttonHeight, 0f);
            buttonGroup.addButton(btn_recordCargo);
            buttonGroup.addButton(btn_recordShip);
            buttonGroup.addButton(btn_recordColony);

            buttonGroup.updateTooltip(info, 10f);
        }

        @Override
        public boolean hasCancelButton() {
            return true;
        }

        @Override
        public String getConfirmText() {
            return null;
        }

        @Override
        public String getCancelText() {
            return null;
        }

        @Override
        public void customDialogConfirm() {
            float extraCredits = 0;
            boolean recordCargo = btn_recordCargo.isChecked();
            boolean recordShip = btn_recordShip.isChecked();
            boolean recordIndustry = btn_recordColony.isChecked();
            CargoAPI convertCargo = Global.getFactory().createCargo(true);
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                if (Misc.playerHasStorageAccess(market)) {
                    CargoAPI storageCargo = Misc.getStorageCargo(market);
                    if (storageCargo != null) {
                        if (recordCargo) {
                            convertCargo.addAll(storageCargo);
                        }
                        if (recordShip) {
                            FleetDataAPI mothballedShips = storageCargo.getMothballedShips();
                            if (mothballedShips != null)
                                for (FleetMemberAPI member : mothballedShips.getMembersListCopy()) {
                                    extraCredits += member.getBaseValue();
                                }
                        }
                    }
                }
                if (recordIndustry) {
                    if (market.isPlayerOwned()) {
                        for (Industry industry : market.getIndustries()) {
                            extraCredits += industry.getBuildCost();
                        }
                    }
                }
            }
            CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
            if (recordCargo) {
                convertCargo.addAll(playerCargo);
                for (CargoStackAPI stack : convertCargo.getStacksCopy()) {
                    extraCredits += stack.getBaseValuePerUnit() * stack.getSize();
                }
            }
            if (recordShip) {
                FleetDataAPI mothballedShips = playerCargo.getMothballedShips();
                if (mothballedShips != null)
                    for (FleetMemberAPI member : mothballedShips.getMembersListCopy()) {
                        extraCredits += member.getBaseValue();
                    }
            }
            textPanel.addPara(d_i18n.get("recordExtraCredits_success") + " %s ", Misc.getHighlightColor(), Misc.getDGSCredits(extraCredits));
            toRecordInheritData.inheritCredits += extraCredits;
            record(selectedOption);
        }

        @Override
        public void customDialogCancel() {
            optionSelected(null, OptionID.CHECK_RECORD);
        }

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            return null;
        }
    }
}
