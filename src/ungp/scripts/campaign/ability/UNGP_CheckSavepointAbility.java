package ungp.scripts.campaign.ability;

import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.UNGP_Settings;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import ungp.scripts.utils.Constants;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class UNGP_CheckSavepointAbility extends BaseDurationAbility {
    @Override
    protected void activateImpl() {
        UNGP_CampaignPlugin.getInstance().showUNGPDialog();
    }

    @Override
    protected void applyEffect(float amount, float level) {

    }

    @Override
    protected void deactivateImpl() {

    }

    @Override
    protected void cleanupImpl() {

    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();


        LabelAPI title = tooltip.addTitle(Constants.ability_i18n.get("checkSavepoint_title"));
        title.setHighlightColor(gray);

        float pad = 10f;

        tooltip.addPara(Constants.ability_i18n.get("checkSavepoint_desc"), pad, highlight,
                        Keyboard.getKeyName(UNGP_Settings.getShowMenuKey1()),
                        Keyboard.getKeyName(UNGP_Settings.getShowMenuKey2()));
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }
}
