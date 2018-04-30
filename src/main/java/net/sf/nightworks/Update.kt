package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Size
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.Wiznet
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.model.extraPractices
import net.sf.nightworks.model.hitp
import net.sf.nightworks.util.CANT_CHANGE_TITLE
import net.sf.nightworks.util.CAN_WEAR
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_HARA_KIRI
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OUTSIDE
import net.sf.nightworks.util.IS_ROOM_AFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.IS_WATER
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.RIDDEN
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.flipCoin
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.randomDir
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.oneChanceOf
import java.util.Formatter

/** used for saving */
private var save_number = 0

/** Advancement stuff. */
fun advance_level(ch: CHAR_DATA) {
    val pcdata = ch.pcdata
    if (pcdata == null) {
        bug("Advance_level: a mob to advance!")
        return
    }

    pcdata.last_level = (ch.played + (current_time - ch.logon).toInt()) / 3600

    if (pcdata.title.contains(ch.clazz.getTitle(ch.level - 1, ch.sex)) || CANT_CHANGE_TITLE(ch)) {
        set_title(ch, "the" + ch.clazz.getTitle(ch.level, ch.sex))
    }

    var add_hp = ch.hitp + number_range(1, 5) - 3
    add_hp = add_hp * ch.clazz.hp_rate / 100
    var add_mana = number_range(ch.curr_stat(PrimeStat.Intelligence) / 2, 2 * ch.curr_stat(PrimeStat.Intelligence) + ch.curr_stat(PrimeStat.Wisdom) / 5)
    add_mana = add_mana * ch.clazz.mana_rate / 100

    var add_move = number_range(1, (ch.curr_stat(PrimeStat.Constitution) + 2 * ch.curr_stat(PrimeStat.Dexterity)) / 6)
    val add_prac = ch.extraPractices

    add_hp = Math.max(3, add_hp)
    add_mana = Math.max(3, add_mana)
    add_move = Math.max(6, add_move)

    if (ch.sex.isFemale()) {
        add_hp -= 1
        add_mana += 2
    }

    ch.max_hit += add_hp
    ch.max_mana += add_mana
    ch.max_move += add_move
    ch.practice += add_prac
    ch.train += if (ch.level % 5 == 0) 1 else 0

    pcdata.perm_hit += add_hp
    pcdata.perm_mana += add_mana
    pcdata.perm_move += add_move

    val f = Formatter().format("You gain: {W%d{x hp, {W%d{x mana, {W%d{x mv {W%d{x prac.\n", add_hp, add_mana, add_move, add_prac)
    send_to_char(f.toString(), ch)
}


internal fun gain_exp(ch: CHAR_DATA, gain: Int) {

    if (ch.pcdata == null || ch.level >= LEVEL_HERO) {
        return
    }
    if (IS_SET(ch.act, PLR_NO_EXP)) {
        send_to_char("You can't gain exp without your spirit.\n", ch)
        return
    }
    /*
ch.exp = UMAX( exp_per_level(ch,ch.pcdata.points), ch.exp + gain );
while ( ch.level < LEVEL_HERO && ch.exp >=
exp_per_level(ch,ch.pcdata.points) * (ch.level+1) )
*/

    ch.exp = Math.max(base_exp(ch), ch.exp + gain)
    while (ch.level < LEVEL_HERO && exp_to_level(ch) <= 0) {
        send_to_char("You raise a level!!  ", ch)
        ch.level += 1

        /* added for samurais by chronos */
        if (ch.clazz === Clazz.SAMURAI && ch.level == 10) {
            ch.wimpy = 0
        }

        /* Level counting */
        if (ch.level > 5) {
            total_levels++
        }

        if (ch.level == 90) {
            log_string(ch.name + " made level 90.")
        }

        wiznet("\$N has attained level " + ch.level + "!", ch, null, Wiznet.Levels, null, 0)
        advance_level(ch)
        save_char_obj(ch)
    }
}

/*
* Regeneration stuff.
*/

fun hit_gain(ch: CHAR_DATA): Int {
    if (ch.room.vnum == ROOM_VNUM_LIMBO) {
        return 0
    }

    var gain: Int
    val number: Int

    val pcdata = ch.pcdata
    if (pcdata == null) {
        gain = 5 + ch.level
        if (IS_AFFECTED(ch, AFF_REGENERATION)) {
            gain *= 2
        }

        when (ch.position) {
            Pos.Sleeping -> gain = 3 * gain / 2
            Pos.Resting -> {
            }
            Pos.Fighting -> gain /= 3
            else -> gain /= 2
        }


    } else {
        gain = Math.max(3, 2 * ch.curr_stat(PrimeStat.Constitution) + 7 * ch.level / 4)
        gain = gain * ch.clazz.hp_rate / 100
        number = randomPercent()
        if (number < get_skill(ch, Skill.fast_healing)) {
            gain += number * gain / 100
            if (ch.hit < ch.max_hit) {
                check_improve(ch, Skill.fast_healing, true, 8)
            }
        }

        if (number < get_skill(ch, Skill.trance)) {
            gain += number * gain / 150
            if (ch.mana < ch.max_mana) {
                check_improve(ch, Skill.trance, true, 8)
            }
        }
        when (ch.position) {
            Pos.Sleeping -> {
            }
            Pos.Resting -> gain /= 2
            Pos.Fighting -> gain /= 6
            else -> gain /= 4
        }

        if (pcdata.condition[COND_HUNGER] < 0) {
            gain = 0
        }

        if (pcdata.condition[COND_THIRST] < 0) {
            gain = 0
        }

    }

    gain = gain * ch.room.heal_rate / 100

    if (ch.on != null && ch.on!!.item_type == ItemType.Furniture) {
        gain = (gain * ch.on!!.value[3] / 100).toInt()
    }

    if (IS_AFFECTED(ch, AFF_POISON)) {
        gain /= 4
    }

    if (IS_AFFECTED(ch, AFF_PLAGUE)) {
        gain /= 8
    }

    if (IS_AFFECTED(ch, AFF_HASTE)) {
        gain /= 2
    }

    if (IS_AFFECTED(ch, AFF_SLOW)) {
        gain *= 2
    }

    if (ch.curr_stat(PrimeStat.Constitution) > 20) {
        gain = gain * 14 / 10
    }

    if (IS_HARA_KIRI(ch)) {
        gain *= 3
    }

    return Math.min(gain, ch.max_hit - ch.hit)
}


internal fun mana_gain(ch: CHAR_DATA): Int {
    if (ch.room.vnum == ROOM_VNUM_LIMBO) {
        return 0
    }

    var gain: Int
    val number: Int

    val pcdata = ch.pcdata
    if (pcdata == null) {
        gain = 5 + ch.level
        when (ch.position) {
            Pos.Sleeping -> gain = 3 * gain / 2
            Pos.Resting -> {
            }
            Pos.Fighting -> gain /= 3
            else -> gain /= 2
        }
    } else {
        gain = ch.curr_stat(PrimeStat.Wisdom) + 2 * ch.curr_stat(PrimeStat.Intelligence) + ch.level
        gain = gain * ch.clazz.mana_rate / 100
        number = randomPercent()
        if (number < get_skill(ch, Skill.meditation)) {
            gain += number * gain / 100
            if (ch.mana < ch.max_mana) {
                check_improve(ch, Skill.meditation, true, 8)
            }
        }

        if (number < get_skill(ch, Skill.trance)) {
            gain += number * gain / 100
            if (ch.mana < ch.max_mana) {
                check_improve(ch, Skill.trance, true, 8)
            }
        }

        if (!ch.clazz.fMana) {
            gain /= 2
        }

        when (ch.position) {
            Pos.Sleeping -> {
            }
            Pos.Resting -> gain /= 2
            Pos.Fighting -> gain /= 6
            else -> gain /= 4
        }

        if (pcdata.condition[COND_HUNGER] < 0) {
            gain = 0
        }

        if (pcdata.condition[COND_THIRST] < 0) {
            gain = 0
        }

    }

    gain = gain * ch.room.mana_rate / 100

    if (ch.on != null && ch.on!!.item_type === ItemType.Furniture) {
        gain = (gain * ch.on!!.value[4] / 100).toInt()
    }

    if (IS_AFFECTED(ch, AFF_POISON)) {
        gain /= 4
    }

    if (IS_AFFECTED(ch, AFF_PLAGUE)) {
        gain /= 8
    }

    if (IS_AFFECTED(ch, AFF_HASTE)) {
        gain /= 2
    }
    if (IS_AFFECTED(ch, AFF_SLOW)) {
        gain *= 2
    }
    if (ch.curr_stat(PrimeStat.Intelligence) > 20) {
        gain = gain * 13 / 10
    }
    if (ch.curr_stat(PrimeStat.Wisdom) > 20) {
        gain = gain * 11 / 10
    }
    if (IS_HARA_KIRI(ch)) {
        gain *= 3
    }

    return Math.min(gain, ch.max_mana - ch.mana)
}


