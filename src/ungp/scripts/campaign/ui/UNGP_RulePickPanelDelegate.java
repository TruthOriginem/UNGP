package ungp.scripts.campaign.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import java.util.Map;

/**
 * 先创建plugin
 * 然后创建delegate，將plugin中的panel指定为delegate传递的panel
 */
public class UNGP_RulePickPanelDelegate implements CustomVisualDialogDelegate {
    protected UNGP_RulePickPanelPlugin panelPlugin;
    protected DialogCallbacks callbacks;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    public UNGP_RulePickPanelDelegate(UNGP_RulePickPanelPlugin panelPlugin, InteractionDialogAPI dialog,
                                      Map<String, MemoryAPI> memoryMap) {
        this.panelPlugin = panelPlugin;
        this.dialog = dialog;
        this.memoryMap = memoryMap;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.callbacks = callbacks;
        callbacks.getPanelFader().setDurationOut(2f);
        // 初始化后，把panel等属性通过panelPlugin的init传递
        this.panelPlugin.init(panel, callbacks, dialog);
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return panelPlugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0;
    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void reportDismissed(int option) {

    }
}
