package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Align
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.AttackType
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.model.max
import net.sf.nightworks.util.CLEVEL_OK
import net.sf.nightworks.util.GET_AC
import net.sf.nightworks.util.GET_DAMROLL
import net.sf.nightworks.util.GET_HITROLL
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_BLINK_ON
import net.sf.nightworks.util.IS_GOLEM
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_QUESTOR
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_TRUSTED
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.IS_WEAPON_STAT
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.RIDDEN
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.flipCoin
import net.sf.nightworks.util.interpolate
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.oneChanceOf
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.random
import net.sf.nightworks.util.randomDir
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.sprintf
import java.io.File

/*
 * Control the fights going on.
 * Called periodically by update_handler.
 */

fun violence_update() {
    for (ch in Index.CHARS) {
        var victim = ch.fighting ?: continue

        if (IS_AWAKE(ch) && ch.room == victim.room) {
            multi_hit(ch, victim, null)
        } else {
            stop_fighting(ch, false)
        }

        victim = ch.fighting ?: continue
        if (victim.pcdata != null) {
            ch.last_fought = victim
        }


        ch.last_fight_time = current_time

        for (obj in ch.carrying) {
            if (IS_SET(obj.progtypes, OPROG_FIGHT)) {
                if (ch.fighting == null) {
                    break /* previously death victims! */
                }
                obj.pIndexData.oprogs.fight_prog!!(obj, ch)
            }
        }

        victim = ch.fighting ?: continue /* death victim */

        if (IS_SET(ch.progtypes, MPROG_FIGHT) && ch.wait <= 0) {
            ch.pIndexData.mprogs.fight_prog!!(ch, victim)
        }

        // Fun for the whole family!
        check_assist(ch, victim)
    }
}

/* for auto assisting */
fun check_assist(ch: CHAR_DATA, victim: CHAR_DATA) {
    for (rch in ch.room.people) {
        if (IS_AWAKE(rch) && rch.fighting == null) {

            /* quick check for ASSIST_PLAYER */
            if (ch.pcdata != null && rch.pcdata == null && IS_SET(rch.off_flags, ASSIST_PLAYERS) && rch.level + 6 > victim.level) {
                do_emote(rch, "screams and attacks!")
                multi_hit(rch, victim, null)
                continue
            }

            /* PCs next */
            if (rch.pcdata != null || IS_AFFECTED(rch, AFF_CHARM)) {
                if ((rch.pcdata != null && IS_SET(rch.act, PLR_AUTO_ASSIST) || IS_AFFECTED(rch, AFF_CHARM)) && is_same_group(ch, rch)) {
                    multi_hit(rch, victim, null)
                }

                continue
            }

            if (ch.pcdata != null && RIDDEN(rch) == ch) {
                multi_hit(rch, victim, null)
                continue
            }

            /* now check the NPC cases */

            if (ch.pcdata == null) {
                if (rch.pcdata == null && IS_SET(rch.off_flags, ASSIST_ALL)
                        || rch.pcdata == null && rch.race === ch.race
                        && IS_SET(rch.off_flags, ASSIST_RACE)
                        || rch.pcdata == null && IS_SET(rch.off_flags, ASSIST_ALIGN)
                        && (rch.align === ch.align)
                        || rch.pIndexData == ch.pIndexData && IS_SET(rch.off_flags, ASSIST_VNUM)) {
                    if (flipCoin()) {
                        continue
                    }

                    var number = 0
                    var target: CHAR_DATA? = null
                    for (vch in ch.room.people) {
                        if (can_see(rch, vch) && is_same_group(vch, victim) && number_range(0, number) == 0) {
                            target = vch
                            number++
                        }
                    }

                    if (target != null) {
                        do_emote(rch, "screams and attacks!")
                        multi_hit(rch, target, null)
                    }
                }
            }
        }
    }
}

/** Do one group of attacks. */
fun multi_hit(ch: CHAR_DATA, victimArg: CHAR_DATA, dt: Skill?) {
    var victim = victimArg

    /* decrement the wait */
    if (get_pc(ch) == null) {
        ch.wait = Math.max(0, ch.wait - PULSE_VIOLENCE)
    }

    /* no attacks for stunnies -- just a check */
    if (ch.position < Pos.Resting) {
        return
    }

    /* ridden's adjustment */
    val ridden = RIDDEN(victim)
    val v_mount = victim.mount
    if (ridden != null && v_mount?.pcdata != null) {
        if (v_mount.fighting == null || v_mount.fighting == ch) {
            victim = v_mount
        } else {
            do_dismount(v_mount)
        }
    }

    /* no attacks on ghosts or attacks by ghosts */
    if (victim.pcdata != null && IS_SET(victim.act, PLR_GHOST) || ch.pcdata != null && IS_SET(ch.act, PLR_GHOST)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_WEAK_STUN)) {
        act("{WYou are too stunned to respond \$N's attack.{x", ch, null, victim, TO_CHAR, Pos.Fighting)
        act("{W\$n is too stunned to respond your attack.{x", ch, null, victim, TO_VICT, Pos.Fighting)
        act("{W\$n seems to be stunned.{x", ch, null, victim, TO_NOTVICT, Pos.Fighting)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_WEAK_STUN)
        return
    }

    if (IS_AFFECTED(ch, AFF_STUN)) {
        act("{WYou are too stunned to respond \$N's attack.{x", ch, null, victim, TO_CHAR, Pos.Fighting)
        act("{W\$n is too stunned to respond your attack.{x", ch, null, victim, TO_VICT, Pos.Fighting)
        act("{W\$n seems to be stunned.{x", ch, null, victim, TO_NOTVICT, Pos.Fighting)
        affect_strip(ch, Skill.power_word_stun)
        ch.affected_by = SET_BIT(ch.affected_by, AFF_WEAK_STUN)
        return
    }

    if (ch.pcdata == null) {
        mob_hit(ch, victim, dt)
        return
    }

    one_hit(ch, victim, dt, false)

    if (ch.fighting != victim) {
        return
    }

    if (CLEVEL_OK(ch, Skill.area_attack) && randomPercent() < get_skill(ch, Skill.area_attack)) {
        var count = 0

        check_improve(ch, Skill.area_attack, true, 6)

        val max_count = when {
            ch.level < 70 -> 1
            ch.level < 80 -> 2
            ch.level < 90 -> 3
            else -> 4
        }

        for (vch in ch.room.people) {
            if (vch != victim && vch.fighting == ch) {
                one_hit(ch, vch, dt, false)
                count++
            }
            if (count == max_count) {
                break
            }
        }
    }

    if (IS_AFFECTED(ch, AFF_HASTE)) one_hit(ch, victim, dt, false)

    if (ch.fighting != victim || dt == Skill.backstab || dt == Skill.cleave
            || dt == Skill.ambush || dt == Skill.dual_backstab || dt == Skill.circle
            || dt == Skill.assassinate || dt == Skill.vampiric_bite) {
        return
    }

    var chance = get_skill(ch, Skill.second_attack) / 2
    if (randomPercent() < chance) {
        one_hit(ch, victim, dt, false)
        check_improve(ch, Skill.second_attack, true, 5)
        if (ch.fighting != victim) {
            return
        }
    }

    chance = get_skill(ch, Skill.third_attack) / 3
    if (randomPercent() < chance) {
        one_hit(ch, victim, dt, false)
        check_improve(ch, Skill.third_attack, true, 6)
        if (ch.fighting != victim) {
            return
        }
    }


    chance = get_skill(ch, Skill.fourth_attack) / 5
    if (randomPercent() < chance) {
        one_hit(ch, victim, dt, false)
        check_improve(ch, Skill.fourth_attack, true, 7)
        if (ch.fighting != victim) {
            return
        }
    }

    chance = get_skill(ch, Skill.fifth_attack) / 6
    if (randomPercent() < chance) {
        one_hit(ch, victim, dt, false)
        check_improve(ch, Skill.fifth_attack, true, 8)
        if (ch.fighting != victim) {
            return
        }
    }

    chance = 20 + (get_skill(ch, Skill.second_weapon) * 0.8).toInt()
    if (randomPercent() < chance) {
        if (get_wield_char(ch, true) != null) {
            one_hit(ch, victim, dt, true)
            check_improve(ch, Skill.second_weapon, true, 2)
            if (ch.fighting != victim) {
                return
            }
        }
    }

    chance = get_skill(ch, Skill.secondary_attack) / 4
    if (randomPercent() < chance) {
        if (get_wield_char(ch, true) != null) {
            one_hit(ch, victim, dt, true)
            check_improve(ch, Skill.secondary_attack, true, 2)
        }
    }

}

/** procedure for all mobile attacks */

fun mob_hit(ch: CHAR_DATA, victim: CHAR_DATA, dt: Skill?) {
    /* no attacks on ghosts */
    if (victim.pcdata != null && IS_SET(victim.act, PLR_GHOST)) {
        return
    }

    /* no attack by ridden mobiles except spec_casts */
    if (RIDDEN(ch) != null) {
        if (ch.fighting != victim) {
            set_fighting(ch, victim)
        }
        return
    }

    one_hit(ch, victim, dt, false)

    if (ch.fighting != victim) {
        return
    }

    /* Area attack -- BALLS nasty! */

    if (IS_SET(ch.off_flags, OFF_AREA_ATTACK)) {
        ch.room.people
                .filter { it != victim && it.fighting == ch }
                .forEach { one_hit(ch, it, dt, false) }
    }

    if (IS_AFFECTED(ch, AFF_HASTE) || IS_SET(ch.off_flags, OFF_FAST)) {
        one_hit(ch, victim, dt, false)
    }

    if (ch.fighting != victim || dt == Skill.backstab || dt == Skill.circle ||
            dt == Skill.dual_backstab || dt == Skill.cleave || dt == Skill.ambush
            || dt == Skill.vampiric_bite) {
        return
    }

    var chance = get_skill(ch, Skill.second_attack) / 2
    if (randomPercent() < chance) {
        one_hit(ch, victim, dt, false)
        if (ch.fighting != victim) {
            return
        }
    }

    chance = get_skill(ch, Skill.third_attack) / 4
    if (randomPercent() < chance) {
        one_hit(ch, victim, dt, false)
        if (ch.fighting != victim) {
            return
        }
    }

    chance = get_skill(ch, Skill.fourth_attack) / 6
    if (randomPercent() < chance) {
        one_hit(ch, victim, dt, false)
        if (ch.fighting != victim) {
            return
        }
    }

    chance = get_skill(ch, Skill.second_weapon) / 2
    if (randomPercent() < chance) {
        if (get_wield_char(ch, true) != null) {
            one_hit(ch, victim, dt, true)
            if (ch.fighting != victim) {
                return
            }
        }
    }

    if (IS_SET(ch.act, ACT_MAGE)) {
        if (randomPercent() < 60 && ch.spec_fun == null) {
            mob_cast_mage(ch, victim)
            return
        }
    }


    if (IS_SET(ch.act, ACT_CLERIC)) {
        if (randomPercent() < 60 && ch.spec_fun == null) {
            mob_cast_cleric(ch, victim)
            return
        }
    }

    /* PC waits */

    if (ch.wait > 0) {
        return
    }

    /* now for the skills */

    val number = number_range(0, 7)

    when (number) {
        0 -> if (IS_SET(ch.off_flags, OFF_BASH)) do_bash(ch, "")
        1 -> if (IS_SET(ch.off_flags, OFF_BERSERK) && !IS_AFFECTED(ch, AFF_BERSERK)) do_berserk(ch)
        2 -> if (IS_SET(ch.off_flags, OFF_DISARM) ||
                get_weapon_sn(ch, false) != Skill.hand_to_hand
                        && (IS_SET(ch.act, ACT_WARRIOR) || IS_SET(ch.act, ACT_THIEF))) do_disarm(ch, "")
        3 -> if (IS_SET(ch.off_flags, OFF_KICK)) do_kick(ch)
        4 -> if (IS_SET(ch.off_flags, OFF_KICK_DIRT)) do_dirt(ch, "")
        5 -> if (IS_SET(ch.off_flags, OFF_TAIL)) do_tail(ch, "")
        6 -> if (IS_SET(ch.off_flags, OFF_TRIP)) do_trip(ch, "")
        7 -> if (IS_SET(ch.off_flags, OFF_CRUSH)) do_crush(ch)
    }
}

