package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.specialist.rules.UNGP_RuleSorter;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.*;
import static org.lwjgl.opengl.GL11.*;

public class UNGP_UIEntityPlugin extends BaseCustomEntityPlugin {
    private static final float SMALL_ICON_SIZE = 32f;
    private static final Color LEVEL_COLOR = new Color(255, 148, 89, 255);
    private static final Rectangle SpecialistIconRect;
    transient private SpriteAPI icon;
    transient private long lastTime;
    transient private UNGP_Progress moveOverAndWaitProgress;

    static {
        int width = (int) (Global.getSettings().getScreenWidth() * Display.getPixelScaleFactor());
        int height = (int) (Global.getSettings().getScreenHeight() * Display.getPixelScaleFactor());
        SpecialistIconRect = new Rectangle(0, height - 100, 64, 64);
    }


    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        readResolve();
    }

    Object readResolve() {
        lastTime = getTime();
        moveOverAndWaitProgress = new UNGP_Progress(0.2f);
        icon = Global.getSettings().getSprite("icons", "UNGP_sdimlogo");
        return this;
    }

    //milli
    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    public float getDeltaTime() {
        long thisTime = getTime();
        int deltaTime = (int) (thisTime - lastTime);
        lastTime = thisTime;
        return deltaTime / 1000f;
    }

    @Override
    public void advance(float amount) {
    }


    public float getRenderRange() {
        return 9999999f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (!UNGP_RulesManager.isSpecialistMode()) {
            return;
        }
        if (layer != CampaignEngineLayers.ABOVE) {
            return;
        }
        DrawableString levelString = UNGPFont.getOrbitronFont().createText(getCurrentDifficultyLevel() + "");

        float amount = getDeltaTime();

        int width = (int) (Global.getSettings().getScreenWidth() * Display.getPixelScaleFactor());
        int height = (int) (Global.getSettings().getScreenHeight() * Display.getPixelScaleFactor());

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -2000, 2000);

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

//        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        if (!isTextureEnabled) {
//            GL11.glEnable(GL11.GL_TEXTURE_2D);
//        }

        float alphaMult = 0.7f;
        if (SpecialistIconRect.contains(Mouse.getX(), Mouse.getY())) {
            moveOverAndWaitProgress.advance(amount);
            alphaMult = 0.7f + moveOverAndWaitProgress.getProgress() * 0.3f;
            if (moveOverAndWaitProgress.getProgress() > 0.7f) {
                List<URule> bonusRules = new ArrayList<>();
                List<URule> notBonusRules = new ArrayList<>();
                for (URule rule : ACTIVATED_RULES_IN_THIS_GAME) {
                    if (rule.isBonus()) {
                        bonusRules.add(rule);
                    } else {
                        notBonusRules.add(rule);
                    }
                }
                Collections.sort(bonusRules, new UNGP_RuleSorter());
                Collections.sort(notBonusRules, new UNGP_RuleSorter());

                float y = SpecialistIconRect.getY() - 5f - SMALL_ICON_SIZE;
                renderIcons(bonusRules, y);
                y -= 5f + SMALL_ICON_SIZE;
                renderIcons(notBonusRules, y);
            }
        } else {
            moveOverAndWaitProgress.reset();
        }

        icon.setAlphaMult(alphaMult);
        icon.render(SpecialistIconRect.getX(), SpecialistIconRect.getY());

        float textHeight = levelString.getHeight();
        float textWidth = levelString.getWidth();
//        LazyFont.DrawableString drawableString = UNGPFont.fontdraw.createText(ShownDifficultyLevel + "");
        levelString.setColor(Misc.interpolateColor(LEVEL_COLOR, Color.red, Math.min(1f, UNGP_RulesManager.getCurrentDifficultyLevel() / 20f)));
        levelString.draw(SpecialistIconRect.getX() + SpecialistIconRect.getWidth() * 0.5f - textWidth * 0.5f,
                SpecialistIconRect.getY() + SpecialistIconRect.getHeight() * 0.5f + textHeight * 0.5f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        levelString.dispose();
        //数字
//        GL11.glPushAttrib(GL_ALL_ATTRIB_BITS);
//        GL11.glMatrixMode(GL11.GL_PROJECTION);
//        GL11.glPushMatrix();
//        GL11.glViewport(0, 0, width, height);
//        GL11.glLoadIdentity();
//
//        GL11.glMatrixMode(GL11.GL_MODELVIEW);
//        GL11.glPushMatrix();
//
//        GL11.glOrtho(0.0, width, 0.0, height, -1.0, 1.0);

//        int bindedTexture = glGetInteger(GL_TEXTURE_BINDING_2D);

//        ORBITRON.setText(UNGP_RulesManager.ShownDifficultyLevel + "");


//        GL11.glEnable(GL11.GL_TEXTURE_2D);
//        glBindTexture(GL_TEXTURE_2D, icon.getTextureId());

//        GL11.glPopMatrix();
//        GL11.glMatrixMode(GL11.GL_PROJECTION);
//        GL11.glPopMatrix();
//        GL11.glPopAttrib();
    }

    private void renderIcons(List<URule> bonusRules, float y) {
        for (int i = 0; i < bonusRules.size(); i++) {
            float x = SpecialistIconRect.getX() + i * (SMALL_ICON_SIZE + 3f);
            SpriteAPI icon = Global.getSettings().getSprite(bonusRules.get(i).getSpritePath());
            icon.setSize(SMALL_ICON_SIZE, SMALL_ICON_SIZE);
            icon.setAlphaMult(0.7f);
            icon.render(x, y);
        }
    }
}
