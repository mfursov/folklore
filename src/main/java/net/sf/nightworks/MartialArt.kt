package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.act.spell.spell_poison
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.AttackType
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.EXTRA_DESC_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.WeaponType
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.number_fuzzy
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.random
import net.sf.nightworks.util.randomPercent

/**
 * Disarm a creature.
 * Caller must check for successful attack.
 */
fun disarm(ch: CHAR_DATA, victim: CHAR_DATA, disarm_second: Boolean) {
    val obj: Obj?

    if (disarm_second) {
        obj = get_wield_char(victim, true)
        if (obj == null) {
            bug("Disarm second with null DUAL_WIELD")
            return
        }
    } else {
        obj = get_wield_char(victim, false)
        if (obj == null) {
            bug("Disarm first with null WEAR_WIELD")
            return
        }
    }

    if (IS_OBJ_STAT(obj, ITEM_NOREMOVE)) {
        act("\$S weapon won't budge!", ch, null, victim, TO_CHAR)
        act("\$n tries to disarm you, but your weapon won't budge!", ch, null, victim, TO_VICT)
        act("\$n tries to disarm \$N, but fails.", ch, null, victim, TO_NOTVICT)
        return
    }

    if (skill_failure_nomessage(victim, Skill.grip, 0) == 0) {
        var skill = get_skill(victim, Skill.grip)

        skill += (victim.curr_stat(PrimeStat.Strength) - ch.curr_stat(PrimeStat.Strength)) * 5
        if (randomPercent() < skill) {
            act("\$N grips and prevent you to disarm \$S!", ch, null, victim, TO_CHAR)
            act("\$n tries to disarm you, but you grip and escape!", ch, null, victim, TO_VICT)
            act("\$n tries to disarm \$N, but fails.", ch, null, victim, TO_NOTVICT)
            check_improve(victim, Skill.grip, true, 1)
            return
        } else {
            check_improve(victim, Skill.grip, false, 1)
        }
    }

    act("\$n {cDISARMS{x you and sends your weapon flying!", ch, null, victim, TO_VICT, Pos.Fighting)
    act("You {Cdisarm{x \$N!", ch, null, victim, TO_CHAR, Pos.Fighting)
    act("\$n {Cdisarms{x \$N!", ch, null, victim, TO_NOTVICT, Pos.Fighting)

    obj_from_char(obj)
    if (IS_OBJ_STAT(obj, ITEM_NODROP) || IS_OBJ_STAT(obj, ITEM_INVENTORY)) {
        obj_to_char(obj, victim)
    } else {
        obj_to_room(obj, victim.room)
        if (victim.pcdata == null && victim.wait == 0 && can_see_obj(victim, obj)) {
            get_obj(victim, obj, null)
        }
    }
    /*
    if ( (obj2 = get_wield_char(victim,true)) != null)
    {
act( "$CYou wield your second weapon as your first!.{x", ch, null,
    victim,TO_VICT,Pos.Fighting,CLR_CYAN);
act( "$C$N wields his second weapon as first!{x",  ch, null,
    victim,TO_CHAR ,Pos.Fighting,CLR_CYAN_BOLD);
act( "$C$N wields his second weapon as first!{x",  ch, null, victim,
    TO_NOTVICT ,Pos.Fighting,CLR_CYAN_BOLD);
    unequip_char( victim, obj2);
    equip_char( victim, obj2 , WEAR_WIELD);
    }
*/
}

fun do_berserk(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.berserk, false, OFF_BERSERK, "You turn red in the face, but nothing happens.\n")) {
        return
    }

    var chance = get_skill(ch, Skill.berserk)

    if (IS_AFFECTED(ch, AFF_BERSERK) || is_affected(ch, Skill.berserk) || is_affected(ch, Skill.frenzy)) {
        send_to_char("You get a little madder.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CALM)) {
        send_to_char("You're feeling too mellow to berserk.\n", ch)
        return
    }

    if (ch.mana < 50) {
        send_to_char("You can't get up enough energy.\n", ch)
        return
    }

    /* modifiers */

    /* fighting */
    if (ch.position == Pos.Fighting) {
        chance += 10
    }

    /* damage -- below 50% of hp helps, above hurts */
    val hp_percent = 100 * ch.hit / ch.max_hit
    chance += 25 - hp_percent / 2

    if (randomPercent() < chance) {


        WAIT_STATE(ch, PULSE_VIOLENCE)
        ch.mana -= 50
        ch.move /= 2

        /* heal a little damage */
        ch.hit += ch.level * 2
        ch.hit = Math.min(ch.hit, ch.max_hit)

        send_to_char("Your pulse races as you are consumned by rage!\n", ch)
        act("{r\$n gets a wild look in \$s eyes.{x", ch, null, null, TO_ROOM, Pos.Fighting)
        check_improve(ch, Skill.berserk, true, 2)
        val af = Affect()
        af.type = Skill.berserk
        af.level = ch.level
        af.duration = number_fuzzy(ch.level / 8)
        af.modifier = Math.max(1, ch.level / 5)
        af.bitvector = AFF_BERSERK

        af.location = Apply.Hitroll
        affect_to_char(ch, af)

        af.location = Apply.Damroll
        affect_to_char(ch, af)

        af.modifier = Math.max(10, 10 * (ch.level / 5))
        af.location = Apply.Ac
        affect_to_char(ch, af)
    } else {
        WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        ch.mana -= 25
        ch.move /= 2

        send_to_char("Your pulse speeds up, but nothing happens.\n", ch)
        check_improve(ch, Skill.berserk, false, 2)
    }
}

fun do_bash(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val victim: CHAR_DATA?
    val argb = StringBuilder()
    rest = one_argument(rest, argb)

    val arg = argb.toString()
    if (arg.isNotEmpty() && eq(arg, "door")) {
        do_bash_door(ch, rest)
        return
    }

    if (skill_failure_check(ch, Skill.bash, false, OFF_BASH, "Bashing? What's that?\n")) {
        return
    }

    var chance = get_skill(ch, Skill.bash)

    if (arg.isEmpty()) {
        victim = ch.fighting
        if (victim == null) {
            send_to_char("But you aren't fighting anyone!\n", ch)
            return
        }
    } else {
        victim = get_char_room(ch, arg)
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }
    }

    if (victim.position < Pos.Fighting) {
        act("You'll have to let \$M get back up first.", ch, null, victim, TO_CHAR)
        return
    }

    if (victim == ch) {
        send_to_char("You try to bash your brains out, but fail.\n", ch)
        return
    }

    if (MOUNTED(victim) != null) {
        send_to_char("You can't bash a riding one!\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("But \$N is your friend!", ch, null, victim, TO_CHAR)
        return
    }

    if (is_affected(victim, Skill.protective_shield)) {
        act("{YYour bash seems to slide around \$N.{x", ch, null, victim, TO_CHAR, Pos.Fighting)
        act("{Y\$n's bash slides off your protective shield.{x", ch, null, victim, TO_VICT, Pos.Fighting)
        act("{Y\$n's bash seems to slide around \$N.{x", ch, null, victim, TO_NOTVICT, Pos.Fighting)
        return
    }

    /* modifiers */

    /* size  and weight */
    chance += ch.carry_weight / 25
    chance -= victim.carry_weight / 20

    chance += if (ch.size < victim.size) {
        (ch.size - victim.size) * 25
    } else {
        (ch.size - victim.size) * 10
    }

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
    if (randomPercent() < chance) {
        act("\$n sends you sprawling with a powerful bash!", ch, null, victim, TO_VICT)
        act("You slam into \$N, and send \$M flying!", ch, null, victim, TO_CHAR)
        act("\$n sends \$N sprawling with a powerful bash.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.bash, true, 1)

        val wait = when (random(4)) {
            0 -> 1
            1 -> 2
            2 -> 4
            else -> 3
        }

        WAIT_STATE(victim, wait * PULSE_VIOLENCE)
        WAIT_STATE(ch, Skill.bash.beats)
        victim.position = Pos.Resting
        val damage_bash = ch.damroll / 2 + number_range(4, 4 + ch.size * 4 + chance / 10)
        damage(ch, victim, damage_bash, Skill.bash, DT.Bash, true)
    } else {
        damage(ch, victim, 0, Skill.bash, DT.Bash, true)
        act("You fall flat on your face!", ch, null, victim, TO_CHAR)
        act("\$n falls flat on \$s face.", ch, null, victim, TO_NOTVICT)
        act("You evade \$n's bash, causing \$m to fall flat on \$s face.", ch, null, victim, TO_VICT)
        check_improve(ch, Skill.bash, false, 1)
        ch.position = Pos.Resting
        WAIT_STATE(ch, Skill.bash.beats * 3 / 2)
    }
    if (victim.pcdata != null && ch.pcdata != null && victim.position > Pos.Stunned && ch.fighting == null) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! Someone is bashing me!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! %s is bashing me!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}

fun do_dirt(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    var chance: Int

    if (skill_failure_check(ch, Skill.dirt_kicking, false, OFF_KICK_DIRT, "You get your feet dirty.\n")) {
        return
    }

    val FightingCheck = ch.fighting != null

    val argb = StringBuilder()
    one_argument(argument, argb)

    chance = get_skill(ch, Skill.dirt_kicking)
    val arg = argb.toString()
    if (arg.isEmpty()) {
        victim = ch.fighting
        if (victim == null) {
            send_to_char("But you aren't in combat!\n", ch)
            return
        }
    } else {
        victim = get_char_room(ch, arg)
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }
    }

    if (IS_AFFECTED(ch, AFF_FLYING)) {
        send_to_char("While flying?.\n", ch)
        return
    }

    if (IS_AFFECTED(victim, AFF_BLIND)) {
        act("\$e's already been blinded.", ch, null, victim, TO_CHAR)
        return
    }

    if (victim == ch) {
        send_to_char("Very funny.\n", ch)
        return
    }

    if (MOUNTED(victim) != null) {
        send_to_char("You can't dirt a riding one!\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("But \$N is such a good friend!", ch, null, victim, TO_CHAR)
        return
    }

    /* modifiers */

    /* dexterity */
    chance += ch.curr_stat(PrimeStat.Dexterity)
    chance -= 2 * victim.curr_stat(PrimeStat.Dexterity)

    /* speed  */
    if (IS_SET(ch.off_flags, OFF_FAST) || IS_AFFECTED(ch, AFF_HASTE)) {
        chance += 10
    }
    if (IS_SET(victim.off_flags, OFF_FAST) || IS_AFFECTED(victim, OFF_FAST)) {
        chance -= 25
    }

    /* level */
    chance += (ch.level - victim.level) * 2

    if (chance % 5 == 0) {
        chance += 1
    }

    /* terrain */

    when (ch.room.sector_type) {
        SECT_INSIDE -> chance -= 20
        SECT_CITY -> chance -= 10
        SECT_FIELD -> chance += 5
        SECT_FOREST -> {
        }
        SECT_HILLS -> {
        }
        SECT_MOUNTAIN -> chance -= 10
        SECT_WATER_SWIM -> chance = 0
        SECT_WATER_NO_SWIM -> chance = 0
        SECT_AIR -> chance = 0
        SECT_DESERT -> chance += 10
    }

    if (chance == 0) {
        send_to_char("There isn't any dirt to kick.\n", ch)
        return
    }

    /* now the attack */
    if (randomPercent() < chance) {
        act("\$n is blinded by the dirt in \$s eyes!", victim, null, null, TO_ROOM)
        damage(ch, victim, number_range(2, 5), Skill.dirt_kicking, DT.None, true)
        send_to_char("You can't see a thing!\n", victim)
        check_improve(ch, Skill.dirt_kicking, true, 2)
        WAIT_STATE(ch, Skill.dirt_kicking.beats)

        val af = Affect()
        af.type = Skill.dirt_kicking
        af.level = ch.level
        af.location = Apply.Hitroll
        af.modifier = -4
        af.bitvector = AFF_BLIND

        affect_to_char(victim, af)
    } else {
        damage(ch, victim, 0, Skill.dirt_kicking, DT.None, true)
        check_improve(ch, Skill.dirt_kicking, false, 2)
        WAIT_STATE(ch, Skill.dirt_kicking.beats)
    }
    if (victim.pcdata != null && ch.pcdata != null && victim.position > Pos.Stunned
            && !FightingCheck) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Someone just kicked dirt in my eyes!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Die, %s!  You dirty fool!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }

}

