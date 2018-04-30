package net.sf.nightworks

import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.EXTRA_DESC_DATA
import net.sf.nightworks.model.MOB_INDEX_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.ObjProto
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Religion
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.max
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_WEAPON_STAT
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.chance
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.random
import net.sf.nightworks.util.startsWith
import java.util.Formatter


/* The main quest function */
fun do_quest(ch: CHAR_DATA, argument: String) {
    var rest = argument
    var obj: Obj? = null
    val questinfoobj: ObjProto?
    val questinfo: MOB_INDEX_DATA?
    val bufvampire = StringBuilder()
    val bufsamurai = StringBuilder()
    var trouble_vnum = 0
    var trouble_n: Int
    val sn: Skill

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    val pcdata = ch.pcdata ?: return

    val arg1Str = arg1.toString()
    if (startsWith(arg1Str, "info")) {
        if (IS_SET(ch.act, PLR_QUESTOR)) {
            when {
                pcdata.questmob == -1 -> send_to_char("Your quest is ALMOST complete!\nGet back to Questor before your time runs out!\n", ch)
                pcdata.questobj > 0 -> {
                    questinfoobj = get_obj_index(pcdata.questobj)
                    if (questinfoobj != null) {
                        send_to_char("You are on a quest to recover the fabled " + questinfoobj.name + "!\n", ch)
                    } else {
                        send_to_char("You aren't currently on a quest.\n", ch)
                    }
                    return
                }
                pcdata.questmob > 0 -> {
                    questinfo = get_mob_index(pcdata.questmob)
                    if (questinfo != null) {
                        send_to_char("You are on a quest to slay the dreaded " + questinfo.short_descr + "!\n", ch)
                    } else {
                        send_to_char("You aren't currently on a quest.\n", ch)
                    }
                    return
                }
            }
        } else {
            send_to_char("You aren't currently on a quest.\n", ch)
        }
        return
    }
    if (startsWith(arg1Str, "points")) {
        send_to_char("You have " + pcdata.questpoints + " quest points.\n", ch)
        return
    }
    if (startsWith(arg1Str, "time")) {
        if (!IS_SET(ch.act, PLR_QUESTOR)) {
            send_to_char("You aren't currently on a quest.\n", ch)
            if (pcdata.nextquest > 1) {
                send_to_char("There are " + pcdata.nextquest + " minutes remaining until you can go on another quest.\n", ch)
            } else if (pcdata.nextquest == 1) {
                send_to_char("There is less than a minute remaining until you can go on another quest.\n", ch)
            }
        } else if (pcdata.countdown > 0) {
            send_to_char("Time left for current quest: " + pcdata.countdown + "\n", ch)
        }
        return
    }

    /* Checks for a character in the room with spec_questmaster set. This special
procedure must be defined in special.c. You could instead use an
ACT_QUESTMASTER flag instead of a special procedure. */

    val questman = ch.room.people.firstOrNull { it.pcdata == null && it.spec_fun === spec_lookup("spec_questmaster") }
    if (questman == null || questman.spec_fun !== spec_lookup("spec_questmaster")) {
        send_to_char("You can't do that here.\n", ch)
        return
    }

    if (questman.fighting != null) {
        send_to_char("Wait until the fighting stops.\n", ch)
        return
    }

    pcdata.questgiver = questman.pIndexData.vnum

    /* And, of course, you will need to change the following lines for YOUR
quest item information. Quest items on Moongate are unbalanced, very
very nice items, and no one has one yet, because it takes awhile to
build up quest points :> Make the item worth their while. */

    if (startsWith(arg1Str, "list")) {
        act("\$n asks \$N for a list of quest items.", ch, null, questman, TO_ROOM)
        act("You ask \$N for a list of quest items.", ch, null, questman, TO_CHAR)
        /*
1000qp.........The COMFY CHAIR!!!!!!\n\
850qp..........Sword of Vassago\n\
750qp..........Amulet of Vassago\n\
750qp..........Shield of Vassago\n\
550qp..........Decanter of Endless Water\n\
*/
        if (ch.clazz === Clazz.VAMPIRE) {
            bufvampire.append("    50qp.........Vampire skill (vampire)\n")
        }
        if (ch.clazz === Clazz.SAMURAI) {
            bufsamurai.append("   100qp.........Katana quest (katana)\n").append("   100qp.........Second katana quest(sharp)\n").append("    50qp.........Decrease number of death (death)\n")
        }
        val buf = "Current Quest Items available for Purchase:\n" +
                "5000qp.........the silk-adamantite backpack (backpack)\n" +
                "1000qp.........the Girth of Real Heroism (girth)\n" +
                "1000qp.........the Ring of Real Heroism (ring)\n" +
                "1000qp.........the Real Hero's Weapon (weapon)\n" +
                "1000qp.........100 Practices (practice)\n" +
                " 500qp.........Decanter of Endless Water (decanter)\n" +
                " 500qp.........350,000 gold pieces (gold)\n" +
                " 250qp.........1 constitution (con)\n" +
                " 200qp.........tattoo of your religion (tattoo)\n" +
                bufsamurai + bufvampire +
                "  50qp.........remove tattoo of your religion (remove)\n" +
                "  50qp.........set religion to none (set)\n" +
                "To buy an item, type 'QUEST BUY <item>'.\n"
        send_to_char(buf, ch)
        return
    } else {
        val flag = if (ch.isGood()) "holy" else if (ch.isNeutralA()) "blue-green" else "evil"
        if (startsWith(arg1Str, "buy")) {
            if (arg2.isEmpty()) {
                send_to_char("To buy an item, type 'QUEST BUY <item>'.\n", ch)
                return
            } else if (is_name(arg2.toString(), "backpack")) {
                if (pcdata.questpoints >= 5000) {
                    pcdata.questpoints -= 5000
                    obj = create_object(get_obj_index_nn(OBJ_VNUM_QUEST_ITEM4), ch.level)
                    if (IS_SET(ch.quest, QUEST_BACKPACK) ||
                            IS_SET(ch.quest, QUEST_BACKPACK2) ||
                            IS_SET(ch.quest, QUEST_BACKPACK3)) {
                        do_tell_quest(ch, questman, "This quest item is beyond the trouble option.")
                    } else {
                        ch.quest = SET_BIT(ch.quest, QUEST_BACKPACK)
                    }
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "decanter")) {
                if (pcdata.questpoints >= 500) {
                    pcdata.questpoints -= 500
                    obj = create_object(get_obj_index_nn(OBJ_VNUM_QUEST_ITEM5), ch.level)
                    if (IS_SET(ch.quest, QUEST_DECANTER) ||
                            IS_SET(ch.quest, QUEST_DECANTER2) ||
                            IS_SET(ch.quest, QUEST_DECANTER3)) {
                        do_tell_quest(ch, questman, "This quest item is beyond the trouble option.")
                    } else {
                        ch.quest = SET_BIT(ch.quest, QUEST_DECANTER)
                    }
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "girth")) {
                if (pcdata.questpoints >= 1000) {
                    pcdata.questpoints -= 1000
                    obj = create_object(get_obj_index_nn(OBJ_VNUM_QUEST_ITEM1), ch.level)
                    if (IS_SET(ch.quest, QUEST_GIRTH) ||
                            IS_SET(ch.quest, QUEST_GIRTH2) ||
                            IS_SET(ch.quest, QUEST_GIRTH3)) {
                        do_tell_quest(ch, questman, "This quest item is beyond the trouble option.")
                    } else {
                        ch.quest = SET_BIT(ch.quest, QUEST_GIRTH)
                    }
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "ring")) {
                if (pcdata.questpoints >= 1000) {
                    pcdata.questpoints -= 1000
                    obj = create_object(get_obj_index_nn(OBJ_VNUM_QUEST_ITEM2), ch.level)
                    if (IS_SET(ch.quest, QUEST_RING) ||
                            IS_SET(ch.quest, QUEST_RING2) ||
                            IS_SET(ch.quest, QUEST_RING3)) {
                        do_tell_quest(ch, questman, "This quest item is beyond the trouble option.")
                    } else {
                        ch.quest = SET_BIT(ch.quest, QUEST_RING)
                    }
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "weapon")) {
                if (pcdata.questpoints >= 1000) {
                    pcdata.questpoints -= 1000
                    obj = create_object(get_obj_index_nn(OBJ_VNUM_QUEST_ITEM3), ch.level)
                    if (IS_SET(ch.quest, QUEST_WEAPON) ||
                            IS_SET(ch.quest, QUEST_WEAPON2) ||
                            IS_SET(ch.quest, QUEST_WEAPON3)) {
                        do_tell_quest(ch, questman, "This quest item is beyond the trouble option.")
                    } else {
                        ch.quest = SET_BIT(ch.quest, QUEST_WEAPON)
                    }
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "practices pracs prac practice")) {
                if (IS_SET(ch.quest, QUEST_PRACTICE)) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you had already got enough practices!")
                    return
                }

                if (pcdata.questpoints >= 1000) {
                    pcdata.questpoints -= 1000
                    ch.practice += 100
                    act("\$N gives 100 practices to \$n.", ch, null, questman, TO_ROOM)
                    act("\$N gives you 100 practices.", ch, null, questman, TO_CHAR)
                    ch.quest = SET_BIT(ch.quest, QUEST_PRACTICE)
                    return
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "vampire")) {
                if (ch.clazz !== Clazz.VAMPIRE) {
                    do_tell_quest(ch, questman, "You cannot gain this skill, " + ch.name + ".")
                    return
                }
                if (pcdata.questpoints >= 50) {
                    pcdata.questpoints -= 50
                    sn = Skill.vampire
                    pcdata.learned[sn.ordinal] = 100
                    act("\$N gives secret of undead to \$n.", ch, null, questman, TO_ROOM)
                    act("\$N gives you SECRET of undead.", ch, null, questman, TO_CHAR)
                    act("{bLightning flashes in the sky.{x", ch, null, questman, TO_ALL, Pos.Sleeping)
                    return
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "con constitution")) {
                if (ch.perm_stat[PrimeStat.Constitution] >= ch.max(PrimeStat.Constitution)) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", you have already sufficient constitution.")
                    return
                }

                if (pcdata.questpoints >= 250) {
                    pcdata.questpoints -= 250
                    ch.perm_stat[PrimeStat.Constitution] += 1
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "dead samurai death")) {
                if (ch.clazz !== Clazz.SAMURAI) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you are not a samurai.")
                    return
                }

                if (pcdata.death < 1) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", you haven't god any death yet.")
                    return
                }

                if (pcdata.questpoints >= 50) {
                    pcdata.questpoints -= 50
                    pcdata.death -= 1
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "katana sword")) {
                if (ch.clazz !== Clazz.SAMURAI) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you are not a samurai.")
                    return
                }

                val katana = get_obj_list(ch, "katana", ch.carrying)
                if (katana == null) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have your katana with you.")
                    return
                }

                if (IS_WEAPON_STAT(katana, WEAPON_KATANA)) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but your katana has already passed the first quest.")
                    return
                }

                if (pcdata.questpoints >= 100) {
                    pcdata.questpoints -= 100
                    val af = Affect()
                    af.where = TO_WEAPON
                    af.type = Skill.reserved
                    af.level = 100
                    af.duration = -1
                    af.bitvector = WEAPON_KATANA
                    affect_to_obj(katana, af)
                    do_tell_quest(ch, questman, "As you wield it, you will feel that it is power will increase, continuously.")
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "sharp second")) {
                if (ch.clazz !== Clazz.SAMURAI) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you are not a samurai.")
                    return
                }
                val katana = get_obj_list(ch, "katana", ch.carrying)
                if (katana == null) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have your katana with you.")
                    return
                }

                if (!IS_WEAPON_STAT(katana, WEAPON_KATANA)) {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but your katana hasn't passed the first quest.")
                    return
                }

                if (pcdata.questpoints >= 100) {
                    pcdata.questpoints -= 100
                    val af = Affect()
                    af.where = TO_WEAPON
                    af.type = Skill.reserved
                    af.level = 100
                    af.duration = -1
                    af.bitvector = WEAPON_SHARP
                    affect_to_obj(katana, af)
                    do_tell_quest(ch, questman, "From now on, your katana will be as sharp as blades of titans.")
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "tattoo religion")) {
                var tattoo: Obj?
                if (ch.religion.isNone()) {
                    send_to_char("You don't have a religion to have a tattoo.\n", ch)
                    return
                }
                tattoo = get_eq_char(ch, Wear.Tattoo)
                if (tattoo != null) {
                    send_to_char("But you have already your tattoo!.\n", ch)
                    return
                }

                if (pcdata.questpoints >= 200) {
                    pcdata.questpoints -= 200

                    tattoo = create_object(get_obj_index_nn(ch.religion.tattooVnum), 100)

                    obj_to_char(tattoo, ch)
                    equip_char(ch, tattoo, Wear.Tattoo)
                    act("\$N tattoos \$n with \$p!.", ch, tattoo, questman, TO_ROOM)
                    act("\$N tattoos you with \$p!.", ch, tattoo, questman, TO_CHAR)
                    return
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "gold gp")) {
                if (pcdata.questpoints >= 500) {
                    pcdata.questpoints -= 500
                    ch.gold += 350000
                    act("\$N gives 350,000 gold pieces to \$n.", ch, null, questman, TO_ROOM)
                    act("\$N has 350,000 in gold transfered from \$s Swiss account to your balance.", ch, null, questman, TO_CHAR)
                    return
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "remove")) {
                val tattoo: Obj?

                if (pcdata.questpoints >= 50) {
                    tattoo = get_eq_char(ch, Wear.Tattoo)
                    if (tattoo == null) {
                        do_tell_quest(ch, questman, "But you don't have any tattoo!")
                        return
                    }

                    pcdata.questpoints -= 50
                    extract_obj(tattoo)
                    act("Through a painful process, your tattoo has been destroyed by \$n.", questman, null, ch, TO_VICT)
                    act("\$N's tattoo is destroyed by \$n.", questman, null, ch, TO_NOTVICT)
                    return
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else if (is_name(arg2.toString(), "set")) {
                if (pcdata.questpoints >= 50) {
                    if (get_eq_char(ch, Wear.Tattoo) != null) {
                        do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you have to remove your tattoo first!")
                        return
                    }
                    if (ch.religion.isNone()) {
                        do_tell_quest(ch, questman, "But you are already an atheist!")
                        return
                    }
                    pcdata.questpoints -= 50
                    ch.religion = Religion.None
                    send_to_char("You don't believe any religion now.\n", ch)
                    act("\$N's religion is set to NONE!.", questman, null, ch, TO_NOTVICT)
                } else {
                    do_tell_quest(ch, questman, "Sorry, " + ch.name + ", but you don't have enough quest points for that.")
                    return
                }
            } else {
                do_tell_quest(ch, questman, "I don't have that item, " + ch.name + ".")
            }
            if (obj != null) {
                if (obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM4 || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM5) {
                    val f = Formatter()
                    f.format(obj.pIndexData.extra_descr!!.description, ch.name)
                    obj.extra_desc = EXTRA_DESC_DATA()
                    obj.extra_desc.keyword = obj.pIndexData.extra_descr!!.keyword
                    obj.extra_desc.description = f.toString()
                    obj.extra_desc.next = null
                }
                if (obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM1
                        || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM2
                        || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM3) {
                    val f = Formatter()
                    f.format(obj.short_desc, flag, ch.name)
                    obj.short_desc = f.toString()
                }
                act("\$N gives \$p to \$n.", ch, obj, questman, TO_ROOM)
                act("\$N gives you \$p.", ch, obj, questman, TO_CHAR)
                obj_to_char(obj, ch)
            }
            return
        } else if (startsWith(arg1Str, "request")) {
            act("\$n asks \$N for a quest.", ch, null, questman, TO_ROOM)
            act("You ask \$N for a quest.", ch, null, questman, TO_CHAR)
            if (IS_SET(ch.act, PLR_QUESTOR)) {
                do_tell_quest(ch, questman, "But you're already on a quest!")
                return
            }
            if (pcdata.nextquest > 0) {
                do_tell_quest(ch, questman, "You're very brave, " + ch.name + ", but let someone else have a chance.")
                do_tell_quest(ch, questman, "Come back later.")
                return
            }

            do_tell_quest(ch, questman, "Thank you, brave " + ch.name + "!")

            generate_quest(ch, questman)

            if (pcdata.questmob > 0 || pcdata.questobj > 0) {
                pcdata.countdown = number_range(15, 30)
                ch.act = SET_BIT(ch.act, PLR_QUESTOR)
                do_tell_quest(ch, questman, "You have {c" + pcdata.countdown + "{x minutes to complete this quest.")
                do_tell_quest(ch, questman, "May the gods go with you!")
            }
            return
        } else if (startsWith(arg1Str, "complete")) {
            act("\$n informs \$N \$e has completed \$s quest.", ch, null, questman, TO_ROOM)
            act("You inform \$N you have completed \$s quest.", ch, null, questman, TO_CHAR)
            if (pcdata.questgiver != questman.pIndexData.vnum) {
                do_tell_quest(ch, questman, "I never sent you on a quest! Perhaps you're thinking of someone else.")
                return
            }

            if (IS_SET(ch.act, PLR_QUESTOR)) {
                if (pcdata.questmob == -1 && pcdata.countdown > 0) {
                    val reward = Math.max(180, 100 + dice(ch.level, 20))
                    val pointreward = number_range(20, 40)

                    do_tell_quest(ch, questman, "Congratulations on completing your quest!")
                    do_tell_quest(ch, questman, "As a reward, I am giving you {w$pointreward{x quest points, and {Y$reward{x gold.")
                    if (chance(2)) {
                        val pracreward = number_range(1, 6)
                        send_to_char("You gain $pracreward practices!\n", ch)
                        ch.practice += pracreward
                    }

                    ch.act = REMOVE_BIT(ch.act, PLR_QUESTOR)
                    pcdata.questgiver = 0
                    pcdata.countdown = 0
                    pcdata.questmob = 0
                    pcdata.questobj = 0
                    pcdata.nextquest = 5
                    ch.gold += reward
                    pcdata.questpoints += pointreward

                    return
                } else if (pcdata.questobj > 0 && pcdata.countdown > 0) {
                    val obj_found = ch.carrying.any { it.pIndexData.vnum == pcdata.questobj && it.extra_desc.description.contains(ch.name) }

                    if (obj_found) {
                        val reward = 200 + number_range(1, 20 * ch.level)
                        val pointreward = number_range(15, 40)

                        act("You hand \$p to \$N.", ch, obj!!, questman, TO_CHAR)
                        act("\$n hands \$p to \$N.", ch, obj, questman, TO_ROOM)

                        do_tell_quest(ch, questman, "Congratulations on completing your quest!")
                        do_tell_quest(ch, questman, "As a reward, I am giving you $pointreward quest points, and $reward gold.")
                        if (chance(15)) {
                            val pracreward = number_range(1, 6)
                            send_to_char("You gain $pracreward practices!\n", ch)
                            ch.practice += pracreward
                        }

                        ch.act = REMOVE_BIT(ch.act, PLR_QUESTOR)
                        pcdata.questgiver = 0
                        pcdata.countdown = 0
                        pcdata.questmob = 0
                        pcdata.questobj = 0
                        pcdata.nextquest = 5
                        ch.gold += reward
                        pcdata.questpoints += pointreward
                        extract_obj(obj)
                    } else {
                        do_tell_quest(ch, questman, "You haven't completed the quest yet, but there is still time!")
                    }
                    return
                } else if ((pcdata.questmob > 0 || pcdata.questobj > 0) && pcdata.countdown > 0) {
                    do_tell_quest(ch, questman, "You haven't completed the quest yet, but there is still time!")
                    return
                }
            }
            if (pcdata.nextquest > 0) {
                do_tell_quest(ch, questman, "But you didn't complete your quest in time!")
            } else {
                do_tell_quest(ch, questman, "You have to REQUEST a quest first, " + ch.name + ".")
            }
            return
        } else if (startsWith(arg1Str, "trouble")) {
            if (arg2.isEmpty()) {
                send_to_char("To correct a quest award's trouble, type: quest trouble <award>'.\n", ch)
                return
            }

            trouble_n = 0
            when {
                is_name(arg2.toString(), "girth") -> {
                    when {
                        IS_SET(ch.quest, QUEST_GIRTH) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_GIRTH)
                            ch.quest = SET_BIT(ch.quest, QUEST_GIRTH2)
                            trouble_n = 1
                        }
                        IS_SET(ch.quest, QUEST_GIRTH2) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_GIRTH2)
                            ch.quest = SET_BIT(ch.quest, QUEST_GIRTH3)
                            trouble_n = 2
                        }
                        IS_SET(ch.quest, QUEST_GIRTH3) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_GIRTH3)
                            trouble_n = 3
                        }
                    }
                    if (trouble_n != 0) {
                        trouble_vnum = OBJ_VNUM_QUEST_ITEM1
                    }
                }
                is_name(arg2.toString(), "backpack") -> {
                    when {
                        IS_SET(ch.quest, QUEST_BACKPACK) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_BACKPACK)
                            ch.quest = SET_BIT(ch.quest, QUEST_BACKPACK2)
                            trouble_n = 1
                        }
                        IS_SET(ch.quest, QUEST_BACKPACK2) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_BACKPACK2)
                            ch.quest = SET_BIT(ch.quest, QUEST_BACKPACK3)
                            trouble_n = 2
                        }
                        IS_SET(ch.quest, QUEST_BACKPACK3) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_BACKPACK3)
                            trouble_n = 3
                        }
                    }
                    if (trouble_n != 0) {
                        trouble_vnum = OBJ_VNUM_QUEST_ITEM4
                    }
                }
                is_name(arg2.toString(), "decanter") -> {
                    when {
                        IS_SET(ch.quest, QUEST_DECANTER) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_DECANTER)
                            ch.quest = SET_BIT(ch.quest, QUEST_DECANTER2)
                            trouble_n = 1
                        }
                        IS_SET(ch.quest, QUEST_DECANTER2) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_DECANTER2)
                            ch.quest = SET_BIT(ch.quest, QUEST_DECANTER3)
                            trouble_n = 2
                        }
                        IS_SET(ch.quest, QUEST_DECANTER3) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_DECANTER3)
                            trouble_n = 3
                        }
                    }
                    if (trouble_n != 0) {
                        trouble_vnum = OBJ_VNUM_QUEST_ITEM5
                    }
                }
                is_name(arg2.toString(), "weapon") -> {
                    when {
                        IS_SET(ch.quest, QUEST_WEAPON) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_WEAPON)
                            ch.quest = SET_BIT(ch.quest, QUEST_WEAPON2)
                            trouble_n = 1
                        }
                        IS_SET(ch.quest, QUEST_WEAPON2) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_WEAPON2)
                            ch.quest = SET_BIT(ch.quest, QUEST_WEAPON3)
                            trouble_n = 2
                        }
                        IS_SET(ch.quest, QUEST_WEAPON3) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_WEAPON3)
                            trouble_n = 3
                        }
                    }
                    if (trouble_n != 0) {
                        trouble_vnum = OBJ_VNUM_QUEST_ITEM3
                    }
                }
                is_name(arg2.toString(), "ring") -> {
                    when {
                        IS_SET(ch.quest, QUEST_RING) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_RING)
                            ch.quest = SET_BIT(ch.quest, QUEST_RING2)
                            trouble_n = 1
                        }
                        IS_SET(ch.quest, QUEST_RING2) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_RING2)
                            ch.quest = SET_BIT(ch.quest, QUEST_RING3)
                            trouble_n = 2
                        }
                        IS_SET(ch.quest, QUEST_RING3) -> {
                            ch.quest = REMOVE_BIT(ch.quest, QUEST_RING3)
                            trouble_n = 3
                        }
                    }
                    if (trouble_n != 0) {
                        trouble_vnum = OBJ_VNUM_QUEST_ITEM2
                    }
                }
            }
            if (trouble_n != 0) {
                do_tell_quest(ch, questman, "Sorry " + ch.name + ", but you haven't bought that quest award, yet.\n")
                return
            }

            for (o in Index.OBJECTS) {
                if (o.pIndexData.vnum == trouble_vnum && o.short_desc.contains(ch.name)) {
                    extract_obj(o)
                    break
                }
            }
            obj = create_object(get_obj_index_nn(trouble_vnum), ch.level)
            if (obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM4 || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM5) {
                val f = Formatter()
                f.format(obj.pIndexData.extra_descr!!.description, ch.name)
                obj.extra_desc = EXTRA_DESC_DATA()
                obj.extra_desc.keyword = obj.pIndexData.extra_descr!!.keyword
                obj.extra_desc.description = f.toString()
                obj.extra_desc.next = null
            }
            if (obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM1 || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM2 || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM3) {
                val f = Formatter()
                f.format(obj.short_desc, flag, ch.name)
                obj.short_desc = f.toString()
            }
            act("\$N gives \$p to \$n.", ch, obj, questman, TO_ROOM)
            act("\$N gives you \$p.", ch, obj, questman, TO_CHAR)
            obj_to_char(obj, ch)
            do_tell_quest(ch, questman, "This is the " + trouble_n + "" + (if (trouble_n == 1) "st" else if (trouble_n == 2) "nd" else "rd") + " time that i am giving that award back.")
            if (trouble_n == 3) {
                do_tell_quest(ch, questman, "And I won't give you that again, with trouble option.\n")
            }
            return
        }
    }

    send_to_char("QUEST COMMANDS: points info time request complete list buy trouble.\n", ch)
    send_to_char("For more information, type: help quests.\n", ch)
}

