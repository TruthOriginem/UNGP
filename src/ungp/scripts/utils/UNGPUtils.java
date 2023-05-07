package ungp.scripts.utils;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UNGPUtils {
    public static final String EMPTY = "[EMPTY]";

    public static final int PLAYER = 0;

    public static boolean isPlayerShip(ShipAPI ship) {
        return ship.getOwner() == PLAYER;
    }

    public static boolean isEnemyShip(ShipAPI ship) {
        return ship.getOwner() == 1;
    }

    public static void clearDuplicatedIdsInList(List<String> ids) {
        Set<String> set = new HashSet<>(ids);
        ids.clear();
        ids.addAll(set);
    }

    public static boolean isEmpty(String target) {
        return target == null || target.isEmpty() || target.contentEquals(EMPTY);
    }
}
