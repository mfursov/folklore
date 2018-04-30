package net.sf.nightworks

import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ConnectionState
import net.sf.nightworks.model.Language
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.Translator
import net.sf.nightworks.model.Wiznet
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_RAFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.atoi
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.startsWith
import net.sf.nightworks.util.upfirst
import java.io.File

/** RT code to delete yourself */
fun do_delet(ch: CHAR_DATA) {
    send_to_char("You must type the full command to delete yourself.\n", ch)
}

/** RT code to display channel status */
fun do_channels(ch: CHAR_DATA) {
    /* lists all channels and their status */
    send_to_char("   channel     status\n", ch)
    send_to_char("---------------------\n", ch)

    send_to_char("auction        ", ch)
    if (!IS_SET(ch.comm, COMM_NOAUCTION)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    if (IS_IMMORTAL(ch)) {
        send_to_char("god channel    ", ch)
        if (!IS_SET(ch.comm, COMM_NOWIZ)) {
            send_to_char("ON\n", ch)
        } else {
            send_to_char("OFF\n", ch)
        }
    }

    send_to_char("tells          ", ch)
    if (!IS_SET(ch.comm, COMM_DEAF)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("quiet mode     ", ch)
    if (IS_SET(ch.comm, COMM_QUIET)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    if (IS_SET(ch.comm, COMM_SNOOP_PROOF)) {
        send_to_char("You are immune to snooping.\n", ch)
    }

    if (ch.lines != PAGE_LEN) {
        if (ch.lines != 0) {
            send_to_char("You display ${ch.lines + 2} lines of scroll.\n", ch)
        } else {
            send_to_char("Scroll buffering is off.\n", ch)
        }
    }


    if (IS_SET(ch.comm, COMM_NOTELL)) {
        send_to_char("You cannot use tell.\n", ch)
    }

    if (IS_SET(ch.comm, COMM_NOCHANNELS)) {
        send_to_char("You cannot use channels.\n", ch)
    }

    if (IS_SET(ch.comm, COMM_NOEMOTE)) {
        send_to_char("You cannot show emotions.\n", ch)
    }

}

fun garble(speech: String): String {
    val garbled = StringBuilder(speech.length)
    (0 until speech.length)
            .map { speech[it] }
            .forEach {
                when (it) {
                    in 'a'..'z' -> garbled.append('a' + number_range(0, 25))
                    in 'A'..'Z' -> garbled.append('A' + number_range(0, 25))
                    else -> garbled.append(it)
                }
            }
    return garbled.toString()
}

/* RT deaf blocks out all shouts */

fun do_deaf(ch: CHAR_DATA) {

    if (IS_SET(ch.comm, COMM_DEAF)) {
        send_to_char("You can now hear tells again.\n", ch)
        ch.comm = REMOVE_BIT(ch.comm, COMM_DEAF)
    } else {
        send_to_char("From now on, you won't hear tells.\n", ch)
        ch.comm = SET_BIT(ch.comm, COMM_DEAF)
    }
}

/* RT quiet blocks out all communication */

fun do_quiet(ch: CHAR_DATA) {
    if (IS_SET(ch.comm, COMM_QUIET)) {
        send_to_char("Quiet mode removed.\n", ch)
        ch.comm = REMOVE_BIT(ch.comm, COMM_QUIET)
    } else {
        send_to_char("From now on, you will only hear says and emotes.\n", ch)
        ch.comm = SET_BIT(ch.comm, COMM_QUIET)
    }
}

fun do_replay(ch: CHAR_DATA) {
    val pcdata = ch.pcdata
    if (pcdata == null) {
        send_to_char("You can't replay.\n", ch)
        return
    }

    page_to_char(pcdata.buffer, ch)
    pcdata.buffer.setLength(0)
}

fun do_immtalk(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        if (IS_SET(ch.comm, COMM_NOWIZ)) {
            send_to_char("Immortal channel is now ON\n", ch)
            ch.comm = REMOVE_BIT(ch.comm, COMM_NOWIZ)
        } else {
            send_to_char("Immortal channel is now OFF\n", ch)
            ch.comm = SET_BIT(ch.comm, COMM_NOWIZ)
        }
        return
    }

    ch.comm = REMOVE_BIT(ch.comm, COMM_NOWIZ)

    if (!is_affected(ch, Skill.deafen)) {
        act("\$n: {C\$t{x", ch, argument, null, TO_CHAR, Pos.Dead)
    }
    Index.PLAYERS
            .filter { IS_IMMORTAL(it.ch) && !IS_SET(it.ch.comm, COMM_NOWIZ) }
            .forEach { act("\$n: {C\$t{x", ch, argument, it.ch, TO_VICT, Pos.Dead) }

}


fun do_say(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Say what?\n", ch)
        return
    }

    val buf = if (is_affected(ch, Skill.garble)) garble(argument) else argument

    for (vch in ch.room.people) {
        if (!is_affected(vch, Skill.deafen)) {
            val trans = translate(ch, vch, buf)
            act("{g\$n says '\$t'{x", ch, trans, vch, TO_VICT, Pos.Resting)
        }
    }

    if (!is_affected(ch, Skill.deafen)) {
        act("{gYou say '\$T'{x", ch, null, buf, TO_CHAR, Pos.Resting)
    }


    ch.room.people
            .filter { IS_SET(it.progtypes, MPROG_SPEECH) && it != ch }
            .forEach { it.pIndexData.mprogs.speech_prog!!(it, ch, buf) }

    ch.carrying
            .filter { IS_SET(it.progtypes, OPROG_SPEECH) }
            .forEach { it.pIndexData.oprogs.speech_prog!!(it, ch, buf) }

    ch.room.objects
            .filter { IS_SET(it.progtypes, OPROG_SPEECH) }
            .forEach { it.pIndexData.oprogs.speech_prog!!(it, ch, buf) }
}


fun do_shout(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Shout what?.\n", ch)
        return
    }

    WAIT_STATE(ch, 12)

    val buf = if (is_affected(ch, Skill.garble)) garble(argument) else argument

    if (!is_affected(ch, Skill.deafen)) {
        act("You shout '{G\$T{x'", ch, null, buf, TO_CHAR, Pos.Dead)
    }

    for (pc in Index.PLAYERS) {
        if (pc.ch != ch && pc.ch.room.area == ch.room.area && !is_affected(pc.ch, Skill.deafen)) {
            val trans = translate(ch, pc.ch, buf)
            act("\$n shouts '{G\$t{x'", ch, trans, pc.ch, TO_VICT, Pos.Dead)
        }
    }
}


