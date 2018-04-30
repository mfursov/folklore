package net.sf.nightworks.model

import java.nio.channels.SocketChannel

/**
 * Descriptor (socket channel) structure.
 */
class Connection(var host: String, var channel: SocketChannel) {
    var ch: CHAR_DATA? = null
    var pc: PC? = null
    var state: ConnectionState = ConnectionState.CON_GET_NAME
    var fcommand: Boolean = false
    var snoop_by: Connection? = null

    val inbuf = StringBuilder()

    /** command to execute */
    var incomm = ""

    /** the last executed command */
    var inlast = ""
    var repeat: Int = 0

    var outbuf = mutableListOf<Any>()
    var showstr_head: String = ""

    /** pointer inside showstr_head */
    var showstr_point: Int = 0
}


//TODO: rename constants
/**  Connected state for a channel. */
enum class ConnectionState {
    CON_GET_NAME,
    CON_CONFIRM_NEW_NAME,
    CON_GET_OLD_PASSWORD,
    CON_GET_NEW_PASSWORD,
    CON_CONFIRM_NEW_PASSWORD,
    CON_GET_NEW_RACE,
    CON_GET_NEW_SEX,
    CON_GET_NEW_CLASS,
    CON_GET_ALIGNMENT,
    CON_DEFAULT_CHOICE,
    CON_GEN_GROUPS,
    CON_PICK_WEAPON,
    CON_READ_IMOTD,
    CON_READ_MOTD,
    CON_BREAK_CONNECT,
    CON_ROLL_STATS,
    CON_ACCEPT_STATS,
    CON_PICK_HOMETOWN,
    CON_GET_ETHOS,
    CON_CREATE_DONE,
    CON_READ_NEWBIE,
    CON_REMORTING,
    CON_PLAYING
}

/** Active player linked to char data */
class PC(
        /** Active connection */
        val con: Connection,

        /** Original character for this player */
        val o_ch: CHAR_DATA,

        /** Active character */
        var ch: CHAR_DATA
)