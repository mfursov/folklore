package net.sf.nightworks.act.comm

import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.Index
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.get_char_room
import net.sf.nightworks.is_same_group
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Pos
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.one_argument

fun do_group(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        val leader = if (ch.leader != null) ch.leader else ch
        val buf = TextBuffer()
        buf.sprintf("%s's group:\n", PERS(leader, ch))
        send_to_char(buf.toString(), ch)

        for (gch in Index.CHARS) {
            if (is_same_group(gch, ch)) {
                buf.clear()
                buf.sprintf("[%2d %s] %-16s %d/%d hp %d/%d mana %d/%d mv   %5d xp\n",
                        gch.level,
                        if (gch.pcdata == null) "Mob" else gch.clazz.who_name,
                        capitalize(PERS(gch, ch)),
                        gch.hit, gch.max_hit,
                        gch.mana, gch.max_mana,
                        gch.move, gch.max_move,
                        gch.exp)
                send_to_char(buf.toString(), ch)
            }
        }
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (ch.master != null || ch.leader != null && ch.leader != ch) {
        send_to_char("But you are following someone else!\n", ch)
        return
    }

    if (victim.master != ch && ch != victim) {
        act("\$N isn't following you.", ch, null, victim, TO_CHAR)
        return
    }

    if (IS_AFFECTED(victim, AFF_CHARM)) {
        send_to_char("You can't remove charmed mobs from your group.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM)) {
        act("You like your master too much to leave \$m!", ch, null, victim, TO_VICT)
        return
    }


    if (is_same_group(victim, ch) && ch != victim) {
        val guarded_by = victim.guarded_by
        if (ch.guarding == victim || guarded_by == ch) {
            act("You stop guarding \$N.", ch, null, victim, TO_CHAR)
            act("\$n stops guarding you.", ch, null, victim, TO_VICT)
            act("\$n stops guarding \$N.", ch, null, victim, TO_NOTVICT)
            victim.guarded_by = null
            ch.guarding = null
        }

        victim.leader = null
        act("{Y\$n removes \$N from \$s group.{x", ch, null, victim, TO_NOTVICT, Pos.Sleeping)
        act("{Y\$n removes you from \$s group.{x", ch, null, victim, TO_VICT, Pos.Sleeping)
        act("{bYou remove \$N from your group.{x", ch, null, victim, TO_CHAR, Pos.Sleeping)

        if (guarded_by != null && !is_same_group(victim, guarded_by)) {
            act("You stop guarding \$N.", guarded_by, null, victim, TO_CHAR)
            act("\$n stops guarding you.", guarded_by, null, victim, TO_VICT)
            act("\$n stops guarding \$N.", guarded_by, null, victim, TO_NOTVICT)
            guarded_by.guarding = null
            victim.guarded_by = null
        }
        return
    }

    if (ch.level - victim.level < -8 || ch.level - victim.level > 8) {
        act("{R\$N cannot join \$n's group.{x", ch, null, victim, TO_NOTVICT, Pos.Sleeping)
        act("{RYou cannot join \$n's group.{x", ch, null, victim, TO_VICT, Pos.Sleeping)
        act("{R\$N cannot join your group.{x", ch, null, victim, TO_CHAR, Pos.Sleeping)
        return
    }

    if (ch.isGood() && victim.isEvil()) {
        act("{rYou are too evil for \$n's group.{x", ch, null, victim, TO_VICT, Pos.Sleeping)
        act("{r\$N is too evil for your group!{x", ch, null, victim, TO_CHAR, Pos.Sleeping)
        return
    }

    if (victim.isGood() && ch.isEvil()) {
        act("{rYou are too pure to join \$n's group!{x", ch, null, victim, TO_VICT, Pos.Sleeping)
        act("{r\$N is too pure for your group!{x", ch, null, victim, TO_CHAR, Pos.Sleeping)
        return
    }

    if (ch.cabal == Clan.Ruler && victim.cabal == Clan.Chaos ||
            ch.cabal == Clan.Chaos && victim.cabal == Clan.Ruler ||
            ch.cabal == Clan.Knight && victim.cabal == Clan.Invader||
            ch.cabal == Clan.Invader&& victim.cabal == Clan.Knight ||
            ch.cabal == Clan.Shalafi && victim.cabal == Clan.BattleRager ||
            ch.cabal == Clan.BattleRager && victim.cabal == Clan.Shalafi) {
        act("{rYou hate \$n's cabal, how can you join \$n's group?!{x", ch, null, victim, TO_VICT, Pos.Sleeping)
        act("{rYou hate \$N's cabal, how can you want \$N to join your group?!{x", ch, null, victim, TO_CHAR, Pos.Sleeping)
        return
    }

    victim.leader = ch
    act("{Y\$N joins \$n's group.{x", ch, null, victim, TO_NOTVICT, Pos.Sleeping)
    act("{YYou join \$n's group.{x", ch, null, victim, TO_VICT, Pos.Sleeping)
    act("{b\$N joins your group.{x", ch, null, victim, TO_CHAR, Pos.Sleeping)
}