fun move_gain(ch: CHAR_DATA): Int {
    var gain: Int

    if (ch.room.vnum == ROOM_VNUM_LIMBO) {
        return 0
    }

    val pcdata = ch.pcdata
    if (pcdata == null) {
        gain = ch.level
    } else {
        gain = Math.max(15, 2 * ch.level)

        gain += when (ch.position) {
            Pos.Sleeping -> 2 * ch.curr_stat(PrimeStat.Dexterity)
            Pos.Resting -> ch.curr_stat(PrimeStat.Dexterity)
            else -> 0
        }

        if (pcdata.condition[COND_HUNGER] < 0) {
            gain = 3
        }

        if (pcdata.condition[COND_THIRST] < 0) {
            gain = 3
        }
    }

    gain = gain * ch.room.heal_rate / 100

    if (ch.on != null && ch.on!!.item_type == ItemType.Furniture) {
        gain = (gain * ch.on!!.value[3] / 100).toInt()
    }

    if (IS_AFFECTED(ch, AFF_POISON)) {
        gain /= 4
    }

    if (IS_AFFECTED(ch, AFF_PLAGUE)) {
        gain /= 8
    }

    if (IS_AFFECTED(ch, AFF_HASTE) || IS_AFFECTED(ch, AFF_SLOW)) {
        gain /= 2
    }
    if (ch.curr_stat(PrimeStat.Dexterity) > 20) {
        gain *= 14 / 10
    }
    if (IS_HARA_KIRI(ch)) {
        gain *= 3
    }

    return Math.min(gain, ch.max_move - ch.move)
}


fun gain_condition(ch: CHAR_DATA, condIdx: Int, value: Int) {
    val pcdata = ch.pcdata

    if (value == 0 || pcdata == null || ch.level >= LEVEL_IMMORTAL || ch.room.vnum == ROOM_VNUM_LIMBO) {
        return
    }

    val condition = pcdata.condition[condIdx]

    pcdata.condition[condIdx] = URANGE(-6, condition + value, 96)

    if (condIdx == COND_FULL && pcdata.condition[COND_FULL] < 0) {
        pcdata.condition[COND_FULL] = 0
    }

    if (condIdx == COND_DRUNK && condition < 1) {
        pcdata.condition[COND_DRUNK] = 0
    }

    if (pcdata.condition[condIdx] < 1 && pcdata.condition[condIdx] > -6) {
        when (condIdx) {
            COND_HUNGER -> send_to_char("You are hungry.\n", ch)

            COND_THIRST -> send_to_char("You are thirsty.\n", ch)

            COND_DRUNK -> if (condition != 0) {
                send_to_char("You are sober.\n", ch)
            }

            COND_BLOODLUST -> if (condition != 0) {
                send_to_char("You are hungry for blood.\n", ch)
            }

            COND_DESIRE -> if (condition != 0) {
                send_to_char("You have missed your home.\n", ch)
            }
        }
    }

    if (pcdata.condition[condIdx] == -6 && ch.level >= PK_MIN_LEVEL) {
        when (condIdx) {
            COND_HUNGER -> {
                send_to_char("You are starving!\n", ch)
                act("\$n is starving!", ch, null, null, TO_ROOM)
                var damage_hunger = ch.max_hit * number_range(2, 4) / 100
                if (damage_hunger == 0) {
                    damage_hunger = 1
                }
                damage(ch, ch, damage_hunger, Skill.x_hunger, DT.Hunger, true)
                if (ch.position == Pos.Sleeping) {
                    return
                }
            }

            COND_THIRST -> {
                send_to_char("You are dying of thrist!\n", ch)
                act("\$n is dying of thirst!", ch, null, null, TO_ROOM)
                var damage_hunger = ch.max_hit * number_range(2, 4) / 100
                if (damage_hunger == 0) {
                    damage_hunger = 1
                }
                damage(ch, ch, damage_hunger, Skill.x_hunger, DT.Thirst, true)
                if (ch.position == Pos.Sleeping) {
                    return
                }
            }

            COND_BLOODLUST -> {
                var fdone = false
                send_to_char("You are suffering from thrist of blood!\n", ch)
                act("\$n is suffering from thirst of blood!", ch, null, null, TO_ROOM)
                if (ch.room.people.isNotEmpty() && ch.fighting != null) {
                    if (!IS_AWAKE(ch)) {
                        do_stand(ch, "")
                    }
                    for (vch in ch.room.people) {
                        //todo: strange code
                        if (ch.fighting != null) {
                            break
                        }
                        if (ch != vch && can_see(ch, vch) && !is_safe_nomessage(ch, vch)) {
                            do_yell(ch, "BLOOD! I NEED BLOOD!")
                            do_murder(ch, vch.name)
                            fdone = true
                            break
                        }
                    }
                }
                if (!fdone) {
                    var damage_hunger = ch.max_hit * number_range(2, 4) / 100
                    if (damage_hunger == 0) {
                        damage_hunger = 1
                    }
                    damage(ch, ch, damage_hunger, Skill.x_hunger, DT.Thirst, true)
                    if (ch.position == Pos.Sleeping) {
                        return
                    }
                }
            }

            COND_DESIRE -> {
                send_to_char("You want to go your home!\n", ch)
                act("\$n desires for \$s home!", ch, null, null, TO_ROOM)
                if (ch.position >= Pos.Standing) {
                    move_char(ch, randomDir())
                }
            }
        }
    }


}

/*
* Mob autonomous action.
* This function takes 25% to 35% of ALL Merc cpu time.
* -- Furey
*/

