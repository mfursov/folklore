package net.sf.nightworks

import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.startsWith

fun do_heal(ch: CHAR_DATA, argument: String) {
    val cost: Int
    var sn: Skill? = null
    val words: String

    /* check for healer */
    var mob: CHAR_DATA? = null
    for (m in ch.room.people) {
        if (m.pcdata == null && IS_SET(m.act, ACT_IS_HEALER)) {
            if (ch.cabal != Clan.None && is_name("cabal", m.name)) {
                if (is_name(ch.cabal.short_name, m.name)) {
                    mob = m
                    break
                }
            } else {
                mob = m
                break
            }
        }
    }

    if (mob == null) {
        send_to_char("You can't do that here.\n", ch)
        return
    }

    if (ch.cabal == Clan.BattleRager) {
        send_to_char("You are BattleRager, not a filthy magician.\n", ch)
        return
    }
    val argb = StringBuilder()
    one_argument(argument, argb)

    if (argb.isEmpty()) {
        /* display price list */
        act("Healer offers the following spells.", ch, null, mob, TO_CHAR)
        send_to_char("  light   : cure light wounds     10 gold\n", ch)
        send_to_char("  serious : cure serious wounds   15 gold\n", ch)
        send_to_char("  critic  : cure critical wounds  25 gold\n", ch)
        send_to_char("  heal    : healing spell         50 gold\n", ch)
        send_to_char("  blind   : cure blindness        20 gold\n", ch)
        send_to_char("  disease : cure disease          15 gold\n", ch)
        send_to_char("  poison  : cure poison           25 gold\n", ch)
        send_to_char("  uncurse : remove curse          50 gold\n", ch)
        send_to_char("  refresh : restore movement       5 gold\n", ch)
        send_to_char("  mana    : restore mana          10 gold\n", ch)
        send_to_char("  master heal: master heal spell 200 gold\n", ch)
        send_to_char("  energize : restore 300 mana    200 gold\n", ch)
        send_to_char(" Type heal <type> to be healed.\n", ch)
        return
    }
    var expl = 0
    val arg = argb.toString()
    if (startsWith(arg, "light")) {
        sn = Skill.cure_light
        words = "judicandus dies"
        cost = 1000
    } else if (startsWith(arg, "serious")) {
        sn = Skill.cure_serious
        words = "judicandus gzfuajg"
        cost = 1600
    } else if (startsWith(arg, "critical")) {
        sn = Skill.cure_critical
        words = "judicandus qfuhuqar"
        cost = 2500
    } else if (startsWith(arg, "heal")) {
        sn = Skill.heal
        words = "pzar"
        cost = 5000
    } else if (startsWith(arg, "blindness")) {
        sn = Skill.cure_blindness
        words = "judicandus noselacri"
        cost = 2000
    } else if (startsWith(arg, "disease")) {
        sn = Skill.cure_disease
        words = "judicandus eugzagz"
        cost = 1500
    } else if (startsWith(arg, "poison")) {
        sn = Skill.cure_poison
        words = "judicandus sausabru"
        cost = 2500
    } else if (startsWith(arg, "uncurse") || startsWith(arg, "curse")) {
        sn = Skill.remove_curse
        words = "candussido judifgz"
        cost = 5000
    } else if (startsWith(arg, "mana")) {
        expl = -3
        words = "candamira"
        cost = 1000
    } else if (startsWith(arg, "refresh") || startsWith(arg, "moves")) {
        sn = Skill.refresh
        words = "candusima"
        cost = 500
    } else if (startsWith(arg, "master")) {
        sn = Skill.master_healing
        words = "candastra nikazubra"
        cost = 20000
    } else if (startsWith(arg, "energize")) {
        expl = -2
        words = "energizer"
        cost = 20000
    } else {
        act("Healer does not offer that spell.  Type 'heal' for a list.", ch, null, mob, TO_CHAR)
        return
    }

    if (cost > ch.gold * 100 + ch.silver) {
        act("You do not have that much gold.",
                ch, null, mob, TO_CHAR)
        return
    }

    WAIT_STATE(ch, PULSE_VIOLENCE)

    deduct_cost(ch, cost)
    mob.gold += cost / 100

    act("\$n utters the words '\$T'.", mob, null, words, TO_ROOM)
    if (expl == -2) {
        ch.mana += 300
        ch.mana = Math.min(ch.mana, ch.max_mana)
        send_to_char("A warm glow passes through you.\n", ch)
    }
    if (expl == -3) {
        ch.mana += dice(2, 8) + mob.level / 3
        ch.mana = Math.min(ch.mana, ch.max_mana)
        send_to_char("A warm glow passes through you.\n", ch)
    }

    if (sn == null) {
        return
    }

    sn.spell_fun(mob.level, mob, ch, TARGET_CHAR)
}


fun heal_battle(mob: CHAR_DATA, ch: CHAR_DATA) {

    if (is_name(ch.cabal.short_name, mob.name)) {
        return
    }

    if (ch.pcdata == null || ch.cabal != Clan.BattleRager) {
        do_say(mob, "I won't help you.")
        return
    }
    if (!IS_AFFECTED(ch, AFF_BLIND) && !IS_AFFECTED(ch, AFF_PLAGUE) && !IS_AFFECTED(ch, AFF_POISON) && !IS_AFFECTED(ch, AFF_CURSE)) {
        do_say(mob, "You don't need my help, my dear. But in case!")
        Skill.remove_curse.spell_fun(mob.level, mob, ch, TARGET_CHAR)
        return
    }

    act("\$n gives you some herbs to eat.", mob, null, ch, TO_VICT)
    act("You eat that herbs.", mob, null, ch, TO_VICT)
    act("You give the herbs to \$N.", mob, null, ch, TO_CHAR)
    act("\$N eats the herbs that you give.", mob, null, ch, TO_CHAR)
    act("\$n gives the herbs to \$N.", mob, null, ch, TO_NOTVICT)
    act("\$n eats the herbs that \$N gave \$m.", mob, null, ch, TO_NOTVICT)

    WAIT_STATE(ch, PULSE_VIOLENCE)

    if (IS_AFFECTED(ch, AFF_BLIND)) {
        Skill.cure_blindness.spell_fun(mob.level, mob, ch, TARGET_CHAR)
    }

    if (IS_AFFECTED(ch, AFF_PLAGUE)) {
        Skill.cure_disease.spell_fun(mob.level, mob, ch, TARGET_CHAR)
    }

    if (IS_AFFECTED(ch, AFF_POISON)) {
        Skill.cure_poison.spell_fun(mob.level, mob, ch, TARGET_CHAR)
    }

    if (IS_AFFECTED(ch, AFF_CURSE)) {
        Skill.remove_curse.spell_fun(mob.level, mob, ch, TARGET_CHAR)
    }
}


