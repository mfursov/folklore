package net.sf.nightworks.act.mob

import create_greet_prog
import net.sf.nightworks.MPROG_AREA
import net.sf.nightworks.MPROG_BRIBE
import net.sf.nightworks.MPROG_DEATH
import net.sf.nightworks.MPROG_ENTRY
import net.sf.nightworks.MPROG_FIGHT
import net.sf.nightworks.MPROG_GIVE
import net.sf.nightworks.MPROG_GREET
import net.sf.nightworks.MPROG_SPEECH
import net.sf.nightworks.util.bug
import net.sf.nightworks.model.MOB_INDEX_DATA
import net.sf.nightworks.util.SET_BIT
import java.lang.System.exit

fun assign_mob_prog(mobindex: MOB_INDEX_DATA, progtype: String, name: String) {
    try {
        when (progtype) {
            "bribe_prog" -> {
                mobindex.mprogs.bribe_prog = create_bribe_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_BRIBE)
            }
            "entry_prog" -> {
                mobindex.mprogs.entry_prog = create_entry_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_ENTRY)
            }
            "greet_prog" -> {
                mobindex.mprogs.greet_prog = create_greet_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_GREET)
            }
            "fight_prog" -> {
                mobindex.mprogs.fight_prog = create_fight_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_FIGHT)
            }
            "death_prog" -> {
                mobindex.mprogs.death_prog = create_death_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_DEATH)
            }
            "area_prog" -> {
                mobindex.mprogs.area_prog = create_area_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_AREA)
            }
            "speech_prog" -> {
                mobindex.mprogs.speech_prog = create_speech_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_SPEECH)
            }
            "give_prog" -> {
                mobindex.mprogs.give_prog = create_give_prog(name)
                mobindex.progtypes = SET_BIT(mobindex.progtypes, MPROG_GIVE)
            }
        }
    } catch (e: Exception) {
        bug("Load_mprogs: 'M': invalid program type for vnum ${mobindex.vnum}")
        exit(1)
    }
}