fun do_tell(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val victim: CHAR_DATA?

    if (IS_SET(ch.comm, COMM_NOTELL) || IS_SET(ch.comm, COMM_DEAF)) {
        send_to_char("Your message didn't get through.\n", ch)
        return
    }

    if (IS_SET(ch.comm, COMM_QUIET)) {
        send_to_char("You must turn off quiet mode first.\n", ch)
        return
    }

    if (IS_SET(ch.comm, COMM_DEAF)) {
        send_to_char("You must turn off deaf mode first.\n", ch)
        return
    }

    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    if (arg.isEmpty() || rest.isEmpty()) {
        send_to_char("Tell whom what?\n", ch)
        return
    }

    /*
     * Can tell to PC's anywhere, but NPC's only in same room.
     * -- Furey
     */
    victim = get_char_world(ch, arg.toString())
    val pcdata = victim?.pcdata
    if (victim == null || pcdata == null && victim.room != ch.room) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    val pc = get_pc(victim)
    if (pc == null && pcdata != null) {
        act("\$N seems to have misplaced \$S link...try again later.", ch, null, victim, TO_CHAR)
        pcdata.buffer.append("${upfirst(PERS(ch, victim))} tells you '$rest'\n")
        return
    }

    if (!(IS_IMMORTAL(ch) && ch.level > LEVEL_IMMORTAL) && !IS_AWAKE(victim)) {
        act("\$E can't hear you.", ch, null, victim, TO_CHAR)
        return
    }

    if ((IS_SET(victim.comm, COMM_QUIET) || IS_SET(victim.comm, COMM_DEAF)) && !IS_IMMORTAL(ch)) {
        act("\$E is not receiving tells.", ch, null, victim, TO_CHAR)
        return
    }

    val buf = if (is_affected(ch, Skill.garble)) garble(rest) else rest

    if (!is_affected(ch, Skill.deafen)) {
        act("{rYou tell \$N '\$t'{x", ch, buf, victim, TO_CHAR, Pos.Sleeping)
    }
    act("{r\$n tells you '\$t'{x", ch, buf, victim, TO_VICT, Pos.Sleeping)

    victim.reply = ch

}