fun mobile_update() {
    /* Examine all mobs. */
    for (ch in Index.CHARS) {
        if (IS_AFFECTED(ch, AFF_REGENERATION)) {
            ch.hit = Math.min(ch.hit + ch.level / 10, ch.max_hit)
            if (ch.race === Race.TROLL) {
                ch.hit = Math.min(ch.hit + ch.level / 10, ch.max_hit)
            }
            if (ch.cabal == Clan.BattleRager && is_affected(ch, Skill.bandage)) {
                ch.hit = Math.min(ch.hit + ch.level / 10, ch.max_hit)
            }
            if (ch.hit != ch.max_hit) {
                send_to_char("", ch)
            }
        }

        if (IS_AFFECTED(ch, AFF_CORRUPTION)) {
            ch.hit -= ch.level / 10
            if (ch.hit < 1) {
                val sn = Skill.corruption
                ch.hit = 1
                damage(ch, ch, 16, sn, DT.None, false)
                continue
            } else {
                send_to_char("", ch)
            }
        }

        if (IS_AFFECTED(ch, AFF_SUFFOCATE)) {
            ch.hit -= ch.level / 5
            if (ch.hit < 1) {
                val sn = Skill.suffocate
                ch.hit = 1
                damage(ch, ch, 16, sn, DT.None, false)
                continue
            } else {
                if (randomPercent() < 30) {
                    send_to_char("You cannot breath!", ch)
                }
            }
        }

        //todo: check reference comparision
        val spec_fun = ch.spec_fun
        if (spec_fun != null && spec_fun === spec_lookup("spec_special_guard")) {
            if (spec_fun(ch)) {
                continue
            }
        }

        if (ch.pcdata != null || IS_AFFECTED(ch, AFF_CHARM)) {
            continue
        }

        val fighting = ch.fighting
        if (IS_SET(ch.act, ACT_HUNTER) && ch.hunting != null && fighting == null) {
            hunt_victim(ch)
        }

        if (ch.room.area.empty && !IS_SET(ch.act, ACT_UPDATE_ALWAYS)) {
            continue
        }

        /* Examine call for special procedure */
        if (spec_fun != null) {
            if (spec_fun(ch)) {
                continue
            }
        }

        if (ch.pIndexData.pShop != null) {/* give him some gold */
            if (ch.gold * 100 + ch.silver < ch.pIndexData.wealth) {
                ch.gold += ch.pIndexData.wealth * number_range(1, 20) / 5000000
                ch.silver += ch.pIndexData.wealth * number_range(1, 20) / 50000
            }
        }

        /*
        *  Potion using and stuff for intelligent mobs
        */

        if (ch.position == Pos.Standing || ch.position == Pos.Resting || ch.position == Pos.Fighting) {
            if (ch.curr_stat(PrimeStat.Intelligence) > 15 && (ch.hit < ch.max_hit * 0.9 ||
                    IS_AFFECTED(ch, AFF_BLIND) ||
                    IS_AFFECTED(ch, AFF_POISON) ||
                    IS_AFFECTED(ch, AFF_PLAGUE) || fighting != null)) {
                for (obj in ch.carrying) {
                    if (obj.item_type != ItemType.Potion) continue
                    if (ch.hit < ch.max_hit * 0.9)
                    /* hp curies */ {
                        val cl = potion_cure_level(obj)
                        if (cl > 0) {
                            if (ch.hit < ch.max_hit * 0.5 && cl > 3) {
                                do_quaff(ch, obj.name)
                                continue
                            } else if (ch.hit < ch.max_hit * 0.7) {
                                do_quaff(ch, obj.name)
                                continue
                            }
                        }
                    }
                    if (IS_AFFECTED(ch, AFF_POISON) && potion_cure_poison(obj)) {
                        do_quaff(ch, obj.name)
                        continue
                    }
                    if (IS_AFFECTED(ch, AFF_PLAGUE) && potion_cure_disease(obj)) {
                        do_quaff(ch, obj.name)
                        continue
                    }
                    if (IS_AFFECTED(ch, AFF_BLIND) && potion_cure_blind(obj)) {
                        do_quaff(ch, obj.name)
                        continue
                    }
                    if (fighting != null) {
                        val al = potion_arm_level(obj)
                        val d = ch.level - fighting.level
                        if ((d < 7 && al > 3) || (d < 8 && al > 2) || (d < 9 && al > 1) || (d < 10 && al > 0)) {
                            do_quaff(ch, obj.name)
                        }
                    }
                }
            }
        }

        /* That's all for sleeping / busy monster, and empty zones */
        if (ch.position != Pos.Standing) {
            continue
        }

        if (IS_SET(ch.progtypes, MPROG_AREA) && ch.room.area.nPlayers > 0) {
            ch.pIndexData.mprogs.area_prog!!(ch)
        }

        if (ch.position < Pos.Standing) {
            continue
        }

        /* Scavenge */
        if (IS_SET(ch.act, ACT_SCAVENGER) && ch.room.objects.isNotEmpty() && oneChanceOf(64)) {
            var obj_best: Obj? = null
            var max = 1
            for (tobj in ch.room.objects) {
                if (CAN_WEAR(tobj, ITEM_TAKE) && can_loot(ch, tobj) && tobj.cost > max) {
                    obj_best = tobj
                    max = tobj.cost
                }
            }

            if (obj_best != null) {
                obj_from_room(obj_best)
                obj_to_char(obj_best, ch)
                act("\$n gets \$p.", ch, obj_best, null, TO_ROOM)
                if (IS_SET(obj_best.progtypes, OPROG_GET)) obj_best.pIndexData.oprogs.get_prog!!(obj_best, ch)
            }
        }

        /* Wander */
        val door = randomDir()
        val pexit = ch.room.exit[door]
        if (!IS_SET(ch.act, ACT_SENTINEL)
                && oneChanceOf(8)
                && door <= 5
                && RIDDEN(ch) == null
                && pexit != null
                && !IS_SET(pexit.exit_info, EX_CLOSED)
                && !IS_SET(pexit.to_room.room_flags, ROOM_NO_MOB)
                && (!IS_SET(ch.act, ACT_STAY_AREA) || pexit.to_room.area == ch.room.area)
                && (!IS_SET(ch.act, ACT_OUTDOORS) || !IS_SET(pexit.to_room.room_flags, ROOM_INDOORS))
                && (!IS_SET(ch.act, ACT_INDOORS) || IS_SET(pexit.to_room.room_flags, ROOM_INDOORS))) {
            move_char(ch, door)
        }
    }

}

internal fun potion_cure_level(potion: Obj): Int {
    var cl = 0
    for (i in 1 until 5) {
        if (Skill.cure_critical.ordinal.toLong() == potion.value[i]) {
            cl += 3
        }
        if (Skill.cure_light.ordinal.toLong() == potion.value[i]) {
            cl += 1
        }
        if (Skill.cure_serious.ordinal.toLong() == potion.value[i]) {
            cl += 2
        }
        if (Skill.heal.ordinal.toLong() == potion.value[i]) {
            cl += 4
        }
    }
    return cl
}

internal fun potion_arm_level(potion: Obj): Int {
    var al = 0
    for (i in 1..4) {
        if (Skill.armor.ordinal.toLong() == potion.value[i]) {
            al += 1
        }
        if (Skill.shield.ordinal.toLong() == potion.value[i]) {
            al += 1
        }
        if (Skill.stone_skin.ordinal.toLong() == potion.value[i]) {
            al += 2
        }
        if (Skill.sanctuary.ordinal.toLong() == potion.value[i]) {
            al += 4
        }
        /*TODO: if (Skill.protection.ordinal() == potion.value[i]) {
            al += 3;
        }*/
    }
    return al
}

private fun potion_cure_blind(potion: Obj) = (0..4).any { Skill.cure_blindness.ordinal.toLong() == potion.value[it] }

private fun potion_cure_poison(potion: Obj) = (0..4).any { Skill.cure_poison.ordinal.toLong() == potion.value[it] }

private fun potion_cure_disease(potion: Obj) = (0..4).any { Skill.cure_disease.ordinal.toLong() == potion.value[it] }

/*
* Update the weather.
*/

