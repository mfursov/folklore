package net.sf.nightworks.model

enum class Sex(val id: Int, val title: String, val he_she: String, val him_her: String, val his_her: String) {
    None(0, "none", "it", "it", "its"),
    Male(1, "male", "he", "him", "his"),
    Female(2, "female", "she", "her", "her");

    fun isFemale() = this == Female
    fun isNotFemale() = !isFemale()
    fun isMale() = this == Male

    companion object {
        fun of(v: String, def: (s: String) -> Sex) = values().firstOrNull({ it.title == v }) ?: def(v)
        fun of(v: Int) = values().firstOrNull({ it.ordinal == v })
    }
}