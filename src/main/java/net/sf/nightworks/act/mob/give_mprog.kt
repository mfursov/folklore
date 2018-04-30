package net.sf.nightworks.act.mob


import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.can_see
import net.sf.nightworks.create_object
import net.sf.nightworks.do_drop
import net.sf.nightworks.do_give
import net.sf.nightworks.do_load
import net.sf.nightworks.do_open
import net.sf.nightworks.do_say
import net.sf.nightworks.do_unlock
import net.sf.nightworks.extract_obj
import net.sf.nightworks.get_obj_carry
import net.sf.nightworks.get_obj_index_nn
import net.sf.nightworks.interpret
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.MPROG_FUN_GIVE
import net.sf.nightworks.model.Obj
import net.sf.nightworks.obj_from_char
import net.sf.nightworks.obj_to_char
import net.sf.nightworks.util.TextBuffer

fun create_give_prog(name: String): MPROG_FUN_GIVE {
    return when (name) {
        "give_prog_dressmaker" -> ::give_prog_dressmaker
        "give_prog_fireflash" -> ::give_prog_fireflash
        "give_prog_keeper" -> ::give_prog_keeper
        "give_prog_solamnia" -> ::give_prog_solamnia
        else -> throw IllegalArgumentException("Prog not found: $name")
    }
}

private fun give_prog_keeper(mob: CHAR_DATA, ch: CHAR_DATA, obj: Obj) {
    if (obj.pIndexData.vnum != 90) {
        do_give(mob, obj.name + " " + ch.name)
        do_say(mob, "Why do i need this?.")
        return;
    }
    do_say(mob, "Finally, the dress I sent for!")
    act("\$n tucks the dress away under her desk.", mob, null, null, TO_ROOM)
    obj_from_char(obj)
    extract_obj(obj)
    if (get_obj_carry(ch, "rug") != null) {
        do_say(mob, "I suppose you'll want to see the FireFlash now")
        do_say(mob, "Be careful, she's been in a nasty mood.")
        do_unlock(mob, "door")
        do_open(mob, "door")
    } else {
        do_say(mob, "It doesn't look like you have any business with the FireFlash.")
        do_say(mob, "I suggest you leave and find some before coming here again.")
    }
}

private fun give_prog_fireflash(mob: CHAR_DATA, ch: CHAR_DATA, obj: Obj) {
    when {
        !can_see(mob, ch) -> do_say(mob, "Is someone there?")
        ch.pcdata == null -> do_say(mob, "How strange, an animal delivering something.")
        obj.pIndexData.vnum != 91 -> {
            do_say(mob, "How interesting!  ...what's it for?")
            interpret(mob, "giggle", false)
            do_give(mob, obj.name + " " + ch.name)
        }
        else -> {
            do_say(mob, "What a wonderful rug!  Let's see....where shall I put it?")
            act("\$n starts wandering about the room, mumbling to \$mself.", mob, null, null, TO_ROOM)
            act("\$n sticks \$s hands in \$s pockets.", mob, null, null, TO_ROOM)
            do_load(mob, "obj 2438")
            do_say(mob, "What's this?  A key?  Here, you can have it.")
            do_give(mob, "xxx " + ch.name)
            act("\$n absently pushes the rug under a chair.", mob, null, null, TO_ROOM)
            obj_from_char(obj)
            extract_obj(obj)
        }
    }
}

private fun give_prog_solamnia(mob: CHAR_DATA, ch: CHAR_DATA, obj: Obj) {
    if (obj.pIndexData.vnum != 2438) return

    do_say(mob, "Here is your reward!")
    val kassandra = create_object(get_obj_index_nn(89), 0)
    kassandra.timer = 500
    obj_to_char(kassandra, mob)
    val buf = TextBuffer()
    buf.sprintf("kassandra %s", ch.name)
    do_give(mob, buf.toString())
    do_say(mob, "This stone has some special powers, use it well.")
    obj_from_char(obj)
    extract_obj(obj)
}

private fun give_prog_dressmaker(mob: CHAR_DATA, ch: CHAR_DATA, obj: Obj) {
    if (ch.pcdata == null) {
        return
    }
    if (!can_see(mob, ch)) {
        do_say(mob, "Where did this come from?")
        return
    }
    if (obj.pIndexData.vnum != 2436) {
        do_say(mob, "I can't do anything with this, I need silk.")
        do_drop(mob, obj.name)
    } else {
        do_say(mob, "Who am I making this dress for?")
        obj_from_char(obj)
        extract_obj(obj)
    }
}