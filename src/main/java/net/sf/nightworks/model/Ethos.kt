package net.sf.nightworks.model

enum class Ethos(val title: String, val prefix: String) {
    Lawful("Lawful", "Law"),
    Neutral("Neutral", "Neut"),
    Chaotic("Chaotic", "Cha");

    fun isLawful() = this == Lawful
    fun isNeutral() = this == Neutral
    fun isChaotic() = this == Chaotic

    companion object {
        fun fromInt(idx: Int) = if (idx <= 0 || idx >= values().size) Ethos.Neutral else values()[idx]
        fun fromString(lcStr: String) = values().first { it.title.toLowerCase() == lcStr }
    }
}