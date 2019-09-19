package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.input.Keyboard;

public class UNGP_CampaignPlugin implements EveryFrameScript {
    private UNGP_InGameData inGameData;
    private boolean passedInheritTime = false;

    private IntervalUtil inheritChecker = new IntervalUtil(1f, 1f);

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
            if (!passedInheritTime) {
                float days = Global.getSector().getClock().convertToDays(amount);
                inheritChecker.advance(days);
                if (inheritChecker.intervalElapsed()) {
                    passedInheritTime = true;
                    inGameData.passedInheritTime = true;
                }
            }
        }
        if (!Global.getSector().getCampaignUI().isShowingDialog()) {
            //打开界面
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_P)) {
                if (Global.getSector().getCampaignUI().showInteractionDialog(new UNGP_InteractionDialog(inGameData), null)) {

                }
            }
        }
    }
}
