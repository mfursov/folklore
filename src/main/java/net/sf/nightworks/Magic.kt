package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Align
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Ethos
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Liquid
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_OUTSIDE
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.IS_WEAPON_STAT
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.capitalize_nn
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.number_fuzzy
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.randomPercent

fun say_spell(ch: CHAR_DATA, sn: Skill) {
    val buf = StringBuilder()
    var pos = 0
    var length = 0
    while (pos < sn.skillName.length) {
        for (aSyl_table in syl_table) {
            val prefix = aSyl_table.old
            val len = prefix.length
            if (sn.skillName.regionMatches(pos, prefix, 0, len)) {
                buf.append(aSyl_table.new)
                length = len
                break
            }
        }
        assert(length > 0)
        pos += length
    }

    for (rch in ch.room.people) {
        if (rch != ch) {
            val skill = get_skill(rch, Skill.spell_craft) * 9 / 10
            if (skill < randomPercent()) {
                act("\$n utters the words, '${sn.skillName}'.", ch, null, rch, TO_VICT)
                check_improve(rch, Skill.spell_craft, true, 5)
            } else {
                act("\$n utters the words, '$buf'.", ch, null, rch, TO_VICT)
                check_improve(rch, Skill.spell_craft, true, 5)
            }
        }
    }
}

/**
 * Compute a saving throw.
 * Negative apply's make saving throw better.
 */
fun saves_spell(level: Int, victim: CHAR_DATA, dam_type: DT): Boolean {
    var save = 40 + (victim.level - level) * 4 - victim.saving_throw * 90 / Math.max(45, victim.level)
    if (IS_AFFECTED(victim, AFF_BERSERK)) {
        save += victim.level / 5
    }

    when (check_immune(victim, dam_type)) {
        IS_IMMUNE -> return true
        IS_RESISTANT -> save += victim.level / 5
        IS_VULNERABLE -> save -= victim.level / 5
    }

    if (victim.pcdata != null && victim.clazz.fMana) {
        save = 9 * save / 10
    }
    save = URANGE(5, save, 95)
    return randomPercent() < save
}

/* RT configuration smashed */
fun saves_dispel(dis_level: Int, spell_level_arg: Int, duration: Int): Boolean {
    var spell_level = spell_level_arg
    /* impossible to dispel permanent effects */
    if (duration == -2) {
        return true
    }
    if (duration == -1) {
        spell_level += 5
    }

    var save = 50 + (spell_level - dis_level) * 5
    save = URANGE(5, save, 95)
    return randomPercent() < save
}

/* co-routine for dispel magic and cancellation */
fun check_dispel(dis_level: Int, victim: CHAR_DATA, sn: Skill?): Boolean {
    if (is_affected(victim, sn!!)) {
        var af: Affect? = victim.affected
        while (af != null) {
            if (af.type == sn) {
                if (!saves_dispel(dis_level, af.level, af.duration)) {
                    affect_strip(victim, sn)
                    if (sn.msg_off.isNotEmpty()) {
                        send_to_char(sn.msg_off, victim)
                        send_to_char("\n", victim)
                    }
                    return true
                } else {
                    af.level--
                }
            }
            af = af.next
        }
    }
    return false
}

/**
 * for casting different rooms
 * returned value is the range
 */
fun allowed_other(ch: CHAR_DATA, sn: Skill?) = when {
    sn!!.minimum_position == Pos.Standing || sn.skill_level[ch.clazz.id] < 26 || sn == Skill.find_spell(ch, "chain lightning") -> 0
    else -> sn.skill_level[ch.clazz.id] / 10
}

/**
 * The kludgy global is for spells who want more stuff from command line.
 */
var target_name = ""
private val door = intArrayOf(0)


fun do_cast(ch: CHAR_DATA, argument: String) {
    var victim: CHAR_DATA?
    val obj: Obj?
    var vo: Any?
    val mana: Int
    var target: Int
    var cast_far = 0
    val range: Int

    /* Switched NPC's can cast spells, but others can't. */
    val pcdata = ch.pcdata
    if (pcdata == null && get_pc(ch) == null) {
        return
    }

    if (is_affected(ch, Skill.shielding)) {
        send_to_char("You reach for the true Source and feel something stopping you.\n", ch)
        return
    }

    if (is_affected(ch, Skill.garble) || is_affected(ch, Skill.deafen)) {
        send_to_char("You can't get the right intonations.\n", ch)
        return
    }

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    val target_name = one_argument(argument, arg1)
    one_argument(target_name, arg2)

    if (arg1.isEmpty()) {
        send_to_char("Cast which what where?\n", ch)
        return
    }

    if (ch.cabal === Clan.BattleRager && !IS_IMMORTAL(ch)) {
        send_to_char("You are a BattleRager, not a filthy magician!\n", ch)
        return
    }

    val sn = Skill.find_spell(ch, arg1.toString())
    if (sn == null || skill_failure_nomessage(ch, sn, 1) != 0) {
        send_to_char("You don't know any spells of that name.\n", ch)
        return
    }

    if (ch.clazz === Clazz.VAMPIRE && !IS_VAMPIRE(ch) && sn.cabal == Clan.None) {
        send_to_char("You must transform to vampire before casting!\n", ch)
        return
    }

    if (!sn.isSpell) {
        send_to_char("That's not a spell.\n", ch)
        return
    }

    if (ch.position < sn.minimum_position) {
        send_to_char("You can't concentrate enough.\n", ch)
        return
    }

    if (!cabal_ok(ch, sn)) {
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_MAGIC)) {
        send_to_char("Your spell fizzles out and fails.\n", ch)
        act("\$n's spell fizzles out and fails.", ch, null, null, TO_ROOM)
        return
    }

    mana = if (ch.level + 2 != sn.skill_level[ch.clazz.id]) {
        Math.max(sn.min_mana, 100 / (2 + ch.level - sn.skill_level[ch.clazz.id]))
    } else 50

    /*
         * Locate targets.
         */
    victim = null
    vo = null
    target = TARGET_NONE

    when (sn.target) {

        TAR_IGNORE -> if (is_affected(ch, Skill.spellbane)) {
            WAIT_STATE(ch, sn.beats)
            act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
            act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
            check_improve(ch, Skill.spellbane, true, 1)
            damage(ch, ch, 3 * ch.level, Skill.spellbane, DT.Negative, true)
            return
        }

        TAR_CHAR_OFFENSIVE -> {
            if (arg2.isEmpty()) {
                victim = ch.fighting
                if (victim == null) {
                    send_to_char("Cast the spell on whom?\n", ch)
                    return
                }
            } else {
                range = allowed_other(ch, sn)
                if (range > 0) {
                    victim = get_char_spell(ch, target_name, door, range)
                    if (victim == null) {
                        return
                    }

                    if (victim.room != ch.room && (victim.pcdata == null && IS_SET(victim.act, ACT_NOTRACK) || is_at_cabal_area(ch) || is_at_cabal_area(victim))) {
                        act("You can't cast this spell to \$N at this distance.", ch, null, victim, TO_CHAR)
                        return
                    }

                    cast_far = 1
                } else {
                    victim = get_char_room(ch, target_name)
                    if (victim == null) {
                        send_to_char("They aren't here.\n", ch)
                        return
                    }
                }
            }

            if (pcdata != null && is_safe(ch, victim)) {
                return
            }
            /*
    if ( IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim )
    {
        send_to_char( "You can't do that on your own follower.\n",
        ch );
        return;
    }
*/
            vo = victim
            target = TARGET_CHAR
            if (pcdata != null && victim != ch &&
                    ch.fighting != victim && victim.fighting != ch &&
                    (IS_SET(victim.affected_by, AFF_CHARM) || victim.pcdata != null)) {
                if (!can_see(victim, ch)) {
                    do_yell(victim, "Help someone is attacking me!")
                } else {
                    do_yell(victim, "Die, ${doppel_name(ch, victim)}, you sorcerous dog!")
                }
            }
            if (is_affected(victim, Skill.spellbane) &&
                    randomPercent() < 2 * get_skill(victim, Skill.spellbane) / 3
                    && sn != Skill.mental_knife && sn != Skill.lightning_breath) {
                WAIT_STATE(ch, sn.beats)
                if (ch == victim) {
                    act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
                    act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(ch, ch, 3 * ch.level, Skill.spellbane, DT.Negative, true)
                } else {
                    act("\$N deflects your spell!", ch, null, victim, TO_CHAR)
                    act("You deflect \$n's spell!", ch, null, victim, TO_VICT)
                    act("\$N deflects \$n's spell!", ch, null, victim, TO_NOTVICT)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(victim, ch, 3 * victim.level, Skill.spellbane, DT.Negative, true)
                    multi_hit(victim, ch, null)
                }
                return
            }
            if (ch != victim && IS_AFFECTED(victim, AFF_ABSORB) &&
                    randomPercent() < 2 * get_skill(victim, Skill.absorb) / 3
                    && sn != Skill.mental_knife && sn != Skill.lightning_breath) {
                act("Your spell fails to pass \$N's energy field!", ch, null, victim, TO_CHAR)
                act("You absorb \$n's spell!", ch, null, victim, TO_VICT)
                act("\$N absorbs \$n's spell!", ch, null, victim, TO_NOTVICT)
                check_improve(victim, Skill.absorb, true, 1)
                victim.mana += mana
                return
            }
        }

        TAR_CHAR_DEFENSIVE -> {
            if (arg2.isEmpty()) {
                victim = ch
            } else {
                victim = get_char_room(ch, target_name)
                if (victim == null) {
                    send_to_char("They aren't here.\n", ch)
                    return
                }
            }

            vo = victim
            target = TARGET_CHAR
            if (is_affected(victim, Skill.spellbane)) {
                WAIT_STATE(ch, sn.beats)
                if (ch == victim) {
                    act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
                    act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(victim, ch, 3 * victim.level, Skill.spellbane, DT.Negative, true)
                } else {
                    act("\$N deflects your spell!", ch, null, victim, TO_CHAR)
                    act("You deflect \$n's spell!", ch, null, victim, TO_VICT)
                    act("\$N deflects \$n's spell!", ch, null, victim, TO_NOTVICT)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(victim, ch, 3 * victim.level, Skill.spellbane, DT.Negative, true)
                }
                return
            }
            if (ch != victim && IS_AFFECTED(victim, AFF_ABSORB) && randomPercent() < 2 * get_skill(victim, Skill.absorb) / 3) {
                act("Your spell fails to pass \$N's energy field!", ch, null, victim, TO_CHAR)
                act("You absorb \$n's spell!", ch, null, victim, TO_VICT)
                act("\$N absorbs \$n's spell!", ch, null, victim, TO_NOTVICT)
                check_improve(victim, Skill.absorb, true, 1)
                victim.mana += mana
                return
            }
        }

        TAR_CHAR_SELF -> {
            if (arg2.isNotEmpty() && !is_name(target_name, ch.name)) {
                send_to_char("You cannot cast this spell on another.\n", ch)
                return
            }

            vo = ch
            target = TARGET_CHAR

            if (is_affected(ch, Skill.spellbane)) {
                WAIT_STATE(ch, sn.beats)
                act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
                act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
                check_improve(ch, Skill.spellbane, true, 1)
                damage(ch, ch, 3 * ch.level, Skill.spellbane, DT.Negative, true)
                return
            }
        }

        TAR_OBJ_INV -> {
            if (arg2.isEmpty()) {
                send_to_char("What should the spell be cast upon?\n", ch)
                return
            }

            obj = get_obj_carry(ch, target_name)
            if (obj == null) {
                send_to_char("You are not carrying that.\n", ch)
                return
            }

            vo = obj
            target = TARGET_OBJ
            if (is_affected(ch, Skill.spellbane)) {
                WAIT_STATE(ch, sn.beats)
                act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
                act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
                check_improve(ch, Skill.spellbane, true, 1)
                damage(ch, ch, 3 * ch.level, Skill.spellbane, DT.Negative, true)
                return
            }
        }

        TAR_OBJ_CHAR_OFF -> {
            if (arg2.isEmpty()) {
                victim = ch.fighting
                if (victim == null) {
                    send_to_char("Cast the spell on whom or what?\n", ch)
                    return
                }

                target = TARGET_CHAR
            } else {
                victim = get_char_room(ch, target_name)
                if (victim != null) {
                    target = TARGET_CHAR
                }
            }

            if (target == TARGET_CHAR) { /* check the sanity of the attack */
                if (is_safe_spell(ch, victim!!, false) && victim != ch) {
                    send_to_char("Your spell didn't work.\n", ch)
                    return
                }

                if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
                    send_to_char("You can't do that on your own follower.\n",
                            ch)
                    return
                }

                if (is_safe(ch, victim)) {
                    return
                }

                vo = victim
            } else {
                obj = get_obj_here(ch, target_name)
                if (obj != null) {
                    vo = obj
                    target = TARGET_OBJ
                } else {
                    send_to_char("You don't see that here.\n", ch)
                    return
                }
            }
        }

        TAR_OBJ_CHAR_DEF -> if (arg2.isEmpty()) {
            vo = ch
            target = TARGET_CHAR
        } else {
            victim = get_char_room(ch, target_name)
            if (victim != null) {
                vo = victim
                target = TARGET_CHAR
            } else {
                obj = get_obj_carry(ch, target_name)
                if (obj != null) {
                    vo = obj
                    target = TARGET_OBJ
                } else {
                    send_to_char("You don't see that here.\n", ch)
                    return
                }
            }
        }
        else -> {
            bug("Do_cast: bad target for sn %d.", sn.ordinal)
            return
        }
    }

    if (pcdata != null && ch.mana < mana) {
        send_to_char("You don't have enough mana.\n", ch)
        return
    }

    if (!eq(sn.skillName, "ventriloquate")) {
        say_spell(ch, sn)
    }

    WAIT_STATE(ch, sn.beats)

    if (randomPercent() > get_skill(ch, sn)) {
        send_to_char("You lost your concentration.\n", ch)
        check_improve(ch, sn, false, 1)
        ch.mana -= mana / 2
        if (cast_far != 0) {
            cast_far = 2
        }
    } else {
        var slevel = when {
            ch.clazz.fMana -> ch.level - Math.max(0, ch.level / 20)
            else -> ch.level - Math.max(5, ch.level / 10)
        }

        if (sn.cabal != Clan.None) {
            slevel = ch.level
        }

        if (ch.level > Skill.spell_craft.skill_level[ch.clazz.id]) {
            if (randomPercent() < get_skill(ch, Skill.spell_craft)) {
                slevel = ch.level
                check_improve(ch, Skill.spell_craft, true, 1)
            }
            check_improve(ch, Skill.spell_craft, false, 1)
        }

        if (ch.cabal == Clan.Shalafi &&
                ch.level > Skill.mastering_spell.skill_level[ch.clazz.id]
                && cabal_ok(ch, Skill.mastering_spell)) {
            if (randomPercent() < get_skill(ch, Skill.mastering_spell)) {
                slevel += number_range(1, 4)
                check_improve(ch, Skill.mastering_spell, true, 1)
            }
        }

        ch.mana -= mana
        val curr_int = ch.curr_stat(PrimeStat.Intelligence)
        slevel = if (curr_int > 21) {
            Math.max(1, slevel + (curr_int - 21))
        } else {
            Math.max(1, slevel)
        }

        val level = if (pcdata == null) ch.level else slevel
        if (vo is CHAR_DATA) {
            sn.spell_fun(level, ch, vo, target)
        } else {
            sn.spell_fun(level, ch, vo as Obj, target)
        }
        check_improve(ch, sn, true, 1)
    }

    if (cast_far == 1 && door[0] != -1) {
        path_to_track(ch, victim!!, door[0])
    } else if ((sn.target == TAR_CHAR_OFFENSIVE || sn.target == TAR_OBJ_CHAR_OFF && target == TARGET_CHAR) && victim != ch && victim!!.master != ch) {
        for (vch in ch.room.people) {
            if (victim == vch && victim.fighting == null) {
                if (victim.position != Pos.Sleeping) {
                    multi_hit(victim, ch, null)
                }

                break
            }
        }
    }
}

