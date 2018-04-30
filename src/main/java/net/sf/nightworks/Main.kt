package net.sf.nightworks

import net.sf.nightworks.util.currentTimeSeconds
import net.sf.nightworks.util.log_string
import java.io.IOException

object Main {

    @JvmStatic fun main(args: Array<String>) {
        // Init time.
        current_time = currentTimeSeconds()
        limit_time = current_time
        boot_time = current_time

        // Run the game.
        try {
            initServer(nw_config.port_num)
            try {
                boot_db()
                log_string("Folklore runs on port " + nw_config.port_num + ".")
                nightworks_engine()
            } finally {
                Server.channel.close()
            }
        } catch (e: IOException) {
            System.exit(-1)
        }

        // That's all, folks.
        log_area_popularity()
        log_string("Normal termination of game.")
        System.exit(Server.nw_exit)
    }
}
