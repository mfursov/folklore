@file:Suppress("FunctionName")

package net.sf.nightworks.act.obj

import net.sf.nightworks.AFF_FLYING
import net.sf.nightworks.AFF_HASTE
import net.sf.nightworks.RES_COLD
import net.sf.nightworks.RES_FIRE
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_RESIST
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.WEAPON_KATANA
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.get_skill
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.OPROG_FUN_WEAR
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.obj_from_char
import net.sf.nightworks.obj_to_room
import net.sf.nightworks.send_to_char
import net.sf.nightworks.unequip_char
import net.sf.nightworks.util.IS_WEAPON_STAT

fun create_wear_prog(name: String): OPROG_FUN_WEAR {
    return when (name) {
        "wear_prog_excalibur" -> ::wear_prog_excalibur
        "wear_prog_bracer" -> { _, ch -> wear_prog_bracer(ch) }
        "wear_prog_ranger_staff" -> ::wear_prog_ranger_staff
        "wear_prog_firegauntlets" -> { _, ch -> wear_prog_firegauntlets(ch) }
        "wear_prog_armbands" -> { _, ch -> wear_prog_armbands(ch) }
        "wear_prog_demonfireshield" -> { _, ch -> wear_prog_demonfireshield(ch) }
        "wear_prog_boots_flying" -> { _, ch -> wear_prog_boots_flying(ch) }
        "wear_prog_wind_boots" -> { _, ch -> wear_prog_wind_boots(ch) }
        "wear_prog_arm_hercules" -> { _, ch -> wear_prog_arm_hercules(ch) }
        "wear_prog_girdle_giant" -> { _, ch -> wear_prog_girdle_giant(ch) }
        "wear_prog_breastplate_strength" -> { _, ch -> wear_prog_breastplate_strength(ch) }
        "wear_prog_eyed_sword" -> ::wear_prog_eyed_sword
        "wear_prog_snake" -> ::wear_prog_snake
        "wear_prog_fire_shield" -> ::wear_prog_fire_shield
        "wear_prog_katana_sword" -> ::wear_prog_katana_sword
        "wear_prog_quest_weapon" -> ::wear_prog_quest_weapon
        "wear_prog_ancient_gloves" -> { _, ch -> wear_prog_ancient_gloves(ch) }
        "wear_prog_ancient_shield" -> ::wear_prog_ancient_shield
        "wear_prog_neckguard" -> { _, ch -> wear_prog_neckguard(ch) }
        "wear_prog_headguard" -> { _, ch -> wear_prog_headguard(ch) }
        "wear_prog_blackguard" -> { _, ch -> wear_prog_blackguard(ch) }
        else -> throw IllegalArgumentException("Prog not found: $name")
    }
}

private fun wear_prog_excalibur(obj: Obj, ch: CHAR_DATA) {
    act("\$p begins to shine a bright white.", ch, obj, null, TO_CHAR)
    act("\$p begins to shine a bright white.", ch, obj, null, TO_ROOM)
    obj.value[2] = when {
        ch.level in 21..30 -> 4
        ch.level in 31..40 -> 5
        ch.level in 41..50 -> 6
        ch.level in 51..60 -> 7
        ch.level in 61..70 -> 9
        ch.level in 71..80 -> 11
        else -> 12
    }
}

private fun wear_prog_bracer(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.haste)) return

    send_to_char("As you slide your arms into these bracers, they mold to your skin.\n", ch)
    send_to_char("Your hands and arms feel incredibly light.\n", ch)
    val af = Affect()
    af.type = Skill.haste
    af.duration = -2
    af.level = ch.level
    af.bitvector = AFF_HASTE
    af.location = Apply.Dex
    af.modifier = 1 + (if (ch.level >= 18) 1 else 0) + (if (ch.level >= 30) 1 else 0) + if (ch.level >= 45) 1 else 0
    affect_to_char(ch, af)
}