/**
 * Cast spells at targets using a magical object.
 */
fun obj_cast_spell(sn: Skill?, level: Int, ch: CHAR_DATA, victimArg: CHAR_DATA?, obj: Obj?) {
    var victim = victimArg
    var vo: Any? = null
    var target = TARGET_NONE

    if (sn == null) {
        return
    }

    if (!sn.isSpell) {
        bug("Obj_cast_spell: bad sn %d.", sn.ordinal)
        return
    }

    if (ch.pcdata == null && ch.position == Pos.Dead || ch.pcdata != null && current_time - ch.last_death_time < 10) {
        bug("Obj_cast_spell: Ch is dead! But it is ok.", sn.ordinal)
        return
    }

    if (victim != null) {
        if (victim.pcdata == null && victim.position == Pos.Dead || victim.pcdata != null && current_time - victim.last_death_time < 10) {
            bug("Obj_cast_spell: Victim is dead! But it is ok.. ", sn.ordinal)
            return
        }
    }

    when (sn.target) {

        TAR_IGNORE -> vo = null

        TAR_CHAR_OFFENSIVE -> {
            if (victim == null) {
                victim = ch.fighting
            }
            if (victim == null) {
                send_to_char("You can't do that.\n", ch)
                return
            }
            if (is_safe(ch, victim) && ch != victim) {
                send_to_char("Something isn't right...\n", ch)
                return
            }
            vo = victim
            target = TARGET_CHAR
            if (is_affected(victim, Skill.spellbane) && randomPercent() < 2 * get_skill(victim, Skill.spellbane) / 3) {
                if (ch == victim) {
                    act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
                    act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(victim, ch, 10 * level, Skill.spellbane, DT.Negative, true)
                } else {
                    act("\$N deflects your spell!", ch, null, victim, TO_CHAR)
                    act("You deflect \$n's spell!", ch, null, victim, TO_VICT)
                    act("\$N deflects \$n's spell!", ch, null, victim, TO_NOTVICT)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(victim, ch, 10 * victim.level, Skill.spellbane, DT.Negative, true)
                }
                return
            }

            if (ch != victim && IS_AFFECTED(victim, AFF_ABSORB) &&
                    randomPercent() < 2 * get_skill(victim, Skill.absorb) / 3
                    && sn != Skill.mental_knife && sn != Skill.lightning_breath) {
                act("Your spell fails to pass \$N's energy field!", ch, null, victim, TO_CHAR)
                act("You absorb \$n's spell!", ch, null, victim, TO_VICT)
                act("\$N absorbs \$n's spell!", ch, null, victim, TO_NOTVICT)
                check_improve(victim, Skill.absorb, true, 1)
                victim.mana += sn.min_mana
                return
            }
        }

        TAR_CHAR_DEFENSIVE, TAR_CHAR_SELF -> {
            if (victim == null) {
                victim = ch
            }
            vo = victim
            target = TARGET_CHAR
            if (is_affected(victim, Skill.spellbane)) {
                if (ch == victim) {
                    act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
                    act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(victim, ch, 10 * victim.level, Skill.spellbane, DT.Negative, true)
                } else {
                    act("\$N deflects your spell!", ch, null, victim, TO_CHAR)
                    act("You deflect \$n's spell!", ch, null, victim, TO_VICT)
                    act("\$N deflects \$n's spell!", ch, null, victim, TO_NOTVICT)
                    check_improve(victim, Skill.spellbane, true, 1)
                    damage(victim, ch, 10 * victim.level, Skill.spellbane, DT.Negative, true)
                }
                return
            }
        }

        TAR_OBJ_INV -> {
            if (obj == null) {
                send_to_char("You can't do that.\n", ch)
                return
            }
            vo = obj
            target = TARGET_OBJ
            if (is_affected(ch, Skill.spellbane)) {
                act("Your spellbane deflects the spell!", ch, null, null, TO_CHAR)
                act("\$n's spellbane deflects the spell!", ch, null, null, TO_ROOM)
                check_improve(ch, Skill.spellbane, true, 1)
                damage(ch, ch, 3 * ch.level, Skill.spellbane, DT.Negative, true)
                return
            }
        }

        TAR_OBJ_CHAR_OFF -> if (victim == null && obj == null) {
            if (ch.fighting != null) {
                victim = ch.fighting
            } else {
                send_to_char("You can't do that.\n", ch)
                return
            }

            if (is_safe_spell(ch, victim!!, false) && ch != victim) {
                send_to_char("Somehting isn't right...\n", ch)
                return
            }

            vo = victim
            target = TARGET_CHAR
        }

        TAR_OBJ_CHAR_DEF -> when {
            victim == null && obj == null -> {
                vo = ch
                target = TARGET_CHAR
            }
            victim != null -> {
                vo = victim
                target = TARGET_CHAR
            }
            else -> {
                vo = obj
                target = TARGET_OBJ
            }
        }
        else -> {
            bug("Obj_cast_spell: bad target for sn %d.", sn.ordinal)
            return
        }
    }

    target_name = ""
    if (vo is CHAR_DATA) {
        sn.spell_fun(level, ch, vo, target)
    } else {
        sn.spell_fun(level, ch, vo as Obj, target)
    }

    if ((sn.target == TAR_CHAR_OFFENSIVE || sn.target == TAR_OBJ_CHAR_OFF && target == TARGET_CHAR) && victim != ch && victim!!.master != ch) {
        for (vch in ch.room.people) {
            if (victim == vch && victim.fighting == null) {
                multi_hit(victim, ch, null)
                break
            }
        }
    }
}

/*
* Spell functions.
*/

fun spell_acid_blast(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 18)
    if (saves_spell(level, victim, DT.Acid)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.acid_blast, DT.Acid, true)
}


fun spell_armor(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.armor)) {
        if (victim == ch) {
            send_to_char("You are already armored.\n", ch)
        } else {
            act("\$N is already armored.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.armor
    af.level = level
    af.duration = 7 + level / 6
    af.modifier = -1 * Math.max(20, 10 + level / 4) /* af.modifier  = -20;*/
    af.location = Apply.Ac
    affect_to_char(victim, af)
    send_to_char("You feel someone protecting you.\n", victim)
    if (ch != victim) {
        act("\$N is protected by your magic.", ch, null, victim, TO_CHAR)
    }
}


fun spell_bless(level: Int, ch: CHAR_DATA, vo: Any, target: Int) {
    /* deal with the object case first */
    if (target == TARGET_OBJ) {
        val obj = vo as Obj
        if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
            act("\$p is already blessed.", ch, obj, null, TO_CHAR)
            return
        }

        if (IS_OBJ_STAT(obj, ITEM_EVIL)) {
            val paf = affect_find(obj.affected, Skill.curse)
            if (!saves_dispel(level, paf?.level ?: obj.level, 0)) {
                if (paf != null) {
                    affect_remove_obj(obj, paf)
                }
                act("\$p glows a pale blue.", ch, obj, null, TO_ALL)
                obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_EVIL)
                return
            } else {
                act("The evil of \$p is too powerful for you to overcome.", ch, obj, null, TO_CHAR)
                return
            }
        }
        val af = Affect()
        af.where = TO_OBJECT
        af.type = Skill.bless
        af.level = level
        af.duration = 6 + level / 2
        af.location = Apply.Saves
        af.modifier = -1
        af.bitvector = ITEM_BLESS
        affect_to_obj(obj, af)

        act("\$p glows with a holy aura.", ch, obj, null, TO_ALL)
        return
    }

    /* character target */
    val victim = vo as CHAR_DATA


    if (victim.position == Pos.Fighting || is_affected(victim, Skill.bless)) {
        if (victim == ch) {
            send_to_char("You are already blessed.\n", ch)
        } else {
            act("\$N already has divine favor.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.bless
    af.level = level
    af.duration = 6 + level / 2
    af.location = Apply.Hitroll
    af.modifier = level / 8
    affect_to_char(victim, af)

    af.location = Apply.SavingSpell
    af.modifier = 0 - level / 8
    affect_to_char(victim, af)
    send_to_char("You feel righteous.\n", victim)
    if (ch != victim) {
        act("You grant \$N the favor of your god.", ch, null, victim, TO_CHAR)
    }
}


fun spell_blindness(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_BLIND) || saves_spell(level, victim, DT.Other)) {
        send_to_char("You failed.\n", ch)
        return
    }
    val af = Affect()
    af.type = Skill.blindness
    af.level = level
    af.location = Apply.Hitroll
    af.modifier = -4
    af.duration = 3 + level / 15
    af.bitvector = AFF_BLIND
    affect_to_char(victim, af)
    send_to_char("You are blinded!\n", victim)
    act("\$n appears to be blinded.", victim, null, null, TO_ROOM)
}


fun spell_burning_hands(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 2) + 7
    if (saves_spell(level, victim, DT.Fire)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.burning_hands, DT.Fire, true)
}


fun spell_call_lightning(level: Int, ch: CHAR_DATA) {
    if (!IS_OUTSIDE(ch)) {
        send_to_char("You must be out of doors.\n", ch)
        return
    }

    if (weather.sky < SKY_RAINING) {
        send_to_char("You need bad weather.\n", ch)
        return
    }

    var dam = dice(level, 14)
    send_to_char("Gods' lightning strikes your foes!\n", ch)
    act("\$n calls lightning to strike \$s foes!", ch, null, null, TO_ROOM)

    for (vch in Index.CHARS) {
        if (vch == ch) {
            continue
        }
        if (vch.room != ch.room) {
            if (vch.room.area == ch.room.area && IS_OUTSIDE(vch) && IS_AWAKE(vch)) {
                send_to_char("Lightning flashes in the sky.\n", vch)
            }
            continue
        }
        if (!is_same_group(ch, vch)) {
            if (is_safe(ch, vch)) {
                continue
            }
        }

        if (IS_AFFECTED(vch, AFF_GROUNDING)) {
            send_to_char("The electricity fizzles at your foes.\n", vch)
            act("A lightning bolt fizzles at \$N's foes.\n", ch, null, vch, TO_ROOM)
            continue
        }

        if (saves_spell(level, vch, DT.Lightning)) {
            dam /= 2
        }
        damage(ch, vch, dam, Skill.call_lightning, DT.Lightning, true)
        continue
    }
}

/* RT calm spell stops all fighting in the room */

