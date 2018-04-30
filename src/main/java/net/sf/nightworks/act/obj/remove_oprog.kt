package net.sf.nightworks.act.obj

import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_strip
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.OPROG_FUN_REMOVE
import net.sf.nightworks.model.Pos
import net.sf.nightworks.send_to_char

fun create_remove_prog(name: String): OPROG_FUN_REMOVE = when (name) {
    "remove_prog_bracer" -> { _, ch -> remove_prog_bracer(ch) }
    "remove_prog_excalibur" -> ::remove_prog_excalibur
    "remove_prog_firegauntlets" -> { _, ch -> remove_prog_firegauntlets(ch) }
    "remove_prog_armbands" -> { _, ch -> remove_prog_armbands(ch) }
    "remove_prog_demonfireshield" -> { _, ch -> remove_prog_demonfireshield(ch) }
    "remove_prog_wind_boots" -> { _, ch -> remove_prog_wind_boots(ch) }
    "remove_prog_boots_flying" -> { _, ch -> remove_prog_boots_flying(ch) }
    "remove_prog_arm_hercules" -> { _, ch -> remove_prog_arm_hercules(ch) }
    "remove_prog_girdle_giant" -> { _, ch -> remove_prog_girdle_giant(ch) }
    "remove_prog_breastplate_strength" -> { _, ch -> remove_prog_breastplate_strength(ch) }
    "remove_prog_snake" -> ::remove_prog_snake
    "remove_prog_fire_shield" -> ::remove_prog_fire_shield
    "remove_prog_ancient_gloves" -> { _, ch -> remove_prog_ancient_gloves(ch) }
    "remove_prog_ancient_shield" -> ::remove_prog_ancient_shield
    "remove_prog_neckguard" -> { _, ch -> remove_prog_neckguard(ch) }
    "remove_prog_headguard" -> { _, ch -> remove_prog_headguard(ch) }
    "remove_prog_blackguard" -> { _, ch -> remove_prog_blackguard(ch) }
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun remove_prog_bracer(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.haste)) return
    affect_strip(ch, Skill.haste)
    send_to_char("Your hands and arms feel heavy again.\n", ch)
}

private fun remove_prog_excalibur(obj: Obj, ch: CHAR_DATA) {
    act("\$p stops glowing.", ch, obj, null, TO_CHAR)
    act("\$p stops glowing.", ch, obj, null, TO_ROOM)
}

private fun remove_prog_firegauntlets(ch: CHAR_DATA) = send_to_char("Your hands cool down.\n", ch)

private fun remove_prog_armbands(ch: CHAR_DATA) = send_to_char("Your arms cool down again.\n", ch)

private fun remove_prog_demonfireshield(ch: CHAR_DATA) = send_to_char("Your hands cool down.\n", ch)

private fun remove_prog_wind_boots(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.fly)) return
    affect_strip(ch, Skill.fly)
    send_to_char("You fall down to the ground.\n", ch)
    send_to_char("Ouch!.\n", ch)
}

private fun remove_prog_boots_flying(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.fly)) return
    affect_strip(ch, Skill.fly)
    send_to_char("You fall down to the ground.\n", ch)
    send_to_char("You start to walk again instead of flying!.\n", ch)
}

private fun remove_prog_arm_hercules(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.giant_strength)) return
    affect_strip(ch, Skill.giant_strength)
    send_to_char("Your muscles regain its original value.\n", ch)
}

private fun remove_prog_girdle_giant(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.giant_strength)) return
    affect_strip(ch, Skill.giant_strength)
    send_to_char("Your muscles regain its original value.\n", ch)
}

private fun remove_prog_breastplate_strength(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.giant_strength)) return
    affect_strip(ch, Skill.giant_strength)
    send_to_char("Your muscles regain its original value.\n", ch)
}

private fun remove_prog_snake(obj: Obj, ch: CHAR_DATA) {
    act("{rSnakes of whip slowly melds to non-living skin.{x", ch, obj, null, TO_CHAR, Pos.Dead)
    act("{rSnakes of whip slowy melds to non-living skin.{x", ch, obj, null, TO_ROOM, Pos.Dead)
}

private fun remove_prog_fire_shield(obj: Obj, ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.fire_shield)) return
    affect_strip(ch, Skill.fire_shield)
    when {
        obj.extra_desc.description.contains("cold") -> send_to_char("You have become normal to cold attacks.\n", ch)
        else -> send_to_char("You have become normal to fire attacks.\n", ch)
    }
}

private fun remove_prog_ancient_gloves(ch: CHAR_DATA) = send_to_char("The flame within your hands disappears.\n", ch)

private fun remove_prog_ancient_shield(obj: Obj, ch: CHAR_DATA) {
    act("{rYour shield returns to its original form.{x", ch, obj, null, TO_CHAR, Pos.Dead)
    act("{rYour shield returns to its original form.{x", ch, obj, null, TO_ROOM, Pos.Dead)
}

private fun remove_prog_neckguard(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.neckguard)) return
    affect_strip(ch, Skill.neckguard)
}

private fun remove_prog_headguard(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.headguard)) return
    affect_strip(ch, Skill.headguard)
}

private fun remove_prog_blackguard(ch: CHAR_DATA) {
    if (!is_affected(ch, Skill.blackguard)) return
    affect_strip(ch, Skill.blackguard)
}