private fun wear_prog_ranger_staff(obj: Obj, ch: CHAR_DATA) {
    if (get_skill(ch, Skill.ranger_staff) <= 0) return

    send_to_char("You don't know to use this thing.\n", ch)
    unequip_char(ch, obj)
    send_to_char("Ranger staff slides off from your hand.\n", ch)
    obj_from_char(obj)
    obj_to_room(obj, ch.room)
}

private fun wear_prog_firegauntlets(ch: CHAR_DATA) = send_to_char("Your hands warm up by the gauntlets.\n", ch)

private fun wear_prog_armbands(ch: CHAR_DATA) = send_to_char("Your arms warm up by the armbands of the volcanoes.\n", ch)

private fun wear_prog_demonfireshield(ch: CHAR_DATA) = send_to_char("Your hands warm up by the fire shield.\n", ch)

private fun wear_prog_wind_boots(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.fly)) return

    send_to_char("As you wear wind boots on your feet, they hold you up.\n", ch)
    send_to_char("You start to fly.\n", ch)
    val af = Affect()
    af.type = Skill.fly
    af.duration = -2
    af.level = ch.level
    af.bitvector = AFF_FLYING
    affect_to_char(ch, af)
}

private fun wear_prog_boots_flying(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.fly)) return

    send_to_char("As you wear boots of flying on your feet, they hold you up.\n", ch)
    send_to_char("You start to fly.\n", ch)
    val af = Affect()
    af.type = Skill.fly
    af.duration = -2
    af.level = ch.level
    af.bitvector = AFF_FLYING
    affect_to_char(ch, af)
}

private fun wear_prog_arm_hercules(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.giant_strength)) return

    send_to_char("As you wear your arms these plates, You feel your self getting stronger.\n", ch)
    send_to_char("Your muscles seems incredibly huge.\n", ch)

    val af = Affect()
    af.type = Skill.giant_strength
    af.duration = -2
    af.level = ch.level
    af.location = Apply.Str
    af.modifier = 1 + (if (ch.level >= 18) 1 else 0) + (if (ch.level >= 30) 1 else 0) + if (ch.level >= 45) 1 else 0
    affect_to_char(ch, af)
}

private fun wear_prog_girdle_giant(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.giant_strength)) return

    send_to_char("As you wear this girdle, You feel your self getting stronger.\n", ch)
    send_to_char("Your muscles seems incredibly huge.\n", ch)

    val af = Affect()
    af.type = Skill.giant_strength
    af.duration = -2
    af.level = ch.level
    af.location = Apply.Str
    af.modifier = 1 + (if (ch.level >= 18) 1 else 0) + (if (ch.level >= 30) 1 else 0) + if (ch.level >= 45) 1 else 0
    affect_to_char(ch, af)
}

private fun wear_prog_breastplate_strength(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.giant_strength)) return

    send_to_char("As you wear breastplate of strength, You feel yourself getting stronger.\n", ch)
    send_to_char("Your muscles seems incredibly huge.\n", ch)

    val af = Affect()
    af.type = Skill.giant_strength
    af.duration = -2
    af.level = ch.level
    af.location = Apply.Str
    af.modifier = 1 + (if (ch.level >= 18) 1 else 0) + (if (ch.level >= 30) 1 else 0) + if (ch.level >= 45) 1 else 0
    affect_to_char(ch, af)
}

private fun wear_prog_eyed_sword(obj: Obj, ch: CHAR_DATA) {
    act("\$p's eye opens.", ch, obj, null, TO_CHAR)
    act("\$p's eye opens.", ch, obj, null, TO_ROOM)

    obj.value[2] = when {
        ch.level <= 10 -> 2
        ch.level in 11..20 -> 3
        ch.level in 21..30 -> 4
        ch.level in 31..40 -> 5
        ch.level in 41..50 -> 6
        ch.level in 51..60 -> 7
        ch.level in 61..70 -> 9
        ch.level in 71..80 -> 11
        else -> 12
    }
    obj.level = ch.level
}