fun generate_quest(ch: CHAR_DATA, questman: CHAR_DATA) {
    val pcdata = ch.pcdata ?: return

    val victim: CHAR_DATA?
    var vsearch: MOB_INDEX_DATA? = null
    val room: ROOM_INDEX_DATA?
    val eyed: Obj
    var level_diff: Int
    var found: Int

    //room	=	new ROOM_INDEX_DATA();
    val mob_buf = IntArray(300)

    var mob_count = 0
    for (m in Index.MOB_INDEX.values) {
        vsearch = m
        level_diff = vsearch.level - ch.level
        if (ch.level < 51 && (level_diff > 4 || level_diff < -1)
                || ch.level > 50 && (level_diff > 6 || level_diff < 0)
                || vsearch.pShop != null
                || IS_SET(vsearch.act, ACT_TRAIN)
                || IS_SET(vsearch.act, ACT_PRACTICE)
                || IS_SET(vsearch.act, ACT_IS_HEALER)
                || IS_SET(vsearch.act, ACT_NOTRACK)
                || IS_SET(vsearch.imm_flags, IMM_SUMMON)) {
            continue
        }
        mob_buf[mob_count] = vsearch.vnum
        mob_count++
        if (mob_count > 299) {
            break
        }
    }

    if (chance(40)) {
        if (mob_count > 0) {
            found = number_range(0, mob_count - 1)
            for (i in 0 until mob_count) {
                vsearch = get_mob_index(mob_buf[found])
                if (vsearch == null) {
                    bug("Unknown mob in generate_obj_quest: %d", mob_buf[found])
                    found++
                    if (found > mob_count - 1) {
                        break
                    }
                } else {
                    break
                }
            }
        } else {
            vsearch = null
        }

        victim = find(vsearch)
        if (victim == null) {
            do_tell_quest(ch, questman, "I'm sorry, but I don't have any quests for you at this time.")
            do_tell_quest(ch, questman, "Try again later.")
            pcdata.nextquest = 5
            return
        }

        room = victim.room

        val objvnum = when (random(4)) {
            0 -> OBJ_VNUM_QUEST_ITEM1
            1 -> OBJ_VNUM_QUEST_ITEM2
            2 -> OBJ_VNUM_QUEST_ITEM3
            else -> OBJ_VNUM_QUEST_ITEM4
        }


        val align = when {
            ch.isGood() -> 0
            ch.isEvil() -> 2
            else -> 1
        }

        eyed = create_object(get_obj_index_nn(objvnum), ch.level)
        eyed.owner = ch.name
        eyed.from = ch.name
        eyed.altar = ch.hometown.altar[align]
        eyed.pit = ch.hometown.pit[align]
        eyed.level = ch.level

        val f1 = Formatter()
        f1.format(eyed.description, ch.name)
        eyed.description = f1.toString()

        val f2 = Formatter()
        f2.format(eyed.pIndexData.extra_descr!!.description, ch.name)
        eyed.extra_desc = EXTRA_DESC_DATA()
        eyed.extra_desc.keyword = eyed.pIndexData.extra_descr!!.keyword
        eyed.extra_desc.description = f2.toString()
        eyed.extra_desc.next = null

        eyed.cost = 0
        eyed.timer = 30

        obj_to_room(eyed, room)
        pcdata.questobj = eyed.pIndexData.vnum

        do_tell_quest(ch, questman, "Vile pilferers have stolen {w" + eyed.short_desc + "{x from the royal treasury!")
        do_tell_quest(ch, questman, "My court wizardess, with her magic mirror, has pinpointed its location.")

        /* I changed my area names so that they have just the name of the area
     and none of the level stuff. You may want to comment these next two
     lines. - Vassago */

        do_tell_quest(ch, questman, "Look in the general area of {w" + room.area.name + "{x for " + room.name + "!")
    } else {         /* Quest to kill a mob */
        if (mob_count > 0) {
            found = number_range(0, mob_count - 1)
            for (i in 0 until mob_count) {
                vsearch = get_mob_index(mob_buf[found])
                if (vsearch == null || vsearch.align == ch.align) {
                    if (vsearch == null) {
                        bug("Unknown mob in mob_quest: %d", mob_buf[found])
                    }
                    found++
                    if (found > mob_count - 1) {
                        vsearch = null
                        break
                    }
                } else {
                    break
                }
            }
        } else {
            vsearch = null
        }

        victim = find(vsearch)
        room = victim?.room
        if (victim == null || room == null || IS_SET(room.area.area_flag, AREA_HOMETOWN)) {
            do_tell_quest(ch, questman, "I'm sorry, but I don't have any quests for you at this time.")
            do_tell_quest(ch, questman, "Try again later.")
            pcdata.nextquest = 5
            return
        }

        if (ch.isGood()) {
            do_tell_quest(ch, questman, "Rune's most heinous criminal, {w" + victim.short_desc + "{x,	has escaped from the dungeon!")
            do_tell_quest(ch, questman, "Since the escape, " + victim.short_desc + " has murdered " + number_range(2, 20) + " civillians!")
            do_tell_quest(ch, questman, "The penalty for this crime is death, and you are to deliver the sentence!")
        } else {
            do_tell_quest(ch, questman, "An enemy of mine, {x" + victim.short_desc + "{x, is making vile threats against the crown.")
            do_tell_quest(ch, questman, "This threat must be eliminated!")
        }

        do_tell_quest(ch, questman, "Seek " + victim.short_desc + " out in vicinity of {w" + room.name + "{x!")

        /* I changed my area names so that they have just the name of the area and none of the level stuff.
        You may want to comment these next two lines. - Vassago */

        do_tell_quest(ch, questman, "That location is in the general area of " + room.area.name + ".")
        pcdata.questmob = victim.pIndexData.vnum
    }
}

