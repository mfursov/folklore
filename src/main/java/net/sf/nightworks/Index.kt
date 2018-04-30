package net.sf.nightworks

import net.sf.nightworks.model.Area
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Connection
import net.sf.nightworks.model.HELP_DATA
import net.sf.nightworks.model.MOB_INDEX_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.ObjProto
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Shop
import net.sf.nightworks.model.Social

object Index {

    /**
     * MOB_INDEX_DATA by vnum.
     */
    val MOB_INDEX = mutableMapOf<Int, MOB_INDEX_DATA>()

    /**
     * OBJ_INDEX_DATA by vnum.
     */
    val OBJ_INDEX = mutableMapOf<Int, ObjProto>()

    /**
     * ROOM_INDEX_DATA by vnum.
     */
    val ROOM_INDEX = mutableMapOf<Int, ROOM_INDEX_DATA>()

    /**
     * List of help topics.
     */
    val HELP = mutableListOf<HELP_DATA>()

    /**
     * All shops .
     */
    val SHOPS = mutableListOf<Shop>()

    /**
     * All characters (mobs + players) .
     */
    val CHARS = mutableListOf<CHAR_DATA>()

    /**
     * All objects.
     */
    val OBJECTS = mutableListOf<Obj>()

    /**
     * List of all areas.
     */
    val AREAS = mutableListOf<Area>()

    /**
     * All active service connections.
     */
    val CONNECTIONS = mutableListOf<Connection>()

    /**
     * Active players.
     */
    val PLAYERS get() = Index.CONNECTIONS.filter { it.pc != null }.map { it -> it.pc!! }

    /**
     * Set of all socials
     */
    val SOCIALS = mutableListOf<Social>()
}
