package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.act.spell.spell_poison
import net.sf.nightworks.act.spell.spell_weaken
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Align
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.EXTRA_DESC_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Religion
import net.sf.nightworks.model.Size
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_OUTSIDE
import net.sf.nightworks.util.IS_RAFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_WEAPON_STAT
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.flipCoin
import net.sf.nightworks.util.interpolate
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.number_fuzzy
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.random
import net.sf.nightworks.util.randomPercent


fun check_place(ch: CHAR_DATA, argument: String): ROOM_INDEX_DATA? {
    var range = ch.level / 10 + 1

    val arg = StringBuilder()
    var number = number_argument(argument, arg)
    val door = check_exit(arg.toString())
    if (door == -1) {
        return null
    }

    var dest_room: ROOM_INDEX_DATA? = ch.room
    while (number > 0) {
        number--
        if (--range < 1) {
            return null
        }
        val exit = dest_room?.exit
        val pExit = if (exit == null) null else exit[door]
        dest_room = pExit?.to_room
        if (dest_room == null || (pExit != null && IS_SET(pExit.exit_info, EX_CLOSED))) {
            break
        }
        if (number < 1) {
            return dest_room
        }
    }
    return null
}

fun spell_portal(level: Int, ch: CHAR_DATA) {
    val victim = get_char_world(ch, target_name)
    if (victim == null || victim == ch || !can_see_room(ch, victim.room) || IS_SET(victim.room.room_flags, ROOM_SAFE) || IS_SET(victim.room.room_flags, ROOM_PRIVATE) || IS_SET(victim.room.room_flags, ROOM_SOLITARY) || IS_SET(victim.room.room_flags, ROOM_NOSUMMON) || IS_SET(ch.room.room_flags, ROOM_NOSUMMON) || IS_SET(ch.room.room_flags, ROOM_NO_RECALL) || victim.level >= level + 3 || victim.pcdata != null && victim.level >= LEVEL_HERO || victim.pcdata == null && is_safe_nomessage(ch, victim) && IS_SET(victim.imm_flags, IMM_SUMMON) || victim.pcdata == null && saves_spell(level, victim, DT.None)) {
        send_to_char("You failed.\n", ch)
        return
    }

    val stone = get_hold_char(ch)
    if (!IS_IMMORTAL(ch) && (stone == null || stone.item_type != ItemType.WarpStone)) {
        send_to_char("You lack the proper component for this spell.\n", ch)
        return
    }

    if (stone != null && stone.item_type == ItemType.WarpStone) {
        act("You draw upon the power of \$p.", ch, stone, null, TO_CHAR)
        act("It flares brightly and vanishes!", ch, stone, null, TO_CHAR)
        extract_obj(stone)
    }

    val portal = create_object(get_obj_index_nn(OBJ_VNUM_PORTAL), 0)
    portal.timer = 2 + level / 25
    portal.value[3] = victim.room.vnum.toLong()

    obj_to_room(portal, ch.room)

    act("\$p rises up from the ground.", ch, portal, null, TO_ROOM)
    act("\$p rises up before you.", ch, portal, null, TO_CHAR)
}

fun spell_nexus(level: Int, ch: CHAR_DATA) {
    val from_room = ch.room
    val victim = get_char_world(ch, target_name)
    val to_room = victim?.room
    if (victim == null
            || victim == ch
            || to_room == null
            || !can_see_room(ch, to_room) || !can_see_room(ch, from_room)
            || IS_SET(to_room.room_flags, ROOM_SAFE)
            || IS_SET(from_room.room_flags, ROOM_SAFE)
            || IS_SET(to_room.room_flags, ROOM_PRIVATE)
            || IS_SET(to_room.room_flags, ROOM_SOLITARY)
            || IS_SET(to_room.room_flags, ROOM_NOSUMMON)
            || victim.level >= level + 3
            || victim.pcdata != null && victim.level >= LEVEL_HERO  /* NOT trust */
            || victim.pcdata == null && is_safe_nomessage(ch, victim) && IS_SET(victim.imm_flags, IMM_SUMMON)
            || victim.pcdata == null && saves_spell(level, victim, DT.None)) {
        send_to_char("You failed.\n", ch)
        return
    }

    val stone = get_hold_char(ch)
    if (!IS_IMMORTAL(ch) && (stone == null || stone.item_type != ItemType.WarpStone)) {
        send_to_char("You lack the proper component for this spell.\n", ch)
        return
    }

    if (stone != null && stone.item_type == ItemType.WarpStone) {
        act("You draw upon the power of \$p.", ch, stone, null, TO_CHAR)
        act("It flares brightly and vanishes!", ch, stone, null, TO_CHAR)
        extract_obj(stone)
    }

    /* portal one */
    val portal1 = create_object(get_obj_index_nn(OBJ_VNUM_PORTAL), 0)
    portal1.timer = 1 + level / 10
    portal1.value[3] = to_room.vnum.toLong()

    obj_to_room(portal1, from_room)

    act("\$p rises up from the ground.", ch, portal1, null, TO_ROOM)
    act("\$p rises up before you.", ch, portal1, null, TO_CHAR)

    /* no second portal if rooms are the same */
    if (to_room == from_room) {
        return
    }

    /* portal two */
    val portal2 = create_object(get_obj_index_nn(OBJ_VNUM_PORTAL), 0)
    portal2.timer = 1 + level / 10
    portal2.value[3] = from_room.vnum.toLong()

    obj_to_room(portal2, to_room)

    act("\$p rises up from the ground.", to_room, portal2, null, TO_ROOM)
    act("\$p rises up from the ground.", to_room, portal2, null, TO_CHAR)
}

fun spell_bark_skin(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(ch, Skill.bark_skin)) {
        if (victim == ch) {
            send_to_char("Your skin is already covered in bark.\n", ch)
        } else {
            act("\$N is already as hard as can be.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.bark_skin
    af.level = level
    af.duration = level
    af.location = Apply.Ac
    af.modifier = (-(level * 1.5)).toInt()
    affect_to_char(victim, af)
    act("\$n's skin becomes covered in bark.", victim, null, null, TO_ROOM)
    send_to_char("Your skin becomes covered in bark.\n", victim)
}

fun spell_ranger_staff(level: Int, ch: CHAR_DATA) {
    val staff = create_object(get_obj_index_nn(OBJ_VNUM_RANGER_STAFF), level)
    send_to_char("You create a ranger's staff!\n", ch)
    act("\$n creates a ranger's staff!", ch, null, null, TO_ROOM)

    if (ch.level < 50) {
        staff.value[2] = (ch.level / 10).toLong()
    } else {
        staff.value[2] = (ch.level / 6 - 3).toLong()
    }
    staff.level = ch.level

    val tohit = Affect()
    tohit.where = TO_OBJECT
    tohit.type = Skill.ranger_staff
    tohit.level = ch.level
    tohit.duration = -1
    tohit.location = Apply.Hitroll
    tohit.modifier = 2 + level / 5
    affect_to_obj(staff, tohit)

    val todam = Affect()
    todam.where = TO_OBJECT
    todam.type = Skill.ranger_staff
    todam.level = ch.level
    todam.duration = -1
    todam.location = Apply.Damroll
    todam.modifier = 2 + level / 5
    affect_to_obj(staff, todam)

    staff.timer = level
    obj_to_char(staff, ch)
}

fun spell_transform(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.transform) || ch.hit > ch.max_hit) {
        send_to_char("You are already overflowing with health.\n", ch)
        return
    }

    ch.hit += Math.min(30000 - ch.max_hit, ch.max_hit)

    val af = Affect()
    af.type = Skill.transform
    af.level = level
    af.duration = 24
    af.location = Apply.Hit
    af.modifier = Math.min(30000 - ch.max_hit, ch.max_hit)
    affect_to_char(ch, af)

    send_to_char("Your mind clouds as your health increases.\n", ch)
}

fun spell_mental_knife(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = when {
        ch.level < 40 -> dice(level, 8)
        ch.level < 65 -> dice(level, 11)
        else -> dice(level, 14)
    }

    if (saves_spell(level, victim, DT.Mental)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.mental_knife, DT.Mental, true)

    if (is_affected(victim, Skill.mental_knife) || saves_spell(level, victim, DT.Mental)) return

    val af = Affect()
    af.type = Skill.mental_knife
    af.level = level
    af.duration = level
    af.location = Apply.Intelligence
    af.modifier = -7
    affect_to_char(victim, af)

    af.location = Apply.Wis
    affect_to_char(victim, af)
    act("Your mental knife sears \$N's mind!", ch, null, victim, TO_CHAR)
    act("\$n's mental knife sears your mind!", ch, null, victim, TO_VICT)
    act("\$n's mental knife sears \$N's mind!", ch, null, victim, TO_NOTVICT)
}