/* Called from update_handler() by pulse_area */

fun quest_update() {
    Index.CHARS.forEach { ch ->
        val pcdata = ch.pcdata ?: return
        if (pcdata.nextquest > 0) {
            pcdata.nextquest--
            if (pcdata.nextquest == 0) {
                send_to_char("You may now quest again.\n", ch)
            }
            return
        }
        if (!IS_SET(ch.act, PLR_QUESTOR)) {
            return
        }
        if (--pcdata.countdown <= 0) {
            pcdata.nextquest = 0
            send_to_char("You have run out of time for your quest!\nYou may now quest again.\n", ch)
            ch.act = REMOVE_BIT(ch.act, PLR_QUESTOR)
            pcdata.questgiver = 0
            pcdata.countdown = 0
            pcdata.questmob = 0
            pcdata.questobj = 0
        }
        if (pcdata.countdown in 1..5) {
            send_to_char("Better hurry, you're almost out of time for your quest!\n", ch)
        }
    }
}

private fun do_tell_quest(ch: CHAR_DATA, victim: CHAR_DATA, argument: String) {
    send_to_char(victim.name + " tells you " + argument + "\n", ch)
}

private fun find(indexData: MOB_INDEX_DATA?) = if (indexData == null) null else Index.CHARS.firstOrNull { it.pIndexData == indexData }

