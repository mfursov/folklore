package net.sf.nightworks.model

enum class Size(val title: String) {
    Tiny("tiny"),
    Small("small"),
    Medium("medium"),
    Large("large"),
    Huge("huge"),
    Giant("giant"),
    Gargantuan("gargantuan");

    companion object {
        fun of(v: String, def: (s: String) -> Size? = { null }) = values().firstOrNull { it.title == v } ?: def(v)
    }

    operator fun plus(mod: Int) = values()[Math.min(Math.max(0, ordinal + mod), values().size - 1)]
    operator fun minus(mod: Int) = plus(-mod)
    operator fun times(mod: Int) = ordinal * mod
    operator fun minus(size: Size) = this.ordinal - size.ordinal

    fun victimDamageModifier() = when (this) {
        Size.Tiny -> 1.5
        Size.Small -> 1.3
        Size.Medium -> 1.0
        Size.Large -> 0.9
        Size.Huge -> 0.7
        Size.Giant -> 0.5
        Size.Gargantuan -> 0.45
    }
}