package net.sf.nightworks.act.obj

import net.sf.nightworks.AFF_BERSERK
import net.sf.nightworks.AFF_PLAGUE
import net.sf.nightworks.AFF_POISON
import net.sf.nightworks.DT
import net.sf.nightworks.PULSE_VIOLENCE
import net.sf.nightworks.SECT_FIELD
import net.sf.nightworks.SECT_FOREST
import net.sf.nightworks.SECT_HILLS
import net.sf.nightworks.SECT_MOUNTAIN
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.act.spell.spell_holy_word
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.damage
import net.sf.nightworks.do_say
import net.sf.nightworks.fire_effect
import net.sf.nightworks.get_eq_char
import net.sf.nightworks.get_shield_char
import net.sf.nightworks.get_wield_char
import net.sf.nightworks.is_affected
import net.sf.nightworks.is_wielded_char
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.Wear
import net.sf.nightworks.obj_cast_spell
import net.sf.nightworks.one_hit
import net.sf.nightworks.raw_kill_org
import net.sf.nightworks.send_to_char
import net.sf.nightworks.spell_bluefire
import net.sf.nightworks.spell_cure_disease
import net.sf.nightworks.spell_cure_poison
import net.sf.nightworks.spell_dispel_evil
import net.sf.nightworks.spell_dispel_good
import net.sf.nightworks.spell_lightning_bolt
import net.sf.nightworks.spell_scream
import net.sf.nightworks.spell_web
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.oneChanceOf
import net.sf.nightworks.util.random
import net.sf.nightworks.util.randomPercent

fun create_fight_prog(name: String) = when (name) {
    "fight_prog_ranger_staff" -> ::fight_prog_ranger_staff
    "fight_prog_sub_weapon" -> ::fight_prog_sub_weapon
    "fight_prog_chaos_blade" -> ::fight_prog_chaos_blade
    "fight_prog_tattoo_apollon" -> ::fight_prog_tattoo_apollon
    "fight_prog_tattoo_zeus" -> ::fight_prog_tattoo_zeus
    "fight_prog_tattoo_siebele" -> ::fight_prog_tattoo_siebele
    "fight_prog_tattoo_ahrumazda" -> ::fight_prog_tattoo_ahrumazda
    "fight_prog_tattoo_hephaestus" -> ::fight_prog_tattoo_hephaestus
    "fight_prog_tattoo_ehrumen" -> ::fight_prog_tattoo_ehrumen
    "fight_prog_tattoo_venus" -> ::fight_prog_tattoo_venus
    "fight_prog_tattoo_ares" -> ::fight_prog_tattoo_ares
    "fight_prog_tattoo_odin" -> ::fight_prog_tattoo_odin
    "fight_prog_tattoo_phobos" -> ::fight_prog_tattoo_phobos
    "fight_prog_tattoo_mars" -> ::fight_prog_tattoo_mars
    "fight_prog_tattoo_athena" -> ::fight_prog_tattoo_athena
    "fight_prog_tattoo_hera" -> ::fight_prog_tattoo_hera
    "fight_prog_tattoo_deimos" -> ::fight_prog_tattoo_deimos
    "fight_prog_tattoo_eros" -> ::fight_prog_tattoo_eros
    "fight_prog_golden_weapon" -> ::fight_prog_golden_weapon
    "fight_prog_snake" -> ::fight_prog_snake
    "fight_prog_tattoo_prometheus" -> ::fight_prog_tattoo_prometheus
    "fight_prog_shockwave" -> ::fight_prog_shockwave
    "fight_prog_firegauntlets" -> ::fight_prog_firegauntlets
    "fight_prog_armbands" -> ::fight_prog_armbands
    "fight_prog_demonfireshield" -> ::fight_prog_demonfireshield
    "fight_prog_vorbalblade" -> ::fight_prog_vorbalblade
    "fight_prog_rose_shield" -> ::fight_prog_rose_shield
    "fight_prog_lion_claw" -> ::fight_prog_lion_claw
    "fight_prog_tattoo_goktengri" -> ::fight_prog_tattoo_goktengri
    "fight_prog_ancient_gloves" -> ::fight_prog_ancient_gloves
    "fight_prog_ancient_shield" -> ::fight_prog_ancient_shield
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun fight_prog_ranger_staff(obj: Obj, ch: CHAR_DATA) {
    if (!is_wielded_char(ch, obj) || randomPercent() >= 10) return
    send_to_char("Your ranger's staff glows blue!\n", ch)
    act("\$n's ranger's staff glows blue!", ch, null, null, TO_ROOM)
    obj_cast_spell(Skill.cure_critical, ch.level, ch, ch, obj)
}

private fun fight_prog_sub_weapon(obj: Obj, ch: CHAR_DATA) {
    if (!is_wielded_char(ch, obj) || randomPercent() >= 30) return
    val k = ch.hit.toFloat() / ch.max_hit.toFloat()
    when {
        k > 0.9 -> send_to_char("Your weapon whispers, 'You're doing great!'\n", ch)
        k > 0.6 -> send_to_char("Your weapon whispers, 'Keep up the good work!'\n", ch)
        k > 0.4 -> send_to_char("Your weapon whispers, 'You can do it!'\n", ch)
        else -> send_to_char("Your weapon whispers, 'Run away! Run away!'\n", ch)
    }
}

private fun fight_prog_chaos_blade(obj: Obj, ch: CHAR_DATA) {
    if (!is_wielded_char(ch, obj)) return
    when (random(128)) {
        0 -> {
            act("The chaotic blade trembles violently!", ch, null, null, TO_ROOM)
            send_to_char("Your chaotic blade trembles violently!\n", ch)
            obj_cast_spell(Skill.mirror, ch.level, ch, ch, obj)
            WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        }

        1 -> {
            act("The chaotic blade shakes a bit.", ch, null, null, TO_ROOM)
            send_to_char("Your chaotic blade shakes a bit.\n", ch)
            obj_cast_spell(Skill.garble, ch.level, ch, ch.fighting, obj)
            WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        }

        2 -> {
            act("The chaotic blade shivers uncontrollably!", ch, null, null, TO_ROOM)
            send_to_char("Your chaotic blade shivers uncontrollably!\n", ch)
            obj_cast_spell(Skill.confuse, ch.level, ch, ch.fighting, obj)
            WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        }
    }
}

private fun fight_prog_tattoo_apollon(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0, 1 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
        2 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            do_yell(ch, "Ever dance with good....")
            spell_holy_word(ch.level, ch)
        }
    }
}

