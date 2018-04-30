package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.act.spell.spell_poison
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Pos
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.RIDDEN
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.flipCoin
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.oneChanceOf
import net.sf.nightworks.util.random


typealias SPEC_FUN = (ch: CHAR_DATA) -> Boolean

fun spec_name(fn: SPEC_FUN?): String {
    //todo:
    return "TODO: " + fn
}

//TODO: ensure 2 invocation returns the same reference */
fun spec_lookup(name: String): SPEC_FUN = when (name) {
    "spec_breath_any" -> ::spec_breath_any
    "spec_breath_acid" -> ::spec_breath_acid
    "spec_breath_fire" -> ::spec_breath_fire
    "spec_breath_frost" -> ::spec_breath_frost
    "spec_breath_gas" -> ::spec_breath_gas
    "spec_breath_lightning" -> ::spec_breath_lightning
    "spec_cast_adept" -> ::spec_cast_adept
    "spec_cast_cleric" -> ::spec_cast_cleric
    "spec_cast_judge" -> ::spec_cast_judge
    "spec_cast_mage" -> ::spec_cast_mage
    "spec_cast_beholder" -> ::spec_cast_beholder
    "spec_cast_undead" -> ::spec_cast_undead
    "spec_executioner" -> ::spec_executioner
    "spec_fido" -> ::spec_fido
    "spec_guard" -> ::spec_guard
    "spec_janitor" -> ::spec_janitor
    "spec_mayor" -> ::spec_mayor
    "spec_poison" -> ::spec_poison
    "spec_thief" -> ::spec_thief
    "spec_nasty" -> ::spec_nasty
    "spec_troll_member" -> ::spec_troll_member
    "spec_ogre_member" -> ::spec_ogre_member
    "spec_patrolman" -> ::spec_patrolman
    "spec_cast_cabal" -> ::spec_cast_cabal
    "spec_stalker" -> ::spec_stalker
    "spec_special_guard" -> ::spec_special_guard
    "spec_questmaster" -> ::spec_questmaster
    "spec_assassinater" -> ::spec_assassinater
    "spec_repairman" -> ::spec_repairman
    "spec_captain" -> ::spec_captain
    "spec_headlamia" -> ::spec_headlamia
    "spec_fight_enforcer" -> ::spec_fight_enforcer
    "spec_fight_invader" -> ::spec_fight_invader
    "spec_fight_ivan" -> ::spec_fight_ivan
    "spec_fight_seneschal" -> ::spec_fight_seneschal
    "spec_fight_powerman" -> ::spec_fight_powerman
    "spec_fight_protector" -> ::spec_fight_protector
    "spec_fight_hunter" -> ::spec_fight_hunter
    "spec_fight_lionguard" -> ::spec_fight_lionguard
    else -> { _ -> spec_not_found() }
}

private fun spec_not_found(): Boolean {
    //todo
    return false
}

private fun spec_troll_member(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch) || IS_AFFECTED(ch, AFF_CALM) || IS_AFFECTED(ch, AFF_CHARM) || ch.fighting != null) return false

    /* find an ogre to beat up */
    var victim: CHAR_DATA? = null
    var count = 0
    for (vch in ch.room.people) {
        if (vch.pcdata != null || ch == vch) {
            continue
        }
        if (vch.pIndexData.vnum == MOB_VNUM_PATROLMAN) {
            return false
        }
        if (vch.pIndexData.group == GROUP_VNUM_OGRES && ch.level > vch.level - 2 && !is_safe(ch, vch)) {
            if (number_range(0, count) == 0) {
                victim = vch
            }
            count++
        }
    }

    if (victim == null) {
        return false
    }

    /* say something, then raise hell */
    val message = when (number_range(0, 6)) {
        0 -> "\$n yells 'I've been looking for you, punk!'"
        1 -> "With a scream of rage, \$n attacks \$N."
        2 -> "\$n says 'What's slimy Ogre trash like you doing around here?'"
        3 -> "\$n cracks his knuckles and says 'Do ya feel lucky?'"
        4 -> "\$n says 'There's no cops to save you this time!'"
        5 -> "\$n says 'Time to join your brother, spud.'"
        6 -> "\$n says 'Let's rock.'"
        else -> null
    }

    if (message != null) {
        act(message, ch, null, victim, TO_ALL)
    }
    multi_hit(ch, victim, null)
    return true
}


