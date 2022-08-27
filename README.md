# README #

Unofficial New Game Plus(UNGP) is a mod for the game [Starsector](http://fractalsoftworks.com). The mod provides a
chance that player could inherit credits and blueprints, bringing them to the next save. And the specialist mode is
the 'main' content that player could choose different kinds of 'buff' or 'debuff' to modify the game experience.

## For Modders ##

For modders who want to implement UNGP stuffs like rules and backgrounds, please make this mod as a library first.

### How to implement rules in your mod ###

Take these files as a template:

* Rule items file(data/campaign/UNGP_rules.csv)
* Rule plugin file(src/data/scripts/ungprules/impl/\*/\*)

### How to implement backgrounds in your mod ### 

Take these files as a template:

* Background items file(data/campaign/UNGP_backgrounds.csv)
* Background plugin file(src/data/scripts/ungpbackgrounds/impl/\*)