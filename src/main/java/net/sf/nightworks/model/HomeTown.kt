package net.sf.nightworks.model

/** 3 values are vnums for (good, neutral, evil) */
enum class HomeTown(
        val id: Int,
        val displayName: String,
        val altar: IntArray,
        val recall: IntArray,
        val pit: IntArray,
        private val alignRestrict: Align? = null) {
    Midgaard(0, "Midgaard", intArrayOf(3070, 3054, 3072), intArrayOf(3068, 3001, 3071), intArrayOf(3069, 3054, 3072)),
    NewThalos(1, "New Thalos", intArrayOf(9605, 9605, 9605), intArrayOf(9609, 9609, 9609), intArrayOf(9609, 9609, 9609)),
    Titans(2, "Titans", intArrayOf(18127, 18127, 18127), intArrayOf(18126, 18126, 18126), intArrayOf(18127, 18127, 18127), Align.Good),
    NewOfcol(3, "New Ofcol", intArrayOf(669, 669, 669), intArrayOf(698, 698, 698), intArrayOf(669, 669, 669), Align.Neutral),
    OldMidgaard(4, "Old Midgaard", intArrayOf(5386, 5386, 5386), intArrayOf(5379, 5379, 5379), intArrayOf(5386, 5386, 5386), Align.Evil);

    override fun toString() = displayName

    fun isAlignAllowed(align: Align) = alignRestrict == null || alignRestrict == align

    companion object {
        fun of(id: Int) = ofOrNull(id) ?: Midgaard
        fun ofOrNull(id: Int) = values().firstOrNull { it.id == id }
    }
}