private fun spec_ogre_member(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch) || IS_AFFECTED(ch, AFF_CALM) || IS_AFFECTED(ch, AFF_CHARM) || ch.fighting != null) {
        return false
    }

    /* find an troll to beat up */
    var victim: CHAR_DATA? = null
    var count = 0
    for (vch in ch.room.people) {
        if (vch.pcdata != null || ch == vch) {
            continue
        }

        if (vch.pIndexData.vnum == MOB_VNUM_PATROLMAN) {
            return false
        }

        if (vch.pIndexData.group == GROUP_VNUM_TROLLS && ch.level > vch.level - 2 && !is_safe(ch, vch)) {
            if (number_range(0, count) == 0) {
                victim = vch
            }

            count++
        }
    }

    if (victim == null) {
        return false
    }

    /* say something, then raise hell */
    val message = when (number_range(0, 6)) {
        0 -> "\$n yells 'I've been looking for you, punk!'"
        1 -> "With a scream of rage, \$n attacks \$N.'"
        2 -> "\$n says 'What's Troll filth like you doing around here?'"
        3 -> "\$n cracks his knuckles and says 'Do ya feel lucky?'"
        4 -> "\$n says 'There's no cops to save you this time!'"
        5 -> "\$n says 'Time to join your brother, spud.'"
        6 -> "\$n says 'Let's rock.'"
        else -> null
    }

    if (message != null) {
        act(message, ch, null, victim, TO_ALL)
    }
    multi_hit(ch, victim, null)
    return true
}

private fun spec_patrolman(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch) || IS_AFFECTED(ch, AFF_CALM) || IS_AFFECTED(ch, AFF_CHARM) || ch.fighting != null) {
        return false
    }

    /* look for a fight in the room */
    var victim: CHAR_DATA? = null
    var count = 0
    for (vch in ch.room.people) {
        if (vch == ch) {
            continue
        }

        val ch_fighting = vch.fighting
        if (ch_fighting != null) { /* break it up! */
            if (number_range(0, count) == 0) {
                victim = if (vch.level > ch_fighting.level) vch else ch_fighting
            }
            count++
        }
    }

    if (victim == null || (victim.pcdata == null && victim.spec_fun === ch.spec_fun)) {
        return false
    }

    val message = when (number_range(0, 6)) {
        0 -> "\$n yells 'All roit! All roit! break it up!'"
        1 -> "\$n says 'Society's to blame, but what's a bloke to do?'"
        2 -> "\$n mumbles 'bloody kids will be the death of us all.'"
        3 -> "\$n shouts 'Stop that! Stop that!' and attacks."
        4 -> "\$n pulls out his billy and goes to work."
        5 -> "\$n sighs in resignation and proceeds to break up the fight."
        6 -> "\$n says 'Settle down, you hooligans!'"
        else -> null
    }

    if (message != null) {
        act(message, ch, null, null, TO_ALL)
    }

    multi_hit(ch, victim, null)
    return true
}

/*
* Core procedure for dragons.
*/
private fun dragon(ch: CHAR_DATA, spell_name: String): Boolean {
    if (ch.position != Pos.Fighting) return false

    var victim: CHAR_DATA? = null
    for (v in ch.room.people) {
        val ridden = RIDDEN(ch)
        if ((ridden != null && ridden.fighting == v || v.fighting == ch) && oneChanceOf(8)) {
            victim = v
            break
        }
    }

    if (victim == null) return false

    val sn = Skill.lookup(spell_name) ?: return false
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}

/*
* Special procedures for mobiles.
*/

private fun spec_breath_any(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    when (random(8)) {
        0 -> return spec_breath_fire(ch)
        1, 2 -> return spec_breath_lightning(ch)
        3 -> return spec_breath_gas(ch)
        4 -> return spec_breath_acid(ch)
        5, 6, 7 -> return spec_breath_frost(ch)
    }

    return false
}

private fun spec_breath_acid(ch: CHAR_DATA): Boolean = dragon(ch, "acid breath")

private fun spec_breath_fire(ch: CHAR_DATA): Boolean = dragon(ch, "fire breath")

private fun spec_breath_frost(ch: CHAR_DATA): Boolean = dragon(ch, "frost breath")

private fun spec_breath_gas(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) {
        return false
    }
    //todo: target? Skill.gas_breath.spell_fun(ch.level, ch, null, TARGET_CHAR)
    return true
}

private fun spec_breath_lightning(ch: CHAR_DATA): Boolean = dragon(ch, "lightning breath")