fun do_reply(ch: CHAR_DATA, argument: String) {
    val victim = ch.reply

    if (IS_SET(ch.comm, COMM_NOTELL)) {
        send_to_char("Your message didn't get through.\n", ch)
        return
    }

    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    val pc = get_pc(victim)
    val v_pcdata = victim.pcdata
    if (pc == null && v_pcdata != null) {
        val speech = if (is_affected(ch, Skill.garble)) garble(argument) else argument
        act("\$N seems to have misplaced \$S link...try again later.", ch, null, victim, TO_CHAR)
        v_pcdata.buffer.append("${upfirst(PERS(ch, victim))} tells you '$speech'\n")
        return
    }

    if (!IS_IMMORTAL(ch) && !IS_AWAKE(victim)) {
        act("\$E can't hear you.", ch, null, victim, TO_CHAR)
        return
    }

    if ((IS_SET(victim.comm, COMM_QUIET) || IS_SET(victim.comm, COMM_DEAF))
            && !IS_IMMORTAL(ch) && !IS_IMMORTAL(victim)) {
        act("\$E is not receiving tells.", ch, null, victim, TO_CHAR, Pos.Dead)
        return
    }

    if (!IS_IMMORTAL(victim) && !IS_AWAKE(ch)) {
        send_to_char("In your dreams, or what?\n", ch)
        return
    }

    if (!is_affected(ch, Skill.deafen)) {
        act("{RYou tell \$N '\$t'{x", ch, argument, victim, TO_CHAR, Pos.Sleeping)
    }
    act("{R\$n tells you '\$t'{x", ch, argument, victim, TO_VICT, Pos.Sleeping)

    victim.reply = ch

}

fun do_emote(ch: CHAR_DATA, argument: String) {

    if (ch.pcdata != null && IS_SET(ch.comm, COMM_NOEMOTE)) {
        send_to_char("You can't show your emotions.\n", ch)
        return
    }

    if (argument.isEmpty()) {
        send_to_char("Emote what?\n", ch)
        return
    }

    val buf = if (is_affected(ch, Skill.garble)) garble(argument) else argument
    act("\$n \$T", ch, null, buf, TO_ALL)
}


fun do_pmote(ch: CHAR_DATA, argument: String) {
    var matches = 0

    if (ch.pcdata != null && IS_SET(ch.comm, COMM_NOEMOTE)) {
        send_to_char("You can't show your emotions.\n", ch)
        return
    }

    if (argument.isEmpty()) {
        send_to_char("Emote what?\n", ch)
        return
    }

    act("\$n \$t", ch, argument, null, TO_CHAR)

    for (vch in ch.room.people) {
        if (vch.pcdata == null || vch == ch) {
            continue
        }

        var letter = argument.indexOf(vch.name)
        if (letter == -1) {
            act("\$N \$t", vch, argument, ch, TO_CHAR)
            continue
        }

        val temp = StringBuilder(argument)
        val last = StringBuilder()
        var name = vch.name
        var namePos = 0

        while (letter < argument.length) {
            val c = argument[letter]
            if (c == '\'' && matches == vch.name.length) {
                temp.append("r")
                letter++
                continue
            }

            if (c == 's' && matches == vch.name.length) {
                matches = 0
                letter++
                continue
            }

            if (matches == vch.name.length) {
                matches = 0
            }
            if (c == name[namePos]) {
                matches++
                namePos++
                if (matches == vch.name.length) {
                    temp.append("you")
                    name = vch.name
                    letter++
                    continue
                }
                last.append(c)
                letter++
                continue
            }

            matches = 0
            temp.append(last)
            temp.append(c)
            last.setLength(0)
            name = vch.name
            letter++
        }

        act("\$N \$t", vch, temp.toString(), ch, TO_CHAR)
    }

}

fun do_pose(ch: CHAR_DATA) {
    if (ch.pcdata == null || ch.clazz.poses.isEmpty()) {
        return
    }

    val poses = ch.clazz.poses
    val level = Math.min(ch.level, poses.size - 1)
    val poseIdx = number_range(0, level)

    val pose = poses[poseIdx]
    act(pose.message_to_char, ch, null, null, TO_CHAR)
    act(pose.message_to_room, ch, null, null, TO_ROOM)

}

fun do_rent(ch: CHAR_DATA) {
    send_to_char("There is no rent here.  Just save and quit.\n", ch)
}


fun do_qui(ch: CHAR_DATA) {
    send_to_char("If you want to QUIT, you have to spell it out.\n", ch)
}


fun do_quit(ch: CHAR_DATA) {
    quit_org(ch, false)
}