fun do_trip(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.trip, false, OFF_TRIP, "Tripping?  What's that?\n")) {
        return
    }

    val argb = StringBuilder()
    one_argument(argument, argb)

    var chance = get_skill(ch, Skill.dirt_kicking)
    val arg = argb.toString()
    val victim: CHAR_DATA?
    if (argb.isEmpty()) {
        victim = ch.fighting
        if (victim == null) {
            send_to_char("But you aren't fighting anyone!\n", ch)
            return
        }
    } else {
        victim = get_char_room(ch, arg)
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }
    }

    if (MOUNTED(victim) != null) {
        send_to_char("You can't trip a riding one!\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(victim, AFF_FLYING)) {
        act("\$S feet aren't on the ground.", ch, null, victim, TO_CHAR)
        return
    }

    if (victim.position < Pos.Fighting) {
        act("\$N is already down.", ch, null, victim, TO_CHAR)
        return
    }

    if (victim == ch) {
        send_to_char("You fall flat on your face!\n", ch)
        WAIT_STATE(ch, 2 * Skill.trip.beats)
        act("\$n trips over \$s own feet!", ch, null, null, TO_ROOM)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("\$N is your beloved master.", ch, null, victim, TO_CHAR)
        return
    }

    /* modifiers */

    /* size */
    if (ch.size < victim.size) {
        chance += (ch.size - victim.size) * 10  /* bigger = harder to trip */
    }

    /* dex */
    chance += ch.curr_stat(PrimeStat.Dexterity)
    chance -= victim.curr_stat(PrimeStat.Dexterity) * 3 / 2

    if (IS_AFFECTED(ch, AFF_FLYING)) {
        chance -= 10
    }

    /* speed */
    if (IS_SET(ch.off_flags, OFF_FAST) || IS_AFFECTED(ch, AFF_HASTE)) {
        chance += 10
    }
    if (IS_SET(victim.off_flags, OFF_FAST) || IS_AFFECTED(victim, AFF_HASTE)) {
        chance -= 20
    }

    /* level */
    chance += (ch.level - victim.level) * 2

    /* now the attack */
    if (randomPercent() < chance) {
        act("\$n trips you and you go down!", ch, null, victim, TO_VICT)
        act("You trip \$N and \$N goes down!", ch, null, victim, TO_CHAR)
        act("\$n trips \$N, sending \$M to the ground.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.trip, true, 1)

        WAIT_STATE(victim, 2 * PULSE_VIOLENCE)
        WAIT_STATE(ch, Skill.trip.beats)
        victim.position = Pos.Resting
        damage(ch, victim, number_range(2, 2 + victim.size * 2), Skill.trip,
                DT.Bash, true)
    } else {
        damage(ch, victim, 0, Skill.trip, DT.Bash, true)
        WAIT_STATE(ch, Skill.trip.beats * 2 / 3)
        check_improve(ch, Skill.trip, false, 1)
    }
    if (victim.pcdata != null && ch.pcdata != null && victim.position > Pos.Stunned && ch.fighting == null) {
        if (!can_see(victim, ch)) {
            do_yell(victim, " Help! Someone just tripped me!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! %s just tripped me!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}


fun do_backstab(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.backstab, false, 0, "You don't know how to backstab.\n")) {
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Backstab whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }
    if (ch.pcdata == null && victim.pcdata != null) {
        return
    }


    if (victim == ch) {
        send_to_char("How can you sneak up on yourself?\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    val obj = get_wield_char(ch, false)
    if (obj == null || AttackType.of(obj.value[3]).damage != DT.Pierce) {
        send_to_char("You need to wield a piercing weapon to backstab.\n", ch)
        return
    }

    if (victim.fighting != null) {
        send_to_char("You can't backstab a fighting person.\n", ch)
        return
    }


    WAIT_STATE(ch, Skill.backstab.beats)

    if (victim.hit < 0.7 * victim.max_hit && IS_AWAKE(victim)) {
        act("\$N is hurt and suspicious ... you couldn't sneak up.", ch, null, victim, TO_CHAR)
        return
    }

    if (current_time - victim.last_fight_time < 300 && IS_AWAKE(victim)) {
        act("\$N is suspicious ... you couldn't sneak up.", ch, null, victim, TO_CHAR)
        return
    }

    if (!IS_AWAKE(victim) || ch.pcdata == null || randomPercent() < get_skill(ch, Skill.backstab)) {
        check_improve(ch, Skill.backstab, true, 1)
        if (ch.pcdata != null && randomPercent() < get_skill(ch, Skill.dual_backstab) / 10 * 8) {
            check_improve(ch, Skill.dual_backstab, true, 1)
            one_hit(ch, victim, Skill.backstab, false)
            one_hit(ch, victim, Skill.dual_backstab, false)
        } else {
            check_improve(ch, Skill.dual_backstab, false, 1)
            multi_hit(ch, victim, Skill.backstab)
        }
    } else {
        check_improve(ch, Skill.backstab, false, 1)
        damage(ch, victim, 0, Skill.backstab, DT.None, true)
    }
    /* Player shouts if he doesn't die */
    if (victim.pcdata != null && ch.pcdata != null && victim.position == Pos.Fighting) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! I've been backstabbed!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Die, %s, you backstabbing scum!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}

fun do_cleave(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?

    if (skill_failure_check(ch, Skill.cleave, false, 0, "You don't know how to cleave.\n")) {
        return
    }
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (ch.master != null && ch.pcdata == null) {
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Cleave whom?\n", ch)
        return
    }

    victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("How can you sneak up on yourself?\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (get_wield_char(ch, false) == null) {
        send_to_char("You need to wield a weapon to cleave.\n", ch)
        return
    }


    if (victim.fighting != null) {
        send_to_char("You can't cleave a fighting person.\n", ch)
        return
    }

    if (victim.hit < 0.9 * victim.max_hit && IS_AWAKE(victim)) {
        act("\$N is hurt and suspicious ... you can't sneak up.", ch, null, victim, TO_CHAR)
        return
    }

    WAIT_STATE(ch, Skill.cleave.beats)
    if (!IS_AWAKE(victim) || ch.pcdata == null || randomPercent() < get_skill(ch, Skill.cleave)) {
        check_improve(ch, Skill.cleave, true, 1)
        multi_hit(ch, victim, Skill.cleave)
    } else {
        check_improve(ch, Skill.cleave, false, 1)
        damage(ch, victim, 0, Skill.cleave, DT.None, true)
    }
    /* Player shouts if he doesn't die */
    if (victim.pcdata != null && ch.pcdata != null && victim.position == Pos.Fighting) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! Someone is attacking me!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Die, %s, you butchering fool!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}

fun do_ambush(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.ambush, false, 0, "You don't know how to ambush.\n")) {
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Ambush whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("How can you ambush yourself?\n", ch)
        return
    }


    if (!IS_AFFECTED(ch, AFF_CAMOUFLAGE) || can_see(victim, ch)) {
        send_to_char("But they can see you.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    WAIT_STATE(ch, Skill.ambush.beats)
    if (!IS_AWAKE(victim) || ch.pcdata == null || randomPercent() < get_skill(ch, Skill.ambush)) {
        check_improve(ch, Skill.ambush, true, 1)
        multi_hit(ch, victim, Skill.ambush)
    } else {
        check_improve(ch, Skill.ambush, false, 1)
        damage(ch, victim, 0, Skill.ambush, DT.None, true)
    }

    /* Player shouts if he doesn't die */
    if (victim.pcdata != null && ch.pcdata != null && victim.position == Pos.Fighting) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! I've been ambushed by someone!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! I've been ambushed by %s!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}


fun do_rescue(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Rescue whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("What about fleeing instead?\n", ch)
        return
    }

    if (ch.pcdata != null && victim.pcdata == null) {
        send_to_char("Doesn't need your help!\n", ch)
        return
    }

    if (ch.fighting == victim) {
        send_to_char("Too late.\n", ch)
        return
    }

    val fch = victim.fighting
    if (fch == null) {
        send_to_char("That person is not fighting right now.\n", ch)
        return
    }
    val ch_master = ch.master
    if (ch.pcdata == null && ch_master != null && victim.pcdata == null) {
        return
    }

    if (is_safe(ch, fch)) {
        return
    }

    if (ch_master != null) {
        if (is_safe(ch_master, fch)) {
            return
        }
    }

    WAIT_STATE(ch, Skill.rescue.beats)
    if (ch.pcdata != null && randomPercent() > get_skill(ch, Skill.rescue) || victim.level > ch.level + 30) {
        send_to_char("You fail the rescue.\n", ch)
        check_improve(ch, Skill.rescue, false, 1)
        return
    }

    act("You rescue \$N!", ch, null, victim, TO_CHAR)
    act("\$n rescues you!", ch, null, victim, TO_VICT)
    act("\$n rescues \$N!", ch, null, victim, TO_NOTVICT)
    check_improve(ch, Skill.rescue, true, 1)

    stop_fighting(fch, false)
    stop_fighting(victim, false)

    set_fighting(ch, fch)
    set_fighting(fch, ch)
}


fun do_kick(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.kick, false, OFF_KICK, "You better leave the martial arts to fighters.\n")) {
        return
    }

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }
    var chance = randomPercent()
    if (IS_AFFECTED(ch, AFF_FLYING)) {
        chance = (chance * 1.1).toInt()
    }
    WAIT_STATE(ch, Skill.kick.beats)
    if (ch.pcdata == null || chance < get_skill(ch, Skill.kick)) {
        var kick_dam = number_range(1, ch.level)
        if (ch.clazz === Clazz.SAMURAI && get_eq_char(ch, Wear.Feet) == null) {
            kick_dam *= 2
        }
        kick_dam += ch.damroll / 2
        damage(ch, victim, kick_dam, Skill.kick, DT.Bash, true)
        check_improve(ch, Skill.kick, true, 1)
    } else {
        damage(ch, victim, 0, Skill.kick, DT.Bash, true)
        check_improve(ch, Skill.kick, false, 1)
    }

}

fun do_circle(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.circle, false, 0, null)) {
        return
    }

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    var second = false
    var wch = get_wield_char(ch, false)
    if (wch == null || AttackType.of(wch.value[3]).damage != DT.Pierce) {
        wch = get_wield_char(ch, true)
        if (wch == null || AttackType.of(wch.value[3]).damage != DT.Pierce) {
            send_to_char("You must wield a piercing weapon to circle stab.\n", ch)
            return
        }
        second = true
    }

    if (is_safe(ch, victim)) {
        return
    }

    WAIT_STATE(ch, Skill.circle.beats)

    for (person in ch.room.people) {
        if (person.fighting == ch) {
            send_to_char("You can't circle while defending yourself.\n", ch)
            return
        }
    }

    if (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.circle)) {
        one_hit(ch, victim, Skill.circle, second)
        check_improve(ch, Skill.circle, true, 1)
    } else {
        damage(ch, victim, 0, Skill.circle, DT.None, true)
        check_improve(ch, Skill.circle, false, 1)
    }

}


