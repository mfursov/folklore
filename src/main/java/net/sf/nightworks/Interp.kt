package net.sf.nightworks

import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Connection
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.Social
import net.sf.nightworks.model.Wiznet
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.isDigit
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.random
import net.sf.nightworks.util.smash_tilde
import net.sf.nightworks.util.startsWith
import net.sf.nightworks.util.trimSpaces
import java.util.ArrayList
import java.util.Formatter

/* this is a listing of all the commands and command related data */

/* for command types */
const val ML = MAX_LEVEL/* implementor */
const val L1 = MAX_LEVEL - 1  /* creator */
const val L2 = MAX_LEVEL - 2  /* supreme being */
const val L3 = MAX_LEVEL - 3  /* deity */
const val L4 = MAX_LEVEL - 4  /* god */
const val L5 = MAX_LEVEL - 5  /* immortal */
const val L6 = MAX_LEVEL - 6  /* demigod */
const val L7 = MAX_LEVEL - 7  /* angel */
const val L8 = MAX_LEVEL - 8  /* avatar */
const val IM = LEVEL_IMMORTAL /* angel */
const val HE = LEVEL_HERO /* hero */

/**
 * Command logging types.
 */
const val LOG_NORMAL = 0
const val LOG_ALWAYS = 1
const val LOG_NEVER = 2

/**
 * Command table.
 */
val commandsTable = CmdType.values()

/**
 * The main entry point for executing commands.
 * Can be recursively called from 'at', 'order', 'force'.
 */