fun spell_demon_summon(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.demon_summon)) {
        send_to_char("You lack the power to summon another demon right now.\n", ch)
        return
    }

    send_to_char("You attempt to summon a demon.\n", ch)
    act("\$n attempts to summon a demon.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_DEMON) {
            send_to_char("Two demons are more than you can control!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val demon = create_mobile(get_mob_index_nn(MOB_VNUM_DEMON))
    demon.perm_stat.copyFrom(ch.perm_stat)
    val pcdata = ch.pcdata
    demon.max_hit = if (pcdata == null) URANGE(ch.max_hit, ch.max_hit, 30000) else URANGE(pcdata.perm_hit, ch.hit, 30000)
    demon.hit = demon.max_hit
    demon.max_mana = pcdata?.perm_mana ?: ch.max_mana
    demon.mana = demon.max_mana
    demon.level = ch.level

    demon.armor.fill(interpolate(demon.level, 100, -100), 0, 3)
    demon.armor[3] = interpolate(demon.level, 100, 0)
    demon.gold = 0
    demon.timer = 0
    demon.damage[DICE_NUMBER] = number_range(level / 15, level / 12)
    demon.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    demon.damage[DICE_BONUS] = number_range(level / 10, level / 8)

    char_to_room(demon, ch.room)
    send_to_char("A demon arrives from the underworld!\n", ch)
    act("A demon arrives from the underworld!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.demon_summon
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    if (randomPercent() < 40) {
        if (can_see(demon, ch)) {
            do_say(demon, "You dare disturb me?!?!!!")
        } else {
            do_say(demon, "Who dares disturb me?!?!!!")
        }
        do_murder(demon, ch.name)
    } else {
        demon.affected_by = SET_BIT(demon.affected_by, AFF_CHARM)
        demon.leader = ch
        demon.master = demon.leader
    }

}

fun spell_scourge(level: Int, ch: CHAR_DATA) {
    var dam = when {
        ch.level < 40 -> dice(level, 6)
        ch.level < 65 -> dice(level, 9)
        else -> dice(level, 12)
    }

    for (tmp_vict in ch.room.people) {
        if (!is_safe_spell(ch, tmp_vict, true)) {
            if (ch.pcdata != null && tmp_vict != ch &&
                    ch.fighting != tmp_vict && tmp_vict.fighting != ch &&
                    (IS_SET(tmp_vict.affected_by, AFF_CHARM) || tmp_vict.pcdata != null)) {
                if (!can_see(tmp_vict, ch)) {
                    do_yell(tmp_vict, "Help someone is attacking me!")
                } else {
                    do_yell(tmp_vict, "Die, ${doppel_name(ch, tmp_vict)}, you sorcerous dog!")
                }
            }

            if (!is_affected(tmp_vict, Skill.scourge)) {
                if (randomPercent() < level) {
                    spell_poison(level, ch, tmp_vict, TARGET_CHAR)
                }

                if (randomPercent() < level) {
                    spell_blindness(level, ch, tmp_vict)
                }

                if (randomPercent() < level) {
                    spell_weaken(level, tmp_vict)
                }

                if (saves_spell(level, tmp_vict, DT.Fire)) {
                    dam /= 2
                }
                damage(ch, tmp_vict, dam, Skill.scourge, DT.Fire, true)
            }
        }
    }
}

fun spell_doppelganger(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch == victim || is_affected(ch, Skill.doppelganger) && ch.doppel == victim) {
        act("You already look like \$M.", ch, null, victim, TO_CHAR)
        return
    }

    if (victim.pcdata == null) {
        act("\$N is too different from yourself to mimic.", ch, null, victim, TO_CHAR)
        return
    }

    if (IS_IMMORTAL(victim)) {
        send_to_char("Yeah, sure. And I'm the Pope.\n", ch)
        return
    }

    if (saves_spell(level, victim, DT.Charm)) {
        send_to_char("You failed.\n", ch)
        return
    }

    act("You change form to look like \$N.", ch, null, victim, TO_CHAR)
    act("\$n changes form to look like YOU!", ch, null, victim, TO_VICT)
    act("\$n changes form to look like \$N!", ch, null, victim, TO_NOTVICT)

    val af = Affect()
    af.type = Skill.doppelganger
    af.level = level
    af.duration = 2 * level / 3
    af.location = Apply.None

    affect_to_char(ch, af)
    ch.doppel = victim

}

fun spell_manacles(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (!IS_SET(victim.act, PLR_WANTED)) {
        act("But \$N is not wanted.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_affected(victim, Skill.manacles) || saves_spell(ch.level, victim, DT.Charm)) return

    val af = Affect()
    af.type = Skill.manacles
    af.level = level
    af.duration = 5 + level / 5

    af.modifier = 0 - (victim.curr_stat(PrimeStat.Dexterity) - 4)
    af.location = Apply.Dex
    affect_to_char(victim, af)

    af.modifier = -5
    af.location = Apply.Hitroll
    affect_to_char(victim, af)

    af.modifier = -10
    af.location = Apply.Damroll
    affect_to_char(victim, af)

    spell_charm_person(level, ch, victim)
}

fun spell_shield_ruler(level: Int, ch: CHAR_DATA) {
    val shield_vnum = when {
        level >= 71 -> OBJ_VNUM_RULER_SHIELD4
        level >= 51 -> OBJ_VNUM_RULER_SHIELD3
        level >= 31 -> OBJ_VNUM_RULER_SHIELD2
        else -> OBJ_VNUM_RULER_SHIELD1
    }

    val shield = create_object(get_obj_index_nn(shield_vnum), level)
    shield.timer = level
    shield.level = ch.level
    shield.cost = 0
    obj_to_char(shield, ch)

    val af = Affect()
    af.where = TO_OBJECT
    af.type = Skill.shield_of_ruler
    af.level = level
    af.duration = -1
    af.modifier = level / 8

    af.location = Apply.Hitroll
    affect_to_obj(shield, af)

    af.location = Apply.Damroll
    affect_to_obj(shield, af)


    af.where = TO_OBJECT
    af.type = Skill.shield_of_ruler
    af.level = level
    af.duration = -1
    af.modifier = -level / 2
    af.location = Apply.Ac
    affect_to_obj(shield, af)

    af.where = TO_OBJECT
    af.type = Skill.shield_of_ruler
    af.level = level
    af.duration = -1
    af.modifier = -level / 9
    af.location = Apply.SavingSpell
    affect_to_obj(shield, af)

    af.where = TO_OBJECT
    af.type = Skill.shield_of_ruler
    af.level = level
    af.duration = -1
    af.modifier = Math.max(1, level / 30)
    af.location = Apply.Cha
    affect_to_obj(shield, af)

    act("You create \$p!", ch, shield, null, TO_CHAR)
    act("\$n creates \$p!", ch, shield, null, TO_ROOM)
}

fun spell_guard_call(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.guard_call)) {
        send_to_char("You lack the power to call another three guards now.\n", ch)
        return
    }

    val buf = "Guards! Guards!"
    do_yell(ch, buf)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_SPECIAL_GUARD) {
            do_say(gch, "What? I'm not good enough?")
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val guard = create_mobile(get_mob_index_nn(MOB_VNUM_SPECIAL_GUARD))

    guard.perm_stat.copyFrom(ch.perm_stat)
    guard.max_hit = 2 * ch.max_hit
    guard.hit = guard.max_hit
    guard.max_mana = ch.max_mana
    guard.mana = guard.max_mana
    guard.alignment = ch.alignment
    guard.level = ch.level

    guard.armor.fill(interpolate(guard.level, 100, -200), 0, 3)
    guard.armor[3] = interpolate(guard.level, 100, -100)
    guard.sex = ch.sex
    guard.gold = 0
    guard.timer = 0

    guard.damage[DICE_NUMBER] = number_range(level / 16, level / 12)
    guard.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    guard.damage[DICE_BONUS] = number_range(level / 9, level / 6)

    guard.affected_by = SET_BIT(guard.affected_by, (A or C or D or E or F or G or H or BIT_30))
    guard.affected_by = SET_BIT(guard.affected_by, AFF_CHARM)
    guard.affected_by = SET_BIT(guard.affected_by, AFF_SANCTUARY)

    val guard2 = create_mobile(guard.pIndexData)
    clone_mobile(guard, guard2)

    val guard3 = create_mobile(guard.pIndexData)
    clone_mobile(guard, guard3)

    guard3.master = ch
    guard2.master = guard3.master
    guard.master = guard2.master
    guard3.leader = ch
    guard2.leader = guard3.leader
    guard.leader = guard2.leader

    char_to_room(guard, ch.room)
    char_to_room(guard2, ch.room)
    char_to_room(guard3, ch.room)
    send_to_char("Three guards come to your rescue!\n", ch)
    act("Three guards come to \$n's rescue!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.guard_call
    af.level = level
    af.duration = 6
    affect_to_char(ch, af)
}

fun spell_nightwalker(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.nightwalker)) {
        send_to_char("You feel too weak to summon a Nightwalker now.\n", ch)
        return
    }

    send_to_char("You attempt to summon a Nightwalker.\n", ch)
    act("\$n attempts to summon a Nightwalker.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_NIGHTWALKER) {
            send_to_char("Two Nightwalkers are more than you can control!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val walker = create_mobile(get_mob_index_nn(MOB_VNUM_NIGHTWALKER))

    walker.perm_stat.copyFrom(ch.perm_stat)
    val pcdata = ch.pcdata
    walker.max_hit = when (pcdata) {
        null -> URANGE(ch.max_hit, ch.max_hit, 30000)
        else -> URANGE(pcdata.perm_hit, pcdata.perm_hit, 30000)
    }
    walker.hit = walker.max_hit
    walker.max_mana = ch.max_mana
    walker.mana = walker.max_mana
    walker.level = ch.level

    walker.armor.fill(interpolate(walker.level, 100, -100), 0, 3)
    walker.armor[3] = interpolate(walker.level, 100, 0)
    walker.gold = 0
    walker.timer = 0
    walker.damage[DICE_NUMBER] = number_range(level / 15, level / 10)
    walker.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    walker.damage[DICE_BONUS] = 0

    char_to_room(walker, ch.room)
    send_to_char("A Nightwalker rises from the shadows!\n", ch)
    act("A Nightwalker rises from the shadows!", ch, null, null, TO_ROOM)
    send_to_char("A Nightwalker kneels before you.", ch)
    act("A Nightwalker kneels before ${ch.name}!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.nightwalker
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    walker.affected_by = SET_BIT(walker.affected_by, AFF_CHARM)
    walker.leader = ch
    walker.master = walker.leader

}

fun spell_eyes(ch: CHAR_DATA) {
    val victim = get_char_world(ch, target_name)
    if (victim == null) {
        send_to_char("Your spy network reveals no such player.\n", ch)
        return
    }

    if (victim.level > ch.level + 7 || saves_spell(ch.level + 9, victim, DT.None)) {
        send_to_char("Your spy network cannot find that player.\n", ch)
        return
    }

    if (ch == victim) {
        do_look(ch, "auto")
    } else {
        val ori_room = ch.room
        char_from_room(ch)
        char_to_room(ch, victim.room)
        do_look(ch, "auto")
        char_from_room(ch)
        char_to_room(ch, ori_room)
    }
}

fun spell_shadow_cloak(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.cabal != victim.cabal) {
        send_to_char("You may only use this spell on fellow cabal members.\n", ch)
        return
    }

    if (is_affected(victim, Skill.shadow_cloak)) {
        if (victim == ch) {
            send_to_char("You are already protected by a shadow cloak.\n", ch)
        } else {
            act("\$N is already protected by a shadow cloak.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.shadow_cloak
    af.level = level
    af.duration = 24
    af.modifier = -level
    af.location = Apply.Ac
    affect_to_char(victim, af)
    send_to_char("You feel the shadows protect you.\n", victim)
    if (ch != victim) {
        act("A cloak of shadows protect \$N.", ch, null, victim, TO_CHAR)
    }
}

fun spell_nightfall(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.nightfall)) {
        send_to_char("You can't find the power to control lights.\n", ch)
        return
    }

    for (vch in ch.room.people) {
        vch.carrying
                .filter { it.item_type == ItemType.Light && it.value[2] != 0L && !is_same_group(ch, vch) }
                .forEach {
                    if (/*light.value[2] != -1 ||*/ saves_spell(level, vch, DT.Energy)) {
                        act("\$p flickers and goes out!", ch, it, null, TO_CHAR)
                        act("\$p flickers and goes out!", ch, it, null, TO_ROOM)
                        it.value[2] = 0
                        if (get_light_char(ch) == null) {
                            ch.room.light--
                        }
                    }
                    /*    else {
            act("$p momentarily dims.",ch,light,null,TO_CHAR);
            act("$p momentarily dims.",ch,light,null,TO_ROOM);
          } */
                }
    }

    for (light in ch.room.objects) {
        if (light.item_type == ItemType.Light && light.value[2] != 0L) {
            act("\$p flickers and goes out!", ch, light, null, TO_CHAR)
            act("\$p flickers and goes out!", ch, light, null, TO_ROOM)
            light.value[2] = 0
        }
    }

    val af = Affect()
    af.type = Skill.nightfall
    af.level = level
    af.duration = 2
    affect_to_char(ch, af)
}

fun spell_mirror(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (victim.pcdata == null) {
        send_to_char("Only players can be mirrored.\n", ch)
        return
    }

    val mirrors = Index.CHARS.count { it.pcdata == null && is_affected(it, Skill.mirror) && is_affected(it, Skill.doppelganger) && it.doppel == victim }
    if (mirrors >= level / 5) {
        if (ch == victim) {
            send_to_char("You cannot be further mirrored.\n", ch)
        } else {
            act("\$N cannot be further mirrored.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.level = level

    var tmp_vict = victim
    while (tmp_vict.doppel != null) tmp_vict = tmp_vict.doppel!!

    val v_pcdata = tmp_vict.pcdata
    val title = v_pcdata?.title ?: tmp_vict.short_desc
    val order = number_range(0, level / 5 - mirrors)
    var new_mirrors = 0
    while (mirrors + new_mirrors < level / 5) {
        val gch = create_mobile(get_mob_index_nn(MOB_VNUM_MIRROR_IMAGE))
        gch.name = tmp_vict.name
        gch.short_desc = tmp_vict.name
        gch.long_desc = "${tmp_vict.name}$title is here.\n"
        gch.description = tmp_vict.description
        gch.sex = tmp_vict.sex

        af.type = Skill.doppelganger
        af.duration = level
        affect_to_char(gch, af)
        af.type = Skill.mirror
        af.duration = -1
        affect_to_char(gch, af)

        gch.hit = 1
        gch.max_hit = gch.hit
        gch.level = 1
        gch.doppel = victim
        gch.master = victim
        char_to_room(gch, victim.room)

        if (randomPercent() < 20) {
            val ori_room = victim.room
            char_from_room(victim)
            char_to_room(victim, ori_room)
        }

        if (new_mirrors == order) {
            char_from_room(victim)
            char_to_room(victim, gch.room)
        }


        if (ch === victim) {
            send_to_char("A mirror image of yourself appears beside you!\n", ch)
            act("A mirror image of \$n appears beside \$M!", ch, null, victim, TO_ROOM)
        } else {
            act("A mirror of \$N appears beside \$M!", ch, null, victim, TO_CHAR)
            act("A mirror of \$N appears beside \$M!", ch, null, victim, TO_NOTVICT)
            send_to_char("A mirror image of yourself appears beside you!\n", victim)
        }
        new_mirrors++
    }
}

fun spell_garble(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch == victim) {
        send_to_char("Garble whose speech?\n", ch)
        return
    }

    if (is_affected(victim, Skill.garble)) {
        act("\$N's speech is already garbled.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_safe_nomessage(ch, victim)) {
        send_to_char("You cannot garble that person.\n", ch)
        return
    }

    if (victim.level > ch.level + 7 || saves_spell(ch.level + 9, victim, DT.Mental)) {
        return
    }

    val af = Affect()
    af.type = Skill.garble
    af.level = level
    af.duration = 10
    affect_to_char(victim, af)

    act("You have garbled \$N's speech!", ch, null, victim, TO_CHAR)
    send_to_char("You feel your tongue contort.\n", victim)
}

fun spell_confuse(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.confuse)) {
        act("\$N is already thoroughly confused.", ch, null, victim, TO_CHAR)
        return
    }

    if (saves_spell(level, victim, DT.Mental)) {
        return
    }

    val af = Affect()
    af.type = Skill.confuse
    af.level = level
    af.duration = 10
    affect_to_char(victim, af)

    val count = ch.room.people.count { it == ch && !can_see(ch, it) && get_trust(ch) < it.invis_level }
    val rch = ch.room.people.firstOrNull { it != ch && can_see(ch, it) && get_trust(ch) >= it.invis_level && number_range(1, count) == 1 }
    if (rch != null) {
        do_murder(victim, rch.name)
    }
    do_murder(victim, ch.name)
}

fun spell_terangreal(level: Int, victim: CHAR_DATA) {
    if (victim.pcdata == null) {
        return
    }

    val af = Affect()
    af.type = Skill.terangreal
    af.level = level
    af.duration = 10
    af.bitvector = AFF_SLEEP
    affect_join(victim, af)

    if (IS_AWAKE(victim)) {
        send_to_char("You are overcome by a sudden surge of fatigue.\n", victim)
        act("\$n falls into a deep sleep.", victim, null, null, TO_ROOM)
        victim.position = Pos.Sleeping
    }

}

fun spell_kassandra(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.kassandra)) {
        send_to_char("The kassandra has been used for this purpose too recently.\n", ch)
        return
    }
    val af = Affect()
    af.type = Skill.kassandra
    af.level = level
    af.duration = 5
    affect_to_char(ch, af)
    ch.hit = Math.min(ch.hit + 150, ch.max_hit)
    update_pos(ch)
    send_to_char("A warm feeling fills your body.\n", ch)
    act("\$n looks better.", ch, null, null, TO_ROOM)
}


fun spell_sebat(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.sebat)) {
        send_to_char("The kassandra has been used for that too recently.\n", ch)
        return
    }
    val af = Affect()
    af.type = Skill.sebat
    af.level = level
    af.duration = level
    af.location = Apply.Ac
    af.modifier = -30
    affect_to_char(ch, af)
    act("\$n is surrounded by a mystical shield.", ch, null, null, TO_ROOM)
    send_to_char("You are surrounded by a mystical shield.\n", ch)
}


fun spell_matandra(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(ch, Skill.matandra)) {
        send_to_char("The kassandra has been used for this purpose too recently.\n", ch)
        return
    }
    val af = Affect()
    af.type = Skill.matandra
    af.level = level
    af.duration = 5
    affect_to_char(ch, af)

    val dam = dice(level, 7)
    damage(ch, victim, dam, Skill.matandra, DT.Holy, true)

}

fun spell_amnesia(victim: CHAR_DATA) {
    val pcdata = victim.pcdata ?: return

    for (i in 0 until MAX_SKILL) {
        pcdata.learned[i] /= 2
    }
    act("You feel your memories slip away.", victim, null, null, TO_CHAR)
    act("\$n gets a blank look on \$s face.", victim, null, null, TO_ROOM)
}


fun spell_chaos_blade(level: Int, ch: CHAR_DATA) {
    val blade = create_object(get_obj_index_nn(OBJ_VNUM_CHAOS_BLADE), level)
    send_to_char("You create a blade of chaos!\n", ch)
    act("\$n creates a blade of chaos!", ch, null, null, TO_ROOM)

    blade.timer = level * 2
    blade.level = ch.level

    blade.value[2] = when {
        ch.level <= 10 -> 2
        ch.level <= 20 -> 3
        ch.level <= 30 -> 4
        ch.level <= 40 -> 5
        ch.level <= 50 -> 6
        ch.level <= 60 -> 7
        ch.level <= 70 -> 9
        ch.level <= 80 -> 11
        else -> 12
    }

    val af = Affect()
    af.where = TO_OBJECT
    af.type = Skill.chaos_blade
    af.level = level
    af.duration = -1
    af.modifier = level / 6

    af.location = Apply.Hitroll
    affect_to_obj(blade, af)

    af.location = Apply.Damroll
    affect_to_obj(blade, af)

    obj_to_char(blade, ch)
}

fun spell_tattoo(ch: CHAR_DATA, victim: CHAR_DATA) {
    if (victim.pcdata == null) {
        act("\$N is too dumb to worship you!", ch, null, victim, TO_CHAR)
        return
    }

    for (r in Religion.values()) {
        if (eq(ch.name, r.leader)) {
            var tattoo = get_eq_char(victim, Wear.Tattoo)
            if (tattoo != null) {
                act("\$N is already tattooed!  You'll have to remove it first.", ch, null, victim, TO_CHAR)
                act("\$n tried to give you another tattoo but failed.", ch, null, victim, TO_VICT)
                act("\$n tried to give \$N another tattoo but failed.", ch, null, victim, TO_NOTVICT)
                return
            } else {
                tattoo = create_object(get_obj_index_nn(r.tattooVnum), 60)
                act("You tattoo \$N with \$p!", ch, tattoo, victim, TO_CHAR)
                act("\$n tattoos \$N with \$p!", ch, tattoo, victim, TO_NOTVICT)
                act("\$n tattoos you with \$p!", ch, tattoo, victim, TO_VICT)

                obj_to_char(tattoo, victim)
                equip_char(victim, tattoo, Wear.Tattoo)
                return
            }
        }
    }
    send_to_char("You don't have a religious tattoo.\n", ch)
}

fun spell_remove_tattoo(ch: CHAR_DATA, victim: CHAR_DATA) {
    val tattoo = get_eq_char(victim, Wear.Tattoo)
    if (tattoo == null) {
        act("\$N doesn't have any tattoos.", ch, null, victim, TO_CHAR)
        return
    }
    extract_obj(tattoo)
    act("Through a painful process, your tattoo has been destroyed by \$n.", ch, null, victim, TO_VICT)
    act("You remove the tattoo from \$N.", ch, null, victim, TO_CHAR)
    act("\$N's tattoo is destroyed by \$n.", ch, null, victim, TO_NOTVICT)
}


fun spell_wrath(level: Int, ch: CHAR_DATA, vo: CHAR_DATA) {
    var victim = vo
    if (ch.pcdata != null && ch.isEvil()) {
        victim = ch
    }

    if (victim.isGood()) {
        act("The gods protect \$N.", ch, null, victim, TO_ROOM)
        return
    }

    if (victim.isNeutralA()) {
        act("\$N does not seem to be affected.", ch, null, victim, TO_CHAR)
        return
    }

    var dam = dice(level, 12)

    if (saves_spell(level, victim, DT.Holy)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.wrath, DT.Holy, true)

    if (IS_AFFECTED(victim, AFF_CURSE) || saves_spell(level, victim, DT.Holy)) {
        return
    }

    val af = Affect()
    af.type = Skill.wrath
    af.level = level
    af.duration = 2 * level
    af.location = Apply.Hitroll
    af.modifier = -1 * (level / 8)
    af.bitvector = AFF_CURSE
    affect_to_char(victim, af)

    af.location = Apply.SavingSpell
    af.modifier = level / 8
    affect_to_char(victim, af)

    send_to_char("You feel unclean.\n", victim)
    if (ch != victim) {
        act("\$N looks very uncomfortable.", ch, null, victim, TO_CHAR)
    }
}

@Suppress("unused")
fun spell_old_randomizer(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.randomizer)) {
        send_to_char("Your power of randomness has been exhausted for now.\n", ch)
        return
    }
    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("This room is far too orderly for your powers to work on it.\n", ch)
        return
    }
    val af = Affect()
    af.type = Skill.randomizer
    af.level = Math.min(level + 15, MAX_LEVEL)

    val pRoomIndex = get_room_index_nn(ch.room.vnum)

    if (flipCoin()) {
        send_to_char("Despite your efforts, the universe resisted chaos.\n", ch)
        if (ch.trust >= 56) {
            af.duration = 1
        } else {
            af.duration = level
        }
        affect_to_char(ch, af)
        return
    }
    var d0 = 0
    while (d0 < 5) {
        val d1 = number_range(d0, 5)
        val exit = pRoomIndex.exit[d0]
        pRoomIndex.exit[d0] = pRoomIndex.exit[d1]
        pRoomIndex.exit[d1] = exit
        d0++

    }
    if (ch.trust >= 56) {
        af.duration = 1
    } else {
        af.duration = 2 * level
    }
    affect_to_char(ch, af)
    send_to_char("The room was successfully randomized!\n", ch)
    send_to_char("You feel very drained from the effort.\n", ch)
    ch.hit -= Math.min(200, ch.hit / 2)

    log_string("${ch.name} used randomizer in room ${ch.room.vnum}")
}

fun spell_stalker(level: Int, ch: CHAR_DATA) {
    val victim = get_char_world(ch, target_name)
    if (victim == null || victim == ch || victim.pcdata == null || !IS_SET(victim.act, PLR_WANTED)) {
        send_to_char("You failed.\n", ch)
        return
    }

    if (is_affected(ch, Skill.stalker)) {
        send_to_char("This power is used too recently.\n", ch)
        return
    }

    if (!is_safe_nomessage(ch, victim) && !IS_SET(ch.act, PLR_CANINDUCT)) {
        send_to_char("You better use special guards for this purpose.\n", ch)
        return
    }

    send_to_char("You attempt to summon a stalker.\n", ch)
    act("\$n attempts to summon a stalker.", ch, null, null, TO_ROOM)

    val stalker = create_mobile(get_mob_index_nn(MOB_VNUM_STALKER))
    val af = Affect()
    af.type = Skill.stalker
    af.level = level
    af.duration = 6
    affect_to_char(ch, af)

    stalker.perm_stat.copyFrom(victim.perm_stat)
    stalker.max_hit = Math.min(30000, 2 * victim.max_hit)
    stalker.hit = stalker.max_hit
    stalker.max_mana = victim.max_mana
    stalker.mana = stalker.max_mana
    stalker.level = victim.level

    stalker.damage[DICE_NUMBER] = number_range(victim.level / 8, victim.level / 6)
    stalker.damage[DICE_TYPE] = number_range(victim.level / 6, victim.level / 5)
    stalker.damage[DICE_BONUS] = number_range(victim.level / 10, victim.level / 8)

    stalker.hitroll = victim.level
    stalker.damroll = victim.level
    stalker.size = victim.size

    stalker.armor.fill(interpolate(stalker.level, 100, -100), 0, 3)
    stalker.armor[3] = interpolate(stalker.level, 100, 0)
    stalker.gold = 0
    stalker.invis_level = LEVEL_IMMORTAL
    stalker.affected_by = (H or J or N or O or U or V or BIT_26 or BIT_28 or (A or B or C or D or E or F or G or H or BIT_30))

    char_to_room(stalker, victim.room)
    stalker.last_fought = victim
    send_to_char("An invisible stalker arrives to stalk you!\n", victim)
    act("An invisible stalker arrives to stalk \$n!", victim, null, null, TO_ROOM)
    send_to_char("An invisible stalker has been sent.\n", ch)

    log_string("${ch.name} used stalker on ${victim.name}")
}


