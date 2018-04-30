package net.sf.nightworks.model

import net.sf.nightworks.MAX_ALIAS
import net.sf.nightworks.MAX_TIME_LOG
import net.sf.nightworks.Skill

/** Data which only PC's have. */
class PC_DATA {
    var buffer = StringBuilder()
    var pwd: String = ""
    var bamfin: String = ""
    var bamfout: String = ""
    var title: String = ""
    var last_note: Long = 0
    var last_idea: Long = 0
    var last_penalty: Long = 0
    var last_news: Long = 0
    var last_changes: Long = 0
    var perm_hit: Int = 0
    var perm_mana: Int = 0
    var perm_move: Int = 0
    var true_sex: Sex = Sex.Male
    var last_level: Int = 0
    val condition = IntArray(Skill.MAX_SKILLS)
    val learned = IntArray(Skill.MAX_SKILLS)
    var points: Int = 0
    var confirm_delete: Boolean = false
    var confirm_remort: Boolean = false
    val alias = arrayOfNulls<String>(MAX_ALIAS)
    val alias_sub = arrayOfNulls<String>(MAX_ALIAS)
    var bank_s: Int = 0
    var bank_g: Int = 0
    var death: Int = 0
    var played: Int = 0
    var anti_killed: Int = 0
    var has_killed: Int = 0
    var questgiver: Int = 0 /* quest */
    var questpoints: Int = 0    /* quest */
    var nextquest: Int = 0  /* quest */
    var countdown: Int = 0  /* quest */
    var questobj: Int = 0   /* quest */
    var questmob: Int = 0       /* quest */
    var time_flag: Long = 0  /* time log problem */
    val log_time = IntArray(MAX_TIME_LOG) /* min.s of playing each day */
    /* 0th day is the current day  */
}