private fun fight_prog_tattoo_zeus(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0, 1, 2 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_critical, ch.level, ch, ch, obj)
        }
        3 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            if (IS_AFFECTED(ch, AFF_PLAGUE)) {
                spell_cure_disease(100, ch, ch)
            }
            if (IS_AFFECTED(ch, AFF_POISON)) {
                spell_cure_poison(100, ch, ch)
            }
        }
    }
}

private fun fight_prog_tattoo_siebele(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
        1 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            spell_bluefire(ch.level, ch, ch.fighting!!)
        }
    }
}

private fun fight_prog_tattoo_ahrumazda(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0 -> {
            act("{bThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
        1 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.demonfire, ch.level, ch, ch.fighting, obj)
        }
    }
}

private fun fight_prog_tattoo_hephaestus(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0, 1 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
        2 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            do_yell(ch, "And justice for all!....")
            spell_scream(ch.level, ch)
        }
    }
}

private fun fight_prog_tattoo_ehrumen(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_light, ch.level, ch, ch.fighting, obj)
        }
        1 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
        2 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            spell_dispel_evil(ch.level, ch, ch.fighting!!)
        }
    }
}

private fun fight_prog_tattoo_venus(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(128)) {
        0, 1, 2 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_light, ch.level, ch, ch, obj)
        }
        3 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.plague, ch.level, ch, ch.fighting, obj)
        }
        4 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.bless, ch.level, ch, ch, obj)
        }
    }
}

private fun fight_prog_tattoo_ares(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(32)) {
        0 -> {
            act("{bThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.dragon_strength, ch.level, ch, ch, obj)
        }
        1 -> {
            act("{rThe tattoo on your shoulder glows RED.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.dragon_breath, ch.level, ch, ch.fighting, obj)
        }
    }
}

private fun fight_prog_tattoo_odin(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(32)) {
        0 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_critical, ch.level, ch, ch, obj)
        }
        1 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.faerie_fire, ch.level, ch, ch.fighting, obj)
        }
    }
}

private fun fight_prog_tattoo_phobos(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
        1 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.colour_spray, ch.level, ch, ch.fighting, obj)
        }
    }
}

private fun fight_prog_tattoo_mars(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(128)) {
        0 -> {
            obj_cast_spell(Skill.blindness, ch.level, ch, ch.fighting, obj)
            send_to_char("You send out a cloud of confusion!\n", ch)
        }
        1 -> {
            obj_cast_spell(Skill.poison, ch.level, ch, ch.fighting, obj)
            send_to_char("Some of your insanity rubs off on your opponent.\n", ch)
        }
        2 -> {
            obj_cast_spell(Skill.haste, ch.level, ch, ch, obj)
            send_to_char("You suddenly feel more hyperactive!\n", ch)
        }
        3 -> {
            obj_cast_spell(Skill.shield, ch.level, ch, ch, obj)
            send_to_char("You feel even more paranoid!\n", ch)
        }
    }
}

