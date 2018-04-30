package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_HASTE
import net.sf.nightworks.DT
import net.sf.nightworks.IMM_MAGIC
import net.sf.nightworks.PULSE_VIOLENCE
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.can_drop_obj
import net.sf.nightworks.check_dispel
import net.sf.nightworks.get_weapon_char
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.WeaponType
import net.sf.nightworks.obj_from_char
import net.sf.nightworks.obj_to_room
import net.sf.nightworks.remove_obj
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.WAIT_STATE

fun spell_fumble(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.fumble)) {
        if (victim == ch) {
            send_to_char("You can't be more fumble!\n", ch)
        } else {
            act("\$N can't get any more fumble than that.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (saves_spell(level, victim, DT.Other) || IS_SET(victim.imm_flags, IMM_MAGIC)) {
        if (victim != ch) {
            send_to_char("Nothing seemed to happen.\n", ch)
        }
        send_to_char("You feel momentarily lethargic.\n", victim)
        return
    }

    if (IS_AFFECTED(victim, AFF_HASTE)) {
        if (!check_dispel(level, victim, Skill.haste)) {
            if (victim != ch) {
                send_to_char("Spell failed.\n", ch)
            }
            send_to_char("You feel momentarily slower.\n", victim)
            return
        }

        act("\$n is moving less quickly.", victim, null, null, TO_ROOM)
        return
    }

    val af = Affect()
    af.type = Skill.fumble
    af.level = level
    af.duration = 4 + level / 12
    af.location = Apply.Dex
    af.modifier = -Math.max(2, level / 6)
    affect_to_char(victim, af)

    //todo: weapon type ??
    var obj = get_weapon_char(victim, WeaponType.Sword)
    if (obj != null) {
        if (can_drop_obj(victim, obj) && remove_obj(victim, obj, true)) {
            act("\$n cannot carry \$p anymore and drops it.", victim, obj, null, TO_ROOM)
            send_to_char("You cannot carry your dual weapon anymore and drop it!\n", victim)
            obj_from_char(obj)
            obj_to_room(obj, victim.room)
        }
    }
    obj = get_weapon_char(victim, WeaponType.Exotic)
    if (obj != null) {
        if (can_drop_obj(victim, obj) && remove_obj(victim, obj, true)) {
            act("\$n cannot carry \$p anymore and drops it.", victim, obj, null, TO_ROOM)
            send_to_char("You cannot carry your weapon anymore and drop it!\n", victim)
            obj_from_char(obj)
            obj_to_room(obj, victim.room)
        }
    }

    WAIT_STATE(victim, PULSE_VIOLENCE)
    send_to_char("You feel yourself very  f u m b l e...\n", victim)
    act("\$n starts to move in a fumble way.", victim, null, null, TO_ROOM)
}