fun quit_org(ch: CHAR_DATA, remort: Boolean): Boolean {
    val id = ch.id
    if (ch.pcdata == null) {
        return false
    }

    if (ch.position == Pos.Fighting) {
        send_to_char("No way! You are fighting.\n", ch)
        return false
    }

    if (IS_AFFECTED(ch, AFF_SLEEP)) {
        send_to_char("Lie still! You are not awaken, yet.\n", ch)
        return false
    }

    if (ch.position < Pos.Stunned) {
        send_to_char("You're not DEAD yet.\n", ch)
        return false
    }

    if (ch.last_fight_time != -1L && !IS_IMMORTAL(ch) && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
        send_to_char("Your adrenalin is gushing! You can't quit yet.\n", ch)
        return false
    }

    if (IS_AFFECTED(ch, AFF_CHARM)) {
        send_to_char("You don't want to leave your master.\n", ch)
        return false
    }

    if (IS_SET(ch.act, PLR_NO_EXP)) {
        send_to_char("You don't want to lose your spirit.\n", ch)
        return false
    }

    val a = auction
    if (a != null && (ch == a.buyer || ch == a.seller)) {
        send_to_char("Wait till you have sold/bought the item on auction.\n", ch)
        return false
    }

    if (!IS_IMMORTAL(ch) && IS_RAFFECTED(ch.room, AFF_ROOM_ESPIRIT)) {
        send_to_char("Evil spirits in the area prevents you from leaving.\n", ch)
        return false
    }

    if (!IS_IMMORTAL(ch) && ch.cabal != Clan.Invader && is_affected(ch, Skill.evil_spirit)) {
        send_to_char("Evil spirits in you prevents you from leaving.\n", ch)
        return false
    }

    if (cabal_area_check(ch)) {
        send_to_char("You cannot quit in other cabal's areas.\n", ch)
        return false
    }

    if (!remort) {
        send_to_char("Alas, all good things must come to an end.\n", ch)
        act("{g\$n has left the game.{x", ch, null, null, TO_ROOM, Pos.Dead)
        log_string(ch.name + " has quit.")
        wiznet("\$N rejoins the real world.", ch, null, Wiznet.Logins, null, get_trust(ch))
    }

    Index.OBJECTS
            .filter {
                (it.pIndexData.vnum == 84 || it.pIndexData.vnum == 85 || it.pIndexData.vnum == 86 || it.pIndexData.vnum == 97)
                        && (it.extra_desc.description.isEmpty() || it.extra_desc.description.contains(ch.name))
            }
            .forEach { extract_obj(it) }

    for (obj in ch.carrying) {
        if (obj.pIndexData.vnum == OBJ_VNUM_MAGIC_JAR) {
            extract_obj(obj)
        }
        if (obj.pIndexData.vnum == 84
                || obj.pIndexData.vnum == 85
                || obj.pIndexData.vnum == 86
                || obj.pIndexData.vnum == 97) {
            when {
                obj.extra_desc.description.isEmpty() -> extract_obj(obj)
                obj.extra_desc.description.contains(ch.name) -> extract_obj(obj)
                else -> {
                    obj_from_char(obj)
                    obj_to_room(obj, ch.room)
                }
            }
        }
    }

    for (vch in Index.CHARS) {
        if (is_affected(vch, Skill.doppelganger) && vch.doppel == ch) {
            send_to_char("You shift to your true form as your victim leaves.\n", vch)
            affect_strip(vch, Skill.doppelganger)
        }

        if (vch.guarding == ch) {
            act("You stops guarding \$N.", vch, null, ch, TO_CHAR)
            act("\$n stops guarding you.", vch, null, ch, TO_VICT)
            act("\$n stops guarding \$N.", vch, null, ch, TO_NOTVICT)
            vch.guarding = null
            ch.guarded_by = null
        }

        if (vch.last_fought == ch) {
            vch.last_fought = null
            back_home(vch)
        }

        if (vch.hunting == ch) {
            vch.hunting = null
        }
    }

    val guarded_by = ch.guarded_by
    if (guarded_by != null) {
        guarded_by.guarding = null
        ch.guarded_by = null
    }

    /*
    * After extract_char the ch is no longer valid!
    */
    save_char_obj(ch)
    extract_char(ch, true)

    val pc = get_pc(ch)
    if (pc != null && !remort) {
        close_socket(pc.con)
    }

    /* toast evil cheating bastards    */
    for (ad in Index.PLAYERS) {
        if (remort && ad === pc) {
            continue
        }
        if (ad.o_ch.id == id) {
            extract_char(ad.ch, true)
            close_socket(ad.con)
        }
    }
    iNumPlayers--
    return true
}


fun do_save(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (ch.level < 2 && !IS_SET(ch.act, PLR_REMORTED)) {
        send_to_char("You must be at least level 2 for saving.\n", ch)
        return
    }
    save_char_obj(ch)
    send_to_char("Saving. Remember that Nightworks MUD has automatic saving.\n", ch)
    WAIT_STATE(ch, PULSE_VIOLENCE)
}