private fun spec_cast_adept(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch)) return false

    val victim = ch.room.people.firstOrNull { it != ch && can_see(ch, it) && flipCoin() && it.pcdata != null && it.level < 11 } ?: return false
    when (random(16)) {
        0 -> {
            act("\$n utters the word 'abrazak'.", ch, null, null, TO_ROOM)
            spell_armor(ch.level, ch, victim)
            return true
        }

        1 -> {
            act("\$n utters the word 'fido'.", ch, null, null, TO_ROOM)
            spell_bless(ch.level, ch, victim, TARGET_CHAR)
            return true
        }

        2 -> {
            act("\$n utters the words 'judicandus noselacri'.", ch, null, null, TO_ROOM)
            spell_cure_blindness(ch.level, ch, victim)
            return true
        }

        3 -> {
            act("\$n utters the words 'judicandus dies'.", ch, null, null, TO_ROOM)
            spell_cure_light(ch.level, ch, victim)
            return true
        }

        4 -> {
            act("\$n utters the words 'judicandus sausabru'.", ch, null, null, TO_ROOM)
            spell_cure_poison(ch.level, ch, victim)
            return true
        }

        5 -> {
            act("\$n utters the word 'candusima'.", ch, null, null, TO_ROOM)
            spell_refresh(ch.level, ch, victim)
            return true
        }

        6 -> {
            act("\$n utters the words 'judicandus eugzagz'.", ch, null, null, TO_ROOM)
            spell_cure_disease(ch.level, ch, victim)
        }
    }

    return false
}


private fun spec_cast_cleric(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false
    val victim: CHAR_DATA = ch.room.people.firstOrNull { it.fighting == ch && oneChanceOf(4) } ?: return false
    mob_cast_cleric(ch, victim)
    return true
}

private fun spec_cast_judge(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false
    val victim: CHAR_DATA = ch.room.people.firstOrNull { it.fighting == ch && oneChanceOf(4) } ?: return false
    Skill.high_explosive.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}


private fun spec_cast_mage(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false
    val victim = ch.room.people.firstOrNull { it.fighting == ch && oneChanceOf(4) } ?: return false
    mob_cast_mage(ch, victim)
    return true
}

private fun spec_cast_undead(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false
    val victim = ch.room.people.firstOrNull { it.fighting == ch && oneChanceOf(4) } ?: return false
    var sn: Skill
    while (true) {
        val min_level: Int
        when (random(16)) {
            0 -> {
                min_level = 0
                sn = Skill.curse
            }
            1 -> {
                min_level = 3
                sn = Skill.weaken
            }
            2 -> {
                min_level = 6
                sn = Skill.chill_touch
            }
            3 -> {
                min_level = 9
                sn = Skill.blindness
            }
            4 -> {
                min_level = 12
                sn = Skill.poison
            }
            5 -> {
                min_level = 15
                sn = Skill.energy_drain
            }
            6 -> {
                min_level = 18
                sn = Skill.harm
            }
            7 -> {
                min_level = 21
                sn = Skill.teleport
            }
            8 -> {
                min_level = 20
                sn = Skill.plague
            }
            else -> {
                min_level = 18
                sn = Skill.harm
            }
        }
        if (ch.level >= min_level) {
            break
        }
    }
    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}

private fun spec_executioner(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch) || ch.fighting != null) return false

    val victim = ch.room.people.firstOrNull { it.pcdata != null && IS_SET(it.act, PLR_WANTED) && can_see(ch, it) } ?: return false
    ch.comm = REMOVE_BIT(ch.comm, COMM_NOSHOUT)
    do_yell(ch, "${victim.name} is a CRIMINAL!  PROTECT THE INNOCENT!  MORE BLOOOOD!!!")
    multi_hit(ch, victim, null)
    return true
}

private fun spec_fido(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch)) return false

    for (corpse in ch.room.objects) {
        if (corpse.item_type == ItemType.CorpseNpc) {
            act("\$n savagely devours a corpse.", ch, null, null, TO_ROOM)
            for (obj in corpse.contains) {
                obj_from_obj(obj)
                obj_to_room(obj, ch.room)
            }
            extract_obj(corpse)
            return true
        }
    }
    return false
}

