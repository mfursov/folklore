package net.sf.nightworks.model

enum class Language(val id: Int, val title: String) {
    Common(0, "common"),
    Human(1, "human"),
    Elvish(2, "elvish"),
    Drawvish(3, "dwarvish"),
    Gnomish(4, "gnomish"),
    Giant(5, "giant"),
    Trollish(6, "trollish"),
    Cat(7, "cat");

    companion object {
        fun of(v: String) = values().firstOrNull { it.title == v }
    }
}
