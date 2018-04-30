package net.sf.nightworks.model

typealias MPROG_FUN_BRIBE = (mob: CHAR_DATA, ch: CHAR_DATA, amount: Int) -> Unit
typealias MPROG_FUN_ENTRY = (mob: CHAR_DATA) -> Unit
typealias MPROG_FUN_GIVE = (mob: CHAR_DATA, ch: CHAR_DATA, obj: Obj) -> Unit
typealias MPROG_FUN_GREET = (mob: CHAR_DATA, ch: CHAR_DATA) -> Unit
typealias MPROG_FUN_FIGHT = (mob: CHAR_DATA, victim: CHAR_DATA) -> Unit
typealias MPROG_FUN_AREA = (mob: CHAR_DATA) -> Unit
typealias MPROG_FUN_SPEECH = (mob: CHAR_DATA, ch: CHAR_DATA, speech: String) -> Unit

/** returning true prevents death */
typealias MPROG_FUN_DEATH = (mob: CHAR_DATA) -> Boolean

class MPROG_DATA {
    var bribe_prog: MPROG_FUN_BRIBE? = null
    var entry_prog: MPROG_FUN_ENTRY? = null
    var give_prog: MPROG_FUN_GIVE? = null
    var greet_prog: MPROG_FUN_GREET? = null
    var fight_prog: MPROG_FUN_FIGHT? = null
    var death_prog: MPROG_FUN_DEATH? = null
    var area_prog: MPROG_FUN_AREA? = null
    var speech_prog: MPROG_FUN_SPEECH? = null
}