private fun fight_prog_tattoo_athena(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    if (randomPercent() >= 50) {
        when (random(16)) {
            0 -> do_yell(ch, "Cry Havoc and Let Loose the Dogs of War!")
            1 -> do_yell(ch, "No Mercy!")
            2 -> do_yell(ch, "Los Valdar Cuebiyari!")
            3 -> do_yell(ch, "Carai an Caldazar! Carai an Ellisande! Al Ellisande!")
            4 -> do_yell(ch, "Siempre Vive el Riesgo!")
        }
        return
    }
    when (random(16)) {
        0 -> {
            if (IS_AFFECTED(ch, AFF_BERSERK) || is_affected(ch, Skill.berserk) || is_affected(ch, Skill.frenzy)) {
                send_to_char("You get a little madder.\n", ch)
                return
            }

            val af = Affect()
            af.type = Skill.berserk
            af.level = ch.level
            af.duration = ch.level / 3
            af.modifier = ch.level / 5
            af.bitvector = AFF_BERSERK

            af.location = Apply.Hitroll
            affect_to_char(ch, af)

            af.location = Apply.Damroll
            affect_to_char(ch, af)

            af.modifier = 10 * (ch.level / 10)
            af.location = Apply.Ac
            affect_to_char(ch, af)

            ch.hit += ch.level * 2
            ch.hit = Math.min(ch.hit, ch.max_hit)

            send_to_char("Your pulse races as you are consumned by rage!\n", ch)
            act("\$n gets a wild look in \$s eyes.", ch, null, null, TO_ROOM)
        }
    }
}

private fun fight_prog_tattoo_hera(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(32)) {
        0 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.plague, ch.level, ch, ch.fighting, obj)
        }
        1 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.poison, ch.level, ch, ch.fighting, obj)
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.weaken, ch.level, ch, ch.fighting, obj)
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.slow, ch.level, ch, ch.fighting, obj)
        }
        2 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.weaken, ch.level, ch, ch.fighting, obj)
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.slow, ch.level, ch, ch.fighting, obj)
        }
        3 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.slow, ch.level, ch, ch.fighting, obj)
        }
    }
}

private fun fight_prog_tattoo_deimos(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(64)) {
        0, 1 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
        2 -> {
            act("{rThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            spell_web(ch.level, ch, ch.fighting!!)
        }
    }
}

private fun fight_prog_tattoo_eros(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(32)) {
        0, 1 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.heal, ch.level, ch, ch, obj)
        }
        2 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.mass_healing, ch.level, ch, ch, obj)
        }
    }
}

private fun fight_prog_golden_weapon(obj: Obj, ch: CHAR_DATA) {
    if (!is_wielded_char(ch, obj)) return
    val roll = randomPercent()
    when {
        roll < 4 -> {
            act("Your \$p glows bright blue!\n", ch, obj, null, TO_CHAR)
            act("\$n's \$p glows bright blue!", ch, obj, null, TO_ROOM)

            obj_cast_spell(Skill.cure_critical, ch.level, ch, ch, obj)
        }
        roll > 92 -> {
            act("Your \$p glows bright blue!\n", ch, obj, null, TO_CHAR)
            act("\$n's \$p glows bright blue!", ch, obj, null, TO_ROOM)

            obj_cast_spell(Skill.cure_serious, ch.level, ch, ch, obj)
        }
    }
}

private fun fight_prog_snake(obj: Obj, ch: CHAR_DATA) {
    if (!is_wielded_char(ch, obj)) return
    when (random(128)) {
        0 -> {
            act("One of the snake heads on your whip bites \$N!", ch, null, ch.fighting, TO_CHAR)
            act("A snake from \$n's whip strikes out and bites you!", ch, null, ch.fighting, TO_VICT)
            act("One of the snakes from \$n's whip strikes at \$N!", ch, null, ch.fighting, TO_NOTVICT)
            obj_cast_spell(Skill.poison, ch.level, ch, ch.fighting, obj)
        }
        1 -> {
            act("One of the snake heads on your whip bites \$N!", ch, null, ch.fighting, TO_CHAR)
            act("A snake from \$n's whip strikes out and bites you!", ch, null, ch.fighting, TO_VICT)
            act("One of the snakes from \$n's whip strikes at \$N!", ch, null, ch.fighting, TO_NOTVICT)
            obj_cast_spell(Skill.weaken, ch.level, ch, ch.fighting, obj)
        }
    }
}