internal fun weather_update() {
    val buf = StringBuilder()
    time_info.minute = time_info.minute + 1
    if (time_info.minute == 2) {
        time_info.minute = 0
        time_info.hour = time_info.hour + 1
    }

    when (time_info.hour) {
        5 -> {
            weather.sunlight = SUN_LIGHT
            buf.append("The day has begun.\n")
        }

        6 -> {
            weather.sunlight = SUN_RISE
            buf.append("The sun rises in the east.\n")
        }

        19 -> {
            weather.sunlight = SUN_SET
            buf.append("The sun slowly disappears in the west.\n")
        }

        20 -> {
            weather.sunlight = SUN_DARK
            buf.append("The night has begun.\n")
        }

        24 -> {
            time_info.hour = 0
            time_info.day = time_info.day + 1
        }
    }

    if (time_info.day >= 35) {
        time_info.day = 0
        time_info.month = time_info.month + 1
    }

    if (time_info.month >= 17) {
        time_info.month = 0
        time_info.year = time_info.year + 1
    }

    /*
     * Weather change.
     */
    val diff = if (time_info.month in 9..16) {
        if (weather.mmhg > 985) -2 else 2
    } else {
        if (weather.mmhg > 1015) -2 else 2
    }

    weather.change = weather.change + diff * dice(1, 4) + dice(2, 6) - dice(2, 6)
    weather.change = Math.max(weather.change, -12)
    weather.change = Math.min(weather.change, 12)

    weather.mmhg = weather.mmhg + weather.change
    weather.mmhg = Math.max(weather.mmhg, 960)
    weather.mmhg = Math.min(weather.mmhg, 1040)

    when (weather.sky) {
        SKY_CLOUDLESS -> if (weather.mmhg < 990 || weather.mmhg < 1010 && oneChanceOf(4)) {
            buf.append("The sky is getting cloudy.\n")
            weather.sky = SKY_CLOUDY
        }

        SKY_CLOUDY -> {
            if (weather.mmhg < 970 || weather.mmhg < 990 && oneChanceOf(4)) {
                buf.append("It starts to rain.\n")
                weather.sky = SKY_RAINING
            }

            if (weather.mmhg > 1030 && oneChanceOf(4)) {
                buf.append("The clouds disappear.\n")
                weather.sky = SKY_CLOUDLESS
            }
        }

        SKY_RAINING -> {
            if (weather.mmhg < 970 && oneChanceOf(4)) {
                buf.append("Lightning flashes in the sky.\n")
                weather.sky = SKY_LIGHTNING
            }

            if (weather.mmhg > 1030 || weather.mmhg > 1010 && oneChanceOf(4)) {
                buf.append("The rain stopped.\n")
                weather.sky = SKY_CLOUDY
            }
        }

        SKY_LIGHTNING -> if (weather.mmhg > 1010 || weather.mmhg > 990 && oneChanceOf(4)) {
            buf.append("The lightning has stopped.\n")
            weather.sky = SKY_RAINING
        }
        else -> {
            bug("Weather_update: bad sky %d.", weather.sky)
            weather.sky = SKY_CLOUDLESS
        }
    }

    if (buf.isNotEmpty()) {
        Index.PLAYERS
                .filter { IS_OUTSIDE(it.ch) && IS_AWAKE(it.ch) }
                .forEach { send_to_char(buf, it.ch) }
    }

}

/*
 * Update all chars, including mobs.
*/
private var char_update_last_save_time: Long = -1

