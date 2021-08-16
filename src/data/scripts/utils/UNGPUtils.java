package data.scripts.utils;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.Sys;

public class UNGPUtils {
    private static long lastTime = getTime();

    //milli
    private static long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    public static float getDeltaTime() {
        long thisTime = getTime();
        int deltaTime = (int) (thisTime - lastTime);
        lastTime = thisTime;
        return deltaTime / 1000f;
    }

    public static final int PLAYER = 0;

    public static boolean isPlayerShip(ShipAPI ship) {
        return ship.getOwner() == PLAYER;
    }
}
