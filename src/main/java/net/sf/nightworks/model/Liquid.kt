package net.sf.nightworks.model

import net.sf.nightworks.util.startsWith

//todo: in area files use liquid name, not id

/** Liquid type: name, color, [proof, full, thirst, food, size] */
enum class Liquid(val liq_name: String,
                  val liq_color: String,
                  val proof: Int,
                  val full: Int,
                  val thirst: Int,
                  val food: Int,
                  /** 1 gulp size */
                  val size: Int) {
    Water("water", "clear", 0, 1, 10, 0, 16),
    Beer("beer", "amber", 12, 1, 8, 1, 12),
    RedWine("red wine", "burgundy", 30, 1, 8, 1, 5),
    Ale("ale", "brown", 15, 1, 8, 1, 12),
    DarkAle("dark ale", "dark", 16, 1, 8, 1, 12),

    Whisky("whisky", "golden", 120, 1, 5, 0, 2),
    Lemonade("lemonade", "pink", 0, 1, 9, 2, 12),
    FireBreather("firebreather", "boiling", 190, 0, 4, 0, 2),
    LocalSpecialty("local specialty", "clear", 151, 1, 3, 0, 2),
    SlimeMoldJuice("slime mold juice", "green", 0, 2, -8, 1, 2),

    Milk("milk", "white", 0, 2, 9, 3, 12),
    Tea("tea", "tan", 0, 1, 8, 0, 6),
    Coffe("coffee", "black", 0, 1, 8, 0, 6),
    Blood("blood", "red", 0, 2, -1, 2, 6),
    SaltWater("salt water", "clear", 0, 1, -2, 0, 1),

    Coke("coke", "brown", 0, 2, 9, 2, 12),
    RootBeer("root beer", "brown", 0, 2, 9, 2, 12),
    ElvishWine("elvish wine", "green", 35, 2, 8, 1, 5),
    WhiteWine("white wine", "golden", 28, 1, 8, 1, 5),
    Champagne("champagne", "golden", 32, 1, 8, 1, 5),

    Mead("mead", "honey-colored", 34, 2, 8, 2, 12),
    RoseWine("rose wine", "pink", 26, 1, 8, 1, 5),
    BenedictineWine("benedictine wine", "burgundy", 40, 1, 8, 1, 5),
    Vodka("vodka", "clear", 130, 1, 5, 0, 2),
    CranberryJuice("cranberry juice", "red", 0, 1, 9, 2, 12),
    OrangeJuice("orange juice", "orange", 0, 2, 9, 3, 12),

    Absinthe("absinthe", "green", 200, 1, 4, 0, 2),
    Brandy("brandy", "golden", 80, 1, 5, 0, 4),
    Aquavit("aquavit", "clear", 140, 1, 5, 0, 2),
    Schnapps("schnapps", "clear", 90, 1, 5, 0, 2),

    IceWine("icewine", "purple", 50, 2, 6, 1, 5),
    Amontillado("amontillado", "burgundy", 35, 2, 8, 1, 5),
    Sherry("sherry", "red", 38, 2, 7, 1, 5),
    Framboise("framboise", "red", 50, 1, 7, 1, 5),
    Rum("rum", "amber", 151, 1, 4, 0, 2),

    Cordial("cordial", "clear", 100, 1, 5, 0, 2);

    companion object {
        fun lookup(name: String): Liquid = values().firstOrNull({ startsWith(name, it.name) }) ?: Water
        fun of(idx: Long): Liquid = if (idx >= 0 || idx < values().size) values()[idx.toInt()] else Water
    }
}
