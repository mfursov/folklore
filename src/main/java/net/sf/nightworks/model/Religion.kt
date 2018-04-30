package net.sf.nightworks.model

import net.sf.nightworks.OBJ_VNUM_TATTOO_AHRUMAZDA
import net.sf.nightworks.OBJ_VNUM_TATTOO_APOLLON
import net.sf.nightworks.OBJ_VNUM_TATTOO_ARES
import net.sf.nightworks.OBJ_VNUM_TATTOO_ATHENA
import net.sf.nightworks.OBJ_VNUM_TATTOO_DEIMOS
import net.sf.nightworks.OBJ_VNUM_TATTOO_EHRUMEN
import net.sf.nightworks.OBJ_VNUM_TATTOO_EROS
import net.sf.nightworks.OBJ_VNUM_TATTOO_GOKTENGRI
import net.sf.nightworks.OBJ_VNUM_TATTOO_HEPHAESTUS
import net.sf.nightworks.OBJ_VNUM_TATTOO_HERA
import net.sf.nightworks.OBJ_VNUM_TATTOO_MARS
import net.sf.nightworks.OBJ_VNUM_TATTOO_ODIN
import net.sf.nightworks.OBJ_VNUM_TATTOO_PHOBOS
import net.sf.nightworks.OBJ_VNUM_TATTOO_PROMETHEUS
import net.sf.nightworks.OBJ_VNUM_TATTOO_SIEBELE
import net.sf.nightworks.OBJ_VNUM_TATTOO_VENUS
import net.sf.nightworks.OBJ_VNUM_TATTOO_ZEUS


/** God's Name, name of religion, tattoo vnum */
enum class Religion(val leader: String, val title: String, val tattooVnum: Int) {
    None("", "None", 0),
    AtumRa("Atum-Ra", "Lawful Good", OBJ_VNUM_TATTOO_APOLLON),
    Zeus("Zeus", "Neutral Good", OBJ_VNUM_TATTOO_ZEUS),
    Siebele("Siebele", "true Neutral", OBJ_VNUM_TATTOO_SIEBELE),
    Shamash("Shamash", "God of Justice", OBJ_VNUM_TATTOO_HEPHAESTUS),
    Ahuramazda("Ahuramazda", "Chaotic Good", OBJ_VNUM_TATTOO_EHRUMEN),
    Ehrumen("Ehrumen", "Chaotic Evil", OBJ_VNUM_TATTOO_AHRUMAZDA),
    Deimos("Deimos", "Lawful Evil", OBJ_VNUM_TATTOO_DEIMOS),
    Phobos("Phobos", "Neutral Evil", OBJ_VNUM_TATTOO_PHOBOS),
    Odin("Odin", "Lawful Neutral", OBJ_VNUM_TATTOO_ODIN),
    Teshub("Teshub", "Chaotic Neutral", OBJ_VNUM_TATTOO_MARS),
    Ares("Ares", "God of War", OBJ_VNUM_TATTOO_ATHENA),
    Goktengri("Goktengri", "God of Honor", OBJ_VNUM_TATTOO_GOKTENGRI),
    Hera("Hera", "God of Hate", OBJ_VNUM_TATTOO_HERA),
    Venus("Venus", "God of Beauty", OBJ_VNUM_TATTOO_VENUS),
    Seth("Seth", "God of Anger", OBJ_VNUM_TATTOO_ARES),
    Enki("Enki", "God of Knowledge", OBJ_VNUM_TATTOO_PROMETHEUS),
    Eros("Eros", "God of Love", OBJ_VNUM_TATTOO_EROS);

    fun isNone() = this === None

    fun isNotAllowed(ch: CHAR_DATA) = !isAllowed(ch)

    private fun isAllowed(ch: CHAR_DATA) = when (this) {
        AtumRa -> ch.isGood() && ch.isLawful()
        Zeus -> ch.isGood() && ch.isNeutralE()
        Siebele -> ch.isNeutralA() || ch.isNeutralE()
        Ehrumen -> ch.isGood() || ch.isChaotic()
        Ahuramazda -> ch.isEvil() || ch.isChaotic()
        Deimos -> ch.isEvil() || ch.isLawful()
        Phobos -> ch.isEvil() || ch.isNeutralE()
        Odin -> ch.isNeutralA() || ch.isLawful()
        Teshub -> ch.isNeutralA() || ch.isChaotic()
        else -> true
    }

    companion object {
        fun fromInt(idx: Int) = if (idx <= 0 || idx >= Religion.values().size) None else Religion.values()[idx]
    }
}
