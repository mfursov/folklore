@file:Suppress("unused")

package net.sf.nightworks

import net.sf.nightworks.model.Align
import net.sf.nightworks.model.Language
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Size
import java.util.TreeMap

class Race constructor(val name: String, val pcRace: Boolean = false) {

    /** act bits for the race */
    var act = 0L

    /** aff bits for the race */
    var aff = 0L

    /** off bits for the race */
    var off = 0L

    /** imm bits for the race */
    var imm = 0L

    /** res bits for the race */
    var res = 0L

    /** vuln bits for the race */
    var vuln = 0L

    /** default form flag for the race */
    var form = 0L

    /** default parts for the race */
    var parts = 0L

    /** How to show in 'who' listing */
    var who_name = "???"

    /* extra cost in exp of the race */
    var points: Int = 0

    /* Race affect to class. Sorted by class name. */
    private val rclass = TreeMap<Clazz, RaceToClassModifier>(Comparator { c1, c2 -> c1.name.compareTo(c2.name) })

    /* bonus skills for the race */
    var skills = arrayOf<Skill>()

    /* starting stats   */
    var stats = PrimeStat.statsOf(0)

    /* maximum stats    */
    var max_stats = PrimeStat.statsOf(20)

    /* aff bits for the race*/
    var size: Size = Size.Medium

    /* Initial hp bonus     */
    var hp_bonus: Int = 0

    /* Initial mana bonus   */
    var mana_bonus: Int = 0

    /* Initial practice bonus */
    var prac_bonus: Int = 0

    /* Alignment restriction */
    var align: Align? = null

    /* language     */
    var language: Language = Language.Common

    fun getClassModifier(clazz: Clazz): RaceToClassModifier? = rclass[clazz]

    fun addClassModifier(clazz: Clazz, mod: RaceToClassModifier) = rclass.put(clazz, mod)

    init {
        assert(lookup(name) == null)
        racesMap.put(name, this)
        racesList.add(this)
    }

    private fun validate(): Boolean = name.isNotEmpty()

    companion object {
        private val racesMap = mutableMapOf<String, Race>()
        private val racesList = mutableListOf<Race>()
        val races: List<Race> get() = this.racesList

        // set of predefined races.
        // these races are the same as from configuration file
        // except 2 difference:
        // 1. Predefined race can be referenced from code by a constant
        // 2. Predefined race must be found in configuration file during startup
        val HUMAN = Race("human", true)
        val ELF = Race("elf", true)
        val HALF_ELF = Race("half-elf", true)
        val DARK_ELF = Race("dark-elf", true)
        val ROCKSEER = Race("rockseer", true)
        val DWARF = Race("dwarf", true)
        val SVIRFNEBLI = Race("svirfnebli", true)
        val DUERGAR = Race("duergar", true)
        val ARIAL = Race("arial", true)
        val GNOME = Race("gnome", true)
        val STORM_GIANT = Race("storm giant", true)
        val CLOUD_GIANT = Race("cloud giant", true)
        val FIRE_GIANT = Race("fire giant", true)
        val FROST_GIANT = Race("frost giant", true)
        val FELAR = Race("felar", true)
        val GITHYANKI = Race("githyanki", true)
        val SATYR = Race("satyr", true)
        val TROLL = Race("troll", true)

        fun lookup(raceName: String): Race? = racesMap[raceName.toLowerCase()]

        fun lookupNN(name: String): Race = lookup(name) ?: throw RuntimeException("Race not found: $name")

        fun lookupByPrefix(prefix: String): Race? = lookup(prefix) ?: races.firstOrNull { it.name.startsWith(prefix) }

        fun validate() {
            races.filterNot { it.validate() }.forEach { throw RuntimeException("Misconfigired race: " + it.name) }
        }
    }
}

class RaceToClassModifier(val clazz: Clazz, val expMult: Int)
