package net.sf.nightworks.model

import net.sf.nightworks.LEVEL_IMMORTAL
import net.sf.nightworks.util.StatsMap
import net.sf.nightworks.util.URANGE

enum class PrimeStat(val shortName: String) {
    Strength("str"),
    Intelligence("int"),
    Dexterity("dex"),
    Wisdom("wis"),
    Constitution("con"),
    Charisma("cha");

    companion object {
        fun of(v: String) = values().firstOrNull({ it.shortName == v })
        fun of(id: Int) = values().firstOrNull({ it.ordinal == id })
        fun statsOf(v: Int) = StatsMap(values().associate { s -> Pair(s, v) }.toMutableMap())
    }
}


/**  training score */
fun CHAR_DATA.max(stat: PrimeStat): Int {
    if (pcdata == null || level > LEVEL_IMMORTAL) {
        return 25
    }
    val max = 20 + race.stats[stat] + clazz.stats[stat]
    return Math.min(max, 25)
}

fun CHAR_DATA?.curr_stat(stat: PrimeStat): Int {
    if (this == null) return 20
    val max = if (pcdata == null || level > LEVEL_IMMORTAL) 25 else Math.min(max(stat), 25)
    return URANGE(3, perm_stat[stat] + mod_stat[stat], max)
}

val CHAR_DATA?.tohit get() = StrengthModifiers[curr_stat(PrimeStat.Strength)].tohit
val CHAR_DATA?.todam get() = StrengthModifiers[curr_stat(PrimeStat.Strength)].todam
val CHAR_DATA?.carry get() = StrengthModifiers[curr_stat(PrimeStat.Strength)].carry
val CHAR_DATA?.wield get() = StrengthModifiers[curr_stat(PrimeStat.Strength)].wield
val CHAR_DATA?.learn get() = IntelligenceModifiers[curr_stat(PrimeStat.Intelligence)].learn
val CHAR_DATA?.defensive get() = DexterityModifiers[curr_stat(PrimeStat.Dexterity)].defensive
val CHAR_DATA?.extraPractices get() = WisdomModifiers[curr_stat(PrimeStat.Wisdom)].practice
val CHAR_DATA?.hitp get() = ConstitutionModifiers[curr_stat(PrimeStat.Constitution)].hitp
val CHAR_DATA?.shock get() = ConstitutionModifiers[curr_stat(PrimeStat.Constitution)].shock

/** Attribute bonus tables.*/

class StrengthModifier(val tohit: Int, val todam: Int, val carry: Int, val wield: Int)

val StrengthModifiers = arrayOf(
        StrengthModifier(-5, -4, 0, 0),  // 0
        StrengthModifier(-5, -4, 3, 1),  // 1
        StrengthModifier(-3, -2, 3, 2),  // 2
        StrengthModifier(-3, -1, 10, 3), // 3
        StrengthModifier(-2, -1, 25, 4), // 4
        StrengthModifier(-2, -1, 55, 5), // 5
        StrengthModifier(-1, 0, 80, 6),  // 6
        StrengthModifier(-1, 0, 90, 7),  // 7
        StrengthModifier(0, 0, 100, 8),  // 8
        StrengthModifier(0, 0, 100, 9),  // 9
        StrengthModifier(0, 0, 115, 10), // 10
        StrengthModifier(0, 0, 115, 11), // 11
        StrengthModifier(0, 0, 130, 12), // 12
        StrengthModifier(0, 0, 130, 13), // 13
        StrengthModifier(0, 1, 140, 14), // 14
        StrengthModifier(1, 1, 150, 15), // 15
        StrengthModifier(1, 2, 165, 16), // 16
        StrengthModifier(2, 3, 180, 22), // 17
        StrengthModifier(2, 3, 200, 25), // 18
        StrengthModifier(3, 4, 225, 30), // 19
        StrengthModifier(3, 5, 250, 35), // 20
        StrengthModifier(4, 6, 300, 40), // 21
        StrengthModifier(4, 6, 350, 45), // 22
        StrengthModifier(5, 7, 400, 50), // 23
        StrengthModifier(5, 8, 450, 55), // 24
        StrengthModifier(6, 9, 500, 60)) // 25

class IntelligenceModifier(val learn: Int)

val IntelligenceModifiers = arrayOf(
        IntelligenceModifier(3),  // 0
        IntelligenceModifier(5),  // 1
        IntelligenceModifier(7),  // 2
        IntelligenceModifier(8),  // 3
        IntelligenceModifier(9),  // 4
        IntelligenceModifier(10), // 5
        IntelligenceModifier(11), // 6
        IntelligenceModifier(12), // 7
        IntelligenceModifier(13), // 8
        IntelligenceModifier(15), // 9
        IntelligenceModifier(17), // 10
        IntelligenceModifier(19), // 11
        IntelligenceModifier(22), // 12
        IntelligenceModifier(25), // 13
        IntelligenceModifier(28), // 14
        IntelligenceModifier(31), // 15
        IntelligenceModifier(34), // 16
        IntelligenceModifier(37), // 17
        IntelligenceModifier(40), // 18
        IntelligenceModifier(44), // 19
        IntelligenceModifier(49), // 20
        IntelligenceModifier(55), // 21
        IntelligenceModifier(60), // 22
        IntelligenceModifier(70), // 23
        IntelligenceModifier(80), // 24
        IntelligenceModifier(85)) // 25

class DexterityModifier(val defensive: Int)

