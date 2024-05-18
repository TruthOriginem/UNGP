package ungp.scripts.campaign.specialist.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.specialist.rules.UNGP_RulePickHelper;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.campaign.ui.UNGP_RulePickPanelDelegate;
import ungp.scripts.campaign.ui.UNGP_RulePickPanelPlugin;
import ungp.scripts.utils.UNGP_Feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import static ungp.scripts.utils.Constants.root_i18n;
import static ungp.scripts.utils.Constants.rules_i18n;

/**
 * 重选的dialog而已
 */
public class UNGP_RepickRulesDialog implements InteractionDialogPlugin {
    private Object OptionRepick = new Object();
    private Object OptionConfirm = new Object();
    private Object OptionLeave = new Object();

    private InteractionDialogAPI dialog;
    private OptionPanelAPI options;
    private TextPanelAPI textPanel;
    private IntelUIAPI intelUI;
    private IntelInfoPlugin intelPlugin;
    private List<URule> pickedList;
    private boolean couldRepick = false;

    public UNGP_RepickRulesDialog(IntelUIAPI ui, IntelInfoPlugin intelPlugin) {
        this.intelUI = ui;
        this.intelPlugin = intelPlugin;
        pickedList = new ArrayList<>();
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.options = dialog.getOptionPanel();
        this.textPanel = dialog.getTextPanel();

        textPanel.addPara(rules_i18n.get("repick_desc"));
        options.addOption(root_i18n.get("rulepick_button"), OptionRepick);
        options.addOption(root_i18n.get("confirm"), OptionConfirm);
        options.addOption(root_i18n.get("leave"), OptionLeave);
        dialog.setOptionOnEscape(null, OptionLeave);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
        if (optionData == OptionRepick) {
            pickedList.clear();
            couldRepick = false;
            float width = Global.getSettings().getScreenWidth();
            float height = Global.getSettings().getScreenHeight();
            final Difficulty difficulty = inGameData.getDifficulty();
            UNGP_RulesManager.setStaticDifficulty(difficulty);
            UNGP_RulePickPanelPlugin plugin = UNGP_RulePickPanelPlugin.createPlugin(
                    difficulty, pickedList, inGameData.getCompletedChallenges(), new Script() {
                        @Override
                        public void run() {
                            textPanel.addPara(root_i18n.get("hardmodeDes"));
                            TooltipMakerAPI tooltip = textPanel.beginTooltip();
                            for (URule rule : pickedList) {
                                TooltipMakerAPI imageMaker = tooltip.beginImageWithText(rule.getSpritePath(), 32f);
                                imageMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                                rule.addDesc(imageMaker, 0f);
                                tooltip.addImageWithText(3f);
                            }
                            couldRepick = true;
                            if (UNGP_RulePickHelper.hasUnmetReasons(pickedList, difficulty)) {
                                tooltip.addPara(root_i18n.get("rulepick_notMeet"), Misc.getNegativeHighlightColor(), 5f);
                                couldRepick = false;
                            }
                            textPanel.addTooltip();
                        }
                    }, null);
            dialog.showCustomVisualDialog(width, height,
                                          new UNGP_RulePickPanelDelegate(plugin, dialog, getMemoryMap()));
        }

        if (optionData == OptionConfirm) {
            if (!pickedList.isEmpty() && couldRepick) {
                inGameData.reduceTimesToChangeSpecialistMode();
                inGameData.saveActivatedRules(pickedList);
                UNGP_Feedback.setFeedBackList(pickedList);
                UNGP_Feedback.resetFeedbackSent();
                UNGP_RulesManager.updateRulesCache();
            }
            dialog.dismiss();
            intelUI.updateUIForItem(intelPlugin);
        }

        if (optionData == OptionLeave) {
            dialog.dismiss();
            intelUI.updateUIForItem(intelPlugin);
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    @Override
    public void advance(float amount) {
        if (pickedList != null) {
            options.setEnabled(OptionConfirm, (!pickedList.isEmpty()) && couldRepick);
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

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
