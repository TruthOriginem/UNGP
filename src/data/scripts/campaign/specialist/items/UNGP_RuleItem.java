package data.scripts.campaign.specialist.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.everyframe.UNGP_UITimeScript;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static data.scripts.utils.UNGPFont.ORBITRON;

public class UNGP_RuleItem extends BaseSpecialItemPlugin {
    private static SpriteAPI BG_NORMAL_SPRITE;
    private static SpriteAPI BG_MASK_SPRITE;

    /**
     * @param rule 不同的special item，享受同一个plugin
     * @return
     */
    public static String getSpecialItemID(URule rule) {
        if (rule.isGolden()) {
            return "ungp_ruleitem_golden";
        } else {
            return rule.isBonus() ? "ungp_ruleitem_positive" : "ungp_ruleitem_negative";
        }
    }

    protected URule rule;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        rule = URule.getByID(stack.getSpecialDataIfSpecial().getData());
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource, boolean useGray) {
        float opad = 10f;
        if (rule == null) return;
        tooltip.setTitleOrbitronLarge();
        Color c = Misc.getTextColor();

        tooltip.addTitle(rule.getName(), Misc.interpolateColor(rule.getCorrectColor(), c, 0.8f));

        rule.addPreDesc(tooltip, opad * 2f);

        rule.addDesc(tooltip, opad * 2f, "        ");

        rule.addCost(tooltip, opad * 2f);
//        tooltip.addPara()
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (rule == null) return;
        final SpriteAPI sprite = BG_NORMAL_SPRITE;
        final SpriteAPI mask = BG_MASK_SPRITE;
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float fontSize = ORBITRON.getFontSize();
        float frameFactor = UNGP_UITimeScript.getFactor("2secs");

        boolean isGoldenRule = rule.isGolden();
        ORBITRON.setText(rule.getCostString());
        ORBITRON.setMaxWidth(32f);
        ORBITRON.setMaxHeight(32f);
        ORBITRON.setColor(rule.getCostColor());
        ORBITRON.draw(cx + 24, cy - 24);
        ORBITRON.setColor(rule.getCorrectColor());
        ORBITRON.setText(rule.getRuleTypeCharacter());
        ORBITRON.draw(cx - 22 - fontSize, cy + 22 + fontSize);

        float speedUpBy2FrameFactor = (frameFactor % 0.5f) / 0.5f;
//        sprite = Global.getSettings().getSprite(getBackgroundSpriteNameByFrame((int) Math.floor(frameFactor * 23.99f)));
        if (sprite != null) {
            Color baseColor;
            if (rule.getId().equals("lobster_perday")) {
                baseColor = UNGP_RulesManager.getGoldenColor();
                isGoldenRule = true;
            } else {
                baseColor = rule.getCorrectColor();
            }
            sprite.setColor(baseColor);
            sprite.setNormalBlend();
            sprite.setAlphaMult(alphaMult * 0.8f);
            sprite.renderAtCenter(cx, cy);

            // 处理遮罩
            GL11.glColorMask(false, false, false, true);
            GL11.glPushMatrix();
            GL11.glTranslatef(cx, cy, 0);
            Misc.renderQuadAlpha(x * 3f, y * 3f, w * 3f, h * 3f, Misc.zeroColor, 0f);
            GL11.glPopMatrix();
            sprite.setBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
            sprite.renderAtCenter(cx, cy);

            mask.setAlphaMult(alphaMult * 0.6f);
            mask.setAngle(-frameFactor * 90f);
            mask.setBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA);
            mask.renderAtCenter(cx, cy);

            GL11.glColorMask(true, true, true, false);
            mask.setBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
            mask.renderAtCenter(cx, cy);

            // 边框渐隐
            sprite.setColor(Color.white);
            sprite.setAdditiveBlend();
            if (isGoldenRule) {
                sprite.setAlphaMult(0.6f * (float) Math.sin(Math.PI * frameFactor));
            } else {
                sprite.setAlphaMult(0.1f * (float) Math.sin(Math.PI * frameFactor));
            }
            sprite.renderAtCenter(cx, cy);

            // glow
            if (glowMult > 0) {
                sprite.setAlphaMult(alphaMult * glowMult * 0.2f);
                sprite.setAdditiveBlend();
                sprite.renderAtCenter(cx, cy);
            }
        }

        // 渲染图标
        SpriteAPI icon = Global.getSettings().getSprite(rule.getSpritePath());
        if (icon != null) {
            icon.setSize(64f, 64f);
            icon.setNormalBlend();
            icon.setAlphaMult(alphaMult);
            icon.renderAtCenter(cx, cy);
            // 扫描效果
            if (isGoldenRule) {
                icon.setColor(UNGP_RulesManager.getGoldenColor());
                icon.setAdditiveBlend();
                float tmp_h = 0.075f; // 扫描条的宽度
                float tmp_y = 1f + tmp_h - (1f + 2f * tmp_h) * speedUpBy2FrameFactor;
                float th = tmp_h;
                float ty = MathUtils.clamp(tmp_y, 0f, 1f);
                if (tmp_y > 1f - tmp_h) {
                    th = tmp_y > 1f ? 0f : 1f - tmp_y;
                }
                if (tmp_y < 0f) {
                    th = tmp_h + tmp_y;
                }
                icon.renderRegionAtCenter(cx, cy, 0f, ty, 1f, th);
            }
        }
    }

    @Override
    public String getDesignType() {
        return null;
    }

    @Override
    public float getTooltipWidth() {
        return getCommonWidth();
    }

    public static float getCommonWidth() {
        return 400f;
    }

    /**
     * 读取背景sprite(用不着改)
     */
    public static void loadSprite() {
        BG_NORMAL_SPRITE = Global.getSettings().getSprite("icons", "UNGP_itemBG");
        BG_MASK_SPRITE = Global.getSettings().getSprite("icons", "UNGP_itemBG_mask");
    }
}
