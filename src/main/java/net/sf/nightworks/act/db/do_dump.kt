package net.sf.nightworks.act.db

import net.sf.nightworks.Index
import net.sf.nightworks.model.Affect
import java.io.FileWriter
import java.io.IOException
import java.util.Formatter

fun do_dump() {
    try {
        var fp = FileWriter("mem.dmp", false)
        try {
            // report use of data structures

            val f = Formatter(fp)
            // mobile prototypes
            f.format("MobProt %4d (%8d bytes)\n", Index.MOB_INDEX.size, -1)

            // mobs
            var num_pcs = 0
            var aff_count = 0
            for (fch in Index.CHARS) {
                if (fch.pcdata != null) {
                    num_pcs++
                }
                var af: Affect? = fch.affected
                while (af != null) {
                    aff_count++
                    af = af.next
                }
            }
            f.format("Mobs    %4d (%8d bytes), %2d free (%d bytes)\n", Index.CHARS.size, -1, -1, -1)

            /* pcdata */
            f.format("Pcdata  %4d (%8d bytes), %2d free (%d bytes)\n", num_pcs, -1, -1, -1)

            /* descriptors */
            f.format("Descs  %4d (%8d bytes), %2d free (%d bytes)\n", Index.CONNECTIONS.size, -1, -1, -1)

            /* object prototypes */
            for (pObjIndex in Index.OBJ_INDEX.values) {
                var af: Affect? = pObjIndex.affected
                while (af != null) {
                    aff_count++
                    af = af.next
                }
            }

            f.format("ObjProt %4d (%8d bytes)\n", Index.OBJ_INDEX.size, -1)

            /* objects */
            var count = 0

            for (obj in Index.OBJECTS) {
                count++
                var af = obj.affected
                while (af != null) {
                    aff_count++
                    af = af.next
                }
            }
            f.format("Objs    %4d (%8d bytes), %2d free (%d bytes)\n", count, -1, -1, -1)

            /* affects */
            f.format("Affects %4d (%8d bytes), %2d free (%d bytes)\n", aff_count, -1, -1, -1)

            /* rooms */
            f.format("Rooms   %4d (%8d bytes)\n", Index.ROOM_INDEX.size, -1)

        } finally {
            fp.close()
        }

        /* start printing out mobile data */
        fp = FileWriter("mob.dmp", false)
        try {
            val f = Formatter(fp)
            f.out().append("\nMobile Analysis\n")
            f.out().append("---------------\n")
            for (pMobIndex in Index.MOB_INDEX.values) {
                f.format("#%-4d %3d active %3d killed     %s\n", pMobIndex.vnum, pMobIndex.count,
                        pMobIndex.killed, pMobIndex.short_descr)
            }
        } finally {
            fp.close()
        }

        /* start printing out object data */
        fp = FileWriter("obj.dmp", false)
        try {
            val f = Formatter(fp)
            f.out().append("\nObject Analysis\n")
            f.out().append("---------------\n")
            for (pObjIndex in Index.OBJ_INDEX.values) {
                f.format("#%-4d %3d active %3d reset      %s\n", pObjIndex.vnum, pObjIndex.count,
                        pObjIndex.reset_num, pObjIndex.short_descr)
            }
        } finally {
            /* close file */
            fp.close()
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

}
