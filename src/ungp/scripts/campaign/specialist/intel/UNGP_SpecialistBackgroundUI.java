package ungp.scripts.campaign.specialist.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class UNGP_SpecialistBackgroundUI extends BaseCustomUIPanelPlugin {
    public static final Color ABOVE_BG_COLOR = new Color(25, 255, 140);
    public static final Color BOTTOM_BG_COLOR = new Color(255, 26, 106);
    private static float uiElapsed = 0f;
    private static float uiLoopVolume = 0f;

    private PositionAPI p;
    private SpriteAPI seamless_bg = Global.getSettings().getSprite("fx", "UNGP_specialist_bg_seamless");
    private SpriteAPI ring_bg0 = Global.getSettings().getSprite("fx", "UNGP_specialist_bg_seamless_ring0");
    private SpriteAPI ring_bg1 = Global.getSettings().getSprite("fx", "UNGP_specialist_bg_seamless_ring1");
    private SpriteAPI ring_bg2 = Global.getSettings().getSprite("fx", "UNGP_specialist_bg_seamless_ring2");
    private SpriteAPI corner = Global.getSettings().getSprite("fx", "UNGP_specialist_bg_corner");
    private float halfSize = corner.getWidth() / 2f;

    public UNGP_SpecialistBackgroundUI() {
        ring_bg0.setSize(650f, 650f);
        ring_bg0.setAdditiveBlend();
        ring_bg1.setSize(650f, 650f);
        ring_bg1.setAdditiveBlend();
        ring_bg2.setSize(650f, 650f);
        ring_bg2.setAdditiveBlend();
    }

    public static void cleanBGUI() {
        if (uiElapsed > 100000f)
            uiElapsed = Misc.random.nextFloat() * 1000f;
        uiLoopVolume = 0f;
        ticking = true;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        p = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (p == null) return;
        float x = p.getX();
        float y = p.getY();
        float w = p.getWidth();
        float h = p.getHeight();

        // 属性入栈
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // 开启模板测试
        // 禁止写入深度和颜色缓冲区
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColorMask(false, false, false, false);
        // GL_ALWAYS: 总通过，ref 比较值，mask 0xFF(255)全过，0x00全不过
        GL11.glStencilFunc(GL11.GL_ALWAYS, 16, 0xFF); // Set stencil to 16
        // 通过则替换为16
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF); // Write to stencil buffer
        // 清除缓冲区
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT); // Clear stencil buffer

        Misc.renderQuad(x, y, w, h, Color.black, 1);

        // 重新打开颜色缓冲区
        GL11.glColorMask(true, true, true, true);
        // 等于16的通过(前面设置为16了)
        GL11.glStencilFunc(GL11.GL_EQUAL, 16, 0xFF);
        // 通过测试不变动
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0x00); // Don't write anything to stencil buffer

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Color color;
        GL11.glBegin(GL11.GL_QUADS);
        {
            color = BOTTOM_BG_COLOR;
            GL11.glColor4ub((byte) color.getRed(),
                            (byte) color.getGreen(),
                            (byte) color.getBlue(),
                            (byte) ((float) color.getAlpha() * alphaMult * 0.08f));

            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + w, y);
            color = ABOVE_BG_COLOR;
            GL11.glColor4ub((byte) color.getRed(),
                            (byte) color.getGreen(),
                            (byte) color.getBlue(),
                            (byte) ((float) color.getAlpha() * alphaMult * 0.08f));

            GL11.glVertex2f(x + w, y + h);
            GL11.glVertex2f(x, y + h);
        }
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, seamless_bg.getTextureId());
        float addOnTexX = w / seamless_bg.getWidth();
        float addOnTexY = h / seamless_bg.getHeight();

        color = Color.cyan;

        for (int i = 0; i < 2; i++) {
            GL11.glColor4ub((byte) color.getRed(),
                            (byte) color.getGreen(),
                            (byte) color.getBlue(),
                            (byte) (color.getAlpha() * alphaMult * (0.06f + 0.06f * i)));
            float progress = uiElapsed * (0.05f + 0.1f * i);
            GL11.glBegin(GL11.GL_QUADS);
            {
                float v = i * 0.2f + 0.1595f;
                float v2 = i * 0.4f + 0.3215f;
                GL11.glTexCoord2f(v + progress, v2 + progress);
                GL11.glVertex2f(x, y);
                GL11.glTexCoord2f(v + progress, v2 + progress + addOnTexY);
                GL11.glVertex2f(x, y + h);
                GL11.glTexCoord2f(v + progress + addOnTexX, v2 + progress + addOnTexY);
                GL11.glVertex2f(x + w, y + h);
                GL11.glTexCoord2f(v + progress + addOnTexX, v2 + progress);
                GL11.glVertex2f(x + w, y);
            }
            GL11.glEnd();
        }


        Color interpolateColor = Misc.interpolateColor(Color.cyan, Color.red, (float) ((Math.sin(uiElapsed * 0.5f) + 1f) / 2f));
        Color interpolateColor2 = Misc.interpolateColor(Color.red, Color.cyan, (float) ((Math.cos(uiElapsed * 0.5f) + 1f) / 2f));

        ring_bg0.setAlphaMult(alphaMult * 0.6f);
        ring_bg0.setAngle(MathUtils.clampAngle(uiElapsed * 30f));
        ring_bg0.setColor(interpolateColor);
        ring_bg0.renderAtCenter(x + w * 0.9f, y + h * 0.2f);

        ring_bg1.setAlphaMult(alphaMult * 0.6f);
        ring_bg1.setAngle(MathUtils.clampAngle(uiElapsed * -10f));
        ring_bg1.setColor(Color.cyan);
        ring_bg1.renderAtCenter(x + w * 0.9f, y + h * 0.2f);

        ring_bg2.setAlphaMult(alphaMult * 0.6f);
        ring_bg2.setAngle(MathUtils.clampAngle(uiElapsed * -30f));
        ring_bg2.setColor(interpolateColor2);
        ring_bg2.renderAtCenter(x + w * 0.9f, y + h * 0.2f);

        corner.setAngle(90f);
        corner.renderAtCenter(x + halfSize, y + halfSize);
        corner.setAngle(0f);
        corner.renderAtCenter(x + halfSize, y + h - halfSize);
        corner.setAngle(-90f);
        corner.renderAtCenter(x + w - halfSize, y + h - halfSize);
        corner.setAngle(-180f);
        corner.renderAtCenter(x + w - halfSize, y + halfSize);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF); // Pass test always
        GL11.glPopAttrib();
    }

    @Override
    public void render(float alphaMult) {

    }

    @Override
    public void advance(float amount) {
        uiElapsed += amount;
        if (ticking) {
            uiLoopVolume += amount;
            if (uiLoopVolume > 1f) {
                uiLoopVolume = 1f;
            }
            Global.getSoundPlayer().playUILoop("ui_specialist_intel_loop", 1f, uiLoopVolume * 0.5f);
            Global.getSector().getCampaignUI().suppressMusic(0.8f);
        } else {
            uiLoopVolume -= amount;
            if (uiLoopVolume < 0) {
                uiLoopVolume = 0;
            }
        }
    }

    private static boolean ticking = true;

    public static void stopTicking() {
        ticking = false;
    }

    public static void resumeTicking() {
        ticking = true;
    }

    @Override
    public void processInput(java.util.List<InputEventAPI> events) {

    }

}