fun spell_tesseract(level: Int, ch: CHAR_DATA) {
    val victim = get_char_world(ch, target_name)

    if (victim == null || victim == ch) {
        send_to_char("You failed.\n", ch)
        return
    }

    if (!can_see_room(ch, victim.room)
            || is_safe(ch, victim) && IS_SET(victim.act, PLR_NOSUMMON)
            || room_is_private(victim.room)
            || IS_SET(victim.room.room_flags, ROOM_NO_RECALL)
            || IS_SET(ch.room.room_flags, ROOM_NO_RECALL)
            || IS_SET(victim.room.room_flags, ROOM_NOSUMMON)
            || IS_SET(ch.room.room_flags, ROOM_NOSUMMON)
            || victim.pcdata != null && victim.level >= LEVEL_HERO  /* NOT trust */
            || victim.pcdata == null && IS_SET(victim.imm_flags, IMM_SUMMON)
            || victim.pcdata != null && IS_SET(victim.act, PLR_NOSUMMON)
            && is_safe_nomessage(ch, victim)
            || saves_spell(level, victim, DT.None)) {
        send_to_char("You failed.\n", ch)
        return
    }

    val pet = ch.pet

    for (wch in ch.room.people) {
        if (is_same_group(wch, ch) && wch != ch) {
            act("\$n utters some strange words and, with a sickening lurch, you feel time", ch, null, wch, TO_VICT)
            act("and space shift around you.", ch, null, wch, TO_VICT)
            char_from_room(wch)
            char_to_room(wch, victim.room)
            act("\$n arrives suddenly.", wch, null, null, TO_ROOM)
            do_look(wch, "auto")
        }
    }

    act("With a sudden flash of light, \$n and \$s friends disappear!", ch, null, null, TO_ROOM)
    send_to_char("As you utter the words, time and space seem to blur.  You feel as though\nspace and time are shifting all around you while you remain motionless.\n", ch)
    char_from_room(ch)
    char_to_room(ch, victim.room)

    act("\$n arrives suddenly.", ch, null, null, TO_ROOM)
    do_look(ch, "auto")

    if (pet != null && ch.room == pet.room) {
        send_to_char("You feel time and space shift around you.\n", pet)
        char_from_room(pet)
        char_to_room(pet, victim.room)
        act("\$n arrives suddenly.", pet, null, null, TO_ROOM)
        do_look(pet, "auto")
    }
}

fun spell_brew(level: Int, ch: CHAR_DATA, obj: Obj) {
    if (obj.item_type != ItemType.Trash && obj.item_type != ItemType.Treasure && obj.item_type != ItemType.Key) {
        send_to_char("That can't be transformed into a potion.\n", ch)
        return
    }

    if (obj.wear_loc.isOn()) {
        send_to_char("The item must be carried to be brewed.\n", ch)
        return
    }

    val vial = ch.carrying.firstOrNull { it.pIndexData.vnum == OBJ_VNUM_POTION_VIAL }
    if (vial == null) {
        send_to_char("You don't have any vials to brew the potion into.\n", ch)
        return
    }

    if (randomPercent() < 50) {
        send_to_char("You failed and destroyed it.\n", ch)
        extract_obj(obj)
        return
    }

    val potion = when (obj.item_type) {
        ItemType.Trash -> create_object(get_obj_index_nn(OBJ_VNUM_POTION_SILVER), level)
        ItemType.Treasure -> create_object(get_obj_index_nn(OBJ_VNUM_POTION_GOLDEN), level)
        else -> create_object(get_obj_index_nn(OBJ_VNUM_POTION_SWIRLING), level)
    }


    potion.value[0] = level.toLong()

    val spell = when (obj.item_type) {
        ItemType.Trash -> {
            val p = randomPercent()
            when {
                p < 20 -> Skill.fireball
                p < 40 -> Skill.cure_poison
                p < 60 -> Skill.cure_blindness
                p < 80 -> Skill.cure_disease
                else -> Skill.word_of_recall
            }
        }
        ItemType.Treasure -> when (random(8)) {
            0 -> Skill.cure_critical
            1 -> Skill.haste
            2 -> Skill.frenzy
            3 -> Skill.create_spring
            4 -> Skill.holy_word
            5 -> Skill.invisibility
            6 -> Skill.cure_light
            else -> Skill.cure_serious
        }
        else -> {
            val p = randomPercent()
            when {
                p < 20 -> Skill.detect_magic
                p < 40 -> Skill.detect_invis
                p < 65 -> Skill.pass_door
                else -> Skill.acute_vision
            }
        }
    }
    potion.value[1] = spell.ordinal.toLong()
    extract_obj(obj)
    act("You brew \$p from your resources!", ch, potion, null, TO_CHAR)
    act("\$n brews \$p from \$s resources!", ch, potion, null, TO_ROOM)

    obj_to_char(potion, ch)
    extract_obj(vial)
}


fun spell_shadowlife(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (victim.pcdata == null) {
        send_to_char("Now, why would you want to do that?!?\n", ch)
        return
    }

    if (is_affected(ch, Skill.shadowlife)) {
        send_to_char("You don't have the strength to raise a Shadow now.\n", ch)
        return
    }

    act("You give life to \$N's shadow!", ch, null, victim, TO_CHAR)
    act("\$n gives life to \$N's shadow!", ch, null, victim, TO_NOTVICT)
    act("\$n gives life to your shadow!", ch, null, victim, TO_VICT)

    val shadow = create_mobile(get_mob_index_nn(MOB_VNUM_SHADOW))

    shadow.perm_stat.copyFrom(ch.perm_stat)

    shadow.max_hit = 3 * ch.max_hit / 4
    shadow.hit = shadow.max_hit
    shadow.max_mana = 3 * ch.max_mana / 4
    shadow.mana = shadow.max_mana
    shadow.alignment = ch.alignment
    shadow.level = ch.level

    shadow.armor.fill(interpolate(shadow.level, 100, -100), 0, 3)
    shadow.armor[3] = interpolate(shadow.level, 100, 0)
    shadow.sex = victim.sex
    shadow.gold = 0

    val name = if (victim.pcdata == null) victim.short_desc else victim.name
    shadow.short_desc = shadow.short_desc.format(name)
    shadow.long_desc = shadow.long_desc.format(name)
    shadow.description = shadow.description.format(name)

    char_to_room(shadow, ch.room)

    do_murder(shadow, victim.name)
    val af = Affect()
    af.type = Skill.shadowlife
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)
}

fun spell_ruler_badge(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (count_worn(ch, Wear.Neck) >= max_can_wear(ch, Wear.Neck)) {
        send_to_char("But you are wearing something else.\n", ch)
        return
    }

    for (badge in ch.carrying) {
        if (badge.pIndexData.vnum == OBJ_VNUM_DEPUTY_BADGE || badge.pIndexData.vnum == OBJ_VNUM_RULER_BADGE) {
            act("Your \$p vanishes.", ch, badge, null, TO_CHAR)
            obj_from_char(badge)
            extract_obj(badge)
        }
    }

    val badge = create_object(get_obj_index_nn(OBJ_VNUM_RULER_BADGE), level)
    val af = Affect()
    af.where = TO_OBJECT
    af.type = Skill.ruler_badge
    af.level = level
    af.duration = -1
    af.modifier = 100 + level / 2

    af.location = Apply.Hit
    affect_to_obj(badge, af)

    af.location = Apply.Mana
    affect_to_obj(badge, af)

    af.where = TO_OBJECT
    af.type = Skill.ruler_badge
    af.level = level
    af.duration = -1
    af.modifier = level / 8

    af.location = Apply.Hitroll
    affect_to_obj(badge, af)

    af.location = Apply.Damroll
    affect_to_obj(badge, af)


    badge.timer = 200
    act("You wear the ruler badge!", ch, null, null, TO_CHAR)
    act("\$n wears the \$s ruler badge!", ch, null, null, TO_ROOM)

    obj_to_char(badge, victim)
    equip_char(ch, badge, Wear.Neck)
    ch.hit = Math.min(ch.hit + 100 + level / 2, ch.max_hit)
    ch.mana = Math.min(ch.mana + 100 + level / 2, ch.max_mana)
}

fun spell_remove_badge(ch: CHAR_DATA, victim: CHAR_DATA) {
    for (badge in victim.carrying) {
        if (badge.pIndexData.vnum == OBJ_VNUM_DEPUTY_BADGE || badge.pIndexData.vnum == OBJ_VNUM_RULER_BADGE) {
            act("Your \$p vanishes.", ch, badge, null, TO_CHAR)
            act("\$n's \$p vanishes.", ch, badge, null, TO_ROOM)

            obj_from_char(badge)
            extract_obj(badge)
        }
    }
}

fun spell_dragon_strength(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.dragon_strength)) {
        send_to_char("You are already full of the strength of the dragon.\n", ch)
        return
    }

    val af = Affect()
    af.type = Skill.dragon_strength
    af.level = level
    af.duration = level / 3

    af.modifier = 2
    af.location = Apply.Hitroll
    affect_to_char(ch, af)

    af.modifier = 2
    af.location = Apply.Damroll
    affect_to_char(ch, af)

    af.modifier = 10
    af.location = Apply.Ac
    affect_to_char(ch, af)

    af.modifier = 2
    af.location = Apply.Str
    affect_to_char(ch, af)

    af.modifier = -2
    af.location = Apply.Dex
    affect_to_char(ch, af)

    send_to_char("The strength of the dragon enters you.\n", ch)
    act("\$n looks a bit meaner now.", ch, null, null, TO_ROOM)
}

fun spell_dragon_breath(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 6)
    if (is_safe_spell(ch, victim, true)) return
    if (saves_spell(level, victim, DT.Fire)) dam /= 2
    damage(ch, victim, dam, Skill.dragon_breath, DT.Fire, true)
}

fun spell_golden_aura(level: Int, ch: CHAR_DATA) {
    for (vch in ch.room.people) {
        if (!is_same_group(vch, ch)) {
            continue
        }

        if (is_affected(vch, Skill.golden_aura) || is_affected(vch, Skill.bless) || IS_AFFECTED(vch, AFF_PROTECT_EVIL)) {
            when (vch) {
                ch -> send_to_char("You are already protected by a golden aura.\n", ch)
                else -> act("\$N is already protected by a golden aura.", ch, null, vch, TO_CHAR)
            }
            continue
        }

        val af = Affect()
        af.type = Skill.golden_aura
        af.level = level
        af.duration = 6 + level
        af.bitvector = AFF_PROTECT_EVIL
        affect_to_char(vch, af)

        af.modifier = level / 8
        af.location = Apply.Hitroll
        affect_to_char(vch, af)

        af.modifier = 0 - level / 8
        af.location = Apply.SavingSpell
        affect_to_char(vch, af)

        send_to_char("You feel a golden aura around you.\n", vch)
        if (ch != vch) {
            act("A golden aura surrounds \$N.", ch, null, vch, TO_CHAR)
        }
    }
}

fun spell_dragonplate(level: Int, ch: CHAR_DATA) {
    val plate_vnum = OBJ_VNUM_PLATE

    val plate = create_object(get_obj_index_nn(plate_vnum), level + 5)
    plate.timer = 2 * level
    plate.cost = 0
    plate.level = ch.level

    val af = Affect()
    af.where = TO_OBJECT
    af.type = Skill.dragonplate
    af.level = level
    af.duration = -1
    af.modifier = level / 8

    af.location = Apply.Hitroll
    affect_to_obj(plate, af)

    af.location = Apply.Damroll
    affect_to_obj(plate, af)

    obj_to_char(plate, ch)

    act("You create \$p!", ch, plate, null, TO_CHAR)
    act("\$n creates \$p!", ch, plate, null, TO_ROOM)
}

fun spell_squire(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.squire)) {
        send_to_char("You cannot command another squire right now.\n", ch)
        return
    }

    send_to_char("You attempt to summon a squire.\n", ch)
    act("\$n attempts to summon a squire.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_SQUIRE) {
            send_to_char("Two squires are more than you need!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val squire = create_mobile(get_mob_index_nn(MOB_VNUM_SQUIRE))

    squire.perm_stat.copyFrom(ch.perm_stat)

    squire.max_hit = ch.max_hit
    squire.hit = squire.max_hit
    squire.max_mana = ch.max_mana
    squire.mana = squire.max_mana
    squire.level = ch.level

    squire.armor.fill(interpolate(squire.level, 100, -100), 0, 3)
    squire.armor[3] = interpolate(squire.level, 100, 0)
    squire.gold = 0

    squire.short_desc = squire.short_desc.format(ch.name)
    squire.long_desc = squire.long_desc.format(ch.name)
    squire.description = squire.description.format(ch.name)

    squire.damage[DICE_NUMBER] = number_range(level / 15, level / 12)
    squire.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    squire.damage[DICE_BONUS] = number_range(level / 8, level / 6)

    char_to_room(squire, ch.room)
    send_to_char("A squire arrives from nowhere!\n", ch)
    act("A squire arrives from nowhere!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.squire
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    squire.affected_by = SET_BIT(squire.affected_by, AFF_CHARM)
    squire.leader = ch
    squire.master = squire.leader
}


fun spell_dragonsword(level: Int, ch: CHAR_DATA) {
    val argb = StringBuilder()
    target_name = one_argument(target_name, argb)
    val arg = argb.toString()

    val sword_vnum = when {
        eq(arg, "sword") -> OBJ_VNUM_DRAGONSWORD
        eq(arg, "mace") -> OBJ_VNUM_DRAGONMACE
        eq(arg, "dagger") -> OBJ_VNUM_DRAGONDAGGER
        eq(arg, "lance") -> OBJ_VNUM_DRAGONLANCE
        else -> {
            send_to_char("You can't make a DragonSword like that!", ch)
            return
        }
    }

    val sword = create_object(get_obj_index_nn(sword_vnum), level)
    sword.timer = level * 2
    sword.cost = 0
    sword.value[2] = if (ch.level < 50) (ch.level / 10).toLong() else (ch.level / 6 - 3).toLong()
    sword.level = ch.level

    val af = Affect()
    af.where = TO_OBJECT
    af.type = Skill.dragonsword
    af.level = level
    af.duration = -1
    af.modifier = level / 5

    af.location = Apply.Hitroll
    affect_to_obj(sword, af)

    af.location = Apply.Damroll
    affect_to_obj(sword, af)

    sword.extra_flags = when (ch.align) {
        Align.Good -> SET_BIT(sword.extra_flags, ITEM_ANTI_NEUTRAL or ITEM_ANTI_EVIL)
        Align.Neutral -> SET_BIT(sword.extra_flags, ITEM_ANTI_GOOD or ITEM_ANTI_EVIL)
        Align.Evil -> SET_BIT(sword.extra_flags, ITEM_ANTI_NEUTRAL or ITEM_ANTI_GOOD)
    }
    obj_to_char(sword, ch)

    act("You create \$p!", ch, sword, null, TO_CHAR)
    act("\$n creates \$p!", ch, sword, null, TO_ROOM)
}

fun spell_entangle(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.room.sector_type == SECT_INSIDE ||
            ch.room.sector_type == SECT_CITY ||
            ch.room.sector_type == SECT_DESERT ||
            ch.room.sector_type == SECT_AIR) {
        send_to_char("No plants can grow here.\n", ch)
        return
    }

    //        int dam = number_range(level, 4 * level);
    //        if (saves_spell(level, victim, DT.Pierce)) {
    //            dam /= 2;
    //        }

    //todo: ch->level -> dam?
    damage(ch, victim, ch.level, Skill.entangle, DT.Pierce, true)

    act("The thorny plants spring up around \$n, entangling \$s legs!", victim, null, null, TO_ROOM)
    act("The thorny plants spring up around you, entangling your legs!", victim, null, null, TO_CHAR)

    victim.move -= dice(level, 6)
    victim.move = Math.max(0, victim.move)

    if (!is_affected(victim, Skill.entangle)) {
        val a = Affect()
        a.type = Skill.entangle
        a.level = level
        a.duration = level / 10
        a.location = Apply.Dex
        a.modifier = -(level / 10)
        affect_to_char(victim, a)

    }
}

fun spell_holy_armor(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.holy_armor)) {
        send_to_char("You are already protected from harm.", ch)
        return
    }
    val af = Affect()
    af.type = Skill.holy_armor
    af.level = level
    af.duration = level
    af.location = Apply.Ac
    af.modifier = -Math.max(10, 10 * (level / 5))
    affect_to_char(ch, af)
    act("\$n is protected from harm.", ch, null, null, TO_ROOM)
    send_to_char("Your are protected from harm.\n", ch)

}

fun spell_love_potion(level: Int, ch: CHAR_DATA) {
    val af = Affect()
    af.type = Skill.love_potion
    af.level = level
    af.duration = 50
    affect_to_char(ch, af)

    send_to_char("You feel like looking at people.\n", ch)
}

