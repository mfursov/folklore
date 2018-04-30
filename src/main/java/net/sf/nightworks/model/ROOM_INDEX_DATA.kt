package net.sf.nightworks.model

/**
 * Room type.
 */
class ROOM_INDEX_DATA {
    /** Characters in room */
    val people = mutableListOf<CHAR_DATA>()
    /** Objects in room */
    var objects = mutableListOf<Obj>()

    var extra_descr: EXTRA_DESC_DATA? = null
    lateinit var area: Area
    val exit = arrayOfNulls<EXIT_DATA>(6)
    lateinit var name: String
    var description: String = ""
    var owner: String? = null
    var vnum: Int = 0
    var room_flags: Long = 0
    var light: Int = 0
    var sector_type: Int = 0
    var heal_rate: Int = 0
    var mana_rate: Int = 0
    var history: ROOM_HISTORY_DATA? = null
    var aff_next: ROOM_INDEX_DATA? = null
    var affected: Affect? = null
    var affected_by: Long = 0
}