fun do_disarm(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.disarm, false, OFF_DISARM, "You don't know how to disarm opponents.\n")) {
        return
    }
    if (ch.master != null && ch.pcdata == null) {
        return
    }

    var disarm_second = false
    var chance = get_skill(ch, Skill.disarm)

    val hth = get_skill(ch, Skill.hand_to_hand)
    if (get_wield_char(ch, false) == null && (hth == 0 || ch.pcdata == null && !IS_SET(ch.off_flags, OFF_DISARM))) {
        send_to_char("You must wield a weapon to disarm.\n", ch)
        return
    }

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)
    if (ch.pcdata != null && arg.isNotEmpty()) {
        disarm_second = is_name(arg.toString(), "second")
    }

    if (get_wield_char(victim, disarm_second) == null) {
        send_to_char("Your opponent is not wielding a weapon.\n", ch)
        return
    }

    /* find weapon skills */
    val ch_weapon = get_weapon_skill(ch, get_weapon_sn(ch, false))

    val vict_weapon = get_weapon_skill(victim, get_weapon_sn(victim, disarm_second))
    val ch_vict_weapon = get_weapon_skill(ch, get_weapon_sn(victim, disarm_second))

    /* modifiers */

    /* skill */
    if (get_wield_char(ch, false) == null) {
        chance = chance * hth / 150
    } else {
        chance = chance * ch_weapon / 100
    }

    chance += (ch_vict_weapon / 2 - vict_weapon) / 2

    /* dex vs. strength */
    chance += ch.curr_stat(PrimeStat.Dexterity)
    chance -= 2 * victim.curr_stat(PrimeStat.Strength)

    /* level */
    chance += (ch.level - victim.level) * 2

    /* and now the attack */
    if (randomPercent() < chance) {
        WAIT_STATE(ch, Skill.disarm.beats)
        disarm(ch, victim, disarm_second)
        check_improve(ch, Skill.disarm, true, 1)
    } else {
        WAIT_STATE(ch, Skill.disarm.beats)
        act("You fail to disarm \$N.", ch, null, victim, TO_CHAR)
        act("\$n tries to disarm you, but fails.", ch, null, victim, TO_VICT)
        act("\$n tries to disarm \$N, but fails.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.disarm, false, 1)
    }
}


fun do_nerve(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.nerve, false, 0, null)) {
        return
    }

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (is_affected(ch, Skill.nerve)) {
        send_to_char("You cannot weaken that character any more.\n", ch)
        return
    }
    WAIT_STATE(ch, Skill.nerve.beats)

    if (ch.pcdata == null || randomPercent() < (get_skill(ch, Skill.nerve) + ch.level + ch.curr_stat(PrimeStat.Dexterity)) / 2) {
        val af = Affect()
        af.type = Skill.nerve
        af.level = ch.level
        af.duration = ch.level * PULSE_VIOLENCE / PULSE_TICK
        af.location = Apply.Str
        af.modifier = -3

        affect_to_char(victim, af)
        act("You weaken \$N with your nerve pressure.", ch, null, victim, TO_CHAR)
        act("\$n weakens you with \$s nerve pressure.", ch, null, victim, TO_VICT)
        act("\$n weakens \$N with \$s nerve pressure.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.nerve, true, 1)
    } else {
        send_to_char("You press the wrong points and fail.\n", ch)
        act("\$n tries to weaken you with nerve pressure, but fails.", ch, null, victim, TO_VICT)
        act("\$n tries to weaken \$N with nerve pressure, but fails.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.nerve, false, 1)
    }

    multi_hit(victim, ch, null)

    if (victim.pcdata != null && ch.pcdata != null && victim.position != Pos.Fighting) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! I'm being attacked by someone!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! I'm being attacked by %s!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}

fun do_endure(ch: CHAR_DATA) {

    if (skill_failure_check(ch, Skill.endure, false, 0, "You lack the concentration.\n")) {
        return
    }

    if (is_affected(ch, Skill.endure)) {
        send_to_char("You cannot endure more concentration.\n", ch)
        return
    }


    WAIT_STATE(ch, Skill.endure.beats)
    val af = Affect()
    af.type = Skill.endure
    af.level = ch.level
    af.duration = ch.level / 4
    af.location = Apply.SavingSpell
    af.modifier = -1 * (get_skill(ch, Skill.endure) / 10)

    affect_to_char(ch, af)

    send_to_char("You prepare yourself for magical encounters.\n", ch)
    act("\$n concentrates for a moment, then resumes \$s position.", ch, null, null, TO_ROOM)
    check_improve(ch, Skill.endure, true, 1)
}

fun do_tame(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.tame, true, 0, "You lack the skills to tame anyone.\n")) {
        return
    }

    if (arg.isEmpty()) {
        send_to_char("You are beyond taming.\n", ch)
        act("\$n tries to tame \$mself but fails miserably.", ch, null, null, TO_ROOM)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They're not here.\n", ch)
        return
    }

    if (victim.pcdata != null) {
        act("\$N is beyond taming.", ch, null, victim, TO_CHAR)
        return
    }

    if (!IS_SET(victim.act, ACT_AGGRESSIVE)) {
        act("\$N is not usually aggressive.", ch, null, victim, TO_CHAR)
        return
    }

    WAIT_STATE(ch, Skill.tame.beats)

    if (randomPercent() < get_skill(ch, Skill.tame) + 15 + 4 * (ch.level - victim.level)) {
        victim.act = REMOVE_BIT(victim.act, ACT_AGGRESSIVE)
        victim.affected_by = SET_BIT(victim.affected_by, AFF_CALM)
        send_to_char("You calm down.\n", victim)
        act("You calm \$N down.", ch, null, victim, TO_CHAR)
        act("\$n calms \$N down.", ch, null, victim, TO_NOTVICT)
        stop_fighting(victim, true)
        check_improve(ch, Skill.tame, true, 1)
    } else {
        send_to_char("You failed.\n", ch)
        act("\$n tries to calm down \$N but fails.", ch, null, victim, TO_NOTVICT)
        act("\$n tries to calm you down but fails.", ch, null, victim, TO_VICT)
        check_improve(ch, Skill.tame, false, 1)
    }
}

fun do_assassinate(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.assassinate, false, 0, null)) {
        return
    }

    val arg = StringBuilder()

    one_argument(argument, arg)

    if (ch.master != null && ch.pcdata == null) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM)) {
        send_to_char("You don't want to kill your beloved master.\n", ch)
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Assassinate whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("Suicide is against your way.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_IMMORTAL(victim) && victim.pcdata != null) {
        send_to_char("Your hands pass through.\n", ch)
        return
    }

    if (victim.fighting != null) {
        send_to_char("You can't assassinate a fighting person.\n", ch)
        return
    }

    if (get_wield_char(ch, false) != null || get_hold_char(ch) != null) {
        send_to_char("You need both hands free to assassinate somebody.\n", ch)
        return
    }

    if (victim.hit < victim.max_hit && can_see(victim, ch) && IS_AWAKE(victim)) {
        act("\$N is hurt and suspicious ... you can't sneak up.", ch, null, victim, TO_CHAR)
        return
    }

    /*
    if (IS_SET(victim.imm_flags, IMM_WEAPON))
      {
    act("$N seems immune to your assassination attempt.", ch, null,
         victim, TO_CHAR);
    act("$N seems immune to $n's assassination attempt.", ch, null,
        victim, TO_ROOM);
    return;
      }
*/
    WAIT_STATE(ch, Skill.assassinate.beats)
    if (ch.pcdata == null || !IS_AWAKE(victim) || randomPercent() < get_skill(ch, Skill.assassinate)) {
        multi_hit(ch, victim, Skill.assassinate)
    } else {
        check_improve(ch, Skill.assassinate, false, 1)
        damage(ch, victim, 0, Skill.assassinate, DT.None, true)
    }
    /* Player shouts if he doesn't die */
    if (victim.pcdata != null && ch.pcdata != null && victim.position == Pos.Fighting) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! Someone tried to assassinate me!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! %s tried to assassinate me!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}


