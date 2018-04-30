package net.sf.nightworks.model

import net.sf.nightworks.IM
import net.sf.nightworks.L1
import net.sf.nightworks.L2
import net.sf.nightworks.L4
import net.sf.nightworks.L5
import net.sf.nightworks.L7

/* WIZnet flags */

enum class Wiznet(val shortName: String, val level: Int) {
    On("on", IM),
    Ticks("ticks", IM),
    Logins("logins", IM),
    Sites("sites", L4),
    Links("links", L7),
    Deaths("deaths", IM),
    Resets("resets", L4),
    MobDeaths("mobdeaths", L4),
    Flags("flags", L5),
    Penalties("penalties", L5),
    Saccing("saccing", L5),
    Levels("levels", IM),
    Secure("secure", L1),
    Switches("switches", L2),
    Snoops("snoops", L2),
    Restore("restore", L2),
    Load("load", L2),
    Newbie("newbies", IM),
    Prefix("prefix", IM),
    Spam("spam", L5);

    companion object {
        fun of(name: String) = values().firstOrNull({ it.shortName == name })
        fun ofList(list: String): Collection<Wiznet> {
            val res = mutableListOf<Wiznet>()
            list.split(",").forEach {
                val w = of(it)
                if (w != null) res.add(w)
            }
            return res
        }
    }
}