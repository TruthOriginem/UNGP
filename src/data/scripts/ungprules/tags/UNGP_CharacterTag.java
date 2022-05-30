package data.scripts.ungprules.tags;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;


/**
 * Could be replaced by {@link UNGP_PlayerCharacterStatsSkillTag}
 */
@Deprecated
public interface UNGP_CharacterTag {
    /**
     * 只会在更新缓存执行一次
     *
     * @param stats
     */
    void applyPlayerCharacterStats(MutableCharacterStatsAPI stats);

    void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats);
}