fun spell_protective_shield(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.protective_shield)) {
        if (victim == ch) {
            send_to_char("You are already surrounded by a protective shield.\n",
                    ch)
        } else {
            act("\$N is already surrounded by a protective shield.", ch, null,
                    victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.protective_shield
    af.level = level
    af.duration = number_fuzzy(level / 30) + 3
    af.location = Apply.Ac
    af.modifier = 20
    affect_to_char(victim, af)
    act("\$n is surrounded by a protective shield.", victim, null, null, TO_ROOM)
    send_to_char("You are surrounded by a protective shield.\n", victim)
}

fun spell_deafen(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch == victim) {
        send_to_char("Deafen who?\n", ch)
        return
    }

    if (is_affected(victim, Skill.deafen)) {
        act("\$N is already deaf.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_safe_nomessage(ch, victim)) {
        send_to_char("You cannot deafen that person.\n", ch)
        return
    }

    if (saves_spell(level, victim, DT.None)) {
        return
    }

    val af = Affect()
    af.type = Skill.deafen
    af.level = level
    af.duration = 10
    affect_to_char(victim, af)

    act("You have deafened \$N!", ch, null, victim, TO_CHAR)
    send_to_char("A loud ringing fills your ears...you can't hear anything!\n",
            victim)
}

fun spell_disperse(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.disperse)) {
        send_to_char("You aren't up to dispersing this crowd.\n", ch)
        return
    }

    for (vch in ch.room.people) {
        if (!IS_SET(vch.room.room_flags, ROOM_NO_RECALL) && !IS_IMMORTAL(vch) && (vch.pcdata == null && !IS_SET(vch.act, ACT_AGGRESSIVE) || vch.pcdata != null && vch.level > PK_MIN_LEVEL && !is_safe_nomessage(ch, vch)) && vch != ch && !IS_SET(vch.imm_flags, IMM_SUMMON)) {
            var pRoomIndex: ROOM_INDEX_DATA?
            while (true) {
                pRoomIndex = get_room_index(number_range(0, 65535))
                if (pRoomIndex != null) {
                    if (can_see_room(ch, pRoomIndex) && !room_is_private(pRoomIndex) && !IS_SET(pRoomIndex.room_flags, ROOM_NO_RECALL)) {
                        break
                    }
                }
            }

            send_to_char("The world spins around you!\n", vch)
            act("\$n vanishes!", vch, null, null, TO_ROOM)
            char_from_room(vch)
            char_to_room(vch, pRoomIndex)
            act("\$n slowly fades into existence.", vch, null, null, TO_ROOM)
            do_look(vch, "auto")
        }
    }
    val af = Affect()
    af.type = Skill.disperse
    af.level = level
    af.duration = 10
    af.location = Apply.None
    affect_to_char(ch, af)
}


fun spell_acute_vision(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_ACUTE_VISION)) {
        if (victim == ch) {
            send_to_char("Your vision is already acute. \n", ch)
        } else {
            act("\$N already sees acutely.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.acute_vision
    af.level = level
    af.duration = level
    af.bitvector = AFF_ACUTE_VISION
    affect_to_char(victim, af)
    send_to_char("Your vision sharpens.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_dragons_breath(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    act("You call the dragon lord to help you.", ch, null, null, TO_CHAR)
    act("\$n start to breath like a dragon.", ch, null, victim, TO_NOTVICT)
    act("\$n breath disturbs you!", ch, null, victim, TO_VICT)
    act("You breath the breath of lord of Dragons.", ch, null, null, TO_CHAR)

    val hpch = Math.max(10, ch.hit)
    val hp_dam = number_range(hpch / 9 + 1, hpch / 5)
    val dice_dam = dice(level, 20)
    val dam = Math.max(hp_dam + dice_dam / 5, dice_dam + hp_dam / 5)

    when (dice(1, 5)) {
        1 -> {
            fire_effect(victim.room, level, dam / 2, TARGET_ROOM)

            victim.room.people
                    .filter { !is_safe_spell(ch, it, true) && !(it.pcdata == null && ch.pcdata == null && (ch.fighting != it || it.fighting != ch)) && !is_safe(ch, it) }
                    .forEach {
                        if (it == victim)
                        /* full damage */ {
                            if (saves_spell(level, it, DT.Fire)) {
                                fire_effect(it, level / 2, dam / 4, TARGET_CHAR)
                                damage(ch, it, dam / 2, Skill.dragon_breath, DT.Fire, true)
                            } else {
                                fire_effect(it, level, dam, TARGET_CHAR)
                                damage(ch, it, dam, Skill.dragon_breath, DT.Fire, true)
                            }
                        } else
                        /* partial damage */ {
                            if (saves_spell(level - 2, it, DT.Fire)) {
                                fire_effect(it, level / 4, dam / 8, TARGET_CHAR)
                                damage(ch, it, dam / 4, Skill.dragon_breath, DT.Fire, true)
                            } else {
                                fire_effect(it, level / 2, dam / 4, TARGET_CHAR)
                                damage(ch, it, dam / 2, Skill.dragon_breath, DT.Fire, true)
                            }
                        }
                    }
        }
        2 ->
            if (saves_spell(level, victim, DT.Acid)) {
                acid_effect(victim, level / 2, dam / 4, TARGET_CHAR)
                damage(ch, victim, dam / 2, Skill.dragon_breath, DT.Acid, true)
            } else {
                acid_effect(victim, level, dam, TARGET_CHAR)
                damage(ch, victim, dam, Skill.dragon_breath, DT.Acid, true)
            }
        3 -> {
            cold_effect(victim.room, level, dam / 2, TARGET_ROOM)
            victim.room.people
                    .filter { !is_safe_spell(ch, it, true) && !(it.pcdata == null && ch.pcdata == null && (ch.fighting != it || it.fighting != ch)) && !is_safe(ch, it) }
                    .forEach {
                        if (it == victim) { /* full damage */
                            if (saves_spell(level, it, DT.Cold)) {
                                cold_effect(it, level / 2, dam / 4, TARGET_CHAR)
                                damage(ch, it, dam / 2, Skill.dragon_breath, DT.Cold, true)
                            } else {
                                cold_effect(it, level, dam, TARGET_CHAR)
                                damage(ch, it, dam, Skill.dragon_breath, DT.Cold, true)
                            }
                        } else {
                            if (saves_spell(level - 2, it, DT.Cold)) {
                                cold_effect(it, level / 4, dam / 8, TARGET_CHAR)
                                damage(ch, it, dam / 4, Skill.dragon_breath, DT.Cold, true)
                            } else {
                                cold_effect(it, level / 2, dam / 4, TARGET_CHAR)
                                damage(ch, it, dam / 2, Skill.dragon_breath, DT.Cold, true)
                            }
                        }
                    }
        }
        4 -> {
            poison_effect(ch.room, level, dam, TARGET_ROOM)
            for (vch in ch.room.people) {
                if (is_safe_spell(ch, vch, true) || ch.pcdata == null && vch.pcdata == null && (ch.fighting == vch || vch.fighting == ch)) {
                    continue
                }
                if (is_safe(ch, vch)) {
                    continue
                }
                if (ch.pcdata != null && vch != ch && ch.fighting != vch && vch.fighting != ch && (IS_SET(vch.affected_by, AFF_CHARM) || vch.pcdata != null)) {
                    if (!can_see(vch, ch)) {
                        do_yell(vch, "Help someone is attacking me!")
                    } else {
                        do_yell(vch, "Die, ${doppel_name(ch, vch)}, you sorcerous dog!")
                    }
                }

                if (saves_spell(level, vch, DT.Poison)) {
                    poison_effect(vch, level / 2, dam / 4, TARGET_CHAR)
                    damage(ch, vch, dam / 2, Skill.dragon_breath, DT.Poison, true)
                } else {
                    poison_effect(vch, level, dam, TARGET_CHAR)
                    damage(ch, vch, dam, Skill.dragon_breath, DT.Poison, true)
                }
            }
        }
        5 ->
            if (saves_spell(level, victim, DT.Lightning)) {
                shock_effect(victim, level / 2, dam / 4, TARGET_CHAR)
                damage(ch, victim, dam / 2, Skill.dragon_breath, DT.Lightning, true)
            } else {
                shock_effect(victim, level, dam, TARGET_CHAR)
                damage(ch, victim, dam, Skill.dragon_breath, DT.Lightning, true)
            }
    }
}

fun spell_sand_storm(level: Int, ch: CHAR_DATA) {
    if (ch.room.sector_type == SECT_AIR || ch.room.sector_type == SECT_WATER_SWIM || ch.room.sector_type == SECT_WATER_NO_SWIM) {
        send_to_char("You don't find any sand here to make storm.\n", ch)
        ch.wait = 0
        return
    }

    act("\$n creates a storm with sands on the floor.", ch, null, null, TO_ROOM)
    act("You create the ..sand.. storm.", ch, null, null, TO_CHAR)

    val hpch = Math.max(10, ch.hit)
    val hp_dam = number_range(hpch / 9 + 1, hpch / 5)
    val dice_dam = dice(level, 20)

    val dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)
    sand_effect(ch.room, level, dam / 2, TARGET_ROOM)

    ch.room.people
            .filter { !is_safe_spell(ch, it, true) && !(it.pcdata == null && ch.pcdata == null && ch.fighting != it) && !is_safe(ch, it) }
            .forEach {
                if (saves_spell(level, it, DT.Cold)) {
                    sand_effect(it, level / 2, dam / 4, TARGET_CHAR)
                    damage(ch, it, dam / 2, Skill.sand_storm, DT.Cold, true)
                } else {
                    sand_effect(it, level, dam, TARGET_CHAR)
                    damage(ch, it, dam, Skill.sand_storm, DT.Cold, true)
                }
            }
}

fun spell_scream(level: Int, ch: CHAR_DATA) {
    act("\$n screames with a disturbing NOISE!.", ch, null, null, TO_ROOM)
    act("You scream with a powerful sound.", ch, null, null, TO_CHAR)

    val hpch = Math.max(10, ch.hit)
    val hp_dam = number_range(hpch / 9 + 1, hpch / 5)
    val dice_dam = dice(level, 20)
    val dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)

    scream_effect(ch.room, level, dam / 2, TARGET_ROOM)

    ch.room.people
            .filter { !is_safe_spell(ch, it, true) && !is_safe(ch, it) }
            .forEach {
                if (saves_spell(level, it, DT.Energy)) {
                    WAIT_STATE(it, PULSE_VIOLENCE)
                    scream_effect(it, level / 2, dam / 4, TARGET_CHAR)
                    // damage(ch,vch,dam/2,sn,DT.Energy,true);
                    // if (vch.fighting)  stop_fighting( vch , true );
                } else {
                    WAIT_STATE(it, Skill.scream.beats + PULSE_VIOLENCE)
                    scream_effect(it, level, dam, TARGET_CHAR)
                    // damage(ch,vch,dam,sn,DT.Energy,true); */
                    if (it.fighting != null) {
                        stop_fighting(it, true)
                    }
                }
            }
}

fun spell_attract_other(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.sex == victim.sex) {
        send_to_char("You'd better try your chance on other sex!\n", ch)
        return
    }
    spell_charm_person(level, ch, victim)
}

fun spell_animate_dead(level: Int, ch: CHAR_DATA, vo: Any, target: Int) {
    if (target != TARGET_OBJ) {
        if (vo === ch) {
            send_to_char("But you aren't dead!!\n", ch)
            return
        }
        send_to_char("But it ain't dead!!\n", ch)
        return
    }
    val obj = vo as Obj
    if (!(obj.item_type == ItemType.CorpseNpc || obj.item_type == ItemType.CorpsePc)) {
        send_to_char("You can animate only corpses!\n", ch)
        return
    }
    /* if (obj.item_type == ItemType.CorpsePc) { send_to_char("The magic fails abruptly!\n",ch); return; } */
    if (is_affected(ch, Skill.animate_dead)) {
        send_to_char("You cannot summon the strength to handle more undead bodies.\n", ch)
        return
    }

    if (count_charmed(ch) != 0) {
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_MOB)) {
        send_to_char("You can't animate deads here.\n", ch)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_SAFE) || IS_SET(ch.room.room_flags, ROOM_PRIVATE) || IS_SET(ch.room.room_flags, ROOM_SOLITARY)) {
        send_to_char("You can't animate here.\n", ch)
        return
    }

    val undead = create_mobile(get_mob_index_nn(MOB_VNUM_UNDEAD))
    char_to_room(undead, ch.room)
    undead.perm_stat.fill { Math.min(25, 2 * ch.perm_stat[it]) }

    val pcdata = ch.pcdata
    undead.max_hit = pcdata?.perm_hit ?: ch.max_hit
    undead.hit = undead.max_hit
    undead.max_mana = pcdata?.perm_mana ?: ch.max_mana
    undead.mana = undead.max_mana
    undead.alignment = ch.alignment
    undead.level = Math.min(100, ch.level - 2)

    undead.armor.fill(interpolate(undead.level, 100, -100), 0, 3)
    undead.armor[3] = interpolate(undead.level, 50, -200)
    undead.damage[DICE_NUMBER] = number_range(level / 20, level / 15)
    undead.damage[DICE_TYPE] = number_range(level / 6, level / 3)
    undead.damage[DICE_BONUS] = number_range(level / 12, level / 10)
    undead.sex = ch.sex
    undead.gold = 0

    undead.act = SET_BIT(undead.act, ACT_UNDEAD)
    undead.affected_by = SET_BIT(undead.affected_by, AFF_CHARM)
    undead.master = ch
    undead.leader = ch

    undead.name = "${obj.name} body undead"
    var argument = obj.short_desc
    val argb = StringBuilder()
    val buf3 = StringBuilder()
    while (argument.isNotEmpty()) {
        argb.setLength(0)
        argument = one_argument(argument, argb)
        val arg = argb.toString()
        if (!(eq(arg, "The") || eq(arg, "undead") || eq(arg, "body") || eq(arg, "corpse") || eq(arg, "of"))) {
            if (buf3.isEmpty()) {
                buf3.append(arg)
            } else {
                buf3.append(" ")
                buf3.append(arg)
            }
        }
    }
    undead.short_desc = "The undead body of $buf3"
    undead.long_desc = "The undead body of $buf3 slowly staggers around.\n"

    for (obj2 in obj.contains) {
        obj_from_obj(obj2)
        obj_to_char(obj2, undead)
    }
    interpret(undead, "wear all", true)

    val af = Affect()
    af.type = Skill.animate_dead
    af.level = ch.level
    af.duration = ch.level / 10
    affect_to_char(ch, af)

    send_to_char("With mystic power, you animate it!\n", ch)
    act("With mystic power, ${ch.name} animates ${obj.name}!", ch, null, null, TO_ROOM)
    act("${obj.short_desc} looks at you and plans to make you pay for distrurbing its rest!", ch, null, null, TO_CHAR)
    extract_obj(obj)
}

