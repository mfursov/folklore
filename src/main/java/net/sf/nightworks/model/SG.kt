package net.sf.nightworks.model

/** Skill Group */
enum class SG(val shortName: String) {
    None("none"),
    WeaponMaster("weaponsmaster"),
    Attack("attack"),
    Beguiling("beguiling"),
    Benedictions("benedictions"),
    Combat("combat"),
    Creation("creation"),
    Curative("curative"),
    Detection("detection"),
    Draconian("draconian"),
    Enchantment("enchantment"),
    Enhancement("enhancement"),
    Harmful("harmful"),
    Healing("healing"),
    Illusion("illusion"),
    Maladictions("maladictions"),
    Protective("protective"),
    Transportation("transportation"),
    Weather("weather"),
    FightMaster("fightmaster"),
    SuddenDeath("suddendeath"),
    Meditation("meditation"),
    Cabal("cabal"),
    Defensive("defensive"),
    Wizard("wizard");

    companion object {
        fun lookup(name: String) = values().firstOrNull({ name.startsWith("group_") && name.startsWith(it.shortName, 6) })
        fun of(name: String) = values().firstOrNull({ it.shortName == name })
    }
}
