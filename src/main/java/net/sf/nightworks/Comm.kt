package net.sf.nightworks

import net.sf.nightworks.act.info.do_help
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.model.Align
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Connection
import net.sf.nightworks.model.ConnectionState
import net.sf.nightworks.model.Ethos
import net.sf.nightworks.model.HomeTown
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.PC
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Sex
import net.sf.nightworks.model.Wiznet
import net.sf.nightworks.model.get_stat_alias
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_HERO
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_QUESTOR
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.UPPER
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.crypt
import net.sf.nightworks.util.currentTimeSeconds
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.isSpace
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.upfirst
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import java.util.Date
import java.util.Formatter

private val echo_off_telnet_command = byteArrayOf(IAC, WILL, TELOPT_ECHO)
private val echo_on_telnet_command = byteArrayOf(IAC, WONT, TELOPT_ECHO)
private val go_ahead_telnet_command = byteArrayOf(IAC, GA)

object Server {
    lateinit var selector: Selector
    lateinit var channel: ServerSocketChannel

    /* Game is wizlocked */
    var wizlock: Boolean = false

    /* Shutdown  */
    var newlock: Boolean = false

    /** Shutdown  */
    var shudown: Boolean = false

    /** Exit Code */
    var nw_exit: Int = 0
}


fun initServer(port: Int) {
    with(Server) {
        selector = SelectorProvider.provider().openSelector()
        channel = ServerSocketChannel.open()
        channel.configureBlocking(false)
        channel.socket().bind(InetSocketAddress(port))
    }
}


fun nightworks_engine() {
    current_time = currentTimeSeconds()
    with(Server) {
        /* Main loop */
        channel.register(selector, SelectionKey.OP_ACCEPT)

        while (!shudown) {
            val periodStart = currentTimeSeconds()

            handle_new_connections()

            //process input from all buffers of all chars
            handle_input()

            // Autonomous game motion.
            update_handler()

            // process output
            handle_output()

            wait_pulse(periodStart)

        }
    }
}

private fun wait_pulse(periodStart: Long) {
    current_time = currentTimeSeconds()
    val waitMillis = (periodStart + 1000 / PULSE_PER_SECOND) - current_time * 1000L
    if (waitMillis > 0) {
        try {
            Thread.sleep(waitMillis)
        } catch (e: InterruptedException) {
            //todo:
            e.printStackTrace()
        }
    }
}

private fun Server.handle_new_connections() {
    val nKeys = selector.selectNow()
    if (nKeys > 0) {
        val readyKeys = selector.selectedKeys()

        var it = readyKeys.iterator()
        while (it.hasNext()) {
            val key = it.next()
            if (key.isAcceptable) {
                it.remove()
                val nextReady = key.channel() as ServerSocketChannel
                val channel = nextReady.accept()
                init_descriptor(channel)
            }
        }

        // process input from all connections
        it = readyKeys.iterator()
        while (it.hasNext()) {
            val key = it.next()
            if (key.isReadable) {
                it.remove()
                val connectionOk = read_from_descriptor(key.attachment() as Connection)
                if (!connectionOk) {
                    key.channel().close()
                }
            }
        }
        assert(readyKeys.isEmpty())
    }
}

private fun handle_output() {
    for (c in Index.CONNECTIONS) {
        var close = !c.channel.isConnected
        if (!close && (c.fcommand || !c.outbuf.isEmpty())) {
            if (!process_output(c, true)) {
                close = true
            }
        }
        if (close) {
            val pc = c.pc
            if (pc != null && pc.ch.level > 1) {
                save_char_obj(pc.ch)
            }
            close_socket(c)
        }
    }
}

private fun handle_input() {
    for (c in Index.CONNECTIONS) {
        c.fcommand = false
        val pc = c.pc
        if (pc != null) {
            if (pc.ch.daze > 0) {
                pc.ch.daze--
            }
            if (pc.ch.wait > 0) {
                pc.ch.wait--
                continue
            }
        }
        read_from_buffer(c)
        if (c.incomm.isNotEmpty()) {
            c.fcommand = true
            if (pc != null) {
                stop_idling(pc)
            }
            when {
                c.showstr_point != 0 -> show_string(c, c.incomm)
                c.state == ConnectionState.CON_PLAYING -> substitute_alias(c, c.incomm)
                else -> nanny(c, c.incomm)
            }
            c.incomm = ""
        }
    }
}

private fun init_descriptor(channel: SocketChannel) {
    channel.configureBlocking(false)
    val readKey = channel.register(Server.selector, SelectionKey.OP_READ)

    //Create a new descriptor.
    val host = channel.socket().inetAddress.hostAddress
    val c = Connection(host, channel)
    c.state = ConnectionState.CON_GET_NAME

    // Init descriptor data.
    readKey.attach(c)
    Index.CONNECTIONS.add(c)

    // Send the greeting.
    send_help_greeting(c)
}


fun close_socket(c: Connection) {
    if (!c.outbuf.isEmpty()) {
        process_output(c, false)
    }

    val snoop_by = c.snoop_by
    if (snoop_by != null) {
        write_to_buffer(snoop_by, "Your victim has left the game.\n")
    }

    Index.CONNECTIONS
            .filter { it.snoop_by == c }
            .forEach { it.snoop_by = null }

    val pc = c.pc
    if (pc != null) {
        val ch = pc.ch
        log_string("Closing link to ${ch.name}.")

        val pet = ch.pet
        if (pet != null) {
            char_to_room(pet, get_room_index(ROOM_VNUM_LIMBO))
            extract_char(pet, true)
        }

        if (c.state == ConnectionState.CON_PLAYING) {
            if (!IS_IMMORTAL(ch)) act("\$n has lost \$s link.", ch, null, null, TO_ROOM)
            wiznet("Net death has claimed \$N.", ch, null, Wiznet.Links, null, 0)
        } else {
            free_char(ch)
        }
    }

    Index.CONNECTIONS.remove(c)

    try {
        c.channel.close()
    } catch (e: IOException) {
        //todo:
        e.printStackTrace()
    }

}

private val byteBuffer = ByteBuffer.allocate(1024)