internal fun char_update() {
    /* update save counter */
    save_number++

    if (save_number > 29) {
        save_number = 0
    }

    val fTimeSync = check_time_sync()
    var ch_quit: CHAR_DATA? = null

    for (ch in Index.CHARS) {
        var paf: Affect?
        var paf_next: Affect?


        /* reset hunters path find */
        val pcdata = ch.pcdata
        if (pcdata != null) {
            /* Time Sync due Midnight */
            if (fTimeSync) {
                var l = nw_config.max_time_log - 1
                while (l > 0) {
                    pcdata.log_time[l] = pcdata.log_time[l - 1]
                    l--
                }

                /* Nothing for today */
                pcdata.log_time[0] = 0
            }

            if (ch.cabal == Clan.Hunter) {
                if (randomPercent() < get_skill(ch, Skill.path_find)) {
                    ch.endur += get_skill(ch, Skill.path_find) / 2
                    check_improve(ch, Skill.path_find, true, 8)
                } else {
                    check_improve(ch, Skill.path_find, false, 16)
                }
            }

            if (ch.cabal == Clan.BattleRager && !is_affected(ch, Skill.spellbane)) {
                do_spellbane(ch)
            }
        }

        /* Remove caltraps effect after fight off */
        if (is_affected(ch, Skill.caltraps) && ch.fighting == null) {
            affect_strip(ch, Skill.caltraps)
        }

        /* Remove vampire effect when morning. */
        if (IS_VAMPIRE(ch) && (weather.sunlight == SUN_LIGHT || weather.sunlight == SUN_RISE)) {
            do_human(ch)
        }

        /* Reset sneak for vampire */
        if (ch.fighting == null && !IS_AFFECTED(ch, AFF_SNEAK) && IS_VAMPIRE(ch) && MOUNTED(ch) == null) {
            send_to_char("You begin to sneak again.\n", ch)
            ch.affected_by = SET_BIT(ch.affected_by, AFF_SNEAK)
        }

        if (ch.fighting == null && !IS_AFFECTED(ch, AFF_SNEAK) && ch.race.aff and AFF_SNEAK != 0L && MOUNTED(ch) == null) {
            send_to_char("You begin to sneak again.\n", ch)
        }

        if (ch.fighting == null && !IS_AFFECTED(ch, AFF_HIDE) && ch.race.aff and AFF_HIDE != 0L && MOUNTED(ch) == null) {
            send_to_char("You step back into the shadows.\n", ch)
        }

        ch.affected_by = SET_BIT(ch.affected_by, ch.race.aff)

        if (pcdata != null && IS_SET(ch.act, PLR_CHANGED_AFF)) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_FLYING)
        }

        if (MOUNTED(ch) != null) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_IMP_INVIS or AFF_FADE or AFF_SNEAK or AFF_HIDE or AFF_CAMOUFLAGE)
        }

        if (ch.timer > 20 && pcdata != null) {
            ch_quit = ch
        }

        if (ch.position >= Pos.Stunned) {
            /* check to see if we need to go home */
            if (pcdata == null && ch.room.area != ch.room.area //todo:
                    && get_pc(ch) == null && ch.fighting == null && ch.progtypes == 0L
                    && !IS_AFFECTED(ch, AFF_CHARM) && ch.last_fought == null
                    && RIDDEN(ch) == null && randomPercent() < 15) {
                if (ch.in_mind.isNotEmpty() && ch.pIndexData.vnum > 100) {
                    back_home(ch)
                } else {
                    act("\$n wanders on home.", ch, null, null, TO_ROOM)
                    extract_char(ch, true)
                }
                continue
            }

            if (ch.hit < ch.max_hit) {
                ch.hit += hit_gain(ch)
            } else {
                ch.hit = ch.max_hit
            }

            if (ch.mana < ch.max_mana) {
                ch.mana += mana_gain(ch)
            } else {
                ch.mana = ch.max_mana
            }

            if (ch.move < ch.max_move) {
                ch.move += move_gain(ch)
            } else {
                ch.move = ch.max_move
            }
        }

        if (ch.position == Pos.Stunned) {
            update_pos(ch)
        }

        if (pcdata != null && ch.level < LEVEL_IMMORTAL) {
            val obj = get_light_char(ch)
            if (obj != null && obj.item_type == ItemType.Light && obj.value[2] > 0) {
                if (--obj.value[2] == 0L) {
                    unequip_char(ch, obj)
                    if (get_light_char(ch) == null) {
                        --ch.room.light
                    }
                    act("\$p goes out.", ch, obj, null, TO_ROOM)
                    act("\$p flickers and goes out.", ch, obj, null, TO_CHAR)
                    extract_obj(obj)
                } else if (obj.value[2] <= 5) {
                    act("\$p flickers.", ch, obj, null, TO_CHAR)
                }
            }

            if (IS_IMMORTAL(ch)) {
                ch.timer = 0
            }

            if (++ch.timer >= 12) {
                if (ch.was_in_room == null) {
                    ch.was_in_room = ch.room
                    if (ch.fighting != null) {
                        stop_fighting(ch, true)
                    }
                    act("\$n disappears into the void.", ch, null, null, TO_ROOM)
                    send_to_char("You disappear into the void.\n", ch)
                    if (ch.level > 1) {
                        save_char_obj(ch)
                    }
                    if (ch.level < 10) {
                        char_from_room(ch)
                        char_to_room(ch, get_room_index(ROOM_VNUM_LIMBO))
                    }
                }
            }

            gain_condition(ch, COND_DRUNK, -1)
            if (ch.clazz === Clazz.VAMPIRE && ch.level > 10) {
                gain_condition(ch, COND_BLOODLUST, -1)
            }
            gain_condition(ch, COND_FULL, if (ch.size > Size.Medium) -4 else -2)
            if (ch.room.sector_type == SECT_DESERT) {
                gain_condition(ch, COND_THIRST, -3)
            } else {
                gain_condition(ch, COND_THIRST, -1)
            }
            gain_condition(ch, COND_HUNGER, if (ch.size > Size.Medium) -2 else -1)
        }

        paf = ch.affected
        while (paf != null) {
            paf_next = paf.next
            if (paf.duration > 0) {
                paf.duration--

                if (number_range(0, 4) == 0 && paf.level > 0) {
                    paf.level--
                }
                /* spell strength fades with time */
            } else if (paf.duration < 0) {
            } else {
                if (paf_next == null
                        || paf_next.type != paf.type
                        || paf_next.duration > 0) {
                    if (paf.type.msg_off.isNotEmpty()) {
                        send_to_char(paf.type.msg_off, ch)
                        send_to_char("\n", ch)
                    }
                }

                when {
                    paf.type == Skill.strangle -> {
                        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_SLEEP)
                        do_wake(ch, "")
                        val neck_af = Affect()
                        neck_af.type = Skill.neckguard
                        neck_af.level = ch.level
                        neck_af.duration = 2 + ch.level / 50
                        affect_join(ch, neck_af)
                    }
                    paf.type == Skill.blackjack -> {
                        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_SLEEP)
                        do_wake(ch, "")
                        val head_af = Affect()
                        head_af.type = Skill.headguard
                        head_af.level = ch.level
                        head_af.duration = 2 + ch.level / 50
                        affect_join(ch, head_af)
                    }
                    paf.type == Skill.vampiric_touch -> {
                        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_SLEEP)
                        do_wake(ch, "")
                        val b_af = Affect()
                        b_af.type = Skill.blackguard
                        b_af.level = ch.level
                        b_af.duration = 2 + ch.level / 50
                        affect_join(ch, b_af)
                    }
                }

                affect_remove(ch, paf)
            }
            paf = paf_next
        }

        /*
        * Careful with the damages here,
        *   MUST NOT refer to ch after damage taken,
        *   as it may be lethal damage (on NPC).
        */

        if (is_affected(ch, Skill.witch_curse)) {
            var af: Affect?

            act("The witch curse makes \$n feel \$s life slipping away.\n", ch, null, null, TO_ROOM)
            send_to_char("The witch curse makes you feeling your life slipping away.\n", ch)

            af = ch.affected
            while (af != null) {
                if (af.type == Skill.witch_curse) {
                    break
                }
                af = af.next
            }

            if (af == null) {
                continue
            }

            if (af.level == 1) {
                continue
            }

            if (af.modifier > -16001) {
                val witch = Affect()
                witch.where = af.where
                witch.type = af.type
                witch.level = af.level
                witch.duration = af.duration
                witch.location = af.location
                witch.modifier = af.modifier * 2

                affect_remove(ch, af)
                affect_to_char(ch, witch)
                ch.hit = Math.min(ch.hit, ch.max_hit)
                if (ch.hit < 1) {
                    affect_strip(ch, Skill.witch_curse)
                    ch.hit = 1
                    damage(ch, ch, 16, Skill.witch_curse, DT.None, false)
                    continue
                }
            } else {
                affect_strip(ch, Skill.witch_curse)
                ch.hit = 1
                damage(ch, ch, 16, Skill.witch_curse, DT.None, false)
                continue
            }
        }

        if (IS_AFFECTED(ch, AFF_PLAGUE)) {
            var af: Affect?
            val dam: Int

            act("\$n writhes in agony as plague sores erupt from \$s skin.", ch, null, null, TO_ROOM)
            send_to_char("You writhe in agony from the plague.\n", ch)
            af = ch.affected
            while (af != null) {
                if (af.type == Skill.plague) {
                    break
                }
                af = af.next
            }

            if (af == null) {
                ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_PLAGUE)
                continue
            }

            if (af.level == 1) {
                continue
            }
            val plague = Affect()
            plague.type = Skill.plague
            plague.level = af.level - 1
            plague.duration = number_range(1, 2 * plague.level)
            plague.location = Apply.Str
            plague.modifier = -5
            plague.bitvector = AFF_PLAGUE

            for (vch in ch.room.people) {
                if (!saves_spell(plague.level + 2, vch, DT.Disease) && !IS_IMMORTAL(vch) && !IS_AFFECTED(vch, AFF_PLAGUE) && oneChanceOf(4)) {
                    send_to_char("You feel hot and feverish.\n", vch)
                    act("\$n shivers and looks very ill.", vch, null, null, TO_ROOM)
                    affect_join(vch, plague)
                }
            }

            dam = Math.min(ch.level, af.level / 5 + 1)
            ch.mana -= dam
            ch.move -= dam
            if (randomPercent() < 70) {
                damage(ch, ch, dam + Math.max(ch.max_hit / 20, 50), Skill.plague, DT.Disease, true)
            } else {
                damage(ch, ch, dam, Skill.plague, DT.Disease, false)
            }
        } else if (IS_AFFECTED(ch, AFF_POISON) && !IS_AFFECTED(ch, AFF_SLOW)) {
            val poison = affect_find(ch.affected, Skill.poison)

            if (poison != null) {
                act("\$n shivers and suffers.", ch, null, null, TO_ROOM)
                send_to_char("You shiver and suffer.\n", ch)
                damage(ch, ch, poison.level / 10 + 1, Skill.poison,
                        DT.Poison, true)
            }
        } else if (ch.position == Pos.Incap && number_range(0, 1) == 0) {
            damage(ch, ch, 1, null, DT.None, false)
        } else if (ch.position == Pos.Mortal) {
            damage(ch, ch, 1, null, DT.None, false)
        }
    }

    /*
     * Autosave and autoquit.
     * Check that these chars still exist.
     */

    if (char_update_last_save_time == -1L || current_time - char_update_last_save_time > 300) {
        char_update_last_save_time = current_time
        for (ch in Index.CHARS) {
            if (ch.pcdata != null) {
                save_char_obj(ch)
            }
            if (ch == ch_quit || ch.timer > 20) {
                do_quit(ch)
            }
        }
    }

    if (fTimeSync) {
        limit_time = current_time
    }

}


internal fun water_float_update() {
    for (obj in Index.OBJECTS) {
        val in_room = obj.in_room ?: continue
        if (IS_WATER(in_room)) {
            val ch = in_room.people.firstOrNull()

            if (obj.water_float != -1) {
                obj.water_float--
            }

            if (obj.water_float < 0) {
                obj.water_float = -1
            }

            if (obj.item_type == ItemType.DrinkContainer) {
                obj.value[1] = URANGE(1, obj.value[1] + 8, obj.value[0])
                if (ch != null) {
                    act("\$p makes bubbles on the water.", ch, obj, null, TO_CHAR)
                    act("\$p makes bubbles on the water.", ch, obj, null, TO_ROOM)
                }
                obj.water_float = (obj.value[0] - obj.value[1]).toInt()
                obj.value[2] = 0
            }
            if (obj.water_float == 0) {
                if (ch != null) {
                    if (obj.item_type == ItemType.CorpseNpc || obj.item_type == ItemType.CorpsePc || obj.item_type == ItemType.Container) {
                        act("\$p sinks down the water releasing some bubbles behind.", ch, obj, null, TO_CHAR)
                        act("\$p sinks down the water releasing some bubbles behind.", ch, obj, null, TO_ROOM)
                    } else {
                        act("\$p sinks down the water.", ch, obj, null, TO_CHAR)
                        act("\$p sinks down the water.", ch, obj, null, TO_ROOM)
                    }
                }
                extract_obj(obj)
            }
        }
    }

}