fun spell_enhanced_armor(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.enchant_armor)) {
        if (victim == ch) {
            send_to_char("You are already enhancedly armored.\n", ch)
        } else {
            act("\$N is already enhancedly armored.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.enchant_armor
    af.level = level
    af.duration = 24
    af.modifier = -60
    af.location = Apply.Ac
    affect_to_char(victim, af)
    send_to_char("You feel protected for all attacks.\n", victim)
    if (ch != victim) {
        act("\$N is protected by your magic.", ch, null, victim, TO_CHAR)
    }
}


fun spell_meld_into_stone(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.meld_into_stone)) {
        if (victim == ch) {
            send_to_char("Your skin is already covered with stone.\n", ch)
        } else {
            act("\$N's skin is already covered with stone.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.meld_into_stone
    af.level = level
    af.duration = level / 10
    af.location = Apply.Ac
    af.modifier = -100
    affect_to_char(victim, af)
    act("\$n's skin melds into stone.", victim, null, null, TO_ROOM)
    send_to_char("Your skin melds into stone.\n", victim)
}

fun spell_web(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (saves_spell(level, victim, DT.Other)) {
        return
    }

    if (is_affected(victim, Skill.web)) {
        if (victim == ch) {
            send_to_char("You are already webbed.\n", ch)
        } else {
            act("\$N is already webbed.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.web
    af.level = level
    af.duration = 1
    af.location = Apply.Hitroll
    af.modifier = -1 * (level / 6)
    af.bitvector = AFF_WEB
    affect_to_char(victim, af)

    af.location = Apply.Dex
    af.modifier = -2
    affect_to_char(victim, af)

    af.location = Apply.Damroll
    af.modifier = -1 * (level / 6)
    affect_to_char(victim, af)
    send_to_char("You are emeshed in thick webs!\n", victim)
    if (ch !== victim) {
        act("You emesh \$N in a bundle of webs!", ch, null, victim, TO_CHAR)
    }
}


fun spell_group_defense(level: Int, ch: CHAR_DATA) {
    val shield_sn = Skill.shield
    val armor_sn = Skill.armor

    for (gch in ch.room.people) {
        if (!is_same_group(gch, ch)) {
            continue
        }
        if (is_affected(gch, armor_sn)) {
            if (gch == ch) {
                send_to_char("You are already armored.\n", ch)
            } else {
                act("\$N is already armored.", ch, null, gch, TO_CHAR)
            }
            continue
        }

        val af = Affect()
        af.type = armor_sn
        af.level = level
        af.duration = level
        af.location = Apply.Ac
        af.modifier = -20
        affect_to_char(gch, af)

        send_to_char("You feel someone protecting you.\n", gch)
        if (ch != gch) {
            act("\$N is protected by your magic.",
                    ch, null, gch, TO_CHAR)
        }
        if (!is_same_group(gch, ch)) {
            continue
        }
        if (is_affected(gch, shield_sn)) {
            if (gch == ch) {
                send_to_char("You are already shielded.\n", ch)
            } else {
                act("\$N is already shielded.", ch, null, gch, TO_CHAR)
            }
            continue
        }

        af.type = shield_sn
        af.level = level
        af.duration = level
        af.location = Apply.Ac
        af.modifier = -20
        affect_to_char(gch, af)

        send_to_char("You are surrounded by a force shield.\n", gch)
        if (ch != gch) {
            act("\$N is surrounded by a force shield.",
                    ch, null, gch, TO_CHAR)
        }
    }
}


fun spell_inspire(level: Int, ch: CHAR_DATA) {
    for (gch in ch.room.people) {
        if (!is_same_group(gch, ch)) {
            continue
        }
        if (is_affected(gch, Skill.bless)) {
            if (gch == ch) {
                send_to_char("You are already inspired.\n", ch)
            } else {
                act("\$N is already inspired.",
                        ch, null, gch, TO_CHAR)
            }
            continue
        }
        val af = Affect()
        af.type = Skill.bless
        af.level = level
        af.duration = 6 + level
        af.location = Apply.Hitroll
        af.modifier = level / 12
        affect_to_char(gch, af)

        af.location = Apply.SavingSpell
        af.modifier = 0 - level / 12
        affect_to_char(gch, af)

        send_to_char("You feel inspired!\n", gch)
        if (ch != gch) {
            act("You inspire \$N with the Creator's power!", ch, null, gch, TO_CHAR)
        }
    }
}

fun spell_mass_sanctuary(level: Int, ch: CHAR_DATA) {
    for (gch in ch.room.people) {
        if (!is_same_group(gch, ch)) {
            continue
        }
        if (IS_AFFECTED(gch, AFF_SANCTUARY)) {
            if (gch == ch) {
                send_to_char("You are already in sanctuary.\n", ch)
            } else {
                act("\$N is already in sanctuary.", ch, null, gch, TO_CHAR)
            }
            continue
        }
        val af = Affect()
        af.type = Skill.sanctuary
        af.level = level
        af.duration = number_fuzzy(level / 6)
        af.bitvector = AFF_SANCTUARY
        affect_to_char(gch, af)

        send_to_char("You are surrounded by a white aura.\n", gch)
        if (ch != gch) {
            act("\$N is surrounded by a white aura.",
                    ch, null, gch, TO_CHAR)
        }
    }
}

fun spell_mend(ch: CHAR_DATA, obj: Obj) {
    if (obj.condition > 99) {
        send_to_char("That item is not in need of mending.\n", ch)
        return
    }

    if (obj.wear_loc.isOn()) {
        send_to_char("The item must be carried to be mended.\n", ch)
        return
    }

    val skill = get_skill(ch, Skill.mend) / 2
    var result = randomPercent() + skill

    if (IS_OBJ_STAT(obj, ITEM_GLOW)) {
        result -= 5
    }
    if (IS_OBJ_STAT(obj, ITEM_MAGIC)) {
        result += 5
    }

    when {
        result >= 50 -> {
            act("\$p glows brightly, and is whole again.  Good Job!", ch, obj, null, TO_CHAR)
            act("\$p glows brightly, and is whole again.", ch, obj, null, TO_ROOM)
            obj.condition += result
            obj.condition = Math.min(obj.condition, 100)
        }
        result >= 10 -> send_to_char("Nothing seemed to happen.\n", ch)
        else -> {
            act("\$p flares blindingly... and evaporates!", ch, obj, null, TO_CHAR)
            act("\$p flares blindingly... and evaporates!", ch, obj, null, TO_ROOM)
            extract_obj(obj)
        }
    }
}

fun spell_shielding(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (saves_spell(level, victim, DT.None)) {
        act("\$N shivers slightly, but it passes quickly.",
                ch, null, victim, TO_CHAR)
        send_to_char("You shiver slightly, but it passes quickly.\n", victim)
        return
    }

    if (is_affected(victim, Skill.shielding)) {
        val af = Affect()
        af.type = Skill.shielding
        af.level = level
        af.duration = level / 20
        affect_to_char(victim, af)
        act("You wrap \$N in more flows of Spirit.", ch, null, victim, TO_CHAR)
        send_to_char("You feel the shielding get stronger.\n", victim)
        return
    }

    val af = Affect()
    af.type = Skill.shielding
    af.level = level
    af.duration = level / 15
    affect_join(victim, af)

    send_to_char("You feel as if you have lost touch with something.\n", victim)
    act("You shield \$N from the true Source.", ch, null, victim, TO_CHAR)
}


fun spell_link(ch: CHAR_DATA, victim: CHAR_DATA) {
    val random = randomPercent()
    var tmpmana = ch.mana
    ch.mana = 0
    ch.endur /= 2
    tmpmana = (0.5 * tmpmana).toInt()
    tmpmana = (tmpmana + random) / 2
    victim.mana = victim.mana + tmpmana
}

fun spell_power_kill(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    act("{rA stream of darkness from your finger surrounds \$N.{x", ch, null, victim, TO_CHAR, Pos.Resting)
    act("{rA stream of darkness from \$n's finger surrounds \$N.{x", ch, null, victim, TO_NOTVICT, Pos.Resting)
    act("{rA stream of darkness from \$N's finger surrounds you.{x", victim, null, ch, TO_CHAR, Pos.Resting)

    if (saves_spell(level, victim, DT.Mental) || flipCoin()) {
        val dam = dice(level, 24)
        damage(ch, victim, dam, Skill.power_word_kill, DT.Mental, true)
        return
    }

    send_to_char("You have been KILLED!\n", victim)

    act("\$N has been killed!\n", ch, null, victim, TO_CHAR)
    act("\$N has been killed!\n", ch, null, victim, TO_ROOM)

    raw_kill(victim)
}

fun spell_lion_help(level: Int, ch: CHAR_DATA) {
    val arg = StringBuilder()
    target_name = one_argument(target_name, arg)
    if (arg.isEmpty()) {
        send_to_char("Whom do you want to have killed.\n", ch)
        return
    }

    val victim = get_char_area(ch, arg.toString())
    if (victim == null) {
        send_to_char("Noone around with that name.\n", ch)
        return
    }
    if (is_safe_nomessage(ch, victim)) {
        send_to_char("God protects your victim.\n", ch)
        return
    }

    send_to_char("You call for a hunter lion.\n", ch)
    act("\$n shouts a hunter lion.", ch, null, null, TO_ROOM)

    if (is_affected(ch, Skill.lion_help)) {
        send_to_char("You cannot summon the strength to handle more lion right now.\n", ch)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_MOB)) {
        send_to_char("No lions can listen you.\n", ch)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_SAFE) ||
            IS_SET(ch.room.room_flags, ROOM_PRIVATE) ||
            IS_SET(ch.room.room_flags, ROOM_SOLITARY) ||
            ch.room.exit[0] == null &&
                    ch.room.exit[1] == null &&
                    ch.room.exit[2] == null &&
                    ch.room.exit[3] == null &&
                    ch.room.exit[4] == null &&
                    ch.room.exit[5] == null ||
            ch.room.sector_type != SECT_FIELD &&
                    ch.room.sector_type != SECT_FOREST &&
                    ch.room.sector_type != SECT_MOUNTAIN &&
                    ch.room.sector_type != SECT_HILLS) {
        send_to_char("No hunter lion can come to you.\n", ch)
        return
    }

    val lion = create_mobile(get_mob_index_nn(MOB_VNUM_HUNTER))
    lion.perm_stat.fill { Math.min(25, 2 * ch.perm_stat[it]) }
    lion.max_hit = Math.min(30000, (ch.max_hit * 1.2).toInt())
    lion.hit = lion.max_hit
    lion.max_mana = ch.max_mana
    lion.mana = lion.max_mana
    lion.alignment = ch.alignment
    lion.level = Math.min(100, ch.level)
    lion.armor.fill(interpolate(lion.level, 100, -100), 0, 3)
    lion.armor[3] = interpolate(lion.level, 100, 0)
    lion.sex = ch.sex
    lion.gold = 0
    lion.damage[DICE_NUMBER] = number_range(level / 15, level / 10)
    lion.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    lion.damage[DICE_BONUS] = number_range(level / 8, level / 6)
    lion.affected_by = lion.affected_by or (A or B or C or D or E or F or G or H or BIT_30)

    /*   SET_BIT(lion.affected_by, AFF_CHARM);
  lion.master = lion.leader = ch; */

    char_to_room(lion, ch.room)

    send_to_char("A hunter lion comes to kill your victim!\n", ch)
    act("A hunter lion comes to kill \$n's victim!", ch, null, null, TO_ROOM)
    val af = Affect()
    af.type = Skill.lion_help
    af.level = ch.level
    af.duration = 24
    affect_to_char(ch, af)
    lion.act = SET_BIT(lion.act, ACT_HUNTER)
    lion.hunting = victim
    hunt_victim(lion)
}


fun spell_magic_jar(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (victim == ch) {
        send_to_char("You like yourself even better.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("Your victim is a npc. Not necessary!.\n", ch)
        return
    }

    if (saves_spell(level, victim, DT.Mental)) {
        send_to_char("You failed.\n", ch)
        return
    }
    val vial = ch.carrying.firstOrNull { it.pIndexData.vnum == OBJ_VNUM_POTION_VIAL }
    if (vial == null) {
        send_to_char("You don't have any vials to put your victim's spirit.\n", ch)
        return
    }
    extract_obj(vial)
    val i = when {
        ch.isGood() -> 0
        ch.isEvil() -> 2
        else -> 1
    }

    val fire = create_object(get_obj_index_nn(OBJ_VNUM_MAGIC_JAR), 0)
    fire.owner = ch.name
    fire.from = ch.name
    fire.altar = ch.hometown.altar[i]
    fire.pit = ch.hometown.pit[i]
    fire.level = ch.level
    fire.name = fire.name.format(victim.name)
    fire.short_desc = fire.short_desc.format(victim.name)
    fire.description = fire.description.format(victim.name)

    fire.extra_desc = EXTRA_DESC_DATA()
    fire.extra_desc.keyword = fire.pIndexData.extra_descr!!.keyword
    fire.extra_desc.description = fire.pIndexData.extra_descr!!.description.format(victim.name)
    fire.extra_desc.next = null

    fire.level = ch.level
    fire.timer = ch.level
    fire.cost = 0
    obj_to_char(fire, ch)
    victim.act = SET_BIT(victim.act, PLR_NO_EXP)
    send_to_char("You catch %s's spirit in to your vial.\n".format(victim.name), ch)
}

fun turn_spell(sn: Skill, level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (victim != ch) {
        act("\$n raises \$s hand, and a blinding ray of light shoots forth!", ch, null, null, TO_ROOM)
        send_to_char("You raise your hand and a blinding ray of light shoots forth!\n", ch)
    }

    if (victim.isGood() || victim.isNeutralA()) {
        act("\$n seems unharmed by the light.", victim, null, victim, TO_ROOM)
        send_to_char("The light seems powerless to affect you.\n", victim)
        return
    }

    var dam = dice(level, 10)
    if (saves_spell(level, victim, DT.Holy)) {
        dam /= 2
    }

    var align = victim.alignment
    align -= 350

    if (align < -1000) {
        align = -1000 + (align + 1000) / 3
    }

    dam = dam * align * align / 1000000

    damage(ch, victim, dam, sn, DT.Holy, true)

    /* cabal guardians */
    if (victim.pcdata == null && victim.cabal != Clan.None) {
        return
    }

    if (!(victim.pcdata == null && victim.position != Pos.Dead || victim.pcdata != null && current_time - victim.last_death_time > 10)) return
    val was_in = victim.room
    var door = 0
    while (door < 6) {
        val pexit = was_in.exit[door]
        if (pexit == null
                || IS_SET(pexit.exit_info, EX_CLOSED)
                || ch.pcdata == null && IS_SET(pexit.to_room.room_flags, ROOM_NO_MOB)) {
            door++
            continue
        }

        move_char(victim, door)
        val now_in = victim.room
        if (now_in == was_in) {
            door++
            continue
        }

        victim.room = was_in
        act("\$n has fled!", victim, null, null, TO_ROOM)
        victim.room = now_in

        if (victim.pcdata == null) {
            victim.last_fought = null
        }

        stop_fighting(victim, true)
        return
    }
}

fun spell_turn(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.turn)) {
        send_to_char("This power is used too recently.", ch)
        return
    }
    val af = Affect()
    af.type = Skill.turn
    af.level = level
    af.duration = 5
    affect_to_char(ch, af)

    if (ch.isEvil()) {
        send_to_char("The energy explodes inside you!\n", ch)
        turn_spell(Skill.turn, ch.level, ch, ch)
        return
    }

    ch.room.people
            .filter { !is_safe_spell(ch, it, true) && !is_safe(ch, it) }
            .forEach { turn_spell(Skill.turn, ch.level, ch, it) }
}


fun spell_fear(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (victim.clazz === Clazz.SAMURAI && victim.level >= 10) {
        send_to_char("Your victim is beyond this power.\n", ch)
        return
    }

    if (is_affected(victim, Skill.fear) || saves_spell(level, victim, DT.Other)) {
        return
    }

    val af = Affect()
    af.type = Skill.fear
    af.level = level
    af.duration = level / 10
    af.bitvector = AFF_FEAR
    affect_to_char(victim, af)
    send_to_char("You are afraid as much as a rabbit.\n", victim)
    act("\$n looks with afraid eyes.", victim, null, null, TO_ROOM)
}

fun spell_protection_heat(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.protection_heat)) {
        if (victim == ch) {
            send_to_char("You are already protected from heat.\n", ch)
        } else {
            act("\$N is already protected from heat.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (is_affected(victim, Skill.protection_cold)) {
        if (victim == ch) {
            send_to_char("You are already protected from cold.\n", ch)
        } else {
            act("\$N is already protected from cold.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (is_affected(victim, Skill.fire_shield)) {
        if (victim == ch) {
            send_to_char("You are already using fire shield.\n", ch)
        } else {
            act("\$N is already using fire shield.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.protection_heat
    af.level = level
    af.duration = 24
    af.location = Apply.SavingSpell
    af.modifier = -1
    affect_to_char(victim, af)
    send_to_char("You feel strengthed against heat.\n", victim)
    if (ch != victim) {
        act("\$N is protected against heat.", ch, null, victim, TO_CHAR)
    }
}

fun spell_protection_cold(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.protection_cold)) {
        if (victim == ch) {
            send_to_char("You are already protected from cold.\n", ch)
        } else {
            act("\$N is already protected from cold.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (is_affected(victim, Skill.protection_heat)) {
        if (victim == ch) {
            send_to_char("You are already protected from heat.\n", ch)
        } else {
            act("\$N is already protected from heat.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (is_affected(victim, Skill.fire_shield)) {
        if (victim == ch) {
            send_to_char("You are already using fire shield.\n", ch)
        } else {
            act("\$N is already using fire shield.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.protection_cold
    af.level = level
    af.duration = 24
    af.location = Apply.SavingSpell
    af.modifier = -1
    affect_to_char(victim, af)
    send_to_char("You feel strengthed against cold.\n", victim)
    if (ch != victim) {
        act("\$N is protected against cold.", ch, null, victim, TO_CHAR)
    }
}

fun spell_fire_shield(ch: CHAR_DATA) {
    val arg = StringBuilder()
    target_name = one_argument(target_name, arg)
    if (!(eq(arg.toString(), "cold") || eq(arg.toString(), "fire"))) {
        send_to_char("You must specify the type.\n", ch)
        return
    }
    val i = when {
        ch.isGood() -> 0
        ch.isEvil() -> 2
        else -> 1
    }

    val fire = create_object(get_obj_index_nn(OBJ_VNUM_FIRE_SHIELD), 0)
    fire.owner = ch.name
    fire.from = ch.name
    fire.altar = ch.hometown.altar[i]
    fire.pit = ch.hometown.pit[i]
    fire.level = ch.level

    fire.short_desc = fire.short_desc.format(arg)
    fire.description = fire.description.format(arg)
    fire.extra_desc = EXTRA_DESC_DATA()
    fire.extra_desc.keyword = fire.pIndexData.extra_descr!!.keyword
    fire.extra_desc.description = fire.pIndexData.extra_descr!!.description.format(arg)
    fire.extra_desc.next = null

    fire.level = ch.level
    fire.cost = 0
    fire.timer = 5 * ch.level
    fire.extra_flags = when (ch.align) {
        Align.Good -> SET_BIT(fire.extra_flags, ITEM_ANTI_NEUTRAL or ITEM_ANTI_EVIL)
        Align.Neutral -> SET_BIT(fire.extra_flags, ITEM_ANTI_GOOD or ITEM_ANTI_EVIL)
        Align.Evil -> SET_BIT(fire.extra_flags, ITEM_ANTI_NEUTRAL or ITEM_ANTI_GOOD)
    }
    obj_to_char(fire, ch)
    send_to_char("You create the fire shield.\n", ch)
}

fun spell_witch_curse(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.witch_curse)) {
        send_to_char("It has already underflowing with health.\n", ch)
        return
    }

    if (saves_spell(level + 5, victim, DT.Mental) || flipCoin()) {
        send_to_char("You failed!\n", ch)
        return
    }

    ch.hit -= 2 * level
    ch.hit = Math.max(ch.hit, 1)

    val af = Affect()
    af.type = Skill.witch_curse
    af.level = level
    af.duration = 24
    af.location = Apply.Hit
    af.modifier = -level
    affect_to_char(victim, af)

    send_to_char("Now he got the path to death.\n", ch)
}

fun spell_knock(ch: CHAR_DATA) {
    val arg = StringBuilder()
    target_name = one_argument(target_name, arg)

    if (arg.isEmpty()) {
        send_to_char("Knock which door or direction.\n", ch)
        return
    }

    if (ch.fighting != null) {
        send_to_char("Wait until the fight finishes.\n", ch)
        return
    }

    val door = find_door(ch, arg.toString())
    val pexit = if (door >= 0) ch.room.exit[door] else null
    if (pexit == null) {
        send_to_char("You can't see that here.\n", ch)
        return
    }
    if (!IS_SET(pexit.exit_info, EX_CLOSED)) {
        send_to_char("It's already open.\n", ch)
        return
    }
    if (!IS_SET(pexit.exit_info, EX_LOCKED)) {
        send_to_char("Just try to open it.\n", ch)
        return
    }
    if (IS_SET(pexit.exit_info, EX_NO_PASS)) {
        send_to_char("A mystical shield protects the exit.\n", ch)
        return
    }
    var chance = ch.level / 5 + ch.curr_stat(PrimeStat.Intelligence) + get_skill(ch, Skill.knock) / 5

    act("You knock \$d, and try to open \$d!", ch, null, pexit.keyword, TO_CHAR)
    act("You knock \$d, and try to open \$d!", ch, null, pexit.keyword, TO_ROOM)

    if (room_dark(ch.room)) {
        chance /= 2
    }

    /* now the attack */
    if (randomPercent() >= chance) {
        act("You couldn't knock the \$d!", ch, null, pexit.keyword, TO_CHAR)
        act("\$n failed to knock \$d.", ch, null, pexit.keyword, TO_ROOM)
        return
    }
    pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_LOCKED)
    pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_CLOSED)
    act("\$n knocks the the \$d and opens the lock.", ch, null, pexit.keyword, TO_ROOM)
    send_to_char("You successed to open the door.\n", ch)

    /* open the other side */
    val to_room = pexit.to_room
    val exit = to_room.exit
    val pexit_rev = exit[rev_dir[door]]
    if (pexit_rev != null && pexit_rev.to_room == ch.room) {
        pexit_rev.exit_info = REMOVE_BIT(pexit_rev.exit_info, EX_CLOSED)
        pexit_rev.exit_info = REMOVE_BIT(pexit_rev.exit_info, EX_LOCKED)
        to_room.people.forEach { act("The \$d opens.", it, null, pexit_rev.keyword, TO_CHAR) }
    }
}


fun spell_magic_resistance(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.magic_resistance)) {
        send_to_char("You are already resistive to magic.\n", ch)
    }
    val af = Affect()
    af.where = TO_RESIST
    af.type = Skill.magic_resistance
    af.duration = level / 10
    af.level = ch.level
    af.bitvector = RES_MAGIC
    affect_to_char(ch, af)
    send_to_char("You are now resistive to magic.\n", ch)
    return
}

fun spell_wolf(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.wolf)) {
        send_to_char("You lack the power to summon another wolf right now.\n", ch)
        return
    }

    send_to_char("You attempt to summon a wolf.\n", ch)
    act("\$n attempts to summon a wolf.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_WOLF) {
            send_to_char("Two wolfs are more than you can control!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val demon = create_mobile(get_mob_index_nn(MOB_VNUM_WOLF))
    demon.perm_stat.copyFrom(ch.perm_stat)

    val pcdata = ch.pcdata
    demon.max_hit = if (pcdata == null) URANGE(ch.max_hit, ch.max_hit, 30000) else URANGE(pcdata.perm_hit, ch.hit, 30000)
    demon.hit = demon.max_hit
    demon.max_mana = pcdata?.perm_mana ?: ch.max_mana
    demon.mana = demon.max_mana
    demon.level = ch.level

    demon.armor.fill(interpolate(demon.level, 100, -100), 0, 3)
    demon.armor[3] = interpolate(demon.level, 100, 0)
    demon.gold = 0
    demon.timer = 0
    demon.damage[DICE_NUMBER] = number_range(level / 15, level / 10)
    demon.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    demon.damage[DICE_BONUS] = number_range(level / 6, level / 5)

    char_to_room(demon, ch.room)
    send_to_char("The wolf arrives and bows before you!\n", ch)
    act("A wolf arrives from somewhere and bows!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.wolf
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    demon.affected_by = SET_BIT(demon.affected_by, AFF_CHARM)
    demon.leader = ch
    demon.master = demon.leader
}

fun spell_vampiric_blast(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 12)
    if (saves_spell(level, victim, DT.Acid)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.vampiric_blast, DT.Acid, true)
}

fun spell_dragon_skin(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.dragon_skin)) {
        if (victim == ch) {
            send_to_char("Your skin is already hard as rock.\n", ch)
        } else {
            act("\$N's skin is already hard as rock.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.dragon_skin
    af.level = level
    af.duration = level
    af.location = Apply.Ac
    af.modifier = -(2 * level)
    affect_to_char(victim, af)
    act("\$n's skin is now hard as rock.", victim, null, null, TO_ROOM)
    send_to_char("Your skin is now hard as rock.\n", victim)
}


fun spell_mind_light(level: Int, ch: CHAR_DATA) {
    if (is_affected_room(ch.room, Skill.mind_light)) {
        send_to_char("This room has already had booster of mana.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_CONST
    af.type = Skill.mind_light
    af.level = level
    af.duration = level / 30
    af.location = Apply.RoomMana
    af.modifier = level
    affect_to_room(ch.room, af)

    val af2 = Affect()
    af2.type = Skill.mind_light
    af2.level = level
    af2.duration = level / 10
    affect_to_char(ch, af2)
    send_to_char("The room starts to be filled with mind light.\n", ch)
    act("The room starts to be filled with \$n's mind light.", ch, null, null, TO_ROOM)
}

fun spell_insanity(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.pcdata == null) {
        send_to_char("This spell can cast on PC's only.\n", ch)
        return
    }

    if (IS_AFFECTED(victim, AFF_BLOODTHIRST) || saves_spell(level, victim, DT.Other)) {
        return
    }

    val af = Affect()
    af.type = Skill.insanity
    af.level = level
    af.duration = level / 10
    af.bitvector = AFF_BLOODTHIRST
    affect_to_char(victim, af)
    send_to_char("You are as aggressive as a battlerager.\n", victim)
    act("\$n looks with red eyes.", victim, null, null, TO_ROOM)
}


fun spell_power_stun(level: Int, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.power_word_stun) || saves_spell(level, victim, DT.Other)) {
        return
    }

    val af = Affect()
    af.type = Skill.power_word_stun
    af.level = level
    af.duration = level / 90
    af.location = Apply.Dex
    af.modifier = -3
    af.bitvector = AFF_STUN
    affect_to_char(victim, af)
    send_to_char("You are stunned.\n", victim)
    act("{r\$n is stunned.{x", victim, null, null, TO_ROOM, Pos.Sleeping)
}


fun spell_improved_invis(level: Int, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_IMP_INVIS)) {
        return
    }

    act("\$n fades out of existence.", victim, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.improved_invis
    af.level = level
    af.duration = level / 10
    af.bitvector = AFF_IMP_INVIS
    affect_to_char(victim, af)
    send_to_char("You fade out of existence.\n", victim)
}


fun spell_improved_detection(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_DETECT_IMP_INVIS)) {
        if (victim == ch) {
            send_to_char("You can already see improved invisible.\n", ch)
        } else {
            act("\$N can already see improved invisible mobiles.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.improved_detect
    af.level = level
    af.duration = level / 3
    af.bitvector = AFF_DETECT_IMP_INVIS
    affect_to_char(victim, af)
    send_to_char("Your eyes tingle.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_severity_force(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    send_to_char("You cracked the ground towards the ${victim.name}.\n", ch)
    act("\$n cracked the ground towards you!.", ch, null, victim, TO_VICT)
    val dam = if (IS_AFFECTED(victim, AFF_FLYING)) 0 else dice(level, 12)
    damage(ch, victim, dam, Skill.severity_force, DT.Bash, true)
}

fun spell_randomizer(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.randomizer)) {
        send_to_char("Your power of randomness has been exhausted for now.\n", ch)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("This room is far too orderly for your powers to work on it.\n", ch)
        return
    }
    if (is_affected_room(ch.room, Skill.randomizer)) {
        send_to_char("This room has already been randomized.\n", ch)
        return
    }

    if (flipCoin()) {
        send_to_char("Despite your efforts, the universe resisted chaos.\n", ch)
        val af2 = Affect()
        af2.type = Skill.randomizer
        af2.level = ch.level
        af2.duration = level / 10
        affect_to_char(ch, af2)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.randomizer
    af.level = ch.level
    af.duration = level / 15
    af.bitvector = AFF_ROOM_RANDOMIZER
    affect_to_room(ch.room, af)

    val af2 = Affect()
    af2.type = Skill.randomizer
    af2.level = ch.level
    af2.duration = level / 5
    affect_to_char(ch, af2)
    send_to_char("The room was successfully randomized!\n", ch)
    send_to_char("You feel very drained from the effort.\n", ch)
    ch.hit -= Math.min(200, ch.hit / 2)
    act("The room starts to randomize exits.", ch, null, null, TO_ROOM)
}

fun spell_bless_weapon(level: Int, ch: CHAR_DATA, obj: Obj) {
    if (obj.item_type != ItemType.Weapon) {
        send_to_char("That isn't a weapon.\n", ch)
        return
    }

    if (obj.wear_loc.isOn()) {
        send_to_char("The item must be carried to be blessed.\n", ch)
        return
    }

    if (IS_WEAPON_STAT(obj, WEAPON_FLAMING)
            || IS_WEAPON_STAT(obj, WEAPON_FROST)
            || IS_WEAPON_STAT(obj, WEAPON_VAMPIRIC)
            || IS_WEAPON_STAT(obj, WEAPON_SHARP)
            || IS_WEAPON_STAT(obj, WEAPON_VORPAL)
            || IS_WEAPON_STAT(obj, WEAPON_SHOCKING)
            || IS_WEAPON_STAT(obj, WEAPON_HOLY)
            || IS_OBJ_STAT(obj, ITEM_BLESS)
            || IS_OBJ_STAT(obj, ITEM_BURN_PROOF)) {
        act("You can't seem to bless \$p.", ch, obj, null, TO_CHAR)
        return
    }
    if (IS_WEAPON_STAT(obj, WEAPON_HOLY)) {
        act("\$p is already blessed for holy attacks.", ch, obj, null, TO_CHAR)
        return
    }

    val af = Affect()
    af.where = TO_WEAPON
    af.type = Skill.bless_weapon
    af.level = level / 2
    af.duration = level / 8
    af.bitvector = WEAPON_HOLY
    affect_to_obj(obj, af)

    act("\$p is prepared for holy attacks.", ch, obj, null, TO_ALL)
}

fun spell_resilience(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.resilience)) {
        send_to_char("You are already resistive to draining attacks.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_RESIST
    af.type = Skill.resilience
    af.duration = level / 10
    af.level = ch.level
    af.bitvector = RES_ENERGY
    affect_to_char(ch, af)
    send_to_char("You are now resistive to draining attacks.\n", ch)
}

fun spell_super_heal(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    val bonus = 170 + level + dice(1, 20)

    victim.hit = Math.min(victim.hit + bonus, victim.max_hit)
    update_pos(victim)
    send_to_char("A warm feeling fills your body.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_master_heal(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    val bonus = 300 + level + dice(1, 40)
    victim.hit = Math.min(victim.hit + bonus, victim.max_hit)
    update_pos(victim)
    send_to_char("A warm feeling fills your body.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_group_heal(level: Int, ch: CHAR_DATA) {
    for (gch in ch.room.people) {
        if (ch.pcdata == null && gch.pcdata == null || ch.pcdata != null && gch.pcdata != null) {
            spell_heal(level, ch, gch)
            spell_refresh(level, ch, gch)
        }
    }
}


fun spell_restoring_light(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_BLIND)) {
        spell_cure_blindness(level, ch, victim)
    }
    if (IS_AFFECTED(victim, AFF_CURSE)) {
        spell_remove_curse(level, ch, victim, TARGET_CHAR)
    }
    if (IS_AFFECTED(victim, AFF_POISON)) {
        spell_cure_poison(level, ch, victim)
    }
    if (IS_AFFECTED(victim, AFF_PLAGUE)) {
        spell_cure_disease(level, ch, victim)
    }

    if (victim.hit != victim.max_hit) {
        val mana_add = Math.min(victim.max_hit - victim.hit, ch.mana)
        victim.hit = Math.min(victim.hit + mana_add, victim.max_hit)
        ch.mana -= mana_add
    }
    update_pos(victim)
    send_to_char("A warm feeling fills your body.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_lesser_golem(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.lesser_golem)) {
        send_to_char("You lack the power to create another golem right now.\n", ch)
        return
    }

    send_to_char("You attempt to create a lesser golem.\n", ch)
    act("\$n attempts to create a lesser golem.", ch, null, null, TO_ROOM)

    var i = 0
    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_LESSER_GOLEM) {
            i++
            if (i > 2) {
                send_to_char("More golems are more than you can control!\n", ch)
                return
            }
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val golem = create_mobile(get_mob_index_nn(MOB_VNUM_LESSER_GOLEM))

    golem.perm_stat.fill(Math.min(25, 15 + ch.level / 10))
    golem.perm_stat[PrimeStat.Strength] += 3
    golem.perm_stat[PrimeStat.Intelligence] -= 1
    golem.perm_stat[PrimeStat.Constitution] += 2

    val pcdata = ch.pcdata
    golem.max_hit = if (pcdata == null) URANGE(ch.max_hit, ch.max_hit, 30000) else Math.min(2 * pcdata.perm_hit + 400, 30000)
    golem.hit = golem.max_hit
    golem.max_mana = pcdata?.perm_mana ?: ch.max_mana
    golem.mana = golem.max_mana
    golem.level = ch.level

    golem.armor.fill(interpolate(golem.level, 100, -100), 0, 3)
    golem.armor[3] = interpolate(golem.level, 100, 0)
    golem.gold = 0
    golem.timer = 0
    golem.damage[DICE_NUMBER] = 3
    golem.damage[DICE_TYPE] = 10
    golem.damage[DICE_BONUS] = ch.level / 2

    char_to_room(golem, ch.room)
    send_to_char("You created a lesser golem!\n", ch)
    act("\$n creates a lesser golem!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.lesser_golem
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    golem.affected_by = SET_BIT(golem.affected_by, AFF_CHARM)
    golem.leader = ch
    golem.master = golem.leader
}


fun spell_stone_golem(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.stone_golem)) {
        send_to_char("You lack the power to create another golem right now.\n", ch)
        return
    }

    send_to_char("You attempt to create a stone golem.\n", ch)
    act("\$n attempts to create a stone golem.", ch, null, null, TO_ROOM)

    var i = 0
    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_STONE_GOLEM) {
            i++
            if (i > 2) {
                send_to_char("More golems are more than you can control!\n", ch)
                return
            }
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val golem = create_mobile(get_mob_index_nn(MOB_VNUM_STONE_GOLEM))
    golem.perm_stat.fill(Math.min(25, 15 + ch.level / 10))

    golem.perm_stat[PrimeStat.Strength] += 3
    golem.perm_stat[PrimeStat.Intelligence] -= 1
    golem.perm_stat[PrimeStat.Constitution] += 2

    val pcdata = ch.pcdata
    golem.max_hit = if (pcdata == null) URANGE(ch.max_hit, ch.max_hit, 30000) else Math.min(5 * pcdata.perm_hit + 2000, 30000)
    golem.hit = golem.max_hit
    golem.max_mana = pcdata?.perm_mana ?: ch.max_mana
    golem.mana = golem.max_mana
    golem.level = ch.level

    golem.armor.fill(interpolate(golem.level, 100, -100), 0, 3)
    golem.armor[3] = interpolate(golem.level, 100, 0)
    golem.gold = 0
    golem.timer = 0
    golem.damage[DICE_NUMBER] = 8
    golem.damage[DICE_TYPE] = 4
    golem.damage[DICE_BONUS] = ch.level / 2

    char_to_room(golem, ch.room)
    send_to_char("You created a stone golem!\n", ch)
    act("\$n creates a stone golem!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.stone_golem
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    golem.affected_by = SET_BIT(golem.affected_by, AFF_CHARM)
    golem.leader = ch
    golem.master = golem.leader
}


fun spell_iron_golem(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.iron_golem)) {
        send_to_char("You lack the power to create another golem right now.\n", ch)
        return
    }

    send_to_char("You attempt to create an iron golem.\n", ch)
    act("\$n attempts to create an iron golem.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_IRON_GOLEM) {
            send_to_char("More golems are more than you can control!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val golem = create_mobile(get_mob_index_nn(MOB_VNUM_IRON_GOLEM))
    golem.perm_stat.fill(Math.min(25, 15 + ch.level / 10))
    golem.perm_stat[PrimeStat.Strength] += 3
    golem.perm_stat[PrimeStat.Intelligence] -= 1
    golem.perm_stat[PrimeStat.Constitution] += 2

    val pcdata = ch.pcdata
    golem.max_hit = if (pcdata == null) URANGE(ch.max_hit, ch.max_hit, 30000) else Math.min(10 * pcdata.perm_hit + 1000, 30000)
    golem.hit = golem.max_hit
    golem.max_mana = pcdata?.perm_mana ?: ch.max_mana
    golem.mana = golem.max_mana
    golem.level = ch.level
    golem.armor.fill(interpolate(golem.level, 100, -100), 0, 3)
    golem.armor[3] = interpolate(golem.level, 100, 0)
    golem.gold = 0
    golem.timer = 0
    golem.damage[DICE_NUMBER] = 11
    golem.damage[DICE_TYPE] = 5
    golem.damage[DICE_BONUS] = ch.level / 2 + 10

    char_to_room(golem, ch.room)
    send_to_char("You created an iron golem!\n", ch)
    act("\$n creates an iron golem!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.iron_golem
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    golem.affected_by = SET_BIT(golem.affected_by, AFF_CHARM)
    golem.leader = ch
    golem.master = golem.leader
}


fun spell_adamantite_golem(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.adamantite_golem)) {
        send_to_char("You lack the power to create another golem right now.\n", ch)
        return
    }

    send_to_char("You attempt to create an Adamantite golem.\n", ch)
    act("\$n attempts to create an Adamantite golem.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_ADAMANTITE_GOLEM) {
            send_to_char("More golems are more than you can control!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val golem = create_mobile(get_mob_index_nn(MOB_VNUM_ADAMANTITE_GOLEM))
    golem.perm_stat.fill(Math.min(25, 15 + ch.level / 10))

    golem.perm_stat[PrimeStat.Strength] += 3
    golem.perm_stat[PrimeStat.Intelligence] -= 1
    golem.perm_stat[PrimeStat.Constitution] += 2

    val pcdata = ch.pcdata
    golem.max_hit = if (pcdata == null) URANGE(ch.max_hit, ch.max_hit, 30000) else Math.min(10 * pcdata.perm_hit + 4000, 30000)
    golem.hit = golem.max_hit
    golem.max_mana = pcdata?.perm_mana ?: ch.max_mana
    golem.mana = golem.max_mana
    golem.level = ch.level

    golem.armor.fill(interpolate(golem.level, 100, -100), 0, 3)
    golem.armor[3] = interpolate(golem.level, 100, 0)
    golem.gold = 0
    golem.timer = 0
    golem.damage[DICE_NUMBER] = 13
    golem.damage[DICE_TYPE] = 9
    golem.damage[DICE_BONUS] = ch.level / 2 + 10

    char_to_room(golem, ch.room)
    send_to_char("You created an Adamantite golem!\n", ch)
    act("\$n creates an Adamantite golem!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.adamantite_golem
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    golem.affected_by = SET_BIT(golem.affected_by, AFF_CHARM)
    golem.leader = ch
    golem.master = golem.leader
}


fun spell_sanctify_lands(ch: CHAR_DATA) {
    if (flipCoin()) {
        send_to_char("You failed.\n", ch)
        return
    }

    if (IS_RAFFECTED(ch.room, AFF_ROOM_CURSE)) {
        affect_strip_room(ch.room, Skill.cursed_lands)
        send_to_char("The curse of the land wears off.\n", ch)
        act("The curse of the land wears off.\n", ch, null, null, TO_ROOM)
    }
    if (IS_RAFFECTED(ch.room, AFF_ROOM_POISON)) {
        affect_strip_room(ch.room, Skill.deadly_venom)
        send_to_char("The land seems more healthy.\n", ch)
        act("The land seems more healthy.\n", ch, null, null, TO_ROOM)
    }
    if (IS_RAFFECTED(ch.room, AFF_ROOM_SLEEP)) {
        send_to_char("The land wake up from mysterious dream.\n", ch)
        act("The land wake up from mysterious dream.\n", ch, null, null, TO_ROOM)
        affect_strip_room(ch.room, Skill.mysterious_dream)
    }
    if (IS_RAFFECTED(ch.room, AFF_ROOM_PLAGUE)) {
        send_to_char("The disease of the land has been treated.\n", ch)
        act("The disease of the land has been treated.\n", ch, null, null, TO_ROOM)
        affect_strip_room(ch.room, Skill.black_death)
    }
    if (IS_RAFFECTED(ch.room, AFF_ROOM_SLOW)) {
        send_to_char("The lethargic mist dissolves.\n", ch)
        act("The lethargic mist dissolves.\n", ch, null, null, TO_ROOM)
        affect_strip_room(ch.room, Skill.lethargic_mist)
    }
}


fun spell_deadly_venom(level: Int, ch: CHAR_DATA) {
    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("This room is protected by gods.\n", ch)
        return
    }
    if (is_affected_room(ch.room, Skill.deadly_venom)) {
        send_to_char("This room has already been effected by deadly venom.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.deadly_venom
    af.level = ch.level
    af.duration = level / 15
    af.bitvector = AFF_ROOM_POISON
    affect_to_room(ch.room, af)

    send_to_char("The room starts to be filled by poison.\n", ch)
    act("The room starts to be filled by poison.\n", ch, null, null, TO_ROOM)
}

fun spell_cursed_lands(level: Int, ch: CHAR_DATA) {
    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("This room is protected by gods.\n", ch)
        return
    }
    if (is_affected_room(ch.room, Skill.cursed_lands)) {
        send_to_char("This room has already been cursed.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.cursed_lands
    af.level = ch.level
    af.duration = level / 15
    af.bitvector = AFF_ROOM_CURSE
    affect_to_room(ch.room, af)

    send_to_char("The gods has forsaken the room.\n", ch)
    act("The gos has forsaken the room.\n", ch, null, null, TO_ROOM)

}

fun spell_lethargic_mist(level: Int, ch: CHAR_DATA) {
    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("This room is protected by gods.\n", ch)
        return
    }
    if (is_affected_room(ch.room, Skill.lethargic_mist)) {
        send_to_char("This room has already been full of lethargic mist.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.lethargic_mist
    af.level = ch.level
    af.duration = level / 15
    af.bitvector = AFF_ROOM_SLOW
    affect_to_room(ch.room, af)

    send_to_char("The air in the room makes you slowing down.\n", ch)
    act("The air in the room makes you slowing down.\n", ch, null, null, TO_ROOM)

}

fun spell_black_death(level: Int, ch: CHAR_DATA) {
    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("This room is protected by gods.\n", ch)
        return
    }
    if (is_affected_room(ch.room, Skill.black_death)) {
        send_to_char("This room has already been diseased.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.black_death
    af.level = ch.level
    af.duration = level / 15
    af.bitvector = AFF_ROOM_PLAGUE
    affect_to_room(ch.room, af)

    send_to_char("The room starts to be filled by disease.\n", ch)
    act("The room starts to be filled by disease.\n", ch, null, null, TO_ROOM)
}

fun spell_mysterious_dream(level: Int, ch: CHAR_DATA) {
    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("This room is protected by gods.\n", ch)
        return
    }
    if (is_affected_room(ch.room, Skill.mysterious_dream)) {
        send_to_char("This room has already been affected by sleep gas.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.mysterious_dream
    af.level = ch.level
    af.duration = level / 15
    af.bitvector = AFF_ROOM_SLEEP
    affect_to_room(ch.room, af)

    send_to_char("The room starts to be seen good place to sleep.\n", ch)
    act("The room starts to be seen good place to you.\n", ch, null, null, TO_ROOM)
}

fun spell_polymorph(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.polymorph)) {
        send_to_char("You are already polymorphed.\n", ch)
        return
    }

    if (target_name.isEmpty()) {
        send_to_char("Usage: cast 'polymorph' <pcracename>.\n", ch)
        return
    }

    val race = Race.lookup(target_name)

    if (race == null || !race.pcRace) {
        send_to_char("That is not a valid race to polymorph.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_RACE
    af.type = Skill.polymorph
    af.level = level
    af.duration = level / 10
    af.raceModifier = race
    affect_to_char(ch, af)

    act("\$n polymorphes \$mself to \$t.", ch, race.name, null, TO_ROOM)
    act("You polymorph yourself to \$t.\n", ch, race.name, null, TO_CHAR)
}


fun spell_blade_barrier(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    act("Many sharp blades appear around \$n and crash \$N.", ch, null, victim, TO_ROOM)
    act("Many sharp blades appear around you and crash \$N.", ch, null, victim, TO_CHAR)
    act("Many sharp blades appear around \$n and crash you!", ch, null, victim, TO_VICT)

    var dam = dice(level, 7)
    if (saves_spell(level, victim, DT.Pierce)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.blade_barrier, DT.Pierce, true)

    if (ch.pcdata != null && victim != ch &&
            ch.fighting != victim && victim.fighting != ch &&
            (IS_SET(victim.affected_by, AFF_CHARM) || victim.pcdata != null)) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help someone is attacking me!")
        } else {
            do_yell(victim, "Die, ${doppel_name(ch, victim)}, you sorcerous dog!")
        }
    }

    act("The blade barriers crash \$n!", victim, null, null, TO_ROOM)
    dam = dice(level, 5)
    if (saves_spell(level, victim, DT.Pierce)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.blade_barrier, DT.Pierce, true)
    act("The blade barriers crash you!", victim, null, null, TO_CHAR)

    if (randomPercent() < 50) {
        return
    }

    act("The blade barriers crash \$n!", victim, null, null, TO_ROOM)
    dam = dice(level, 4)
    if (saves_spell(level, victim, DT.Pierce)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.blade_barrier, DT.Pierce, true)
    act("The blade barriers crash you!", victim, null, null, TO_CHAR)

    if (randomPercent() < 50) {
        return
    }

    act("The blade barriers crash \$n!", victim, null, null, TO_ROOM)
    dam = dice(level, 2)
    if (saves_spell(level, victim, DT.Pierce)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.blade_barrier, DT.Pierce, true)
    act("The blade barriers crash you!", victim, null, null, TO_CHAR)
}


fun spell_protection_negative(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.protection_negative)) {
        send_to_char("You are already immune to negative attacks.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_IMMUNE
    af.type = Skill.protection_negative
    af.duration = level / 4
    af.level = ch.level
    af.bitvector = IMM_NEGATIVE
    affect_to_char(ch, af)
    send_to_char("You are now immune to negative attacks.\n", ch)
}

fun spell_ruler_aura(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.ruler_aura)) {
        send_to_char("You are as much self confident as you can.\n", ch)
        return
    }
    val af = Affect()
    af.where = TO_IMMUNE
    af.type = Skill.ruler_aura
    af.duration = level / 4
    af.level = ch.level
    af.bitvector = IMM_CHARM
    affect_to_char(ch, af)
    send_to_char("You now feel more self confident in rulership.\n", ch)
}

fun spell_evil_spirit(level: Int, ch: CHAR_DATA) {
    if (IS_RAFFECTED(ch.room, AFF_ROOM_ESPIRIT) || is_affected_room(ch.room, Skill.evil_spirit)) {
        send_to_char("The zone is already full of evil spirit.\n", ch)
        return
    }

    if (is_affected(ch, Skill.evil_spirit)) {
        send_to_char("Your power of evil spirit is less for you, now.\n", ch)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_LAW) || IS_SET(ch.room.area.area_flag, AREA_HOMETOWN)) {
        send_to_char("Holy aura in this room prevents your powers to work on it.\n", ch)
        return
    }

    val af2 = Affect()
    af2.type = Skill.evil_spirit
    af2.level = ch.level
    af2.duration = level / 5
    affect_to_char(ch, af2)

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.evil_spirit
    af.level = ch.level
    af.duration = level / 25
    af.bitvector = AFF_ROOM_ESPIRIT

    val pArea = ch.room.area
    for (i in pArea.min_vnum until pArea.max_vnum) {
        val room = get_room_index(i) ?: continue
        affect_to_room(room, af)
        act("The zone is starts to be filled with evil spirit.", room, null, null, TO_ALL)
    }
}

fun spell_disgrace(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.disgrace) || saves_spell(level, victim, DT.Mental)) {
        send_to_char("You failed.\n", ch)
        return
    }
    val af = Affect()
    af.type = Skill.disgrace
    af.level = level
    af.duration = level
    af.location = Apply.Cha
    af.modifier = -(5 + level / 5)
    affect_to_char(victim, af)
    act("\$N feels \$M less confident!", ch, null, victim, TO_ALL)
}

fun spell_control_undead(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (victim.pcdata != null || !IS_SET(victim.act, ACT_UNDEAD)) {
        act("\$N doesn't seem to be an undead.", ch, null, victim, TO_CHAR)
        return
    }
    spell_charm_person(level, ch, victim)
}

fun spell_assist(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(ch, Skill.assist)) {
        send_to_char("This power is used too recently.\n", ch)
        return
    }

    val af = Affect()
    af.type = Skill.assist
    af.level = level
    af.duration = 1 + level / 50
    affect_to_char(ch, af)

    victim.hit += 100 + level * 5
    update_pos(victim)
    send_to_char("A warm feeling fills your body.\n", victim)
    act("\$n looks better.", victim, null, null, TO_ROOM)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_aid(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(ch, Skill.aid)) {
        send_to_char("This power is used too recently.\n", ch)
        return
    }

    val af = Affect()
    af.type = Skill.aid
    af.level = level
    af.duration = level / 50
    affect_to_char(ch, af)

    victim.hit += level * 5
    update_pos(victim)
    send_to_char("A warm feeling fills your body.\n", victim)
    act("\$n looks better.", victim, null, null, TO_ROOM)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_summon_shadow(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.summon_shadow)) {
        send_to_char("You lack the power to summon another shadow right now.\n", ch)
        return
    }

    send_to_char("You attempt to summon a shadow.\n", ch)
    act("\$n attempts to summon a shadow.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_SUM_SHADOW) {
            send_to_char("Two shadows are more than you can control!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val shadow = create_mobile(get_mob_index_nn(MOB_VNUM_SUM_SHADOW))
    shadow.perm_stat.copyFrom(ch.perm_stat)
    val pcdata = ch.pcdata
    shadow.max_hit = if (pcdata == null)
        URANGE(ch.max_hit, ch.max_hit, 30000)
    else
        URANGE(pcdata.perm_hit, ch.hit, 30000)
    shadow.hit = shadow.max_hit
    shadow.max_mana = pcdata?.perm_mana ?: ch.max_mana
    shadow.mana = shadow.max_mana
    shadow.level = ch.level

    shadow.armor.fill(interpolate(shadow.level, 100, -100), 0, 3)
    shadow.armor[3] = interpolate(shadow.level, 100, 0)
    shadow.gold = 0
    shadow.timer = 0
    shadow.damage[DICE_NUMBER] = number_range(level / 15, level / 10)
    shadow.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    shadow.damage[DICE_BONUS] = number_range(level / 8, level / 6)

    char_to_room(shadow, ch.room)
    act("A shadow conjures!", ch, null, null, TO_ALL)

    val af = Affect()
    af.type = Skill.summon_shadow
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    shadow.affected_by = SET_BIT(shadow.affected_by, AFF_CHARM)
    shadow.leader = ch
    shadow.master = shadow.leader
}

fun spell_farsight(ch: CHAR_DATA) {
    val room = check_place(ch, target_name)
    if (room == null) {
        send_to_char("You cannot see that much far.\n", ch)
        return
    }

    if (ch.room == room) {
        do_look(ch, "auto")
        return
    }
    val mount = MOUNTED(ch) != null
    val oldr = ch.room
    char_from_room(ch)
    char_to_room(ch, room)
    do_look(ch, "auto")
    char_from_room(ch)
    char_to_room(ch, oldr)
    if (mount) {
        ch.riding = true
        MOUNTED(ch)!!.riding = true
    }
}

fun spell_remove_fear(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (check_dispel(level, victim, Skill.fear)) {
        send_to_char("You feel more brave.\n", victim)
        act("\$n looks more conscious.", victim, null, null, TO_ROOM)
    } else {
        send_to_char("You failed.\n", ch)
    }
}

fun spell_desert_fist(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.room.sector_type != SECT_HILLS && ch.room.sector_type != SECT_MOUNTAIN && ch.room.sector_type != SECT_DESERT) {
        send_to_char("You don't find any sand here to create a fist.\n", ch)
        ch.wait = 0
        return
    }

    act("An existing parcel of sand rises up and forms a fist and pummels \$n.", victim, null, null, TO_ROOM)
    act("An existing parcel of sand rises up and forms a fist and pummels you.", victim, null, null, TO_CHAR)
    val dam = dice(level, 16)
    damage(ch, victim, dam, Skill.desert_fist, DT.Other, true)
    sand_effect(victim, level, dam, TARGET_CHAR)
}

fun spell_holy_aura(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.holy_aura)) {
        if (victim == ch) {
            send_to_char("You have already got a holy aura.\n", ch)
        } else {
            act("\$N has already got a holy aura.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (!victim.isGood()) {
        send_to_char("It doesn't worth to protect to with holy aura!", ch)
        return
    }

    val af = Affect()
    af.type = Skill.holy_aura
    af.level = level
    af.duration = 7 + level / 6
    af.modifier = -(20 + level / 4)
    af.location = Apply.Ac
    affect_to_char(victim, af)

    af.where = TO_RESIST
    af.bitvector = RES_NEGATIVE
    affect_to_char(ch, af)

    send_to_char("You feel ancient holy power protecting you.\n", victim)
    if (ch != victim) {
        act("\$N is protected by ancient holy power.", ch, null, victim, TO_CHAR)
    }
}

fun spell_holy_fury(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.holy_fury) || IS_AFFECTED(victim, AFF_BERSERK)) {
        if (victim == ch) {
            send_to_char("You are already in a holy fury.\n", ch)
        } else {
            act("\$N is already in a holy fury.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (is_affected(victim, Skill.calm)) {
        if (victim == ch) {
            send_to_char("Why don't you just relax for a while?\n", ch)
        } else {
            act("\$N doesn't look like \$e wants to fight anymore.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (!victim.isGood()) {
        act("\$N doesn't worth to influence with holy fury.", ch, null, victim, TO_CHAR)
        return
    }

    val af = Affect()
    af.type = Skill.holy_fury
    af.level = level
    af.duration = level / 3
    af.modifier = level / 6

    af.location = Apply.Hitroll
    affect_to_char(victim, af)

    af.location = Apply.Damroll
    affect_to_char(victim, af)

    af.modifier = 10 * (level / 12)
    af.location = Apply.Ac
    affect_to_char(victim, af)

    send_to_char("You are filled with holy fury!\n", victim)
    act("\$n gets a wild look in \$s eyes!", victim, null, null, TO_ROOM)
}

fun spell_light_arrow(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 12)
    if (saves_spell(level, victim, DT.Holy)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.light_arrow, DT.Holy, true)
}

fun spell_hydroblast(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.room.sector_type != SECT_WATER_SWIM
            && ch.room.sector_type != SECT_WATER_NO_SWIM
            && (weather.sky != SKY_RAINING || !IS_OUTSIDE(ch))) {
        send_to_char("You couldn't reach any water molecule here.\n", ch)
        ch.wait = 0
        return
    }

    act("The water molecules around \$n comes together and forms a fist.", ch, null, null, TO_ROOM)
    act("The water molecules around you comes together and forms a fist.", ch, null, null, TO_CHAR)
    val dam = dice(level, 14)
    damage(ch, victim, dam, Skill.hydroblast, DT.Bash, true)
}

fun spell_wolf_spirit(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.wolf_spirit)) {
        send_to_char("The blood in your vains flowing as fast as it can!\n", ch)
        return
    }

    /* haste */
    val af = Affect()
    af.type = Skill.wolf_spirit
    af.level = level
    af.duration = 3 + level / 30
    af.location = Apply.Dex
    af.modifier = 1 + (if (level > 40) 1 else 0) + if (level > 60) 1 else 0
    af.bitvector = AFF_HASTE
    affect_to_char(ch, af)

    /* damroll */
    val af2 = Affect()
    af2.type = Skill.wolf_spirit
    af2.level = level
    af2.duration = 3 + level / 30
    af2.location = Apply.Damroll
    af2.modifier = level / 2
    af2.bitvector = AFF_BERSERK
    affect_to_char(ch, af2)

    /* infravision */
    val af3 = Affect()
    af3.type = Skill.wolf_spirit
    af3.level = level
    af3.duration = 3 + level / 30
    af3.bitvector = AFF_INFRARED
    affect_to_char(ch, af3)

    send_to_char("The blood in your vains start to flow faster.\n", ch)
    act("The eyes of \$n turn to RED!", ch, null, null, TO_ROOM)
}

fun spell_sword_of_justice(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.align == Align.Neutral || ch.align == victim.align) {
        if (victim.pcdata == null || !IS_SET(victim.act, PLR_WANTED)) {
            send_to_char("You failed!\n", ch)
            return
        }
    }

    var dam = if (victim.pcdata != null && IS_SET(victim.act, PLR_WANTED)) dice(level, 20) else dice(level, 14)
    if (saves_spell(level, victim, DT.Mental)) {
        dam /= 2
    }

    do_yell(ch, "The Sword of Justice!")
    act("The sword of justice appears and strikes \$N!", ch, null, victim, TO_ALL)

    damage(ch, victim, dam, Skill.sword_of_justice, DT.Mental, true)
}

fun spell_guard_dogs(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.guard_dogs)) {
        send_to_char("You lack the power to summon another wolf right now.\n", ch)
        return
    }

    send_to_char("You attempt to summon a dog.\n", ch)
    act("\$n attempts to summon a dog.", ch, null, null, TO_ROOM)

    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_DOG) {
            send_to_char("Two dogs are more than you can control!\n", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val dog = create_mobile(get_mob_index_nn(MOB_VNUM_DOG))
    dog.perm_stat.copyFrom(ch.perm_stat)

    val pcdata = ch.pcdata
    dog.max_hit = if (pcdata == null)
        URANGE(ch.max_hit, ch.max_hit, 30000)
    else
        URANGE(pcdata.perm_hit, ch.hit, 30000)
    dog.hit = dog.max_hit
    dog.max_mana = pcdata?.perm_mana ?: ch.max_mana
    dog.mana = dog.max_mana
    dog.level = ch.level

    dog.armor.fill(interpolate(dog.level, 100, -100), 0, 3)
    dog.armor[3] = interpolate(dog.level, 100, 0)
    dog.gold = 0
    dog.timer = 0
    dog.damage[DICE_NUMBER] = number_range(level / 15, level / 12)
    dog.damage[DICE_TYPE] = number_range(level / 3, level / 2)
    dog.damage[DICE_BONUS] = number_range(level / 10, level / 8)

    val dog2 = create_mobile(dog.pIndexData)
    clone_mobile(dog, dog2)

    dog.affected_by = SET_BIT(dog.affected_by, AFF_CHARM)
    dog2.affected_by = SET_BIT(dog2.affected_by, AFF_CHARM)
    dog2.master = ch
    dog.master = dog2.master
    dog2.leader = ch
    dog.leader = dog2.leader

    char_to_room(dog, ch.room)
    char_to_room(dog2, ch.room)
    send_to_char("Two dogs arrive and bows before you!\n", ch)
    act("Two dogs arrive from somewhere and bows!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.guard_dogs
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)
}

fun spell_eyes_of_tiger(ch: CHAR_DATA) {
    val victim = get_char_world(ch, target_name)
    if (victim == null) {
        send_to_char("Your tiger eyes cannot see such a player.\n", ch)
        return
    }

    if (victim.pcdata == null || victim.cabal != Clan.Hunter) {
        send_to_char("Your tiger eyes sees only hunters!\n", ch)
        return
    }

    if (victim.level > ch.level + 7 || saves_spell(ch.level + 9, victim, DT.None)) {
        send_to_char("Your tiger eyes cannot see that player.\n", ch)
        return
    }

    if (ch == victim) {
        do_look(ch, "auto")
        return
    }
    val ori_room = ch.room
    char_from_room(ch)
    char_to_room(ch, victim.room)
    do_look(ch, "auto")
    char_from_room(ch)
    char_to_room(ch, ori_room)
}

fun spell_lion_shield(level: Int, ch: CHAR_DATA) {
    val shield = create_object(get_obj_index_nn(OBJ_VNUM_LION_SHIELD), level)
    shield.timer = level
    shield.level = ch.level
    shield.cost = 0
    obj_to_char(shield, ch)


    val af = Affect()
    af.where = TO_OBJECT
    af.type = Skill.lion_shield
    af.level = level
    af.duration = -1
    af.modifier = level / 8
    af.location = Apply.Hitroll
    affect_to_obj(shield, af)

    af.location = Apply.Damroll
    affect_to_obj(shield, af)

    af.where = TO_OBJECT
    af.type = Skill.lion_shield
    af.level = level
    af.duration = -1
    af.modifier = -(level * 2) / 3
    af.location = Apply.Ac
    affect_to_obj(shield, af)

    af.where = TO_OBJECT
    af.type = Skill.lion_shield
    af.level = level
    af.duration = -1
    af.modifier = Math.max(1, level / 30)
    af.location = Apply.Cha
    affect_to_obj(shield, af)

    af.where = TO_OBJECT
    af.type = Skill.lion_shield
    af.level = level
    af.duration = -1
    af.modifier = -level / 9
    af.location = Apply.SavingSpell
    affect_to_obj(shield, af)

    act("You create \$p!", ch, shield, null, TO_CHAR)
    act("\$n creates \$p!", ch, shield, null, TO_ROOM)
}

fun spell_evolve_lion(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.evolve_lion) || ch.hit > ch.max_hit) {
        send_to_char("You are already as lion as you can get.\n", ch)
        return
    }

    val pcdata = ch.pcdata ?: return

    ch.hit += pcdata.perm_hit

    val af = Affect()
    af.type = Skill.evolve_lion
    af.level = level
    af.duration = 3 + level / 30
    af.location = Apply.Hit
    af.modifier = pcdata.perm_hit
    affect_to_char(ch, af)

    af.type = Skill.evolve_lion
    af.level = level
    af.duration = 3 + level / 30
    af.location = Apply.Dex
    af.modifier = -(1 + level / 30)
    af.bitvector = AFF_SLOW
    affect_to_char(ch, af)

    af.type = Skill.evolve_lion
    af.level = level
    af.duration = 3 + level / 30
    af.location = Apply.Damroll
    af.modifier = level / 2
    af.bitvector = AFF_BERSERK
    affect_to_char(ch, af)

    af.type = Skill.evolve_lion
    af.level = level
    af.duration = 3 + level / 30
    af.location = Apply.None
    af.bitvector = AFF_LION
    affect_to_char(ch, af)

    send_to_char("You feel yourself more clumsy, but more strong.\n", ch)
    act("The skin of \$n turns to grey!", ch, null, null, TO_ROOM)
}

