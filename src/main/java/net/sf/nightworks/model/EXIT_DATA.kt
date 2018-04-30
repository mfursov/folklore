package net.sf.nightworks.model

import net.sf.nightworks.ROOM_VNUM_BLOCKED_EXIT

/**
 * Exit data.
 */
class EXIT_DATA {
    lateinit var to_room: ROOM_INDEX_DATA
    var vnum = ROOM_VNUM_BLOCKED_EXIT
    var exit_info = 0L
    var key = 0
    var keyword = ""
    var description = ""
}
