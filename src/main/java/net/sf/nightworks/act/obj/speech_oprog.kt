package net.sf.nightworks.act.obj

import net.sf.nightworks.PULSE_VIOLENCE
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.get_hold_char
import net.sf.nightworks.is_equiped_char
import net.sf.nightworks.is_wielded_char
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Wear
import net.sf.nightworks.obj_cast_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.eq

fun create_speech_prog(name: String) = when (name) {
    "speech_prog_excalibur" -> ::speech_prog_excalibur
    "speech_prog_kassandra" -> ::speech_prog_kassandra
    "speech_prog_ring_ra" -> ::speech_prog_ring_ra
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun speech_prog_excalibur(obj: Obj, ch: CHAR_DATA, speech: String) {
    val fighting = ch.fighting
    if (!eq(speech, "sword of acid") || fighting == null || !is_wielded_char(ch, obj)) return

    send_to_char("Acid sprays from the blade of Excalibur.\n", ch)
    act("Acid sprays from the blade of Excalibur.", ch, null, null, TO_ROOM)
    obj_cast_spell(Skill.acid_blast, ch.level, ch, fighting, null)
    WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
}

private fun speech_prog_kassandra(obj: Obj, ch: CHAR_DATA, speech: String) {
    val fighting = ch.fighting
    when {
        eq(speech, "kassandra") && get_hold_char(ch) == obj && ch.pcdata != null -> obj_cast_spell(Skill.kassandra, ch.level, ch, ch, null)
        eq(speech, "sebat") && get_hold_char(ch) == obj && ch.pcdata != null -> obj_cast_spell(Skill.sebat, ch.level, ch, ch, null)
        eq(speech, "matandra") && get_hold_char(ch) == obj && fighting != null && ch.pcdata != null -> {
            act("A blast of energy bursts from your hand toward \$N!", ch, null, fighting, TO_CHAR)
            act("A blast of energy bursts from \$n's hand toward you!", ch, null, fighting, TO_VICT)
            act("A blast of energy bursts from \$n's hand toward \$N!", ch, null, fighting, TO_NOTVICT)
            obj_cast_spell(Skill.matandra, ch.level, ch, fighting, null)
        }
    }
}

private fun speech_prog_ring_ra(obj: Obj, ch: CHAR_DATA, speech: String) {
    val fighting = ch.fighting
    if (!eq(speech, "punish") || fighting == null || !is_equiped_char(ch, obj, Wear.Finger)) return

    send_to_char("An electrical arc sprays from the ring.\n", ch)
    act("An electrical arc sprays from the ring.", ch, null, null, TO_ROOM)
    obj_cast_spell(Skill.lightning_breath, ch.level, ch, fighting, null)
    WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
}
