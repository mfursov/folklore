package net.sf.nightworks.model

import net.sf.nightworks.VNum

/**
 * Area definition.
 */
class Area(
        val name: String,
        val writer: String,
        val credits: String,

        var age: Int,
        var nPlayers: Int,
        var low_range: Int,
        var high_range: Int,
        val min_vnum: VNum,
        val max_vnum: VNum,

        var empty: Boolean,
        var count: Int,
        var resetMessage: String = "",

        var area_flag: Long  = 0,

        var reset_first: RESET_DATA? = null
)
