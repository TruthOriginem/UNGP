package ungp.scripts.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.awt.*;

public class UIRect {
    // top left = (0,0)
    private float x;
    private float y;
    private float width;
    private float height;

    public UIRect(float x, float y, float width, float height) {
        this.x = (int) x;
        this.y = (int) y;
        this.width = (int) width;
        this.height = (int) height;
    }

    public UIRect shrink(float marginTop, float marginBottom, float marginLeft, float marginRight) {
        float newWidth = width - marginLeft - marginRight;
        float newHeight = height - marginTop - marginBottom;
        float newX = x + marginLeft;
        float newY = y + marginTop;
        return new UIRect(newX, newY, newWidth, newHeight);
    }

    public UIRect shrink(float margin) {
        return shrink(margin, margin, margin, margin);
    }

    public UIRect shrink(float xMargin, float yMargin) {
        return shrink(yMargin, yMargin, xMargin, xMargin);
    }

    public UIRect[] splitHorizontally(float... weights) {
        if (weights == null || weights.length == 0) {
            return new UIRect[]{this};
        } else {
            float total = 0;
            for (float weight : weights) {
                total += weight;
            }
            float addOnX = 0;
            UIRect[] newRects = new UIRect[weights.length];
            for (int i = 0; i < weights.length; i++) {
                float curWidth = width * (weights[i] / total);
                newRects[i] = new UIRect(x + addOnX, y, curWidth, height);
                addOnX += curWidth;
            }
            return newRects;
        }
    }

    public UIRect[] splitHorizontally(float splitWidth) {
        if (splitWidth >= width) {
            return new UIRect[]{this};
        } else {
            UIRect[] newRects = new UIRect[2];
            newRects[0] = new UIRect(x, y, splitWidth, height);
            newRects[1] = new UIRect(x + splitWidth, y, width - splitWidth, height);
            return newRects;
        }
    }

    public UIRect[] splitHorizontallyReverse(float splitWidth) {
        if (splitWidth >= width) {
            return new UIRect[]{this};
        } else {
            UIRect[] newRects = new UIRect[2];
            newRects[0] = new UIRect(x, y, width - splitWidth, height);
            newRects[1] = new UIRect(x + (width - splitWidth), y, splitWidth, height);
            return newRects;
        }
    }

    public UIRect[] splitVertically(float... weights) {
        if (weights == null || weights.length == 0) {
            return new UIRect[]{this};
        } else {
            float total = 0;
            for (float weight : weights) {
                total += weight;
            }
            float addOnY = 0;
            UIRect[] newRects = new UIRect[weights.length];
            for (int i = 0; i < weights.length; i++) {
                float curHeight = height * (weights[i] / total);
                newRects[i] = new UIRect(x, y + addOnY, width, curHeight);
                addOnY += curHeight;
            }
            return newRects;
        }
    }

    public UIRect[] splitVertically(float splitHeight) {
        if (splitHeight >= height) {
            return new UIRect[]{this};
        } else {
            UIRect[] newRects = new UIRect[2];
            newRects[0] = new UIRect(x, y, width, splitHeight);
            newRects[1] = new UIRect(x, y + splitHeight, width, height - splitHeight);
            return newRects;
        }
    }

    public UIRect[] splitVerticallyReverse(float splitHeight) {
        if (splitHeight >= height) {
            return new UIRect[]{this};
        } else {
            UIRect[] newRects = new UIRect[2];
            newRects[0] = new UIRect(x, y, width, height - splitHeight);
            newRects[1] = new UIRect(x, y + (height - splitHeight), width, splitHeight);
            return newRects;
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }


    private CustomPanelAPI rootPanel;
    private TooltipMakerAPI latestTooltip;
    private UIPanelManager latestSubPanelManager;

    public void setRootPanel(CustomPanelAPI rootPanel) {
        this.rootPanel = rootPanel;
    }

    public TooltipMakerAPI beginTooltip(CustomPanelAPI rootPanel, boolean withSpoiler) {
        latestTooltip = rootPanel.createUIElement(width, height, withSpoiler);
        setRootPanel(rootPanel);
        return latestTooltip;
    }

    public void addTooltip() {
        if (rootPanel != null)
            rootPanel.addUIElement(latestTooltip).inTL(x, y);
    }

    public void removeLatestSubPanel() {
        if (rootPanel != null && latestSubPanelManager != null) {
            latestSubPanelManager.clearAllComponents();
            rootPanel.removeComponent(latestSubPanelManager.getPanel());
            latestSubPanelManager = null;
        }
    }

    public void addBoarder(CustomPanelAPI rootPanel, Color color, float thickness) {
        TooltipMakerAPI tooltipMaker = rootPanel.createUIElement(width, height, false);
        UIComponentAPI boarder = tooltipMaker.createRect(color, thickness);
        tooltipMaker.addCustomDoNotSetPosition(boarder).getPosition().inTL(0, 0).setSize(width, height);
        rootPanel.addUIElement(tooltipMaker).inTL(x, y);
    }

    public PositionAPI syncPositionSize(PositionAPI position) {
        return position.setSize(width, height);
    }

    public UIPanelManager createSubPanel(CustomPanelAPI rootPanel, CustomUIPanelPlugin uiPlugin) {
        if (this.rootPanel == null) setRootPanel(rootPanel);
        latestSubPanelManager = new UIPanelManager(rootPanel.createCustomPanel(width, height, uiPlugin), uiPlugin);
        rootPanel.addComponent(latestSubPanelManager.getPanel()).inTL(x, y);
        return latestSubPanelManager;
    }
}
