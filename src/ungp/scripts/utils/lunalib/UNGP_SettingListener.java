package ungp.scripts.utils.lunalib;

import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;
import ungp.scripts.campaign.everyframe.UNGP_SpecialistWidgetPlugin;

public class UNGP_SettingListener implements LunaSettingsListener {
    @Override
    public void settingsChanged(@NotNull String modId) {
        UNGP_SpecialistWidgetPlugin.updateUI();
    }
}
