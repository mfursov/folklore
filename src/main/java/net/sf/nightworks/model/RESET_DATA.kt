package net.sf.nightworks.model

/**
 * Area-reset definition.
 *
 *
 * Reset commands:
 * '*': comment
 * 'M': read a mobile
 * 'O': read an object
 * 'P': put object in object
 * 'G': give object to mobile
 * 'E': equip object to mobile
 * 'D': set state of door
 * 'R': randomize room exits
 * 'S': stop (end of list)
 */

class RESET_DATA {
    var next: RESET_DATA? = null
    var command: Char = ' '
    var arg1: Int = 0
    var arg2: Int = 0
    var arg3: Int = 0
    var arg4: Int = 0
}