fun do_caltraps(ch: CHAR_DATA) {
    val victim = ch.fighting

    if (skill_failure_check(ch, Skill.caltraps, false, 0, "Caltraps? Is that a dance step?\n")) {
        return
    }

    if (victim == null) {
        send_to_char("You must be in combat.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    act("You throw a handful of sharp spikes at the feet of \$N.",
            ch, null, victim, TO_CHAR)
    act("\$n throws a handful of sharp spikes at your feet!",
            ch, null, victim, TO_VICT)

    WAIT_STATE(ch, Skill.caltraps.beats)

    if (ch.pcdata != null && randomPercent() >= get_skill(ch, Skill.caltraps)) {
        damage(ch, victim, 0, Skill.caltraps, DT.Pierce, true)
        check_improve(ch, Skill.caltraps, false, 1)
        return
    }

    damage(ch, victim, ch.level, Skill.caltraps, DT.Pierce, true)

    if (!is_affected(victim, Skill.caltraps)) {
        val tohit = Affect()
        tohit.type = Skill.caltraps
        tohit.level = ch.level
        tohit.duration = -1
        tohit.location = Apply.Hitroll
        tohit.modifier = -5
        affect_to_char(victim, tohit)

        val todam = Affect()
        todam.type = Skill.caltraps
        todam.level = ch.level
        todam.duration = -1
        todam.location = Apply.Damroll
        todam.modifier = -5
        affect_to_char(victim, todam)

        val todex = Affect()
        todex.type = Skill.caltraps
        todex.level = ch.level
        todex.duration = -1
        todex.location = Apply.Dex
        todex.modifier = -5
        affect_to_char(victim, todex)

        act("\$N starts limping.", ch, null, victim, TO_CHAR)
        act("You start to limp.", ch, null, victim, TO_VICT)
        check_improve(ch, Skill.caltraps, true, 1)
    }
}


fun do_throw(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    if (eq(arg.toString(), "spear")) {
        do_throw_spear(ch, rest)
        return
    }

    if (skill_failure_check(ch, Skill.Throw, false, 0, "A clutz like you couldn't throw down a worm.\n")) {
        return
    }

    if (IS_AFFECTED(ch, AFF_FLYING)) {
        send_to_char("Your feet should touch the ground to balance\n", ch)
        return
    }

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("But \$N is your friend!", ch, null, victim, TO_CHAR)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    WAIT_STATE(ch, Skill.Throw.beats)

    if (is_affected(victim, Skill.protective_shield)) {
        act("{YYou fail to reach \$s arm.{x", ch, null, victim, TO_CHAR, Pos.Fighting)
        act("{Y\$n fails to throw you.{x", ch, null, victim, TO_VICT, Pos.Fighting)
        act("{Y\$n fails to throw \$N.{x", ch, null, victim, TO_NOTVICT, Pos.Fighting)
        return
    }

    var chance = get_skill(ch, Skill.Throw)

    chance += if (ch.size < victim.size) {
        (ch.size - victim.size) * 10
    } else {
        (ch.size - victim.size) * 25
    }

    /* stats */
    chance += ch.curr_stat(PrimeStat.Strength)
    chance -= victim.curr_stat(PrimeStat.Dexterity) * 4 / 3

    if (IS_AFFECTED(victim, AFF_FLYING)) {
        chance += 10
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

    if (ch.pcdata == null || randomPercent() < chance) {
        act("You throw \$N to the ground with stunning force.", ch, null, victim, TO_CHAR)
        act("\$n throws you to the ground with stunning force.", ch, null, victim, TO_VICT)
        act("\$n throws \$N to the ground with stunning force.", ch, null, victim, TO_NOTVICT)
        WAIT_STATE(victim, 2 * PULSE_VIOLENCE)

        damage(ch, victim, ch.level + ch.curr_stat(PrimeStat.Strength), Skill.Throw, DT.Bash, true)
        check_improve(ch, Skill.Throw, true, 1)
    } else {
        act("You fail to grab your opponent.", ch, null, null, TO_CHAR)
        act("\$N tries to throw you, but fails.", victim, null, ch, TO_CHAR)
        act("\$n tries to grab \$N's arm.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.Throw, false, 1)
    }

}

fun do_strangle(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.strangle, false, 0, "You lack the skill to strangle.\n")) {
        return
    }

    val victim = get_char_room(ch, argument)
    if (victim == null) {
        send_to_char("You do not see that person here.\n", ch)
        return
    }

    if (ch == victim) {
        send_to_char("Even you are not that stupid.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && victim == ch.leader) {
        send_to_char("You don't want to grap your beloved masters' neck.\n", ch)
        return
    }

    if (IS_AFFECTED(victim, AFF_SLEEP)) {
        act("\$E is already asleep.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_affected(victim, Skill.neckguard)) {
        act("\$N's guarding \$S neck.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    victim.last_fight_time = current_time
    ch.last_fight_time = current_time

    WAIT_STATE(ch, Skill.strangle.beats)

    var chance = if (ch.pcdata == null) Math.min(35, ch.level / 2) else (0.6 * get_skill(ch, Skill.strangle)).toInt()

    if (victim.pcdata == null && victim.pIndexData.pShop != null) {
        chance -= 40
    }

    if (randomPercent() < chance) {
        act("You grab hold of \$N's neck and put \$M to sleep.",
                ch, null, victim, TO_CHAR)
        act("\$n grabs hold of your neck and puts you to sleep.",
                ch, null, victim, TO_VICT)
        act("\$n grabs hold of \$N's neck and puts \$M to sleep.",
                ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.strangle, true, 1)

        val af = Affect()
        af.type = Skill.strangle
        af.level = ch.level
        af.duration = ch.level / 50 + 1
        af.bitvector = AFF_SLEEP
        affect_join(victim, af)

        if (IS_AWAKE(victim)) {
            victim.position = Pos.Sleeping
        }
    } else {

        damage(ch, victim, 0, Skill.strangle, DT.None, true)
        check_improve(ch, Skill.strangle, false, 1)
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! I'm being strangled by someone!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! I'm being strangled by %s!", doppel_name(ch, victim))
            if (victim.pcdata != null) {
                do_yell(victim, buf.toString())
            }
        }
        val af = Affect()
        af.type = Skill.neckguard
        af.level = victim.level
        af.duration = 2 + victim.level / 50
        affect_join(victim, af)
    }
}

fun do_blackjack(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.blackjack, false, 0, null)) {
        return
    }

    val victim = get_char_room(ch, argument)
    if (victim == null) {
        send_to_char("You do not see that person here.\n", ch)
        return
    }

    if (ch == victim) {
        send_to_char("You idiot?! Blackjack your self?!\n", ch)
        return
    }


    if (IS_AFFECTED(ch, AFF_CHARM) && victim == ch.leader) {
        send_to_char("You don't want to hit your beloved masters' head with a full filled jack.\n", ch)
        return
    }

    if (IS_AFFECTED(victim, AFF_SLEEP)) {
        act("\$E is already asleep.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_affected(victim, Skill.headguard)) {
        act("\$N's guarding \$S head!.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }
    victim.last_fight_time = current_time
    ch.last_fight_time = current_time

    WAIT_STATE(ch, Skill.blackjack.beats)

    var chance = (0.5 * get_skill(ch, Skill.blackjack)).toInt()
    chance += URANGE(0, (ch.curr_stat(PrimeStat.Dexterity) - 20) * 2, 10)
    chance += if (can_see(victim, ch)) 0 else 5
    if (victim.pcdata == null) {
        if (victim.pIndexData.pShop != null) {
            chance -= 40
        }
    }

    if (ch.pcdata == null || randomPercent() < chance) {
        act("You hit \$N's head with a lead filled sack.", ch, null, victim, TO_CHAR)
        act("You feel a sudden pain erupts through your skull!", ch, null, victim, TO_VICT)
        act("\$n whacks \$N at the back of \$S head with a heavy looking sack!  *OUCH*", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.blackjack, true, 1)

        val af = Affect()
        af.type = Skill.blackjack
        af.level = ch.level
        af.duration = ch.level / 50 + 1
        af.bitvector = AFF_SLEEP
        affect_join(victim, af)

        if (IS_AWAKE(victim)) {
            victim.position = Pos.Sleeping
        }
    } else {

        damage(ch, victim, ch.level / 2, Skill.blackjack, DT.None, true)
        check_improve(ch, Skill.blackjack, false, 1)

        if (victim.pcdata != null) {
            if (!can_see(victim, ch)) {
                do_yell(victim, "Help! I'm being blackjacked by someone!")
            } else {
                val buf = TextBuffer()
                buf.sprintf("Help! I'm being blackjacked by %s!", doppel_name(ch, victim))
                if (victim.pcdata != null) {
                    do_yell(victim, buf.toString())
                }
            }
        }

        val af = Affect()
        af.type = Skill.headguard
        af.level = victim.level
        af.duration = 2 + victim.level / 50
        affect_join(victim, af)
    }
}


fun do_bloodthirst(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.bloodthirst, true, 0, "You're not that thirsty.\n")) {
        return
    }

    var chance = get_skill(ch, Skill.bloodthirst)

    if (IS_AFFECTED(ch, AFF_BLOODTHIRST)) {
        send_to_char("Your thirst for blood continues.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CALM)) {
        send_to_char("You're feeling to mellow to be bloodthirsty.\n", ch)
        return
    }

    if (ch.fighting == null) {
        send_to_char("You need to be fighting.\n", ch)
        return
    }

    /* modifiers */

    val hp_percent = 100 * ch.hit / ch.max_hit
    chance += 25 - hp_percent / 2

    if (randomPercent() < chance) {

        WAIT_STATE(ch, PULSE_VIOLENCE)


        send_to_char("You hunger for blood!\n", ch)
        act("\$n gets a bloodthirsty look in \$s eyes.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.bloodthirst, true, 2)

        val af = Affect()
        af.type = Skill.bloodthirst
        af.level = ch.level
        af.duration = 2 + ch.level / 18
        af.modifier = 5 + ch.level / 4
        af.bitvector = AFF_BLOODTHIRST

        af.location = Apply.Hitroll
        affect_to_char(ch, af)

        af.location = Apply.Damroll
        affect_to_char(ch, af)

        af.modifier = -Math.min(ch.level - 5, 35)
        af.location = Apply.Ac
        affect_to_char(ch, af)
    } else {
        WAIT_STATE(ch, 3 * PULSE_VIOLENCE)

        send_to_char("You feel bloodthirsty for a moment, but it passes.\n", ch)
        check_improve(ch, Skill.bloodthirst, false, 2)
    }
}


fun do_spellbane(ch: CHAR_DATA) {

    if (skill_failure_check(ch, Skill.spellbane, true, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.spellbane)) {
        send_to_char("You are already deflecting spells.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.spellbane.beats)

    val af = Affect()
    af.type = Skill.spellbane
    af.level = ch.level
    af.duration = ch.level / 3
    af.location = Apply.SavingSpell
    af.modifier = -ch.level / 4
    af.bitvector = AFF_SPELLBANE

    affect_to_char(ch, af)

    act("Your hatred of magic surrounds you.", ch, null, null, TO_CHAR)
    act("\$n fills the air with \$s hatred of magic.", ch, null, null, TO_ROOM)
    check_improve(ch, Skill.spellbane, true, 1)

}

fun do_resistance(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.resistance, true, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.resistance)) {
        send_to_char("You are as resistant as you will get.\n", ch)
        return
    }

    if (ch.mana < 50) {
        send_to_char("You cannot muster up the energy.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.resistance.beats)

    if (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.resistance)) {
        val af = Affect()
        af.type = Skill.resistance
        af.level = ch.level
        af.duration = ch.level / 6
        af.bitvector = AFF_PROTECTOR

        affect_to_char(ch, af)
        ch.mana -= 50

        act("You feel tough!", ch, null, null, TO_CHAR)
        act("\$n looks tougher.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.resistance, true, 1)
    } else {
        ch.mana -= 25

        send_to_char("You flex your muscles, but you don't feel tougher.\n", ch)
        act("\$n flexes \$s muscles, trying to look tough.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.resistance, false, 1)
    }

}

fun do_trophy(ch: CHAR_DATA, argument: String) {
    val trophy_vnum: Int
    val trophy: Obj
    val part: Obj?
    val level: Int
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.trophy, false, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.trophy)) {
        send_to_char("But you've already got one trophy!\n", ch)
        return
    }

    if (ch.mana < 30) {
        send_to_char("You feel too weak to concentrate on a trophy.\n", ch)
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Make a trophy of what?\n", ch)
        return
    }

    part = get_obj_carry(ch, arg.toString())
    if (part == null) {
        send_to_char("You do not have that body part.\n", ch)
        return
    }

    if (randomPercent() < get_skill(ch, Skill.trophy) / 3 * 2) {
        send_to_char("You failed and destroyed it.\n", ch)
        extract_obj(part)
        return
    }

    WAIT_STATE(ch, Skill.trophy.beats)

    trophy_vnum = when {
        part.pIndexData.vnum == OBJ_VNUM_SLICED_ARM -> OBJ_VNUM_BATTLE_PONCHO
        part.pIndexData.vnum == OBJ_VNUM_SLICED_LEG -> OBJ_VNUM_BATTLE_PONCHO
        part.pIndexData.vnum == OBJ_VNUM_SEVERED_HEAD -> OBJ_VNUM_BATTLE_PONCHO
        part.pIndexData.vnum == OBJ_VNUM_TORN_HEART -> OBJ_VNUM_BATTLE_PONCHO
        part.pIndexData.vnum == OBJ_VNUM_GUTS -> OBJ_VNUM_BATTLE_PONCHO
        part.pIndexData.vnum == OBJ_VNUM_BRAINS -> {
            send_to_char("Why don't you just eat those instead?\n", ch)
            return
        }
        else -> {
            send_to_char("You can't make a trophy out of that!\n", ch)
            return
        }
    }

    if (part.from.isEmpty()) {
        send_to_char("Invalid body part.\n", ch)
        return
    }

    val pcdata = ch.pcdata
    if (pcdata != null && randomPercent() < pcdata.learned[Skill.trophy.ordinal]) {
        val af = Affect()
        af.type = Skill.trophy
        af.level = ch.level
        af.duration = ch.level / 2
        affect_to_char(ch, af)

        val buf = TextBuffer()
        level = Math.min(part.level + 5, MAX_LEVEL)

        trophy = create_object(get_obj_index_nn(trophy_vnum), level)
        trophy.timer = ch.level * 2

        buf.sprintf(trophy.short_desc, part.from)
        trophy.short_desc = buf.toString()

        buf.sprintf(trophy.description, part.from)
        trophy.description = buf.toString()
        trophy.cost = 0
        trophy.level = ch.level
        ch.mana -= 30

        af.where = TO_OBJECT
        af.type = Skill.trophy
        af.level = level
        af.duration = -1
        af.location = Apply.Damroll
        af.modifier = ch.level / 5
        affect_to_obj(trophy, af)

        af.location = Apply.Hitroll
        af.modifier = ch.level / 5
        affect_to_obj(trophy, af)

        af.location = Apply.Intelligence
        af.modifier = if (level > 20) -2 else -1
        affect_to_obj(trophy, af)

        af.location = Apply.Str
        af.modifier = if (level > 20) 2 else 1
        affect_to_obj(trophy, af)

        trophy.value[0] = ch.level.toLong()
        trophy.value[1] = ch.level.toLong()
        trophy.value[2] = ch.level.toLong()
        trophy.value[3] = ch.level.toLong()


        obj_to_char(trophy, ch)
        check_improve(ch, Skill.trophy, true, 1)

        act("You make a poncho from \$p!", ch, part, null, TO_CHAR)
        act("\$n makes a poncho from \$p!", ch, part, null, TO_ROOM)

        extract_obj(part)
    } else {
        send_to_char("You destroyed it.\n", ch)
        extract_obj(part)
        ch.mana -= 15
        check_improve(ch, Skill.trophy, false, 1)
    }
}


fun do_truesight(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.truesight, true, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.truesight)) {
        send_to_char("Your eyes are as sharp as they will get.\n", ch)
        return
    }

    if (ch.mana < 50) {
        send_to_char("You cannot seem to focus enough.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.truesight.beats)

    val pcdata = ch.pcdata
    if (pcdata != null && randomPercent() < pcdata.learned[Skill.truesight.ordinal]) {
        val af = Affect()
        af.type = Skill.truesight
        af.level = ch.level
        af.duration = ch.level / 2 + 5
        af.bitvector = AFF_DETECT_HIDDEN
        affect_to_char(ch, af)

        af.bitvector = AFF_DETECT_INVIS
        affect_to_char(ch, af)

        af.bitvector = AFF_DETECT_IMP_INVIS
        affect_to_char(ch, af)

        af.bitvector = AFF_ACUTE_VISION
        affect_to_char(ch, af)

        af.bitvector = AFF_DETECT_MAGIC
        affect_to_char(ch, af)

        ch.mana -= 50

        act("You look around sharply!", ch, null, null, TO_CHAR)
        act("\$n looks more enlightened.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.truesight, true, 1)
    } else {
        ch.mana -= 25

        send_to_char("You look about sharply, but you don't see anything new.\n", ch)
        act("\$n looks around sharply but doesn't seem enlightened.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.truesight, false, 1)
    }

}

fun do_warcry(ch: CHAR_DATA) {
    val pcdata = ch.pcdata ?: return

    if (skill_failure_check(ch, Skill.warcry, true, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.bless) || is_affected(ch, Skill.warcry)) {
        send_to_char("You are already blessed.\n", ch)
        return
    }

    if (ch.mana < 30) {
        send_to_char("You can't concentrate enough right now.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.warcry.beats)

    if (randomPercent() > pcdata.learned[Skill.warcry.ordinal]) {
        send_to_char("You grunt softly.\n", ch)
        act("\$n makes some soft grunting noises.", ch, null, null, TO_ROOM)
        return
    }

    ch.mana -= 30
    val af = Affect()

    af.type = Skill.warcry
    af.level = ch.level
    af.duration = 6 + ch.level
    af.location = Apply.Hitroll
    af.modifier = ch.level / 8
    affect_to_char(ch, af)

    af.location = Apply.SavingSpell
    af.modifier = 0 - ch.level / 8
    affect_to_char(ch, af)
    send_to_char("You feel righteous as you yell out your warcry.\n", ch)
}

fun do_guard(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.guard, true, 0, null)) {
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Guard whom?\n", ch)
        return
    }

    victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        act("\$N doesn't need any of your help!", ch, null, victim, TO_CHAR)
        return
    }

    val guarding = ch.guarding
    if (eq(arg.toString(), "none") || eq(arg.toString(), "self") || victim == ch) {
        if (guarding == null) {
            send_to_char("You can't guard yourself!\n", ch)
            return
        } else {
            act("You stop guarding \$N.", ch, null, guarding, TO_CHAR)
            act("\$n stops guarding you.", ch, null, guarding, TO_VICT)
            act("\$n stops guarding \$N.", ch, null, guarding, TO_NOTVICT)
            guarding.guarded_by = null
            ch.guarding = null
            return
        }
    }

    if (guarding == victim) {
        act("You're already guarding \$N!", ch, null, victim, TO_CHAR)
        return
    }

    if (guarding != null) {
        send_to_char("But you're already guarding someone else!\n", ch)
        return
    }

    if (victim.guarded_by != null) {
        act("\$N is already being guarded by someone.", ch, null, victim, TO_CHAR)
        return
    }

    if (victim.guarding == ch) {
        act("But \$N is guarding you!", ch, null, victim, TO_CHAR)
        return
    }

    if (!is_same_group(victim, ch)) {
        act("But you aren't in the same group as \$N.", ch, null, victim, TO_CHAR)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM)) {
        act("You like your master too much to bother guarding \$N!",
                ch, null, victim, TO_VICT)
        return
    }

    if (victim.fighting != null) {
        send_to_char("Why don't you let them stop fighting first?\n", ch)
        return
    }

    if (ch.fighting != null) {
        send_to_char("Better finish your own battle before you worry about guarding someone else.\n",
                ch)
        return
    }

    act("You are now guarding \$N.", ch, null, victim, TO_CHAR)
    act("You are being guarded by \$n.", ch, null, victim, TO_VICT)
    act("\$n is now guarding \$N.", ch, null, victim, TO_NOTVICT)

    ch.guarding = victim
    victim.guarded_by = ch

}

fun check_guard(ch: CHAR_DATA, mob: CHAR_DATA): CHAR_DATA {
    val guarded_by = ch.guarded_by
    if (guarded_by == null || get_char_room(ch, guarded_by.name) == null) {
        return ch
    }
    val chance = (get_skill(guarded_by, Skill.guard) - 1.5 * (ch.level - mob.level)).toInt()
    if (randomPercent() >= Math.min(100, chance)) {
        check_improve(guarded_by, Skill.guard, false, 3)
        return ch
    }
    act("\$n jumps in front of \$N!", guarded_by, null, ch, TO_NOTVICT)
    act("\$n jumps in front of you!", guarded_by, null, ch, TO_VICT)
    act("You jump in front of \$N!", guarded_by, null, ch, TO_CHAR)
    check_improve(guarded_by, Skill.guard, true, 3)
    return guarded_by
}


fun do_explode(ch: CHAR_DATA, argument: String) {
    var victim: CHAR_DATA? = ch.fighting
    var dam = 0
    val level = ch.level

    if (skill_failure_check(ch, Skill.explode, false, 0, "Flame? What is that?\n")) {
        return
    }

    if (victim == null) {
        val arg = StringBuilder()
        one_argument(argument, arg)
        if (arg.isEmpty()) {
            send_to_char("You play with the exploding material.\n", ch)
            return
        }
        victim = get_char_room(ch, arg.toString())
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }
    }

    val mana = Skill.explode.min_mana

    if (ch.mana < mana) {
        send_to_char("You can't find that much energy to fire\n", ch)
        return
    }
    ch.mana -= mana

    act("\$n burns something.", ch, null, victim, TO_NOTVICT)
    act("\$n burns a cone of exploding material over you!", ch, null, victim, TO_VICT)
    act("Burn them all!.", ch, null, null, TO_CHAR)

    WAIT_STATE(ch, Skill.explode.beats)

    val pcdata = ch.pcdata
    if (pcdata != null && randomPercent() >= pcdata.learned[Skill.explode.ordinal]) {
        damage(ch, victim, 0, Skill.explode, DT.Fire, true)
        check_improve(ch, Skill.explode, false, 1)
        return
    }

    val hpch = Math.max(10, ch.hit)
    val hp_dam = number_range(hpch / 9 + 1, hpch / 5)
    var dice_dam = dice(level, 20)

    if (!is_safe(ch, victim)) {
        dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)
        fire_effect(victim.room, level, dam / 2, TARGET_ROOM)
    }
    victim.room.people
            .filter { !is_safe_spell(ch, it, true) && !(it.pcdata == null && pcdata == null && (ch.fighting != it || it.fighting != ch)) && !is_safe(ch, it) }
            .forEach {
                if (it == victim) {/* full damage */
                    fire_effect(it, level, dam, TARGET_CHAR)
                    damage(ch, it, dam, Skill.explode, DT.Fire, true)
                } else { /* partial damage */
                    fire_effect(it, level / 2, dam / 4, TARGET_CHAR)
                    damage(ch, it, dam / 2, Skill.explode, DT.Fire, true)
                }
            }
    if (pcdata != null && randomPercent() >= pcdata.learned[Skill.explode.ordinal]) {
        fire_effect(ch, level / 4, dam / 10, TARGET_CHAR)
        damage(ch, ch, ch.hit / 10, Skill.explode, DT.Fire, true)
    }
}


fun do_target(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.Target, false, 0, "You don't know how to change the target while fighting a group.\n")) {
        return
    }

    if (ch.fighting == null) {
        send_to_char("You aren't fighting yet.\n", ch)
        return
    }

    if (argument.isEmpty()) {
        send_to_char("Change target to whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, argument)
    if (victim == null) {
        send_to_char("You don't see that item.\n", ch)
        return
    }

    /* check victim is fighting with him */

    if (victim.fighting != ch) {
        send_to_char("Target is not fighting with you.\n", ch)
        return
    }


    WAIT_STATE(ch, Skill.Target.beats)

    if (ch.pcdata != null && randomPercent() < get_skill(ch, Skill.Target) / 2) {
        check_improve(ch, Skill.Target, false, 1)

        ch.fighting = victim

        act("\$n changes \$s target to \$N!", ch, null, victim, TO_NOTVICT)
        act("You change your target to \$N!", ch, null, victim, TO_CHAR)
        act("\$n changes target to you!", ch, null, victim, TO_VICT)
        return
    }

    send_to_char("You tried, but you couldn't. But for honour try again!.\n", ch)
    check_improve(ch, Skill.Target, false, 1)

}


fun do_tiger(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.tiger_power, false, 0, null)) {
        return
    }

    var chance = get_skill(ch, Skill.tiger_power)
    if (chance == 0) {
        send_to_char("You called fizzled in the space...\n", ch)
        return
    }

    act("\$n calls the power of 10 tigers!.", ch, null, null, TO_ROOM)

    if (IS_AFFECTED(ch, AFF_BERSERK) || is_affected(ch, Skill.berserk) ||
            is_affected(ch, Skill.tiger_power) || is_affected(ch, Skill.frenzy)) {
        send_to_char("You get a little madder.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CALM)) {
        send_to_char("You're feeling too mellow to call 10 tigers.\n", ch)
        return
    }
    if (ch.room.sector_type != SECT_FIELD &&
            ch.room.sector_type != SECT_FOREST &&
            ch.room.sector_type != SECT_MOUNTAIN &&
            ch.room.sector_type != SECT_HILLS) {
        send_to_char("No tigers can hear your call.\n", ch)
        return
    }


    if (ch.mana < 50) {
        send_to_char("You can't get up enough energy.\n", ch)
        return
    }

    /* modifiers */

    /* fighting */
    if (ch.position == Pos.Fighting) {
        chance += 10
    }

    val hp_percent = 100 * ch.hit / ch.max_hit
    chance += 25 - hp_percent / 2

    if (randomPercent() < chance) {

        WAIT_STATE(ch, PULSE_VIOLENCE)
        ch.mana -= 50
        ch.move /= 2

        /* heal a little damage */
        ch.hit += ch.level * 2
        ch.hit = Math.min(ch.hit, ch.max_hit)

        send_to_char("10 tigers come for your call, as you call them!\n", ch)
        act("10 tigers come across \$n , and connect with \$n.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.tiger_power, true, 2)

        val af = Affect()
        af.type = Skill.tiger_power
        af.level = ch.level
        af.duration = number_fuzzy(ch.level / 8)
        af.modifier = Math.max(1, ch.level / 5)
        af.bitvector = AFF_BERSERK

        af.location = Apply.Hitroll
        affect_to_char(ch, af)

        af.location = Apply.Damroll
        affect_to_char(ch, af)

        af.modifier = Math.max(10, 10 * (ch.level / 5))
        af.location = Apply.Ac
        affect_to_char(ch, af)
    } else {
        WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        ch.mana -= 25
        ch.move /= 2

        send_to_char("Your feel stregthen up, but nothing happens.\n", ch)
        check_improve(ch, Skill.tiger_power, false, 2)
    }
}

fun do_hara(ch: CHAR_DATA) {
    val pcdata = ch.pcdata ?: return

    if (skill_failure_check(ch, Skill.hara_kiri, false, 0, null)) {
        return
    }

    val chance = get_skill(ch, Skill.hara_kiri)
    if (chance == 0) {
        send_to_char("You try to kill yourself, but you can't resist this ache.\n", ch)
        return
    }

    /* fighting */
    if (ch.position == Pos.Fighting) {
        send_to_char("Try your chance during fighting.\n", ch)
        return
    }

    if (is_affected(ch, Skill.hara_kiri)) {
        send_to_char("One more try will kill you.\n", ch)
        return
    }

    if (randomPercent() < chance) {
        WAIT_STATE(ch, PULSE_VIOLENCE)

        ch.hit = 1
        ch.mana = 1
        ch.move = 1

        if (pcdata.condition[COND_HUNGER] < 40) {
            pcdata.condition[COND_HUNGER] = 40
        }
        if (pcdata.condition[COND_THIRST] < 40) {
            pcdata.condition[COND_THIRST] = 40
        }

        send_to_char("Yo cut your finger and wait till all your blood finishes.\n", ch)
        act("{r\$n cuts his body and look in a deadly figure.{x", ch, null, null, TO_ROOM, Pos.Fighting)
        check_improve(ch, Skill.hara_kiri, true, 2)
        do_sleep(ch, "")
        ch.act = SET_BIT(ch.act, PLR_HARA_KIRI)

        val af = Affect()
        af.type = Skill.hara_kiri
        af.level = ch.level
        af.duration = 10
        affect_to_char(ch, af)
    } else {
        WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        val af = Affect()
        af.type = Skill.hara_kiri
        af.level = ch.level
        affect_to_char(ch, af)

        send_to_char("You couldn't cut your finger.It is not so easy as you know.\n", ch)
        check_improve(ch, Skill.hara_kiri, false, 2)
    }
}

/*
 * ground strike
*/

fun ground_strike(ch: CHAR_DATA, victim: CHAR_DATA, damArg: Int): Int {
    var dam = damArg
    if (skill_failure_nomessage(ch, Skill.ground_strike, 0) != 0) {
        return dam
    }

    if (ch.room.sector_type != SECT_HILLS
            && ch.room.sector_type != SECT_MOUNTAIN
            && ch.room.sector_type != SECT_FOREST
            && ch.room.sector_type != SECT_FIELD) {
        return dam
    }

    var diceroll = number_range(0, 100)
    if (victim.level > ch.level) {
        diceroll += (victim.level - ch.level) * 2
    }
    if (victim.level < ch.level) {
        diceroll -= ch.level - victim.level
    }

    if (diceroll <= get_skill(ch, Skill.ground_strike) / 3) {
        check_improve(ch, Skill.ground_strike, true, 2)
        dam += dam * diceroll / 200
    }

    if (diceroll <= get_skill(ch, Skill.ground_strike) / 15) {
        diceroll = randomPercent()
        when {
            diceroll < 75 -> {
                act("{rThe ground underneath your feet starts moving!{x", ch, null, victim, TO_VICT, Pos.Resting)
                act("{rYou take the control of the ground underneath the feet of \$N!{x", ch, null, victim, TO_CHAR, Pos.Resting)

                check_improve(ch, Skill.ground_strike, true, 3)
                WAIT_STATE(victim, 2 * PULSE_VIOLENCE)
                dam += dam * number_range(2, 5) / 5
                return dam
            }
            diceroll in 76..94 -> {
                act("{yYou are blinded by \$n's attack!{x", ch, null, victim, TO_VICT, Pos.Resting)
                act("{yYou blind \$N with your attack!{x", ch, null, victim, TO_CHAR, Pos.Resting)

                check_improve(ch, Skill.ground_strike, true, 4)
                if (!IS_AFFECTED(victim, AFF_BLIND)) {
                    val baf = Affect()
                    baf.type = Skill.dirt_kicking
                    baf.level = ch.level
                    baf.location = Apply.Hitroll
                    baf.modifier = -4
                    baf.duration = number_range(1, 5)
                    baf.bitvector = AFF_BLIND
                    affect_to_char(victim, baf)
                }
                dam += dam * number_range(1, 2)
                return dam
            }
            else -> {
                act("{r\$C\$n cuts out your heart! OUCH!!{x", ch, null, victim, TO_VICT, Pos.Resting)
                act("{rYou cut out \$N's heart!  I bet that hurt!{x", ch, null, victim, TO_CHAR, Pos.Resting)

                check_improve(ch, Skill.ground_strike, true, 5)
                dam += dam * number_range(2, 5)
                return dam
            }
        }
    }

    return dam
}

/*
 * critical strike
*/

fun critical_strike(ch: CHAR_DATA, victim: CHAR_DATA, damArg: Int): Int {
    var dam = damArg
    if (skill_failure_nomessage(ch, Skill.critical_strike, 0) != 0) {
        return dam
    }

    if (get_wield_char(ch, false) != null
            && get_wield_char(ch, true) != null
            && randomPercent() > ch.hit * 100 / ch.max_hit) {
        return dam
    }

    var diceroll = number_range(0, 100)
    if (victim.level > ch.level) {
        diceroll += (victim.level - ch.level) * 2
    }
    if (victim.level < ch.level) {
        diceroll -= ch.level - victim.level
    }

    if (diceroll <= get_skill(ch, Skill.critical_strike) / 2) {
        check_improve(ch, Skill.critical_strike, true, 2)
        dam += dam * diceroll / 200
    }

    if (diceroll <= get_skill(ch, Skill.critical_strike) / 13) {
        diceroll = randomPercent()
        when {
            diceroll < 75 -> {
                act("{r\$n takes you down with a weird judo move!{x", ch, null, victim, TO_VICT, Pos.Resting)
                act("{rYou take \$N down with a weird judo move!{x", ch, null, victim, TO_CHAR, Pos.Resting)

                check_improve(ch, Skill.critical_strike, true, 3)
                WAIT_STATE(victim, 2 * PULSE_VIOLENCE)
                dam += dam * number_range(2, 5) / 5
                return dam
            }
            diceroll in 76..94 -> {
                act("{yYou are blinded by \$n's attack!{x", ch, null, victim, TO_VICT, Pos.Resting)
                act("{yYou blind \$N with your attack!{x", ch, null, victim, TO_CHAR, Pos.Resting)

                check_improve(ch, Skill.critical_strike, true, 4)
                if (!IS_AFFECTED(victim, AFF_BLIND)) {
                    val baf = Affect()
                    baf.type = Skill.dirt_kicking
                    baf.level = ch.level
                    baf.location = Apply.Hitroll
                    baf.modifier = -4
                    baf.duration = number_range(1, 5)
                    baf.bitvector = AFF_BLIND
                    affect_to_char(victim, baf)
                }
                dam += dam * number_range(1, 2)
                return dam
            }
            diceroll > 95 -> {
                act("{r\$n cuts out your heart! OUCH!!{x", ch, null, victim, TO_VICT, Pos.Resting)
                act("{rYou cut out \$N's heart!  I bet that hurt!{x", ch, null, victim, TO_CHAR, Pos.Resting)

                check_improve(ch, Skill.critical_strike, true, 5)
                dam += dam * number_range(2, 5)
                return dam
            }
            else -> {
            }
        }
    }

    return dam
}

fun do_shield(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.shield_cleave, false, 0, "You don't know how to cleave opponents's shield.\n")) {
        return
    }

    var chance = get_skill(ch, Skill.shield_cleave)

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    val axe = get_wield_char(ch, false)
    if (axe == null) {
        send_to_char("You must be wielding a weapon.\n", ch)
        return
    }

    val shield = get_shield_char(victim)
    if (shield == null) {
        send_to_char("Your opponent must wield a shield.\n", ch)
        return
    }

    if (axe.weaponType == WeaponType.Axe) {
        chance = (chance * 1.2).toInt()
    } else if (axe.weaponType != WeaponType.Sword) {
        send_to_char("Your weapon must be an axe or a sword.\n", ch)
        return
    }

    /* find weapon skills */
    val ch_weapon = get_weapon_skill(ch, get_weapon_sn(ch, false))
    val vict_shield = get_skill(ch, Skill.shield_block)
    /* modifiers */

    /* skill */
    chance = chance * ch_weapon / 200
    chance = chance * 100 / vict_shield

    /* dex vs. strength */
    chance += ch.curr_stat(PrimeStat.Dexterity)
    chance -= 2 * victim.curr_stat(PrimeStat.Strength)

    /* level */
    /*    chance += (ch.level - victim.level) * 2; */
    chance += ch.level - victim.level
    chance += axe.level - shield.level

    /* cleave proofness */
    if (check_material(shield, "platinum") || shield.pIndexData.limit != -1) {
        chance = -1
    }

    /* and now the attack */
    ch.affected_by = SET_BIT(ch.affected_by, AFF_WEAK_STUN)

    if (randomPercent() < chance) {
        WAIT_STATE(ch, Skill.shield_cleave.beats)
        act("You cleaved \$N's shield into two.", ch, null, victim, TO_CHAR)
        act("\$n cleaved your shield into two.", ch, null, victim, TO_VICT)
        act("\$n cleaved \$N's shield into two.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.shield_cleave, true, 1)
        extract_obj(get_shield_char(victim)!!)
    } else {
        WAIT_STATE(ch, Skill.shield_cleave.beats)
        act("You fail to cleave \$N's shield.", ch, null, victim, TO_CHAR)
        act("\$n tries to cleave your shield, but fails.", ch, null, victim, TO_VICT)
        act("\$n tries to cleave \$N's shield, but fails.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.shield_cleave, false, 1)
    }
}

fun do_weapon(ch: CHAR_DATA) {

    if (skill_failure_check(ch, Skill.shield_cleave, false, 0, "You don't know how to cleave opponents's weapon.\n")) {
        return
    }

    var chance = get_skill(ch, Skill.weapon_cleave)

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    val axe = get_wield_char(ch, false)
    if (axe == null) {
        send_to_char("You must be wielding a weapon.\n", ch)
        return
    }

    val wield = get_wield_char(victim, false)
    if (wield == null) {
        send_to_char("Your opponent must wield a weapon.\n", ch)
        return
    }

    if (axe.weaponType == WeaponType.Axe) {
        chance = (chance * 1.2).toInt()
    } else if (axe.weaponType != WeaponType.Sword) {
        send_to_char("Your weapon must be an axe or a sword.\n", ch)
        return
    }

    /* find weapon skills */
    val ch_weapon = get_weapon_skill(ch, get_weapon_sn(ch, false))
    val vict_weapon = get_weapon_skill(victim, get_weapon_sn(victim, false))
    /* modifiers */

    /* skill */
    chance = chance * ch_weapon / 200
    chance = chance * 100 / vict_weapon

    /* dex vs. strength */
    chance += ch.curr_stat(PrimeStat.Dexterity) + ch.curr_stat(PrimeStat.Strength)
    chance -= victim.curr_stat(PrimeStat.Strength) + 2 * victim.curr_stat(PrimeStat.Dexterity)

    chance += ch.level - victim.level
    chance += axe.level - wield.level

    if (check_material(wield, "platinum") || wield.pIndexData.limit != -1) {
        chance = -1
    }

    /* and now the attack */
    ch.affected_by = SET_BIT(ch.affected_by, AFF_WEAK_STUN)

    if (randomPercent() < chance) {
        WAIT_STATE(ch, Skill.weapon_cleave.beats)
        act("You cleaved \$N's weapon into two.", ch, null, victim, TO_CHAR)
        act("\$n cleaved your weapon into two.", ch, null, victim, TO_VICT)
        act("\$n cleaved \$N's weapon into two.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.weapon_cleave, true, 1)
        extract_obj(get_wield_char(victim, false)!!)
    } else {
        WAIT_STATE(ch, Skill.weapon_cleave.beats)
        act("You fail to cleave \$N's weapon.", ch, null, victim, TO_CHAR)
        act("\$n tries to cleave your weapon, but fails.", ch, null, victim, TO_VICT)
        act("\$n tries to cleave \$N's weapon, but fails.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.weapon_cleave, false, 1)
    }
}


fun do_tail(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.tail, false, OFF_TAIL, null)) {
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)

    var chance = get_skill(ch, Skill.tail)
    if (chance == 0) {
        send_to_char("You waived your tail aimlessly.\n", ch)
        return
    }

    val victim: CHAR_DATA?
    if (arg.isEmpty()) {
        victim = ch.fighting
        if (victim == null) {
            send_to_char("But you aren't fighting anyone!\n", ch)
            return
        }
    } else {
        victim = get_char_room(ch, arg.toString())
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }
    }
    /*
    if (victim.position < Pos.Fighting)
    {
    act("You'll have to let $M get back up first.",ch,null,victim,TO_CHAR);
    return;
    }
*/
    if (victim == ch) {
        send_to_char("You try to hit yourself by your tail, but failed.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("But \$N is your friend!", ch, null, victim, TO_CHAR)
        return
    }

    if (is_affected(victim, Skill.protective_shield)) {
        act("{YYour tail seems to slide around \$N.{x", ch, null, victim, TO_CHAR, Pos.Fighting)
        act("{Y\$n's tail slides off your protective shield.{x", ch, null, victim, TO_VICT, Pos.Fighting)
        act("{Y\$n's tail seems to slide around \$N.{x", ch, null, victim, TO_NOTVICT, Pos.Fighting)
        return
    }

    /* modifiers */

    /* size  and weight */
    chance -= ch.carry_weight / 20
    chance += victim.carry_weight / 25

    chance += if (ch.size < victim.size) {
        (ch.size - victim.size) * 25
    } else {
        (ch.size - victim.size) * 10
    }

    /* stats */
    chance += ch.curr_stat(PrimeStat.Strength) + ch.curr_stat(PrimeStat.Dexterity)
    chance -= victim.curr_stat(PrimeStat.Dexterity) * 2

    if (IS_AFFECTED(ch, AFF_FLYING)) {
        chance -= 10
    }

    /* speed */
    if (IS_SET(ch.off_flags, OFF_FAST)) {
        chance += 20
    }
    if (IS_SET(victim.off_flags, OFF_FAST)) {
        chance -= 30
    }

    /* level */
    chance += (ch.level - victim.level) * 2

    /* now the attack */
    if (randomPercent() < chance / 4) {

        act("\$n sends you sprawling with a powerful tail!", ch, null, victim, TO_VICT)
        act("You sprawle \$N with your tail , and send \$M flying!", ch, null, victim, TO_CHAR)
        act("\$n sends \$N sprawling with a powerful tail.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.tail, true, 1)

        val wait = when (random(4)) {
            0 -> 1
            1 -> 2
            2 -> 4
            else -> 3
        }

        WAIT_STATE(victim, wait * PULSE_VIOLENCE)
        WAIT_STATE(ch, Skill.tail.beats)
        victim.position = Pos.Resting
        val damage_tail = ch.damroll + 2 * number_range(4, 4 + ch.size * 10 + chance / 10)
        damage(ch, victim, damage_tail, Skill.tail, DT.Bash, true)

    } else {
        damage(ch, victim, 0, Skill.tail, DT.Bash, true)
        act("You lost your position and fall down!", ch, null, victim, TO_CHAR)
        act("\$n lost \$s position and fall down!.", ch, null, victim, TO_NOTVICT)
        act("You evade \$n's tail, causing \$m to fall down.", ch, null, victim, TO_VICT)
        check_improve(ch, Skill.tail, false, 1)
        ch.position = Pos.Resting
        WAIT_STATE(ch, Skill.tail.beats * 3 / 2)
    }
    if (victim.pcdata != null && ch.pcdata != null && victim.position > Pos.Stunned && ch.fighting == null) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! Someone hit me!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! %s try to hit me with its tail!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}

fun do_concentrate(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.concentrate, false, 0, "You try to concentrate on what is going on.\n")) {
        return
    }

    val chance = get_skill(ch, Skill.concentrate)
    if (chance == 0) {
        send_to_char("You try to concentrate on what is going on.\n", ch)
        return
    }

    if (is_affected(ch, Skill.concentrate)) {
        send_to_char("You are already concentrated for the fight.\n", ch)
        return
    }

    if (ch.mana < 50) {
        send_to_char("You can't get up enough energy.\n", ch)
        return
    }

    /* fighting */
    if (ch.fighting != null) {
        send_to_char("Concentrate on your fighting!\n", ch)
        return
    }

    if (randomPercent() < chance) {
        WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        ch.mana -= 50
        ch.move /= 2

        do_sit(ch, "")
        send_to_char("You sit down and relax , concentrate on the next fight.!\n", ch)
        act("{r\$n concentrates for the next fight.{x", ch, null, null, TO_ROOM, Pos.Fighting)
        check_improve(ch, Skill.concentrate, true, 2)
        val af = Affect()
        af.type = Skill.concentrate
        af.level = ch.level
        af.duration = number_fuzzy(ch.level / 8)
        af.modifier = Math.max(1, ch.level / 8)

        af.location = Apply.Hitroll
        affect_to_char(ch, af)

        af.location = Apply.Damroll
        affect_to_char(ch, af)

        af.modifier = Math.max(1, ch.level / 10)
        af.location = Apply.Ac
        affect_to_char(ch, af)
    } else {
        send_to_char("You try to concentrate for the next fight but fail.\n", ch)
        check_improve(ch, Skill.concentrate, false, 2)
    }
}


fun do_bandage(ch: CHAR_DATA) {
    val heal: Int

    if (skill_failure_check(ch, Skill.bandage, false, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.bandage)) {
        send_to_char("You have already using your bandage.\n", ch)
        return
    }


    if (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.bandage)) {

        WAIT_STATE(ch, PULSE_VIOLENCE)


        send_to_char("You place your bandage to your shoulder!\n", ch)
        act("\$n places a bandage to \$s shulder.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.bandage, true, 2)

        heal = dice(4, 8) + ch.level / 2
        ch.hit = Math.min(ch.hit + heal, ch.max_hit)
        update_pos(ch)
        send_to_char("You feel better!\n", ch)

        val af = Affect()
        af.type = Skill.bandage
        af.level = ch.level
        af.duration = ch.level / 10
        af.modifier = Math.min(15, ch.level / 2)
        af.bitvector = AFF_REGENERATION
        affect_to_char(ch, af)

    } else {
        WAIT_STATE(ch, PULSE_VIOLENCE)
        send_to_char("You failed to place your bandage to your shoulder.\n", ch)
        check_improve(ch, Skill.bandage, false, 2)
    }
}


fun do_katana(ch: CHAR_DATA, argument: String) {
    val katana: Obj
    val part: Obj?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.katana, false, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.katana)) {
        send_to_char("But you've already got one katana!\n", ch)
        return
    }

    if (ch.mana < 300) {
        send_to_char("You feel too weak to concentrate on a katana.\n", ch)
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Make a katana from what?\n", ch)
        return
    }

    part = get_obj_carry(ch, arg.toString())
    if (part == null) {
        send_to_char("You do not have chunk of iron.\n", ch)
        return
    }

    if (part.pIndexData.vnum != OBJ_VNUM_CHUNK_IRON) {
        send_to_char("You do not have the right material.\n", ch)
        return
    }

    if (randomPercent() < get_skill(ch, Skill.katana) / 3 * 2) {
        send_to_char("You failed and destroyed it.\n", ch)
        extract_obj(part)
        return
    }

    WAIT_STATE(ch, Skill.katana.beats)

    if (ch.pcdata != null && randomPercent() < get_skill(ch, Skill.katana)) {
        val af = Affect()
        af.type = Skill.katana
        af.level = ch.level
        af.duration = ch.level
        affect_to_char(ch, af)

        katana = create_object(get_obj_index_nn(OBJ_VNUM_KATANA_SWORD), ch.level)
        katana.cost = 0
        katana.level = ch.level
        ch.mana -= 300

        af.where = TO_OBJECT
        af.type = Skill.katana
        af.level = ch.level
        af.duration = -1
        af.location = Apply.Damroll
        af.modifier = ch.level / 10
        affect_to_obj(katana, af)

        af.location = Apply.Hitroll
        affect_to_obj(katana, af)

        katana.value[2] = (ch.level / 10).toLong()
        val buf = TextBuffer()
        val extra_descr = katana.pIndexData.extra_descr!!
        buf.sprintf(extra_descr.description, ch.name)
        katana.extra_desc = EXTRA_DESC_DATA()
        katana.extra_desc.keyword = extra_descr.keyword
        katana.extra_desc.description = buf.toString()
        katana.extra_desc.next = null

        obj_to_char(katana, ch)
        check_improve(ch, Skill.katana, true, 1)

        act("You make a katana from \$p!", ch, part, null, TO_CHAR)
        act("\$n makes a katana from \$p!", ch, part, null, TO_ROOM)

        extract_obj(part)
    } else {
        send_to_char("You destroyed it.\n", ch)
        extract_obj(part)
        ch.mana -= 150
        check_improve(ch, Skill.katana, false, 1)
    }
}


fun do_crush(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.crush, false, OFF_CRUSH, null)) {
        return
    }

    var chance = get_skill(ch, Skill.crush)

    val victim = ch.fighting
    if (victim == null) {
        send_to_char("You are not fighting anyone.\n", ch)
        return
    }

    if (victim.position < Pos.Fighting) {
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        return
    }

    if (is_affected(victim, Skill.protective_shield)) {
        act("{YYour crush seems to slide around \$N.{x", ch, null, victim, TO_CHAR, Pos.Fighting)
        act("{Y\$n's crush slides off your protective shield.{x", ch, null, victim, TO_VICT, Pos.Fighting)
        act("{Y\$n's crush seems to slide around \$N.{x", ch, null, victim, TO_NOTVICT, Pos.Fighting)
        return
    }

    /* modifiers */

    /* size  and weight */
    chance += ch.carry_weight / 25
    chance -= victim.carry_weight / 20

    if (ch.size < victim.size) {
        chance += (ch.size - victim.size) * 25
    } else {
        chance += (ch.size - victim.size) * 10
    }

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
    if (randomPercent() < chance) {
        act("\$n squezes you with a powerful crush!", ch, null, victim, TO_VICT)
        act("You slam into \$N, and crushes \$M!", ch, null, victim, TO_CHAR)
        act("\$n squezes \$N with a powerful crush.", ch, null, victim, TO_NOTVICT)

        val wait = when (random(4)) {
            0 -> 1
            1 -> 2
            2 -> 4
            else -> 3
        }

        WAIT_STATE(victim, wait * PULSE_VIOLENCE)
        WAIT_STATE(ch, Skill.crush.beats)
        victim.position = Pos.Resting
        var damage_crush = ch.damroll + number_range(4, 4 + ch.size * 4 + chance / 2)
        if (ch.level < 5) {
            damage_crush = Math.min(ch.level, damage_crush)
        }
        damage(ch, victim, damage_crush, Skill.crush, DT.Bash, true)

    } else {
        damage(ch, victim, 0, Skill.crush, DT.Bash, true)
        act("You fall flat on your face!", ch, null, victim, TO_CHAR)
        act("\$n falls flat on \$s face.", ch, null, victim, TO_NOTVICT)
        act("You evade \$n's crush, causing \$m to fall flat on \$s face.", ch, null, victim, TO_VICT)
        ch.position = Pos.Resting
        WAIT_STATE(ch, Skill.crush.beats * 3 / 2)
    }
}