fun interpret(ch: CHAR_DATA, argument: String, is_order: Boolean) {
    var rest = argument
    /*
    * Strip leading spaces.
    */
    rest = smash_tilde(rest)
    rest = trimSpaces(rest, 0)
    if (rest.isEmpty()) {
        return
    }

    /*
    * Implement freeze command.
    */
    if (ch.pcdata != null && IS_SET(ch.act, PLR_FREEZE)) {
        send_to_char("You're totally frozen!\n", ch)
        return
    }

    /*
    * Grab the command word.
    * Special parsing so ' can be a command,
    * also no spaces needed after punctuation.
    */
    var logLine = rest
    val command = StringBuilder()
    var pos = 0
    val c0 = rest[0]
    rest = if (!Character.isLetter(c0) && !isDigit(c0)) {
        command.append(c0)
        pos++
        trimSpaces(rest, pos)
    } else {
        one_argument(rest, command)
    }

    /*
    * Look for command in command table.
    */
    val trust = get_trust(ch)

    val commandsTable = commandsTable
    var cmd: CmdType? = null
    val commandStr = command.toString()
    for (c in commandsTable) {
        if (!matches(commandStr, c.names) || c.level > trust) {
            continue
        }
        if (!is_order && IS_AFFECTED(ch, AFF_CHARM)) {
            send_to_char("First ask to your beloved master!\n", ch)
            return
        }

        if (IS_AFFECTED(ch, AFF_STUN) && c.extra and CMD_KEEP_HIDE == 0) {
            send_to_char("You are STUNNED to do that.\n", ch)
            return
        }
        /* Come out of hiding for most commands */
        if (IS_AFFECTED(ch, AFF_HIDE) && ch.pcdata != null && c.extra and CMD_KEEP_HIDE == 0) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_HIDE)
            send_to_char("You step out of the shadows.\n", ch)
            act("\$n steps out of the shadows.", ch, null, null, TO_ROOM)
        }

        if (IS_AFFECTED(ch, AFF_FADE) && ch.pcdata != null && c.extra and CMD_KEEP_HIDE == 0) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_FADE)
            send_to_char("You step out of the shadows.\n", ch)
            act("\$n steps out of the shadows.", ch, null, null, TO_ROOM)
        }

        if (IS_AFFECTED(ch, AFF_IMP_INVIS) && ch.pcdata != null && c.position == Pos.Fighting) {
            affect_strip(ch, Skill.improved_invis)
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_IMP_INVIS)
            send_to_char("You fade into existence.\n", ch)
            act("\$n fades into existence.", ch, null, null, TO_ROOM)
        }

        if (IS_AFFECTED(ch, AFF_EARTHFADE) && ch.pcdata != null && c.position == Pos.Fighting) {
            fadeOutToNormal(ch)
        }

        /* prevent ghosts from doing a bunch of commands */
        if (IS_SET(ch.act, PLR_GHOST) && ch.pcdata != null && c.extra and CMD_GHOST == 0) {
            continue
        }

        cmd = c
        break
    }

    /*
    * Log and snoop.
    */
    if (cmd == null || cmd.log == LOG_NEVER) {
        logLine = ""
    }

    if (ch.pcdata != null && IS_SET(ch.act, PLR_LOG) || fLogAll
            || cmd != null && cmd.log == LOG_ALWAYS && logLine.isNotEmpty() && logLine[0] != '\n') {
        val log_buf = "Log " + ch.name + ": " + logLine
        wiznet(log_buf, ch, null, Wiznet.Secure, null, get_trust(ch))
        log_string(log_buf)
    }

    val pc = get_pc(ch)
    val snoop_by = pc?.con?.snoop_by
    if (snoop_by != null) {
        write_to_buffer(snoop_by, "# ")
        write_to_buffer(snoop_by, logLine)
        write_to_buffer(snoop_by, "\n")
    }

    var soc: Social? = null
    val minPos: Pos
    val cmdFlags: Int

    if (cmd == null) {
        //Look for command in socials table.
        soc = Social.lookup(command.toString())
        if (soc == null) {
            send_to_char("Huh?\n", ch)
            return
        }
        if (ch.pcdata != null && IS_SET(ch.comm, COMM_NOEMOTE)) {
            send_to_char("You are anti-social!\n", ch)
            return
        }

        if (IS_AFFECTED(ch, AFF_EARTHFADE) && ch.pcdata != null && soc.minPos == Pos.Fighting) {
            fadeOutToNormal(ch)
        }
        minPos = soc.minPos
        cmdFlags = 0
    } else {
        minPos = cmd.position
        cmdFlags = cmd.extra
    }

    if (ch.pcdata != null) {
        /* Come out of hiding for most commands */
        if (IS_AFFECTED(ch, AFF_HIDE or AFF_FADE) && !IS_SET(cmdFlags, CMD_KEEP_HIDE)) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_HIDE or AFF_FADE)
            send_to_char("You step out of shadows.\n", ch)
            act("\$n steps out of shadows.", ch, null, null, TO_ROOM)
        }

        if (IS_AFFECTED(ch, AFF_IMP_INVIS) && ch.pcdata != null && minPos == Pos.Fighting) {
            affect_strip(ch, Skill.improved_invis)
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_IMP_INVIS)
            send_to_char("You fade into existence.\n", ch)
            act("\$n fades into existence.", ch, null, null, TO_ROOM)
        }
    }

    /*
    * Character not in position for command?
    */
    if (ch.position < minPos) {
        when (ch.position) {
            Pos.Dead -> send_to_char("Lie still; you are DEAD.\n", ch)
            Pos.Mortal, Pos.Incap -> send_to_char("You are hurt far too bad for that.\n", ch)
            Pos.Stunned -> send_to_char("You are too stunned to do that.\n", ch)
            Pos.Sleeping -> send_to_char("In your dreams, or what?\n", ch)
            Pos.Resting -> send_to_char("Nah... You feel too relaxed...\n", ch)
            Pos.Sitting -> send_to_char("Better stand up first.\n", ch)
            Pos.Fighting -> send_to_char("No way!  You are still fighting!\n", ch)
            Pos.Standing -> {
            }
        }
        return
    }
    if (soc != null) {
        interpret_social(ch, rest, soc)
    } else {
        // Dispatch the command
        cmd!!.do_fun(ch, rest)
    }
}

private fun fadeOutToNormal(ch: CHAR_DATA) {
    affect_strip(ch, Skill.earthfade)
    ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_EARTHFADE)
    WAIT_STATE(ch, PULSE_VIOLENCE / 2)
    send_to_char("You slowly fade to your neutral form.\n", ch)
    act("Earth forms \$n in front of you.", ch, null, null, TO_ROOM)
}

private fun matches(command: String, names: Array<String>) = names.any { startsWith(command, it) }