private fun wear_prog_katana_sword(obj: Obj, ch: CHAR_DATA) {
    if (obj.item_type != ItemType.Weapon || !IS_WEAPON_STAT(obj, WEAPON_KATANA) || !obj.extra_desc.description.contains(ch.name)) return

    obj.value[2] = when {
        ch.level <= 10 -> 2
        ch.level in 11..20 -> 3
        ch.level in 21..30 -> 4
        ch.level in 31..40 -> 5
        ch.level in 41..50 -> 6
        ch.level in 51..60 -> 7
        ch.level in 61..70 -> 9
        ch.level in 71..80 -> 11
        else -> 12
    }
    obj.level = ch.level
    send_to_char("You feel your katana like a part of you!\n", ch)
}

private fun wear_prog_snake(obj: Obj, ch: CHAR_DATA) {
    act("{gSnakes of whip starts to breath a poisonous air.{x", ch, obj, null, TO_CHAR, Pos.Dead)
    act("{gSnakes of whip starts to breath a poisonous air.{x", ch, obj, null, TO_ROOM, Pos.Dead)
    obj.value[2] = when {
        ch.level in 21..30 -> 4
        ch.level in 31..40 -> 5
        ch.level in 41..50 -> 6
        ch.level in 51..60 -> 7
        ch.level in 61..70 -> 9
        ch.level in 71..80 -> 11
        else -> 12
    }
}

private fun wear_prog_fire_shield(obj: Obj, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.fire_shield)) return

    if (obj.extra_desc.description.contains("cold")) {
        val af = Affect()
        af.where = TO_RESIST
        af.type = Skill.fire_shield
        af.duration = -2
        af.level = ch.level
        af.bitvector = RES_COLD
        send_to_char("As you wear shield, you become resistive to cold.\n", ch)
        affect_to_char(ch, af)
        return
    }
    val af = Affect()
    af.where = TO_RESIST
    af.type = Skill.fire_shield
    af.duration = -2
    af.level = ch.level
    af.bitvector = RES_FIRE
    send_to_char("As you wear shield, you become resistive to fire.\n", ch)
    affect_to_char(ch, af)
}

private fun wear_prog_quest_weapon(obj: Obj, ch: CHAR_DATA) {
    if (!obj.short_desc.contains(ch.name)) {
        act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)
        obj_from_char(obj)
        obj_to_room(obj, ch.room)
        return
    }
    send_to_char("{bYour weapon starts glowing.{x", ch)
    obj.value[2] = when {
        ch.level in 21..30 -> 4
        ch.level in 31..40 -> 5
        ch.level in 41..50 -> 6
        ch.level in 51..60 -> 7
        ch.level in 61..70 -> 9
        ch.level in 71..80 -> 11
        else -> 12
    }
    obj.level = ch.level
}

private fun wear_prog_ancient_gloves(ch: CHAR_DATA) = send_to_char("A flame starts to burn within your hands!\n", ch)

private fun wear_prog_ancient_shield(obj: Obj, ch: CHAR_DATA) {
    act("{rYour shield changes its shape and surrounds itself with dragon skin.\n" +
            "A dragon head gets born on the upper surface of the shield and opens its mouth!{x",
            ch, obj, null, TO_CHAR, Pos.Dead)
    act("{r\$n's shield changes its shape and surrounds itself with dragon skin.\n" +
            "A dragon head gets born on the upper surface of the shield and opens its mouth!{x",
            ch, obj, null, TO_ROOM, Pos.Dead)
}

private fun wear_prog_neckguard(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.neckguard)) return

    val af = Affect()
    af.type = Skill.neckguard
    af.duration = -2
    af.level = ch.level
    af.location = Apply.None
    affect_to_char(ch, af)
}

private fun wear_prog_headguard(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.headguard)) return

    val af = Affect()
    af.type = Skill.headguard
    af.duration = -2
    af.level = ch.level
    af.location = Apply.None
    affect_to_char(ch, af)
}

private fun wear_prog_blackguard(ch: CHAR_DATA) {
    if (is_affected(ch, Skill.blackguard)) return

    val af = Affect()
    af.type = Skill.blackguard
    af.duration = -2
    af.level = ch.level
    af.location = Apply.None
    affect_to_char(ch, af)
}