fun do_sense(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.sense_life, true, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.sense_life)) {
        send_to_char("You can already feel life forms.\n", ch)
        return
    }

    if (ch.mana < 20) {
        send_to_char("You cannot seem to concentrate enough.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.sense_life.beats)

    val pcdata = ch.pcdata
    if (pcdata != null && randomPercent() < pcdata.learned[Skill.sense_life.ordinal]) {
        val af = Affect()
        af.type = Skill.sense_life
        af.level = ch.level
        af.duration = ch.level
        af.bitvector = AFF_DETECT_LIFE
        affect_to_char(ch, af)

        ch.mana -= 20

        act("You start to sense life forms in the room!", ch, null, null, TO_CHAR)
        act("\$n looks more sensitive.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.sense_life, true, 1)
    } else {
        ch.mana -= 10

        send_to_char("You failed.\n", ch)
        check_improve(ch, Skill.sense_life, false, 1)
    }
}


fun do_poison_smoke(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.poison_smoke, true, 0, null)) {
        return
    }

    if (ch.mana < Skill.poison_smoke.min_mana) {
        send_to_char("You can't get up enough energy.\n", ch)
        return
    }
    ch.mana -= Skill.poison_smoke.min_mana
    WAIT_STATE(ch, Skill.poison_smoke.beats)

    if (randomPercent() > get_skill(ch, Skill.poison_smoke)) {
        send_to_char("You failed.\n", ch)
        check_improve(ch, Skill.poison_smoke, false, 1)
        return
    }

    send_to_char("A cloud of poison smoke fills the room.\n", ch)
    act("A cloud of poison smoke fills the room.", ch, null, null, TO_ROOM)

    check_improve(ch, Skill.poison_smoke, true, 1)
    val buf = TextBuffer()
    for (tmp_vict in ch.room.people) {
        if (!is_safe_spell(ch, tmp_vict, true)) {
            if (ch.pcdata != null && tmp_vict != ch &&
                    ch.fighting != tmp_vict && tmp_vict.fighting != ch &&
                    (IS_SET(tmp_vict.affected_by, AFF_CHARM) || tmp_vict.pcdata != null)) {
                if (!can_see(tmp_vict, ch)) {
                    do_yell(tmp_vict, "Help someone is attacking me!")
                } else {
                    buf.sprintf("Die, %s, you sorcerous dog!", doppel_name(ch, tmp_vict))
                    do_yell(tmp_vict, buf.toString())
                }
            }

            spell_poison(ch.level, ch, tmp_vict, TARGET_CHAR)
            if (tmp_vict != ch) {
                multi_hit(tmp_vict, ch, null)
            }
        }
    }
}

