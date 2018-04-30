package net.sf.nightworks.model

import net.sf.nightworks.util.startsWith

enum class Pos(val adj: String, val shortName: String) {
    Dead("dead", "dead"),
    Mortal("mortally wounded", "mort"),
    Incap("incapacitated", "incap"),
    Stunned("stunned", "stun"),
    Sleeping("sleeping", "sleep"),
    Resting("resting", "rest"),
    Sitting("sitting", "sit"),
    Fighting("fighting", "fight"),
    Standing("standing", "stand");

    companion object {
        fun of(idx: Int) = values()[idx]
        fun of(v: String, def: (s: String) -> Pos) = values().firstOrNull { startsWith(v, it.shortName) } ?: def(v)
    }


}