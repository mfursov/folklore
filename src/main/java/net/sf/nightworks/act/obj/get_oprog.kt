package net.sf.nightworks.act.obj

import net.sf.nightworks.AFF_CURSE
import net.sf.nightworks.AFF_POISON
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.act.spell.spell_poison
import net.sf.nightworks.util.dice
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.OPROG_FUN_GET
import net.sf.nightworks.model.Pos
import net.sf.nightworks.obj_from_char
import net.sf.nightworks.obj_to_room
import net.sf.nightworks.send_to_char
import net.sf.nightworks.spell_cure_poison
import net.sf.nightworks.spell_curse
import net.sf.nightworks.spell_remove_curse
import net.sf.nightworks.util.IS_AFFECTED

fun create_get_prog(name: String): OPROG_FUN_GET = when (name) {
    "get_prog_spec_weapon" -> ::get_prog_spec_weapon
    "get_prog_quest_hreward" -> ::get_prog_quest_hreward
    "get_prog_quest_obj" -> ::get_prog_quest_obj
    "get_prog_cabal_item" -> ::get_prog_cabal_item
    "get_prog_heart" -> { obj, _ -> get_prog_heart(obj) }
    "get_prog_snake" -> ::get_prog_snake
    "get_prog_quest_reward" -> ::get_prog_quest_reward
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun get_prog_spec_weapon(obj: Obj, ch: CHAR_DATA) {
    if (obj.extra_desc.description.contains(ch.name)) {
        when {
            IS_AFFECTED(ch, AFF_POISON) && dice(1, 5) == 1 -> {
                send_to_char("Your weapon glows blue.", ch)
                act("\$n's weapon glows blue.", ch, null, null, TO_ROOM)
                spell_cure_poison(30, ch, ch)
            }
            IS_AFFECTED(ch, AFF_CURSE) && dice(1, 5) == 1 -> {
                send_to_char("Your weapon glows blue.", ch)
                act("\$n's weapon glows blue.", ch, null, null, TO_ROOM)
                spell_remove_curse(30, ch, ch, TARGET_CHAR)
            }
            else -> send_to_char("Your weapon's humming gets lauder.\n", ch)
        }
        return
    }

    act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)
    obj_from_char(obj)
    obj_to_room(obj, ch.room)

    when (dice(1, 10)) {
        1 -> spell_curse(if (ch.level < 10) 1 else ch.level - 9, ch, ch, TARGET_CHAR)
        2 -> spell_poison(if (ch.level < 10) 1 else ch.level - 9, ch, ch, TARGET_CHAR)
    }
}

private fun get_prog_quest_hreward(obj: Obj, ch: CHAR_DATA) {
    if (obj.extra_desc.description.contains(ch.name)) {
        act("{bYour \$p starts glowing.\n{x", ch, obj, null, TO_CHAR, Pos.Sleeping)
        return
    }

    act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)

    obj_from_char(obj)
    obj_to_room(obj, ch.room)

}

private fun get_prog_quest_obj(obj: Obj, ch: CHAR_DATA) {
    if (obj.extra_desc.description.contains(ch.name)) {
        when {
            IS_AFFECTED(ch, AFF_POISON) && dice(1, 5) == 1 -> {
                send_to_char("Your weapon glows blue.", ch)
                act("\$n's weapon glows blue.", ch, null, null, TO_ROOM)
                spell_cure_poison(30, ch, ch)
            }
            IS_AFFECTED(ch, AFF_CURSE) && dice(1, 5) == 1 -> {
                send_to_char("Your weapon glows blue.", ch)
                act("\$n's weapon glows blue.", ch, null, null, TO_ROOM)
                spell_remove_curse(30, ch, ch, TARGET_CHAR)
            }
            else -> send_to_char("Quest staff waits patiently to return.\n", ch)
        }
        return
    }
    act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)

    obj_from_char(obj)
    obj_to_room(obj, ch.room)

    when (dice(1, 10)) {
        1 -> spell_curse(if (ch.level < 10) 1 else ch.level - 9, ch, ch, TARGET_CHAR)
        2 -> spell_poison(if (ch.level < 10) 1 else ch.level - 9, ch, ch, TARGET_CHAR)
    }

}

private fun get_prog_cabal_item(obj: Obj, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        act("You are not worthy to have \$p and drop it.", ch, obj, null, TO_CHAR)
        act("\$n is not worthy to have \$p and drops it.", ch, obj, null, TO_ROOM)
        obj_from_char(obj)
        obj_to_room(obj, ch.room)
        return
    }

    if (obj.timer < 1) {
        obj.timer = 30
        act("\$p becomes transparent.", ch, obj, null, TO_CHAR)
        act("\$p becomes transparent.", ch, obj, null, TO_ROOM)
    }
}

private fun get_prog_heart(obj: Obj) {
    if (obj.timer == 0) {
        obj.timer = 24
    }
}

private fun get_prog_snake(obj: Obj, ch: CHAR_DATA) {
    act("You feel as if snakes of whip moved.", ch, obj, null, TO_CHAR)
}

private fun get_prog_quest_reward(obj: Obj, ch: CHAR_DATA) {
    if (obj.short_desc.contains(ch.name)) {
        act("{bYour \$p starts glowing.\n{x", ch, obj, null, TO_CHAR, Pos.Sleeping)
        return
    }
    act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)

    obj_from_char(obj)
    obj_to_room(obj, ch.room)
}