fun do_blindness_dust(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.blindness_dust, true, 0, null)) {
        return
    }

    if (ch.mana < Skill.blindness_dust.min_mana) {
        send_to_char("You can't get up enough energy.\n", ch)
        return
    }

    ch.mana -= Skill.blindness_dust.min_mana
    WAIT_STATE(ch, Skill.blindness_dust.beats)

    if (randomPercent() > get_skill(ch, Skill.blindness_dust)) {
        send_to_char("You failed.\n", ch)
        check_improve(ch, Skill.blindness_dust, false, 1)
        return
    }

    send_to_char("A cloud of dust fills in the room.\n", ch)
    act("A cloud of dust fills the room.", ch, null, null, TO_ROOM)

    check_improve(ch, Skill.blindness_dust, true, 1)
    val buf = TextBuffer()
    for (tmp_vict in ch.room.people) {
        if (!is_safe_spell(ch, tmp_vict, true)) {
            if (ch.pcdata != null && tmp_vict != ch &&
                    ch.fighting != tmp_vict && tmp_vict.fighting != ch &&
                    (IS_SET(tmp_vict.affected_by, AFF_CHARM) || tmp_vict.pcdata != null)) {
                if (!can_see(tmp_vict, ch)) {
                    do_yell(tmp_vict, "Help someone is attacking me!")
                } else {
                    buf.sprintf("Die, %s, you sorcerous dog!", doppel_name(ch, tmp_vict))
                    do_yell(tmp_vict, buf.toString())
                }
            }

            spell_blindness(ch.level, ch, tmp_vict)
            if (tmp_vict != ch) {
                multi_hit(tmp_vict, ch, null)
            }
        }
    }
}