val DexterityModifiers = arrayOf(
        DexterityModifier(60),   // 0
        DexterityModifier(50),   // 1
        DexterityModifier(50),   // 2
        DexterityModifier(40),   // 3
        DexterityModifier(30),   // 4
        DexterityModifier(20),   // 5
        DexterityModifier(10),   // 6
        DexterityModifier(0),    // 7
        DexterityModifier(0),    // 8
        DexterityModifier(0),    // 9
        DexterityModifier(0),    // 10
        DexterityModifier(0),    // 11
        DexterityModifier(0),    // 12
        DexterityModifier(0),    // 13
        DexterityModifier(0),    // 14
        DexterityModifier(-10),  // 15
        DexterityModifier(-15),  // 16
        DexterityModifier(-20),  // 17
        DexterityModifier(-30),  // 18
        DexterityModifier(-40),  // 19
        DexterityModifier(-50),  // 20
        DexterityModifier(-60),  // 21
        DexterityModifier(-75),  // 22
        DexterityModifier(-90),  // 23
        DexterityModifier(-105), // 24
        DexterityModifier(-120)) // 25


class WisdomModifier(val practice: Int)

val WisdomModifiers = arrayOf(
        WisdomModifier(0), // 0
        WisdomModifier(0), // 1
        WisdomModifier(0), // 2
        WisdomModifier(0), // 3
        WisdomModifier(0), // 4
        WisdomModifier(1), // 5
        WisdomModifier(1), // 6
        WisdomModifier(1), // 7
        WisdomModifier(1), // 8
        WisdomModifier(1), // 9
        WisdomModifier(1), // 10
        WisdomModifier(1), // 11
        WisdomModifier(1), // 12
        WisdomModifier(1), // 13
        WisdomModifier(1), // 14
        WisdomModifier(2), // 15
        WisdomModifier(2), // 16
        WisdomModifier(2), // 17
        WisdomModifier(3), // 18
        WisdomModifier(3), // 19
        WisdomModifier(3), // 20
        WisdomModifier(3), // 21
        WisdomModifier(4), // 22
        WisdomModifier(4), // 23
        WisdomModifier(4), // 24
        WisdomModifier(5)) // 25

class ConstitutionModifier(val hitp: Int, val shock: Int)

val ConstitutionModifiers = arrayOf(
        ConstitutionModifier(0, 20),  // 0
        ConstitutionModifier(1, 25),  // 1
        ConstitutionModifier(1, 30),  // 2
        ConstitutionModifier(2, 35),  // 3
        ConstitutionModifier(3, 40),  // 4
        ConstitutionModifier(4, 45),  // 5
        ConstitutionModifier(5, 50),  // 6
        ConstitutionModifier(6, 55),  // 7
        ConstitutionModifier(7, 60),  // 8
        ConstitutionModifier(8, 65),  // 9
        ConstitutionModifier(9, 70),  // 10
        ConstitutionModifier(10, 75), // 11
        ConstitutionModifier(11, 80), // 12
        ConstitutionModifier(12, 85), // 13
        ConstitutionModifier(13, 88), // 14
        ConstitutionModifier(14, 90), // 15
        ConstitutionModifier(15, 95), // 16
        ConstitutionModifier(16, 97), // 17
        ConstitutionModifier(17, 99), // 18
        ConstitutionModifier(18, 99), // 19
        ConstitutionModifier(19, 99), // 20
        ConstitutionModifier(20, 99), // 21
        ConstitutionModifier(21, 99), // 22
        ConstitutionModifier(22, 99), // 23
        ConstitutionModifier(23, 99), // 24
        ConstitutionModifier(24, 99)) // 25

/** Function for save processes. */
fun get_stat_alias(ch: CHAR_DATA, where: PrimeStat): String {
    val s = ch.curr_stat(where)
    return when (where) {
        PrimeStat.Strength -> {
            when {
                s > 22 -> "Titanic"
                s >= 20 -> "Herculean"
                s >= 18 -> "Strong"
                s >= 14 -> "Average"
                s >= 10 -> "Poor"
                else -> "Weak"
            }
        }
        PrimeStat.Wisdom -> {
            when {
                s > 22 -> "Excellent"
                s >= 20 -> "Wise"
                s >= 18 -> "Good"
                s >= 14 -> "Average"
                s >= 10 -> "Dim"
                else -> "Fool"
            }
        }
        PrimeStat.Constitution -> {
            when {
                s > 22 -> "Iron"
                s >= 20 -> "Hearty"
                s >= 18 -> "Healthy"
                s >= 14 -> "Average"
                s >= 10 -> "Poor"
                else -> "Fragile"
            }
        }
        PrimeStat.Intelligence -> {
            when {
                s > 22 -> "Genius"
                s >= 20 -> "Clever"
                s >= 18 -> "Good"
                s >= 14 -> "Average"
                s >= 10 -> "Poor"
                else -> "Hopeless"
            }
        }
        PrimeStat.Dexterity -> {
            when {
                s > 22 -> "Fast"
                s >= 20 -> "Quick"
                s >= 18 -> "Dexterous"
                s >= 14 -> "Average"
                s >= 10 -> "Clumsy"
                else -> "Slow"
            }
        }
        PrimeStat.Charisma -> {
            when {
                s > 22 -> "Charismatic"
                s >= 20 -> "Familiar"
                s >= 18 -> "Good"
                s >= 14 -> "Average"
                s >= 10 -> "Poor"
                else -> "Mongol"
            }
        }
    }
}