/**
 * Update all objs.
 * This function is performance sensitive.
 */
private var obj_update_pit_count = 1

fun obj_update() {
    for (obj in Index.OBJECTS) {
        /* go through affects and decrement */
        var paf = obj.affected
        val carried_by = obj.carried_by
        val obj_room = obj.in_room
        while (paf != null) {
            val paf_next = paf.next
            when {
                paf.duration > 0 -> {
                    paf.duration--
                    if (number_range(0, 4) == 0 && paf.level > 0) {
                        paf.level--  /* spell strength fades with time */
                    }
                }
                paf.duration < 0 -> {
                }
                else -> {
                    if (paf_next == null || paf_next.type != paf.type || paf_next.duration > 0) {
                        if (paf.type.msg_obj.isNotEmpty()) {
                            if (carried_by != null) {
                                val rch = carried_by
                                act(paf.type.msg_obj, rch, obj, null, TO_CHAR)
                            }
                            if (obj_room != null && obj_room.people.isNotEmpty()) {
                                val rch = obj_room.people.first()
                                act(paf.type.msg_obj, rch, obj, null, TO_ALL)
                            }
                        }
                    }
                    affect_remove_obj(obj, paf)
                }
            }
            paf = paf_next
        }

        var t_obj: Obj = obj
        while (t_obj.in_obj != null) {
            t_obj = t_obj.in_obj!!
        }

        if (IS_SET(obj.progtypes, OPROG_AREA)) {
            if (t_obj.in_room != null && t_obj.in_room!!.area.nPlayers > 0 || t_obj.carried_by != null && t_obj.carried_by!!.room.area.nPlayers > 0) {
                obj.pIndexData.oprogs.area_prog!!(obj)
            }
        }

        if (check_material(obj, "ice")) {
            if (carried_by != null) {
                if (carried_by.room.sector_type == SECT_DESERT) {
                    if (randomPercent() < 40) {
                        act("The extreme heat melts \$p.", carried_by, obj, null, TO_CHAR)
                        extract_obj(obj)
                        continue
                    }
                }
            } else if (obj_room != null) {
                if (obj_room.sector_type == SECT_DESERT) {
                    if (randomPercent() < 50) {
                        act("The extreme heat melts \$p.", obj_room, obj, null, TO_ROOM)
                        act("The extreme heat melts \$p.", obj_room, obj, null, TO_CHAR)
                        extract_obj(obj)
                        continue
                    }
                }
            }
        }

        if (!check_material(obj, "glass") && obj.item_type == ItemType.Potion) {
            if (carried_by != null) {
                if (carried_by.room.sector_type == SECT_DESERT && carried_by.pcdata != null) {
                    if (randomPercent() < 20) {
                        act("\$p evaporates.", carried_by, obj, null, TO_CHAR)
                        extract_obj(obj)
                        continue
                    }
                }
            } else if (obj_room != null) {
                if (obj_room.sector_type == SECT_DESERT) {
                    if (randomPercent() < 30) {
                        act("\$p evaporates by the extream heat.", obj_room, obj, null, TO_ROOM)
                        act("\$p evaporates by the extream heat.", obj_room, obj, null, TO_CHAR)
                        extract_obj(obj)
                        continue
                    }
                }
            }
        }

        if (obj.condition > -1 && (obj.timer <= 0 || --obj.timer > 0)) {
            continue
        }

        val message = when (obj.item_type) {
            ItemType.Fountain -> "\$p dries up."
            ItemType.CorpseNpc -> "\$p decays into dust."
            ItemType.CorpsePc -> "\$p decays into dust."
            ItemType.Food -> "\$p decomposes."
            ItemType.Potion -> "\$p has evaporated from disuse."
            ItemType.Portal -> "\$p fades out of existence."
            ItemType.Container -> if (CAN_WEAR(obj, ITEM_WEAR_FLOAT)) {
                when {
                    obj.contains.isNotEmpty() -> "\$p flickers and vanishes, spilling its contents on the floor."
                    else -> "\$p flickers and vanishes."
                }
            } else "\$p crumbles into dust."
            else -> "\$p crumbles into dust."
        }

        if (carried_by != null) {
            if (carried_by.pcdata == null && carried_by.pIndexData.pShop != null) {
                carried_by.silver += obj.cost / 5
            } else {
                act(message, carried_by, obj, null, TO_CHAR)
                if (obj.wear_loc == Wear.Float) {
                    act(message, carried_by, obj, null, TO_ROOM)
                }
            }
        } else {
            val in_obj = obj.in_obj
            val rch = obj_room?.people?.firstOrNull()
            if (obj_room != null && rch != null) {
                if (in_obj == null || in_obj.pIndexData.vnum != OBJ_VNUM_PIT || CAN_WEAR(in_obj, ITEM_TAKE)) {
                    act(message, rch, obj, null, TO_ROOM)
                    act(message, rch, obj, null, TO_CHAR)
                }
            }
        }

        obj_update_pit_count = ++obj_update_pit_count % 120 /* more or less an hour */
        if (obj.pIndexData.vnum == OBJ_VNUM_PIT && obj_update_pit_count == 121) {
            obj.contains.forEach {
                obj_from_obj(it)
                extract_obj(it)
            }
        }


        if ((obj.item_type == ItemType.CorpsePc || obj.wear_loc == Wear.Float) && obj.contains.isNotEmpty()) {   /* save the contents */
            for (o in obj.contains) {
                obj_from_obj(o)

                val in_obj = obj.in_obj
                when {
                    in_obj != null -> obj_to_obj(o, in_obj)
                    carried_by != null ->
                        if (obj.wear_loc == Wear.Float) {
                            obj_to_room(o, carried_by.room)
                        } else {
                            obj_to_char(o, carried_by)
                        }
                    obj_room == null -> extract_obj(o)
                    else -> { /* to the pit */
                        val objects = get_room_index_nn(obj.altar).objects
                        val pit = objects.firstOrNull { it.pIndexData.vnum == obj.pit }
                        if (pit == null) {
                            obj_to_room(o, obj_room)
                        } else {
                            obj_to_obj(o, pit)
                        }
                    }
                }
            }
        }
        extract_obj(obj)
    }
}

/*
* Aggress.
*
* for each mortal PC
*     for each mob in room
*         aggress on some random PC
*
* This function takes 25% to 35% of ALL Merc cpu time.
* Unfortunately, checking on each PC move is too tricky,
*   because we don't the mob to just attack the first PC
*   who leads the party into the room.
*
* -- Furey
*/

