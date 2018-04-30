package net.sf.nightworks.model

import net.sf.nightworks.Race
import net.sf.nightworks.SPEC_FUN

class MOB_INDEX_DATA {
    var spec_fun: SPEC_FUN? = null
    var mprogs: MPROG_DATA = MPROG_DATA()
    var progtypes: Long = 0
    var pShop: Shop? = null
    var vnum: Int = 0
    var group: Int = 0
    var count: Int = 0
    var killed: Int = 0
    lateinit var player_name: String
    lateinit var short_descr: String
    lateinit var long_descr: String
    lateinit var description: String
    var act: Long = 0
    var affected_by: Long = 0
    var level: Int = 0
    var hitroll: Int = 0
    val hit = IntArray(3)
    val mana = IntArray(3)
    val damage = IntArray(3)
    val ac = IntArray(4)
    var attack: AttackType = AttackType.None
    var off_flags: Long = 0
    var imm_flags: Long = 0
    var res_flags: Long = 0
    var vuln_flags: Long = 0
    var start_pos: Pos = Pos.Dead
    var default_pos: Pos = Pos.Dead
    /** 0-2 - as Sex, other value -> random Male/Female */
    var sexOption: Int = 3;
    lateinit var race: Race
    var wealth: Int = 0
    var form: Long = 0
    var parts: Long = 0
    var size: Size = Size.Medium
    var material: String? = null
    var practicer: SG = SG.None
    var alignment: Int = 0
    val align get() = Align.get(alignment)
}
