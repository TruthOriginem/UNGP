package data.scripts.ungprules.tags;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;

public interface UNGP_PlayerCharacterStatsSkillTag extends UNGP_SkillTag {
    void apply(MutableCharacterStatsAPI stats);

    void unapply(MutableCharacterStatsAPI stats);
}