fun spell_prevent(level: Int, ch: CHAR_DATA) {
    if (is_affected_room(ch.room, Skill.prevent)) {
        send_to_char("This room has already prevented from revenges!\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.prevent
    af.level = level
    af.duration = level / 30
    af.bitvector = AFF_ROOM_PREVENT
    affect_to_room(ch.room, af)

    val af2 = Affect()
    af2.type = Skill.prevent
    af2.level = level
    af2.duration = level / 10
    affect_to_char(ch, af2)
    send_to_char("The room is now protected from hunter revenges!\n", ch)
    act("The room starts to be filled with \$n's prevention from Hunters", ch, null, null, TO_ROOM)
}


fun spell_enlarge(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.enlarge)) {
        if (victim == ch) {
            send_to_char("You can't enlarge more!\n", ch)
        } else {
            act("\$N is already as large as \$N can get.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.enlarge
    af.level = level
    af.duration = level / 2
    af.location = Apply.Size
    af.modifier = 1 + (if (level >= 35) 1 else 0) + if (level >= 65) 1 else 0
    af.modifier = Math.min(Size.Gargantuan - victim.size, af.modifier)
    affect_to_char(victim, af)

    send_to_char("You feel yourself getting larger and larger.\n", victim)
    act("\$n's body starts to enlarge.", victim, null, null, TO_ROOM)
}

fun spell_chromatic_orb(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 14)
    if (saves_spell(level, victim, DT.Light)) {
        dam /= 2
    }

    if (randomPercent() < 0.7 * get_skill(ch, Skill.chromatic_orb)) {
        spell_blindness(level - 10, ch, victim)
        spell_slow(level - 10, ch, victim)
    }

    damage(ch, victim, dam, Skill.chromatic_orb, DT.Light, true)
}

fun spell_suffocate(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_SUFFOCATE)) {
        act("\$N already cannot breathe.\n", ch, null, victim, TO_CHAR)
        return
    }

    if (saves_spell(level, victim, DT.Negative) || victim.pcdata == null && IS_SET(victim.act, ACT_UNDEAD)) {
        if (ch == victim) {
            send_to_char("You feel momentarily ill, but it passes.\n", ch)
        } else {
            act("\$N seems to be unaffected.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.suffocate
    af.level = level * 3 / 4
    af.duration = 3 + level / 30
    af.bitvector = AFF_SUFFOCATE
    affect_join(victim, af)

    send_to_char("You cannot breathe.\n", victim)
    act("\$n tries to breathe, but cannot.", victim, null, null, TO_ROOM)

}

fun spell_mummify(level: Int, ch: CHAR_DATA, vo: Any, target: Int) {
    if (target != TARGET_OBJ) {
        val victim = vo as CHAR_DATA
        if (victim == ch) {
            send_to_char("But you aren't dead!!\n", ch)
            return
        }
        send_to_char("But it ain't dead!!\n", ch)
        return
    }
    val obj = vo as Obj
    if (!(obj.item_type == ItemType.CorpseNpc || obj.item_type == ItemType.CorpsePc)) {
        send_to_char("You can animate only corpses!\n", ch)
        return
    }

    if (obj.level > level + 10) {
        send_to_char("The dead body is too strong for you to mummify!\n", ch)
        return
    }

    if (is_affected(ch, Skill.mummify)) {
        send_to_char("You cannot summon the strength to handle more undead bodies.\n", ch)
        return
    }

    if (count_charmed(ch) != 0) {
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_MOB)) {
        send_to_char("You can't mummify deads here.\n", ch)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_SAFE) || IS_SET(ch.room.room_flags, ROOM_PRIVATE) || IS_SET(ch.room.room_flags, ROOM_SOLITARY)) {
        send_to_char("You can't mummify here.\n", ch)
        return
    }

    val undead = create_mobile(get_mob_index_nn(MOB_VNUM_UNDEAD))
    char_to_room(undead, ch.room)
    undead.perm_stat.fill { Math.min(25, 2 * ch.perm_stat[it]) }
    undead.level = obj.level
    undead.max_hit = when {
        undead.level < 30 -> undead.level * 30
        undead.level < 60 -> undead.level * 60
        else -> undead.level * 90
    }
    undead.hit = undead.max_hit
    val pcdata = ch.pcdata
    undead.max_mana = pcdata?.perm_mana ?: ch.max_mana
    undead.mana = undead.max_mana
    undead.alignment = ch.alignment

    undead.armor.fill(interpolate(undead.level, 100, -100), 0, 3)
    undead.armor[3] = interpolate(undead.level, 50, -200)
    undead.damage[DICE_NUMBER] = number_range(undead.level / 20, undead.level / 15)
    undead.damage[DICE_TYPE] = number_range(undead.level / 6, undead.level / 3)
    undead.damage[DICE_BONUS] = number_range(undead.level / 12, undead.level / 10)
    undead.sex = ch.sex
    undead.gold = 0

    undead.act = SET_BIT(undead.act, ACT_UNDEAD)
    undead.affected_by = SET_BIT(undead.affected_by, AFF_CHARM)
    undead.master = ch
    undead.leader = ch

    undead.name = "${obj.name} body undead"
    var argument = obj.short_desc
    val buf3 = StringBuilder()
    val argb = StringBuilder()
    while (argument.isNotEmpty()) {
        argb.setLength(0)
        argument = one_argument(argument, argb)
        val arg = argb.toString()
        if (!(eq(arg, "The") || eq(arg, "undead") || eq(arg, "body") ||
                eq(arg, "corpse") || eq(arg, "of"))) {
            if (buf3.isEmpty()) {
                buf3.append(arg)
            } else {
                buf3.append(" ").append(arg)
            }
        }
    }
    undead.short_desc = "The mummified corpse of $buf3"
    undead.long_desc = "The mummifed corpse of $buf3 slowly staggers around.\n"

    for (obj2 in obj.contains) {
        obj_from_obj(obj2)
        obj_to_char(obj2, undead)
    }
    interpret(undead, "wear all", true)

    val af = Affect()
    af.type = Skill.mummify
    af.level = ch.level
    af.duration = ch.level / 10
    affect_to_char(ch, af)

    send_to_char("With mystic power, you mummify and give life to it!\n", ch)
    act("With mystic power, ${ch.name} mummifies ${ch.name} and give life to it!", ch, null, null, TO_ROOM)
    act("${obj.short_desc} looks at you and plans to make you pay for distrurbing its rest!", ch, null, null, TO_CHAR)
    extract_obj(obj)
}

fun spell_soul_bind(ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.pet != null) {
        send_to_char("Your soul is already binded to someone else.\n", ch)
        return
    }

    if (victim.pcdata != null || !(IS_AFFECTED(victim, AFF_CHARM) && victim.master == ch)) {
        send_to_char("You cannot bind that soul to you.\n", ch)
        return
    }

    victim.leader = ch
    ch.pet = victim

    act("You bind \$N's soul to yourself.", ch, null, victim, TO_CHAR)
    act("\$n binds \$N's soul to \$mself.", ch, null, victim, TO_ROOM)

}