internal fun aggr_update() {
    for (wch in Index.CHARS) {
        //            if (!IS_VALID(wch)) {
        //                bug("Aggr_update: Invalid char.", 0);
        //                break;
        //            }

        if (IS_AFFECTED(wch, AFF_BLOODTHIRST) && IS_AWAKE(wch) && wch.fighting == null) {
            for (vch in wch.room.people) {
                if (wch.fighting != null) {
                    break
                }
                if (wch != vch && can_see(wch, vch) && !is_safe_nomessage(wch, vch)) {
                    act("{rMORE BLOOD! MORE BLOOD! MORE BLOOD!!!{x", wch, null, null, TO_CHAR, Pos.Resting)
                    do_murder(wch, vch.name)
                }
            }
        }

        if (wch.cabal != Clan.None && wch.pcdata == null) {
            wch.room.people
                    .filter { it.pcdata != null && !IS_IMMORTAL(it) && it.cabal != wch.cabal && it.fighting == null }
                    .forEach { multi_hit(wch, it, null) }
            continue
        }

        if (wch.pcdata == null || wch.level >= LEVEL_IMMORTAL || wch.room.area.empty) {
            continue
        }

        for (ch in wch.room.people) {
            if (ch.pcdata != null
                    || !IS_SET(ch.act, ACT_AGGRESSIVE) && ch.last_fought == null
                    || IS_SET(ch.room.room_flags, ROOM_SAFE)
                    || IS_AFFECTED(ch, AFF_CALM)
                    || ch.fighting != null
                    || RIDDEN(ch) != null
                    || IS_AFFECTED(ch, AFF_CHARM)
                    || IS_AFFECTED(ch, AFF_SCREAM)
                    || !IS_AWAKE(ch)
                    || IS_SET(ch.act, ACT_WIMPY) && IS_AWAKE(wch)
                    || !can_see(ch, wch)
                    || flipCoin()
                    || is_safe_nomessage(ch, wch)) {
                continue
            }

            /* Mad mob attacks! */
            if (ch.last_fought == wch) {
                val buf = (if (is_affected(wch, Skill.doppelganger) && !IS_SET(ch.act, PLR_HOLYLIGHT))
                    PERS(wch.doppel, ch)
                else
                    PERS(wch, ch)) + "! Now you die!"
                do_yell(ch, buf)
                val g = check_guard(wch, ch)
                multi_hit(ch, g, null)
                continue
            }

            if (ch.last_fought != null) {
                continue
            }

            /*
            * Ok we have a 'wch' player character and a 'ch' npc aggressor.
            * Now make the aggressor fight a RANDOM pc victim in the room,
            *   giving each 'vch' an equal chance of selection.
            */
            var victim = wch.room.people
                    .filter {
                        it.pcdata != null && it.level < LEVEL_IMMORTAL && ch.level >= it.level - 5
                                && (!IS_SET(ch.act, ACT_WIMPY) || !IS_AWAKE(it)) && can_see(ch, it)
                                && it.clazz !== Clazz.VAMPIRE /* do not attack vampires */ && !(ch.isGood() && it.isGood())
                    }
                    .filterIndexed { count, _ -> number_range(0, count) == 0 }
                    .lastOrNull() ?: continue

            if (!is_safe_nomessage(ch, victim)) {
                victim = check_guard(victim, ch)
                if (IS_SET(ch.off_flags, OFF_BACKSTAB) && get_wield_char(ch, false) != null) {
                    multi_hit(ch, victim, Skill.backstab)
                } else {
                    multi_hit(ch, victim, null)
                }
            }
        }
    }
}


private var pulse_area: Int = 0
private var pulse_mobile: Int = 0
private var pulse_violence: Int = 0
private var pulse_point: Int = 0
private var pulse_music: Int = 0
private var pulse_water_float: Int = 0
private var pulse_raffect: Int = 0
private var pulse_track: Int = 0

/*
* Handle all kinds of updates.
* Called once per pulse from game loop.
* Random times to defeat tick-timing clients and players.
*/

internal fun update_handler() {
    //TODO: Moved from COMM
    //
    //                if (d.character != null && d.character.daze > 0) {
    //                    --d.character.daze;
    //                }
    //
    //                if (d.character != null && d.character.wait > 0) {
    //                    --d.character.wait;
    //                    continue;
    //                }

    if (--pulse_area <= 0) {
        wiznet("AREA & ROOM TICK!", null, null, Wiznet.Ticks, null, 0)
        pulse_area = PULSE_AREA
        area_update()
        room_update()
    }

    if (--pulse_music <= 0) {
        pulse_music = PULSE_MUSIC
        /*  song_update(); */
    }

    if (--pulse_mobile <= 0) {
        pulse_mobile = PULSE_MOBILE
        mobile_update()
        light_update()
    }

    if (--pulse_violence <= 0) {
        pulse_violence = PULSE_VIOLENCE
        violence_update()
    }

    if (--pulse_water_float <= 0) {
        pulse_water_float = PULSE_WATER_FLOAT
        water_float_update()
    }

    if (--pulse_raffect <= 0) {
        pulse_raffect = PULSE_ROOM_AFFECT
        room_affect_update()
    }

    if (--pulse_track <= 0) {
        pulse_track = PULSE_TRACK
        track_update()
    }

    if (--pulse_point <= 0) {
        wiznet("CHAR TICK!", null, null, Wiznet.Ticks, null, 0)
        pulse_point = PULSE_TICK
        weather_update()
        char_update()
        quest_update()
        obj_update()
        check_reboot()

        /* room counting */
        Index.CHARS
                .filterNot { it.pcdata == null }
                .forEach { it.room.area.count = Math.min(it.room.area.count + 1, 5000000) }
    }

    aggr_update()
    auction_update()

}

fun light_update() {
    for (d in Index.PLAYERS) {

        val ch = d.o_ch

        if (IS_IMMORTAL(ch)) {
            continue
        }

        if (ch.clazz !== Clazz.VAMPIRE) {
            continue
        }

        /* also checks vampireness */
        val flag = isn_dark_safe(ch)
        if (flag == 0) {
            continue
        }

        if (flag != 2 && randomPercent() < get_skill(ch, Skill.light_resistance)) {
            check_improve(ch, Skill.light_resistance, true, 32)
            continue
        }

        if (flag == 1) {
            send_to_char("The light in the room disturbs you.\n", ch)
        } else {
            send_to_char("Sun light disturbs you.\n", ch)
        }

        val dam = Math.max(ch.max_hit * 4 / 100, 1)
        damage(ch, ch, dam, Skill.x_hunger, DT.RoomLight, true)

        if (ch.position == Pos.Stunned) {
            update_pos(ch)
        }

        if (randomPercent() < 10) {
            gain_condition(ch, COND_DRUNK, -1)
        }
    }
}


internal fun room_update() {
    var room = top_affected_room
    while (room != null) {
        var paf: Affect?
        var paf_next: Affect?

        val room_next = room.aff_next

        paf = room.affected
        while (paf != null) {
            paf_next = paf.next
            if (paf.duration > 0) {
                paf.duration--
                /*
    if (number_range(0,4) == 0 && paf.level > 0)
      paf.level--;
 spell strength shouldn't fade with time
 because checks safe_rpsell with af.level */
            } else if (paf.duration < 0) {
            } else {
                if (paf_next == null
                        || paf_next.type != paf.type
                        || paf_next.duration > 0) {
                    /*
        if ( paf.type > 0 && paf.type.msg_off )
        {
        act( paf.type.msg_off, ch );
        send_to_char( "\n", ch );
        }
*/
                }

                affect_remove_room(room, paf)
            }
            paf = paf_next
        }
        room = room_next

    }
}