fun interpret_social(ch: CHAR_DATA, argument: String, soc: Social) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        act(soc.noarg_room, ch, null, null, TO_ROOM)
        act(soc.noarg_char, ch, null, null, TO_CHAR)
        return
    }
    val victim = get_char_room(ch, arg.toString())
    when (victim) {
        null -> send_to_char("They aren't here.\n", ch)
        ch -> {
            act(soc.self_room, ch, null, victim, TO_ROOM)
            act(soc.self_char, ch, null, victim, TO_CHAR)
        }
        else -> {
            act(soc.found_novictim, ch, null, victim, TO_NOTVICT)
            act(soc.found_char, ch, null, victim, TO_CHAR)
            act(soc.found_novictim, ch, null, victim, TO_VICT)
            val pc = get_pc(ch)
            if (ch.pcdata != null && victim.pcdata == null && !IS_AFFECTED(victim, AFF_CHARM) && IS_AWAKE(victim) && pc == null) {
                when (random(16)) {
                    in 0..8 -> {
                        act(soc.found_novictim, victim, null, ch, TO_NOTVICT)
                        act(soc.found_char, victim, null, ch, TO_CHAR)
                        act(soc.found_victim, victim, null, ch, TO_VICT)
                    }
                    in 9..12 -> {
                        act("\$n slaps \$N.", victim, null, ch, TO_NOTVICT)
                        act("You slap \$N.", victim, null, ch, TO_CHAR)
                        act("\$n slaps you.", victim, null, ch, TO_VICT)
                    }
                }
            }
        }
    }
}

/**
 * Given a string like 14.foo, return 14 and 'foo'
 */
fun number_argument(argument: String, arg: StringBuilder) = int_argument(argument, arg, '.')

/**
 * Given a string like 14*foo, return 14 and 'foo'
 */
fun multiply_argument(argument: String, arg: StringBuilder) = int_argument(argument, arg, '*')

fun int_argument(argument: String, arg: StringBuilder, c: Char): Int {
    val dot = argument.indexOf(c)
    var number = 1
    if (dot > 0) {
        number = try {
            Integer.parseInt(argument.substring(0, dot))
        } catch (e: NumberFormatException) {
            0
        }

    }
    arg.append(argument, dot + 1, argument.length)
    return number
}


fun do_commands(ch: CHAR_DATA) {
    showAvailableCommands(ch, { cmd -> cmd.level < LEVEL_HERO })
}

private fun showAvailableCommands(ch: CHAR_DATA, cmdCheckFn: (CmdType) -> Boolean) {
    val cmd_table = commandsTable
    val names = cmd_table
            .filter { cmdCheckFn(it) && it.level <= get_trust(ch) }
            .mapTo(ArrayList()) { it.names[0] }
    names.sortWith(Comparator { obj, anotherString -> obj.compareTo(anotherString) })
    val buf = StringBuilder()
    val f = Formatter(buf)
    var col = 0
    for (name in names) {
        f.format("%-12s", name)
        if (++col % 6 == 0) {
            buf.append("\n")
        }
    }

    if (col % 6 != 0) {
        buf.append("\n")
    }
    page_to_char(buf.toString(), ch)
}

fun do_wizhelp(ch: CHAR_DATA) {
    showAvailableCommands(ch, { cmd -> cmd.level >= LEVEL_HERO })
}


/**
 * Does aliasing and other fun stuff
 */
fun substitute_alias(d: Connection, argument: String) {
    var arg = argument
    var alias = 0
    val ch = d.pc?.o_ch ?: d.ch ?: return
    val pcdata = ch.pcdata ?: return
    /* check for prefix */
    if (ch.prefix.isNotEmpty() && !startsWith("prefix", arg)) {
        if (ch.prefix.length + arg.length > MAX_INPUT_LENGTH) {
            send_to_char("Line to long, prefix not processed.\n", ch)
        } else {
            arg = ch.prefix + " " + arg
        }
    }

    if ((pcdata.alias[0] == null) || startsWith("alias", arg) || startsWith("una", arg) || startsWith("prefix", arg)) {
        interpret(ch, arg, false)
        return
    }

    val buf = StringBuilder(arg)

    /* go through the aliases */
    while (alias < nw_config.max_alias) {
        val a = pcdata.alias[alias] ?: break

        if (startsWith(a, arg)) {
            val name = StringBuilder()
            val point = one_argument(arg, name)
            if (a.matches(name.toString().toRegex())) {
                buf.setLength(0)
                buf.append(pcdata.alias_sub[alias])
                buf.append(" ")
                buf.append(point)
                break
            }
            if (buf.length > MAX_INPUT_LENGTH) {
                send_to_char("Alias substitution too long. Truncated.\n", ch)
                buf.delete(MAX_INPUT_LENGTH, buf.length)
            }
        }
        alias++
    }
    interpret(ch, buf.toString(), false)
}

