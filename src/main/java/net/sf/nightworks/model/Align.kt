package net.sf.nightworks.model


enum class Align(val title: String, val lcTitle: String, val prefix: String) {
    Good("Good", "good", "Good"),
    Neutral("Neutral", "neutral", "Neut"),
    Evil("Evil", "evil", "Evil");

    fun isGood() = this == Good
    fun isNeutral() = this == Neutral
    fun isEvil() = this == Evil

    companion object {
        fun get(score: Int) = when {
            score >= 350 -> Align.Good
            score <= -350 -> Align.Evil
            else -> Align.Neutral
        }

        fun of(v: String) = values().firstOrNull { it.title == v }

        fun isGood(alignment: Int) = alignment >= 350
        fun isEvil(alignment: Int) = alignment <= -350
        fun isNeutral(alignment: Int) = !isGood(alignment) && !isEvil(alignment)
    }
}

