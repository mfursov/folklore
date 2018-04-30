package net.sf.nightworks.act.obj

import net.sf.nightworks.OPROG_AREA
import net.sf.nightworks.OPROG_DEATH
import net.sf.nightworks.OPROG_DROP
import net.sf.nightworks.OPROG_ENTRY
import net.sf.nightworks.OPROG_FIGHT
import net.sf.nightworks.OPROG_GET
import net.sf.nightworks.OPROG_GIVE
import net.sf.nightworks.OPROG_GREET
import net.sf.nightworks.OPROG_REMOVE
import net.sf.nightworks.OPROG_SAC
import net.sf.nightworks.OPROG_SPEECH
import net.sf.nightworks.OPROG_WEAR
import net.sf.nightworks.util.bug
import net.sf.nightworks.model.ObjProto
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.exit

fun assign_obj_prog(objindex: ObjProto, progtype: String, name: String) {
    try {
        when (progtype) {
            "wear_prog" -> {
                objindex.oprogs.wear_prog = create_wear_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_WEAR)
            }
            "remove_prog" -> {
                objindex.oprogs.remove_prog = create_remove_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_REMOVE)
            }
            "get_prog" -> {
                objindex.oprogs.get_prog = create_get_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_GET)
            }
            "drop_prog" -> {
                objindex.oprogs.drop_prog = create_drop_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_DROP)
            }
            "sac_prog" -> {
                objindex.oprogs.sac_prog = create_sac_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_SAC)
            }
            "entry_prog" -> {
                objindex.oprogs.entry_prog = create_entry_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_ENTRY)
            }
            "give_prog" -> {
                objindex.oprogs.give_prog = create_give_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_GIVE)
            }
            "greet_prog" -> {
                objindex.oprogs.greet_prog = create_greet_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_GREET)
            }
            "fight_prog" -> {
                objindex.oprogs.fight_prog = create_fight_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_FIGHT)
            }
            "death_prog" -> /* returning true prevents death */ {
                objindex.oprogs.death_prog = create_death_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_DEATH)
            }
            "speech_prog" -> {
                objindex.oprogs.speech_prog = create_speech_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_SPEECH)
            }
            "area_prog" -> {
                objindex.oprogs.area_prog = create_area_prog(name)
                objindex.progtypes = SET_BIT(objindex.progtypes, OPROG_AREA)
            }
        }
    } catch (e: Exception) {
        bug("Load_oprogs: 'O': invalid program type for vnum ${objindex.vnum}")
        exit(1)
    }
}


