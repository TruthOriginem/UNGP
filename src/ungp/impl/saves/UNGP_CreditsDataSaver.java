package ungp.impl.saves;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.inherit.UNGP_InheritData;
import ungp.api.saves.UNGP_DataSaverAPI;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static ungp.scripts.utils.Constants.root_i18n;

public class UNGP_CreditsDataSaver implements UNGP_DataSaverAPI {
    public int credits;

    @Override
    public UNGP_DataSaverAPI createSaverBasedOnCurrentGame(UNGP_InGameData inGameData) {
        UNGP_CreditsDataSaver dataSaver = new UNGP_CreditsDataSaver();
        dataSaver.credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        return dataSaver;
    }

    @Override
    public UNGP_DataSaverAPI createEmptySaver() {
        return new UNGP_CreditsDataSaver();
    }

    @Override
    public void loadDataFromSavepointSlot(JSONObject jsonObject) throws JSONException {
        credits = jsonObject.optInt("credits", 0);
        // compatible things
        if (credits == 0 && jsonObject.has("inheritCredits")) {
            credits = jsonObject.optInt("inheritCredits", 0);
        }
    }

    @Override
    public void saveDataToSavepointSlot(JSONObject jsonObject) throws JSONException {
        jsonObject.put("credits", credits);
    }

    @Override
    public void startInheritDataFromSaver(TooltipMakerAPI root, Map<String, Object> params) {
        float inheritCreditsPercent = (float) params.get("inheritCreditsFactor");
        // Credits
        int creditsInherited = (int) (credits * inheritCreditsPercent);
        Global.getSector().getPlayerFleet().getCargo().getCredits().add(creditsInherited);
        if (Global.getSector().getCampaignUI().isShowingDialog()) {
            AddRemoveCommodity.addCreditsGainText(creditsInherited, Global.getSector().getCampaignUI().getCurrentInteractionDialog().getTextPanel());
        }
    }

    @Override
    public void addSaverInfo(TooltipMakerAPI root, String descKey) {
        TooltipMakerAPI section = root.beginImageWithText("graphics/icons/reports/generic_income.png", 24f);
        section.addPara(root_i18n.get(descKey + "_2"), 3f);
        root.addImageWithText(5f);
        root.addPara(UNGP_InheritData.BULLETED_PREFIX + Misc.getDGSCredits(credits), Misc.getHighlightColor(), 5f);
    }
}
