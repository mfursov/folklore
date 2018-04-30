package net.sf.nightworks.model

typealias OPROG_FUN_WEAR = (obj: Obj, ch: CHAR_DATA) -> Unit
typealias OPROG_FUN_REMOVE = (obj: Obj, ch: CHAR_DATA) -> Unit
typealias OPROG_FUN_GET = (obj: Obj, ch: CHAR_DATA) -> Unit
typealias OPROG_FUN_DROP = (obj: Obj, ch: CHAR_DATA) -> Unit
typealias OPROG_FUN_SAC = (obj: Obj, ch: CHAR_DATA) -> Boolean
typealias OPROG_FUN_ENTRY = (obj: Obj) -> Unit
typealias OPROG_FUN_GIVE = (obj: Obj, from: CHAR_DATA, to: CHAR_DATA) -> Unit
typealias OPROG_FUN_GREET = (obj: Obj, ch: CHAR_DATA) -> Unit
typealias OPROG_FUN_FIGHT = (obj: Obj, ch: CHAR_DATA) -> Unit
typealias OPROG_FUN_DEATH = (obj: Obj, ch: CHAR_DATA) -> Boolean
typealias OPROG_FUN_SPEECH = (obj: Obj, ch: CHAR_DATA, speech: String) -> Unit
typealias OPROG_FUN_AREA = (obj: Obj) -> Unit

class OPROG_DATA {
    var wear_prog: OPROG_FUN_WEAR? = null
    var remove_prog: OPROG_FUN_REMOVE? = null
    var get_prog: OPROG_FUN_GET? = null
    var drop_prog: OPROG_FUN_DROP? = null
    var sac_prog: OPROG_FUN_SAC? = null
    var entry_prog: OPROG_FUN_ENTRY? = null
    var give_prog: OPROG_FUN_GIVE? = null
    var greet_prog: OPROG_FUN_GREET? = null
    var fight_prog: OPROG_FUN_FIGHT? = null
    var death_prog: OPROG_FUN_DEATH? = null
    var speech_prog: OPROG_FUN_SPEECH? = null
    var area_prog: OPROG_FUN_AREA? = null
}
