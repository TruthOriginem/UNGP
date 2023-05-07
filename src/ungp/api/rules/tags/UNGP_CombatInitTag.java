package ungp.api.rules.tags;


import com.fs.starfarer.api.combat.CombatEngineAPI;

/**
 * Called when {@link CombatEngineAPI} is inited
 */
public interface UNGP_CombatInitTag {
    void init(CombatEngineAPI engine);
}