private fun spec_janitor(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch)) return false
    for (trash in ch.room.objects) {
        if (!IS_SET(trash.wear_flags, ITEM_TAKE) || !can_loot(ch, trash)) {
            continue
        }
        if (trash.item_type === ItemType.DrinkContainer
                || trash.item_type === ItemType.Trash
                || trash.cost < 10) {
            act("\$n picks up some trash.", ch, null, null, TO_ROOM)
            obj_from_room(trash)
            obj_to_char(trash, ch)
            if (IS_SET(trash.progtypes, OPROG_GET)) {
                val f2 = trash.pIndexData.oprogs.get_prog
                f2?.invoke(trash, ch)
            }
            return true
        }
    }

    return false
}

private object spec_mayor_data {
    val open_path = "W3a3003b000c000d111Oe333333Oe22c222112212111a1S."
    val close_path = "W3a3003b000c000d111CE333333CE22c222112212111a1S."

    var path: String? = null
    var pos: Int = 0
    var move: Boolean = false
}

private fun spec_mayor(ch: CHAR_DATA): Boolean {
    if (!spec_mayor_data.move) {
        if (time_info.hour == 6) {
            spec_mayor_data.path = spec_mayor_data.open_path
            spec_mayor_data.move = true
            spec_mayor_data.pos = 0
        }

        if (time_info.hour == 20) {
            spec_mayor_data.path = spec_mayor_data.close_path
            spec_mayor_data.move = true
            spec_mayor_data.pos = 0
        }
    }

    if (!spec_mayor_data.move || ch.position < Pos.Sleeping) {
        return false
    }

    when (spec_mayor_data.path!![spec_mayor_data.pos]) {
        '0', '1', '2', '3' -> move_char(ch, spec_mayor_data.path!![spec_mayor_data.pos] - '0')

        'W' -> {
            ch.position = Pos.Standing
            act("\$n awakens and groans loudly.", ch, null, null, TO_ROOM)
        }

        'S' -> {
            ch.position = Pos.Sleeping
            act("\$n lies down and falls asleep.", ch, null, null, TO_ROOM)
        }

        'a' -> do_say(ch, "Hello Honey!")

        'b' -> do_say(ch, "What a view!  I must do something about that dump!")

        'c' -> do_say(ch, "Vandals  Youngsters have no respect for anything!")

        'd' -> do_say(ch, "Good day, citizens!")

        'e' -> do_say(ch, "I hereby declare the city of Midgaard open!")

        'E' -> do_say(ch, "I hereby declare the city of Midgaard closed!")

        'O' -> {
            do_unlock(ch, "gate")
            do_open(ch, "gate")
            interpret(ch, "emote unlocks the gate key from the gate.", false)
            val key = ch.room.objects.firstOrNull { it.pIndexData.vnum == 3379 }
            if (key != null) {
                key.wear_flags = SET_BIT(key.wear_flags, ITEM_TAKE)
            }
            do_get(ch, "gatekey")
        }

        'C' -> {
            do_close(ch, "gate")
            do_lock(ch, "gate")
            do_drop(ch, "key")
            interpret(ch, "emote locks the gate key to the gate, with chain.", false)
            val key = ch.room.objects.firstOrNull { it.pIndexData.vnum == 3379 }
            if (key != null) {
                key.wear_flags = REMOVE_BIT(key.wear_flags, ITEM_TAKE)
            }
        }

        '.' -> spec_mayor_data.move = false
    }
    spec_mayor_data.pos++
    return false
}


private fun spec_poison(ch: CHAR_DATA): Boolean {
    val victim = ch.fighting
    if (ch.position != Pos.Fighting || victim == null || randomPercent() > 2 * ch.level) {
        return false
    }

    act("You bite \$N!", ch, null, victim, TO_CHAR)
    act("\$n bites \$N!", ch, null, victim, TO_NOTVICT)
    act("\$n bites you!", ch, null, victim, TO_VICT)
    spell_poison(ch.level, ch, victim, TARGET_CHAR)
    return true
}


private fun spec_thief(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Standing) {
        return false
    }

    for (victim in ch.room.people) {
        if (victim.pcdata == null
                || victim.level >= LEVEL_IMMORTAL
                || oneChanceOf(32)
                || !can_see(ch, victim)) {
            continue
        }

        if (IS_AWAKE(victim) && number_range(0, ch.level) == 0) {
            act("You discover \$n's hands in your wallet!", ch, null, victim, TO_VICT)
            act("\$N discovers \$n's hands in \$S wallet!", ch, null, victim, TO_NOTVICT)
        } else {
            var gold = victim.gold * Math.min(number_range(1, 20), ch.level / 2) / 100
            gold = Math.min(gold, ch.level * ch.level * 10)
            ch.gold += gold
            victim.gold -= gold
            var silver = victim.silver * Math.min(number_range(1, 20), ch.level / 2) / 100
            silver = Math.min(silver, ch.level * ch.level * 25)
            ch.silver += silver
            victim.silver -= silver
        }
        return true
    }
    return false
}


