package net.sf.nightworks.model

import net.sf.nightworks.Clazz
import net.sf.nightworks.PAGE_LEN
import net.sf.nightworks.Race
import net.sf.nightworks.SPEC_FUN
import net.sf.nightworks.current_time

/**
 * One character (PC or NPC).
 */
class CHAR_DATA {
    var master: CHAR_DATA? = null
    var leader: CHAR_DATA? = null
    var fighting: CHAR_DATA? = null
    var reply: CHAR_DATA? = null
    var last_fought: CHAR_DATA? = null
    var last_fight_time: Long = -1
    var last_death_time: Long = -1
    var pet: CHAR_DATA? = null
    var doppel: CHAR_DATA? = null
    var guarding: CHAR_DATA? = null
    var guarded_by: CHAR_DATA? = null
    var spec_fun: SPEC_FUN? = null

    lateinit var pIndexData: MOB_INDEX_DATA
    var pcdata: PC_DATA? = null //todo: make val!
    var affected: Affect? = null
    /** Unread notes*/
    var noteData: NOTE_DATA? = null

    /** List of objects this character carries */
    val carrying = mutableListOf<Obj>()

    var on: Obj? = null

    //todo: on exit remove char from all references!!!
    lateinit var room: ROOM_INDEX_DATA

    /**
     * Used to store player's room when player is moved to 'limbo'.
     */
    var was_in_room: ROOM_INDEX_DATA? = null

    var name = ""
    var id: Int = 0
    var short_desc = ""
    var long_desc = ""
    var description = ""
    var prompt = ""
    var prefix: String = ""
    var group: Int = 0
    var sex: Sex = Sex.None
    lateinit var clazz: Clazz
    lateinit var race: Race
    var cabal: Clan = Clan.None
    var hometown: HomeTown = HomeTown.Midgaard
    var level: Int = 0
    var trust: Int = 0
    var played: Int = 0
    var lines = PAGE_LEN  /* for the pager */
    var logon = current_time
    var timer: Int = 0
    var wait: Int = 0
    var daze: Int = 0
    var hit = 20
    var max_hit = 20
    var mana = 100
    var max_mana = 100
    var move = 100
    var max_move = 100
    var gold: Int = 0
    var silver: Int = 0
    var exp: Int = 0
    var act: Long = 0
    var comm: Long = 0   /* RT added to pad the vector */
    val wiznet = mutableListOf<Wiznet>()
    var imm_flags: Long = 0
    var res_flags: Long = 0
    var vuln_flags: Long = 0
    var invis_level: Int = 0
    var incog_level: Int = 0
    var affected_by: Long = 0
    var position = Pos.Standing
    var practice: Int = 0
    var train: Int = 0
    var carry_weight: Int = 0
    var carry_number: Int = 0
    var saving_throw: Int = 0
    var hitroll: Int = 0
    var damroll: Int = 0
    val armor = intArrayOf(100, 100, 100, 100)
    var wimpy: Int = 0
    /* stats */
    val perm_stat = PrimeStat.statsOf(13)
    val mod_stat = PrimeStat.statsOf(0)
    /* parts stuff */
    var form: Long = 0
    var parts: Long = 0
    var size: Size = Size.Medium
    var material: String? = null
    /* mobile stuff */
    var off_flags: Long = 0
    val damage = IntArray(3)
    var attack: AttackType = AttackType.None
    var start_pos: Pos = Pos.Dead
    var default_pos: Pos = Pos.Dead
    var status: Int = 0
    var progtypes: Long = 0
    var extracted: Boolean = false
    var in_mind: String = ""
    var quest: Long = 0
    var religion: Religion = Religion.None
    var hunting: CHAR_DATA? = null    /* hunt data */
    var endur: Int = 0
    var riding: Boolean = false /* mount data */
    var mount: CHAR_DATA? = null

    /** Language character speaks now */
    var language: Language = Language.Common
    var new_character = true


    var alignment: Int = 0
    val align get() = Align.get(alignment)
    fun isGood() = align.isGood()
    fun isEvil() = align.isEvil()
    fun isNeutralA() = align.isNeutral()

    var ethos: Ethos = Ethos.Neutral
    fun isLawful() = ethos.isLawful()
    fun isChaotic() = ethos.isChaotic()
    fun isNeutralE() = ethos.isNeutral()
}
