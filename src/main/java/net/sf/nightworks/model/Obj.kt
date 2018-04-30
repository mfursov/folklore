package net.sf.nightworks.model

/**
 * One object.
 */
class Obj {
    var contains = mutableListOf<Obj>()
    var in_obj: Obj? = null
    var on: Obj? = null
    var carried_by: CHAR_DATA? = null
    var extra_desc = EXTRA_DESC_DATA()
    var affected: Affect? = null
    lateinit var pIndexData: ObjProto
    var in_room: ROOM_INDEX_DATA? = null
    var enchanted: Boolean = false
    var owner: String? = null
    var name = ""
    var short_desc = ""
    var description = ""
    lateinit var item_type: ItemType
    var weaponType: WeaponType = WeaponType.None
    var extra_flags: Long = 0
    /** How this object can be worn */
    var wear_flags: Long = 0
    /** Where is object right now */
    var wear_loc: Wear = Wear.None
    var weight: Int = 0
    var cost: Int = 0
    var level: Int = 0
    var condition: Int = 0
    lateinit var material: String
    var timer: Int = 0
    val value = LongArray(5)
    var progtypes: Long = 0
    var from: String = ""
    var altar: Int = 0
    var pit: Int = 0
    var extracted: Boolean = false
    var water_float: Int = 0
}