private fun spec_cast_cabal(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch)) return false
    val victim = ch.room.people.firstOrNull { it != ch && can_see(ch, it) && flipCoin() } ?: return false
    when (random(16)) {
        0 -> {
            act("\$n utters the word 'abracal'.", ch, null, null, TO_ROOM)
            spell_armor(ch.level, ch, victim)
            return true
        }

        1 -> {
            act("\$n utters the word 'balc'.", ch, null, null, TO_ROOM)
            spell_bless(ch.level, ch, victim, TARGET_CHAR)
            return true
        }

        2 -> {
            act("\$n utters the word 'judicandus noselacba'.", ch, null, null, TO_ROOM)
            spell_cure_blindness(ch.level, ch, victim)
            return true
        }

        3 -> {
            act("\$n utters the word 'judicandus bacla'.", ch, null, null, TO_ROOM)
            spell_cure_light(ch.level, ch, victim)
            return true
        }

        4 -> {
            act("\$n utters the words 'judicandus sausabcla'.", ch, null, null, TO_ROOM)
            spell_cure_poison(ch.level, ch, victim)
            return true
        }

        5 -> {
            act("\$n utters the words 'candabala'.", ch, null, null, TO_ROOM)
            spell_refresh(ch.level, ch, victim)
            return true
        }
    }

    return false
}

private fun spec_guard(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch) || ch.fighting != null) return false

    var ech: CHAR_DATA? = null
    var victim: CHAR_DATA? = null
    for (v in ch.room.people) {
        if (!can_see(ch, v)) {
            continue
        }
        if (IS_SET(ch.room.area.area_flag, AREA_HOMETOWN) && randomPercent() < 2) {
            do_say(ch, "Do i know you?.")
            if (!eq(ch.room.area.name, v.hometown.displayName)) {
                do_say(ch, "I don't remember you. Go away!")
            } else {
                do_say(ch, "Ok, my dear. I have just remembered.")
                interpret(ch, "smile", false)
            }
        }

        if (v.pcdata != null && IS_SET(v.act, PLR_WANTED)) {
            victim = v
            break
        }

        val v_fighting = v.fighting
        if (v_fighting != null && v_fighting != ch && !v.isLawful() && !v.isGood() && !v_fighting.isEvil()) {
            ech = v
            break
        }
    }

    if (victim != null) {
        do_yell(ch, "${victim.name} is a CRIMINAL!  PROTECT THE INNOCENT!!  BANZAI!!")
        multi_hit(ch, victim, null)
        return true
    }

    if (ech != null) {
        act("\$n screams 'PROTECT THE INNOCENT!!  BANZAI!!", ch, null, null, TO_ROOM)
        multi_hit(ch, ech, null)
        return true
    }

    return false
}


private fun spec_special_guard(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch) || ch.fighting != null) return false

    var ech: CHAR_DATA? = null

    var victim: CHAR_DATA? = null
    for (v in ch.room.people) {
        if (!can_see(ch, v)) {
            continue
        }

        if (v.pcdata != null && IS_SET(v.act, PLR_WANTED)) {
            victim = v
            break
        }

        val v_fighting = v.fighting
        if (v_fighting != null && v_fighting != ch && v_fighting.cabal == Clan.Ruler) {
            ech = v
            break
        }
    }

    if (victim != null) {
        do_yell(ch, "${victim.name} is a CRIMINAL!  PROTECT THE INNOCENT!!  BANZAI!!")
        multi_hit(ch, victim, null)
        return true
    }

    if (ech != null) {
        act("\$n screams 'PROTECT THE INNOCENT!!  BANZAI!!", ch, null, null, TO_ROOM)
        multi_hit(ch, ech, null)
        return true
    }

    return false
}