fun spell_calm(level: Int, ch: CHAR_DATA) {
    var mlevel = 0
    var count = 0
    var high_level = 0

    /* get sum of all mobile levels in the room */
    for (vch in ch.room.people) {
        if (vch.position == Pos.Fighting) {
            count++
            mlevel += when {
                vch.pcdata == null -> vch.level
                else -> vch.level / 2
            }
            high_level = Math.max(high_level, vch.level)
        }
    }

    /* compute chance of stopping combat */
    val chance = 4 * level - high_level + 2 * count

    if (IS_IMMORTAL(ch)) { /* always works */
        mlevel = 0
    }

    if (number_range(0, chance) >= mlevel) { /* hard to stop large fights */
        for (vch in ch.room.people) {
            if (vch.pcdata == null && (IS_SET(vch.imm_flags, IMM_MAGIC) || IS_SET(vch.act, ACT_UNDEAD))) {
                return
            }

            if (IS_AFFECTED(vch, AFF_CALM) || IS_AFFECTED(vch, AFF_BERSERK) || is_affected(vch, Skill.frenzy)) {
                return
            }

            send_to_char("A wave of calm passes over you.\n", vch)

            if (vch.fighting != null || vch.position == Pos.Fighting) {
                stop_fighting(vch, false)
            }

            val af = Affect()
            af.type = Skill.calm
            af.level = level
            af.duration = level / 4
            af.location = Apply.Hitroll
            if (vch.pcdata != null) {
                af.modifier = -5
            } else {
                af.modifier = -2
            }
            af.bitvector = AFF_CALM
            affect_to_char(vch, af)

            af.location = Apply.Damroll
            affect_to_char(vch, af)
        }
    }
}