fun do_follow(ch: CHAR_DATA, argument: String) {
    /* RT changed to allow unlimited following and follow the NOFOLLOW rules */
    val victim: CHAR_DATA?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Follow whom?\n", ch)
        return
    }

    victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master != null) {
        act("But you'd rather follow \$N!", ch, null, ch.master, TO_CHAR)
        return
    }

    if (victim == ch) {
        if (ch.master == null) {
            send_to_char("You already follow yourself.\n", ch)
            return
        }
        stop_follower(ch)
        return
    }

    if (victim.pcdata != null && IS_SET(victim.act, PLR_NOFOLLOW) && !IS_IMMORTAL(ch)) {
        act("\$N doesn't seem to want any followers.\n", ch, null, victim, TO_CHAR)
        return
    }

    ch.act = REMOVE_BIT(ch.act, PLR_NOFOLLOW)

    if (ch.master != null) {
        stop_follower(ch)
    }

    add_follower(ch, victim)
}


fun add_follower(ch: CHAR_DATA, master: CHAR_DATA) {
    if (ch.master != null) {
        bug("Add_follower: non-null master.")
        return
    }

    ch.master = master
    ch.leader = null

    if (can_see(master, ch)) {
        act("{Y\$n now follows you.{x", ch, null, master, TO_VICT, Pos.Resting)
    }
    act("{YYou now follow \$N.{x", ch, null, master, TO_CHAR, Pos.Resting)

}


fun stop_follower(ch: CHAR_DATA) {
    val master = ch.master
    if (master == null) {
        bug("Stop_follower: null master.")
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM)) {
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_CHARM)
        affect_strip(ch, Skill.charm_person)
    }

    if (can_see(master, ch)) {
        act("{b\$n stops following you.{x", ch, null, master, TO_VICT, Pos.Resting)
        act("{bYou stop following \$N.{x", ch, null, master, TO_CHAR, Pos.Resting)
    }
    if (master.pet == ch) {
        master.pet = null
    }

    ch.master = null
    ch.leader = null
}

/* nukes charmed monsters and pets */

fun nuke_pets(ch: CHAR_DATA) {
    val pet = ch.pet
    if (pet != null) {
        stop_follower(pet)
        act("\$N slowly fades away.", ch, null, pet, TO_NOTVICT)
        extract_char(pet, true)
    }
    ch.pet = null
}


fun die_follower(ch: CHAR_DATA) {
    val master = ch.master
    if (master != null) {
        if (master.pet == ch) {
            master.pet = null
        }
        stop_follower(ch)
    }

    ch.leader = null

    Index.CHARS.forEach { fch ->
        if (fch.master == ch) {
            stop_follower(fch)
        }
        if (fch.leader == ch) {
            fch.leader = fch
        }
    }
}


fun do_order(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val victim: CHAR_DATA?
    var found: Boolean
    val fAll: Boolean

    val arg = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg)
    one_argument(rest, arg2)

    if (eq(arg2.toString(), "delete")) {
        send_to_char("That will NOT be done.\n", ch)
        return
    }

    if (arg.isEmpty() || rest.isEmpty()) {
        send_to_char("Order whom to do what?\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM)) {
        send_to_char("You feel like taking, not giving, orders.\n", ch)
        return
    }

    if (eq(arg.toString(), "all")) {
        fAll = true
        victim = null
    } else {
        fAll = false
        victim = get_char_room(ch, arg.toString())
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }

        if (victim == ch) {
            send_to_char("Aye aye, right away!\n", ch)
            return
        }

        if (!IS_AFFECTED(victim, AFF_CHARM) || victim.master != ch
                || IS_IMMORTAL(victim) && victim.trust >= ch.trust) {
            send_to_char("Do it yourself!\n", ch)
            return
        }
    }

    found = false
    for (och in ch.room.people) {
        if (IS_AFFECTED(och, AFF_CHARM) && och.master == ch && (fAll || och == victim)) {
            found = true
            if (!proper_order(och, rest)) {
                continue
            }
            act("\$n orders you to '$rest', you do.", ch, null, och, TO_VICT)
            interpret(och, rest, true)
        }
    }

    if (found) {
        WAIT_STATE(ch, PULSE_VIOLENCE)
        send_to_char("Ok.\n", ch)
    } else {
        send_to_char("You have no followers here.\n", ch)
    }
}

