package net.sf.nightworks.act.info

import net.sf.nightworks.ITEM_TAKE
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.act
import net.sf.nightworks.can_see_obj
import net.sf.nightworks.get_obj_carry
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Wear
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.one_argument

fun do_compare(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    rest = one_argument(rest, arg1b)
    one_argument(rest, arg2b)
    if (arg1b.isEmpty()) {
        send_to_char("Compare what to what?\n", ch)
        return
    }

    val arg1 = arg1b.toString()
    val obj1 = get_obj_carry(ch, arg1)
    if (obj1 == null) {
        send_to_char("You do not have that item.\n", ch)
        return
    }

    var obj2: Obj? = null
    if (arg2b.isEmpty()) {
        for (o in ch.carrying) {
            if (o.wear_loc.isOn()
                    && can_see_obj(ch, o)
                    && obj1.item_type == o.item_type
                    && obj1.wear_flags and o.wear_flags and ITEM_TAKE.inv() != 0L) {
                obj2 = o
                break
            }
        }

        if (obj2 == null) {
            send_to_char("You aren't wearing anything comparable.\n", ch)
            return
        }
    } else {
        obj2 = get_obj_carry(ch, arg2b.toString())
        if (obj2 == null) {
            send_to_char("You do not have that item.\n", ch)
            return
        }
    }

    var msg = ""
    var value1 = 0L
    var value2 = 0L

    when {
        obj1 == obj2 -> msg = "You compare \$p to itself.  It looks about the same."
        obj1.item_type != obj2.item_type -> msg = "You can't compare \$p and \$P."
        else -> when (obj1.item_type) {

            ItemType.Armor -> {
                value1 = obj1.value[0] + obj1.value[1] + obj1.value[2]
                value2 = obj2.value[0] + obj2.value[1] + obj2.value[2]
            }

            ItemType.Weapon -> {
                value1 = if (obj1.pIndexData.new_format) {
                    (1 + obj1.value[2]) * obj1.value[1]
                } else {
                    obj1.value[1] + obj1.value[2]
                }

                value2 = if (obj2.pIndexData.new_format) {
                    (1 + obj2.value[2]) * obj2.value[1]
                } else {
                    obj2.value[1] + obj2.value[2]
                }
            }
            else -> msg = "You can't compare \$p and \$P."
        }
    }

    if (msg.isEmpty()) {
        msg = when {
            value1 == value2 -> "\$p and \$P look about the same."
            value1 > value2 -> "\$p looks better than \$P."
            else -> "\$p looks worse than \$P."
        }
    }

    act(msg, ch, obj1, obj2, TO_CHAR)
}