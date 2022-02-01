package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.intel.inspection.HegemonyInspectionIntel;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static com.fs.starfarer.api.impl.campaign.intel.inspection.HegemonyInspectionIntel.AntiInspectionOrders;

public class UNGP_MakeAnException extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    private float rollChance = 0.5f;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        rollChance = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.5f, 0.5f);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            List<HegemonyInspectionIntel> checkedHegemonyInspection = getDataInCampaign(0);
            if (checkedHegemonyInspection == null) {
                checkedHegemonyInspection = new ArrayList<>();
                saveDataInCampaign(0, checkedHegemonyInspection);
            }
            for (IntelInfoPlugin intelInfoPlugin : Global.getSector().getIntelManager().getIntel(HegemonyInspectionIntel.class)) {
                HegemonyInspectionIntel inspectionIntel = (HegemonyInspectionIntel) intelInfoPlugin;
                if (!checkedHegemonyInspection.contains(inspectionIntel) &&
                        !(inspectionIntel.isDone() || inspectionIntel.shouldRemoveIntel())) {
                    FactionAPI faction = inspectionIntel.getFaction();
                    if (!faction.isHostileTo(Factions.PLAYER) && inspectionIntel.getOrders() != AntiInspectionOrders.BRIBE) {
                        if (roll(getRandomByDay(), rollChance)) {
                            inspectionIntel.setOrders(AntiInspectionOrders.BRIBE);
                            MessageIntel message = createMessage();
                            message.addLine(rule.getExtra1());
                            showMessage(message);
                        }
                    }
                    checkedHegemonyInspection.add(inspectionIntel);
                }
            }
            ListIterator<HegemonyInspectionIntel> iterator = checkedHegemonyInspection.listIterator();
            while (iterator.hasNext()) {
                HegemonyInspectionIntel next = iterator.next();
                if (next.isDone() || next.shouldRemoveIntel()) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void cleanUp() {
        clearDataInCampaign(0);
    }
}