/** Hit one guy once. */

fun one_hit(ch: CHAR_DATA, victim: CHAR_DATA, dtArg: Skill?, secondaryArg: Boolean) {
    var dt = dtArg
    var secondary = secondaryArg
    var wield: Obj?
    var thac0: Int
    val thac0_00: Int
    var thac0_32: Int
    var dam: Int

    val corpse: Obj?

    var counter = false

    /* just in case */
    if (victim == ch) {
        return
    }

    /* ghosts can't fight */
    if (victim.pcdata != null && IS_SET(victim.act, PLR_GHOST) || ch.pcdata != null && IS_SET(ch.act, PLR_GHOST)) {
        return
    }

    /*
        * Can't beat a dead char!
        * Guard against weird room-leavings.
        */
    if (victim.position == Pos.Dead || ch.room != victim.room) {
        return
    }

    /* Figure out the type of damage message. */

    wield = get_wield_char(ch, secondary)

    /* if there is no weapon held by pro-hand, and there is a weapon  in the other hand, than don't fight with punch.*/
    if (!secondary && dt == null && wield == null && get_wield_char(ch, true) != null) {
        if (ch.fighting == victim) {
            return
        }
        secondary = true
        wield = get_wield_char(ch, true)
    }

    var dam_type: DT = DT.None
    if (dt == null) {
        dt = Skill.x_hit
        dam_type = if (wield != null && wield.item_type == ItemType.Weapon) {
            AttackType.of(wield.value[3]).damage
        } else {
            ch.attack.damage
        }
    } else if (dt.ordinal < Skill.x_hit.ordinal) {
        dam_type = if (wield != null) {
            AttackType.of(wield.value[3]).damage
        } else {
            ch.attack.damage
        }
    }
    if (dam_type == DT.None) {
        dam_type = DT.Bash
    }

    /* get the weapon skill */
    val sn = get_weapon_sn(ch, secondary)
    val skill = 20 + get_weapon_skill(ch, sn)

    /*
        * Calculate to-hit-armor-class-0 versus armor.
        */
    if (ch.pcdata == null) {
        thac0_00 = 20
        thac0_32 = -4   /* as good as a thief */
        when {
            IS_SET(ch.act, ACT_WARRIOR) -> thac0_32 = -10
            IS_SET(ch.act, ACT_THIEF) -> thac0_32 = -4
            IS_SET(ch.act, ACT_CLERIC) -> thac0_32 = 2
            IS_SET(ch.act, ACT_MAGE) -> thac0_32 = 6
        }
    } else {
        thac0_00 = ch.clazz.thac0_00
        thac0_32 = ch.clazz.thac0_32
    }

    thac0 = interpolate(ch.level, thac0_00, thac0_32)

    if (thac0 < 0) {
        thac0 /= 2
    }

    if (thac0 < -5) {
        thac0 = -5 + (thac0 + 5) / 2
    }

    thac0 -= GET_HITROLL(ch) * skill / 100
    thac0 += 5 * (100 - skill) / 100

    if (dt == Skill.backstab) {
        thac0 -= 10 * (100 - get_skill(ch, Skill.backstab))
    }

    if (dt == Skill.dual_backstab) {
        thac0 -= 10 * (100 - get_skill(ch, Skill.dual_backstab))
    }

    if (dt == Skill.cleave) {
        thac0 -= 10 * (100 - get_skill(ch, Skill.cleave))
    }

    if (dt == Skill.ambush) {
        thac0 -= 10 * (100 - get_skill(ch, Skill.ambush))
    }

    if (dt == Skill.vampiric_bite) {
        thac0 -= 10 * (100 - get_skill(ch, Skill.vampiric_bite))
    }

    var victim_ac = when (dam_type) {
        DT.Pierce -> GET_AC(victim, AC_PIERCE) / 10
        DT.Bash -> GET_AC(victim, AC_BASH) / 10
        DT.Slash -> GET_AC(victim, AC_SLASH) / 10
        else -> GET_AC(victim, AC_EXOTIC) / 10
    }

    if (victim_ac < -15) {
        victim_ac = (victim_ac + 15) / 5 - 15
    }

    if (get_skill(victim, Skill.armor_use) > 70) {
        check_improve(victim, Skill.armor_use, true, 8)
        victim_ac -= victim.level / 2
    }

    if (!can_see(ch, victim)) {
        if (skill_failure_nomessage(ch, Skill.blind_fighting, 0) == 0 && randomPercent() < get_skill(ch, Skill.blind_fighting)) {
            check_improve(ch, Skill.blind_fighting, true, 16)
        } else {
            victim_ac -= 4
        }
    }

    if (victim.position < Pos.Fighting) {
        victim_ac += 4
    }

    if (victim.position < Pos.Resting) {
        victim_ac += 6
    }

    // The moment of excitement!
    var diceroll = random(20)
    if (diceroll == 0 || diceroll != 19 && diceroll < thac0 - victim_ac) {
        // Miss
        damage(ch, victim, 0, dt, dam_type, true)
        return
    }

    /* Hit.  Calc damage. */
    if (ch.pcdata == null && wield == null) {
        dam = dice(ch.damage[DICE_NUMBER], ch.damage[DICE_TYPE])
    } else {
        if (sn != null) {
            check_improve(ch, sn, true, 5)
        }
        if (wield != null) {
            dam = if (wield.pIndexData.new_format) {
                dice(wield.value[1].toInt(), wield.value[2].toInt()) * skill / 100
            } else {
                number_range((wield.value[1] * skill / 100).toInt(), (wield.value[2] * skill / 100).toInt())
            }

            if (get_shield_char(ch) == null)
            /* no shield = more */ {
                dam = dam * 21 / 20
            }

            /* sharpness! */
            if (IS_WEAPON_STAT(wield, WEAPON_SHARP)) {
                val percent = randomPercent()
                if (percent <= skill / 8) {
                    dam = 2 * dam + dam * 2 * percent / 100
                }
            }
            /* holy weapon */
            if (IS_WEAPON_STAT(wield, WEAPON_HOLY) &&
                    ch.isGood() && victim.isEvil() && randomPercent() < 30) {
                act("{Y\$p shines with a holy area.{x", ch, wield, null, TO_CHAR, Pos.Dead)
                act("{Y\$p shines with a holy area.{x", ch, wield, null, TO_ROOM, Pos.Dead)
                dam += dam * 120 / 100
            }
        } else {
            if (CLEVEL_OK(ch, Skill.hand_to_hand)) {
                if (randomPercent() < get_skill(ch, Skill.hand_to_hand)) {
                    dam = number_range(4 + ch.level / 10, 2 * ch.level / 3) * skill / 100
                } else {
                    dam = number_range(5, ch.level / 2) * skill / 100
                    check_improve(ch, Skill.hand_to_hand, false, 5)
                }
            } else {
                dam = number_range(5, ch.level / 2) * skill / 100
            }

            if (get_skill(ch, Skill.mastering_pound) > 0) {
                val d = randomPercent()
                if (d <= get_skill(ch, Skill.mastering_pound)) {
                    check_improve(ch, Skill.mastering_pound, true, 6)
                    dam *= 2
                    if (d < 10) {
                        victim.affected_by = SET_BIT(victim.affected_by, AFF_WEAK_STUN)
                        act("{rYou hit \$N with a stunning force!{x", ch, null, victim, TO_CHAR, Pos.Dead)
                        act("{r\$n hit you with a stunning force!{x", ch, null, victim, TO_VICT, Pos.Dead)
                        act("{r\$n hits \$N with a stunning force!{x", ch, null, victim, TO_NOTVICT, Pos.Dead)
                        check_improve(ch, Skill.mastering_pound, true, 6)
                    }
                }

            }
        }
    }

    /*
        * Bonuses.
        */
    val skillLevel = get_skill(ch, Skill.enhanced_damage)
    if (skillLevel > 0) {
        diceroll = randomPercent()
        if (diceroll <= get_skill(ch, Skill.enhanced_damage)) {
            check_improve(ch, Skill.enhanced_damage, true, 6)
            dam += dam * diceroll * skillLevel / 10000
        }
    }

    if (get_skill(ch, Skill.mastering_sword) > 0 && sn == Skill.sword) {
        if (randomPercent() <= get_skill(ch, Skill.mastering_sword)) {
            var katana: Obj?

            check_improve(ch, Skill.mastering_sword, true, 6)
            dam += dam * 110 / 100

            katana = get_wield_char(ch, false)
            if (katana != null) {
                val paf: Affect?
                if (IS_WEAPON_STAT(katana, WEAPON_KATANA) && katana.extra_desc.description.contains(ch.name)) {
                    katana.cost++
                    if (katana.cost > 249) {
                        paf = affect_find(katana.affected, Skill.katana)
                        if (paf != null) {
                            val old_mod = paf.modifier
                            paf.modifier = Math.min(paf.modifier + 1, ch.level / 3)
                            ch.hitroll += paf.modifier - old_mod
                            val p_next = paf.next
                            if (p_next != null) {
                                p_next.modifier = paf.modifier
                                ch.damroll += paf.modifier - old_mod
                            }
                            act("\$n's katana glows blue.\n", ch, null, null, TO_ROOM)
                            send_to_char("Your katana glows blue.\n", ch)
                        }
                        katana.cost = 0
                    }
                }
            } else {
                katana = get_wield_char(ch, true)
                if (katana != null) {
                    val paf: Affect?

                    if (IS_WEAPON_STAT(katana, WEAPON_KATANA) && katana.extra_desc.description.contains(ch.name)) {
                        katana.cost++
                        if (katana.cost > 249) {
                            paf = affect_find(katana.affected, Skill.katana)
                            if (paf != null) {
                                val old_mod = paf.modifier
                                paf.modifier = Math.min(paf.modifier + 1, ch.level / 3)
                                ch.hitroll += paf.modifier - old_mod
                                val p_next = paf.next
                                if (p_next != null) {
                                    p_next.modifier = paf.modifier
                                    ch.damroll += paf.modifier - old_mod
                                }
                                act("\$n's katana glows blue.\n", ch, null, null, TO_ROOM)
                                send_to_char("Your katana glows blue.\n", ch)
                            }
                            katana.cost = 0
                        }
                    }
                }
            }
        }
    }

    if (!IS_AWAKE(victim)) {
        dam *= 2
    } else if (victim.position < Pos.Fighting) {
        dam = dam * 3 / 2
    }

    var sercount = randomPercent()

    if (dt == Skill.backstab || dt == Skill.vampiric_bite) {
        sercount += 40  /* 80% chance decrease of counter */
    }

    if (victim.last_fight_time != -1L && current_time - victim.last_fight_time < FIGHT_DELAY_TIME) {
        sercount += 25 /* 50% chance decrease of counter */
    }

    sercount *= 2

    if (victim.fighting == null && victim.pcdata != null &&
            !is_safe_nomessage(victim, ch) && !is_safe_nomessage(ch, victim) &&
            (victim.position == Pos.Sitting || victim.position == Pos.Standing)
            && dt != Skill.assassinate && sercount <= get_skill(victim, Skill.counter)) {
        counter = true
        check_improve(victim, Skill.counter, true, 1)
        act("\$N turns your attack against you!", ch, null, victim, TO_CHAR)
        act("You turn \$n's attack against \$m!", ch, null, victim, TO_VICT)
        act("\$N turns \$n's attack against \$m!", ch, null, victim, TO_NOTVICT)
        ch.fighting = victim
    } else if (victim.fighting == null) {
        check_improve(victim, Skill.counter, false, 1)
    }

    if (dt == Skill.backstab && wield != null) {
        dam = (1 + ch.level / 10) * dam + ch.level
    } else if (dt == Skill.dual_backstab && wield != null) {
        dam = (1 + ch.level / 14) * dam + ch.level
    } else if (dt == Skill.circle) {
        dam = (ch.level / 40 + 1) * dam + ch.level
    } else if (dt == Skill.vampiric_bite && IS_VAMPIRE(ch)) {
        dam = (ch.level / 20 + 1) * dam + ch.level
    } else if (dt == Skill.cleave && wield != null) {
        if (randomPercent() < URANGE(4, 5 + (ch.level - victim.level), 10) && !counter) {
            act("Your cleave chops \$N {rIN HALF!{x", ch, null, victim, TO_CHAR, Pos.Resting)
            act("\$n's cleave chops you {rIN HALF!{x", ch, null, victim, TO_VICT, Pos.Resting)
            act("\$n's cleave chops \$N {rIN HALF!{x", ch, null, victim, TO_NOTVICT, Pos.Resting)
            send_to_char("You have been KILLED!\n", victim)
            act("\$n is DEAD!", victim, null, null, TO_ROOM)
            WAIT_STATE(ch, 2)
            raw_kill(victim)
            if (ch.pcdata != null && victim.pcdata == null) {
                corpse = get_obj_list(ch, "corpse", ch.room.objects)

                if (IS_SET(ch.act, PLR_AUTO_LOOT) && corpse != null && corpse.contains.isNotEmpty())
                /* exists and not empty */ {
                    do_get(ch, "all corpse")
                }

                if (IS_SET(ch.act, PLR_AUTO_GOLD) && corpse != null && corpse.contains.isNotEmpty() && !IS_SET(ch.act, PLR_AUTO_LOOT)) { /* exists and not empty */
                    do_get(ch, "gold corpse")
                    do_get(ch, "silver corpse")
                }

                if (IS_SET(ch.act, PLR_AUTO_SAC)) {
                    if (IS_SET(ch.act, PLR_AUTO_LOOT) && corpse != null && corpse.contains.isNotEmpty()) {
                        return   /* leave if corpse has treasure */
                    } else {
                        do_sacrifice(ch, "corpse")
                    }
                }
            }
            return
        }
        dam = dam * 2 + ch.level
    }

    if (dt == Skill.assassinate) {
        if (randomPercent() <= URANGE(10, 20 + (ch.level - victim.level) * 2, 50) && !counter) {
            act("You {r+++ASSASSINATE+++{x \$N!", ch, null, victim, TO_CHAR, Pos.Resting)
            act("\$N is DEAD!", ch, null, victim, TO_CHAR)
            act("\$n {r+++ASSASSINATES+++{x \$N!", ch, null, victim, TO_NOTVICT, Pos.Resting)
            act("\$N is DEAD!", ch, null, victim, TO_NOTVICT)
            act("\$n {r+++ASSASSINATES+++{x you!", ch, null, victim, TO_VICT, Pos.Dead)
            send_to_char("You have been KILLED!\n", victim)
            check_improve(ch, Skill.assassinate, true, 1)
            raw_kill(victim)
            if (ch.pcdata != null && victim.pcdata == null) {
                corpse = get_obj_list(ch, "corpse", ch.room.objects)

                if (IS_SET(ch.act, PLR_AUTO_LOOT) && corpse != null && corpse.contains.isNotEmpty())
                /* exists and not empty */ {
                    do_get(ch, "all corpse")
                }

                if (IS_SET(ch.act, PLR_AUTO_GOLD) && corpse != null && corpse.contains.isNotEmpty() && !IS_SET(ch.act, PLR_AUTO_LOOT))
                /* exists and not empty */ {
                    do_get(ch, "gold corpse")
                }

                if (IS_SET(ch.act, PLR_AUTO_SAC)) {
                    if (IS_SET(ch.act, PLR_AUTO_LOOT) && corpse != null && corpse.contains.isNotEmpty()) {
                        return   /* leave if corpse has treasure */
                    } else {
                        do_sacrifice(ch, "corpse")
                    }
                }
            }
            return

        } else {
            check_improve(ch, Skill.assassinate, false, 1)
            dam *= 2
        }
    }


    dam += GET_DAMROLL(ch) * Math.min(100, skill) / 100

    if (dt == Skill.ambush) {
        dam *= 3
    }

    if (skill_failure_nomessage(ch, Skill.deathblow, 0) == 0 && get_skill(ch, Skill.deathblow) > 1) {
        if (randomPercent() < 0.125 * get_skill(ch, Skill.deathblow)) {
            act("You deliver a blow of deadly force!", ch, null, null, TO_CHAR)
            act("\$n delivers a blow of deadly force!", ch, null, null, TO_ROOM)

            dam *= (ch.level.toFloat() / 20).toInt()
            check_improve(ch, Skill.deathblow, true, 1)
        } else {
            check_improve(ch, Skill.deathblow, false, 3)
        }
    }

    if (dam <= 0) {
        dam = 1
    }

    val result: Boolean
    if (counter) {
        result = damage(ch, ch, 2 * dam, dt, dam_type, true)
        multi_hit(victim, ch, null)
    } else {
        result = damage(ch, victim, dam, dt, dam_type, true)
    }

    /* vampiric bite gives hp to ch from victim */
    if (dt == Skill.vampiric_bite) {
        ch.hit += Math.min(dam / 2, victim.max_hit)
        ch.hit = Math.min(ch.hit, ch.max_hit)
        update_pos(ch)
        send_to_char("Your health increases as you suck your victim's blood.\n", ch)
    }

    /* but do we have a funky weapon? */
    if (result && wield != null) {
        if (ch.fighting == victim && IS_WEAPON_STAT(wield, WEAPON_POISON)) {
            val poison = affect_find(wield.affected, Skill.poison)
            val level = poison?.level ?: wield.level
            if (!saves_spell(level / 2, victim, DT.Poison)) {
                send_to_char("You feel poison coursing through your veins.\n", victim)
                act("\$n is poisoned by the venom on \$p.", victim, wield, null, TO_ROOM)

                val af = Affect()
                af.type = Skill.poison
                af.level = level * 3 / 4
                af.duration = level / 2
                af.location = Apply.Str
                af.modifier = -1
                af.bitvector = AFF_POISON
                affect_join(victim, af)
            }

            /* weaken the poison if it's temporary */
            if (poison != null) {
                poison.level = Math.max(0, poison.level - 2)
                poison.duration = Math.max(0, poison.duration - 1)
                if (poison.level == 0 || poison.duration == 0) {
                    act("The poison on \$p has worn off.", ch, wield, null, TO_CHAR)
                }
            }
        }
        if (ch.fighting == victim && IS_WEAPON_STAT(wield, WEAPON_VAMPIRIC)) {
            dam = number_range(1, wield.level / 5 + 1)
            act("\$p draws life from \$n.", victim, wield, null, TO_ROOM)
            act("You feel \$p drawing your life away.", victim, wield, null, TO_CHAR)
            damage(ch, victim, dam, null, DT.Negative, false)
            ch.hit += dam / 2
        }
        if (ch.fighting == victim && IS_WEAPON_STAT(wield, WEAPON_FLAMING)) {
            dam = number_range(1, wield.level / 4 + 1)
            act("\$n is burned by \$p.", victim, wield, null, TO_ROOM)
            act("\$p sears your flesh.", victim, wield, null, TO_CHAR)
            fire_effect(victim, wield.level / 2, dam, TARGET_CHAR)
            damage(ch, victim, dam, null, DT.Fire, false)
        }
        if (ch.fighting == victim && IS_WEAPON_STAT(wield, WEAPON_FROST)) {
            dam = number_range(1, wield.level / 6 + 2)
            act("\$p freezes \$n.", victim, wield, null, TO_ROOM)
            act("The cold touch of \$p surrounds you with ice.", victim, wield, null, TO_CHAR)
            cold_effect(victim, wield.level / 2, dam, TARGET_CHAR)
            damage(ch, victim, dam, null, DT.Cold, false)
        }
        if (ch.fighting == victim && IS_WEAPON_STAT(wield, WEAPON_SHOCKING)) {
            dam = number_range(1, wield.level / 5 + 2)
            act("\$n is struck by lightning from \$p.", victim, wield, null, TO_ROOM)
            act("You are shocked by \$p.", victim, wield, null, TO_CHAR)
            shock_effect(victim, wield.level / 2, dam, TARGET_CHAR)
            damage(ch, victim, dam, null, DT.Lightning, false)
        }
    }

}