fun do_alia(ch: CHAR_DATA) = send_to_char("I'm sorry, alias must be entered in full.\n", ch)

fun do_alias(ch: CHAR_DATA, argument: String) {
    var rest = smash_tilde(argument)
    val pc = get_pc(ch)
    val rch = pc?.o_ch ?: ch
    val pcdata = rch.pcdata ?: return

    val argb = StringBuilder()
    rest = one_argument(rest, argb)


    if (argb.isEmpty()) {
        if (pcdata.alias[0] == null) {
            send_to_char("You have no aliases defined.\n", ch)
            return
        }
        send_to_char("Your current aliases are:\n", ch)

        var pos = 0
        while (pos < nw_config.max_alias) {
            if (pcdata.alias[pos] == null || pcdata.alias_sub[pos] == null) {
                break
            }

            val buf = "    " + pcdata.alias[pos] + ":  " + pcdata.alias_sub[pos] + "\n"
            send_to_char(buf, ch)
            pos++
        }
        return
    }
    val arg = argb.toString()

    if (startsWith("una", arg) || eq("alias", arg)) {
        send_to_char("Sorry, that word is reserved.\n", ch)
        return
    }

    if (rest.isEmpty()) {
        var pos = 0
        while (pos < nw_config.max_alias) {
            val a = pcdata.alias[pos]
            if (a == null || pcdata.alias_sub[pos] == null) {
                break
            }

            if (eq(arg, a)) {
                val buf = a + " aliases to '" + pcdata.alias_sub[pos] + "'.\n"
                send_to_char(buf, ch)
                return
            }
            pos++
        }

        send_to_char("That alias is not defined.\n", ch)
        return
    }

    if (startsWith(rest, "delete") || startsWith(rest, "prefix")) {
        send_to_char("That shall not be done!\n", ch)
        return
    }

    var pos = 0
    while (pos < nw_config.max_alias) {
        val a = pcdata.alias[pos] ?: break

        if (eq(arg, a))
        /* redefine an alias */ {
            pcdata.alias_sub[pos] = rest
            send_to_char("$arg is now aliased to '$rest'.\n", ch)
            return
        }
        pos++
    }

    if (pos >= nw_config.max_alias) {
        send_to_char("Sorry, you have reached the alias limit.\n", ch)
        return
    }

    /* make a new alias */
    pcdata.alias[pos] = arg
    pcdata.alias_sub[pos] = rest
    send_to_char("$arg is now aliased to '$rest'.\n", ch)
}


fun do_unalias(ch: CHAR_DATA, argument: String) {
    var found = false
    val pc = get_pc(ch)
    val rch = pc?.o_ch ?: ch
    val pcdata = rch.pcdata ?: return

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Unalias what?\n", ch)
        return
    }

    (0 until nw_config.max_alias)
            .asSequence()
            .takeWhile { pcdata.alias[it] != null }
            .forEach {
                if (found) {
                    pcdata.alias[it - 1] = pcdata.alias[it]
                    pcdata.alias_sub[it - 1] = pcdata.alias_sub[it]
                    pcdata.alias[it] = null
                    pcdata.alias_sub[it] = null
                } else if (arg.toString() == pcdata.alias[it]) {
                    send_to_char("Alias removed.\n", ch)
                    pcdata.alias[it] = null
                    pcdata.alias_sub[it] = null
                    found = true
                }
            }

    if (!found) {
        send_to_char("No alias of that name to remove.\n", ch)
    }
}
