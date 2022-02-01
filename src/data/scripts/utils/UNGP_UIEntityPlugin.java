package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.*;

public class UNGP_UIEntityPlugin extends BaseCustomEntityPlugin {
    private static final float BASIC_ICON_ALPHA = 0.4f;
    private static final float SMALL_ICON_SIZE = 32f;
    //    private static final Color LEVEL_COLOR = new Color(255, 148, 89, 255);
    public static final Rectangle specialistIconRect;
    transient private UNGP_Progress moveOverAndWaitProgress;
    transient private SpriteAPI layer0;
    transient private SpriteAPI layer1;
    transient private float targetIconAlphaMult = BASIC_ICON_ALPHA;
    transient private float currentIconAlphaMult = BASIC_ICON_ALPHA;
    transient private float elapsed = 0f;
    transient private long lastSysTime;

    static {
        specialistIconRect = new Rectangle(10, (int) (Global.getSettings().getScreenHeightPixels()) - 120, 80, 80);
    }


    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        readResolve();
    }

    Object readResolve() {
        moveOverAndWaitProgress = new UNGP_Progress(0.2f);
        targetIconAlphaMult = 0f;
        currentIconAlphaMult = 0f;
        lastSysTime = System.nanoTime();
        layer0 = Global.getSettings().getSprite("fx", "UNGP_specialist_icon_layer0");
        layer1 = Global.getSettings().getSprite("fx", "UNGP_specialist_icon_layer1");
        return this;
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
        Difficulty difficulty = getGlobalDifficulty();
//        float checker = elapsed % 8;
//        int index = (int) checker / 2;
//        difficulty = Difficulty.values()[index];

        long curSysTime = System.nanoTime();
        float amount = Math.min(Math.max((curSysTime - lastSysTime) / 1000000000f, 0f), 0.1f);
        elapsed += amount;

        int width = (int) Global.getSettings().getScreenWidthPixels();
        int height = (int) (Global.getSettings().getScreenHeightPixels());

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -2000, 2000);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float iconCenterX = specialistIconRect.getX() + specialistIconRect.getWidth() / 2f;
        float iconCenterY = specialistIconRect.getY() + specialistIconRect.getHeight() / 2f;

        float size = (float) (110f * (1f + 0.1f * FastTrig.sin(this.elapsed)));
        layer0.setAngle(MathUtils.clampAngle(this.elapsed * 60f));
        layer0.setAlphaMult(currentIconAlphaMult);
        layer0.setWidth(size);
        layer0.setHeight(size);
        layer0.setAdditiveBlend();
        layer0.renderAtCenter(iconCenterX, iconCenterY);

        size = (float) (128f * (1f + 0.05f * FastTrig.cos(this.elapsed)));
        layer1.setAngle(MathUtils.clampAngle(-this.elapsed * 30f));
        layer1.setAlphaMult(currentIconAlphaMult * 0.6f);
        layer1.setWidth(size);
        layer1.setHeight(size);
        layer1.setColor(difficulty.color);
        layer1.setAdditiveBlend();
        layer1.renderAtCenter(iconCenterX, iconCenterY);

        layer1.setColor(Color.white);
        layer1.setAngle(MathUtils.clampAngle(this.elapsed * 30f));
        layer1.setSize(size * 1.5f, size * 1.5f);
        layer1.setAlphaMult(currentIconAlphaMult * 0.4f);
        layer1.renderAtCenter(iconCenterX, iconCenterY);

        size = 64f;
        SpriteAPI character = Global.getSettings().getSprite(difficulty.spritePath);
        character.setSize(size, size);
        character.renderAtCenter(iconCenterX, iconCenterY);

        targetIconAlphaMult = BASIC_ICON_ALPHA;
        if (specialistIconRect.contains(Mouse.getX(), Mouse.getY())) {
            moveOverAndWaitProgress.advance(amount);
            targetIconAlphaMult = BASIC_ICON_ALPHA + moveOverAndWaitProgress.getProgress() * (1f - BASIC_ICON_ALPHA);
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
                Collections.sort(bonusRules, new UNGP_RulesManager.UNGP_RuleSorter());
                Collections.sort(notBonusRules, new UNGP_RulesManager.UNGP_RuleSorter());

                float iconGroup_y = specialistIconRect.getY() - 5f - SMALL_ICON_SIZE;
                renderIcons(bonusRules, iconGroup_y);
                iconGroup_y -= 5f + SMALL_ICON_SIZE;
                renderIcons(notBonusRules, iconGroup_y);
            }
        } else {
            moveOverAndWaitProgress.reset();
        }
        targetIconAlphaMult *= viewport.getAlphaMult();
        targetIconAlphaMult = Math.min(1f, Math.max(0f, targetIconAlphaMult));
        currentIconAlphaMult = Misc.interpolate(currentIconAlphaMult, targetIconAlphaMult, 5f * amount);


        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        lastSysTime = curSysTime;
    }

    private void renderIcons(List<URule> bonusRules, float y) {
        for (int i = 0; i < bonusRules.size(); i++) {
            float x = specialistIconRect.getX() + i * (SMALL_ICON_SIZE + 3f);
            SpriteAPI icon = Global.getSettings().getSprite(bonusRules.get(i).getSpritePath());
            icon.setSize(SMALL_ICON_SIZE, SMALL_ICON_SIZE);
            icon.setAlphaMult(0.7f);
            icon.render(x, y);
        }
    }
}