private fun spec_stalker(ch: CHAR_DATA): Boolean {
    if (ch.fighting != null) return false

    if (ch.status == 10) {
        ch.cabal = Clan.Ruler
        do_cb(ch, "I have killed my victim, now I can leave the realms.")
        extract_char(ch, true)
        return true
    }

    val victim = ch.last_fought
    if (victim == null) {
        ch.cabal = Clan.Ruler
        do_cb(ch, "To their shame, my victim has cowardly left the game. I must leave also.")
        extract_char(ch, true)
        return true
    }

    val i = when {
        victim.isGood() -> 0
        victim.isEvil() -> 2
        else -> 1
    }

    for (wch in ch.room.people) {
        if (victim == wch) {
            do_yell(ch, victim.name + ", you criminal! Now you die!")
            multi_hit(ch, wch, null)
            return true
        }
    }
    do_track(ch, victim.name)

    return when {
        ch.status == 5 -> false
        ch.room != get_room_index(victim.hometown.recall[1]) -> {
            char_from_room(ch)
            char_to_room(ch, get_room_index(victim.hometown.recall[i]))
            do_track(ch, victim.name)
            true
        }
        else -> {
            ch.cabal = Clan.Ruler
            do_cb(ch, "To my shame I have lost track of " + victim.name + ".  I must leave.")
            extract_char(ch, true)
            true
        }
    }
}

private fun spec_nasty(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch)) return false

    if (ch.position != Pos.Fighting) {
        for (victim in ch.room.people) {
            if (victim.pcdata != null && victim.level > ch.level && victim.level < ch.level + 10) {
                do_backstab(ch, victim.name)
                if (ch.position != Pos.Fighting) {
                    do_murder(ch, victim.name)
                }
                /* should steal some coins right away? :) */
                return true
            }
        }
        return false    /*  No one to attack */
    }

    /* okay, we must be fighting.... steal some coins and flee */
    val victim = ch.fighting ?: return false   /* let's be paranoid.... */

    when (random(4)) {
        0 -> {
            act("\$n rips apart your coin purse, spilling your gold!", ch, null, victim, TO_VICT)
            act("You slash apart \$N's coin purse and gather his gold.", ch, null, victim, TO_CHAR)
            act("\$N's coin purse is ripped apart!", ch, null, victim, TO_NOTVICT)
            val gold = (victim.gold / 10).toLong()  /* steal 10% of his gold */
            victim.gold -= gold.toInt()
            ch.gold += gold.toInt()
            return true
        }
        1 -> {
            do_flee(ch)
            return true
        }

        else -> return false
    }
}

private fun spec_questmaster(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch)) return false
    if (number_range(0, 100) == 0) {
        do_say(ch, "Don't you want a quest???.")
        return true
    }
    return false
}

private fun spec_assassinater(ch: CHAR_DATA): Boolean {
    if (ch.fighting != null) return false


    /* thieves & ninjas*/
    val victim = ch.room.people.firstOrNull { it.clazz !== Clazz.THIEF && it.clazz !== Clazz.NINJA }
    if (victim == null || victim == ch || IS_IMMORTAL(victim)) {
        return false
    }
    if (victim.level > ch.level + 7 || victim.pcdata == null) {
        return false
    }
    if (victim.hit < victim.max_hit) {
        return false
    }

    val rnd_say = number_range(1, 40)

    val buf = when (rnd_say) {
        5 -> "Death to is the true end..."
        6 -> "Time to die...."
        7 -> "Cabrone...."
        8 -> "Welcome to your fate...."
        9 -> "A sacrifice to immortals.. "
        10 -> "Ever dance with the devil...."
        else -> return false
    }
    do_say(ch, buf)
    multi_hit(ch, victim, Skill.assassinate)
    return true
}


private fun spec_repairman(ch: CHAR_DATA): Boolean {
    if (!IS_AWAKE(ch)) return false
    if (number_range(0, 100) == 0) {
        do_say(ch, "Now it is time to repair the other equipments.")
        return true
    }
    return false
}

private object spec_captain_data {
    val open_path = "Wn0onc0oe1f2212211s2tw3xw3xd3322a22b22yO00d00a0011e1fe1fn0o3300300w3xs2ts2tS."
    val close_path = "Wn0on0oe1f2212211s2twc3xw3x3322d22a22EC0a00d0b0011e1fe1fn0o3300300w3xs2ts2tS."
    var path: String? = null
    var pos: Int = 0
    var move: Boolean = false
}