fun spell_forcecage(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_PROTECTOR)) {
        if (victim == ch) {
            send_to_char("You have already your forcecage around you.\n", ch)
        } else {
            act("\$N has already a forcecage around \$mself.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.forcecage
    af.level = level
    af.duration = level / 6
    af.bitvector = AFF_PROTECTOR
    affect_to_char(victim, af)
    act("\$n calls in arcane powers to build a cage of power around \$mself.", victim, null, null, TO_ROOM)
    send_to_char("You call in arcane powers to build a cage of power around you.\n", victim)
}

fun spell_iron_body(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_PROTECTOR)) {
        if (victim == ch) {
            send_to_char("Your skin is already as hard as iron.\n", ch)
        } else {
            act("\$N's skin is already as hard as iron.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.iron_body
    af.level = level
    af.duration = level / 6
    af.bitvector = AFF_PROTECTOR
    affect_to_char(victim, af)
    act("\$n skin is now as hard as iron.", victim, null, null, TO_ROOM)
    send_to_char("Your skin is now as hard as iron.\n", victim)
}

fun spell_elemental_sphere(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_PROTECTOR)) {
        if (victim == ch) {
            send_to_char("An elemental sphere is already protecting you.\n", ch)
        } else {
            act("An elemental sphere is already protecting \$N.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.elemental_sphere
    af.level = level
    af.duration = level / 6
    af.bitvector = AFF_PROTECTOR
    affect_to_char(victim, af)
    act("\$n uses all elemental powers to build a sphere around \$mself.", victim, null, null, TO_ROOM)
    send_to_char("You use all elemental powers to build a sphere around you.\n", victim)
}

fun spell_aura_of_chaos(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.cabal != victim.cabal) {
        send_to_char("You may only use this spell on fellow cabal members.\n", ch)
        return
    }

    if (is_affected(victim, Skill.aura_of_chaos)) {
        if (victim == ch) {
            send_to_char("You are already protected by gods of chaos.\n", ch)
        } else {
            act("\$N is already protected by aura of chaos.", ch, null, victim, TO_CHAR)
        }
        return
    }

    val af = Affect()
    af.type = Skill.aura_of_chaos
    af.level = level
    af.duration = 24
    af.bitvector = AFF_AURA_CHAOS
    affect_to_char(victim, af)
    send_to_char("You feel the gods of chaos protect you.\n", victim)
    if (ch != victim) {
        act("An aura appears around \$N.", ch, null, victim, TO_CHAR)
    }
}