/** Inflict damage from a hit. */
fun damage(ch: CHAR_DATA, victim: CHAR_DATA, damArg: Int, dt: Skill?, dam_type: DT, show: Boolean): Boolean {
    var dam = damArg

    if (victim.position == Pos.Dead) {
        return false
    }

    /*
        * Stop up any residual loopholes.
        */
    if (dam > 1000 && !IS_IMMORTAL(ch)) {
        val buf = TextBuffer()
        buf.sprintf("%s:Damage more than 1000 points :%d", ch.name, dam)
        bug(buf)
        if (ch.pcdata == null && ch.pcdata != null) {
            dam = 1000
        }

        /*
 *  For a 100-leveled MUD?....

    dam = 1000;
    if (!IS_IMMORTAL(ch))
    {
        OBJ_DATA obj;
        obj = get_wield_char( ch );
        send_to_char("You really shouldn't cheat.\n",ch);
        if (obj)
          extract_obj(obj);
    }
*/
    }


    val v_master = victim.master
    val v_pcdata = victim.pcdata
    if (victim != ch) {
        /* Certain attacks are forbidden.  Most other attacks are returned. */
        if (is_safe(ch, victim)) {
            return false
        }

        if (victim.position > Pos.Stunned) {
            if (victim.fighting == null) {
                set_fighting(victim, ch)
            }
            if (victim.timer <= 4) {
                victim.position = Pos.Fighting
            }
        }

        if (victim.position > Pos.Stunned) {
            if (ch.fighting == null) {
                set_fighting(ch, victim)
            }

            /*
                * If victim is charmed, ch might attack victim's master.
                */
            if (ch.pcdata == null
                    && v_pcdata == null
                    && IS_AFFECTED(victim, AFF_CHARM)
                    && v_master != null
                    && v_master.room == ch.room
                    && oneChanceOf(8)) {
                stop_fighting(ch, false)
                multi_hit(ch, v_master, null)
                return false
            }
        }

        /*
            * More charm and group stuff.
            */
        if (v_master == ch) {
            stop_follower(victim)
        }

        if (MOUNTED(victim) == ch || RIDDEN(victim) == ch) {
            ch.riding = false
            victim.riding = ch.riding
        }
    }

    /*
        * No one in combat can sneak, hide, or be invis or camoed.
        */
    if (IS_SET(ch.affected_by, AFF_HIDE)
            || IS_SET(ch.affected_by, AFF_INVISIBLE)
            || IS_SET(ch.affected_by, AFF_SNEAK)
            || IS_SET(ch.affected_by, AFF_FADE)
            || IS_SET(ch.affected_by, AFF_CAMOUFLAGE)
            || IS_SET(ch.affected_by, AFF_IMP_INVIS)
            || IS_AFFECTED(ch, AFF_EARTHFADE)) {
        do_visible(ch)
    }

    /*
        * Damage modifiers.
        */
    if (IS_AFFECTED(victim, AFF_SANCTUARY) && !(dt == Skill.cleave && randomPercent() < 50)) {
        dam /= 2
    } else if (IS_AFFECTED(victim, AFF_PROTECTOR)) {
        dam = 3 * dam / 5
    }

    if (IS_AFFECTED(victim, AFF_PROTECT_EVIL) && ch.isEvil()) {
        dam -= dam / 4
    }

    if (IS_AFFECTED(victim, AFF_PROTECT_GOOD) && ch.isGood()) {
        dam -= dam / 4
    }

    if (IS_AFFECTED(victim, AFF_AURA_CHAOS)
            && victim.cabal == Clan.Chaos
            && v_pcdata != null && IS_SET(victim.act, PLR_WANTED)) {
        dam -= dam / 4
    }

    if (is_affected(victim, Skill.protection_heat) && dam_type == DT.Fire) {
        dam -= dam / 4
    }

    if (is_affected(victim, Skill.protection_cold) && dam_type == DT.Cold) {
        dam -= dam / 4
    }

    var immune = false

    if (dt != null && dt.ordinal < Skill.x_hit.ordinal) {
        if (IS_AFFECTED(victim, AFF_ABSORB)
                && dt.target == TAR_CHAR_OFFENSIVE
                && dt.isSpell
                && ch != victim
                && randomPercent() < 2 * get_skill(victim, Skill.absorb) / 3
                /* update.c damages */
                && dt != Skill.poison
                && dt != Skill.plague
                && dt != Skill.witch_curse
                /* update.c damages */
                && dt != Skill.mental_knife
                && dt != Skill.lightning_breath) {
            act("Your spell fails to pass \$N's energy field!", ch, null, victim, TO_CHAR)
            act("You absorb \$n's spell!", ch, null, victim, TO_VICT)
            act("\$N absorbs \$n's spell!", ch, null, victim, TO_NOTVICT)
            check_improve(victim, Skill.absorb, true, 1)
            victim.mana += dt.min_mana
            return false
        }
        if (IS_AFFECTED(victim, AFF_SPELLBANE)
                && dt.target != TAR_IGNORE
                && dt.isSpell
                && randomPercent() < 2 * get_skill(victim, Skill.spellbane) / 3
                /* update.c damages */
                && dt != Skill.poison
                && dt != Skill.plague
                && dt != Skill.witch_curse
                /* spellbane passing spell damages */
                && dt != Skill.mental_knife
                && dt != Skill.lightning_breath) {
            act("\$N deflects your spell!", ch, null, victim, TO_CHAR)
            act("You deflect \$n's spell!", ch, null, victim, TO_VICT)
            act("\$N deflects \$n's spell!", ch, null, victim, TO_NOTVICT)
            check_improve(victim, Skill.spellbane, true, 1)
            damage(victim, ch, 3 * victim.level, Skill.spellbane, DT.Negative, true)
            return false
        }
    }

    /*
        * Check for parry, and dodge.
        */
    if (dt == Skill.x_hit && ch != victim) {
        when {
            is_affected(victim, Skill.mirror) -> {
                act("\$n shatters into tiny fragments of glass.", victim, null, null, TO_ROOM)
                extract_char(victim, true)
                return false
            }
            check_parry(ch, victim) -> return false
            check_cross(ch, victim) -> return false
            check_block(ch, victim) -> return false
            check_dodge(ch, victim) -> return false
            check_hand(ch, victim) -> return false
            check_blink(ch, victim) -> return false
            else -> {
            }
        }
    }

    when (check_immune(victim, dam_type)) {
        IS_IMMUNE -> {
            immune = true
            dam = 0
        }
        IS_RESISTANT -> dam -= dam / 3
        IS_VULNERABLE -> dam += dam / 2
    }

    if (dt == Skill.x_hit && ch != victim) {
        dam = critical_strike(ch, victim, dam)
        dam = ground_strike(ch, victim, dam)
    }

    if (show) {
        dam_message(ch, victim, dam, dt, immune, dam_type)
    }

    if (dam == 0) {
        return false
    }

    /* temporarily second wield doesn't inflict damage */

    if (dt == Skill.x_hit && ch != victim) {
        check_weapon_destroy(ch, victim, false)
    }

    /*
        * Hurt the victim.
        * Inform the victim of his new state.
        * make sure that negative overflow doesn't happen!
        */
    if (dam < 0 || dam > victim.hit + 16) {
        victim.hit = -16
    } else {
        victim.hit -= dam
    }

    if (v_pcdata != null && victim.level >= LEVEL_IMMORTAL && victim.hit < 1) {
        victim.hit = 1
    }

    update_pos(victim)

    when {
        victim.position == Pos.Mortal -> {
            if (dam_type != DT.Hunger && dam_type != DT.Thirst) {
                act("\$n is mortally wounded, and will die soon, if not aided.", victim, null, null, TO_ROOM)
                send_to_char("You are mortally wounded, and will die soon, if not aided.\n", victim)
            }
        }
        victim.position == Pos.Incap -> {
            if (dam_type != DT.Hunger && dam_type != DT.Thirst) {
                act("\$n is incapacitated and will slowly die, if not aided.", victim, null, null, TO_ROOM)
                send_to_char("You are incapacitated and will slowly die, if not aided.\n", victim)
            }
        }
        victim.position == Pos.Stunned -> {
            if (dam_type != DT.Hunger && dam_type != DT.Thirst) {
                act("\$n is stunned, but will probably recover.", victim, null, null, TO_ROOM)
                send_to_char("You are stunned, but will probably recover.\n", victim)
            }
        }
        victim.position == Pos.Dead -> {
            act("\$n is DEAD!!", victim, null, null, TO_ROOM)
            send_to_char("You have been KILLED!!\r\n\n", victim)
        }
        else -> {
            if (dam_type != DT.Hunger && dam_type != DT.Thirst) {
                if (dam > victim.max_hit / 4) {
                    send_to_char("That really did HURT!\n", victim)
                }
                if (victim.hit < victim.max_hit / 4) {
                    send_to_char("You sure are BLEEDING!\n", victim)
                }
            }
        }
    }

    /*
        * Sleep spells and extremely wounded folks.
        */
    if (!IS_AWAKE(victim)) {
        stop_fighting(victim, false)
    }

    /* Payoff for killing things.*/
    if (victim.position == Pos.Dead) {
        group_gain(ch, victim)

        if (v_pcdata != null) {
            /*  Dying penalty:  2/3 way back. */
            if (victim == ch || ch.pcdata == null && ch.master == null && ch.leader == null || IS_SET(victim.act, PLR_WANTED)) {
                if (victim.exp > exp_per_level(victim) * victim.level) {
                    val lost_exp = 2 * (exp_per_level(victim) * victim.level - victim.exp) / 3 + 50
                    gain_exp(victim, lost_exp)
                }
            }

            /*  Die too much and deleted ... :( */
            if (victim == ch || ch.pcdata == null && ch.master == null && ch.leader == null || IS_SET(victim.act, PLR_WANTED)) {
                v_pcdata.death++
                if (victim.clazz === Clazz.SAMURAI) {
                    if (v_pcdata.death % 3 == 2) {
                        victim.perm_stat[PrimeStat.Charisma]--
                        if (v_pcdata.death > 10) {

                            send_to_char("You became a ghost permanently and leave the earth realm.\n", victim)
                            act("\$n is dead, and will not rise again.\n", victim, null, null, TO_ROOM)
                            victim.last_fight_time = -1
                            victim.hit = 1
                            victim.position = Pos.Standing
                            val strsave = nw_config.lib_player_dir + "/" + capitalize(victim.name)
                            wiznet("\$N is deleted due to 10 deaths limit of Samurai.", ch, null, null, null, 0)
                            do_quit(victim)

                            File(strsave).delete()
                            return true
                        }
                    }
                } else if (v_pcdata.death % 3 == 2) {
                    victim.perm_stat[PrimeStat.Constitution]--
                    if (victim.perm_stat[PrimeStat.Constitution] < 3) {
                        send_to_char("You became a ghost permanently and leave the earth realm.\n", victim)
                        act("\$n is dead, and will not rise again.\n", victim, null, null, TO_ROOM)
                        victim.last_fight_time = -1
                        victim.hit = 1
                        victim.position = Pos.Standing
                        val strsave = nw_config.lib_player_dir + "/" + capitalize(victim.name)
                        wiznet("\$N is deleted due to lack of CON.", ch, null, null, null, 0)
                        do_quit(victim)

                        File(strsave).delete()
                        return true
                    } else {
                        send_to_char("You feel your life power has decreased with this death.\n", victim)
                    }
                }
            }
        }

        raw_kill(victim)

        /* don't remember killed victims anymore */

        if (ch.pcdata == null) {
            if (ch.pIndexData.vnum == MOB_VNUM_STALKER) {
                ch.status = 10
            }
            remove_mind(ch, victim.name)
            if (IS_SET(ch.act, ACT_HUNTER) && ch.hunting == victim) {
                ch.hunting = null
                ch.act = REMOVE_BIT(ch.act, ACT_HUNTER)
            }
        }

        /* RT new auto commands */

        if (ch.pcdata != null && v_pcdata == null) {
            val corpse = get_obj_list(ch, "corpse", ch.room.objects)

            if (IS_SET(ch.act, PLR_AUTO_LOOT) && corpse != null && corpse.contains.isNotEmpty())
            /* exists and not empty */ {
                do_get(ch, "all corpse")
            }

            if (IS_SET(ch.act, PLR_AUTO_GOLD) && corpse != null && corpse.contains.isNotEmpty() && !IS_SET(ch.act, PLR_AUTO_LOOT))
            /* exists and not empty */ {
                do_get(ch, "gold corpse")
            }
            if (ch.clazz === Clazz.VAMPIRE && ch.level > 10 && corpse != null) {
                act("{R\$n suck blood from \$N's corpse!!{x", ch, null, victim, TO_ROOM, Pos.Sleeping)
                send_to_char("{RYou suck blood from the corpse!!{x\n", ch)
                gain_condition(ch, COND_BLOODLUST, 3)
            }

            if (IS_SET(ch.act, PLR_AUTO_SAC)) {
                if (IS_SET(ch.act, PLR_AUTO_LOOT) && corpse != null && corpse.contains.isNotEmpty()) {
                    return true  /* leave if corpse has treasure */
                } else {
                    do_sacrifice(ch, "corpse")
                }
            }
        }

        return true
    }

    if (victim == ch) {
        return true
    }

    /*
        * Take care of link dead people.
        */
    if (v_pcdata != null && get_pc(victim) == null) {
        if (number_range(0, victim.wait) == 0) {
            if (victim.level < 11) {
                do_recall(victim)
            } else {
                do_flee(victim)
            }
            return true
        }
    }

    /*
        * Wimp out?
        */
    if (v_pcdata == null && dam > 0 && victim.wait < PULSE_VIOLENCE / 2) {
        if (IS_SET(victim.act, ACT_WIMPY) && oneChanceOf(4)
                && victim.hit < victim.max_hit / 5 || IS_AFFECTED(victim, AFF_CHARM) && v_master != null
                && v_master.room != victim.room || IS_AFFECTED(victim, AFF_FEAR) && !IS_SET(victim.act, ACT_NOTRACK)) {
            do_flee(victim)
            victim.last_fought = null
        }
    }

    if (v_pcdata != null && victim.hit > 0 && (victim.hit <= victim.wimpy
            || IS_AFFECTED(victim, AFF_FEAR)) && victim.wait < PULSE_VIOLENCE / 2) {
        do_flee(victim)
    }

    return true
}