fun proper_order(ch: CHAR_DATA, argument: String): Boolean {
    val command = StringBuilder()
    one_argument(argument, command)

    val trust = get_trust(ch)
    val cmd_table = commandsTable
    var cmd: CmdType? = null
    val commandStr = command.toString()
    for (c in cmd_table) {
        for (name in c.names) {
            if (commandStr[0] == name[0] && startsWith(commandStr, name) && c.level <= trust) {
                cmd = c
                break
            }
        }
        if (cmd != null) {
            break
        }
    }
    if (cmd == null) {
        return true
    }
    if (ch.pcdata != null) {
        return !(cmd == CmdType.do_delete || cmd == CmdType.do_remort || cmd == CmdType.do_induct
                || cmd == CmdType.do_quest || cmd == CmdType.do_practice || cmd == CmdType.do_train)
    }

    if ((cmd == CmdType.do_bash || cmd == CmdType.do_dirt || cmd == CmdType.do_kick
            || cmd == CmdType.do_murder || cmd == CmdType.do_trip) && ch.fighting == null) {
        return false
    }

    return when (cmd) {
        CmdType.do_assassinate, CmdType.do_ambush, CmdType.do_blackjack, CmdType.do_cleave, CmdType.do_kill, CmdType.do_murder, CmdType.do_recall, CmdType.do_strangle, CmdType.do_vtouch -> false
        CmdType.do_close, CmdType.do_lock, CmdType.do_open, CmdType.do_unlock -> true
        CmdType.do_backstab, CmdType.do_hide, CmdType.do_pick, CmdType.do_sneak -> IS_SET(ch.act, ACT_THIEF)
        else -> true
    }
}


/**
 * 'Split' originally by Gnort, God of Chaos.
 */
fun do_split(ch: CHAR_DATA, argument: String) {
    var rest = argument
    var amount_gold = 0
    val amount_silver: Int
    val share_gold: Int
    val share_silver: Int
    val extra_gold: Int
    val extra_silver: Int

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty()) {
        send_to_char("Split how much?\n", ch)
        return
    }


    amount_silver = atoi(arg1.toString())

    if (arg2.isNotEmpty()) {
        amount_gold = atoi(arg2.toString())
    }

    if (amount_gold < 0 || amount_silver < 0) {
        send_to_char("Your group wouldn't like that.\n", ch)
        return
    }

    if (amount_gold == 0 && amount_silver == 0) {
        send_to_char("You hand out zero coins, but no one notices.\n", ch)
        return
    }

    if (ch.gold < amount_gold || ch.silver < amount_silver) {
        send_to_char("You don't have that much to split.\n", ch)
        return
    }

    val members = ch.room.people.count { is_same_group(it, ch) && !IS_AFFECTED(it, AFF_CHARM) }
    if (members < 2) {
        send_to_char("Just keep it all.\n", ch)
        return
    }

    share_silver = amount_silver / members
    extra_silver = amount_silver % members

    share_gold = amount_gold / members
    extra_gold = amount_gold % members

    if (share_gold == 0 && share_silver == 0) {
        send_to_char("Don't even bother, cheapskate.\n", ch)
        return
    }

    ch.silver -= amount_silver
    ch.silver += share_silver + extra_silver
    ch.gold -= amount_gold
    ch.gold += share_gold + extra_gold

    if (share_silver > 0) {
        send_to_char("You split $amount_silver silver coins. Your share is ${share_silver + extra_silver} silver.\n", ch)
    }

    if (share_gold > 0) {
        send_to_char("You split $amount_gold gold coins. Your share is ${share_gold + extra_gold} gold.\n", ch)
    }

    val msg = when {
        share_gold == 0 -> "\$n splits $amount_silver silver coins. Your share is $share_silver silver."
        share_silver == 0 -> "\$n splits $amount_gold gold coins. Your share is $share_gold gold."
        else -> "\$n splits $amount_silver silver and $amount_gold gold coins, giving you $share_silver silver and $share_gold gold.\n"
    }

    for (v in ch.room.people) {
        if (v != ch && is_same_group(v, ch) && !IS_AFFECTED(v, AFF_CHARM)) {
            act(msg, ch, null, v, TO_VICT)
            v.gold += share_gold
            v.silver += share_silver
        }
    }
}


fun do_gtell(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Tell your group what?\n", ch)
        return
    }

    if (IS_SET(ch.comm, COMM_NOTELL)) {
        send_to_char("Your message didn't get through!\n", ch)
        return
    }

    val buf = if (is_affected(ch, Skill.garble)) garble(argument) else argument

    // Note use of send_to_char, so gtell works on sleepers.
    var i = 0
    for (gch in Index.CHARS) {
        if (is_same_group(gch, ch) && !is_affected(gch, Skill.deafen)) {
            act("{m\$n tells the group '\$t'{x", ch, buf, gch, TO_VICT, Pos.Dead)
            i++
        }
    }

    if (i > 1 && !is_affected(ch, Skill.deafen)) {
        act("{cYou tell your group '\$t'{x", ch, buf, null, TO_CHAR, Pos.Dead)
    } else {
        send_to_char("Quit talking to yourself. You are all alone.", ch)
    }

}

