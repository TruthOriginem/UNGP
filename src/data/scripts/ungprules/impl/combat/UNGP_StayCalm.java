package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;

public class UNGP_StayCalm extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final String DATA_KEY = "UNGP_calm_data";
    private static final float PREDICT_TIME = 2f;
    private static final float HIT_POINTS_THRESHOLD = 0.2f;
    private static final float TIME_MULTIPLIER = 0.5f;
    private static final float COOL_DOWN = 15f;
    private static final float FADE_IN_SPEED = 10f;
    private static final float FADE_OUT_SPEED = 5f;
    private static final Color FRAME_COLOR = new Color(160, 0, 0);

    private enum CalmState {
        IDLE,
        ACTIVE,
        COOLDOWN
    }

    private static final class CalmData {
        CalmState state = CalmState.IDLE;
        float elapsed = 0f;
        float effectLevel = 1f;
        ShipAPI affectedShip;

        void advance(CombatEngineAPI engine, float amount) {
            elapsed += amount;
            if (state == CalmState.ACTIVE) {
                effectLevel = Misc.interpolate(effectLevel, TIME_MULTIPLIER, FADE_IN_SPEED * amount);
                engine.getTimeMult().modifyMult(DATA_KEY, effectLevel);
                if (affectedShip != null)
                    affectedShip.getMutableStats().getTimeMult().modifyMult(DATA_KEY, 1f / effectLevel);
                if (elapsed > PREDICT_TIME) {
                    stop();
                }
            } else if (state == CalmState.COOLDOWN) {
                effectLevel = Misc.interpolate(effectLevel, 1f, FADE_IN_SPEED * amount);
                engine.getTimeMult().modifyMult(DATA_KEY, effectLevel);
                if (affectedShip != null)
                    affectedShip.getMutableStats().getTimeMult().modifyMult(DATA_KEY, 1f / effectLevel);
                if (elapsed > COOL_DOWN) {
                    state = CalmState.IDLE;
                    elapsed = 0f;
                    engine.getTimeMult().unmodify(DATA_KEY);
                    if (affectedShip != null)
                        affectedShip.getMutableStats().getTimeMult().unmodify(DATA_KEY);
                }
            }
        }

        void setAffectedShip(ShipAPI ship) {
            if (affectedShip != null) {
                affectedShip.getMutableStats().getTimeMult().unmodify(DATA_KEY);
            }
            affectedShip = ship;
        }

        void start() {
            state = CalmState.ACTIVE;
            elapsed = 0f;
            Global.getSoundPlayer().playUISound("UNGP_staycalm_start", 1f, 1f);
        }

        void stop() {
            state = CalmState.COOLDOWN;
            elapsed = 0f;
            Global.getSoundPlayer().playUISound("UNGP_staycalm_end", 1f, 1f);
        }

        float getRemainTime() {
            switch (state) {
                case IDLE:
                    return 0f;
                case ACTIVE:
                    return PREDICT_TIME - elapsed;
                case COOLDOWN:
                    return COOL_DOWN - elapsed;
            }
            return 0f;
        }
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(PREDICT_TIME);
        if (index == 1) return getPercentString(HIT_POINTS_THRESHOLD * 100f);
        if (index == 2) return getPercentString((1f - TIME_MULTIPLIER) * 100f);
        if (index == 3) return getFactorString(PREDICT_TIME);
        if (index == 4) return getFactorString(COOL_DOWN);
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        ShipAPI player = engine.getPlayerShip();
        CalmData data = getDataInEngine(engine, DATA_KEY);
        if (data == null) {
            data = new CalmData();
            putDataInEngine(engine, DATA_KEY, data);
        }
        if (engine.isPaused()) amount = 0f;
        data.advance(engine, amount);
        if (data.state == CalmState.IDLE) {
            // 当玩家的船存在时
            if (player != null) {
                data.setAffectedShip(player);
                // 每0.2秒判断一次
                if ((int) (data.elapsed * 10f) % 2 == 1) {
                    float damage = 0f;
                    for (Iterator<Object> iter = engine.getMissileGrid().getCheckIterator(player.getLocation(), 3000f, 3000f); iter.hasNext(); ) {
                        final MissileAPI tmp = (MissileAPI) iter.next();
                        if (tmp.didDamage() || tmp.isFizzling() || tmp.getOwner() == player.getOwner()) continue;
                        Vector2f predicted = new Vector2f(tmp.getVelocity());
                        predicted.scale(PREDICT_TIME);
                        Vector2f.add(predicted, tmp.getLocation(), predicted);
                        float distance = MathUtils.getDistance(player, predicted);
                        boolean consider = false;
                        if (distance <= 0f) {
                            consider = true;
                        } else if (tmp.isGuided() && distance <= player.getCollisionRadius() * 2f) {
                            consider = true;
                        }
                        if (consider) {
                            damage += tmp.getDamageAmount();
                        }
                    }
                    if (damage > player.getMaxHitpoints() * HIT_POINTS_THRESHOLD) {
                        data.start();
                        final CalmData finalData = data;
                        engine.addLayeredRenderingPlugin(new BaseCombatLayeredRenderingPlugin() {
                            private float alphaLevel = 0f;

                            @Override
                            public void advance(float amount) {
                                if (finalData.state == CalmState.ACTIVE) {
                                    alphaLevel = Misc.interpolate(alphaLevel, 0.6f, FADE_IN_SPEED * amount);
                                } else {
                                    alphaLevel = Misc.interpolate(alphaLevel, 0f, FADE_OUT_SPEED * amount);
                                }
                            }

                            @Override
                            public boolean isExpired() {
                                return finalData.state == CalmState.IDLE;
                            }

                            @Override
                            public float getRenderRadius() {
                                return 10000f;
                            }

                            @Override
                            public void render(CombatEngineLayers layer, ViewportAPI viewport) {
                                if (alphaLevel > 0.05f) {
                                    Vector2f center = viewport.getCenter();
                                    float alpha = alphaLevel * viewport.getAlphaMult();
                                    float width = viewport.getVisibleWidth() * 1.1f;
                                    float height = viewport.getVisibleHeight() * 1.1f;
                                    SpriteAPI sprite = Global.getSettings().getSprite("fx", "UNGP_frame");
                                    sprite.setColor(FRAME_COLOR);
                                    sprite.setWidth(width);
                                    sprite.setHeight(height);
                                    sprite.setAdditiveBlend();
                                    sprite.setAlphaMult(alpha);
                                    sprite.renderAtCenter(center.x, center.y);
                                }
                            }
                        });
                    }
                }
            }
        } else {
            String format;
            boolean isNegative;
            if (data.state == CalmState.ACTIVE) {
                format = rule.getExtra1();
                isNegative = false;
            } else {
                format = rule.getExtra2();
                isNegative = true;
            }
            String content = String.format(format, String.format("%.1f", data.getRemainTime()));
            engine.maintainStatusForPlayerShip(DATA_KEY, rule.getSpritePath(), rule.getName(),
                    content, isNegative);
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
    }
}
