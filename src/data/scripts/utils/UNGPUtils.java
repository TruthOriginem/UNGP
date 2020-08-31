package data.scripts.utils;

import com.fs.starfarer.api.combat.ShipAPI;

public class UNGPUtils {
    public static final int PLAYER = 0;
    public static boolean isPlayerShip(ShipAPI ship){
        return ship.getOwner() == PLAYER;
    }
}
