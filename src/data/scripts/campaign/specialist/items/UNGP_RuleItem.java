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
import data.scripts.utils.UNGPFont;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static data.scripts.utils.UNGPFont.*;

public class UNGP_RuleItem extends BaseSpecialItemPlugin {
    private static SpriteAPI BG_NORMAL_SPRITE;
    private static SpriteAPI BG_MASK_SPRITE;

    /**
     * @param rule 不同的special item，享受同一个plugin
     * @return
     */
    public static String getSpecialItemID(URule rule) {
        if (rule.isMileStone()) {
            return "ungp_ruleitem_milestone";
        } else if (rule.isGolden()) {
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

        String prefix = "        ";

        rule.addDesc(tooltip, opad * 2f, prefix);

        rule.addRollDesc(tooltip, opad * 22f, prefix);

        boolean showMore = Keyboard.isKeyDown(Keyboard.KEY_F1);
        rule.addChallengeRelatedDesc(tooltip, opad * 2f, prefix, showMore);

        rule.addCost(tooltip, opad * 2f);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (rule == null) return;
        final SpriteAPI sprite = BG_NORMAL_SPRITE;
        final SpriteAPI mask = BG_MASK_SPRITE;
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float frameFactor = UNGP_UITimeScript.getFactor("2secs");

        boolean isGoldenOrMilestone = rule.isGolden() || rule.isMileStone();
        float speedUpBy2FrameFactor = (frameFactor % 0.5f) / 0.5f;


//        sprite = Global.getSettings().getSprite(getBackgroundSpriteNameByFrame((int) Math.floor(frameFactor * 23.99f)));
        if (sprite != null) {
            Color baseColor = rule.getCorrectColor();

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
            if (isGoldenOrMilestone) {
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

        frameFactor = UNGP_UITimeScript.getFactor("6secs");

        frameFactor = (float) (FastTrig.cos(2f * Math.PI * frameFactor) + 1f) / 2f;
        float upCap = 0.6f;
        float downCap = 0.05f;
        if (frameFactor > upCap) {
            frameFactor = 1f;
        } else if (frameFactor < downCap) {
            frameFactor = 0f;
        } else {
            frameFactor = (frameFactor - downCap) / (upCap - downCap);
        }

        // 渲染图标
        SpriteAPI icon = Global.getSettings().getSprite(rule.getSpritePath());
        if (icon != null) {
            icon.setSize(64f, 64f);
            icon.setNormalBlend();
            icon.setAlphaMult(alphaMult * (0.3f + 0.7f * (frameFactor)));
            icon.renderAtCenter(cx, cy);
            // 扫描效果
            if (isGoldenOrMilestone) {
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


        float nameAlpha = 1f - frameFactor;
        if (nameAlpha != 0) {
            float fontSize = 20f;
            float maxWidth;
            float maxHeight;
            String ruleName = rule.getName();
            DrawableString drawableName;
            if (!notOnlyEN(ruleName)) {
                if (ruleName.length() > 5) {
                    fontSize = 16f;
                }
                drawableName = ORBITRON_BOLD;
                maxWidth = fontSize * 5;
                maxHeight = fontSize * 5;
            } else {
                fontSize = 16f;
                drawableName = ORBITRON;
                maxWidth = 100;
                maxHeight = 100;
            }
            drawableName.setText(ruleName);
            drawableName.setFontSize(fontSize);
            drawableName.setMaxWidth(maxWidth);
            drawableName.setMaxHeight(maxHeight);
            Color color = Misc.scaleColor(Color.BLACK, 1f - frameFactor);
            drawableName.setColor(color);
            UNGPFont.drawShadow(drawableName, cx - drawableName.getWidth() * 0.5f, cy + drawableName.getHeight() * 0.5f, 2);
            color = Misc.scaleColor(rule.getCorrectColor(), 1f - frameFactor);
            drawableName.setColor(color);
            drawableName.draw((int) (cx - drawableName.getWidth() * 0.5f), cy + drawableName.getHeight() * 0.5f);
        }
        final DrawableString drawableCost = UNGPFont.getDynamicDrawable(rule.getCostString(), rule.getCostColor());
        drawableCost.draw(cx + 24, cy - 24);

        final DrawableString drawableRuleType = UNGPFont.getDynamicDrawable(rule.getRuleTypeCharacter(), rule.getCorrectColor());
        drawableRuleType.draw(cx - 42, cy + 42);
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
