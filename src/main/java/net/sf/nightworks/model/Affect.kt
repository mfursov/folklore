package net.sf.nightworks.model

import net.sf.nightworks.Race
import net.sf.nightworks.Skill

/** An affect. */
class Affect {
    var next: Affect? = null
    var where: Int = 0
    lateinit var type: Skill
    var level: Int = 0
    var duration: Int = 0
    var location: Apply = Apply.None
    var modifier: Int = 0
    var raceModifier: Race? = null
    var bitvector: Long = 0

    fun assignValuesFrom(source: Affect) {
        next = source.next
        where = source.where
        type = source.type
        level = source.level
        duration = source.duration
        location = source.location
        modifier = source.modifier
        raceModifier = source.raceModifier
        bitvector = source.bitvector
    }

    companion object {
        fun of(id: Int, def: (id: Int) -> Apply): Apply = Apply.values().firstOrNull { it.id == id } ?: def(id)
    }
}

enum class Apply(val id: Int, val locName: String) {
    None(0, "none"),
    Str(1, "strength"),
    Dex(2, "dexterity"),
    Intelligence(3, "intelligence"),
    Wis(4, "wisdom"),
    Con(5, "constitution"),
    Cha(6, "charisma"),
    Class(7, "class"),
    Level(8, "level"),
    Age(9, "age"),
    Height(10, "height"),
    Weight(11, "weight"),
    Mana(12, "mana"),
    Hit(13, "hp"),
    Move(14, "move"),
    Gold(15, "gold"),
    Exp(16, "experience"),
    Ac(17, "armor class"),
    Hitroll(18, "hit roll"),
    Damroll(19, "damage roll"),
    Saves(20, "saves"),
    SavingPara(20, "save vs para"),
    SavingRod(21, "save vs rod"),
    SavingPetri(22, "save vs petrification"),
    SavingBreath(23, "save vs breath"),
    SavingSpell(24, "save vs spell"),
    SpellAffect(25, "spell"),
    Size(26, "size"),

    // never in files, only in runtime
    RoomHeal(-1, "room heal"),
    RoomMana(-1, "room mana"),
}