private fun read_from_descriptor(con: Connection): Boolean {
    try {
        byteBuffer.clear()
        val nBytes = con.channel.read(byteBuffer)
        if (nBytes == -1) {
            return false
        }
        if (nBytes == 0) {
            return true
        }
        // Check for overflow
        if (con.inbuf.length >= MAX_INPUT_LENGTH) {
            log_string(con.host + " input overflow!")
            write_to_channel(con.channel, "\n*** PUT A LID ON IT!!! ***\n")
            return true
        }
        byteBuffer.flip()
        while (byteBuffer.remaining() > 0) {
            var c = byteBuffer.get()
            if (c.toInt() != 0) {
                if (c == IAC) { //Interpret As Command
                    if (byteBuffer.remaining() == 0) {
                        break
                    }
                    c = byteBuffer.get()
                    if (c != IAC) { // this is a real command
                        if (byteBuffer.remaining() == 0) {
                            break
                        }
                        c = byteBuffer.get()
                        if (c == WILL) { // if command is WILL echo DON'T
                            write_to_buffer(con, byteArrayOf(IAC, WONT, c)) // we do not want any command (do not support)
                        } else if (c == DO) {// if command is DO echo WON'T
                            write_to_buffer(con, byteArrayOf(IAC, DONT, c))
                        }
                        continue
                    }
                }
                if (c == '\r'.toByte() || c == '\b'.toByte()) {
                    continue
                }
                con.inbuf.append(c.toChar())
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return true
}

/** Transfer one line from input buffer to input line. */
private fun read_from_buffer(con: Connection) {
    /* Hold horses if pending command already. */
    if (con.incomm.isNotEmpty()) return

    /* Look for at least one new line.*/
    val lineEnd = con.inbuf.indexOf("\n")
    if (lineEnd == -1) return
    con.incomm = con.inbuf.substring(0, lineEnd)
    con.inbuf.delete(0, lineEnd + 1)

    if (con.incomm.isEmpty()) {
        con.incomm = " "
    }
    if (con.incomm.length > MAX_INPUT_LENGTH) {
        con.incomm = con.incomm.substring(0, MAX_INPUT_LENGTH)
        write_to_channel(con.channel, "Line too long.\n")
    }

    /*
    * Deal with bozos with #repeat 1000 ...
    */
    if (con.incomm.length > 1 || con.incomm[0] == '!') {
        if (con.incomm[0] != '!' && con.incomm != con.inlast) {
            con.repeat = 0
        } else {
            if (++con.repeat >= 25)
            /* corrected by chronos */ {
                log_string(con.host + " input spamming!")
                val pc = con.pc
                if (pc != null) {
                    wiznet("SPAM SPAM SPAM ${pc.ch.name} spamming, and OUT!", pc.ch, null, Wiznet.Spam, null, get_trust(pc.ch))
                    wiznet("[${pc.ch.name}]'s  Inlast:[${con.inlast}] Incomm:[${con.incomm}]!", pc.ch, null, Wiznet.Spam, null, get_trust(pc.ch))
                    con.repeat = 0
                    write_to_channel(con.channel, "\n*** PUT A LID ON IT!!! ***\n")
                    con.incomm = "quit"
                    close_socket(con)
                    return
                }
            }
        }
    }

    /* Do '!' substitution. */
    if (con.incomm[0] == '!') {
        con.incomm = con.inlast
    } else {
        con.inlast = con.incomm
    }
}

/** Some specials added by KIO */
private fun process_output(con: Connection, fPrompt: Boolean): Boolean {
    // Bust a prompt.
    val pc = con.pc
    if (!Server.shudown && con.showstr_point != 0) {
        write_to_buffer(con, "\r[Hit Return to continue]\n")
    } else if (fPrompt && !Server.shudown && con.state == ConnectionState.CON_PLAYING) {
        if (pc != null) {
            /* battle prompt */
            val victim = pc.ch.fighting
            if (victim != null && can_see(pc.ch, victim)) {
                val percent = if (victim.max_hit > 0) victim.hit * 100 / victim.max_hit else -1
                val wound = when {
                    percent >= 100 -> "is in perfect health."
                    percent >= 90 -> "has a few scratches."
                    percent >= 75 -> "has some small but disgusting cuts."
                    percent >= 50 -> "is covered with bleeding wounds."
                    percent >= 30 -> "is gushing blood."
                    percent >= 15 -> "is writhing in agony."
                    percent >= 0 -> "is convulsing on the ground."
                    else -> "is nearly dead."
                }
                write_to_buffer(con, upfirst(if (victim.pcdata == null) victim.short_desc else victim.name) + " " + wound + "\n")
            }


            if (!IS_SET(pc.o_ch.comm, COMM_COMPACT)) {
                write_to_buffer(con, "\n")
            }


            if (IS_SET(pc.o_ch.comm, COMM_PROMPT)) {
                bust_a_prompt(pc)
            }

            if (IS_SET(pc.o_ch.comm, COMM_TELNET_GA)) {
                write_to_buffer(con, go_ahead_telnet_command)
            }
        }
    }

    // Snoop-o-rama.
    val snoop_by = con.snoop_by
    if (snoop_by != null) {
        if (pc != null) {
            write_to_buffer(snoop_by, pc.ch.name)
        }
        write_to_buffer(snoop_by, "> ")
        write_to_buffer(snoop_by, con.outbuf)
    }
    @Suppress("LoopToCallChain")
    for (o in con.outbuf) {
        if (!write_to_channel(con.channel, o)) {
            break
        }
    }
    con.outbuf.clear()
    return con.outbuf.isEmpty()
}


private val dir_name_char = charArrayOf('N', 'E', 'S', 'W', 'U', 'D')

/** Bust a prompt (player settable prompt) coded by Morgenes for Aldara Mud */
private fun bust_a_prompt(PC: PC) {
    val ch = PC.ch
    if (ch.prompt.isEmpty()) {
        send_to_char("<${ch.hit}hp ${ch.mana}m ${ch.move}mv> ${ch.prefix}", ch)
        return
    }

    val buf = StringBuilder()
    var p = 0
    while (p < ch.prompt.length) {
        var c = ch.prompt[p]
        if (c != '%') {
            buf.append(c)
            p++
            continue
        }
        p++
        c = if (p < ch.prompt.length) ch.prompt[p] else 0.toChar()
        val text: CharSequence = when (c) {
            'e' -> {
                val doors = StringBuilder()
                for (door in 0..5) {
                    val pExit = ch.room.exit[door]
                    if (pExit != null
                            && (can_see_room(ch, pExit.to_room) || IS_AFFECTED(ch, AFF_INFRARED) && !IS_AFFECTED(ch, AFF_BLIND))
                            && !IS_SET(pExit.exit_info, EX_CLOSED)) {
                        doors.append(dir_name_char[door])
                    }
                }
                if (doors.isEmpty()) "none" else doors
            }
            'c' -> "\n"
        // added from here by KIO
            'n' -> ch.name
            'S' -> upfirst(ch.sex.title)
            'y' -> if (ch.hit >= 0) (100 * ch.hit / Math.max(1, ch.max_hit)).toString() + "%%" else "BAD!!"
            'o' -> {
                val victim = ch.fighting
                if (victim != null) {
                    if (victim.hit >= 0) {
                        "{Y" + 100 * victim.hit / Math.max(1, victim.max_hit) + "%{x"
                    } else {
                        "{RBAD!!{x"
                    }
                } else {
                    "None"
                }
            }
            'T' -> (if (time_info.hour % 12 == 0) 12 else time_info.hour % 12).toString() + " " + (if (time_info.hour >= 12) "pm" else "am")
            'h' -> ch.hit.toString()
            'H' -> ch.max_hit.toString()
            'm' -> ch.mana.toString()
            'M' -> ch.max_mana.toString()
            'v' -> ch.move.toString()
            'V' -> ch.max_move.toString()
            'x' -> ch.exp.toString()
            'X' -> "" + (if (ch.pcdata == null) 0 else exp_to_level(ch))
            'g' -> ch.gold.toString()
            's' -> ch.silver.toString()
            'a' -> ch.align.lcTitle
            'r' -> if (ch.pcdata != null && IS_SET(ch.act, PLR_HOLYLIGHT) || !IS_AFFECTED(ch, AFF_BLIND) && !room_is_dark(ch))
                ch.room.name
            else
                "darkness"
            'R' -> if (IS_IMMORTAL(ch)) ch.room.vnum.toString() else " "
            'z' -> if (IS_IMMORTAL(ch)) ch.room.area.name else " "
            '%' -> "%%"
            else -> " "
        }
        buf.append(text)
        p++
    }
    write_to_buffer(PC.con, buf)

    if (ch.prefix.isNotEmpty()) {
        write_to_buffer(PC.con, ch.prefix)
    }
}

/** Append onto an output buffer.*/
private fun write_to_buffer(snoopBy: Connection, outBuf: List<Any>) {
    snoopBy.outbuf.addAll(outBuf)
}

private fun write_to_buffer(d: Connection, data: ByteArray) {
    d.outbuf.add(data)
}

fun write_to_buffer(d: Connection, txt: CharSequence) {
    // Initial \n if needed.
    if (d.outbuf.size == 0 && !d.fcommand) {
        d.outbuf.add("\n")
    }

    // Expand the buffer as needed.
    if (d.outbuf.size > 100) {
        bug("Buffer overflow. Closing.")
        close_socket(d)
        return
    }
    d.outbuf.add(txt.toString())//create a copy if text buffer
}

/**
 * Lowest level output function.
 * Write a block of text to the file descriptor.
 * If this gives errors on very long blocks (like 'ofind all'),
 * try lowering the max block size.
 */
private val outBuf = StringBuilder(1000)

private fun write_to_channel(channel: SocketChannel, data: Any): Boolean {
    try {
        val buf: ByteBuffer
        if (data is CharSequence) {
            val text = data.toString()
            var i = 0
            while (i < text.length) {
                val c = text[i]
                when (c) {
                    '{' -> {
                        i++
                        if (i < text.length) {
                            val c2 = text[i]
                            if (c2 == '{') {
                                outBuf.append("{")
                            } else {
                                val colorMask = toColor(c2)
                                outBuf.append(colorMask)
                            }
                        }
                    }
                    '\n' -> outBuf.append("\r\n")
                    else -> outBuf.append(c)
                }
                i++
            }
            buf = ByteBuffer.wrap(outBuf.toString().toByteArray())
            outBuf.setLength(0)
        } else {
            buf = ByteBuffer.wrap(data as ByteArray)
        }
        channel.write(buf)
        //            desc.socket().getOutputStream().flush();
    } catch (e: IOException) {
        log_string("write_to_channel: ${e.message}")
        return false
    }
    return true
}


/** Check if there is another registration in progress */
private fun check_name_connected(con: Connection, name: String) = Index.CONNECTIONS.any { it != con && it.ch?.name == name }

/**
 * Deal with sockets that haven't logged in yet.
 */
private fun nanny(con: Connection, args: String) {
    var argument = args
    argument = argument.trim { it <= ' ' }

    val arg = StringBuilder()
    val c0 = if (argument.isEmpty()) 'X' else argument[0].toUpperCase()
    when (con.state) {
        ConnectionState.CON_GET_NAME -> {
            if (argument.isEmpty()) {
                close_socket(con)
                return
            }
            val name = upfirst(argument)
            if (!check_parse_name(name)) {
                write_to_buffer(con, "Illegal name, try another.\nName: ")
                return
            }

            val ch = load_char_obj(con, name)
            con.ch = ch

            if (IS_SET(ch.act, PLR_DENY)) {
                log_string("Denying access to " + argument + "@" + con.host + ".")
                write_to_buffer(con, "You are denied access.\n")
                close_socket(con)
                return
            }

            if (Server.wizlock && !IS_HERO(ch)) {
                write_to_buffer(con, "The game is wizlocked.\n")
                close_socket(con)
                return
            }

            if (!IS_IMMORTAL(ch) && !IS_SET(ch.act, PLR_CANINDUCT)) {
                if (iNumPlayers >= max_oldies && !ch.new_character) {
                    val buf = "\nThere are currently " +
                            "$iNumPlayers players mudding out of a maximum of " +
                            "$max_oldies.\nPlease try again soon.\n"
                    write_to_buffer(con, buf)
                    close_socket(con)
                    return
                }

                if (iNumPlayers >= max_newbies && !ch.new_character) {
                    val buf = "\nThere are currently $iNumPlayers players mudding.New player creation is limited to \n" +
                            "when there are less than $max_newbies players. Please try again soon.\n"
                    write_to_buffer(con, buf)
                    close_socket(con)
                    return
                }
            }

            if (!ch.new_character) {
                write_to_buffer(con, "Password: ")
                write_to_buffer(con, echo_off_telnet_command)
                con.state = ConnectionState.CON_GET_OLD_PASSWORD
                return
            }
            /* New player */
            if (Server.newlock) {
                write_to_buffer(con, "The game is newlocked.\n")
                close_socket(con)
                return
            }

            if (check_name_connected(con, name)) {
                write_to_buffer(con, "That player is already playing, try another.\nName: ")
                free_char(ch)
                con.ch = null
                con.state = ConnectionState.CON_GET_NAME
            } else {
                do_help(ch, "NAME")
                con.state = ConnectionState.CON_CONFIRM_NEW_NAME
            }
        }
        ConnectionState.CON_GET_OLD_PASSWORD -> {
            var ch = con.ch!!
            val pcdata = ch.pcdata
            pcdata!!
            write_to_buffer(con, "\n")
            if (crypt(argument, ch.name) != pcdata.pwd) {
                write_to_buffer(con, "Wrong password.\n")
                log_string("Wrong password by " + ch.name + "@" + con.host)
                if (ch.endur == 2) {
                    close_socket(con)
                } else {
                    write_to_buffer(con, "Password: ")
                    write_to_buffer(con, echo_off_telnet_command)
                    con.state = ConnectionState.CON_GET_OLD_PASSWORD
                    ch.endur++
                }
                return
            }


            if (pcdata.pwd.isEmpty()) {
                write_to_buffer(con, "Warning! null password!\n")
                write_to_buffer(con, "Please report old password with bug.\n")
                write_to_buffer(con, "Type 'password null <new password>' to fix.\n")
            }
            write_to_buffer(con, echo_on_telnet_command)

            if (check_reconnect(con)) {
                return
            }

            if (check_playing(con, ch.name)) {
                write_to_buffer(con, "That character is already playing.\n")
                write_to_buffer(con, "Do you wish to connect anyway (Y/N)?")
                con.state = ConnectionState.CON_BREAK_CONNECT
                return
            }

            /* Count objects in loaded player file */
            val obj_count = ch.carrying.sumBy { get_obj_realnumber(it) }
            val name = ch.name
            free_char(ch)
            ch = load_char_obj(con, name)
            con.ch = ch

            if (ch.new_character) {
                write_to_buffer(con, "Please login again to create a new character.\n")
                close_socket(con)
                return
            }

            /* Count objects in refreshed player file */
            val obj_count2 = ch.carrying.sumBy { get_obj_realnumber(it) }

            log_string(ch.name + "@" + con.host + " has connected.")

            if (IS_HERO(ch)) {
                do_help(ch, "imotd")
                con.state = ConnectionState.CON_READ_IMOTD
            } else {
                do_help(ch, "motd")
                con.state = ConnectionState.CON_READ_MOTD
            }

            //TODO: unload/save character on re-login

            /** This player tried to use the clone cheat --
             * Log in once, connect a second time and enter only name,
             * drop all and quit with first character, finish login with second.
             * This clones the player's inventory.
             */
            if (obj_count != obj_count2) {
                log_string(ch.name + "@" + con.host + " tried to use the clone cheat.")
                send_to_char("The gods frown upon your actions.\n", ch)
            }
        }
    /* RT code for breaking link */
        ConnectionState.CON_BREAK_CONNECT -> when (c0) {
            'Y' -> {
                val ch = con.ch!!
                for (oldCon in Index.CONNECTIONS) {
                    val oldCh = oldCon.ch
                    if (oldCon == con || oldCh == null) {
                        continue
                    }

                    if (!eq(ch.name, oldCh.name)) {
                        continue
                    }
                    close_socket(oldCon)
                }
                if (check_reconnect(con)) {
                    return
                }
                write_to_buffer(con, "Reconnect attempt failed.\nName: ")
                free_char(ch)
                con.ch = null
                con.state = ConnectionState.CON_GET_NAME
            }

            'N' -> {
                write_to_buffer(con, "Name: ")
                val ch = con.ch
                if (ch != null) {
                    free_char(ch)
                    con.ch = null
                }
                con.state = ConnectionState.CON_GET_NAME
            }

            else -> write_to_buffer(con, "Please type Y or N? ")
        }

        ConnectionState.CON_CONFIRM_NEW_NAME -> when (c0) {
            'Y' -> {
                val ch = con.ch!!
                write_to_buffer(con, "New character.\nGive me a password for " + ch.name + ": ")
                write_to_buffer(con, echo_off_telnet_command)
                con.state = ConnectionState.CON_GET_NEW_PASSWORD
            }

            'N' -> {
                val ch = con.ch!!
                write_to_buffer(con, "Ok, what IS it, then? ")
                free_char(ch)
                con.ch = null
                con.state = ConnectionState.CON_GET_NAME
            }

            else -> write_to_buffer(con, "Please type Yes or No? ")
        }

        ConnectionState.CON_GET_NEW_PASSWORD -> {
            write_to_buffer(con, "\n")

            if (argument.length < 5) {
                write_to_buffer(con, "Password must be at least five characters long.\nPassword: ")
                return
            }

            val ch = con.ch!!
            val pwdNew = crypt(argument, ch.name)
            if (pwdNew.indexOf('~') != -1) {
                write_to_buffer(con, "New password not acceptable, try again.\nPassword: ")
                return
            }

            ch.pcdata!!.pwd = pwdNew
            write_to_buffer(con, "Please retype password: ")
            con.state = ConnectionState.CON_CONFIRM_NEW_PASSWORD
        }

        ConnectionState.CON_CONFIRM_NEW_PASSWORD -> {
            write_to_buffer(con, "\n")
            val ch = con.ch!!
            val pcdata = ch.pcdata!!
            if (crypt(argument, ch.name) != pcdata.pwd) {
                write_to_buffer(con, "Passwords don't match.\nRetype password: ")
                con.state = ConnectionState.CON_GET_NEW_PASSWORD
                return
            }

            write_to_buffer(con, echo_on_telnet_command)
            write_to_buffer(con, "The Nightworks MUD is home to ${Race.races.size} different races with brief descriptions below:\n")
            do_help(ch, "RACETABLE")
            con.state = ConnectionState.CON_GET_NEW_RACE
        }

        ConnectionState.CON_REMORTING -> {
            val ch = con.ch!!
            ch.act = SET_BIT(ch.act, PLR_CANREMORT)
            ch.act = SET_BIT(ch.act, PLR_REMORTED)
            write_to_buffer(con, "As you know, the Nightworks MUD is home to ${Race.races.size} different races...\n")
            do_help(ch, "RACETABLE")
            con.state = ConnectionState.CON_GET_NEW_RACE
        }

        ConnectionState.CON_GET_NEW_RACE -> {
            val ch = con.ch!!
            arg.setLength(0)
            one_argument(argument, arg)

            if (eq(arg.toString(), "help")) {
                arg.setLength(0)
                argument = one_argument(argument, arg)
                if (argument.isEmpty()) {
                    write_to_buffer(con, "The Nightworks MUD is home to ${Race.races.size} different races with brief descriptions below:\n")
                    do_help(ch, "RACETABLE")
                } else {
                    do_help(ch, argument)
                    write_to_buffer(con, "What is your race? (help for more information) ")
                }
            } else {
                val race = Race.lookupByPrefix(argument)
                if (race == null) {
                    write_to_buffer(con, "That is not a valid race.\n")
                    write_to_buffer(con, "The following races are available:\n  ")
                    var i = 0
                    for (r in Race.races) {
                        if (!r.pcRace) {
                            continue
                        }
                        if (i == 9 || i == 15) {
                            write_to_buffer(con, "\n  ")
                        }
                        write_to_buffer(con, "(${r.name})")
                        i++
                    }
                    write_to_buffer(con, "\n")
                    write_to_buffer(con, "What is your race? (help for more information) ")
                } else {
                    ch.race = race
                    ch.race = race
                    ch.mod_stat.fill(0)

                    /* Acon race stat modifiers
  for (i = 0; i < MAX_STATS; i++)
      ch.mod_stat[i] += pc_race_table[race].stats[i];    */

                    /* Acon race modifiers */
                    ch.max_hit += race.hp_bonus
                    ch.hit = ch.max_hit
                    ch.max_mana += race.mana_bonus
                    ch.mana = ch.max_mana
                    ch.practice += race.prac_bonus

                    ch.affected_by = ch.affected_by or race.aff
                    ch.imm_flags = ch.imm_flags or race.imm
                    ch.res_flags = ch.res_flags or race.res
                    ch.vuln_flags = ch.vuln_flags or race.vuln
                    ch.form = race.form
                    ch.parts = race.parts

                    /* acon skills */
                    val pcData = ch.pcdata!!
                    race.skills.forEach { skill ->
                        val sn = skill.ordinal
                        pcData.learned[sn] = 100
                    }
                    /* acon cost */
                    pcData.points = race.points
                    ch.size = race.size
                    write_to_buffer(con, "What is your sex (M/F)? ")
                    con.state = ConnectionState.CON_GET_NEW_SEX
                }
            }
        }

        ConnectionState.CON_GET_NEW_SEX -> {
            val ch = con.ch!!
            val pcData = ch.pcdata!!
            when (c0) {
                'M' -> {
                    ch.sex = Sex.Male
                    pcData.true_sex = Sex.Male
                }
                'F' -> {
                    ch.sex = Sex.Female
                    pcData.true_sex = Sex.Female
                }
                else -> {
                    write_to_buffer(con, "That's not a sex.\nWhat IS your sex? ")
                    return
                }
            }

            do_help(ch, "class help")

            val buf0 = StringBuilder("Select a class:\n[ ")
            val buf1 = StringBuilder("(Continuing:)  ")
            val classes = Clazz.classes
            for (i in classes.indices) {
                val c = classes[i]
                if (class_ok(ch, c)) {
                    if (i < 7) {
                        buf0.append(c.name).append(" ")
                    } else {
                        buf1.append(c.name).append(" ")
                    }
                }
            }
            buf0.append("\n ")
            buf1.append("]:\n ")
            write_to_buffer(con, buf0)
            write_to_buffer(con, buf1)
            write_to_buffer(con, "What is your class (help for more information)? ")
            con.state = ConnectionState.CON_GET_NEW_CLASS
        }

        ConnectionState.CON_GET_NEW_CLASS -> {
            val ch = con.ch!!
            val pcData = ch.pcdata!!
            val iClass = Clazz.lookupByPrefix(argument)
            arg.setLength(0)
            argument = one_argument(argument, arg)

            if (eq(arg.toString(), "help")) {
                if (argument.isEmpty()) {
                    do_help(ch, "class help")
                } else {
                    do_help(ch, argument)
                }
                write_to_buffer(con, "What is your class (help for more information)? ")
                return
            }
            if (iClass == null) {
                write_to_buffer(con, "That's not a class.\nWhat IS your class? ")
                return
            }

            if (!class_ok(ch, iClass)) {
                write_to_buffer(con, "That class is not available for your race or sex.\nChoose again: ")
                return
            }
            ch.clazz = iClass
            pcData.points += iClass.points
            write_to_buffer(con, "You are now ${iClass.name}.\n")

            ch.perm_stat.fill { s -> Math.min(25, number_range(10, 20 + ch.race.stats[s] + ch.clazz.stats[s])) }

            val stats = Formatter().format("Str:%s  Int:%s  Wis:%s  Dex:%s  Con:%s Cha:%s \n Accept (Y/N)? ",
                    get_stat_alias(ch, PrimeStat.Strength),
                    get_stat_alias(ch, PrimeStat.Intelligence),
                    get_stat_alias(ch, PrimeStat.Wisdom),
                    get_stat_alias(ch, PrimeStat.Dexterity),
                    get_stat_alias(ch, PrimeStat.Constitution),
                    get_stat_alias(ch, PrimeStat.Charisma))


            do_help(ch, "stats")
            write_to_buffer(con, "\nNow rolling for your stats (10-20+).\n")
            write_to_buffer(con, "You don't get many trains, so choose well.\n")
            write_to_buffer(con, stats.toString())
            con.state = ConnectionState.CON_ACCEPT_STATS
        }

        ConnectionState.CON_ACCEPT_STATS -> when (c0) {
            'H', '?' -> do_help(con.ch!!, "stats")
            'Y' -> {
                val ch = con.ch!!
                ch.mod_stat.fill(0)
                write_to_buffer(con, "\n")
                if (align_restrict(con, ch) == null) {
                    write_to_buffer(con, "You may be good, neutral, or evil.\n")
                    write_to_buffer(con, "Which alignment (G/N/E)? ")
                    con.state = ConnectionState.CON_GET_ALIGNMENT
                } else {
                    write_to_buffer(con, "[Hit Return to Continue]\n")
                    ch.endur = 100
                    con.state = ConnectionState.CON_PICK_HOMETOWN
                }
            }

            'N' -> {
                val ch = con.ch!!
                ch.perm_stat.fill { s -> Math.min(25, number_range(10, 20 + ch.race.stats[s] + ch.clazz.stats[s])) }
                val stats = Formatter().format("Str:%s  Int:%s  Wis:%s  Dex:%s  Con:%s Cha:%s \n Accept (Y/N)? ",
                        get_stat_alias(ch, PrimeStat.Strength),
                        get_stat_alias(ch, PrimeStat.Intelligence),
                        get_stat_alias(ch, PrimeStat.Wisdom),
                        get_stat_alias(ch, PrimeStat.Dexterity),
                        get_stat_alias(ch, PrimeStat.Constitution),
                        get_stat_alias(ch, PrimeStat.Charisma))

                write_to_buffer(con, stats.toString())
                con.state = ConnectionState.CON_ACCEPT_STATS
            }
            else -> write_to_buffer(con, "Please answer (Y/N)? ")
        }

        ConnectionState.CON_GET_ALIGNMENT -> {
            val ch = con.ch!!
            when (c0) {
                'G' -> {
                    ch.alignment = 1000
                    write_to_buffer(con, "Now your character is good.\n")
                }
                'N' -> {
                    ch.alignment = 0
                    write_to_buffer(con, "Now your character is neutral.\n")
                }
                'E' -> {
                    ch.alignment = -1000
                    write_to_buffer(con, "Now your character is evil.\n")
                }
                else -> {
                    write_to_buffer(con, "That's not a valid alignment.\n")
                    write_to_buffer(con, "Which alignment (G/N/E)? ")
                    return
                }
            }
            write_to_buffer(con, "\n[Hit Return to Continue]\n")
            ch.endur = 100
            con.state = ConnectionState.CON_PICK_HOMETOWN
        }

        ConnectionState.CON_PICK_HOMETOWN -> {
            val ch = con.ch!!
            val buf = "[M]idgaard, [N]ew Thalos${if (ch.isNeutralA()) ", [O]fcol" else ""}?"
            if (ch.endur != 0) {
                ch.endur = 0
                if (!hometown_check(con, ch)) {
                    do_help(ch, "hometown")
                    write_to_buffer(con, buf)
                    con.state = ConnectionState.CON_PICK_HOMETOWN
                    return
                } else {
                    write_to_buffer(con, "[Hit Return to Continue]\n")
                    ch.endur = 100
                    con.state = ConnectionState.CON_GET_ETHOS
                }
            } else {
                if (c0 == 'H' || c0 == '?') {
                    do_help(ch, "hometown")
                    write_to_buffer(con, buf)
                    return
                }
                val town = when (c0) {
                    'M' -> if (hometown_ok(ch, HomeTown.Midgaard)) HomeTown.Midgaard else null
                    'N' -> if (hometown_ok(ch, HomeTown.NewThalos)) HomeTown.NewThalos else null
                    'O' -> if (hometown_ok(ch, HomeTown.NewOfcol)) HomeTown.NewOfcol else null
                    else -> null
                }
                if (town != null) {
                    ch.hometown = town
                    write_to_buffer(con, "Now your hometown is $town.\n")
                } else {
                    write_to_buffer(con, "\nThat is not a valid hometown.\n")
                    write_to_buffer(con, "Which hometown do you want <type help for more info>? ")
                    return
                }
            }
            ch.endur = 100
            write_to_buffer(con, "\n[Hit Return to Continue]\n")
            con.state = ConnectionState.CON_GET_ETHOS
        }

        ConnectionState.CON_GET_ETHOS -> {
            val ch = con.ch!!
            if (ch.endur == 0) {
                when (c0) {
                    'H', '?' -> {
                        do_help(ch, "alignment")
                        return
                    }
                    'L' -> {
                        write_to_buffer(con, "\nNow you are lawful-${ch.align.lcTitle}.\n")
                        ch.ethos = Ethos.Lawful
                    }
                    'N' -> {
                        write_to_buffer(con, "\nNow you are neutral-${ch.align.lcTitle}.\n")
                        ch.ethos = Ethos.Neutral
                    }
                    'C' -> {
                        write_to_buffer(con, "\nNow you are chaotic-${ch.align.lcTitle}.\n")
                        ch.ethos = Ethos.Chaotic
                    }
                    else -> {
                        write_to_buffer(con, "\nThat is not a valid ethos.\n")
                        write_to_buffer(con, "What ethos do you want, (L/N/C) <type help for more info> ?")
                        return
                    }
                }
            } else {
                ch.endur = 0
                var chooseEthos = true
                val classEthos = ch.clazz.ethos
                if (classEthos != null) {
                    ch.ethos = classEthos
                    write_to_buffer(con, "You are " + upfirst(ch.ethos.title) + ".\n")
                    chooseEthos = false
                }
                if (chooseEthos) {
                    write_to_buffer(con, "What ethos do you want, (L/N/C) <type help for more info> ?")
                    con.state = ConnectionState.CON_GET_ETHOS
                    return
                }
            }
            write_to_buffer(con, "\n[Hit Return to Continue]\n")
            con.state = ConnectionState.CON_CREATE_DONE
        }

        ConnectionState.CON_CREATE_DONE -> {
            val ch = con.ch!!
            log_string(ch.name + "@" + con.host + " new player.")
            ch.pcdata!!.learned[Skill.recall.ordinal] = 75
            write_to_buffer(con, "\n")
            do_help(ch, "GENERAL")
            write_to_buffer(con, "[Hit Return to Continue]\n")
            con.state = ConnectionState.CON_READ_NEWBIE
            return
        }
        ConnectionState.CON_READ_NEWBIE -> {
            write_to_buffer(con, "\n")
            do_help(con.ch!!, "motd")
            con.state = ConnectionState.CON_READ_MOTD
            return
        }
        ConnectionState.CON_READ_IMOTD -> {
            write_to_buffer(con, "\n")
            do_help(con.ch!!, "motd")
            con.state = ConnectionState.CON_READ_MOTD
        }

        ConnectionState.CON_READ_MOTD -> {
            val ch = con.ch!!
            val pcdata = ch.pcdata!!
            con.pc = PC(con, ch, ch)
            con.state = ConnectionState.CON_PLAYING

            write_to_buffer(con, "\nWelcome to Multi User Dungeon of Enjoy!!...\n")

            val count = Index.CONNECTIONS.count { it.state == ConnectionState.CON_PLAYING }
            max_on = Math.max(count, max_on)

            iNumPlayers++

            when {
                ch.level == 0 -> {
                    ch.level = 1
                    ch.exp = base_exp(ch)
                    ch.hit = ch.max_hit
                    ch.mana = ch.max_mana
                    ch.move = ch.max_move
                    pcdata.perm_hit = ch.max_hit
                    pcdata.perm_mana = ch.max_mana
                    pcdata.perm_move = ch.max_move
                    ch.train += 3
                    ch.practice += 5
                    pcdata.death = 0

                    set_title(ch, "the " + ch.clazz.getTitle(ch.level, ch.sex))

                    obj_to_char(create_object(get_obj_index_nn(OBJ_VNUM_MAP), 0), ch)
                    obj_to_char(create_object(get_obj_index_nn(OBJ_VNUM_NMAP1), 0), ch)
                    obj_to_char(create_object(get_obj_index_nn(OBJ_VNUM_NMAP2), 0), ch)

                    val mapVnum = when (ch.hometown) {
                        HomeTown.Midgaard -> if (ch.isEvil()) OBJ_VNUM_MAP_MIDGAARD else 0
                        HomeTown.NewThalos -> OBJ_VNUM_MAP_NEW_THALOS
                        HomeTown.Titans -> OBJ_VNUM_MAP_TITAN
                        HomeTown.NewOfcol -> OBJ_VNUM_MAP_NEW_OFCOL
                        HomeTown.OldMidgaard -> OBJ_VNUM_MAP_OLD_MIDGAARD
                    }
                    if (mapVnum != 0) {
                        obj_to_char(create_object(get_obj_index_nn(mapVnum), 0), ch)
                    }

                    pcdata.learned[get_weapon_sn(ch, false)!!.ordinal] = 40

                    char_to_room(ch, get_room_index(ROOM_VNUM_SCHOOL))
                    send_to_char("\n", ch)
                    do_help(ch, "NEWBIE INFO")
                    send_to_char("\n", ch)

                    /* give some bonus time */
                    var l = 0
                    while (l < MAX_TIME_LOG) {
                        pcdata.log_time[l] = 60
                        l++
                    }
                    do_outfit(ch)
                }
                cabal_area_check(ch) -> {
                    val i = when {
                        ch.isGood() -> 0
                        ch.isEvil() -> 2
                        else -> 1
                    }
                    char_to_room(ch, get_room_index(ch.hometown.altar[i]))
                }
                else -> char_to_room(ch, ch.room)
            }

            reset_char(ch)
            if (!IS_IMMORTAL(ch)) {
                act("\$n has entered the game.", ch, null, null, TO_ROOM)
            }
            wiznet("\$N entered the realms.", ch, null, Wiznet.Logins, null, 0)

            if (ch.exp < exp_per_level(ch) * ch.level) {
                ch.exp = ch.level * exp_per_level(ch)
            } else if (ch.exp > exp_per_level(ch) * (ch.level + 1)) {
                ch.exp = (ch.level + 1) * exp_per_level(ch)
                ch.exp -= 10
            }

            if (IS_QUESTOR(ch) && pcdata.questmob == 0) {
                pcdata.nextquest = pcdata.countdown
                pcdata.questobj = 0
                ch.act = REMOVE_BIT(ch.act, PLR_QUESTOR)
            }

            if (IS_SET(ch.act, PLR_NO_EXP)) {
                ch.act = REMOVE_BIT(ch.act, PLR_NO_EXP)
            }
            if (IS_SET(ch.act, PLR_CHANGED_AFF)) {
                ch.act = REMOVE_BIT(ch.act, PLR_CHANGED_AFF)
            }

            for (s in ch.perm_stat.keys) {
                if (ch.perm_stat[s] > 20 + ch.race.stats[s] + ch.clazz.stats[s]) {
                    ch.train += ch.perm_stat[s] - (20 + ch.race.stats[s] + ch.clazz.stats[s])
                    ch.perm_stat[s] = 20 + ch.race.stats[s] + ch.clazz.stats[s]
                }
            }

            do_look(ch, "auto")

            if (ch.gold > 10000 && !IS_IMMORTAL(ch)) {
                send_to_char("You are taxed ${(ch.gold - 10000) / 2} gold to pay for the Mayor's bar.\n", ch)
                ch.gold -= (ch.gold - 10000) / 2
            }


            if (pcdata.bank_g > 400000 && !IS_IMMORTAL(ch)) {
                send_to_char("You are taxed ${pcdata.bank_g - 400000} gold to pay for war expenses of Sultan.\n", ch)
                pcdata.bank_g = 400000
            }


            val pet = ch.pet
            if (pet != null) {
                char_to_room(pet, ch.room)
                act("\$n has entered the game.", pet, null, null, TO_ROOM)
            }

            if (pcdata.confirm_delete) {
                send_to_char("You are given some bonus played time per week.\n", ch)
                pcdata.confirm_delete = false
            }

            do_unread(ch, "login")
        }
        else -> {
            bug("Nanny: bad con.state %d.", con.state)
            close_socket(con)
        }
    }
}

/*
* Parse a name for acceptability.
*/

fun check_parse_name(name: String): Boolean {
    /** Reserved words. */
    if (is_name(name, "all auto immortal self someone something the you demise balance circle loner honor")) {
        return false
    }

    if (eq(capitalize(name), "Chronos")) {
        return false
    }

    /* Length restrictions.*/
    if (name.length < 2 || name.length > 12) {
        return false
    }

    /* Alphanumerics only. */
    var total_caps = 0
    for (i in 0 until name.length) {
        val c = name[i]
        if (!Character.isLetter(c)) {
            return false
        }
        if (Character.isUpperCase(c)) {/* ugly anti-caps hack */
            total_caps++
        }
    }
    if (total_caps > name.length / 2 && name.length < 3) {
        return false
    }

    // Prevent players from naming themselves after mobs.
    return Index.MOB_INDEX.values.none { is_name(name, it.player_name) }
}

/** Look for link-dead player to reconnect. */
private fun check_reconnect(con: Connection): Boolean {
    val cch = con.ch!!
    for (ch in Index.CHARS) {
        val pcdata = ch.pcdata
        if (pcdata != null && con.pc == null && eq(cch.name, ch.name)) {
            free_char(cch)
            con.ch = ch
            ch.timer = 0
            con.state = ConnectionState.CON_PLAYING
            con.pc = PC(con, ch, ch)
            send_to_char("Reconnecting. Type replay to see missed tells.\n", ch)
            if (!IS_IMMORTAL(ch)) {
                act("\$n has reconnected.", ch, null, null, TO_ROOM)
            }
            if (get_light_char(ch) != null) {
                ch.room.light--
            }
            log_string(ch.name + "@" + con.host + " reconnected.")
            wiznet("\$N groks the fullness of \$S link.", ch, null, Wiznet.Links, null, 0)
            return true
        }
    }
    return false
}

/* Check if already playing. */
private fun check_playing(d: Connection, name: String): Boolean {
    return Index.CONNECTIONS.any {
        it.pc != null
                && it != d
                && it.state != ConnectionState.CON_GET_NAME
                && it.state != ConnectionState.CON_GET_OLD_PASSWORD
                && eq(name, it.pc!!.o_ch.name)
    }
}


private fun stop_idling(pc: PC) {
    if (pc.con.state != ConnectionState.CON_PLAYING || pc.o_ch.was_in_room == null || pc.o_ch.room != get_room_index(ROOM_VNUM_LIMBO)) {
        return
    }
    pc.o_ch.timer = 0
    char_from_room(pc.o_ch)
    char_to_room(pc.o_ch, pc.o_ch.was_in_room)
    pc.o_ch.was_in_room = null
    act("\$n has returned from the void.", pc.o_ch, null, null, TO_ROOM)
}

fun get_con(ch: CHAR_DATA) = Index.CONNECTIONS.firstOrNull { it -> it.ch == ch }
fun get_pc(ch: CHAR_DATA) = get_con(ch)?.pc

/** Write to one char. */
fun send_to_char(txt: CharSequence, ch: CHAR_DATA) {
    val con = get_con(ch) ?: return
    write_to_buffer(con, txt)
}

/*** Send a page to one char.*/
fun page_to_char(txt: CharSequence, ch: CHAR_DATA) {
    val con = get_con(ch) ?: return
    if (ch.lines == 0) {
        write_to_buffer(con, txt)
        return
    }
    con.showstr_head = txt.toString()
    con.showstr_point = 0
    show_string(con, "")
}

/** string pager */
private fun show_string(con: Connection, input: String) {
    val buf = StringBuilder()
    one_argument(input, buf)
    if (buf.isNotEmpty()) {
        con.showstr_head = ""
        con.showstr_point = 0
        return
    }
    val show_lines = con.pc?.ch?.lines ?: 0
    var toggle = 1
    var lines = 0
    val buffer = StringBuilder()
    while (true) {
        val c: Char
        if (con.showstr_head.length == con.showstr_point) {
            c = 0.toChar()
        } else {
            c = con.showstr_head[con.showstr_point]
            buffer.append(c)
            if ((c == '\n' || c == '\r')) {
                toggle = -toggle
                if (toggle < 0) {
                    lines++
                }
            }
        }
        if (c.toInt() == 0 || show_lines in 1..lines) {
            write_to_buffer(con, buffer)
            var chk = con.showstr_point
            while (chk < con.showstr_head.length && isSpace(con.showstr_head[chk])) {
                chk++
            }
            if (chk == con.showstr_head.length) {
                con.showstr_head = ""
                con.showstr_point = 0
            }
            return
        }
        con.showstr_point++
    }
}

fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: CHAR_DATA?, arg2: CHAR_DATA?, type: Int) = _act(actStr, room, arg1, arg2, type)
fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: Obj, arg2: CHAR_DATA?, type: Int) = _act(actStr, room, arg1, arg2, type)
fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: String, arg2: CHAR_DATA?, type: Int) = _act(actStr, room, arg1, arg2, type)

fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: CHAR_DATA?, arg2: Obj, type: Int) = _act(actStr, room, arg1, arg2, type)
fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: Obj, arg2: Obj, type: Int) = _act(actStr, room, arg1, arg2, type)
fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: String, arg2: Obj, type: Int) = _act(actStr, room, arg1, arg2, type)

fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: CHAR_DATA?, arg2: String, type: Int) = _act(actStr, room, arg1, arg2, type)
fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: Obj, arg2: String, type: Int) = _act(actStr, room, arg1, arg2, type)
fun act(actStr: String, room: ROOM_INDEX_DATA, arg1: String, arg2: String, type: Int) = _act(actStr, room, arg1, arg2, type)

private fun _act(actStr: String, room: ROOM_INDEX_DATA, arg1: Any?, arg2: Any?, type: Int) {
    val ch = room.people.firstOrNull() ?: return
    _act(actStr, ch, arg1, arg2, type)
}

fun act(actStr: String, ch: CHAR_DATA, arg1: CHAR_DATA?, arg2: CHAR_DATA?, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)
fun act(actStr: String, ch: CHAR_DATA, arg1: Obj, arg2: CHAR_DATA?, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)
fun act(actStr: String, ch: CHAR_DATA, arg1: String, arg2: CHAR_DATA?, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)

