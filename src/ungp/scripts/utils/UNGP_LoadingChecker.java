package ungp.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.Description;

import java.util.ArrayList;
import java.util.List;

public class UNGP_LoadingChecker {
    private static final String ATTENTION_INFO = "Attention: Any unauthorized modifications to the mods will be considered as a blatant violation of the mod author's rights." +
            "\nYes, UNGP is not compatible with Fairy Empire, you know what I mean.";
    private static boolean checkedMods;

    public static void runCheckProcess() {
        checkMods();
    }

    public static void checkMods() {
        if (checkedMods) return;
        boolean shouldThrow = check();
        if (shouldThrow) {
            throw new RuntimeException(ATTENTION_INFO);
        }
        checkedMods = true;
    }

    private static final List<String> BAN_MOD_CONSTANTS = new ArrayList<>();
    private static final List<String> BAN_MOD_PATHS = new ArrayList<>();
    private static final List<String> BAN_MOD_SHIPS = new ArrayList<>();

    static {
        BAN_MOD_CONSTANTS.add("fairyempire");
        BAN_MOD_CONSTANTS.add("fairyempire");
        BAN_MOD_CONSTANTS.add("N127S");
        BAN_MOD_PATHS.add("data.scripts.FairyEmpireModPlugin");
        BAN_MOD_PATHS.add("data.scripts.world.FairyEmpireGen");
        BAN_MOD_SHIPS.add("FYL_frigatesheep");
        BAN_MOD_SHIPS.add("FYL_frigate");
        BAN_MOD_SHIPS.add("FYL_destroyer");
        BAN_MOD_SHIPS.add("FYL_destroyerlight");
        BAN_MOD_SHIPS.add("FYL_carrierlight");
        BAN_MOD_SHIPS.add("FYL_destroyerMK2");
        BAN_MOD_SHIPS.add("FYL_cruiserlight");
        BAN_MOD_SHIPS.add("FYL_cruiser");
        BAN_MOD_SHIPS.add("FYL_cruiserarmor");
        BAN_MOD_SHIPS.add("FYL_carrierbattlecruiser");
        BAN_MOD_SHIPS.add("FYL_carrier");
        BAN_MOD_SHIPS.add("HGN_cruiser");
        BAN_MOD_SHIPS.add("FYL_Cargoship");
        BAN_MOD_SHIPS.add("FYL_fuelCargoship");
        BAN_MOD_SHIPS.add("FYL_heavycruiser");
        BAN_MOD_SHIPS.add("FYL_battlecruiser");
        BAN_MOD_SHIPS.add("FYL_battleship");
        BAN_MOD_SHIPS.add("FYL_battleshipturret");
        BAN_MOD_SHIPS.add("FYL_battlecruiserarmor");
        BAN_MOD_SHIPS.add("FYL_hugeCargoship");
        BAN_MOD_SHIPS.add("FYL_battleship09A");
        BAN_MOD_SHIPS.add("FYL_engineering");
        BAN_MOD_SHIPS.add("FYL_battleshipcarrier");
        BAN_MOD_SHIPS.add("FYL_hugeCargoshipfuel");
        BAN_MOD_SHIPS.add("FYL_battleshipheavy");
        BAN_MOD_SHIPS.add("FYL_destroysuper");
        BAN_MOD_SHIPS.add("FYL_supercarrier");
        BAN_MOD_SHIPS.add("FYL_supercarrierblack");
        BAN_MOD_SHIPS.add("FYL_dreadnaught");
        BAN_MOD_SHIPS.add("FYL_hugecarrier");
        BAN_MOD_SHIPS.add("FYL_dreadnaughtgod");
        BAN_MOD_SHIPS.add("FYL_dreadnaughtstar");
        BAN_MOD_SHIPS.add("FYL_hugecarrierMK3");
        BAN_MOD_SHIPS.add("FYL_supershipioncannon");
        BAN_MOD_SHIPS.add("FYL_supership");
        BAN_MOD_SHIPS.add("FYL_supershipMK6");
        BAN_MOD_SHIPS.add("FYL_supershipMK5");
        BAN_MOD_SHIPS.add("FYL_carrierSUPER");
        BAN_MOD_SHIPS.add("VGR_cruiser");
        BAN_MOD_SHIPS.add("XAO_cruiserAA");
        BAN_MOD_SHIPS.add("HGN_battlecruiser");
        BAN_MOD_SHIPS.add("VGR_battlecruiser");
        BAN_MOD_SHIPS.add("TAI_battleship");
        BAN_MOD_SHIPS.add("VGR_battleship");
        BAN_MOD_SHIPS.add("VGR_battleshipMK2");
        BAN_MOD_SHIPS.add("VGR_heavybattleship");
        BAN_MOD_SHIPS.add("XAO_destroyer");
        BAN_MOD_SHIPS.add("XAO_battlecruiser");
        BAN_MOD_SHIPS.add("XAO_battlecruisermissle");
        BAN_MOD_SHIPS.add("HGN_battleship");
        BAN_MOD_SHIPS.add("XAO_carrier");
        BAN_MOD_SHIPS.add("VGR_battleshipMK2X2");
        BAN_MOD_SHIPS.add("VGR_carrierhuge");
        BAN_MOD_SHIPS.add("VGR_carriersuper");
        BAN_MOD_SHIPS.add("XAO_CargoshipBC");
        BAN_MOD_SHIPS.add("XAO_terminalBC");
        BAN_MOD_SHIPS.add("XAO_dreadnaughtstar");
        BAN_MOD_SHIPS.add("VGR_dreadnaught");
        BAN_MOD_SHIPS.add("TAI_dreadnaught");
        BAN_MOD_SHIPS.add("ALOS_superbattleshipW");
        BAN_MOD_SHIPS.add("ALOS_superbattleship");
        BAN_MOD_SHIPS.add("ALOS_supership");
        BAN_MOD_SHIPS.add("SUZ_dreadnaught");
        BAN_MOD_SHIPS.add("XAO_battleship");
        BAN_MOD_SHIPS.add("VGR_supership");
        BAN_MOD_SHIPS.add("VGR_supershipMK3");
        BAN_MOD_SHIPS.add("HEG_dreadnaught");
        BAN_MOD_SHIPS.add("XAO_supermissleship");
        BAN_MOD_SHIPS.add("XAO_dreadnaughtBSM");
        BAN_MOD_SHIPS.add("XAO_supercargoship");
        BAN_MOD_SHIPS.add("XAO_CargoshipFFTH");
        BAN_MOD_SHIPS.add("BUS_dreadnaught");
        BAN_MOD_SHIPS.add("BUS_supership");
        BAN_MOD_SHIPS.add("BUS_frigate");
        BAN_MOD_SHIPS.add("BUS_cruiser");
        BAN_MOD_SHIPS.add("BUS_carrier");
        BAN_MOD_SHIPS.add("BUS_battleship");
        BAN_MOD_SHIPS.add("BUS_hugecarrier");
        BAN_MOD_SHIPS.add("BUS_destroyer");
    }

