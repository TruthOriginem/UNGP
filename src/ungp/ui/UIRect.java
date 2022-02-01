package ungp.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class UIRect {
    // top left = (0,0)
    float x;
    float y;
    float width;
    float height;

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


    private TooltipMakerAPI startedTooltip;
    private CustomPanelAPI linkedPanel;

    public TooltipMakerAPI beginTooltip(CustomPanelAPI rootPanel, boolean withSpoiler) {
        startedTooltip = rootPanel.createUIElement(width, height, withSpoiler);
        linkedPanel = rootPanel;
        return startedTooltip;
    }

    public void addTooltip() {
        if (linkedPanel != null)
            linkedPanel.addUIElement(startedTooltip).inTL(x, y);
    }

    public CustomPanelAPI createPanel(CustomPanelAPI rootPanel, CustomUIPanelPlugin uiPlugin) {
        CustomPanelAPI newPanel = rootPanel.createCustomPanel(width, height, uiPlugin);
        rootPanel.addComponent(newPanel).inTL(x, y);
        return newPanel;
    }
}
