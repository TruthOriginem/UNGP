package ungp.scripts.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.util.ArrayList;
import java.util.Collections;
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
        return Collections.unmodifiableList(components);
    }

    public PositionAPI addComponent(UIComponentAPI component) {
        PositionAPI position;
        if (component instanceof TooltipMakerAPI) {
            TooltipMakerAPI tooltip = (TooltipMakerAPI) component;
            position = panel.addUIElement(tooltip);
            UIComponentAPI scroller = tooltip.getExternalScroller();
            components.add(scroller != null ? scroller : component);
        } else {
            position = panel.addComponent(component);
            components.add(component);
        }
        return position;
    }

    public void clearAllComponents() {
        for (UIComponentAPI component : components) {
            panel.removeComponent(component);
        }
        components.clear();
    }
}
