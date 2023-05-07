package ungp.scripts.campaign.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class UNGP_InteractionPanelPlugin extends BaseCustomUIPanelPlugin {
    private CustomPanelAPI customPanel;
    private PositionAPI pos;
    private float elapsed = 0f;
    private float lastYOffset = 0f;
    private SpriteAPI sprite = Global.getSettings().getSprite("illustrations", "UNGP_logo");
    private List<UIComponentAPI> currentComponents = new ArrayList<>();

    public void update(VisualPanelAPI visualPanel) {
        clearUIComponents();
        lastYOffset = 0f;
        customPanel = visualPanel.showCustomPanel(400f, 300f, this);
//        customPanel.getPosition().inTMid(200f);
    }


    public TooltipMakerAPI beginTooltip(float height, boolean withScroller) {
        TooltipMakerAPI tooltip = this.customPanel.createUIElement(pos.getWidth(), height, withScroller);
        currentComponents.add(tooltip);
        return tooltip;
    }

    public void addTooltip(float height, TooltipMakerAPI tooltip) {
        customPanel.addUIElement(tooltip).inTL(0, lastYOffset);
        lastYOffset += height;
    }


    public void clearUIComponents() {
        for (UIComponentAPI component : currentComponents) {
            customPanel.removeComponent(component);
        }
        currentComponents.clear();
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.pos = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
//        drawBorder(alphaMult);
    }

    private void drawBorder(float alphaMult) {
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        GL11.glPushMatrix();
        GL11.glDisable(GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Color color = Color.cyan;
        GL11.glColor4f(color.getRed() / 255f,
                       color.getGreen() / 255f,
                       color.getBlue() / 255f,
                       (float) (alphaMult * (0.75f + FastTrig.cos(-elapsed) * 0.25f)));
        GL11.glBegin(GL11.GL_LINE_LOOP);
        {
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + w, y);
            GL11.glVertex2f(x + w, y + h);
            GL11.glVertex2f(x, y + h);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    @Override
    public void render(float alphaMult) {
        if (!currentComponents.isEmpty()) return;
        sprite.setAlphaMult(alphaMult);
//        float screenWidth = Global.getSettings().getScreenWidth() / Display.getPixelScaleFactor();
//        float screenHeight = Global.getSettings().getScreenHeight() / Display.getPixelScaleFactor();
//        float preferX = screenWidth * 0.55f;
//        float preferY = screenHeight * 0.5f;
//        if (preferX + sprite.getWidth() > screenWidth) {
//            preferX = screenWidth - sprite.getWidth();
//        }
//        if (preferY + sprite.getHeight() > screenHeight) {
//            preferY = screenHeight - sprite.getHeight();
//        }
        float centerX = pos.getCenterX();
        float centerY = pos.getCenterY();
//        sprite.render(0, 0);
        sprite.renderAtCenter(centerX, centerY);
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }
}
