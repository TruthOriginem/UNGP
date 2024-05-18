package ungp.scripts.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.util.ArrayList;
import java.util.List;

public class UIPanelManager {
    private CustomPanelAPI panel;
    private CustomUIPanelPlugin panelPlugin;
    private List<UIComponentAPI> components = new ArrayList<>();

    public UIPanelManager(CustomPanelAPI panel, CustomUIPanelPlugin panelPlugin) {
        this.panel = panel;
        this.panelPlugin = panelPlugin;
    }

    public CustomPanelAPI getPanel() {
        return panel;
    }

    public CustomUIPanelPlugin getPanelPlugin() {
        return panelPlugin;
    }

    public List<UIComponentAPI> getComponents() {
        return components;
    }

    public void addComponent(UIComponentAPI component) {
        if (component instanceof TooltipMakerAPI) {
            panel.addUIElement((TooltipMakerAPI) component);
        } else {
            panel.addComponent(component);
        }
    }

    public void clearAllComponents() {
        for (UIComponentAPI component : components) {
            panel.bringComponentToTop(component);
            panel.removeComponent(component);
        }
        components.clear();
    }
}