fun is_safe(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (!is_safe_nomessage(ch, victim)) {
        return false
    }
    act("The gods protect \$N.", ch, null, victim, TO_CHAR)
    act("The gods protect \$N from \$n.", ch, null, victim, TO_ROOM)
    return true
}


fun is_safe_nomessage(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (victim.fighting == ch || ch == victim) {
        return false
    }

    /* Ghosts are safe */
    if (victim.pcdata != null && IS_SET(victim.act, PLR_GHOST) || ch.pcdata != null && IS_SET(ch.act, PLR_GHOST)) {
        return true
    }

    /* link dead players whose adrenalin is not gushing are safe */
    if (victim.pcdata != null && (victim.last_fight_time == -1L || current_time - victim.last_fight_time > FIGHT_DELAY_TIME) &&
            get_pc(victim) == null) {
        return true
    }

    if (ch.pcdata != null && victim.pcdata != null && victim.level < 5 || ch.pcdata != null && victim.pcdata != null && ch.level < 5) {
        return true
    }

    /* newly death staff */
    return if (!IS_IMMORTAL(ch) && victim.pcdata != null &&
            (ch.last_death_time != -1L && current_time - ch.last_death_time < 600 || victim.last_death_time != -1L && current_time - victim.last_death_time < 600)) {
        true
    } else !IS_IMMORTAL(ch) && ch.pcdata != null && victim.pcdata != null &&
            (ch.level >= victim.level + Math.max(4, ch.level / 10 + 2) || ch.level <= victim.level - Math.max(4, ch.level / 10 + 2)) &&
            (victim.level >= ch.level + Math.max(4, victim.level / 10 + 2) || victim.level <= ch.level - Math.max(4, victim.level / 10 + 2))

    /* level adjustement */

}