private fun fight_prog_tattoo_prometheus(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return
    when (random(32)) {
        0 -> {
            act("{cThe tattoo on your shoulder glows blue.{x", ch, null, null, TO_CHAR, Pos.Dead)
            obj_cast_spell(Skill.cure_critical, ch.level, ch, ch, obj)
        }
        1, 2 -> {
            act("{cThe tattoo on your shoulder glows red.{x", ch, null, null, TO_CHAR, Pos.Dead)
            val fighting = ch.fighting!!
            when {
                fighting.isEvil() -> spell_dispel_evil((1.2 * ch.level).toInt(), ch, fighting)
                fighting.isGood() -> spell_dispel_good((1.2 * ch.level).toInt(), ch, fighting)
                else -> spell_lightning_bolt((1.2 * ch.level).toInt(), ch, fighting)
            }
        }
    }
}

private fun fight_prog_shockwave(obj: Obj, ch: CHAR_DATA) {
    if (!is_wielded_char(ch, obj)) return
    if (oneChanceOf(32)) {
        act("A bolt of lightning arcs out from your bolt, hitting \$N!", ch, null, ch.fighting, TO_CHAR)
        act("A bolt of lightning crackles along \$n's bolt and arcs towards you!", ch, null, ch.fighting, TO_VICT)
        act("A bolt of lightning shoots out from \$n's bolt, arcing towards \$N!", ch, null, ch.fighting, TO_NOTVICT)
        obj_cast_spell(Skill.lightning_bolt, ch.level, ch, ch.fighting, null)
    }
}

private fun fight_prog_firegauntlets(obj: Obj, ch: CHAR_DATA) {
    if (get_wield_char(ch, false) != null) return
    if (get_eq_char(ch, Wear.Hands) != obj) return
    if (randomPercent() >= 50) return
    val fighting = ch.fighting ?: return

    val dam = dice(ch.level, 8) + randomPercent() / 2
    act("Your gauntlets burns \$N's face!", ch, null, fighting, TO_CHAR)
    act("\$n's gauntlets burns \$N's face!", ch, null, fighting, TO_NOTVICT)
    act("\$N's gauntlets burns your face!", fighting, null, ch, TO_CHAR)
    damage(ch, fighting, dam / 2, Skill.burning_hands, DT.Fire, true)
    fire_effect(fighting, obj.level / 2, dam / 2, TARGET_CHAR)
}

private fun fight_prog_armbands(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Arms) != obj) return
    if (ch.pcdata == null) return
    if (randomPercent() >= 20) return
    val fighting = ch.fighting ?: return

    val dam = randomPercent() / 2 + 30 + 5 * ch.level
    act("Your armbands burns \$N's face!", ch, null, fighting, TO_CHAR)
    act("\$n's armbands burns \$N's face!", ch, null, fighting, TO_NOTVICT)
    act("\$N's armbands burns your face!", fighting, null, ch, TO_CHAR)
    damage(ch, fighting, dam, Skill.burning_hands, DT.Fire, true)
    fire_effect(fighting, obj.level / 2, dam, TARGET_CHAR)
}

private fun fight_prog_demonfireshield(obj: Obj, ch: CHAR_DATA) {
    if (get_shield_char(ch) != obj) return
    if (randomPercent() >= 15) return

    val dam = randomPercent() / 2 + 5 * ch.level
    val fighting = ch.fighting!!
    act("A magical hole appears in your shield !", ch, null, fighting, TO_CHAR)
    act("Your shield burns \$N's face!", ch, null, fighting, TO_CHAR)
    act("\$n's shield burns \$N's face!", ch, null, fighting, TO_NOTVICT)
    act("\$N's shield burns your face!", fighting, null, ch, TO_CHAR)
    fire_effect(fighting, obj.level, dam, TARGET_CHAR)
    damage(ch, fighting, dam, Skill.demonfire, DT.Fire, true)
}

private fun fight_prog_vorbalblade(obj: Obj, ch: CHAR_DATA) {
    if (ch.pcdata == null) return
    if (!is_wielded_char(ch, obj)) return

    val victim = ch.fighting!!
    if (!victim.isEvil()) return

    if (randomPercent() >= 10) return

    send_to_char("Your weapon swings at your victim's neck without your control!\n", ch)
    if (randomPercent() >= 20) return

    act("It makes an huge arc in the air, chopping \$N's head OFF!", ch, null, victim, TO_CHAR)
    act("\$N's weapon whistles in the air, chopping your head OFF!", ch, null, victim, TO_NOTVICT)
    act("\$n's weapon whistles in the air, chopping \$N's head OFF!", ch, null, victim, TO_ROOM)
    act("\$n is DEAD!!", victim, null, null, TO_ROOM)
    act("\$n is DEAD!!", victim, null, null, TO_CHAR)
    raw_kill_org(victim, 3)
    send_to_char("You have been KILLED!!\n", victim)
}

