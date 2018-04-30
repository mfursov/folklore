package net.sf.nightworks

import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Ethos
import net.sf.nightworks.model.HomeTown
import net.sf.nightworks.model.Language
import net.sf.nightworks.model.Pos


fun new_char(): CHAR_DATA {
    val ch = CHAR_DATA()
    ch.name = ""
    ch.short_desc = ""
    ch.long_desc = ""
    ch.description = ""
    ch.prompt = ""
    ch.prefix = ""
    ch.logon = current_time
    ch.lines = PAGE_LEN
    for (i in 0..3) {
        ch.armor[i] = 100
    }
    ch.position = Pos.Standing
    ch.hit = 20
    ch.max_hit = 20
    ch.mana = 100
    ch.max_mana = 100
    ch.move = 100
    ch.max_move = 100

    ch.ethos = Ethos.Neutral
    ch.cabal = Clan.None
    ch.hometown = HomeTown.Midgaard
    ch.guarded_by = null
    ch.guarding = null
    ch.doppel = null
    ch.language = Language.Common
    ch.perm_stat.fill(13)
    ch.mod_stat.fill(0)
    return ch
}


fun free_char(ch: CHAR_DATA) {
    ch.extracted = true
    ch.carrying.forEach { obj -> extract_obj(obj) }
    var paf: Affect? = ch.affected
    while (paf != null) {
        val paf_next = paf.next
        affect_remove(ch, paf)
        paf = paf_next
    }

    ch.extracted = false
}

/* stuff for setting ids */
private var last_mob_id: Int = 0

fun nextMobId() = ++last_mob_id