fun is_safe_spell(ch: CHAR_DATA, victim: CHAR_DATA, area: Boolean) = when {
    ch == victim && !area -> true
    IS_IMMORTAL(victim) && area -> true
    is_same_group(ch, victim) && area -> true
    ch == victim && ch.room.sector_type == SECT_INSIDE -> true
    (RIDDEN(ch) == victim || MOUNTED(ch) == victim) && area -> true
    else -> is_safe(ch, victim)
}

/*
* Check for parry.
*/

fun check_parry(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (!IS_AWAKE(victim)) {
        return false
    }

    if (get_wield_char(victim, false) == null) {
        return false
    }

    var chance: Int
    if (victim.pcdata == null) {
        chance = Math.min(40, victim.level)
    } else {
        chance = get_skill(victim, Skill.parry) / 2
        if (victim.clazz === Clazz.WARRIOR || victim.clazz === Clazz.SAMURAI) {
            chance = (chance * 1.2).toInt()
        }
    }


    if (randomPercent() >= chance + victim.level - ch.level) {
        return false
    }

    act("You parry \$n's attack.", ch, null, victim, TO_VICT)
    act("\$N parries your attack.", ch, null, victim, TO_CHAR)
    check_weapon_destroyed(ch, victim, false)
    if (randomPercent() > get_skill(victim, Skill.parry)) {
        /* size  and weight */
        chance += ch.carry_weight / 25
        chance -= victim.carry_weight / 20

        val dSize = ch.size - victim.size
        chance += if (dSize < 0) dSize * 25 else dSize * 10

        /* stats */
        chance += ch.curr_stat(PrimeStat.Strength)
        chance -= victim.curr_stat(PrimeStat.Dexterity) * 4 / 3

        if (IS_AFFECTED(ch, AFF_FLYING)) {
            chance -= 10
        }

        /* speed */
        if (IS_SET(ch.off_flags, OFF_FAST)) {
            chance += 10
        }
        if (IS_SET(victim.off_flags, OFF_FAST)) {
            chance -= 20
        }

        /* level */
        chance += (ch.level - victim.level) * 2

        /* now the attack */
        if (randomPercent() < chance / 20) {
            act("You couldn't manage to keep your position!", ch, null, victim, TO_VICT)
            act("You fall down!", ch, null, victim, TO_VICT)
            act("\$N couldn't manage to hold your attack and falls down!", ch, null, victim, TO_CHAR)
            act("\$n stunning force makes \$N falling down.", ch, null, victim, TO_NOTVICT)

            WAIT_STATE(victim, Skill.bash.beats)
            victim.position = Pos.Resting
        }
    }
    check_improve(victim, Skill.parry, true, 6)
    return true
}

/*
 * check blink
 */

fun check_blink(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (!IS_BLINK_ON(victim)) {
        return false
    }

    val pcdata = victim.pcdata ?: return false

    val chance = pcdata.learned[Skill.blink.ordinal] / 2

    if (randomPercent() >= chance + victim.level - ch.level || randomPercent() < 50 || victim.mana < 10) {
        return false
    }

    victim.mana -= Math.max(victim.level / 10, 1)

    act("You blink out \$n's attack.", ch, null, victim, TO_VICT)
    act("\$N blinks out your attack.", ch, null, victim, TO_CHAR)
    check_improve(victim, Skill.blink, true, 6)
    return true
}

/** Check for shield block. */

fun check_block(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (!IS_AWAKE(victim)) {
        return false
    }

    if (get_shield_char(victim) == null) {
        return false
    }

    var chance: Int
    if (victim.pcdata == null) {
        chance = 10
    } else {
        if (get_skill(victim, Skill.shield_block) <= 1) {
            return false
        }
        chance = get_skill(victim, Skill.shield_block) / 2
        chance -= if (victim.clazz === Clazz.WARRIOR) 0 else 10
    }

    if (randomPercent() >= chance + victim.level - ch.level) {
        return false
    }

    act("Your shield blocks \$n's attack.", ch, null, victim, TO_VICT)
    act("\$N deflects your attack with \$S shield.", ch, null, victim, TO_CHAR)
    check_shield_destroyed(ch, victim, false)
    check_improve(victim, Skill.shield_block, true, 6)
    return true
}

/*
* Check for dodge.
*/

fun check_dodge(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (!IS_AWAKE(victim)) {
        return false
    }

    if (MOUNTED(victim) != null) {
        return false
    }

    var chance: Int
    if (victim.pcdata == null) {
        chance = Math.min(30, victim.level)
    } else {
        chance = get_skill(victim, Skill.dodge) / 2
        /* chance for high dex. */
        chance += 2 * (victim.curr_stat(PrimeStat.Dexterity) - 20)
        if (victim.clazz === Clazz.WARRIOR || victim.clazz === Clazz.SAMURAI) {
            chance = (chance * 1.2).toInt()
        } else if (victim.clazz === Clazz.THIEF || victim.clazz === Clazz.NINJA) {
            chance = (chance * 1.1).toInt()
        }
    }

    if (randomPercent() >= chance + (victim.level - ch.level) / 2) {
        return false
    }

    act("You dodge \$n's attack.", ch, null, victim, TO_VICT)
    act("\$N dodges your attack.", ch, null, victim, TO_CHAR)
    if (randomPercent() < get_skill(victim, Skill.dodge) / 20 && !(IS_AFFECTED(ch, AFF_FLYING) || ch.position < Pos.Fighting)) {
        /* size */
        if (victim.size < ch.size) {
            chance += (victim.size - ch.size) * 10  /* bigger = harder to trip */
        }

        /* dex */
        chance += victim.curr_stat(PrimeStat.Dexterity)
        chance -= ch.curr_stat(PrimeStat.Dexterity) * 3 / 2

        if (IS_AFFECTED(victim, AFF_FLYING)) {
            chance -= 10
        }

        /* speed */
        if (IS_SET(victim.off_flags, OFF_FAST) || IS_AFFECTED(victim, AFF_HASTE)) {
            chance += 10
        }
        if (IS_SET(ch.off_flags, OFF_FAST) || IS_AFFECTED(ch, AFF_HASTE)) {
            chance -= 20
        }

        /* level */
        chance += (victim.level - ch.level) * 2

        /* now the attack */
        if (randomPercent() < chance / 20) {
            act("\$n lost his postion and fall down!", ch, null, victim, TO_VICT)
            act("As \$N moves you lost your position fall down!", ch, null, victim, TO_CHAR)
            act("As \$N dodges \$N's attack ,\$N lost his position and falls down.", ch, null, victim, TO_NOTVICT)

            WAIT_STATE(ch, Skill.trip.beats)
            ch.position = Pos.Resting
        }
    }
    check_improve(victim, Skill.dodge, true, 6)
    return true
}

/*
* Check for cross.
*/

fun check_cross(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (!IS_AWAKE(victim)) {
        return false
    }

    if (get_wield_char(victim, false) == null || get_wield_char(victim, true) == null) {
        return false
    }

    var chance: Int
    if (victim.pcdata == null) {
        chance = Math.min(35, victim.level)
    } else {
        chance = get_skill(victim, Skill.cross_block) / 3
        if (victim.clazz === Clazz.WARRIOR || victim.clazz === Clazz.SAMURAI) {
            chance = (chance * 1.2).toInt()
        }
    }


    if (randomPercent() >= chance + victim.level - ch.level) {
        return false
    }

    act("Your cross blocks \$n's attack.", ch, null, victim, TO_VICT)
    act("\$N's cross blocks your attack.", ch, null, victim, TO_CHAR)
    check_weapon_destroyed(ch, victim, false)
    if (randomPercent() > get_skill(victim, Skill.cross_block)) {
        /* size  and weight */
        chance += ch.carry_weight / 25
        chance -= victim.carry_weight / 10

        val dSize = ch.size - victim.size
        chance += if (dSize < 0) dSize * 25 else dSize * 10

        /* stats */
        chance += ch.curr_stat(PrimeStat.Strength)
        chance -= victim.curr_stat(PrimeStat.Dexterity) * 5 / 3

        if (IS_AFFECTED(ch, AFF_FLYING)) {
            chance -= 20
        }

        /* speed */
        if (IS_SET(ch.off_flags, OFF_FAST)) {
            chance += 10
        }
        if (IS_SET(victim.off_flags, OFF_FAST)) {
            chance -= 20
        }

        /* level */
        chance += (ch.level - victim.level) * 2

        /* now the attack */
        if (randomPercent() < chance / 20) {
            act("You couldn't manage to keep your position!", ch, null, victim, TO_VICT)
            act("You fall down!", ch, null, victim, TO_VICT)
            act("\$N couldn't manage to hold your attack and falls down!", ch, null, victim, TO_CHAR)
            act("\$n stunning force makes \$N falling down.", ch, null, victim, TO_NOTVICT)

            WAIT_STATE(victim, Skill.bash.beats)
            victim.position = Pos.Resting
        }
    }
    check_improve(victim, Skill.cross_block, true, 6)
    return true
}

/** Check for hand.*/
fun check_hand(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (!IS_AWAKE(victim)) {
        return false
    }

    if (get_wield_char(victim, false) != null) {
        return false
    }

    var chance: Int
    if (victim.pcdata == null) {
        chance = Math.min(35, victim.level)
    } else {
        chance = get_skill(victim, Skill.hand_block) / 3
        if (victim.clazz === Clazz.NINJA) chance += chance / 2
    }


    if (randomPercent() >= chance + victim.level - ch.level) {
        return false
    }

    act("Your hands block \$n's attack.", ch, null, victim, TO_VICT)
    act("\$N's hands block your attack.", ch, null, victim, TO_CHAR)
    check_improve(victim, Skill.hand_block, true, 6)
    return true
}