private fun fight_prog_rose_shield(obj: Obj, ch: CHAR_DATA) {
    val st = ch.room.sector_type
    if (st != SECT_FIELD && st != SECT_FOREST || st != SECT_MOUNTAIN || st != SECT_HILLS) return
    if (get_shield_char(ch) != obj) return
    if (randomPercent() < 90) return

    val fighting = ch.fighting!!
    send_to_char("The leaves of your shield grows suddenly.\n", ch)
    send_to_char("The leaves of shield surrounds you!.\n", fighting)
    act("\$n's shield of rose grows suddenly.", ch, null, null, TO_ROOM)
    obj_cast_spell(Skill.slow, ch.level, ch, fighting, null)
}

private fun fight_prog_lion_claw(obj: Obj, ch: CHAR_DATA) {
    if (randomPercent() < 90) return
    if (!is_wielded_char(ch, obj)) return

    send_to_char("The nails of your claw appears form its fingers.\n", ch)
    act("the nails of \$n's claw appears for an instant.", ch, null, null, TO_ROOM, Pos.Dead)

    val victim = ch.fighting!!
    one_hit(ch, victim, Skill.x_hit, false)
    one_hit(ch, victim, Skill.x_hit, false)
    one_hit(ch, victim, Skill.x_hit, false)
    one_hit(ch, victim, Skill.x_hit, false)
    send_to_char("The nails of your claw disappears.\n", ch)
    act("the nails of \$n's claw disappears suddenly.", ch, null, null, TO_ROOM, Pos.Dead)
}

private fun fight_prog_tattoo_goktengri(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Tattoo) != obj) return

    when (random(16)) {
        0, 1 -> {
            act("{WThe tattoo on your shoulder glows white.{x", ch, null, null, TO_CHAR, Pos.Dead)
            do_say(ch, "My honour is my life.")
            val fighting = ch.fighting!!
            one_hit(ch, fighting, null, false)
        }
    }
}

private fun fight_prog_ancient_gloves(obj: Obj, ch: CHAR_DATA) {
    if (get_eq_char(ch, Wear.Hands) != obj || get_wield_char(ch, false) != null) return
    if (randomPercent() >= 20) return

    val fighting = ch.fighting!!
    val dam = randomPercent() + dice(ch.level, 14)
    act("As you touch \$N, the flame within your hands blows UP on \$N!", ch, null, fighting, TO_CHAR)
    act("As \$n touches \$N, the flame within \$s hands blows UP on \$N!", ch, null, fighting, TO_NOTVICT)
    act("As \$N touches you, the flame within \$S hands blows UP on YOU!", fighting, null, ch, TO_CHAR)
    fire_effect(fighting, obj.level, dam, TARGET_CHAR)
    damage(ch, fighting, dam, Skill.burning_hands, DT.Fire, true)
}

private fun fight_prog_ancient_shield(obj: Obj, ch: CHAR_DATA) {
    if (get_shield_char(ch) != obj) return

    val fighting = ch.fighting!!
    val chance = randomPercent()
    when {
        chance < 5 -> {
            val dam = dice(ch.level, 20)
            act("Your shield SHIMMERS brightly!", ch, null, fighting, TO_CHAR)
            act("\$n's shield SHIMMERS brightly!", ch, null, fighting, TO_VICT)
            act("\$n's shield SHIMMERS brightly!", ch, null, fighting, TO_NOTVICT)
            fire_effect(fighting, obj.level / 2, dam, TARGET_CHAR)
            damage(ch, fighting, dam, Skill.fire_breath, DT.Fire, true)
        }
        chance < 10 -> {
            act("Your shield shines with a bright red aura!", ch, null, fighting, TO_CHAR)
            act("\$n's shield shine with a bright red aura!", ch, null, fighting, TO_VICT)
            act("\$n's shield shines with a bright red aura!", ch, null, fighting, TO_NOTVICT)
            obj_cast_spell(Skill.blindness, ch.level + 5, ch, fighting, obj)
            obj_cast_spell(Skill.slow, ch.level + 5, ch, fighting, obj)
        }
    }
}