fun do_lash(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.lash, true, 0, null)) {
        return
    }

    val FightingCheck = ch.fighting != null
    val arg = StringBuilder()
    one_argument(argument, arg)
    var chance = get_skill(ch, Skill.lash)

    val victim: CHAR_DATA?
    if (arg.isEmpty()) {
        victim = ch.fighting
        if (victim == null) {
            send_to_char("But you aren't fighting anyone!\n", ch)
            return
        }
    } else {
        victim = get_char_room(ch, arg.toString())
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }
    }

    if (get_weapon_char(ch, WeaponType.Whip) == null) {
        send_to_char("You need a flail to lash.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("Are you that much stupid to lasy your body!\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("But \$N is your friend!", ch, null, victim, TO_CHAR)
        return
    }

    /* modifiers */

    /* stats */
    chance += ch.curr_stat(PrimeStat.Strength) / 2
    chance -= victim.curr_stat(PrimeStat.Dexterity) * 2

    if (IS_AFFECTED(ch, AFF_FLYING)) {
        chance += 20
    }

    /* speed */
    if (IS_AFFECTED(ch, AFF_HASTE)) {
        chance += 20
    }
    if (IS_AFFECTED(victim, AFF_HASTE)) {
        chance -= 20
    }

    if (IS_AFFECTED(ch, AFF_SLOW)) {
        chance -= 40
    }
    if (IS_AFFECTED(victim, AFF_SLOW)) {
        chance += 20
    }

    if (MOUNTED(ch) != null) {
        chance -= 20
    }
    if (MOUNTED(victim) != null) {
        chance += 40
    }

    /* level */
    chance += (ch.level - victim.level) * 2

    /* now the attack */
    if (randomPercent() < chance) {
        check_improve(ch, Skill.lash, true, 1)

        WAIT_STATE(ch, PULSE_VIOLENCE)
        WAIT_STATE(victim, Skill.lash.beats)
        val damage_lash = number_range(4, 4 + chance / 10)
        damage(ch, victim, damage_lash, Skill.lash, DT.Bash, true)
    } else {
        damage(ch, victim, 0, Skill.lash, DT.Bash, true)
        act("You failed to lash \$N!", ch, null, victim, TO_CHAR)
        act("\$n tried to lash \$N, but failed.", ch, null, victim, TO_NOTVICT)
        act("You escaped from \$n's lash!", ch, null, victim, TO_VICT)
        check_improve(ch, Skill.lash, false, 1)
        WAIT_STATE(ch, PULSE_VIOLENCE)
    }

    if (victim.pcdata != null && ch.pcdata != null && victim.position > Pos.Stunned && !FightingCheck) {
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! Someone is lashing me!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! %s is lashing me!", doppel_name(ch, victim))
            do_yell(victim, buf.toString())
        }
    }
}