/*
* It is very important that this be an equivalence relation:
* (1) A ~ A
* (2) if A ~ B then B ~ A
* (3) if A ~ B  and B ~ C, then A ~ C
*/

/**
 * New is_same_group by chronos
 */
fun is_same_group(ach: CHAR_DATA, bch: CHAR_DATA): Boolean {
    var vcount = 0
    var count = vcount
    var ch: CHAR_DATA? = ach
    while (ch != null) {
        val ch_next = ch.leader
        var vch: CHAR_DATA? = bch
        while (vch != null) {
            val vch_next = vch.leader
            if (ch == vch) {
                return true
            }
            if (++vcount > 6) {
                break    /* cyclic loop! */
            }
            vch = vch_next
        }
        if (++count > 6) {
            break  /* cyclic loop! */
        }
        ch = ch_next
    }
    return false
}


fun do_cb(ch: CHAR_DATA, argument: String) {
    if (ch.cabal == Clan.None) {
        send_to_char("You are not in a Cabal.\n", ch)
        return
    }
    val buf = "[${ch.cabal.short_name}] \$n: {y\$t{x"
    val buf2 = if (is_affected(ch, Skill.garble)) garble(argument) else argument
    if (!is_affected(ch, Skill.deafen)) {
        act(buf, ch, argument, null, TO_CHAR, Pos.Dead)
    }
    Index.PLAYERS
            .filter { it.ch.cabal == ch.cabal && !is_affected(it.ch, Skill.deafen) }
            .forEach { act(buf, ch, buf2, it.ch, TO_VICT, Pos.Dead) }

}

fun do_pray(ch: CHAR_DATA, argument: String) {
    if (IS_SET(ch.comm, COMM_NOCHANNELS)) {
        send_to_char("The gods refuse to listen to you right now.", ch)
        return
    }

    send_to_char("You pray to the heavens for help!\n", ch)
    send_to_char("This is not an emote, but a channel to the immortals.\n",
            ch)

    Index.PLAYERS
            .filter { IS_IMMORTAL(it.ch) && !IS_SET(it.ch.comm, COMM_NOWIZ) }
            .forEach {
                if (argument.isEmpty()) {
                    act("{c\$n is PRAYING for: any god{x", ch, argument, it.ch, TO_VICT, Pos.Dead)
                } else {
                    act("{c\$n is PRAYING for: \$t{x", ch, argument, it.ch, TO_VICT, Pos.Dead)
                }
            }
}

/*
* ch says
* victim hears
*/
fun translate(ch: CHAR_DATA?, victim: CHAR_DATA?, argument: String): String {
    if (argument.isEmpty()
            || ch == null || victim == null
            || ch.pcdata == null || victim.pcdata == null
            || IS_IMMORTAL(ch) || IS_IMMORTAL(victim)
            || ch.language == Language.Common
            || ch.language == victim.race.language) {

        return when {
            victim != null && IS_IMMORTAL(victim) -> "{${(ch ?: victim).language.title}} $argument"
            else -> argument
        }
    }
    return "{${ch.language.title}} ${Translator.tr(argument)}"
}


fun do_speak(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()

    one_argument(argument, arg)
    val chLang = ch.language.title
    if (arg.isEmpty()) {
        send_to_char("You now speak $chLang.\n", ch)
        send_to_char("You can speak :\n", ch)
        send_to_char("       common, ${ch.race.language.title}\n", ch)
        return
    }

    val name = arg.toString()
    val language = when {
        startsWith(name, "mother") -> ch.race.language
        else -> Language.values().firstOrNull { startsWith(name, it.title) }
    }

    if (language == null) {
        send_to_char("You never heard of that language.\n", ch)
        return
    }

    ch.language = language
    send_to_char("Now you speak ${language.title}.\n", ch)
}

/* Thanx zihni@karmi.emu.edu.tr for the code of do_judge */

fun do_judge(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?

    if (skill_failure_check(ch, Skill.judge, true, 0, null)) {
        return
    }
    val arg = StringBuilder()

    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Judge whom?\n", ch)
        return
    }

    /* judge thru world */
    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }


    if (victim.pcdata == null) {
        send_to_char("Not a mobile, of course.\n", ch)
        return
    }

    if (IS_IMMORTAL(victim) && !IS_IMMORTAL(ch)) {
        send_to_char("You do not have the power to judge Immortals.\n", ch)
        return
    }
    send_to_char("${victim.name}'s ethos is ${victim.ethos.title} and aligment is ${victim.align.title}.\n", ch)
}

