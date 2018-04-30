package net.sf.nightworks.act.mob

import net.sf.nightworks.Index
import net.sf.nightworks.OBJ_VNUM_EYED_SWORD
import net.sf.nightworks.QUEST_EYE
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.create_object
import net.sf.nightworks.do_say
import net.sf.nightworks.do_smite
import net.sf.nightworks.extract_obj
import net.sf.nightworks.get_obj_index_nn
import net.sf.nightworks.heal_battle
import net.sf.nightworks.interpret
import net.sf.nightworks.is_name
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.EXTRA_DESC_DATA
import net.sf.nightworks.model.MPROG_FUN_SPEECH
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Religion
import net.sf.nightworks.obj_to_char
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.startsWith

private const val GIVE_HELP_RELIGION = 16
private const val RELIG_CHOSEN = 17

fun create_speech_prog(name: String): MPROG_FUN_SPEECH = when (name) {
    "speech_prog_crier" -> { mob, _, speech -> speech_prog_crier(mob, speech) }
    "speech_prog_hunter_cleric" -> ::speech_prog_hunter_cleric
    "speech_prog_keeper" -> ::speech_prog_keeper
    "speech_prog_templeman" -> ::speech_prog_templeman
    "speech_prog_wiseman" -> ::speech_prog_wiseman
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun speech_prog_keeper(mob: CHAR_DATA, ch: CHAR_DATA, speech: String) {
    val obj: Obj

    if (eq(speech, "keeper") && ch.pcdata != null) {
        obj = create_object(get_obj_index_nn(90), 0)
        obj.name = "keeper dress"
        act("\$n fashions a white gown out of the bolt of silk.", mob, null, null, TO_ROOM)
        act("You make a white gown for the Keeper.", mob, null, null, TO_CHAR)
        do_say(mob, "Here is the dress for the keeper.")
        obj_to_char(obj, ch)
    }
}

private fun speech_prog_templeman(mob: CHAR_DATA, ch: CHAR_DATA, speech: String) {
    if (eq(speech, "religion")) {
        mob.status = GIVE_HELP_RELIGION
        do_say(mob, "Himm yes, religion. Do you really interested in that?.")
        do_say(mob, "Read the help first.Type 'help religion'")
        do_say(mob, "Do not forget that once you choose your religion. ")
        do_say(mob, "You have to complete some quests in order to change your religion.")
        return
    }
    val chosen = lookup_religion_leader(speech)
    if (chosen == Religion.None) {
        return
    }
    mob.status = RELIG_CHOSEN
    if (!ch.religion.isNone()) {
        do_say(mob, "You are already in the way of ${ch.religion.leader}")
        return
    }

    if (chosen.isNotAllowed(ch)) {
        do_say(mob, "That religion doesn't match your ethos and alignment.")
        return
    }

    ch.religion = chosen
    do_say(mob, "From now on and forever, you are in the way of ${ch.religion.leader}")
}

private fun speech_prog_wiseman(mob: CHAR_DATA, ch: CHAR_DATA, speech: String) {
    if (eq(speech, "Help me wiseman")) heal_battle(mob, ch)
}

private fun speech_prog_crier(mob: CHAR_DATA, speech: String) {
    val arg = StringBuilder()
    one_argument(speech, arg)
    if (is_name(arg.toString(), "what")) {
        do_say(mob, "My girlfriend left me.")
    }
}

private fun speech_prog_hunter_cleric(mob: CHAR_DATA, ch: CHAR_DATA, speech: String) {
    if (!eq(speech, "trouble")) {
        return
    }

    if (ch.cabal != Clan.Hunter) {
        do_say(mob, "You must try hard!")
        return
    }

    if (!IS_SET(ch.quest, QUEST_EYE)) {
        do_say(mob, "What do you mean?")
        return
    }

    var matched = false
    val buf = TextBuffer()
    for (obj in Index.OBJECTS) {
        if (obj.pIndexData.vnum != OBJ_VNUM_EYED_SWORD || !obj.short_desc.contains(ch.name)) {
            continue
        }

        matched = true
        var in_obj: Obj? = obj
        while (in_obj!!.in_obj != null) {
            in_obj = in_obj.in_obj
        }

        val carried_by = in_obj.carried_by
        if (carried_by != null) {
            if (carried_by == ch) {
                do_say(mob, "Are you kidding me? Your sword is already carried by you!")
                do_smite(mob, ch.name)
                return
            }

            buf.sprintf("Your sword is carried by %s!", PERS(carried_by, ch))
            do_say(mob, buf.toString())
            buf.sprintf("%s is in general area of %s at %s!",
                    PERS(carried_by, ch),
                    carried_by.room.area.name,
                    carried_by.room.name)
            do_say(mob, buf.toString())
            return
        }
        if (in_obj.in_room != null) {
            buf.sprintf("Your sword is in general area of %s at %s!", in_obj.in_room!!.area.name, in_obj.in_room!!.name)
            do_say(mob, buf.toString())
            return
        } else {
            extract_obj(obj)
            do_say(mob, "I will give you a new one.")
        }
        break
    }

    if (!matched) {
        do_say(mob, "Your sword is completely lost!")
    }

    val i = when {
        ch.isGood() -> 0
        ch.isEvil() -> 2
        else -> 1
    }

    val obj = create_object(get_obj_index_nn(OBJ_VNUM_EYED_SWORD), 0)
    obj.owner = ch.name
    obj.from = ch.name
    obj.altar = ch.hometown.altar[i]
    obj.pit = ch.hometown.pit[i]
    obj.level = ch.level

    buf.sprintf(obj.short_desc, ch.name)
    obj.short_desc = buf.toString()

    buf.sprintf(obj.pIndexData.extra_descr!!.description, ch.name)
    obj.extra_desc = EXTRA_DESC_DATA()
    obj.extra_desc.keyword = obj.pIndexData.extra_descr!!.keyword
    obj.extra_desc.description = buf.toString()
    obj.extra_desc.next = null

    obj.value[2] = (ch.level / 10 + 3).toLong()
    obj.level = ch.level
    obj.cost = 0

    interpret(mob, "emote creates the Hunter's Sword.", false)
    do_say(mob, "I gave you another hunter's sword to you.")
    act("\$N gives \$p to \$n.", ch, obj, mob, TO_ROOM)
    act("\$N gives you \$p.", ch, obj, mob, TO_CHAR)
    obj_to_char(obj, ch)
    do_say(mob, "Don't lose again!")
}

private fun lookup_religion_leader(name: String) = Religion.values().firstOrNull { startsWith(name, it.leader) } ?: Religion.None
