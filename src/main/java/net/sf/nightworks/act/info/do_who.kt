package net.sf.nightworks.act.info

import net.sf.nightworks.Clazz
import net.sf.nightworks.Index
import net.sf.nightworks.LEVEL_HERO
import net.sf.nightworks.LEVEL_IMMORTAL
import net.sf.nightworks.MAX_CLASS
import net.sf.nightworks.MAX_LEVEL
import net.sf.nightworks.OBJ_VNUM_RULER_BADGE
import net.sf.nightworks.PK_MIN_LEVEL
import net.sf.nightworks.PLR_CANINDUCT
import net.sf.nightworks.PLR_WANTED
import net.sf.nightworks.Race
import net.sf.nightworks.can_see
import net.sf.nightworks.get_eq_char
import net.sf.nightworks.is_equiped_n_char
import net.sf.nightworks.is_safe_nomessage
import net.sf.nightworks.max_on
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.page_to_char
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_TRUSTED
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.atoi
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.is_number
import net.sf.nightworks.util.one_argument
import java.util.HashSet

fun do_who(ch: CHAR_DATA, argument: String) {
    var rest = argument
    var iLevelLower = 0
    var iLevelUpper = MAX_LEVEL
    var fClassRestrict = false
    var fRaceRestrict = false
    var fImmortalOnly = false
    var fPKRestrict = false
    var fRulerRestrict = false
    var fChaosRestrict = false
    var fShalafiRestrict = false
    var fInvaderRestrict = false
    var fBattleRestrict = false
    var fKnightRestrict = false
    var fLionsRestrict = false
    var fTattoo = false

    val rgfClass = BooleanArray(MAX_CLASS)
    var rgfRaces: MutableSet<Race>? = null

    /*
        * Parse arguments.
        */
    var nNumber = 0
    var vnum = 0
    while (true) {
        val argb = StringBuilder()
        rest = one_argument(rest, argb)
        if (argb.isEmpty()) {
            break
        }

        val arg = argb.toString()
        if (eq(arg, "pk")) {
            fPKRestrict = true
            break
        }

        if (eq(arg, "ruler")) {
            if (ch.cabal != Clan.Ruler && !IS_IMMORTAL(ch)) {
                send_to_char("You are not in that cabal!\n", ch)
                return
            } else {
                fRulerRestrict = true
                break
            }
        }
        if (eq(arg, "shalafi")) {
            if (ch.cabal != Clan.Shalafi && !IS_IMMORTAL(ch)) {
                send_to_char("You are not in that cabal!\n", ch)
                return
            } else {
                fShalafiRestrict = true
                break
            }
        }
        if (eq(arg, "battle")) {
            if (ch.cabal != Clan.BattleRager && !IS_IMMORTAL(ch)) {
                send_to_char("You are not in that cabal!\n", ch)
                return
            } else {
                fBattleRestrict = true
                break
            }
        }
        if (eq(arg, "invader")) {
            if (ch.cabal != Clan.Invader&& !IS_IMMORTAL(ch)) {
                send_to_char("You are not in that cabal!\n", ch)
                return
            } else {
                fInvaderRestrict = true
                break
            }
        }
        if (eq(arg, "chaos")) {
            if (ch.cabal != Clan.Chaos && !IS_IMMORTAL(ch)) {
                send_to_char("You are not in that cabal!\n", ch)
                return
            } else {
                fChaosRestrict = true
                break
            }
        }
        if (eq(arg, "knight")) {
            if (ch.cabal != Clan.Knight && !IS_IMMORTAL(ch)) {
                send_to_char("You are not in that cabal!\n", ch)
                return
            } else {
                fKnightRestrict = true
                break
            }
        }
        if (eq(arg, "lions")) {
            if (ch.cabal != Clan.Lion && !IS_IMMORTAL(ch)) {
                send_to_char("You are not in that cabal!\n", ch)
                return
            } else {
                fLionsRestrict = true
                break
            }
        }

        if (eq(arg, "tattoo")) {
            if (get_eq_char(ch, Wear.Tattoo) == null) {
                send_to_char("You haven't got a tattoo yetl!\n", ch)
                return
            } else {
                fTattoo = true
                vnum = get_eq_char(ch, Wear.Tattoo)!!.pIndexData.vnum
                break
            }
        }


        if (is_number(arg) && IS_IMMORTAL(ch)) {
            when (++nNumber) {
                1 -> iLevelLower = atoi(arg)
                2 -> iLevelUpper = atoi(arg)
                else -> {
                    send_to_char("This function of who is for immortals.\n", ch)
                    return
                }
            }
        } else {

            /*
                * Look for classes to turn on.
                */
            if (arg[0] == 'i') {
                fImmortalOnly = true
            } else {
                val iClass = Clazz.lookup(arg)
                if (iClass == null || !IS_IMMORTAL(ch)) {
                    val race = Race.lookup(arg)
                    if (race == null || !race.pcRace) {
                        send_to_char("That's not a valid race.\n", ch)
                        return
                    } else {
                        fRaceRestrict = true
                        if (rgfRaces == null) {
                            rgfRaces = HashSet()
                        }
                        rgfRaces.add(race)
                    }
                } else {
                    fClassRestrict = true
                    rgfClass[iClass.id] = true
                }
            }
        }
    }

    /*
         * Now show matching chars.
         */
    var nMatch = 0
    val buf = TextBuffer()
    val output = StringBuilder()
    for (d in Index.PLAYERS) {
        /*
            * Check for match against restrictions.
            * Don't use trust as that exposes trusted mortals.
            */
        if (!can_see(ch, d.ch)) continue
        if (IS_VAMPIRE(d.ch) && !IS_IMMORTAL(ch) && ch != d.ch) continue
        if (!can_see(ch, d.o_ch)) continue /* can't see switched wizi imms */

        if (d.o_ch.level < iLevelLower
                || d.o_ch.level > iLevelUpper
                || fImmortalOnly && d.o_ch.level < LEVEL_HERO
                || fClassRestrict && !rgfClass[d.o_ch.clazz.id]
                || fRaceRestrict && !rgfRaces!!.contains(d.o_ch.race)
                || fPKRestrict && is_safe_nomessage(ch, d.o_ch)
                || fTattoo && vnum == get_eq_char(d.o_ch, Wear.Tattoo)!!.pIndexData.vnum
                || fRulerRestrict && d.o_ch.cabal != Clan.Ruler
                || fChaosRestrict && d.o_ch.cabal != Clan.Chaos
                || fBattleRestrict && d.o_ch.cabal != Clan.BattleRager
                || fInvaderRestrict && d.o_ch.cabal != Clan.Invader
                || fShalafiRestrict && d.o_ch.cabal != Clan.Shalafi
                || fKnightRestrict && d.o_ch.cabal != Clan.Knight
                || fLionsRestrict && d.o_ch.cabal != Clan.Lion) {
            continue
        }

        nMatch++

        /* Figure out what to print for class. */
        val clazz = when (d.o_ch.level) {
            MAX_LEVEL -> "IMP"
            MAX_LEVEL - 1 -> "CRE"
            MAX_LEVEL - 2 -> "SUP"
            MAX_LEVEL - 3 -> "DEI"
            MAX_LEVEL - 4 -> "GOD"
            MAX_LEVEL - 5 -> "IMM"
            MAX_LEVEL - 6 -> "DEM"
            MAX_LEVEL - 7 -> "ANG"
            MAX_LEVEL - 8 -> "AVA"
            else -> d.o_ch.clazz.who_name
        }

        /* for cabals
            if ((wch.cabal && (wch.cabal == ch.cabal ||
                       IS_TRUSTED(ch,LEVEL_IMMORTAL))) ||
                                   wch.level >= LEVEL_HERO)
            */
        var cabalbuf = ""
        if (d.o_ch.cabal != Clan.None && ch.cabal == d.o_ch.cabal || IS_IMMORTAL(ch)
                || IS_SET(d.o_ch.act, PLR_CANINDUCT) && d.o_ch.cabal == Clan.Ruler
                || d.o_ch.cabal == Clan.Hunter
                || d.o_ch.cabal == Clan.Ruler && is_equiped_n_char(d.o_ch, OBJ_VNUM_RULER_BADGE, Wear.Neck)) {
            cabalbuf = "{c" + d.o_ch.cabal.short_name + "{x"
        }
        if (d.o_ch.cabal == Clan.None) {
            cabalbuf = ""
        }

        var pk_buf = ""
        if (!(ch == d.o_ch && ch.level < PK_MIN_LEVEL || is_safe_nomessage(ch, d.o_ch))) {
            pk_buf = "{r(PK){x"
        }

        val act_buf = if (IS_SET(ch.act, PLR_WANTED)) "{W(WANTED){x " else ""
        val pcdata = d.o_ch.pcdata!!
        val titlebuf = pcdata.title
        /*
            * Format it up.
            */
        val level_buf = TextBuffer()
        level_buf.sprintf("{c%2d{x", d.o_ch.level)
        val classbuf = "{Y$clazz{x"

        if (IS_TRUSTED(ch, LEVEL_IMMORTAL) || ch == d.o_ch || d.o_ch.level >= LEVEL_HERO) {
            buf.sprintf("[%2d %s %s] %s %s %s %s %s\n",
                    d.o_ch.level,
                    d.o_ch.race.who_name,
                    classbuf,
                    pk_buf,
                    cabalbuf,
                    act_buf,
                    d.o_ch.name,
                    titlebuf)
        } else
        /*    buf.sprintf( "[%s %s %s] %s%s%s%s%s\n",    */ {
            buf.sprintf("[%s %s    ] %s %s %s %s %s\n",
                    if (d.o_ch.curr_stat(PrimeStat.Charisma) < 18) level_buf else "  ",
                    d.o_ch.race.who_name,
                    /*      classbuf,   */
                    pk_buf,
                    cabalbuf,
                    act_buf,
                    d.o_ch.name,
                    titlebuf)
        }

        output.append(buf)
    }

    val count = Index.PLAYERS.size
    max_on = Math.max(count, max_on)
    buf.sprintf("\nPlayers found: %d. Most so far today: %d.\n", nMatch, max_on)
    output.append(buf)
    page_to_char(output, ch)
}
