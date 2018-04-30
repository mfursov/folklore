package net.sf.nightworks.model

import net.sf.nightworks.Skill

enum class WeaponType(val dbName: String, val skill: Skill? = null, private val tName: String? = null) {
    None("none", null, "unknown"),
    Exotic("exotic"),
    Sword("sword", Skill.sword),
    Dagger("dagger", Skill.dagger),
    Staff("staff", Skill.spear, "spear/staff"),
    Mace("mace", Skill.mace, "mace/club"),
    Axe("axe", Skill.axe),
    Flail("flail", Skill.flail),
    Whip("whip", Skill.whip),
    Polearm("polearm", Skill.polearm),
    Bow("bow", Skill.bow),
    Arrow("arrow", Skill.arrow),
    Lance("lance", Skill.lance);

    val typeName: String get() = tName ?: dbName

    companion object {
        fun of(s: String, def: (s: String) -> WeaponType) = values().firstOrNull { it.dbName == s } ?: def(s)
    }
}