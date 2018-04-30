package net.sf.nightworks.act.comm

import net.sf.nightworks.do_quit
import net.sf.nightworks.get_trust
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.nw_config
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.wiznet
import java.io.File

fun do_delete(ch: CHAR_DATA, argument: String) {
    val pcdata = ch.pcdata ?: return

    if (pcdata.confirm_delete) {
        if (argument.isNotEmpty()) {
            send_to_char("Delete status removed.\n", ch)
            pcdata.confirm_delete = false
        } else {
            wiznet("\$N turns \$Mself into line noise.", ch, null, null, null, 0)
            ch.last_fight_time = -1
            do_quit(ch)
            File(nw_config.lib_player_dir + "/" + capitalize(ch.name)).delete()
        }
        return
    }

    if (argument.isNotEmpty()) {
        send_to_char("Just type delete. No argument.\n", ch)
        return
    }

    send_to_char("Type delete again to confirm this command.\n", ch)
    send_to_char("WARNING: this command is irreversible.\n", ch)
    send_to_char("Typing delete with an argument will undo delete status.\n", ch)
    pcdata.confirm_delete = true
    wiznet("\$N is contemplating deletion.", ch, null, null, null, get_trust(ch))
}
