package net.sf.nightworks.model

enum class ItemType(
        private val type: Int,
        private val codeName: String
) {
    /* item type list */
    None(0, "none"),
    Light(1, "light"),
    Scroll(2, "scroll"),
    Wand(3, "wand"),
    Staff(4, "staff"),
    Weapon(5, "weapon"),
    Treasure(8, "treasure"),
    Armor(9, "armor"),
    Potion(10, "potion"),
    Clothing(11, "clothing"),
    Furniture(12, "furniture"),
    Trash(13, "trash"),
    Container(15, "container"),
    DrinkContainer(17, "drink"),
    Key(18, "key"),
    Food(19, "food"),
    Money(20, "money"),
    Boat(22, "boat"),
    CorpseNpc(23, "npc_corpse"),
    CorpsePc(24, "pc_corpse"),
    Fountain(25, "fountain"),
    Pill(26, "pill"),
    Protect(27, "protect"),
    Map(28, "map"),
    Portal(29, "portal"),
    WarpStone(30, "warp_stone"),
    RoomKey(31, "room_key"),
    Gem(32, "gem"),
    Jewelry(33, "jewelry"),
    Jukebox(34, "jukebox"),
    Tattoo(35, "tattoo");

    val displayName get() = codeName.replace('_', ' ')

    companion object {
        fun of(v: String, def: (v: String) -> ItemType) = values().firstOrNull { v == it.codeName } ?: def(v)
        fun of(v: Int, def: (v: Int) -> ItemType) = values().firstOrNull { v == it.type } ?: def(v)
    }
}
