package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.utils.SimpleI18n;
import org.lwjgl.input.Keyboard;

public class UNGP_CampaignPlugin implements EveryFrameScript {
    private static final SimpleI18n.I18nSection i18n = new SimpleI18n.I18nSection("UNGP", "c", true);

    private UNGP_InGameData inGameData;

    private IntervalUtil inheritChecker = new IntervalUtil(1f, 1f);
    private float newGameCheckDays = 0.1f;
    private boolean newGameChecked = false;
    private boolean shouldShowDialog = false;

    public UNGP_CampaignPlugin() {
        inGameData = new UNGP_InGameData();
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (!Global.getSector().isPaused()) {
            if (!inGameData.passedInheritTime) {
                float days = Global.getSector().getClock().convertToDays(amount);
                if (!newGameChecked) {
                    if (newGameCheckDays > 0f) {
                        newGameCheckDays -= days;
                    } else {
                        newGameChecked = true;
                        if (UNGP_InheritData.InheritDataExists()) {
                            Global.getSector().getCampaignUI().showConfirmDialog(i18n.get("message"), i18n.get("yes"), i18n.get("no"), new Script() {
                                @Override
                                public void run() {
                                    shouldShowDialog = true;
                                }
                            }, null);
                        }
                    }
                }
                inheritChecker.advance(days);
                if (inheritChecker.intervalElapsed()) {
                    inGameData.passedInheritTime = true;
                }
            }
        }
        if (!Global.getSector().getCampaignUI().isShowingDialog()) {
            //打开界面
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_P)) {
                shouldShowDialog = true;
            }
            if (shouldShowDialog) {
                if (showUNGPDialog()) {
                    shouldShowDialog = false;
                }
            }
        }
    }

    private boolean showUNGPDialog() {
        return Global.getSector().getCampaignUI().showInteractionDialog(new UNGP_InteractionDialog(inGameData), null);
    }
}