    private static boolean check() {
//        if (getCodeString().hashCode() != -1475221603) {
//            throw new RuntimeException("Modifying the code? Are you actually doing this?");
//        }

        if (Global.getSettings().getModManager().isModEnabled(BAN_MOD_CONSTANTS.get(0))) {
            return true;
        }
        Description description = Global.getSettings().getDescription(BAN_MOD_CONSTANTS.get(1), Description.Type.FACTION);
        if (description.hasText1() || description.hasText2() || description.hasText3()) {
            return true;
        }
        description = Global.getSettings().getDescription(BAN_MOD_CONSTANTS.get(2), Description.Type.CUSTOM);
        if (description.hasText1() || description.hasText2() || description.hasText3()) {
            return true;
        }
        ClassLoader classLoader = Global.getSettings().getScriptClassLoader();
        for (String className : BAN_MOD_PATHS) {
            if (isClassFound(className, classLoader)) {
                return true;
            }
        }
        for (String hullID : BAN_MOD_SHIPS) {
            try {
                if (Global.getSettings().getHullSpec(hullID) != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean isClassFound(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static String getCodeString() {
        return BAN_MOD_CONSTANTS.toString() + BAN_MOD_PATHS + BAN_MOD_SHIPS + ATTENTION_INFO;
    }
}