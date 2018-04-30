package net.sf.nightworks.model

import net.sf.nightworks.DT

enum class AttackType(val code: String, val noun: String, val damage: DT) {
    None("none", "hit", DT.None),
    Slice("slice", "slice", DT.Slash),
    Stab("stab", "stab", DT.Pierce),
    Slash("slash", "slash", DT.Slash),
    Whip("whip", "whip", DT.Slash),
    Claw("claw", "claw", DT.Slash),
    Blast("blast", "blast", DT.Bash),
    Pound("pound", "pound", DT.Bash),
    Crush("crush", "crush", DT.Bash),
    Grep("grep", "grep", DT.Slash),
    Bit("bite", "bite", DT.Pierce),
    Pierce("pierce", "pierce", DT.Pierce),
    Suction("suction", "suction", DT.Bash),
    Beating("beating", "beating", DT.Bash),
    Digestion("digestion", "digestion", DT.Acid),
    Charge("charge", "charge", DT.Bash),
    Slap("slap", "slap", DT.Bash),
    Punch("punch", "punch", DT.Bash),
    Wrath("wrath", "wrath", DT.Energy),
    Magic("magic", "magic", DT.Energy),
    Divine("divine", "divine power", DT.Holy),
    Cleave("cleave", "cleave", DT.Slash),
    Scratch("scratch", "scratch", DT.Pierce),
    Peck("peck", "peck", DT.Pierce),
    PeckB("peckb", "peck", DT.Bash),
    Chop("chop", "chop", DT.Slash),
    Sting("sting", "sting", DT.Pierce),
    Smash("smash", "smash", DT.Bash),
    ShockingBite("shbite", "shocking bite", DT.Lightning),
    FlamingBite("flbite", "flaming bite", DT.Fire),
    FreezingBite("frbite", "freezing bite", DT.Cold),
    AcidicBite("acbite", "acidic bite", DT.Acid),
    Chomp("chomp", "chomp", DT.Pierce),
    LifeDrain("drain", "life drain", DT.Negative),
    Thrust("thrust", "thrust", DT.Pierce),
    Slime("slime", "slime", DT.Acid),
    Shock("shock", "shock", DT.Lightning),
    Thwack("thwack", "thwack", DT.Bash),
    Flame("flame", "flame", DT.Fire),
    Chill("chill", "chill", DT.Cold),
    Kick("kick", "kick", DT.Bash),
    Bash("bash", "bash", DT.Bash),
    Holy("holy", "hit", DT.Holy),
    Negative("negative", "hit", DT.Negative),
    Light("light", "hit", DT.Light),
    Lightning("lightning", "hit", DT.Lightning);

    companion object {
        fun of(v: String, def: (s: String) -> AttackType) = values().firstOrNull { it.code == v } ?: def(v)
        fun of(idx: Long) = values()[idx.toInt()]
    }
}
