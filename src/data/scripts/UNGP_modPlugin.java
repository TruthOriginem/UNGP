package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.campaign.UNGP_CampaignPlugin;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.UNGP_InheritData;

import static com.fs.starfarer.api.Global.getSettings;

public class UNGP_modPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() throws Exception {
        boolean hasLazyLib = getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Unofficial New Game Plus requires LazyLib!");
        }
    }

    @Override
    public void onNewGame() {
    }

    @Override
    public void onGameLoad(boolean newGame) {
        addScriptsIfNeeded();
    }

    void addScriptsIfNeeded() {
        if (!Global.getSector().hasScript(UNGP_CampaignPlugin.class)) {
            Global.getSector().addScript(new UNGP_CampaignPlugin());
        }
    }

    @Override
    public void afterGameSave() {
        UNGP_InGameData inGameData = UNGP_InGameData.getInstance();
        if (inGameData.shouldDeleteRecordNextSave) {
            inGameData.shouldDeleteRecordNextSave = false;
            UNGP_InheritData.Delete();
        }

    }
}