fun do_claw(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?

    if (skill_failure_check(ch, Skill.claw, true, 0, null)) {
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        victim = ch.fighting
        if (victim == null) {
            send_to_char("But you aren't fighting anyone!\n", ch)
            return
        }
    } else {
        victim = get_char_room(ch, arg.toString())
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }
    }

    if (victim == ch) {
        send_to_char("You don't want to cut your head out.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && ch.master == victim) {
        act("But \$N is your friend!", ch, null, victim, TO_CHAR)
        return
    }

    if (ch.mana < 50) {
        send_to_char("You can't get up enough energy.\n", ch)
        return
    }

    /* size  and weight */
    var chance = get_skill(ch, Skill.claw)

    if (IS_AFFECTED(ch, AFF_LION)) {
        chance += 25
    }

    /* stats */
    chance += ch.curr_stat(PrimeStat.Strength) + ch.curr_stat(PrimeStat.Dexterity)
    chance -= victim.curr_stat(PrimeStat.Dexterity) * 2

    if (IS_AFFECTED(ch, AFF_FLYING)) {
        chance -= 10
    }

    /* speed */
    if (IS_AFFECTED(ch, AFF_HASTE)) {
        chance += 10
    }
    if (IS_SET(victim.off_flags, OFF_FAST) || IS_AFFECTED(victim, AFF_HASTE)) {
        chance -= 20
    }

    /* level */
    chance += (ch.level - victim.level) * 2

    /* now the attack */
    if (randomPercent() < chance) {
        ch.mana -= 50
        check_improve(ch, Skill.claw, true, 1)
        victim.position = Pos.Resting

        var damage_claw = ch.size * 10
        damage_claw += if (IS_AFFECTED(ch, AFF_LION)) {
            WAIT_STATE(ch, Skill.claw.beats / 2)
            dice(ch.level, 12) + ch.damroll
        } else {
            WAIT_STATE(ch, Skill.claw.beats)
            dice(ch.level, 24) + ch.damroll
        }

        damage(ch, victim, damage_claw, Skill.claw, DT.Bash, true)
    } else {
        ch.mana -= 25
        damage(ch, victim, 0, Skill.claw, DT.Bash, true)
        check_improve(ch, Skill.claw, false, 1)
        ch.position = Pos.Resting
        WAIT_STATE(ch, Skill.claw.beats / 2)
    }
}