private fun spec_captain(ch: CHAR_DATA): Boolean {
    if (!spec_captain_data.move) {
        if (time_info.hour == 6) {
            spec_captain_data.path = spec_captain_data.open_path
            spec_captain_data.move = true
            spec_captain_data.pos = 0
        }

        if (time_info.hour == 20) {
            spec_captain_data.path = spec_captain_data.close_path
            spec_captain_data.move = true
            spec_captain_data.pos = 0
        }
    }

    if (ch.fighting != null) {
        return spec_cast_cleric(ch)
    }

    if (!spec_captain_data.move || ch.position < Pos.Sleeping) {
        return false
    }

    when (spec_captain_data.path!![spec_captain_data.pos]) {
        '0', '1', '2', '3' -> move_char(ch, spec_captain_data.path!![spec_captain_data.pos] - '0')

        'W' -> {
            ch.position = Pos.Standing
            act("{W\$n awakens suddenly and yawns.{x", ch, null, null, TO_ROOM, Pos.Resting)
        }

        'S' -> {
            ch.position = Pos.Sleeping
            act("{W\$n lies down and falls asleep.{x", ch, null, null, TO_ROOM, Pos.Resting)
        }

        'a' -> act("{Y\$n says 'Greetings! Good Hunting to you!'{x", ch, null, null, TO_ROOM, Pos.Resting)

        'b' -> act("{Y\$n says 'Keep the streets clean please. Keep Solace tidy.'{x", ch, null, null, TO_ROOM, Pos.Resting)

        'c' -> {
            act("{Y\$n says 'I must do something about all these doors.{x", ch, null, null, TO_ROOM, Pos.Resting)
            act("{Y\$n says, 'I will never get out of here.'{x", ch, null, null, TO_ROOM, Pos.Resting)
        }

        'd' -> act("{Y\$n says 'Salutations Citizens of Solace!'{x", ch, null, null, TO_ROOM, Pos.Resting)

        'y' -> act("{Y\$n says 'I hereby declare the city of Solace open!'{x", ch, null, null, TO_ROOM, Pos.Resting)

        'E' -> act("{Y\$n says 'I hereby declare the city of Solace closed!'{x", ch, null, null, TO_ROOM, Pos.Resting)

        'O' -> {
            do_unlock(ch, "gate")
            do_open(ch, "gate")
        }

        'C' -> {
            do_close(ch, "gate")
            do_lock(ch, "gate")
        }

        'n' -> do_open(ch, "north")

        'o' -> do_close(ch, "south")

        's' -> do_open(ch, "south")

        't' -> do_close(ch, "north")

        'e' -> do_open(ch, "east")

        'f' -> do_close(ch, "west")

        'w' -> do_open(ch, "west")

        'x' -> do_close(ch, "east")

        '.' -> spec_captain_data.move = false
    }

    spec_captain_data.pos++
    return false
}


private object spec_headlamia_data {
    val path = "T111111100003332222232211."
    var pos = 0
    var move: Boolean = false
    var count = 0
}

private fun spec_headlamia(ch: CHAR_DATA): Boolean {
    if (!spec_headlamia_data.move) {
        if (spec_headlamia_data.count++ == 10000) {
            spec_headlamia_data.move = true
        }
    }

    if (ch.position < Pos.Sleeping || ch.fighting != null) return false

    for (vch in ch.room.people) {
        if (vch.pcdata != null && vch.pIndexData.vnum == 3143) {
            do_kill(ch, vch.name)
            break
        }
    }

    if (!spec_headlamia_data.move) return false

    when (spec_headlamia_data.path[spec_headlamia_data.pos]) {
        '0', '1', '2', '3' -> {
            move_char(ch, spec_headlamia_data.path[spec_headlamia_data.pos] - '0')
            spec_headlamia_data.pos++
        }

        'T' -> {
            spec_headlamia_data.pos++
            for (vch2 in Index.CHARS) {
                if (vch2.pcdata == null) {
                    continue
                }
                if (vch2.pIndexData.vnum == 5201) {
                    if (vch2.fighting == null && vch2.last_fought == null) {
                        char_from_room(vch2)
                        char_to_room(vch2, ch.room)
                        vch2.master = ch
                        vch2.leader = ch
                    }
                }
            }
        }

        '.' -> {
            spec_headlamia_data.move = false
            spec_headlamia_data.count = 0
            spec_headlamia_data.pos = 0
        }
    }

    return false
}


