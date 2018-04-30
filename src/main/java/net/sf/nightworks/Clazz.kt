package net.sf.nightworks

import net.sf.nightworks.model.Align
import net.sf.nightworks.model.Ethos
import net.sf.nightworks.model.Pose
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Sex
import java.util.ArrayList

class Clazz constructor(val name: String) {

    /** Three-letter name for 'who'  */
    var who_name = ""

    /** Prime attribute */
    var attr_prime: PrimeStat? = null

    /** First weapon */
    var weapon = 0

    /** Vnum of guild rooms  */
    val guildVnums = ArrayList<VNum>()

    /** Maximum skill level */
    var skill_adept = 0

    /* Thac0 for level  0 */
    var thac0_00 = 0

    /* Thac0 for level 32 */
    var thac0_32 = 0

    /** HP rate gained on leveling */
    var hp_rate = 0

    /** Mana rate gained on leveling */
    var mana_rate = 0

    /* Class gains mana on level */
    var fMana = false

    /* Cost in exp of class */
    var points = 0

    /* Stat modifiers */
    val stats = PrimeStat.statsOf(0)

    /* Alignment */
    var align: Align? = Align.Neutral

    var sex: Sex? = null

    var ethos: Ethos? = null

    val id: Int

    val poses = ArrayList<Pose>()

    val femaleTitles = Array(MAX_LEVEL + 1, { _ -> "" })

    val maleTitles = Array(MAX_LEVEL + 1, { _ -> "" })

    init {
        assert(Clazz.lookup(name) == null)
        id = idGen++
        clazzByName[name.toLowerCase()] = this
        classesList.add(this)
    }

    fun getTitle(level: Int, sex: Sex): String = if (sex.isFemale()) femaleTitles[level] else maleTitles[level]

    private fun validate(): Boolean = name.isNotEmpty() && who_name.isNotEmpty() && weapon != 0 && guildVnums.size > 0

    companion object {
        private var idGen: Int = 0
        private val clazzByName = mutableMapOf<String, Clazz>()
        private val classesList = mutableListOf<Clazz>()

        val MOB_CLASS = Clazz("mob")
        val NECROMANCER = Clazz("necromancer")
        val NINJA = Clazz("ninja")
        val SAMURAI = Clazz("samurai")
        val THIEF = Clazz("thief")
        val VAMPIRE = Clazz("vampire")
        val WARRIOR = Clazz("warrior")

        val classes: List<Clazz> get() = this.classesList

        fun validate() = classes.filterNot { it.validate() }.forEach { throw RuntimeException("Misconfigired class: ${it.name}") }

        fun lookupNN(className: String): Clazz = lookup(className) ?: throw RuntimeException("Clazz not found:" + className)

        fun lookup(className: String): Clazz? = clazzByName[className.toLowerCase()]

        fun lookupByPrefix(prefix: String): Clazz? = lookup(prefix) ?: classes.firstOrNull { it.name.startsWith(prefix) }
    }
}
