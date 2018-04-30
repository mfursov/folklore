package net.sf.nightworks.model

/** Prototype for an object. */
class ObjProto {
    var extra_descr: EXTRA_DESC_DATA? = null
    var affected: Affect? = null
    var new_format: Boolean = false
    var name: String = ""
    lateinit var short_descr: String
    lateinit var description: String
    var vnum: Int = 0
    var reset_num: Int = 0
    var material: String = ""
    lateinit var item_type: ItemType
    var weaponType: WeaponType =WeaponType.None
    var extra_flags: Long = 0
    var wear_flags: Long = 0
    var level: Int = 0
    var condition: Int = 0
    var count: Int = 0
    var weight: Int = 0
    var cost: Int = 0
    val value = LongArray(5)
    var progtypes: Long = 0
    var limit: Int = 0
    var oprogs: OPROG_DATA = OPROG_DATA()
}