fun act(actStr: String, ch: CHAR_DATA, arg1: CHAR_DATA?, arg2: Obj, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)
fun act(actStr: String, ch: CHAR_DATA, arg1: Obj, arg2: Obj, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)
fun act(actStr: String, ch: CHAR_DATA, arg1: String, arg2: Obj, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)

fun act(actStr: String, ch: CHAR_DATA, arg1: CHAR_DATA?, arg2: String, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)
fun act(actStr: String, ch: CHAR_DATA, arg1: Obj, arg2: String, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)
fun act(actStr: String, ch: CHAR_DATA, arg1: String, arg2: String, type: Int, min_pos: Pos = Pos.Resting) = _act(actStr, ch, arg1, arg2, type, min_pos)

private fun _act(actStr: String, ch: CHAR_DATA, arg1: Any?, arg2: Any?, type: Int, min_pos: Pos = Pos.Resting) {
    if (actStr.isEmpty()) {
        return
    }
    val victim = arg2 as? CHAR_DATA
    var to = ch.room.people
    if (type == TO_VICT) {
        if (victim == null) {
            bug("Act: null vch with TO_VICT.")
            return
        }
        to = victim.room.people
    }

    val buf = StringBuilder()
    val name = StringBuilder()
    for (t in to) {
        buf.setLength(0)
        val to_pc = get_pc(t)
        if (to_pc == null || t.position < min_pos) {
            continue
        }

        if (type == TO_CHAR && t != ch) {
            continue
        }
        if (type == TO_VICT && (t != victim || t == ch)) {
            continue
        }
        if (type == TO_ROOM && t == ch) {
            continue
        }
        if (type == TO_NOTVICT && (t == ch || t == victim)) {
            continue
        }

        var ii = 0
        while (ii < actStr.length) {
            var c = actStr[ii]
            if (c != '$') {
                buf.append(c)
                ii++
                continue
            }
            c = actStr[++ii]
            val i: String = when (c) {
                't' -> arg1 as String
                'T' -> arg2 as String
                'n' -> when {
                    is_affected(ch, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> PERS(ch.doppel, t)
                    else -> PERS(ch, t)
                }
                'N' -> when {
                    is_affected(victim, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> PERS(victim!!.doppel, t)
                    else -> PERS(victim, t)
                }
                'e' -> when {
                    is_affected(ch, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> ch.doppel!!.sex.he_she
                    else -> ch.sex.he_she
                }
                'E' -> when {
                    is_affected(victim, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> victim!!.doppel!!.sex.he_she
                    else -> victim!!.sex.he_she
                }
                'm' -> when {
                    is_affected(ch, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> ch.doppel!!.sex.him_her
                    else -> ch.sex.him_her
                }
                'M' -> when {
                    is_affected(victim, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> victim!!.doppel!!.sex.him_her
                    else -> victim!!.sex.him_her
                }
                's' -> when {
                    is_affected(ch, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> ch.doppel!!.sex.his_her
                    else -> ch.sex.his_her
                }
                'S' -> when {
                    is_affected(victim, Skill.doppelganger) && !IS_SET(t.act, PLR_HOLYLIGHT) -> victim!!.doppel!!.sex.his_her
                    else -> victim!!.sex.his_her
                }
                'p' -> when {
                    can_see_obj(t, arg1 as Obj?) -> (arg1 as Obj).short_desc
                    else -> "something"
                }

                'P' -> when {
                    can_see_obj(t, arg2 as Obj?) -> (arg2 as Obj).short_desc
                    else -> "something"
                }

                'd' -> when {
                    arg2 == null || (arg2 as String).length == 0 -> "door"
                    else -> {
                        name.setLength(0)
                        one_argument(arg2, name)
                        name.toString()
                    }
                }
                else -> {
                    bug("Act: bad code %d.", c)
                    " <@@@> "
                }
            }
            buf.append(i)
            ii++
        }
        buf.append("\n")

        /* fix for color prefix and capitalization */
        if (buf[0].toInt() == 0x1B) {
            val n = buf.indexOf("m", 1)
            buf.setCharAt(n + 1, UPPER(buf[n + 1]))
        } else {
            buf.setCharAt(0, UPPER(buf[0]))
        }
        write_to_buffer(to_pc.con, buf)
    }
}

fun log_area_popularity() {
    val file = File("area_stat.txt")
    if (file.exists()) {
        file.delete()
    }
    try {
        FileWriter(nw_config.var_astat_file, true).use { fp ->
            val f = Formatter(fp)
            f.format("\nBooted %s Area popularity statistics (in String  ticks)\n", Date(boot_time))
            for (area in Index.AREAS) {
                if (area.count >= 5000000) {
                    f.format("%-60s overflow\n", area.name)
                } else {
                    f.format("%-60s %d\n", area.name, area.count)
                }
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

}

private fun class_ok(ch: CHAR_DATA, clazz: Clazz): Boolean {
    ch.race.getClassModifier(clazz) ?: return false
    return clazz.sex == null || clazz.sex == ch.sex
}

private fun align_restrict(con: Connection, ch: CHAR_DATA) = when {
    ch.race.align == Align.Good || ch.clazz.align == Align.Good -> {
        write_to_buffer(con, "Your character has good tendencies.\n")
        ch.alignment = 1000 //todo: move to enum
        Align.Good
    }
    ch.race.align == Align.Neutral || ch.clazz.align == Align.Neutral -> {
        write_to_buffer(con, "Your character has neutral tendencies.\n")
        ch.alignment = 0
        Align.Neutral
    }
    ch.race.align == Align.Evil || ch.clazz.align == Align.Evil -> {
        write_to_buffer(con, "Your character has evil tendencies.\n")
        ch.alignment = -1000
        Align.Evil
    }
    else -> null
}

private fun hometown_check(con: Connection, ch: CHAR_DATA): Boolean {
    if (ch.clazz === Clazz.NECROMANCER || ch.clazz === Clazz.VAMPIRE) {
        write_to_buffer(con, "\n")
        write_to_buffer(con, "Your hometown is Old Midgaard, permanently.\n")
        ch.hometown = HomeTown.OldMidgaard
        write_to_buffer(con, "\n")
        return true
    }

    val race = ch.race
    if (race === Race.STORM_GIANT || race === Race.CLOUD_GIANT || race === Race.FIRE_GIANT || race === Race.FROST_GIANT) {
        write_to_buffer(con, "\n")
        write_to_buffer(con, "Your hometown is Valley of Titans, permanently.\n")
        ch.hometown = HomeTown.Titans
        write_to_buffer(con, "\n")
        return true
    }
    return false
}

private fun hometown_ok(ch: CHAR_DATA, t: HomeTown) = ch.isNeutralA() || t != HomeTown.OldMidgaard

private fun send_help_greeting(d: Connection) = write_to_buffer(d, help_greeting)