/** Set position of a victim. */
fun update_pos(victim: CHAR_DATA) {
    victim.position = when {
        victim.hit > 0 -> if (victim.position <= Pos.Stunned) Pos.Standing else victim.position
        victim.pcdata == null -> Pos.Dead
        victim.hit <= -11 -> Pos.Dead
        victim.hit <= -6 -> Pos.Mortal
        victim.hit <= -3 -> Pos.Incap
        else -> Pos.Stunned
    }
}

/**
 * Starts fights.
 */
fun set_fighting(ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.fighting != null) {
        bug("Set_fighting: already fighting")
        return
    }

    if (IS_AFFECTED(ch, AFF_SLEEP)) {
        affect_strip(ch, Skill.sleep)
    }

    ch.fighting = victim
    ch.position = Pos.Fighting

}

/** Stop fights. */

fun stop_fighting(ch: CHAR_DATA, fBoth: Boolean) {
    for (fch in Index.CHARS) {
        if (fch == ch || fBoth && fch.fighting == ch) {
            fch.fighting = null
            fch.position = if (fch.pcdata == null) ch.default_pos else Pos.Standing
            update_pos(fch)
        }
    }
}

/** Make a corpse out of a character. */

fun make_corpse(ch: CHAR_DATA) {
    val name: String
    val corpse: Obj
    if (ch.pcdata == null) {
        name = ch.short_desc
        corpse = create_object(get_obj_index_nn(OBJ_VNUM_CORPSE_NPC), 0)
        corpse.timer = number_range(3, 6)
        if (ch.gold > 0 || ch.silver > 0) {
            if (IS_SET(ch.form, FORM_INSTANT_DECAY)) {
                obj_to_room(create_money(ch.gold, ch.silver), ch.room)
            } else {
                obj_to_obj(create_money(ch.gold, ch.silver), corpse)
            }
            ch.gold = 0
        }
        corpse.from = ch.short_desc
        corpse.cost = 0
    } else {
        val align = when {
            ch.isGood() -> 0
            ch.isEvil() -> 2
            else -> 1
        }

        name = ch.name
        corpse = create_object(get_obj_index_nn(OBJ_VNUM_CORPSE_PC), 0)
        corpse.timer = number_range(25, 40)
        ch.act = REMOVE_BIT(ch.act, PLR_CANLOOT)
        corpse.owner = ch.name
        corpse.from = ch.name
        corpse.altar = ch.hometown.altar[align]
        corpse.pit = ch.hometown.pit[align]

        if (ch.gold > 0 || ch.silver > 0) {
            obj_to_obj(create_money(ch.gold, ch.silver), corpse)
            ch.gold = 0
            ch.silver = 0
        }
        corpse.cost = 0
    }

    corpse.level = ch.level

    val buf = TextBuffer()
    buf.sprintf(corpse.short_desc, name)
    corpse.short_desc = buf.toString()

    buf.sprintf(corpse.description, name)
    corpse.description = buf.toString()

    for (obj in ch.carrying) {
        obj_from_char(obj)
        if (obj.item_type == ItemType.Potion) {
            obj.timer = number_range(500, 1000)
        }
        if (obj.item_type == ItemType.Scroll) {
            obj.timer = number_range(1000, 2500)
        }
        if (IS_SET(obj.extra_flags, ITEM_ROT_DEATH)) {
            obj.timer = number_range(5, 10)
            if (obj.item_type == ItemType.Potion) {
                obj.timer += obj.level * 20
            }
        }
        obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_VIS_DEATH)
        obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_ROT_DEATH)

        when {
            IS_SET(obj.extra_flags, ITEM_INVENTORY) || obj.pIndexData.limit != -1
                    && obj.pIndexData.count > obj.pIndexData.limit -> extract_obj(obj)
            IS_SET(ch.form, FORM_INSTANT_DECAY) -> obj_to_room(obj, ch.room)
            else -> obj_to_obj(obj, corpse)
        }
    }

    obj_to_room(corpse, ch.room)
}

/** Improved Death_cry contributed by Diavolo. */
fun death_cry_org(ch: CHAR_DATA, partArg: Int) {
    var part = partArg
    var msg = "You hear \$n's death cry."
    if (part == -1) {
        part = random(16)
    }
    var vnum = 0
    when (part) {
        0 -> msg = "\$n hits the ground ... DEAD."
        1 -> {
            if (ch.material == null) {
                msg = "\$n splatters blood on your armor."
            } else if (IS_SET(ch.parts, PART_GUTS)) {
                msg = "\$n spills \$s guts all over the floor."
                vnum = OBJ_VNUM_GUTS
            }
        }
        2 -> if (IS_SET(ch.parts, PART_GUTS)) {
            msg = "\$n spills \$s guts all over the floor."
            vnum = OBJ_VNUM_GUTS
        }
        3 -> if (IS_SET(ch.parts, PART_HEAD)) {
            msg = "\$n's severed head plops on the ground."
            vnum = OBJ_VNUM_SEVERED_HEAD
        }
        4 -> if (IS_SET(ch.parts, PART_HEART)) {
            msg = "\$n's heart is torn from \$s chest."
            vnum = OBJ_VNUM_TORN_HEART
        }
        5 -> if (IS_SET(ch.parts, PART_ARMS)) {
            msg = "\$n's arm is sliced from \$s dead body."
            vnum = OBJ_VNUM_SLICED_ARM
        }
        6 -> if (IS_SET(ch.parts, PART_LEGS)) {
            msg = "\$n's leg is sliced from \$s dead body."
            vnum = OBJ_VNUM_SLICED_LEG
        }
        7 -> if (IS_SET(ch.parts, PART_BRAINS)) {
            msg = "\$n's head is shattered, and \$s brains splash all over you."
            vnum = OBJ_VNUM_BRAINS
        }
    }

    act(msg, ch, null, null, TO_ROOM)

    if (vnum != 0) {
        val name = if (ch.pcdata == null) ch.short_desc else ch.name
        val obj = create_object(get_obj_index_nn(vnum), 0)
        obj.timer = number_range(4, 7)

        val buf = TextBuffer()
        buf.sprintf(obj.short_desc, name)
        obj.short_desc = buf.toString()

        buf.sprintf(obj.description, name)
        obj.description = buf.toString()

        obj.from = name

        if (obj.item_type == ItemType.Food) {
            if (IS_SET(ch.form, FORM_POISON)) {
                obj.value[3] = 1
            } else if (!IS_SET(ch.form, FORM_EDIBLE)) {
                obj.item_type = ItemType.Trash
            }
        }

        obj_to_room(obj, ch.room)
    }

    msg = if (ch.pcdata == null) "You hear something's death cry." else "You hear someone's death cry."

    val was_in_room = ch.room
    for (door in 0..5) {
        val pexit = was_in_room.exit[door]
        if (pexit != null && pexit.to_room != was_in_room) {
            ch.room = pexit.to_room
            act(msg, ch, null, null, TO_ROOM)
        }
    }
    ch.room = was_in_room

}

fun raw_kill(victim: CHAR_DATA) = raw_kill_org(victim, -1)

fun raw_kill_org(victim: CHAR_DATA, part: Int) {
    stop_fighting(victim, true)

    for (obj in victim.carrying) {
        if (IS_SET(obj.progtypes, OPROG_DEATH) && obj.wear_loc.isOn()) {
            if (obj.pIndexData.oprogs.death_prog!!(obj, victim)) {
                victim.position = Pos.Standing
                return
            }
        }
    }
    victim.last_fight_time = -1
    if (IS_SET(victim.progtypes, MPROG_DEATH)) {
        if (victim.pIndexData.mprogs.death_prog!!(victim)) {
            victim.position = Pos.Standing
            return
        }
    }

    victim.last_death_time = current_time

    val tattoo = get_eq_char(victim, Wear.Tattoo)
    if (tattoo != null) {
        obj_from_char(tattoo)
    }

    death_cry_org(victim, part)
    make_corpse(victim)

    val v_pcdata = victim.pcdata
    if (v_pcdata == null) {
        victim.pIndexData.killed++
        kill_table[URANGE(0, victim.level, MAX_LEVEL - 1)].killed++
        extract_char(victim, true)
        return
    }

    send_to_char("You turn into an invincible ghost for a few minutes.\n", victim)
    send_to_char("As long as you don't attack anything.\n", victim)

    extract_char(victim, false)

    while (victim.affected != null) {
        affect_remove(victim, victim.affected!!)
    }
    victim.affected_by = 0
    victim.armor.fill(100)
    victim.position = Pos.Resting
    victim.hit = victim.max_hit / 10
    victim.mana = victim.max_mana / 10
    victim.move = victim.max_move

    /* RT added to prevent infinite deaths */
    victim.act = REMOVE_BIT(victim.act, PLR_WANTED)
    victim.act = REMOVE_BIT(victim.act, PLR_BOUGHT_PET)
    /*  SET_BIT(victim.act, PLR_GHOST);    */

    v_pcdata.condition[COND_THIRST] = 40
    v_pcdata.condition[COND_HUNGER] = 40
    v_pcdata.condition[COND_FULL] = 40
    v_pcdata.condition[COND_BLOODLUST] = 40
    v_pcdata.condition[COND_DESIRE] = 40

    if (tattoo != null) {
        obj_to_char(tattoo, victim)
        equip_char(victim, tattoo, Wear.Tattoo)
    }
    save_char_obj(victim)

    // Calm down the tracking mobiles
    Index.CHARS
            .filter { it.last_fought == victim }
            .forEach { it.last_fought = null }
}


fun group_gain(ch: CHAR_DATA, victim: CHAR_DATA) {
    val v_pcdata = victim.pcdata
    if (victim == ch || v_pcdata == null && victim.pIndexData.vnum < 100) {
        return
    }

    /* quest */
    val master = ch.master
    val gch = if (IS_GOLEM(ch) && master != null && master.clazz === Clazz.NECROMANCER) master else ch
    val gch_pcdata = gch.pcdata
    if (gch_pcdata != null && IS_QUESTOR(gch) && v_pcdata == null) {
        if (gch_pcdata.questmob == victim.pIndexData.vnum) {
            send_to_char("You have almost completed your QUEST!\n", gch)
            send_to_char("Return to questmaster before your time runs out!\n", gch)
            gch_pcdata.questmob = -1
        }
    }
    /* end quest */

    if (v_pcdata != null) {
        return
    }

    if (victim.master != null || victim.leader != null) {
        return
    }

    var members = 1
    var group_levels = 0
    for (gch2 in ch.room.people) {
        if (is_same_group(gch2, ch)) {
            if (gch_pcdata != null && gch2 != ch) {
                members++
            }
            group_levels += gch2.level
        }
    }

    val lch = ch.leader ?: ch

    for (gch2 in ch.room.people) {
        if (!is_same_group(gch2, ch) || gch_pcdata == null) {
            continue
        }


        if (gch2.level - lch.level > 8) {
            send_to_char("You are too high for this group.\n", gch2)
            continue
        }

        if (gch2.level - lch.level < -8) {
            send_to_char("You are too low for this group.\n", gch2)
            continue
        }


        val xp = xp_compute(gch2, victim, group_levels, members)
        val buf = TextBuffer()
        buf.sprintf("You receive %d experience points.\n", xp)
        send_to_char(buf, gch2)
        gain_exp(gch2, xp)

        for (obj in ch.carrying) {
            if (obj.wear_loc.isNone()) {
                continue
            }

            if (IS_OBJ_STAT(obj, ITEM_ANTI_EVIL) && ch.isEvil()
                    || IS_OBJ_STAT(obj, ITEM_ANTI_GOOD) && ch.isGood()
                    || IS_OBJ_STAT(obj, ITEM_ANTI_NEUTRAL) && ch.isNeutralA()) {
                act("You are zapped by \$p.", ch, obj, null, TO_CHAR)
                act("\$n is zapped by \$p.", ch, obj, null, TO_ROOM)
                obj_from_char(obj)
                obj_to_room(obj, ch.room)
            }
        }
    }
}