private fun spec_cast_beholder(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) {
        return false
    }

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    val sn = when (dice(1, 16)) {
        0 -> Skill.fear
        1 -> Skill.fear
        2 -> Skill.slow
        3 -> Skill.cause_serious
        4 -> Skill.cause_critical
        5 -> Skill.harm
        6 -> Skill.harm
        7 -> Skill.dispel_magic
        8 -> Skill.dispel_magic
        else -> return false
    }

    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}

private fun spec_fight_enforcer(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    val sn = when (dice(1, 16)) {
        0, 1 -> Skill.dispel_magic
        2, 3 -> Skill.acid_arrow
        4, 5 -> Skill.caustic_font
        6, 7, 8, 9, 10 -> Skill.acid_blast
        else -> return false
    }
    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}


private fun spec_fight_invader(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    val sn = when (dice(1, 16)) {
        0, 1 -> Skill.blindness
        2, 3 -> Skill.dispel_magic
        4, 5 -> Skill.weaken
        6, 7 -> Skill.energy_drain
        8, 9 -> Skill.plague
        10, 11 -> Skill.acid_arrow
        12, 13, 14 -> Skill.acid_blast
        15 -> if (ch.hit < ch.max_hit / 3) Skill.shadow_cloak else return false
        else -> return false
    }

    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}


private fun spec_fight_ivan(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    val sn = when (dice(1, 16)) {
        0, 1 -> Skill.dispel_magic
        2, 3 -> Skill.acid_arrow
        4, 5 -> Skill.caustic_font
        6, 7, 8 -> Skill.acid_blast
        9 -> Skill.disgrace
        10 -> if (ch.hit < ch.max_hit / 3) Skill.garble else return false
        else -> return false
    }
    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}


private fun spec_fight_seneschal(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    val sn = when (dice(1, 16)) {
        0 -> Skill.blindness
        1 -> Skill.dispel_magic
        2 -> Skill.weaken
        3 -> Skill.blindness
        4 -> Skill.acid_arrow
        5 -> Skill.caustic_font
        6 -> Skill.energy_drain
        7, 8, 9 -> Skill.acid_blast
        10 -> Skill.plague
        11 -> Skill.acid_blast
        12, 13 -> Skill.lightning_breath
        14, 15 -> Skill.mental_knife
        else -> return false
    }
    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}

private fun spec_fight_powerman(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    ch.cabal = Clan.BattleRager

    if (!is_affected(ch, Skill.spellbane)) do_spellbane(ch)

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    if (randomPercent() < 33) {
        act("You deliver triple blows of deadly force!", ch, null, null, TO_CHAR)
        act("\$n delivers triple blows of deadly force!", ch, null, null, TO_ROOM)
        one_hit(ch, victim, null, false)
        one_hit(ch, victim, null, false)
        one_hit(ch, victim, null, false)
    }

    if (!is_affected(ch, Skill.resistance)) {
        do_resistance(ch)
    }

    if (ch.hit < ch.max_hit / 3 && !IS_AFFECTED(ch, AFF_REGENERATION)) {
        do_bandage(ch)
    }

    return true
}


private fun spec_fight_protector(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    val sn = when (dice(1, 16)) {
        0, 1 -> Skill.dispel_magic
        2, 3 -> Skill.acid_arrow
        4, 5 -> Skill.caustic_font
        6, 7, 8, 9, 10 -> Skill.acid_blast
        else -> return false
    }
    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}

private fun spec_fight_lionguard(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false
    if (randomPercent() < 33) {
        val damage_claw = dice(ch.level, 24) + ch.damroll
        damage(ch, victim, damage_claw, Skill.claw, DT.Bash, true)
        return true
    }

    val sn = when (dice(1, 16)) {
        0, 1 -> Skill.dispel_magic
        2, 3 -> Skill.acid_blast
        4, 5 -> Skill.caustic_font
        6, 7, 8 -> Skill.acid_arrow
        else -> return false
    }

    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}


private fun spec_fight_hunter(ch: CHAR_DATA): Boolean {
    if (ch.position != Pos.Fighting) return false

    val victim = ch.room.people.firstOrNull { it.fighting == ch && flipCoin() } ?: return false

    val sn = when (dice(1, 16)) {
        0, 1 -> Skill.dispel_magic
        2, 3 -> Skill.acid_arrow
        4, 5 -> Skill.caustic_font
        6, 7, 8, 9 -> Skill.acid_blast
        else -> return false
    }

    say_spell(ch, sn)
    sn.spell_fun(ch.level, ch, victim, TARGET_CHAR)
    return true
}