fun room_affect_update() {
    var room = top_affected_room
    while (room != null) {
        val room_next = room.aff_next

        if (IS_ROOM_AFFECTED(room, AFF_ROOM_PLAGUE) && room.people.isNotEmpty()) {
            var af = room.affected
            while (af != null) {
                if (af.type == Skill.black_death) {
                    break
                }
                af = af.next
            }

            if (af == null) {
                room.affected_by = REMOVE_BIT(room.affected_by, AFF_ROOM_PLAGUE)
            } else {
                if (af.level == 1) {
                    af.level = 2
                }
                val plague = Affect()
                plague.type = Skill.plague
                plague.level = af.level - 1
                plague.duration = number_range(1, plague.level / 2 + 1)
                plague.modifier = -5
                plague.bitvector = AFF_PLAGUE

                for (vch in room.people) {
                    if (!saves_spell(plague.level, vch, DT.Disease)
                            && !IS_IMMORTAL(vch)
                            && !is_safe_rspell(af.level, vch)
                            && !IS_AFFECTED(vch, AFF_PLAGUE) && oneChanceOf(8)) {
                        send_to_char("You feel hot and feverish.\n", vch)
                        act("\$n shivers and looks very ill.", vch, null, null, TO_ROOM)
                        affect_join(vch, plague)
                    }
                }

            }
        }
        if (IS_ROOM_AFFECTED(room, AFF_ROOM_POISON) && room.people.isNotEmpty()) {
            var af = room.affected
            while (af != null) {
                if (af.type == Skill.deadly_venom) {
                    break
                }
                af = af.next
            }

            if (af == null) {
                room.affected_by = REMOVE_BIT(room.affected_by, AFF_ROOM_POISON)
            } else {

                if (af.level == 1) {
                    af.level = 2
                }

                val paf = Affect()
                paf.type = Skill.poison
                paf.level = af.level - 1
                paf.duration = number_range(1, paf.level / 5 + 1)
                paf.modifier = -5
                paf.bitvector = AFF_POISON

                for (vch in room.people) {
                    if (!saves_spell(paf.level, vch, DT.Poison)
                            && !IS_IMMORTAL(vch)
                            && !is_safe_rspell(af.level, vch)
                            && !IS_AFFECTED(vch, AFF_POISON) && oneChanceOf(8)) {
                        send_to_char("You feel very sick.\n", vch)
                        act("\$n looks very ill.", vch, null, null, TO_ROOM)
                        affect_join(vch, paf)
                    }
                }
            }
        }

        if (IS_ROOM_AFFECTED(room, AFF_ROOM_SLOW) && room.people.isNotEmpty()) {
            var af = room.affected
            while (af != null) {
                if (af.type == Skill.lethargic_mist) {
                    break
                }
                af = af.next
            }

            if (af == null) {
                room.affected_by = REMOVE_BIT(room.affected_by, AFF_ROOM_SLOW)
            } else {

                if (af.level == 1) {
                    af.level = 2
                }

                val paf = Affect()
                paf.type = Skill.slow
                paf.level = af.level - 1
                paf.duration = number_range(1, paf.level / 5 + 1)
                paf.modifier = -5
                paf.bitvector = AFF_SLOW

                for (vch in room.people) {
                    if (!saves_spell(paf.level, vch, DT.Other)
                            && !IS_IMMORTAL(vch)
                            && !is_safe_rspell(af.level, vch)
                            && !IS_AFFECTED(vch, AFF_SLOW) && oneChanceOf(8)) {
                        send_to_char("You start to move less quickly.\n", vch)
                        act("\$n is moving less quickly.", vch, null, null, TO_ROOM)
                        affect_join(vch, paf)
                    }
                }
            }
        }

        if (IS_ROOM_AFFECTED(room, AFF_ROOM_SLEEP) && room.people.isNotEmpty()) {
            var af = room.affected
            while (af != null) {
                if (af.type == Skill.mysterious_dream) {
                    break
                }
                af = af.next
            }

            if (af == null) {
                room.affected_by = REMOVE_BIT(room.affected_by, AFF_ROOM_SLEEP)
            } else {

                if (af.level == 1) {
                    af.level = 2
                }
                val paf = Affect()
                paf.type = Skill.sleep
                paf.level = af.level - 1
                paf.duration = number_range(1, paf.level / 10 + 1)
                paf.modifier = -5
                paf.bitvector = AFF_SLEEP

                for (vch in room.people) {
                    if (!saves_spell(paf.level - 4, vch, DT.Charm)
                            && !IS_IMMORTAL(vch)
                            && !is_safe_rspell(af.level, vch)
                            && !(vch.pcdata == null && IS_SET(vch.act, ACT_UNDEAD))
                            && !IS_AFFECTED(vch, AFF_SLEEP) && oneChanceOf(8)) {
                        if (IS_AWAKE(vch)) {
                            send_to_char("You feel very sleepy.......zzzzzz.\n", vch)
                            act("\$n goes to sleep.", vch, null, null, TO_ROOM)
                            vch.position = Pos.Sleeping
                        }
                        affect_join(vch, paf)
                    }
                }
            }
        }


        if (IS_ROOM_AFFECTED(room, AFF_ROOM_ESPIRIT) && room.people.isNotEmpty()) {
            var af = room.affected
            while (af != null) {
                if (af.type == Skill.evil_spirit) {
                    break
                }
                af = af.next
            }

            if (af == null) {
                room.affected_by = REMOVE_BIT(room.affected_by, AFF_ROOM_ESPIRIT)
            } else {

                if (af.level == 1) {
                    af.level = 2
                }

                val paf = Affect()
                paf.type = Skill.evil_spirit
                paf.level = af.level
                paf.duration = number_range(1, paf.level / 30)

                for (vch in room.people) {
                    if (!saves_spell(paf.level + 2, vch, DT.Mental)
                            && !IS_IMMORTAL(vch)
                            && !is_safe_rspell(af.level, vch)
                            && !is_affected(vch, Skill.evil_spirit) && oneChanceOf(8)) {
                        send_to_char("You feel worse than ever.\n", vch)
                        act("\$n looks more evil.", vch, null, null, TO_ROOM)
                        affect_join(vch, paf)
                    }
                }
            }
        }
        room = room_next

        /* new ones here
    while (IS_ROOM_AFFECTED(room, AFF_ROOM_) && room.people != null)
    {
        AFFECT_DATA *af, paf;
        CHAR_DATA vch;

        for ( af = room.affected; af != null; af = af.next )
        {
            if (af.type == Skill.)
                break;
        }

        if (af == null)
        {
room.affected_by=                REMOVE_BIT(room.affected_by,AFF_ROOM_);
            break;
        }

        if (af.level == 1)
            af.level = 2;

    paf.where       = TO_AFFECTS;
        paf.type        = Skill.;
        paf.level       = af.level - 1;
        paf.duration    = number_range(1,((paf.level/5)+1));
        paf.location    = Apply.None;
        paf.modifier    = -5;
        paf.bitvector   = AFF_;

        for ( vch = room.people; vch != null; vch = vch.next_in_room)
        {
            if (!saves_spell(paf.level + 2,vch,DAM_)
    &&  !IS_IMMORTAL(vch)
    &&  !is_safe_rspell(af.level,vch)
            &&  !IS_AFFECTED(vch,AFF_) && oneChanceOf(8))
            {
                send_to_char("You feel hot and feverish.\n",vch);
                act("$n shivers and looks very ill.",vch,null,null,TO_ROOM);
                affect_join(vch,paf);
            }
        }
 break;
    }
*/
    }
}


internal fun check_reboot() {
    when (reboot_counter) {
        -1 -> {
        }
        0 -> {
            reboot_nightworks(true, NIGHTWORKS_REBOOT)
            return
        }
        1, 2, 3, 4, 5, 10, 15 -> {
            val buf = "\u0007***** REBOOT IN $reboot_counter MINUTES *****\u0007\n"
            Index.CONNECTIONS.forEach { d -> write_to_buffer(d, buf) }
            reboot_counter--
        }
        else -> reboot_counter--
    }
}

internal fun track_update() {
    for (ch in Index.CHARS) {
        if (ch.pcdata != null
                || IS_AFFECTED(ch, AFF_CALM)
                || IS_AFFECTED(ch, AFF_CHARM)
                || ch.fighting != null
                || !IS_AWAKE(ch)
                || IS_SET(ch.act, ACT_NOTRACK)
                || RIDDEN(ch) != null
                || IS_AFFECTED(ch, AFF_SCREAM)) continue

        if (ch.last_fought != null && ch.room != ch.last_fought!!.room) {
            do_track(ch, ch.last_fought!!.name)
            continue
        }
        if (ch.in_mind.isEmpty()) continue

        for (vch in ch.room.people) {
            if (ch == vch) {
                continue
            }
            if (!IS_IMMORTAL(vch) && can_see(ch, vch) && !is_safe_nomessage(ch, vch) && is_name(vch.name, ch.in_mind)) {
                do_yell(ch, "So we meet again, " + vch.name)
                do_murder(ch, vch.name)
                break /* one fight at a time */
            }
        }
    }
}
