package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatInitTag;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UNGP_Headhunting extends UNGP_BaseRuleEffect implements UNGP_CombatTag, UNGP_CombatInitTag {
    public static final float COOL_DOWN = 60f;
    public static final float DAMAGE_BONUS = 15f;
    private float maxDuration = 20f;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        maxDuration = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(21f, 6f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        if (index == 1) return getPercentString(DAMAGE_BONUS);
        if (index == 2) return getFactorString(COOL_DOWN);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        HeadhuntingData headHuntingData = getDataInEngine(engine, "UNGP_headhunting_data");
        if (headHuntingData != null) {
            // Cooling down
            if (headHuntingData.remainCoolDown > 0) {
                headHuntingData.remainCoolDown -= amount;
            }
            headHuntingData.checkInterval.advance(amount);
            if (headHuntingData.checkInterval.intervalElapsed()) {
                CombatFleetManagerAPI playerFleetManager = engine.getFleetManager(FleetSide.PLAYER);
                CombatTaskManagerAPI taskManager = playerFleetManager.getTaskManager(false);
                List<ShipAPI> allTargets = new ArrayList<>();
                for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                    if (assignment.getType() == CombatAssignmentType.INTERCEPT && assignment.getTarget() instanceof DeployedFleetMemberAPI) {
                        ShipAPI target = ((DeployedFleetMemberAPI) assignment.getTarget()).getShip();
                        if (target.isAlive()) {
                            allTargets.add(target);
                        }
                    }
                }
                List<ShipAPI> previousTargets = headHuntingData.previousTargets;
                List<ShipAPI> newTargets = new ArrayList<>(allTargets);
                newTargets.removeAll(previousTargets);
                if (headHuntingData.remainCoolDown <= 0) {
                    if (!headHuntingData.showedRestoreMessage) {
                        headHuntingData.showedRestoreMessage = true;
                        engine.getCombatUI().addMessage(0, Misc.getPositiveHighlightColor(), rule.getExtra2());
                        Global.getSoundPlayer().playUISound("UNGP_headhunting_charge", 1f, 1f);
                    }
                    if (!newTargets.isEmpty()) {
                        headHuntingData.remainCoolDown = COOL_DOWN;
                        headHuntingData.showedRestoreMessage = false;
                        ShipAPI target = newTargets.get(0);
                        engine.addLayeredRenderingPlugin(new HeadhuntingStatusPlugin(target, maxDuration));
                        Global.getSoundPlayer().playUISound("UNGP_headhunting_activate", 1f, 1f);
                        engine.getCombatUI().addMessage(0, target,
                                                        Misc.getNegativeHighlightColor(),
                                                        target.getName() + "(" + target.getHullSpec().getHullNameWithDashClass() + ")",
                                                        Misc.getHighlightColor(),
                                                        rule.getExtra1());
                    }
                }
                previousTargets.clear();
                previousTargets.addAll(allTargets);
            }
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }

    @Override
    public void init(CombatEngineAPI engine) {
        putDataInEngine(engine, "UNGP_headhunting_data", new HeadhuntingData());
    }

    private static class HeadhuntingData {
        private IntervalUtil checkInterval = new IntervalUtil(0.1f, 0.1f);
        private List<ShipAPI> previousTargets = new ArrayList<>();
        private float remainCoolDown = 0f;

        private boolean showedRestoreMessage = false;
    }

    private static class HeadhuntingStatusPlugin extends BaseCombatLayeredRenderingPlugin {
        public static final String HEADHUNTING_HUNTED = "UNGP_headhunting_hunted";
        private ShipAPI target;
        private float duration;
        private boolean isExpired;
        private float elapsed = 0f;

        private static int TEXTURE_ID = -1;


        public HeadhuntingStatusPlugin(ShipAPI target, float duration) {
            this.target = target;
            this.duration = duration;
            this.layer = CombatEngineLayers.BELOW_SHIPS_LAYER;
            if (TEXTURE_ID == -1) {
                SpriteAPI sprite = Global.getSettings().getSprite("fx", "UNGP_headhunting_target_edge");
                TEXTURE_ID = sprite.getTextureId();
            }
        }

        @Override
        public void advance(float amount) {
            elapsed += amount;
            entity.getLocation().set(target.getLocation());
            MutableShipStatsAPI stats = target.getMutableStats();
            if (elapsed > duration) {
                isExpired = true;
                stats.getEnergyDamageTakenMult().unmodify(HEADHUNTING_HUNTED);
                stats.getKineticDamageTakenMult().unmodify(HEADHUNTING_HUNTED);
                stats.getHighExplosiveDamageTakenMult().unmodify(HEADHUNTING_HUNTED);
                stats.getFragmentationDamageTakenMult().unmodify(HEADHUNTING_HUNTED);
                return;
            }
            Global.getCombatEngine().addHitParticle(target.getLocation(), new Vector2f(), 30f, 1f, 0.1f, Color.white);
            float multiplier = 1f + DAMAGE_BONUS / 100f;
            stats.getEnergyDamageTakenMult().modifyMult(HEADHUNTING_HUNTED, multiplier);
            stats.getKineticDamageTakenMult().modifyMult(HEADHUNTING_HUNTED, multiplier);
            stats.getHighExplosiveDamageTakenMult().modifyMult(HEADHUNTING_HUNTED, multiplier);
            stats.getFragmentationDamageTakenMult().modifyMult(HEADHUNTING_HUNTED, multiplier);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            float imageSizeMultiplier = (float) (1f + 0.05f * FastTrig.sin(elapsed * 2f));
            float imageAngle = MathUtils.clampAngle(-15f * elapsed);
            float imageAlphaMult = 1f;
            if (elapsed < 0.5f) {
                imageAlphaMult = elapsed / 0.5f;
            } else if (duration - elapsed < 0.5f) {
                imageAlphaMult = (duration - elapsed) / 0.5f;
            }
            GL11.glPushMatrix();
            GL11.glTranslatef(entity.getLocation().x, entity.getLocation().y, 0);
            GL11.glScalef(imageSizeMultiplier, imageSizeMultiplier, 1f);
            GL11.glRotatef(imageAngle, 0, 0, 1);
            float outerRadius = target.getCollisionRadius();
            float innerRadius = outerRadius - 60f;
            float segments = 60;
            float angle = 360f / segments;
            float texSeg = 4;
            float texPerSeg = texSeg / segments;

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, TEXTURE_ID);
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            Misc.setColor(Color.white, 0.5f * imageAlphaMult);
            {
                for (int i = 0; i <= segments; i++) {
                    float curAngle = angle * i;
                    float curTex = texPerSeg * i;
                    Vector2f innerPoint = MathUtils.getPointOnCircumference(null, innerRadius, curAngle);
                    Vector2f outerPoint = MathUtils.getPointOnCircumference(null, outerRadius, curAngle);
                    GL11.glTexCoord2f(curTex, 0);
                    GL11.glVertex2f(innerPoint.x, innerPoint.y);
                    GL11.glTexCoord2f(curTex, 1);
                    GL11.glVertex2f(outerPoint.x, outerPoint.y);
                }
            }
            GL11.glEnd();

            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);

            GL11.glPopMatrix();
        }

        @Override
        public float getRenderRadius() {
            return target.getCollisionRadius() * 2f;
        }

        @Override
        public boolean isExpired() {
            return isExpired || !target.isAlive();
        }
    }
}
