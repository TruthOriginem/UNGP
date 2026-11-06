# README #

Unofficial New Game Plus(UNGP) is a mod for the game [Starsector](http://fractalsoftworks.com). The mod provides a
chance that player could inherit credits and blueprints, bringing them to the next save. And the specialist mode is
the 'main' content that player could choose different kinds of 'buff' or 'debuff' to modify the game experience.

## For Modders ##

For modders who want to implement UNGP stuffs like rules and backgrounds, please make this mod as a library first.

### How to implement rules in your mod ###

Take these files as a template:

* Rule items file(data/campaign/UNGP_rules.csv)
* Rule plugin file(src/ungp/impl/rules/\*/\*)

### How to implement backgrounds in your mod ### 

Take these files as a template:

* Background items file(data/campaign/UNGP_backgrounds.csv)
* Background plugin file(src/ungp/impl/backgrounds/impl/\*)

### How to block combat rules in the next battle ###

The public API `ungp.api.UNGP_CombatRulesAPI` lets campaign code block all combat specialist rules for the next campaign battle:

```java
UNGP_CombatRulesAPI.blockInNextBattle();       // set the block, saved in the campaign
UNGP_CombatRulesAPI.isBlockedInNextBattle();   // query the pending block
UNGP_CombatRulesAPI.cancelBlockInNextBattle(); // cancel the pending block
```

The block is stored in the save and consumed by the next real campaign battle. It does not affect simulated battles or auto-resolve, and calling it outside the campaign (e.g. from a mission) does nothing.

Note that this only blocks the combat rules applied by `UNGP_SpecialistCombatPlugin` at battle start. Effects already applied through other channels are not reverted, for example campaign fleet/member buffs (`UNGP_PlayerFleetMemberTag`), player character or ship skill effects (such as the cruiser deployment point reduction from `UNGP_CruisingHunting`), and campaign battle listeners.