/*
* Compute xp for a kill.
* Also adjust alignment of killer.
* Edit this function to change xp computations.
*/
fun xp_compute(ch: CHAR_DATA, victim: CHAR_DATA, total_levels: Int, members: Int): Int {
    val pcdata = ch.pcdata ?: return 0
    val level_range = victim.level - ch.level

    var base_exp = when (level_range) {
        -9 -> 1
        -8 -> 2
        -7 -> 5
        -6 -> 9
        -5 -> 11
        -4 -> 22
        -3 -> 33
        -2 -> 43
        -1 -> 60
        0 -> 74
        1 -> 84
        2 -> 99
        3 -> 121
        4 -> 143
        else -> 0
    }


    if (level_range > 4) {
        base_exp = 140 + 20 * (level_range - 4)
    }

    /* calculate exp multiplier */
    var xp = when {
        IS_SET(victim.act, ACT_NOALIGN) -> base_exp
        ch.isEvil() && victim.isGood() || victim.isEvil() && ch.isGood() -> base_exp * 8 / 5
        ch.isGood() && victim.isGood() -> 0
        !ch.isNeutralA() && victim.isNeutralA() -> (base_exp * 1.1).toInt()
        ch.isNeutralA() && !victim.isNeutralA() -> (base_exp * 1.3).toInt()
        else -> base_exp
    }/* alignment */

    /* more exp at the low levels */
    if (ch.level < 6) {
        xp = 15 * xp / (ch.level + 4)
    }

    /* randomize the rewards */
    xp = number_range(xp * 3 / 4, xp * 5 / 4)

    /* adjust for grouping */
    xp = xp * ch.level / total_levels

    if (members == 2 || members == 3) {
        xp *= 3 / 2
    }

    xp = when {
        ch.level < 15 -> Math.min(250 + dice(1, 25), xp)
        ch.level < 40 -> Math.min(225 + dice(1, 20), xp)
        ch.level < 60 -> Math.min(200 + dice(1, 20), xp)
        else -> Math.min(180 + dice(1, 20), xp)
    }

    xp += xp * (ch.max_hit - ch.hit) / (ch.max_hit * 5)

    var neg_cha = 0
    var pos_cha = 0

    if (ch.isGood()) {
        when (victim.align) {
            Align.Good -> {
                pcdata.anti_killed++
                neg_cha = 1
            }
            Align.Neutral -> {
                pcdata.has_killed++
                pos_cha = 1
            }
            Align.Evil -> {
                pcdata.has_killed++
                pos_cha = 1
            }
        }
    }

    if (ch.isNeutralA()) {
        if (xp > 0) {
            when (victim.align) {
                Align.Good -> {
                    pcdata.has_killed++
                    pos_cha = 1
                }
                Align.Neutral -> {
                    pcdata.anti_killed++
                    neg_cha = 1
                }
                Align.Evil -> {
                    pcdata.has_killed++
                    pos_cha = 1
                }
            }
        }
    }

    if (ch.isEvil()) {
        if (xp > 0) {
            when (victim.align) {
                Align.Good -> {
                    pcdata.has_killed++
                    pos_cha = 1
                }
                Align.Neutral -> {
                    pcdata.has_killed++
                    pos_cha = 1
                }
                Align.Evil -> {
                    pcdata.anti_killed++
                    neg_cha = 1
                }
            }
        }
    }

    if (neg_cha != 0) {
        if (pcdata.anti_killed % 100 == 99) {
            send_to_char("You have killed ${pcdata.anti_killed} ${ch.align.lcTitle}s up to now.\n", ch)
            if (ch.perm_stat[PrimeStat.Charisma] > 3 && ch.isGood()) {
                send_to_char("So your charisma has reduced by one.\n", ch)
                ch.perm_stat[PrimeStat.Charisma] -= 1
            }
        }
    } else if (pos_cha != 0) {
        if (pcdata.has_killed % 200 == 199) {
            val buf = TextBuffer()
            buf.sprintf("You have killed %d %s up to now.\n",
                    pcdata.anti_killed,
                    when (ch.align) {
                        Align.Good -> "anti-goods"
                        Align.Neutral -> "anti-neutrals"
                        Align.Evil -> "anti-evils"
                    })
            send_to_char(buf, ch)
            if (ch.perm_stat[PrimeStat.Charisma] < ch.max(PrimeStat.Charisma) && ch.isGood()) {
                send_to_char("So your charisma has increased by one.\n", ch)
                ch.perm_stat[PrimeStat.Charisma] += 1
            }
        }
    }
    return xp
}


private typealias  DP = Pair<String, String>
fun dam_message(ch: CHAR_DATA, victim: CHAR_DATA, dam: Int, dSkill: Skill?, immune: Boolean, dam_type: DT) {
    val (vp, vs) = when {
        dam == 0 -> DP("miss", "misses")
        dam <= 4 -> DP("{cscratch{x", "{cscratches{x")
        dam <= 8 -> DP("{cgraze{x", "{cgrazes{x")
        dam <= 12 -> DP("{chit{x", "{chits{x")
        dam <= 16 -> DP("{cinjure{x", "{cinjures{x")
        dam <= 20 -> DP("{cwound{x", "{cwounds{x")
        dam <= 24 -> DP("{cmaul{x", "{cmauls{x")
        dam <= 28 -> DP("{cdecimate{x", "{cdecimates{x")
        dam <= 32 -> DP("{cdevastate{x", "{cdevastates{x")
        dam <= 36 -> DP("{cmaim{x", "{cmaims{x")
        dam <= 42 -> DP("{mMUTILATE{x", "{mMUTILATES{x")
        dam <= 52 -> DP("{mDISEMBOWEL{x", "{mDISEMBOWELS{x")
        dam <= 65 -> DP("{mDISMEMBER{x", "{mDISMEMBERS{x")
        dam <= 80 -> DP("{mMASSACRE{x", "{mMASSACRES{x")
        dam <= 100 -> DP("{mMANGLE{x", "{mMANGLES{x")
        dam <= 130 -> DP("{y*** DEMOLISH ***{x", "{y*** DEMOLISHES ***{x")
        dam <= 175 -> DP("{y*** DEVASTATE ***{x", "{y*** DEVASTATES ***{x")
        dam <= 250 -> DP("{y=== OBLITERATE ==={x", "{y=== OBLITERATES ==={x")
        dam <= 325 -> DP("{y==== ATOMIZE ===={x", "{y==== ATOMIZES ===={x")
        dam <= 400 -> DP("{r<*> <*> ANNIHILATE <*> <*>{x", "{r<*> <*> ANNIHILATES <*> <*>{x")
        dam <= 500 -> DP("{r<*>!<*> ERADICATE <*>!<*>{x", "{r<*>!<*> ERADICATES <*>!<*>{x")
        dam <= 650 -> DP("{r<*><*><*> ELECTRONIZE <*><*><*>{x", "{r<*><*><*> ELECTRONIZES <*><*><*>{x")
        dam <= 800 -> DP("{r(<*>)!(<*>) SKELETONIZE (<*>)!(<*>){x", "{r(<*>)!(<*>) SKELETONIZES (<*>)!(<*>){x")
        dam <= 1000 -> DP("{r(*)!(*)!(*) NUKE (*)!(*)!(*){x", "{r(*)!(*)!(*) NUKES (*)!(*)!(*){x")
        dam <= 1250 -> DP("{r(*)!<*>!(*) TERMINATE (*)!<*>!(*){x", "{r(*)!<*>!(*) TERMINATES (*)!<*>!(*){x")
        dam <= 1500 -> DP("{r<*>!(*)!<*>> TEAR UP <<*)!(*)!<*>{x", "{r<*>!(*)!<*>> TEARS UP <<*)!(*)!<*>{x")
        else -> DP("{r=<*) (*>= ! POWER HIT ! =<*) (*>={*{x", "{r=<*) (*>= ! POWER HITS ! =<*) (*>={*{x")
    }

    val punct = when {
        victim.level < 20 -> if (dam <= 24) '.' else '!'
        victim.level < 50 -> if (dam <= 50) '.' else '!'
        else -> if (dam <= 75) '.' else '!'
    }

    val buf1 = TextBuffer()
    val buf2 = TextBuffer()
    val buf3 = TextBuffer()
    if (dSkill == Skill.x_hit || dSkill == Skill.x_hunger) {
        if (ch == victim) {
            when (dam_type) {
                DT.Hunger -> {
                    sprintf(buf1, "\$n's hunger %s \$mself%c", vp, punct)
                    sprintf(buf2, "Your hunger %s yourself%c", vs, punct)
                }
                DT.Thirst -> {
                    sprintf(buf1, "\$n's thirst %s \$mself%c", vp, punct)
                    sprintf(buf2, "Your thirst %s yourself%c", vs, punct)
                }
                DT.RoomLight -> {
                    sprintf(buf1, "The light of room %s \$n!%c", vp, punct)
                    sprintf(buf2, "The light of room %s you!%c", vs, punct)
                }
                DT.Trap -> {
                    sprintf(buf1, "The trap at room %s \$n!%c", vp, punct)
                    sprintf(buf2, "The trap at room %s you!%c", vs, punct)
                }
                else -> {
                    sprintf(buf1, "\$n %s \$mself%c", vp, punct)
                    sprintf(buf2, "You %s yourself%c", vs, punct)
                }
            }
        } else {
            sprintf(buf1, "\$n %s \$N%c", vp, punct)
            sprintf(buf2, "You %s \$N%c", vs, punct)
            sprintf(buf3, "\$n %s you%c", vp, punct)
        }
    } else {
        val attack = when {
            dSkill != null -> dSkill.noun_damage
            else -> {
                bug("Dam_message: bad dt %d.", dSkill)
                AttackType.None.noun
            }
        }

        if (immune) {
            if (ch === victim) {
                sprintf(buf1, "\$n is unaffected by \$s own %s.", attack)
                sprintf(buf2, "Luckily, you are immune to that.")
            } else {
                sprintf(buf1, "\$N is unaffected by \$n's %s!", attack)
                sprintf(buf2, "\$N is unaffected by your %s!", attack)
                sprintf(buf3, "\$n's %s is powerless against you.", attack)
            }
        } else {
            if (ch === victim) {
                sprintf(buf1, "\$n's %s %s \$m%c", attack, vp, punct)
                sprintf(buf2, "Your %s %s you%c", attack, vp, punct)
            } else {
                sprintf(buf1, "\$n's %s %s \$N%c", attack, vp, punct)
                sprintf(buf2, "Your %s %s \$N%c", attack, vp, punct)
                sprintf(buf3, "\$n's %s %s you%c", attack, vp, punct)
            }
        }
    }

    if (ch == victim) {
        act(buf1.toString(), ch, null, null, TO_ROOM, Pos.Resting)
        act(buf2.toString(), ch, null, null, TO_CHAR, Pos.Resting)
    } else {
        act(buf1.toString(), ch, null, victim, TO_NOTVICT, Pos.Resting)
        act(buf2.toString(), ch, null, victim, TO_CHAR, Pos.Resting)
        act(buf3.toString(), ch, null, victim, TO_VICT, Pos.Resting)
    }

}