fun do_remor(ch: CHAR_DATA) {
    send_to_char("If you want to REMORT, spell it out.\n", ch)
}

fun do_remort(ch: CHAR_DATA, argument: String) {
    val bankg: Int
    val banks: Int
    val qp: Int
    val silver: Int
    val gold: Int

    val pcdata = ch.pcdata
    val pc = get_pc(ch)
    if (pcdata == null || pc == null) {
        return
    }

    if (ch.level != LEVEL_HERO) {
        send_to_char("You must be a HERO to remort.\n", ch)
        return
    }

    if (!IS_SET(ch.act, PLR_CANREMORT) && !IS_SET(ch.act, PLR_REMORTED)) {
        send_to_char("You have to get permission from an immortal to remort.\n", ch)
        return
    }

    if (argument.isNotEmpty()) {
        if (!pcdata.confirm_remort) {
            send_to_char("Just type remort. No argument.\n", ch)
        }
        pcdata.confirm_remort = false
        return
    }

    if (pcdata.confirm_remort) {
        ch.act = SET_BIT(ch.act, PLR_REMORTED)
        send_to_char("\nNOW YOU ARE REMORTING.\n", ch)
        send_to_char("You will create a new char with new race, class and new stats.\n", ch)
        send_to_char("If you are somehow disconnected from the mud or mud crashes:\n", ch)
        send_to_char("    CREATE A NEW CHARACTER WITH THE SAME NAME AND NOTE TO IMMORTALS\n", ch)
        send_to_char("Note that, the items below will be saved:\n", ch)
        send_to_char("        all of the gold and silver you have (also in bank)\n", ch)
        send_to_char("        your current quest points.\n", ch)
        send_to_char("IN ADDITION, you will be able to wear two more rings.\n", ch)
        send_to_char("             You will have additional 10 trains.\n", ch)

        val remstr = nw_config.lib_player_dir + "/" + capitalize(ch.name)
        val name = ch.name
        banks = pcdata.bank_s
        bankg = pcdata.bank_g
        qp = pcdata.questpoints
        silver = ch.silver
        gold = ch.gold

        if (!quit_org(ch, true)) {
            return
        }

        File(remstr).delete()

        val new_ch = load_char_obj(pc.con, name)
        val new_pcdata = new_ch.pcdata!!
        pc.con.state = ConnectionState.CON_REMORTING

        /* give the remorting bonus */
        new_pcdata.bank_s += banks
        new_pcdata.bank_g += bankg
        new_ch.silver += silver
        new_ch.gold += gold
        new_pcdata.questpoints += qp
        ch.train += 10

        //TODO: copy new_ch & new_pcdata to old values or do complete relogin!

        write_to_buffer(pc.con, "\n[Hit Return to Continue]\n")
        return
    }

    send_to_char("Type remort again to confirm this command.\n", ch)
    send_to_char("WARNING: this command is irreversible.\n", ch)
    send_to_char("Typing remort with an argument will undo remort status.\n", ch)
    send_to_char("Note that, the items below will be saved:\n", ch)
    send_to_char("        all of the gold and silver you have (also in bank)\n", ch)
    send_to_char("        your current practice, train and questpoints\n", ch)
    send_to_char("IN ADDITION, you will be able to wear two more rings.\n", ch)
    pcdata.confirm_remort = true
    wiznet("\$N is contemplating remorting.", ch, null, null, null, get_trust(ch))
}


fun cabal_area_check(ch: CHAR_DATA): Boolean {
    if (IS_IMMORTAL(ch)) return false

    val areaName = ch.room.area.name
    return when {
        ch.cabal != Clan.Ruler && areaName == "Ruler" -> true
        ch.cabal != Clan.Invader && areaName == "Invader" -> true
        ch.cabal != Clan.Chaos && areaName === "Chaos" -> true
        ch.cabal != Clan.Shalafi && areaName === "Shalafi" -> true
        ch.cabal != Clan.BattleRager && areaName == "Battlerager" -> true
        ch.cabal != Clan.Knight && areaName == "Knight" -> true
        ch.cabal != Clan.Hunter && areaName == "Hunter" -> true
        ch.cabal != Clan.Lion && areaName == "Lions" -> true
        else -> false
    }
}

fun is_at_cabal_area(ch: CHAR_DATA): Boolean {
    if (IS_IMMORTAL(ch)) return false

    val areaName = ch.room.area.name
    //todo: add flag to area
    return areaName == "Ruler"
            || areaName == "Invader"
            || areaName == "Chaos"
            || areaName == "Shalafi"
            || areaName == "Battlerager"
            || areaName == "Knight"
            || areaName == "Hunter"
            || areaName == "Lions"

}