fun spell_cancellation(levelArg: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var level = levelArg
    var found = false

    level += 2

    if (ch.pcdata != null && victim.pcdata == null &&
            !(IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) || ch.pcdata == null && victim.pcdata != null) {
        send_to_char("You failed, try dispel magic.\n", ch)
        return
    }

    if (!is_same_group(ch, victim) && ch != victim
            && (victim.pcdata == null || IS_SET(victim.act, PLR_NOCANCEL))) {
        act("You cannot cast this spell to \$N.", ch, null, victim, TO_CHAR)
        return
    }

    /* unlike dispel magic, the victim gets NO save */

    /* begin running through the spells */

    if (check_dispel(level, victim, Skill.armor)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.enhanced_armor)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.bless)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.blindness)) {
        found = true
        act("\$n is no longer blinded.", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.calm)) {
        found = true
        act("\$n no longer looks so peaceful...", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.lookup("change sex"))) {
        found = true
        act("\$n looks more like \$mself again.", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.charm_person)) {
        found = true
        act("\$n regains \$s free will.", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.chill_touch)) {
        found = true
        act("\$n looks warmer.", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.curse)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_evil)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_good)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_hidden)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_invis)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_hidden)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_magic)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.faerie_fire)) {
        act("\$n's outline fades.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.fly)) {
        act("\$n falls to the ground!", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.frenzy)) {
        act("\$n no longer looks so wild.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.giant_strength)) {
        act("\$n no longer looks so mighty.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.haste)) {
        act("\$n is no longer moving so quickly.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.infravision)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.invisibility)) {
        act("\$n fades into existance.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.mass_invis)) {
        act("\$n fades into existance.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.pass_door)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.protection_evil)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.protection_good)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.sanctuary)) {
        act("The white aura around \$n's body vanishes.",
                victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.shield)) {
        act("The shield protecting \$n vanishes.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.sleep)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.slow)) {
        act("\$n is no longer moving so slowly.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.stone_skin)) {
        act("\$n's skin regains its normal texture.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.weaken)) {
        act("\$n looks stronger.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.shielding)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.web)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.fear)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.protection_heat)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.protection_cold)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.magic_resistance)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.terangreal)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.power_word_stun)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.corruption)) {
        act("\$n looks healthier.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.web)) {
        act("The webs around \$n dissolve.", victim, null, null, TO_ROOM)
        found = true
    }

    if (found) {
        send_to_char("Ok.\n", ch)
    } else {
        send_to_char("Spell failed.\n", ch)
    }
}

fun spell_cause_light(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    damage(ch, victim, dice(1, 8) + level / 3, Skill.cause_light, DT.Harm, true)
}


fun spell_cause_critical(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    damage(ch, victim, dice(3, 8) + level - 6, Skill.cause_critical, DT.Harm, true)
}


fun spell_cause_serious(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    damage(ch, victim, dice(2, 8) + level / 2, Skill.cause_critical, DT.Harm, true)
}

fun spell_chain_lightning(levelArg: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var level = levelArg
    var last_vict: CHAR_DATA

    /* first strike */

    act("A lightning bolt leaps from \$n's hand and arcs to \$N.", ch, null, victim, TO_ROOM)
    act("A lightning bolt leaps from your hand and arcs to \$N.", ch, null, victim, TO_CHAR)
    act("A lightning bolt leaps from \$n's hand and hits you!", ch, null, victim, TO_VICT)

    var dam = dice(level, 6)

    if (IS_AFFECTED(victim, AFF_GROUNDING)) {
        send_to_char("The electricity fizzles at your foes.\n", victim)
        act("A lightning bolt fizzles at \$N's foes.\n", ch, null, victim, TO_ROOM)
    } else {
        if (saves_spell(level, victim, DT.Lightning)) {
            dam /= 3
        }
        damage(ch, victim, dam, Skill.chain_lightning, DT.Lightning, true)
    }

    if (ch.pcdata != null && victim != ch && ch.fighting != victim && victim.fighting != ch && (IS_SET(victim.affected_by, AFF_CHARM) || victim.pcdata != null)) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help someone is attacking me!")
        } else {
            do_yell(victim, "Die, ${doppel_name(ch, victim)}, you sorcerous dog!")
        }
    }


    last_vict = victim
    level -= 4   /* decrement damage */

    /* new targets */
    while (level > 0) {
        var found = false
        for (tmp_vict in ch.room.people) {
            if (!is_safe_spell(ch, tmp_vict, true) && tmp_vict != last_vict) {
                found = true
                last_vict = tmp_vict
                if (is_safe(ch, tmp_vict)) {
                    act("The bolt passes around \$n's body.", ch, null, null, TO_ROOM)
                    act("The bolt passes around your body.", ch, null, null, TO_CHAR)
                } else {
                    act("The bolt arcs to \$n!", tmp_vict, null, null, TO_ROOM)
                    act("The bolt hits you!", tmp_vict, null, null, TO_CHAR)
                    dam = dice(level, 6)

                    if (ch.pcdata != null && tmp_vict != ch &&
                            ch.fighting != tmp_vict && tmp_vict.fighting != ch &&
                            (IS_SET(tmp_vict.affected_by, AFF_CHARM) || tmp_vict.pcdata != null)) {
                        if (!can_see(tmp_vict, ch)) {
                            do_yell(tmp_vict, "Help someone is attacking me!")
                        } else {
                            do_yell(tmp_vict, "Die, ${doppel_name(ch, victim)}, you sorcerous dog!")
                        }
                    }

                    if (IS_AFFECTED(tmp_vict, AFF_GROUNDING)) {
                        send_to_char("The electricity fizzles at your foes.\n", tmp_vict)
                        act("A lightning bolt fizzles at \$N's foes.\n", ch, null, tmp_vict, TO_ROOM)
                    } else {
                        if (saves_spell(level, tmp_vict, DT.Lightning)) {
                            dam /= 3
                        }
                        damage(ch, tmp_vict, dam, Skill.chain_lightning, DT.Lightning, true)
                    }
                    level -= 4  /* decrement damage */
                }
            }
        }   /* end target searching loop */

        if (!found) { /* no target found, hit the caster */
            if (last_vict == ch) { /* no double hits */
                act("The bolt seems to have fizzled out.", ch, null, null, TO_ROOM)
                act("The bolt grounds out through your body.", ch, null, null, TO_CHAR)
                return
            }

            last_vict = ch
            act("The bolt arcs to \$n...whoops!", ch, null, null, TO_ROOM)
            send_to_char("You are struck by your own lightning!\n", ch)
            dam = dice(level, 6)

            if (IS_AFFECTED(ch, AFF_GROUNDING)) {
                send_to_char("The electricity fizzles at your foes.\n", ch)
                act("A lightning bolt fizzles at \$N's foes.\n", ch, null, ch, TO_ROOM)
            } else {
                if (saves_spell(level, ch, DT.Lightning)) {
                    dam /= 3
                }
                damage(ch, ch, dam, Skill.chain_lightning, DT.Lightning, true)
            }
            level -= 4  /* decrement damage */
        }
        /* now go back and find more targets */
    }
}


fun spell_healing_light(level: Int, ch: CHAR_DATA) {
    if (is_affected_room(ch.room, Skill.healing_light)) {
        send_to_char("This room has already been healed by light.\n", ch)
        return
    }
    val af = Affect()
    af.where = TO_ROOM_CONST
    af.type = Skill.healing_light
    af.level = level
    af.duration = level / 25
    af.location = Apply.RoomHeal
    af.modifier = level
    affect_to_room(ch.room, af)

    val af2 = Affect()
    af2.type = Skill.healing_light
    af2.level = level
    af2.duration = level / 10
    affect_to_char(ch, af2)
    send_to_char("The room starts to be filled with healing light.\n", ch)
    act("The room starts to be filled with \$n's healing light.", ch, null, null, TO_ROOM)
}


fun spell_charm_person(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_safe(ch, victim)) {
        return
    }

    if (count_charmed(ch) != 0) {
        return
    }

    if (victim == ch) {
        send_to_char("You like yourself even better!\n", ch)
        return
    }

    if (IS_AFFECTED(victim, AFF_CHARM)
            || IS_AFFECTED(ch, AFF_CHARM)
            || ch.sex.isNotFemale() && level < victim.level
            || ch.sex.isFemale() && level < victim.level - 2
            || IS_SET(victim.imm_flags, IMM_CHARM)
            || saves_spell(level, victim, DT.Charm)
            || victim.pcdata == null && victim.pIndexData.pShop != null) {
        return
    }

    if (victim.master != null) {
        stop_follower(victim)
    }
    add_follower(victim, ch)
    victim.leader = ch

    val af = Affect()
    af.type = Skill.charm_person
    af.level = level
    af.duration = number_fuzzy(level / 5)
    af.bitvector = AFF_CHARM
    affect_to_char(victim, af)
    act("Isn't \$n just so nice?", ch, null, victim, TO_VICT)
    act("\$N looks at you with adoring eyes.", ch, null, victim, TO_CHAR)

    if (victim.pcdata == null && ch.pcdata != null) {
        if (randomPercent() < (4 + (victim.level - ch.level)) * 10) {
            add_mind(victim, ch.name)
        } else if (victim.in_mind.isEmpty()) {
            victim.in_mind = victim.room.vnum.toString()
        }
    }
}


fun spell_chill_touch(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = number_range(1, level)
    if (!saves_spell(level, victim, DT.Cold)) {
        act("\$n turns blue and shivers.", victim, null, null, TO_ROOM)
        val af = Affect()
        af.type = Skill.chill_touch
        af.level = level
        af.duration = 6
        af.location = Apply.Str
        af.modifier = -1
        affect_join(victim, af)
    } else {
        dam /= 2
    }

    damage(ch, victim, dam, Skill.chill_touch, DT.Cold, true)
}


fun spell_colour_spray(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 3) + 13
    if (saves_spell(level, victim, DT.Light)) {
        dam /= 2
    } else {
        spell_blindness(level / 2, ch, victim)
    }

    damage(ch, victim, dam, Skill.colour_spray, DT.Light, true)

}


fun spell_continual_light(ch: CHAR_DATA) {
    if (target_name.isNotEmpty()) { /* do a glow on some object */
        val light = get_obj_carry(ch, target_name)

        if (light == null) {
            send_to_char("You don't see that here.\n", ch)
            return
        }

        if (IS_OBJ_STAT(light, ITEM_GLOW)) {
            act("\$p is already glowing.", ch, light, null, TO_CHAR)
            return
        }

        light.extra_flags = SET_BIT(light.extra_flags, ITEM_GLOW)
        act("\$p glows with a white light.", ch, light, null, TO_ALL)
        return
    }

    val light = create_object(get_obj_index_nn(OBJ_VNUM_LIGHT_BALL), 0)
    obj_to_room(light, ch.room)
    act("\$n twiddles \$s thumbs and \$p appears.", ch, light, null, TO_ROOM)
    act("You twiddle your thumbs and \$p appears.", ch, light, null, TO_CHAR)
}


fun spell_control_weather(level: Int, ch: CHAR_DATA) {
    when {
        eq(target_name, "better") -> weather.change = weather.change + dice(level / 3, 4)
        eq(target_name, "worse") -> weather.change = weather.change - dice(level / 3, 4)
        else -> {
            send_to_char("Do you want it to get better or worse?\n", ch)
            return
        }
    }
    send_to_char("Ok.\n", ch)
}


fun spell_create_food(level: Int, ch: CHAR_DATA) {
    val mushroom = create_object(get_obj_index_nn(OBJ_VNUM_MUSHROOM), 0)
    mushroom.value[0] = (level / 2).toLong()
    mushroom.value[1] = level.toLong()
    obj_to_room(mushroom, ch.room)
    act("\$p suddenly appears.", ch, mushroom, null, TO_ROOM)
    act("\$p suddenly appears.", ch, mushroom, null, TO_CHAR)
}

fun spell_create_rose(ch: CHAR_DATA) {
    val rose = create_object(get_obj_index_nn(OBJ_VNUM_ROSE), 0)
    act("\$n has created a beautiful red rose.", ch, rose, null, TO_ROOM)
    send_to_char("You create a beautiful red rose.\n", ch)
    obj_to_char(rose, ch)
}

fun spell_create_spring(level: Int, ch: CHAR_DATA) {
    val spring = create_object(get_obj_index_nn(OBJ_VNUM_SPRING), 0)
    spring.timer = level
    obj_to_room(spring, ch.room)
    act("\$p flows from the ground.", ch, spring, null, TO_ROOM)
    act("\$p flows from the ground.", ch, spring, null, TO_CHAR)
}


fun spell_create_water(level: Int, ch: CHAR_DATA, obj: Obj) {
    if (obj.item_type != ItemType.DrinkContainer) {
        send_to_char("It is unable to hold water.\n", ch)
        return
    }

    if (Liquid.of(obj.value[2]) === Liquid.Water && obj.value[1] != 0L) {
        send_to_char("It contains some other liquid.\n", ch)
        return
    }

    val a = level * if (weather.sky >= SKY_RAINING) 4 else 2
    val water = Math.min(a.toLong(), obj.value[0] - obj.value[1]).toInt()

    if (water > 0) {
        obj.value[2] = Liquid.Water.ordinal.toLong()
        obj.value[1] += water.toLong()
        if (!is_name("water", obj.name)) {
            obj.name = "${obj.name} water"
        }
        act("\$p is filled.", ch, obj, null, TO_CHAR)
    }

}


fun spell_cure_blindness(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (!is_affected(victim, Skill.blindness)) {
        if (victim == ch) {
            send_to_char("You aren't blind.\n", ch)
        } else {
            act("\$N doesn't appear to be blinded.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (check_dispel(level, victim, Skill.blindness)) {
        send_to_char("Your vision returns!\n", victim)
        act("\$n is no longer blinded.", victim, null, null, TO_ROOM)
    } else {
        send_to_char("Spell failed.\n", ch)
    }
}


fun spell_cure_critical(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    val heal = dice(3, 8) + level / 2

    victim.hit = Math.min(victim.hit + heal, victim.max_hit)
    update_pos(victim)
    send_to_char("You feel better!\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

/* RT added to cure plague */
fun spell_cure_disease(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (!is_affected(victim, Skill.plague)) {
        if (victim == ch) {
            send_to_char("You aren't ill.\n", ch)
        } else {
            act("\$N doesn't appear to be diseased.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (check_dispel(level, victim, Skill.plague)) {
        send_to_char("Your sores vanish.\n", victim)
        act("\$n looks relieved as \$s sores vanish.", victim, null, null, TO_ROOM)
    } else {
        send_to_char("Spell failed.\n", ch)
    }
}


fun spell_cure_light(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    val heal = dice(1, 8) + level / 4 + 5

    victim.hit = Math.min(victim.hit + heal, victim.max_hit)
    update_pos(victim)
    send_to_char("You feel better!\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_cure_poison(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (!is_affected(victim, Skill.poison)) {
        if (victim == ch) {
            send_to_char("You aren't poisoned.\n", ch)
        } else {
            act("\$N doesn't appear to be poisoned.", ch, null, victim, TO_CHAR)
        }
        return
    }

    if (check_dispel(level, victim, Skill.poison)) {
        send_to_char("A warm feeling runs through your body.\n", victim)
        act("\$n looks much better.", victim, null, null, TO_ROOM)
    } else {
        send_to_char("Spell failed.\n", ch)
    }
}

fun spell_cure_serious(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    val heal = dice(2, 8) + level / 3 + 10

    victim.hit = Math.min(victim.hit + heal, victim.max_hit)
    update_pos(victim)
    send_to_char("You feel better!\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_curse(level: Int, ch: CHAR_DATA, vo: Any, target: Int) {
    /* deal with the object case first */
    if (target == TARGET_OBJ) {
        val obj = vo as Obj

        if (obj.wear_loc.isOn()) {
            act("You must remove \$p first.", ch, obj, null, TO_CHAR)
            return
        }

        if (IS_OBJ_STAT(obj, ITEM_EVIL)) {
            act("\$p is already filled with evil.", ch, obj, null, TO_CHAR)
            return
        }

        if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
            val paf = affect_find(obj.affected, Skill.bless)
            if (!saves_dispel(level, paf?.level ?: obj.level, 0)) {
                if (paf != null) {
                    affect_remove_obj(obj, paf)
                }
                act("\$p glows with a red aura.", ch, obj, null, TO_ALL)
                obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_BLESS)
            } else {
                act("The holy aura of \$p is too powerful for you to overcome.", ch, obj, null, TO_CHAR)
            }
            return
        }
        val af = Affect()
        af.where = TO_OBJECT
        af.type = Skill.curse
        af.level = level
        af.duration = 8 + level / 5
        af.location = Apply.Saves
        af.modifier = +1
        af.bitvector = ITEM_EVIL
        affect_to_obj(obj, af)

        act("\$p glows with a malevolent aura.", ch, obj, null, TO_ALL)
        return
    }

    /* character curses */
    val victim = vo as CHAR_DATA

    if (IS_AFFECTED(victim, AFF_CURSE) || saves_spell(level, victim, DT.Negative)) {
        return
    }
    val af = Affect()
    af.type = Skill.curse
    af.level = level
    af.duration = 8 + level / 10
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

/* RT replacement demonfire spell */
fun spell_demonfire(level: Int, ch: CHAR_DATA, vo: CHAR_DATA) {
    var victim = vo
    if (ch.pcdata != null && !ch.isEvil()) {
        victim = ch
        send_to_char("The demons turn upon you!\n", ch)
    }

    if (victim != ch) {
        act("\$n calls forth the demons of Hell upon \$N!", ch, null, victim, TO_ROOM)
        act("\$n has assailed you with the demons of Hell!", ch, null, victim, TO_VICT)
        send_to_char("You conjure forth the demons of hell!\n", ch)
    }
    var dam = dice(level, 10)
    if (saves_spell(level, victim, DT.Negative)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.demonfire, DT.Negative, true)
    spell_curse(3 * level / 4, ch, victim, TARGET_CHAR)
}

/* added by chronos */

fun spell_bluefire(level: Int, ch: CHAR_DATA, vo: CHAR_DATA) {
    var victim = vo
    if (ch.pcdata != null && !ch.isNeutralA()) {
        victim = ch
        send_to_char("Your blue fire turn upon you!\n", ch)
    }

    if (victim != ch) {
        act("\$n calls forth the blue fire of earth \$N!", ch, null, victim, TO_ROOM)
        act("\$n has assailed you with the neutrals of earth!", ch, null, victim, TO_VICT)
        send_to_char("You conjure forth the blue fire!\n", ch)
    }

    var dam = dice(level, 10)
    if (saves_spell(level, victim, DT.Fire)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.bluefire, DT.Fire, true)
}


fun spell_detect_evil(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_DETECT_EVIL)) {
        if (victim == ch) {
            send_to_char("You can already sense evil.\n", ch)
        } else {
            act("\$N can already detect evil.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.detect_evil
    af.level = level
    af.duration = 5 + level / 3
    af.bitvector = AFF_DETECT_EVIL
    affect_to_char(victim, af)
    send_to_char("Your eyes tingle.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_detect_good(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_DETECT_GOOD)) {
        if (victim == ch) {
            send_to_char("You can already sense good.\n", ch)
        } else {
            act("\$N can already detect good.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.detect_good
    af.level = level
    af.duration = 5 + level / 3
    af.bitvector = AFF_DETECT_GOOD
    affect_to_char(victim, af)
    send_to_char("Your eyes tingle.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_detect_hidden(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_DETECT_HIDDEN)) {
        if (victim == ch) {
            send_to_char("You are already as alert as you can be. \n", ch)
        } else {
            act("\$N can already sense hidden lifeforms.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.detect_hidden
    af.level = level
    af.duration = 5 + level / 3
    af.bitvector = AFF_DETECT_HIDDEN
    affect_to_char(victim, af)
    send_to_char("Your awareness improves.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_detect_invis(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_DETECT_INVIS)) {
        if (victim == ch) {
            send_to_char("You can already see invisible.\n", ch)
        } else {
            act("\$N can already see invisible things.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.detect_invis
    af.level = level
    af.duration = 5 + level / 3
    af.bitvector = AFF_DETECT_INVIS
    affect_to_char(victim, af)
    send_to_char("Your eyes tingle.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_detect_magic(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_DETECT_MAGIC)) {
        if (victim == ch) {
            send_to_char("You can already sense magical auras.\n", ch)
        } else {
            act("\$N can already detect magic.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.detect_magic
    af.level = level
    af.duration = 5 + level / 3
    af.bitvector = AFF_DETECT_MAGIC
    affect_to_char(victim, af)
    send_to_char("Your eyes tingle.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_detect_poison(ch: CHAR_DATA, obj: Obj) {
    if (obj.item_type == ItemType.DrinkContainer || obj.item_type == ItemType.Food) {
        if (obj.value[3] != 0L) {
            send_to_char("You smell poisonous fumes.\n", ch)
        } else {
            send_to_char("It looks delicious.\n", ch)
        }
    } else {
        send_to_char("It doesn't look poisoned.\n", ch)
    }

}


fun spell_dispel_evil(level: Int, ch: CHAR_DATA, vo: CHAR_DATA) {
    var victim = vo
    if (ch.pcdata != null && ch.isEvil()) {
        victim = ch
    }

    if (victim.isGood()) {
        act("Gods protects \$N.", ch, null, victim, TO_ROOM)
        return
    }

    if (victim.isNeutralA()) {
        act("\$N does not seem to be affected.", ch, null, victim, TO_CHAR)
        return
    }

    var dam = if (victim.hit > ch.level * 4) dice(level, 4) else Math.max(victim.hit, dice(level, 4))
    if (saves_spell(level, victim, DT.Holy)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.dispel_evil, DT.Holy, true)
}


fun spell_dispel_good(level: Int, ch: CHAR_DATA, vo: CHAR_DATA) {
    var victim = vo
    if (ch.pcdata != null && ch.isGood()) {
        victim = ch
    }

    if (victim.isEvil()) {
        act("\$N is protected by \$S evil.", ch, null, victim, TO_ROOM)
        return
    }

    if (victim.isNeutralA()) {
        act("\$N does not seem to be affected.", ch, null, victim, TO_CHAR)
        return
    }

    var dam = if (victim.hit > ch.level * 4) dice(level, 4) else Math.max(victim.hit, dice(level, 4))
    if (saves_spell(level, victim, DT.Negative)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.dispel_good, DT.Negative, true)
}

/* modified for enhanced use */
fun spell_dispel_magic(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var found = false

    if (saves_spell(level, victim, DT.Other)) {
        send_to_char("You feel a brief tingling sensation.\n", victim)
        send_to_char("You failed.\n", ch)
        return
    }

    /* begin running through the spells */
    if (check_dispel(level, victim, Skill.armor)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.enhanced_armor)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.bless)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.blindness)) {
        found = true
        act("\$n is no longer blinded.", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.calm)) {
        found = true
        act("\$n no longer looks so peaceful...", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.charm_person)) {
        found = true
        act("\$n regains \$s free will.", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.chill_touch)) {
        found = true
        act("\$n looks warmer.", victim, null, null, TO_ROOM)
    }

    if (check_dispel(level, victim, Skill.curse)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_evil)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_good)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_hidden)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_invis)) {
        found = true
    }

    //TODO???            found = true;

    if (check_dispel(level, victim, Skill.detect_hidden)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.detect_magic)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.faerie_fire)) {
        act("\$n's outline fades.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.fly)) {
        act("\$n falls to the ground!", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.frenzy)) {
        act("\$n no longer looks so wild.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.giant_strength)) {
        act("\$n no longer looks so mighty.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.haste)) {
        act("\$n is no longer moving so quickly.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.infravision)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.invisibility)) {
        act("\$n fades into existance.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.mass_invis)) {
        act("\$n fades into existance.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.pass_door)) {
        found = true
    }


    if (check_dispel(level, victim, Skill.protection_evil)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.protection_good)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.sanctuary)) {
        act("The white aura around \$n's body vanishes.", victim, null, null, TO_ROOM)
        found = true
    }

    if (IS_AFFECTED(victim, AFF_SANCTUARY) && !saves_dispel(level, victim.level, -1)
            && !is_affected(victim, Skill.sanctuary)
            && !(victim.spec_fun === spec_lookup("spec_special_guard") || victim.spec_fun === spec_lookup("spec_stalker"))) {
        victim.affected_by = REMOVE_BIT(victim.affected_by, AFF_SANCTUARY)
        act("The white aura around \$n's body vanishes.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.shield)) {
        act("The shield protecting \$n vanishes.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.sleep)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.slow)) {
        act("\$n is no longer moving so slowly.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.stone_skin)) {
        act("\$n's skin regains its normal texture.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.weaken)) {
        act("\$n looks stronger.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.shielding)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.web)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.fear)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.protection_heat)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.protection_cold)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.magic_resistance)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.terangreal)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.power_word_stun)) {
        found = true
    }

    if (check_dispel(level, victim, Skill.corruption)) {
        act("\$n looks healthier.", victim, null, null, TO_ROOM)
        found = true
    }

    if (check_dispel(level, victim, Skill.web)) {
        act("The webs around \$n dissolve.", victim, null, null, TO_ROOM)
        found = true
    }

    if (found) {
        send_to_char("Ok.\n", ch)
    } else {
        send_to_char("Spell failed.\n", ch)
    }
}

fun spell_earthquake(level: Int, ch: CHAR_DATA) {
    send_to_char("The earth trembles beneath your feet!\n", ch)
    act("\$n makes the earth tremble and shiver.", ch, null, null, TO_ROOM)

    for (vch in Index.CHARS) {
        if (vch.room != ch.room) {
            if (vch.room.area == ch.room.area) {
                send_to_char("The earth trembles and shivers.\n", vch)
            }
            continue
        }
        if (vch != ch && !is_safe_spell(ch, vch, true) && !is_same_group(ch, vch)) {
            if (is_safe(ch, vch)) {
                continue
            }
        }
        if (ch == vch) {
            continue
        }
        if (IS_AFFECTED(vch, AFF_FLYING)) {
            damage(ch, vch, 0, Skill.earthquake, DT.Bash, true)
        } else {
            damage(ch, vch, level + dice(2, 8), Skill.earthquake, DT.Bash, true)
        }
        continue
    }

    for (obj in ch.room.objects) {
        val corpse = obj.contains.firstOrNull()
        if (obj.pIndexData.vnum == OBJ_VNUM_GRAVE_STONE && corpse != null && randomPercent() < get_skill(ch, Skill.earthquake)) {
            obj_from_obj(corpse)
            corpse.extra_flags = REMOVE_BIT(corpse.extra_flags, ITEM_BURIED)
            obj_to_room(corpse, ch.room)
            extract_obj(obj)
            corpse.timer = number_range(25, 40)
            act("The earthquake reveals \$p.\n", ch, corpse, null, TO_ALL)
        }
    }

}

fun spell_enchant_armor(level: Int, ch: CHAR_DATA, obj: Obj) {
    var ac_found = false

    if (obj.item_type != ItemType.Armor) {
        send_to_char("That isn't an armor.\n", ch)
        return
    }

    if (obj.wear_loc.isOn()) {
        send_to_char("The item must be carried to be enchanted.\n", ch)
        return
    }

    /* this means they have no bonus */
    var fail = 25  /* base 25% chance of failure */

    /* find the bonuses */

    if (!obj.enchanted) {
        var paf: Affect? = obj.pIndexData.affected
        while (paf != null) {
            if (paf.location == Apply.Ac) {
                val ac_bonus = paf.modifier
                ac_found = true
                fail += 5 * (ac_bonus * ac_bonus)
            } else { /* things get a little harder */
                fail += 20
            }
            paf = paf.next
        }
    }

    run {
        var paf = obj.affected
        while (paf != null) {
            if (paf!!.location == Apply.Ac) {
                val ac_bonus = paf!!.modifier
                ac_found = true
                fail += 5 * (ac_bonus * ac_bonus)
            } else { /* things get a little harder */
                fail += 20
            }
            paf = paf!!.next
        }
    }

    /* apply other modifiers */
    fail -= level

    if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
        fail -= 15
    }
    if (IS_OBJ_STAT(obj, ITEM_GLOW)) {
        fail -= 5
    }

    fail = URANGE(5, fail, 85)

    val result = randomPercent()

    /* the moment of truth */
    if (result < fail / 5) { /* item destroyed */
        act("\$p flares blindingly... and evaporates!", ch, obj, null, TO_CHAR)
        act("\$p flares blindingly... and evaporates!", ch, obj, null, TO_ROOM)
        extract_obj(obj)
        return
    }

    if (result < fail / 3) { /* item disenchanted */
        act("\$p glows brightly, then fades...oops.", ch, obj, null, TO_CHAR)
        act("\$p glows brightly, then fades.", ch, obj, null, TO_ROOM)
        obj.enchanted = true

        /* remove all affects */
        obj.affected = null

        /* clear all flags */
        obj.extra_flags = 0
        return
    }

    if (result <= fail) { /* failed, no bad result */
        send_to_char("Nothing seemed to happen.\n", ch)
        return
    }

    /* okay, move all the old flags into new vectors if we have to */
    if (!obj.enchanted) {
        obj.enchanted = true

        var paf: Affect? = obj.pIndexData.affected
        while (paf != null) {
            val af_new = Affect()

            af_new.next = obj.affected
            obj.affected = af_new

            af_new.where = paf!!.where
            af_new.type = paf!!.type
            af_new.level = paf!!.level
            af_new.duration = paf!!.duration
            af_new.location = paf!!.location
            af_new.modifier = paf!!.modifier
            af_new.bitvector = paf!!.bitvector
            paf = paf!!.next
        }
    }

    val added: Int
    if (result <= 90 - level / 5) { /* success! */
        act("\$p shimmers with a gold aura.", ch, obj, null, TO_CHAR)
        act("\$p shimmers with a gold aura.", ch, obj, null, TO_ROOM)
        obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_MAGIC)
        added = -1
    } else { /* exceptional enchant */
        act("\$p glows a brillant gold!", ch, obj, null, TO_CHAR)
        act("\$p glows a brillant gold!", ch, obj, null, TO_ROOM)
        obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_MAGIC)
        obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_GLOW)
        added = -2
    }

    /* now add the enchantments */

    if (obj.level < LEVEL_HERO) {
        obj.level = Math.min(LEVEL_HERO - 1, obj.level + 1)
    }

    if (ac_found) {
        var paf = obj.affected
        while (paf != null) {
            if (paf!!.location == Apply.Ac) {
                paf!!.type = Skill.enchant_armor
                paf!!.modifier += added
                paf!!.level = Math.max(paf!!.level, level)
            }
            paf = paf!!.next
        }
    } else { /* add a new affect */
        val paf = Affect()
        paf.where = TO_OBJECT
        paf.type = Skill.enchant_armor
        paf.level = level
        paf.duration = -1
        paf.location = Apply.Ac
        paf.modifier = added
        paf.next = obj.affected
        obj.affected = paf
    }

}


fun spell_enchant_weapon(level: Int, ch: CHAR_DATA, obj: Obj) {
    var hit_found = false
    var dam_found = false

    if (obj.item_type != ItemType.Weapon) {
        send_to_char("That isn't a weapon.\n", ch)
        return
    }

    if (obj.wear_loc.isOn()) {
        send_to_char("The item must be carried to be enchanted.\n", ch)
        return
    }

    /* this means they have no bonus */
    var fail = 25  /* base 25% chance of failure */

    /* find the bonuses */

    if (!obj.enchanted) {
        var paf: Affect? = obj.pIndexData.affected
        while (paf != null) {
            when {
                paf.location == Apply.Hitroll -> {
                    val hit_bonus = paf.modifier
                    hit_found = true
                    fail += 2 * (hit_bonus * hit_bonus)
                }
                paf.location == Apply.Damroll -> {
                    val dam_bonus = paf.modifier
                    dam_found = true
                    fail += 2 * (dam_bonus * dam_bonus)
                }
                else -> fail += 25 /* things get a little harder */
            }
            paf = paf.next
        }
    }

    run {
        var paf = obj.affected
        while (paf != null) {
            when {
                paf!!.location == Apply.Hitroll -> {
                    val hit_bonus = paf!!.modifier
                    hit_found = true
                    fail += 2 * (hit_bonus * hit_bonus)
                }
                paf!!.location == Apply.Damroll -> {
                    val dam_bonus = paf!!.modifier
                    dam_found = true
                    fail += 2 * (dam_bonus * dam_bonus)
                }
                else -> fail += 25/* things get a little harder */
            }
            paf = paf!!.next
        }
    }

    /* apply other modifiers */
    fail -= 3 * level / 2

    if (IS_OBJ_STAT(obj, ITEM_BLESS)) {
        fail -= 15
    }
    if (IS_OBJ_STAT(obj, ITEM_GLOW)) {
        fail -= 5
    }

    fail = URANGE(5, fail, 95)

    val result = randomPercent()

    /* the moment of truth */
    if (result < fail / 5)
    /* item destroyed */ {
        act("\$p shivers violently and explodes!", ch, obj, null, TO_CHAR)
        act("\$p shivers violently and explodeds!", ch, obj, null, TO_ROOM)
        extract_obj(obj)
        return
    }

    if (result < fail / 2)
    /* item disenchanted */ {

        act("\$p glows brightly, then fades...oops.", ch, obj, null, TO_CHAR)
        act("\$p glows brightly, then fades.", ch, obj, null, TO_ROOM)
        obj.enchanted = true

        /* remove all affects */
        obj.affected = null

        /* clear all flags */
        obj.extra_flags = 0
        return
    }

    if (result <= fail)
    /* failed, no bad result */ {
        send_to_char("Nothing seemed to happen.\n", ch)
        return
    }

    /* okay, move all the old flags into new vectors if we have to */
    if (!obj.enchanted) {
        var af_new: Affect
        obj.enchanted = true

        var paf: Affect? = obj.pIndexData.affected
        while (paf != null) {
            af_new = Affect()

            af_new.next = obj.affected
            obj.affected = af_new

            af_new.where = paf!!.where
            af_new.type = paf!!.type
            af_new.level = paf!!.level
            af_new.duration = paf!!.duration
            af_new.location = paf!!.location
            af_new.modifier = paf!!.modifier
            af_new.bitvector = paf!!.bitvector
            paf = paf!!.next
        }
    }

    val added: Int
    if (result <= 100 - level / 5) { /* success! */
        act("\$p glows blue.", ch, obj, null, TO_CHAR)
        act("\$p glows blue.", ch, obj, null, TO_ROOM)
        obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_MAGIC)
        added = 1
    } else { /* exceptional enchant */
        act("\$p glows a brillant blue!", ch, obj, null, TO_CHAR)
        act("\$p glows a brillant blue!", ch, obj, null, TO_ROOM)
        obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_MAGIC)
        obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_GLOW)
        added = 2
    }

    /* now add the enchantments */

    if (obj.level < LEVEL_HERO - 1) {
        obj.level = Math.min(LEVEL_HERO - 1, obj.level + 1)
    }

    if (dam_found) {
        var paf = obj.affected
        while (paf != null) {
            if (paf!!.location == Apply.Damroll) {
                paf!!.type = Skill.enchant_weapon
                paf!!.modifier += added
                paf!!.level = Math.max(paf!!.level, level)
                if (paf!!.modifier > 4) {
                    obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_HUM)
                }
            }
            paf = paf!!.next
        }
    } else { /* add a new affect */
        val paf = Affect()

        paf.where = TO_OBJECT
        paf.type = Skill.enchant_weapon
        paf.level = level
        paf.duration = -1
        paf.location = Apply.Damroll
        paf.modifier = added
        paf.next = obj.affected
        obj.affected = paf
    }

    if (hit_found) {
        var paf = obj.affected
        while (paf != null) {
            if (paf!!.location == Apply.Hitroll) {
                paf!!.type = Skill.enchant_weapon
                paf!!.modifier += added
                paf!!.level = Math.max(paf!!.level, level)
                if (paf!!.modifier > 4) {
                    obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_HUM)
                }
            }
            paf = paf!!.next
        }
    } else { /* add a new affect */
        val paf = Affect()
        paf.type = Skill.enchant_weapon
        paf.level = level
        paf.duration = -1
        paf.location = Apply.Hitroll
        paf.modifier = added
        paf.next = obj.affected
        obj.affected = paf
    }
}

/*
* Drain XP, MANA, HP.
* Caster gains HP.
*/

fun spell_energy_drain(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (saves_spell(level, victim, DT.Negative)) {
        send_to_char("You feel a momentary chill.\n", victim)
        return
    }

    val dam: Int
    if (victim.level <= 2) {
        dam = ch.hit + 1
    } else {
        gain_exp(victim, 0 - number_range(level / 5, 3 * level / 5))
        victim.mana /= 2
        victim.move /= 2
        dam = dice(1, level)
        ch.hit += dam
    }

    send_to_char("You feel your life slipping away!\n", victim)
    send_to_char("Wow....what a rush!\n", ch)
    damage(ch, victim, dam, Skill.energy_drain, DT.Negative, true)
}

fun spell_hellfire(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    val dam = dice(level, 7)

    damage(ch, victim, dam, Skill.hellfire, DT.Fire, true)

}

fun spell_iceball(level: Int, ch: CHAR_DATA) {
    var dam = dice(level, 12)
    val movedam = number_range(ch.level, 2 * ch.level)

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

            if (saves_spell(level, tmp_vict, DT.Cold)) {
                dam /= 2
            }
            damage(ch, tmp_vict, dam, Skill.iceball, DT.Cold, true)
            tmp_vict.move -= Math.min(tmp_vict.move, movedam)

        }
    }
}

fun spell_fireball(level: Int, ch: CHAR_DATA) {
    var dam = dice(level, 12)
    val movedam = number_range(ch.level, 2 * ch.level)

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

            if (saves_spell(level, tmp_vict, DT.Fire)) {
                dam /= 2
            }
            damage(ch, tmp_vict, dam, Skill.fireball, DT.Fire, true)
            tmp_vict.move -= Math.min(tmp_vict.move, movedam)

        }
    }
}


fun spell_fireproof(level: Int, ch: CHAR_DATA, obj: Obj) {
    if (IS_OBJ_STAT(obj, ITEM_BURN_PROOF)) {
        act("\$p is already protected from burning.", ch, obj, null, TO_CHAR)
        return
    }
    val af = Affect()
    af.where = TO_OBJECT
    af.type = Skill.fireproof
    af.level = level
    af.duration = number_fuzzy(level / 4)
    af.bitvector = ITEM_BURN_PROOF

    affect_to_obj(obj, af)

    act("You protect \$p from fire.", ch, obj, null, TO_CHAR)
    act("\$p is surrounded by a protective aura.", ch, obj, null, TO_ROOM)
}


fun spell_flamestrike(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 10)

    if (saves_spell(level, victim, DT.Fire)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.flamestrike, DT.Fire, true)
}


fun spell_faerie_fire(level: Int, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_FAERIE_FIRE)) {
        return
    }
    val af = Affect()
    af.type = Skill.faerie_fire
    af.level = level
    af.duration = 10 + level / 5
    af.location = Apply.Ac
    af.modifier = 2 * level
    af.bitvector = AFF_FAERIE_FIRE
    affect_to_char(victim, af)
    send_to_char("You are surrounded by a pink outline.\n", victim)
    act("\$n is surrounded by a pink outline.", victim, null, null, TO_ROOM)
}


fun spell_faerie_fog(level: Int, ch: CHAR_DATA) {
    act("\$n conjures a cloud of purple smoke.", ch, null, null, TO_ROOM)
    send_to_char("You conjure a cloud of purple smoke.\n", ch)

    for (ich in ch.room.people) {
        if (ich.invis_level > 0) {
            continue
        }

        if (ich == ch || saves_spell(level, ich, DT.Other)) {
            continue
        }

        affect_strip(ich, Skill.invisibility)
        affect_strip(ich, Skill.mass_invis)
        affect_strip(ich, Skill.improved_invis)
        ich.affected_by = REMOVE_BIT(ich.affected_by, AFF_HIDE)
        ich.affected_by = REMOVE_BIT(ich.affected_by, AFF_FADE)
        ich.affected_by = REMOVE_BIT(ich.affected_by, AFF_INVISIBLE)
        ich.affected_by = REMOVE_BIT(ich.affected_by, AFF_IMP_INVIS)

        /* An elf sneaks eternally */
        if (ich.pcdata == null || !IS_SET(ich.race.aff, AFF_SNEAK)) {
            affect_strip(ich, Skill.sneak)
            ich.affected_by = REMOVE_BIT(ich.affected_by, AFF_SNEAK)
        }

        act("\$n is revealed!", ich, null, null, TO_ROOM)
        send_to_char("You are revealed!\n", ich)
    }

}

fun spell_floating_disc(level: Int, ch: CHAR_DATA) {
    val floating = get_eq_char(ch, Wear.Float)
    if (floating != null && IS_OBJ_STAT(floating, ITEM_NOREMOVE)) {
        act("You can't remove \$p.", ch, floating, null, TO_CHAR)
        return
    }

    val disc = create_object(get_obj_index_nn(OBJ_VNUM_DISC), 0)
    disc.value[0] = (ch.level * 10).toLong() /* 10 pounds per level capacity */
    disc.value[3] = (ch.level * 5).toLong() /* 5 pounds per level max per item */
    disc.timer = ch.level * 2 - number_range(0, level / 2)

    act("\$n has created a floating black disc.", ch, null, null, TO_ROOM)
    send_to_char("You create a floating disc.\n", ch)
    obj_to_char(disc, ch)
    wear_obj(ch, disc, true)
}


fun spell_fly(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_FLYING)) {
        if (victim == ch) {
            send_to_char("You are already airborne.\n", ch)
        } else {
            act("\$N doesn't need your help to fly.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.fly
    af.level = level
    af.duration = level + 3
    af.bitvector = AFF_FLYING
    affect_to_char(victim, af)
    send_to_char("Your feet rise off the ground.\n", victim)
    act("\$n's feet rise off the ground.", victim, null, null, TO_ROOM)
}

/* RT clerical berserking spell */

fun spell_frenzy(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.frenzy) || IS_AFFECTED(victim, AFF_BERSERK)) {
        if (victim == ch) {
            send_to_char("You are already in a frenzy.\n", ch)
        } else {
            act("\$N is already in a frenzy.", ch, null, victim, TO_CHAR)
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

    if (ch.align !== victim.align) {
        act("Your god doesn't seem to like \$N", ch, null, victim, TO_CHAR)
        return
    }
    val af = Affect()
    af.type = Skill.frenzy
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

    send_to_char("You are filled with holy wrath!\n", victim)
    act("\$n gets a wild look in \$s eyes!", victim, null, null, TO_ROOM)
}

fun spell_gate(level: Int, ch: CHAR_DATA) {
    val victim = get_char_world(ch, target_name)
    if (victim == null
            || victim == ch
            || !can_see_room(ch, victim.room)
            || IS_SET(victim.room.room_flags, ROOM_SAFE)
            || IS_SET(victim.room.room_flags, ROOM_PRIVATE)
            || IS_SET(victim.room.room_flags, ROOM_SOLITARY)
            || IS_SET(ch.room.room_flags, ROOM_NOSUMMON)
            || IS_SET(victim.room.room_flags, ROOM_NOSUMMON)
            || victim.level >= level + 3
            || saves_spell(level, victim, DT.Other)
            /*    ||   (!IS_NPC(victim) && victim.level >= LEVEL_HERO)  * NOT trust */
            || victim.pcdata == null && is_safe_nomessage(ch, victim) && IS_SET(victim.imm_flags, IMM_SUMMON)
            || victim.pcdata != null && is_safe_nomessage(ch, victim) && IS_SET(victim.act, PLR_NOSUMMON)
            || victim.pcdata != null && ch.room.area != victim.room.area
            || victim.pcdata == null && saves_spell(level, victim, DT.Other)) {
        send_to_char("You failed.\n", ch)
        return
    }
    val pet = ch.pet

    act("\$n steps through a gate and vanishes.", ch, null, null, TO_ROOM)
    send_to_char("You step through a gate and vanish.\n", ch)
    char_from_room(ch)
    char_to_room(ch, victim.room)

    act("\$n has arrived through a gate.", ch, null, null, TO_ROOM)
    do_look(ch, "auto")

    if (pet != null && ch.room == pet.room) {
        act("\$n steps through a gate and vanishes.", pet, null, null, TO_ROOM)
        send_to_char("You step through a gate and vanish.\n", pet)
        char_from_room(pet)
        char_to_room(pet, victim.room)
        act("\$n has arrived through a gate.", pet, null, null, TO_ROOM)
        do_look(pet, "auto")
    }
}


fun spell_giant_strength(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.giant_strength)) {
        if (victim == ch) {
            send_to_char("You are already as strong as you can get!\n", ch)
        } else {
            act("\$N can't get any stronger.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.giant_strength
    af.level = level
    af.duration = 10 + level / 3
    af.location = Apply.Str
    af.modifier = Math.max(2, level / 10)
    affect_to_char(victim, af)
    send_to_char("Your muscles surge with heightened power!\n", victim)
    act("\$n's muscles surge with heightened power.", victim, null, null, TO_ROOM)
}


fun spell_harm(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = Math.max(20, victim.hit - dice(1, 4))
    if (saves_spell(level, victim, DT.Harm)) {
        dam = Math.min(50, dam / 2)
    }
    dam = Math.min(100, dam)
    damage(ch, victim, dam, Skill.harm, DT.Harm, true)
}

/* RT haste spell */

fun spell_haste(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.haste) || IS_AFFECTED(victim, AFF_HASTE)
            || IS_SET(victim.off_flags, OFF_FAST)) {
        if (victim == ch) {
            send_to_char("You can't move any faster!\n", ch)
        } else {
            act("\$N is already moving as fast as \$E can.",
                    ch, null, victim, TO_CHAR)
        }
        return
    }

    if (IS_AFFECTED(victim, AFF_SLOW)) {
        if (!check_dispel(level, victim, Skill.slow)) {
            if (victim != ch) {
                send_to_char("Spell failed.\n", ch)
            }
            send_to_char("You feel momentarily faster.\n", victim)
            return
        }
        act("\$n is moving less slowly.", victim, null, null, TO_ROOM)
        return
    }
    val af = Affect()
    af.type = Skill.haste
    af.level = level
    af.duration = if (victim == ch) level / 2 else level / 4
    af.location = Apply.Dex
    af.modifier = Math.max(2, level / 12)
    af.bitvector = AFF_HASTE
    affect_to_char(victim, af)
    send_to_char("You feel yourself moving more quickly.\n", victim)
    act("\$n is moving more quickly.", victim, null, null, TO_ROOM)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}


fun spell_heal(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    victim.hit = Math.min(victim.hit + 100 + level / 10, victim.max_hit)
    update_pos(victim)
    send_to_char("A warm feeling fills your body.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_heat_metal(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = 0
    var fail = true

    if (!saves_spell(level + 2, victim, DT.Fire) && !IS_SET(victim.imm_flags, IMM_FIRE)) {
        victim.carrying
                .filter { number_range(1, 2 * level) > it.level && !saves_spell(level, victim, DT.Fire) && is_metal(it) && !IS_OBJ_STAT(it, ITEM_BURN_PROOF) }
                .forEach {
                    when (it.item_type) {
                        ItemType.Armor ->
                            if (it.wear_loc.isOn()) { /* remove the item */
                                if (can_drop_obj(victim, it)
                                        && it.weight / 10 < number_range(1, 2 * victim.curr_stat(PrimeStat.Dexterity))
                                        && remove_obj(victim, it, true)) {
                                    act("\$n yelps and throws \$p to the ground!", victim, it, null, TO_ROOM)
                                    act("You remove and drop \$p before it burns you.", victim, it, null, TO_CHAR)
                                    dam += number_range(1, it.level) / 3
                                    obj_from_char(it)
                                    obj_to_room(it, victim.room)
                                    fail = false
                                } else { /* stuck on the body! ouch! */
                                    act("Your skin is seared by \$p!", victim, it, null, TO_CHAR)
                                    dam += number_range(1, it.level)
                                    fail = false
                                }
                            } else { /* drop it if we can */
                                if (can_drop_obj(victim, it)) {
                                    act("\$n yelps and throws \$p to the ground!", victim, it, null, TO_ROOM)
                                    act("You and drop \$p before it burns you.", victim, it, null, TO_CHAR)
                                    dam += number_range(1, it.level) / 6
                                    obj_from_char(it)
                                    obj_to_room(it, victim.room)
                                    fail = false
                                } else { /* cannot drop */
                                    act("Your skin is seared by \$p!", victim, it, null, TO_CHAR)
                                    dam += number_range(1, it.level) / 2
                                    fail = false
                                }
                            }
                        ItemType.Weapon ->
                            if (it.wear_loc.isOn()) { /* try to drop it */
                                if (IS_WEAPON_STAT(it, WEAPON_FLAMING)) {
                                    // do nothing
                                } else if (can_drop_obj(victim, it) && remove_obj(victim, it, true)) {
                                    act("\$n is burned by \$p, and throws it to the ground.", victim, it, null, TO_ROOM)
                                    send_to_char("You throw your red-hot weapon to the ground!\n", victim)
                                    dam += 1
                                    obj_from_char(it)
                                    obj_to_room(it, victim.room)
                                    fail = false
                                } else { /* YOWCH! */
                                    send_to_char("Your weapon sears your flesh!\n", victim)
                                    dam += number_range(1, it.level)
                                    fail = false
                                }
                            } else { /* drop it if we can */
                                if (can_drop_obj(victim, it)) {
                                    act("\$n throws a burning hot \$p to the ground!", victim, it, null, TO_ROOM)
                                    act("You and drop \$p before it burns you.", victim, it, null, TO_CHAR)
                                    dam += number_range(1, it.level) / 6
                                    obj_from_char(it)
                                    obj_to_room(it, victim.room)
                                    fail = false
                                } else { /* cannot drop */
                                    act("Your skin is seared by \$p!", victim, it, null, TO_CHAR)
                                    dam += number_range(1, it.level) / 2
                                    fail = false
                                }
                            }
                        else -> {
                        }
                    }
                }
    }
    if (fail) {
        send_to_char("Your spell had no effect.\n", ch)
        send_to_char("You feel momentarily warmer.\n", victim)
    } else { /* damage! */
        if (saves_spell(level, victim, DT.Fire)) {
            dam = 2 * dam / 3
        }
        damage(ch, victim, dam, Skill.heat_metal, DT.Fire, true)
    }
}

fun spell_identify(ch: CHAR_DATA, obj: Obj) {
    send_to_char("Object '%s' is type %s, extra flags %s.\nWeight is %d, value is %d, level is %d.\n"
            .format(obj.name,
                    obj.item_type.displayName,
                    extra_bit_name(obj.extra_flags),
                    obj.weight / 10,
                    obj.cost,
                    obj.level), ch)

    if (obj.pIndexData.limit != -1) {
        send_to_char("This equipment has been LIMITED by number ${obj.pIndexData.limit}\n", ch)
    }

    when (obj.item_type) {
        ItemType.Scroll, ItemType.Potion, ItemType.Pill -> {
            send_to_char("Level ${obj.value[0]} spells of:", ch)

            if (obj.value[1] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[1].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            if (obj.value[2] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[2].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            if (obj.value[3] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[3].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            if (obj.value[4] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[4].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            send_to_char(".\n", ch)
        }

        ItemType.Wand, ItemType.Staff -> {
            send_to_char("Has %d charges of level %d".format(obj.value[2], obj.value[0]), ch)

            if (obj.value[3] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[3].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            send_to_char(".\n", ch)
        }

        ItemType.DrinkContainer -> {
            val liquid = Liquid.of(obj.value[2])
            send_to_char("It holds %s-colored %s.\n".format(liquid.liq_color, liquid.liq_name), ch)
        }

        ItemType.Container -> {
            send_to_char("Capacity: %d#  Maximum weight: %d#  flags: %s\n".format(obj.value[0], obj.value[3], cont_bit_name(obj.value[1])), ch)
            if (obj.value[4] != 100L) {
                send_to_char("Weight multiplier: %d%%\n".format(obj.value[4]), ch)
            }
        }

        ItemType.Weapon -> {
            send_to_char("Weapon type is ${obj.weaponType.typeName}.\n", ch)
            val msg = if (obj.pIndexData.new_format) {
                "Damage is %dd%d (average %d).\n".format(obj.value[1], obj.value[2], (1 + obj.value[2]) * obj.value[1] / 2)
            } else {
                "Damage is %d to %d (average %d).\n".format(obj.value[1], obj.value[2], (obj.value[1] + obj.value[2]) / 2)
            }
            send_to_char(msg, ch)
            if (obj.value[4] != 0L) {
                /* weapon flags */
                send_to_char("Weapons flags: %s\n".format(weapon_bit_name(obj.value[4])), ch)
            }
        }

        ItemType.Armor -> {
            send_to_char("Armor class is %d pierce, %d bash, %d slash, and %d vs. magic.\n"
                    .format(obj.value[0], obj.value[1], obj.value[2], obj.value[3])
                    , ch)
        }
        else -> {
        }
    }

    if (!obj.enchanted) {
        var paf: Affect? = obj.pIndexData.affected
        while (paf != null) {
            if (paf.location != Apply.None && paf.modifier != 0) {
                send_to_char("Affects %s by %d.\n".format(paf.location.locName, paf.modifier), ch)
                if (paf.bitvector != 0L) {
                    val msg = when (paf.where) {
                        TO_AFFECTS -> "Adds %s affect.\n".format(affect_bit_name(paf.bitvector))
                        TO_OBJECT -> "Adds %s object flag.\n".format(extra_bit_name(paf.bitvector))
                        TO_IMMUNE -> "Adds immunity to %s.\n".format(imm_bit_name(paf.bitvector))
                        TO_RESIST -> "Adds resistance to %s.\n".format(imm_bit_name(paf.bitvector))
                        TO_VULN -> "Adds vulnerability to %s.\n".format(imm_bit_name(paf.bitvector))
                        else -> "Unknown bit %d: %d\n".format(paf.where, paf.bitvector)
                    }
                    send_to_char(msg, ch)
                }
            }
            paf = paf.next
        }
    }

    var paf = obj.affected
    while (paf != null) {
        if (paf.location != Apply.None && paf.modifier != 0) {
            send_to_char("Affects %s by %d".format(paf.location.locName, paf.modifier), ch)
            val msg = if (paf.duration > -1) {
                ", %d hours.\n".format(paf.duration)
            } else {
                ".\n"
            }
            send_to_char(msg, ch)

            if (paf.bitvector != 0L) {
                val msg2 = when (paf.where) {
                    TO_AFFECTS -> "Adds %s affect.\n".format(affect_bit_name(paf.bitvector))
                    TO_OBJECT -> "Adds %s object flag.\n".format(extra_bit_name(paf.bitvector))
                    TO_WEAPON -> "Adds %s weapon flags.\n".format(weapon_bit_name(paf.bitvector))
                    TO_IMMUNE -> "Adds immunity to %s.\n".format(imm_bit_name(paf.bitvector))
                    TO_RESIST -> "Adds resistance to %s.\n".format(imm_bit_name(paf.bitvector))
                    TO_VULN -> "Adds vulnerability to %s.\n".format(imm_bit_name(paf.bitvector))
                    else -> "Unknown bit %d: %d\n".format(paf.where, paf.bitvector)
                }
                send_to_char(msg2, ch)
            }
        }
        paf = paf.next
    }

}


fun spell_infravision(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_INFRARED)) {
        if (victim == ch) {
            send_to_char("You can already see in the dark.\n", ch)
        } else {
            act("\$N already has infravision.\n", ch, null, victim, TO_CHAR)
        }
        return
    }
    act("\$n's eyes glow red.\n", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.infravision
    af.level = level
    af.duration = 2 * level
    af.bitvector = AFF_INFRARED
    affect_to_char(victim, af)
    send_to_char("Your eyes glow red.\n", victim)
}


fun spell_invis(level: Int, ch: CHAR_DATA, vo: Any, target: Int) {
    /* object invisibility */
    if (target == TARGET_OBJ) {
        val obj = vo as Obj

        if (IS_OBJ_STAT(obj, ITEM_INVIS)) {
            act("\$p is already invisible.", ch, obj, null, TO_CHAR)
            return
        }

        val af = Affect()
        af.where = TO_OBJECT
        af.type = Skill.invisibility
        af.level = level
        af.duration = level / 4 + 12
        af.bitvector = ITEM_INVIS
        affect_to_obj(obj, af)

        act("\$p fades out of sight.", ch, obj, null, TO_ALL)
        return
    }

    /* character invisibility */
    val victim = vo as CHAR_DATA

    if (IS_AFFECTED(victim, AFF_INVISIBLE)) {
        return
    }

    act("\$n fades out of existence.", victim, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.invisibility
    af.level = level
    af.duration = level / 8 + 10
    af.bitvector = AFF_INVISIBLE
    affect_to_char(victim, af)
    send_to_char("You fade out of existence.\n", victim)
}


fun spell_know_alignment(ch: CHAR_DATA, victim: CHAR_DATA) {
    var msg = when (victim.align) {
        Align.Good -> "\$N has a pure and good aura."
        Align.Neutral -> "\$N act as no align."
        else -> "\$N is the embodiment of pure evil!."
    }
    act(msg, ch, null, victim, TO_CHAR)

    if (victim.pcdata != null) {
        msg = when (victim.ethos) {
            Ethos.Lawful -> "\$N upholds the laws."
            Ethos.Neutral -> "\$N seems ambivalent to society."
            Ethos.Chaotic -> "\$N seems very chaotic."
        }
        act(msg, ch, null, victim, TO_CHAR)
    }
}


fun spell_lightning_bolt(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_GROUNDING)) {
        send_to_char("The electricity fizzles at your foes.\n", victim)
        act("A lightning bolt fizzles at \$N's foes.\n", ch, null, victim, TO_ROOM)
        return
    }
    var dam = dice(level, 4) + 12
    if (saves_spell(level, victim, DT.Lightning)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.lightning_bolt, DT.Lightning, true)
}


fun spell_locate_object(level: Int, ch: CHAR_DATA) {
    var found = false
    var number = 0
    val max_found = if (IS_IMMORTAL(ch)) 200 else 2 * level

    val buf = StringBuilder()
    for (obj in Index.OBJECTS) {
        if (!can_see_obj(ch, obj) || !is_name(target_name, obj.name) || IS_OBJ_STAT(obj, ITEM_NOLOCATE)
                || randomPercent() > 2 * level || ch.level < obj.level) {
            continue
        }

        found = true
        number++

        buf.append(capitalize_nn(prepareCarriedByText(ch, obj)))

        if (number >= max_found) {
            break
        }
    }

    if (!found) {
        send_to_char("Nothing like that in heaven or earth.\n", ch)
    } else {
        page_to_char(buf, ch)
    }
}

private fun prepareCarriedByText(ch: CHAR_DATA, obj: Obj): String {
    var in_obj: Obj?
    in_obj = obj
    while (in_obj!!.in_obj != null) {
        in_obj = in_obj.in_obj
    }

    val carried_by = in_obj.carried_by
    return if (carried_by != null && can_see(ch, carried_by)) {
        "one is carried by ${PERS(carried_by, ch)}\n"
    } else {
        val in_room = in_obj.in_room
        when {
            IS_IMMORTAL(ch) && in_room != null -> "one is in ${in_room.name} [Room ${in_room.vnum}]\n"
            in_room != null -> "one is in ${in_room.name}\n"
            else -> "one is somewhere\n"
        }
    }
}

private val dam_each_mm = intArrayOf(0, 3, 3, 4, 4, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14)

fun spell_magic_missile(levelArg: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(ch, Skill.protective_shield)) {
        if (ch.level > 4) {
            send_to_char("Your magic missiles fizzle out near your victim.\n", ch)
            act("Your shield blocks \$N's magic missiles.", victim, null, ch, TO_CHAR)
        } else {
            send_to_char("Your magic missile fizzle out near your victim.\n", ch)
            act("Your shield blocks \$N's magic missile.", victim, null, ch, TO_CHAR)
        }
        return
    }


    var level = Math.min(levelArg, dam_each_mm.size - 1)
    level = Math.max(0, level)
    var dam = if (ch.level > 50) {
        level / 4
    } else {
        number_range(dam_each_mm[level] / 2, dam_each_mm[level] * 2)
    }

    if (saves_spell(level, victim, DT.Energy)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.magic_missile, DT.Energy, true)
    if (ch.level > 4) {
        dam = number_range(dam_each_mm[level] / 2, dam_each_mm[level] * 2)
        if (saves_spell(level, victim, DT.Energy)) {
            dam /= 2
        }
        damage(ch, victim, dam, Skill.magic_missile, DT.Energy, true)
    }
    if (ch.level > 8) {
        dam = number_range(dam_each_mm[level] / 2, dam_each_mm[level] * 2)
        if (saves_spell(level, victim, DT.Energy)) {
            dam /= 2
        }
        damage(ch, victim, dam, Skill.magic_missile, DT.Energy, true)
    }
    if (ch.level > 12) {
        dam = number_range(dam_each_mm[level] / 2, dam_each_mm[level] * 2)
        if (saves_spell(level, victim, DT.Energy)) {
            dam /= 2
        }
        damage(ch, victim, dam, Skill.magic_missile, DT.Energy, true)
    }
    if (ch.level > 16) {
        dam = number_range(dam_each_mm[level] / 2, dam_each_mm[level] * 2)
        if (saves_spell(level, victim, DT.Energy)) {
            dam /= 2
        }
        damage(ch, victim, dam, Skill.magic_missile, DT.Energy, true)
    }

}

fun spell_mass_healing(level: Int, ch: CHAR_DATA) {
    for (gch in ch.room.people) {
        if (ch.pcdata == null && gch.pcdata == null || ch.pcdata != null && gch.pcdata != null) {
            spell_heal(level, ch, gch)
            spell_refresh(level, ch, gch)
        }
    }
}


fun spell_mass_invis(level: Int, ch: CHAR_DATA) {
    for (gch in ch.room.people) {
        if (!is_same_group(gch, ch) || IS_AFFECTED(gch, AFF_INVISIBLE)) {
            continue
        }
        act("\$n slowly fades out of existence.", gch, null, null, TO_ROOM)
        send_to_char("You slowly fade out of existence.\n", gch)

        val af = Affect()
        af.type = Skill.mass_invis
        af.level = level / 2
        af.duration = 24
        af.bitvector = AFF_INVISIBLE
        affect_to_char(gch, af)
    }
    send_to_char("Ok.\n", ch)
}


fun spell_pass_door(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_PASS_DOOR)) {
        if (victim == ch) {
            send_to_char("You are already out of phase.\n", ch)
        } else {
            act("\$N is already shifted out of phase.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.pass_door
    af.level = level
    af.duration = number_fuzzy(level / 4)
    af.bitvector = AFF_PASS_DOOR
    affect_to_char(victim, af)
    act("\$n turns translucent.", victim, null, null, TO_ROOM)
    send_to_char("You turn translucent.\n", victim)
}

/* RT plague spell, very nasty */

fun spell_plague(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (saves_spell(level, victim, DT.Disease) || victim.pcdata == null && IS_SET(victim.act, ACT_UNDEAD)) {
        if (ch == victim) {
            send_to_char("You feel momentarily ill, but it passes.\n", ch)
        } else {
            act("\$N seems to be unaffected.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.plague
    af.level = level * 3 / 4
    af.duration = 10 + level / 10
    af.location = Apply.Str
    af.modifier = -1 * Math.max(1, 3 + level / 15)
    af.bitvector = AFF_PLAGUE
    affect_join(victim, af)

    send_to_char("You scream in agony as plague sores erupt from your skin.\n", victim)
    act("\$n screams in agony as plague sores erupt from \$s skin.",
            victim, null, null, TO_ROOM)
}


fun spell_protection_evil(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_PROTECT_EVIL) || IS_AFFECTED(victim, AFF_PROTECT_GOOD)) {
        if (victim == ch) {
            send_to_char("You are already protected.\n", ch)
        } else {
            act("\$N is already protected.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.protection_evil
    af.level = level
    af.duration = 10 + level / 5
    af.location = Apply.SavingSpell
    af.modifier = -1
    af.bitvector = AFF_PROTECT_EVIL
    affect_to_char(victim, af)
    send_to_char("You feel holy and pure.\n", victim)
    if (ch != victim) {
        act("\$N is protected from evil.", ch, null, victim, TO_CHAR)
    }
}

fun spell_protection_good(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_PROTECT_GOOD) || IS_AFFECTED(victim, AFF_PROTECT_EVIL)) {
        if (victim == ch) {
            send_to_char("You are already protected.\n", ch)
        } else {
            act("\$N is already protected.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.protection_good
    af.level = level
    af.duration = 10 + level / 5
    af.location = Apply.SavingSpell
    af.modifier = -1
    af.bitvector = AFF_PROTECT_GOOD
    affect_to_char(victim, af)
    send_to_char("You feel aligned with darkness.\n", victim)
    if (ch != victim) {
        act("\$N is protected from good.", ch, null, victim, TO_CHAR)
    }
}


fun spell_ray_of_truth(level: Int, ch: CHAR_DATA, vo: CHAR_DATA) {
    var victim = vo

    if (ch.isEvil()) {
        victim = ch
        send_to_char("The energy explodes inside you!\n", ch)
    }

    if (victim != ch) {
        act("\$n raises \$s hand, and a blinding ray of light shoots forth!",
                ch, null, null, TO_ROOM)
        send_to_char(
                "You raise your hand and a blinding ray of light shoots forth!\n",
                ch)
    }

    if (victim.isGood()) {
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

    damage(ch, victim, dam, Skill.ray_of_truth, DT.Holy, true)
    spell_blindness(3 * level / 4, ch, victim)
}


fun spell_recharge(level: Int, ch: CHAR_DATA, obj: Obj) {
    if (obj.item_type != ItemType.Wand && obj.item_type != ItemType.Staff) {
        send_to_char("That item does not carry charges.\n", ch)
        return
    }

    if (obj.value[3] >= 3 * level / 2) {
        send_to_char("Your skills are not great enough for that.\n", ch)
        return
    }

    if (obj.value[1] == 0L) {
        send_to_char("That item has already been recharged once.\n", ch)
        return
    }

    var chance = 40 + 2 * level

    chance -= obj.value[3].toInt() /* harder to do high-level spells */
    chance -= ((obj.value[1] - obj.value[2]) * (obj.value[1] - obj.value[2])).toInt()

    chance = Math.max(level / 2, chance)

    val percent = randomPercent()

    when {
        percent < chance / 2 -> {
            act("\$p glows softly.", ch, obj, null, TO_CHAR)
            act("\$p glows softly.", ch, obj, null, TO_ROOM)
            obj.value[2] = Math.max(obj.value[1], obj.value[2])
            obj.value[1] = 0
        }
        percent <= chance -> {
            act("\$p glows softly.", ch, obj, null, TO_CHAR)
            act("\$p glows softly.", ch, obj, null, TO_CHAR)

            val chargemax = (obj.value[1] - obj.value[2]).toInt()
            val chargeback = if (chargemax > 0) Math.max(1, chargemax * percent / 100) else 0

            obj.value[2] += chargeback.toLong()
            obj.value[1] = 0
        }
        percent <= Math.min(95, 3 * chance / 2) -> {
            send_to_char("Nothing seems to happen.\n", ch)
            if (obj.value[1] > 1) {
                obj.value[1]--
            }
        }
        else -> /* whoops! */ {
            act("\$p glows brightly and explodes!", ch, obj, null, TO_CHAR)
            act("\$p glows brightly and explodes!", ch, obj, null, TO_ROOM)
            extract_obj(obj)
        }
    }
}

fun spell_refresh(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    victim.move = Math.min(victim.move + level, victim.max_move)
    if (victim.max_move == victim.move) {
        send_to_char("You feel fully refreshed!\n", victim)
    } else {
        send_to_char("You feel less tired.\n", victim)
    }
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}

fun spell_remove_curse(level: Int, ch: CHAR_DATA, vo: Any, target: Int) {
    /* do object cases first */
    if (target == TARGET_OBJ) {
        val obj = vo as Obj
        if (IS_OBJ_STAT(obj, ITEM_NODROP) || IS_OBJ_STAT(obj, ITEM_NOREMOVE)) {
            if (!IS_OBJ_STAT(obj, ITEM_NOUNCURSE) && !saves_dispel(level + 2, obj.level, 0)) {
                obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_NODROP)
                obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_NOREMOVE)
                act("\$p glows blue.", ch, obj, null, TO_ALL)
                return
            }
            act("The curse on \$p is beyond your power.", ch, obj, null, TO_CHAR)
        } else {
            send_to_char("Nothing happens...\n", ch)
        }
        return
    }

    /* characters */
    val victim = vo as CHAR_DATA

    if (check_dispel(level, victim, Skill.curse)) {
        send_to_char("You feel better.\n", victim)
        act("\$n looks more relaxed.", victim, null, null, TO_ROOM)
    }
    for (obj in victim.carrying) {
        if ((IS_OBJ_STAT(obj, ITEM_NODROP) || IS_OBJ_STAT(obj, ITEM_NOREMOVE)) && !IS_OBJ_STAT(obj, ITEM_NOUNCURSE)) {   /* attempt to remove curse */
            if (!saves_dispel(level, obj.level, 0)) {
                obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_NODROP)
                obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_NOREMOVE)
                act("Your \$p glows blue.", victim, obj, null, TO_CHAR)
                act("\$n's \$p glows blue.", victim, obj, null, TO_ROOM)
                break
            }
        }
    }
}

fun spell_sanctuary(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_SANCTUARY)) {
        if (victim == ch) {
            send_to_char("You are already in sanctuary.\n", ch)
        } else {
            act("\$N is already in sanctuary.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.sanctuary
    af.level = level
    af.duration = level / 6
    af.bitvector = AFF_SANCTUARY
    affect_to_char(victim, af)
    act("\$n is surrounded by a white aura.", victim, null, null, TO_ROOM)
    send_to_char("You are surrounded by a white aura.\n", victim)
}


fun spell_shield(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.shield)) {
        if (victim == ch) {
            send_to_char("You are already shielded from harm.\n", ch)
        } else {
            act("\$N is already protected by a shield.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.shield
    af.level = level
    af.duration = 8 + level / 3
    af.location = Apply.Ac
    af.modifier = -1 * Math.max(20, 10 + level / 3) /* af.modifier  = -20;*/
    affect_to_char(victim, af)
    act("\$n is surrounded by a force shield.", victim, null, null, TO_ROOM)
    send_to_char("You are surrounded by a force shield.\n", victim)
}

private val dam_each_sg = intArrayOf(6, 8, 10, 12, 14, 16, 18, 20, 25, 29, 33, 36, 39, 39, 39, 40, 40, 41, 41, 42, 42, 43, 43, 44, 44, 45, 45, 46, 46, 47, 47, 48, 48, 49, 49, 50, 50, 51, 51, 52, 52, 53, 53, 54, 54, 55, 55, 56, 56, 57, 57)

fun spell_shocking_grasp(levelArg: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var level = Math.min(levelArg, dam_each_sg.size - 1)
    level = Math.max(0, level)
    var dam = if (ch.level > 50) {
        level / 2
    } else {
        number_range(dam_each_sg[level] / 2, dam_each_sg[level] * 2)
    }
    if (saves_spell(level, victim, DT.Lightning)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.shocking_grasp, DT.Lightning, true)
}


fun spell_sleep(level: Int, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_SLEEP)
            || victim.pcdata == null && IS_SET(victim.act, ACT_UNDEAD)
            || level < victim.level
            || saves_spell(level - 4, victim, DT.Charm)) {
        return
    }
    val af = Affect()
    af.type = Skill.sleep
    af.level = level
    af.duration = 1 + level / 10
    af.bitvector = AFF_SLEEP
    affect_join(victim, af)

    if (IS_AWAKE(victim)) {
        send_to_char("You feel very sleepy ..... zzzzzz.\n", victim)
        act("\$n goes to sleep.", victim, null, null, TO_ROOM)
        victim.position = Pos.Sleeping
    }
}

fun spell_slow(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.slow) || IS_AFFECTED(victim, AFF_SLOW)) {
        if (victim == ch) {
            send_to_char("You can't move any slower!\n", ch)
        } else {
            act("\$N can't get any slower than that.", ch, null, victim, TO_CHAR)
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
    af.type = Skill.slow
    af.level = level
    af.duration = 4 + level / 12
    af.location = Apply.Dex
    af.modifier = -Math.max(2, level / 12)
    af.bitvector = AFF_SLOW
    affect_to_char(victim, af)
    send_to_char("You feel yourself slowing d o w n...\n", victim)
    act("\$n starts to move in slow motion.", victim, null, null, TO_ROOM)
}


@Suppress("unused")
fun spell_find_object(level: Int, ch: CHAR_DATA) {
    var found = false
    var number = 0
    val max_found = if (IS_IMMORTAL(ch)) 200 else 2 * level

    val buf = StringBuilder()
    for (obj in Index.OBJECTS) {
        if (!can_see_obj(ch, obj) || !is_name(target_name, obj.name) || randomPercent() > 2 * level || ch.level < obj.level) {
            continue
        }

        found = true
        number++

        val cbText = prepareCarriedByText(ch, obj)
        buf.append(capitalize_nn(cbText))

        if (number >= max_found) {
            break
        }
    }

    if (!found) {
        send_to_char("Nothing like that in heaven or earth.\n", ch)
    } else {
        page_to_char(buf, ch)
    }
}

fun spell_acid_arrow(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 12)
    if (saves_spell(level, victim, DT.Acid)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.acid_arrow, DT.Acid, true)
}

/* energy spells */

fun spell_etheral_fist(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 12)

    if (saves_spell(level, victim, DT.Energy)) {
        dam /= 2
    }
    act("A fist of black, otherworldly ether rams into \$N, leaving \$M looking stunned!", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.etheral_fist, DT.Energy, true)
}

fun spell_spectral_furor(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 8)

    if (saves_spell(level, victim, DT.Energy)) {
        dam /= 2
    }
    act("The fabric of the cosmos strains in fury about \$N!", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.spectral_furor, DT.Energy, true)
}

fun spell_disruption(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 9)

    if (saves_spell(level, victim, DT.Energy)) {
        dam /= 2
    }
    act("A weird energy encompasses \$N, causing you to question \$S continued existence.", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.disruption, DT.Energy, true)
}


fun spell_sonic_resonance(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 7)

    if (saves_spell(level, victim, DT.Energy)) {
        dam /= 2
    }
    act("A cylinder of kinetic energy enshrouds \$N causing \$S to resonate.",
            ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.sonic_resonance, DT.Energy, true)
    WAIT_STATE(victim, Skill.sonic_resonance.beats)
}

/* mental */
fun spell_mind_wrack(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 7)

    if (saves_spell(level, victim, DT.Mental)) {
        dam /= 2
    }
    act("\$n stares intently at \$N, causing \$N to seem very lethargic.",
            ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.mind_wrack, DT.Mental, true)
}

fun spell_mind_wrench(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 9)

    if (saves_spell(level, victim, DT.Mental)) {
        dam /= 2
    }
    act("\$n stares intently at \$N, causing \$N to seem very hyperactive.",
            ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.mind_wrench, DT.Mental, true)
}
/* acid */

fun spell_sulfurus_spray(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 7)

    if (saves_spell(level, victim, DT.Acid)) {
        dam /= 2
    }
    act("A stinking spray of sulfurous liquid rains down on \$N.", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.sulfurus_spray, DT.Acid, true)
}

fun spell_caustic_font(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 9)

    if (saves_spell(level, victim, DT.Acid)) {
        dam /= 2
    }
    act("A fountain of caustic liquid forms below \$N.  The smell of \$S degenerating tissues is revolting! ", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.caustic_font, DT.Acid, true)
}

fun spell_acetum_primus(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 8)
    if (saves_spell(level, victim, DT.Acid)) {
        dam /= 2
    }
    act("A cloak of primal acid enshrouds \$N, sparks form as it consumes all it touches. ", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.acetum_primus, DT.Acid, true)
}

/*  Electrical  */

fun spell_galvanic_whip(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 7)

    if (saves_spell(level, victim, DT.Lightning)) {
        dam /= 2
    }
    act("\$n conjures a whip of ionized particles, which lashes ferociously at \$N.", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.galvanic_whip, DT.Lightning, true)
}


fun spell_magnetic_trust(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 8)

    if (saves_spell(level, victim, DT.Lightning)) {
        dam /= 2
    }
    act("An unseen energy moves nearby, causing your hair to stand on end!", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.magnetic_trust, DT.Lightning, true)
}

fun spell_quantum_spike(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 9)
    if (saves_spell(level, victim, DT.Lightning)) {
        dam /= 2
    }
    act("\$N seems to dissolve into tiny unconnected particles, then is painfully reassembled.", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.quantum_spike, DT.Lightning, true)
}