fun do_kill(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Kill whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (ch.position == Pos.Fighting) {
        when {
            victim == ch.fighting -> send_to_char("You do the best you can!\n", ch)
            victim.fighting != ch -> send_to_char("One battle at a time, please.\n", ch)
            else -> {
                act("You start aiming at \$N.", ch, null, victim, TO_CHAR)
                ch.fighting = victim
            }
        }
        return
    }

    if (victim.pcdata != null) {
        send_to_char("You must MURDER a player.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("You hit yourself.  Ouch!\n", ch)
        multi_hit(ch, ch, null)
        return
    }


    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("\$N is your beloved master.", ch, null, victim, TO_CHAR)
        return
    }


    WAIT_STATE(ch, PULSE_VIOLENCE)

    val wield = get_wield_char(ch, false)
    if (skill_failure_nomessage(ch, Skill.mortal_strike, 0) == 0 && wield != null && wield.level > victim.level - 5) {
        var chance = 1 + get_skill(ch, Skill.mortal_strike) / 30
        chance += (ch.level - victim.level) / 2
        if (randomPercent() < chance) {
            act("{rYour flash strike instantly slays \$N!{x", ch, null, victim, TO_CHAR, Pos.Resting)
            act("{r\$n flash strike instantly slays \$N!{x", ch, null, victim, TO_NOTVICT, Pos.Resting)
            act("{r\$n flash strike instantly slays you!{x", ch, null, victim, TO_VICT, Pos.Dead)
            damage(ch, victim, victim.hit + 1, Skill.mortal_strike, DT.None, true)
            check_improve(ch, Skill.mortal_strike, true, 1)
            return
        } else {
            check_improve(ch, Skill.mortal_strike, false, 3)
        }
    }

    multi_hit(ch, victim, null)
}


fun do_murde(ch: CHAR_DATA) = send_to_char("If you want to MURDER, spell it out.\n", ch)


fun do_murder(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Murder whom?\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) || ch.pcdata == null && IS_SET(ch.act, ACT_PET)) {
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("Suicide is a mortal sin.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("\$N is your beloved master.", ch, null, victim, TO_CHAR)
        return
    }

    if (ch.position == Pos.Fighting) {
        send_to_char("You do the best you can!\n", ch)
        return
    }

    WAIT_STATE(ch, PULSE_VIOLENCE)
    if (!can_see(victim, ch)) {
        do_yell(victim, "Help! I am being attacked by someone!")
    } else {
        val buf = TextBuffer()
        if (ch.pcdata == null) {
            buf.sprintf("Help! I am being attacked by %s!", ch.short_desc)
        } else {
            buf.sprintf("Help!  I am being attacked by %s!", doppel_name(ch, victim))
        }
        do_yell(victim, buf)
    }

    val wield = get_wield_char(ch, false)
    if (skill_failure_nomessage(ch, Skill.mortal_strike, 0) == 0 && wield != null && wield.level > victim.level - 5) {
        var chance = 1 + get_skill(ch, Skill.mortal_strike) / 30
        chance += (ch.level - victim.level) / 2
        if (randomPercent() < chance) {
            act("{rYour flash strike instantly slays \$N!{x", ch, null, victim, TO_CHAR, Pos.Resting)
            act("{r\$n flash strike instantly slays \$N!{x", ch, null, victim, TO_NOTVICT, Pos.Resting)
            act("{r\$n flash strike instantly slays you!{x", ch, null, victim, TO_VICT, Pos.Dead)
            damage(ch, victim, victim.hit + 1, Skill.mortal_strike, DT.None, true)
            check_improve(ch, Skill.mortal_strike, true, 1)
            return
        } else {
            check_improve(ch, Skill.mortal_strike, false, 3)
        }
    }

    multi_hit(ch, victim, null)
}


fun do_flee(ch: CHAR_DATA) {
    if (RIDDEN(ch) != null) {
        send_to_char("You should ask to your rider!\n", ch)
        return
    }

    if (MOUNTED(ch) != null) {
        do_dismount(ch)
    }

    if (ch.fighting == null) {
        if (ch.position == Pos.Fighting) {
            ch.position = Pos.Standing
        }
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    if (ch.clazz === Clazz.SAMURAI && ch.level >= 10) {
        send_to_char("Your honour doesn't let you flee, try dishonoring yourself.\n", ch)
        return
    }

    val was_in = ch.room
    var attempt = 0

    while (attempt < 6) {
        val door = randomDir()
        val pexit = was_in.exit[door]
        if (pexit == null || IS_SET(pexit.exit_info, EX_CLOSED) && (!IS_AFFECTED(ch, AFF_PASS_DOOR) || IS_SET(pexit.exit_info, EX_NO_PASS)) && !IS_TRUSTED(ch, ANGEL) || IS_SET(pexit.exit_info, EX_NO_FLEE) || ch.pcdata == null && IS_SET(pexit.to_room.room_flags, ROOM_NO_MOB)) {
            attempt++
            continue
        }

        move_char(ch, door)
        val now_in = ch.room
        if (now_in == was_in) {
            attempt++
            continue
        }

        ch.room = was_in
        act("\$n has fled!", ch, null, null, TO_ROOM)
        ch.room = now_in

        if (ch.pcdata != null) {
            send_to_char("You flee from combat!  You lose 10 exps.\n", ch)
            if (ch.clazz === Clazz.SAMURAI && ch.level >= 10) {
                gain_exp(ch, -1 * ch.level)
            } else {
                gain_exp(ch, -10)
            }
        } else {
            ch.last_fought = null
        }

        stop_fighting(ch, true)
        return
    }

    send_to_char("PANIC! You couldn't escape!\n", ch)
}


fun do_sla(ch: CHAR_DATA) = send_to_char("If you want to SLAY, spell it out.\n", ch)


fun do_slay(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Slay whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (ch === victim) {
        send_to_char("Suicide is a mortal sin.\n", ch)
        return
    }

    if (victim.pcdata != null && victim.level >= get_trust(ch)
            && !(ch.pcdata == null && ch.cabal != Clan.None && !IS_IMMORTAL(victim))) {
        send_to_char("You failed.\n", ch)
        return
    }

    act("You slay \$M in cold blood!", ch, null, victim, TO_CHAR)
    act("\$n slays you in cold blood!", ch, null, victim, TO_VICT)
    act("\$n slays \$N in cold blood!", ch, null, victim, TO_NOTVICT)
    raw_kill(victim)
}

/* Check for obj dodge. */

fun check_obj_dodge(ch: CHAR_DATA, victim: CHAR_DATA, obj: Obj, bonus: Int): Boolean {
    if (!IS_AWAKE(victim) || MOUNTED(victim) != null) {
        return false
    }

    var chance: Int
    if (victim.pcdata == null) {
        chance = Math.min(30, victim.level)
    } else {
        chance = get_skill(victim, Skill.dodge) / 2
        /* chance for high dex. */
        chance += 2 * (victim.curr_stat(PrimeStat.Dexterity) - 20)
        if (victim.clazz === Clazz.WARRIOR || victim.clazz === Clazz.SAMURAI) {
            chance = (chance * 1.2).toInt()
        }
        if (victim.clazz === Clazz.THIEF || victim.clazz === Clazz.NINJA) {
            chance = (chance * 1.1).toInt()
        }
    }

    chance -= bonus - 90
    chance /= 2
    if (randomPercent() >= chance && (victim.pcdata == null || victim.cabal != Clan.BattleRager)) {
        return false
    }

    if (victim.pcdata != null && victim.cabal == Clan.BattleRager && IS_SET(victim.act, PLR_CANINDUCT)) {
        act("You catch \$p that had been shot to you.", ch, obj, victim, TO_VICT)
        act("\$N catches \$p that had been shot to \$M.", ch, obj, victim, TO_CHAR)
        act("\$n catches \$p that had been shot to \$m.", victim, obj, ch, TO_NOTVICT)
        obj_to_char(obj, victim)
        return true
    }

    act("You dodge \$p that had been shot to you.", ch, obj, victim, TO_VICT)
    act("\$N dodges \$p that had been shot to \$M.", ch, obj, victim, TO_CHAR)
    act("\$n dodges \$p that had been shot to \$m.", victim, obj, ch, TO_NOTVICT)
    obj_to_room(obj, victim.room)
    check_improve(victim, Skill.dodge, true, 6)

    return true
}


fun do_dishonor(ch: CHAR_DATA) {
    if (RIDDEN(ch) != null) {
        send_to_char("You should ask to your rider!\n", ch)
        return
    }

    if (ch.clazz !== Clazz.SAMURAI || ch.level < 10) {
        send_to_char("Which honor?.\n", ch)
        return
    }

    val ch_fighting = ch.fighting
    if (ch_fighting == null) {
        if (ch.position == Pos.Fighting) {
            ch.position = Pos.Standing
        }
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    val level = Index.CHARS
            .filter { is_same_group(it, ch_fighting) || it.fighting == ch }
            .sumBy { it.level }

    if (ch_fighting.level - ch.level < 5 && ch.level > level / 3) {
        send_to_char("Your fighting doesn't worth to dishonor yourself.\n", ch)
        return
    }

    val was_in = ch.room
    for (attempt in 0 until 6) {
        val door = randomDir()
        val exit = was_in.exit[door]
        if (exit == null
                || IS_SET(exit.exit_info, EX_CLOSED)
                && (!IS_AFFECTED(ch, AFF_PASS_DOOR) || IS_SET(exit.exit_info, EX_NO_PASS))
                && !IS_TRUSTED(ch, ANGEL)
                || IS_SET(exit.exit_info, EX_NO_FLEE)
                || ch.pcdata == null && IS_SET(exit.to_room.room_flags, ROOM_NO_MOB)) {
            continue
        }

        move_char(ch, door)
        val now_in = ch.room
        if (now_in == was_in) continue

        ch.room = was_in
        act("\$n has dishonored \$mself!", ch, null, null, TO_ROOM)
        ch.room = now_in

        if (ch.pcdata != null) {
            send_to_char("You dishonored yourself and flee from combat.\n", ch)
            send_to_char("You lose ${ch.level} exps.\n", ch)
            gain_exp(ch, -ch.level)
        } else {
            ch.last_fought = null
        }

        stop_fighting(ch, true)
        if (MOUNTED(ch) != null) do_dismount(ch)
        return
    }

    send_to_char("PANIC! You couldn't escape!\n", ch)
}

private typealias SP = Pair<Int, Skill>
fun mob_cast_cleric(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    while (true) {
        val (min_level, sn) = when (random(16)) {
            0 -> SP(0, Skill.blindness)
            1 -> SP(3, Skill.cure_serious)
            2 -> SP(7, Skill.earthquake)
            3 -> SP(9, Skill.cause_critical)
            4 -> SP(10, Skill.dispel_evil)
            5 -> SP(12, Skill.curse)
            6 -> SP(14, Skill.cause_critical)
            7 -> SP(18, Skill.flamestrike)
            8, 9, 10 -> SP(20, Skill.harm)
            11 -> SP(25, Skill.plague)
            12, 13 -> SP(45, Skill.severity_force)
            else -> SP(26, Skill.dispel_magic)
        }

        if (ch.level >= min_level) {
            say_spell(ch, sn)
            sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
            return true
        }
    }
}

fun mob_cast_mage(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    while (true) {
        val (min_level, sn) = when (random(16)) {
            0 -> SP(0, Skill.blindness)
            1 -> SP(3, Skill.chill_touch)
            2 -> SP(7, Skill.weaken)
            3 -> SP(9, Skill.teleport)
            4 -> SP(14, Skill.colour_spray)
            5 -> SP(19, Skill.caustic_font)
            6 -> SP(25, Skill.energy_drain)
            7, 8, 9 -> SP(35, Skill.caustic_font)
            10 -> SP(40, Skill.plague)
            11, 12, 13 -> SP(40, Skill.acid_arrow)
            else -> SP(55, Skill.acid_blast)
        }
        if (ch.level >= min_level) {
            say_spell(ch, sn)
            sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
            return true
        }
    }
}
