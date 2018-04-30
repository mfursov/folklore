package net.sf.nightworks

import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.AttackType
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Connection
import net.sf.nightworks.model.EXTRA_DESC_DATA
import net.sf.nightworks.model.Ethos
import net.sf.nightworks.model.HomeTown
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.PC_DATA
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Religion
import net.sf.nightworks.model.Sex
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.Wiznet
import net.sf.nightworks.util.DikuTextFile
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_QUESTOR
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.sprintf
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Arrays
import java.util.Formatter

fun print_flags(flag: Long): String {
    val buf = StringBuilder()
    (0 until 64)
            .filter { IS_SET(flag, (1 shl it).toLong()) }
            .forEach {
                when {
                    it < 26 -> buf.append('A' + it)
                    else -> buf.append(('a' + (it - 26)))
                }
            }
    return buf.toString()
}

/* Array of containers read for proper re-nesting of objects. */
private val MAX_NEST = 100
private val rgObjNest = arrayOfNulls<Obj>(MAX_NEST)

/**
 * Save a character and inventory.
 * Would be cool to save NPC's too for quest purposes,
 *   some of the infrastructure is provided.
 */
fun save_char_obj(ch: CHAR_DATA) {
    val pcData = ch.pcdata!!
    if (ch.level < 2 && !IS_SET(ch.act, PLR_REMORTED)) {
        return
    }
    /* create god log */
    if (IS_IMMORTAL(ch) || ch.level >= LEVEL_IMMORTAL) {
        val strsave = nw_config.lib_god_dir + "/" + capitalize(ch.name)
        try {
            FileWriter(strsave).use { f ->
                val fp = Formatter(f)
                fp.format("Lev %2d Trust %2d  %s%s\n", ch.level, get_trust(ch), ch.name, pcData.title)
            }
        } catch (e: IOException) {
            bug("Save_char_obj: fopen")
            log_string(strsave)
        }
    }

    val fp = TextBuffer()
    fwrite_char(ch, fp)
    if (ch.carrying.isNotEmpty()) {
        fwrite_obj(ch, ch.carrying.first(), fp, 0)
    }
    /* save the pets */
    val pet = ch.pet
    if (pet != null && pet.room == ch.room) {
        fwrite_pet(pet, fp)
    }
    fp.append("#END\n")

    val fileName = nw_config.lib_player_dir + "/" + capitalize(ch.name)
    try {
        FileWriter(fileName).use { f -> f.write(fp.toString()) }
    } catch (e: IOException) {
        bug("Save_char_obj: fopen")
        log_string(fileName)
    }

    File(nw_config.pl_temp_file).renameTo(File(fileName))
}

/** Write the char.*/
private fun fwrite_char(ch: CHAR_DATA, fp: TextBuffer) {
    val pcData = ch.pcdata!!
    fp.sprintf(false, "#%s\n", if (ch.pcdata == null) "MOB" else "PLAYER")
    fp.sprintf(false, "Name %s~\n", ch.name)
    fp.sprintf(false, "Id   %d\n", ch.id)
    fp.sprintf(false, "LogO %d\n", current_time)
    fp.sprintf(false, "Vers %d\n", 7)
    fp.sprintf(false, "Etho %d\n", ch.ethos)
    fp.sprintf(false, "Home %d\n", ch.hometown.id)
    fp.sprintf(false, "Cab  %d\n", ch.cabal)
    fp.sprintf(false, "Dead %d\n", pcData)
    fp.sprintf(false, "Ques %s\n", print_flags(ch.quest))

    if (ch.short_desc.isNotEmpty()) {
        fp.sprintf(false, "ShD  %s~\n", ch.short_desc)
    }
    if (ch.long_desc.isNotEmpty()) {
        fp.sprintf(false, "LnD  %s~\n", ch.long_desc)
    }
    if (ch.description.isNotEmpty()) {
        fp.sprintf(false, "Desc %s~\n", ch.description)
    }
    if (!ch.prompt.isEmpty()) {
        fp.sprintf(false, "Prom %s~\n", ch.prompt)
    }
    fp.sprintf(false, "Race %s~\n", ch.race.name)
    fp.sprintf(false, "Sex  %d\n", ch.sex)
    fp.sprintf(false, "Cla  %s\n", ch.clazz.name)
    fp.sprintf(false, "Levl %d\n", ch.level)
    if (ch.trust != 0) {
        fp.sprintf(false, "Tru  %d\n", ch.trust)
    }
    fp.sprintf(false, "Plyd %d\n", pcData.played + (current_time - ch.logon).toInt())
    fp.sprintf(false, "Not  %d %d %d %d %d\n",
            pcData.last_note, pcData.last_idea, pcData.last_penalty,
            pcData.last_news, pcData.last_changes)
    fp.sprintf(false, "Scro %d\n", ch.lines)
    val was_in_room = ch.was_in_room
    fp.sprintf(false, "Room %d\n",
            when {
                ch.room == get_room_index(ROOM_VNUM_LIMBO) && was_in_room != null -> was_in_room.vnum
                else -> ch.room.vnum
            })

    fp.sprintf(false, "HMV  %d %d %d\n", ch.hit, ch.mana, ch.move)
    fp.sprintf(false, "Gold %d\n", Math.max(0, ch.gold))
    fp.sprintf(false, "Silv %d\n", Math.max(0, ch.silver))
    fp.sprintf(false, "Banks %d\n", pcData.bank_s)
    fp.sprintf(false, "Bankg %d\n", pcData.bank_g)
    fp.sprintf(false, "Exp  %d\n", ch.exp)
    if (ch.act != 0L) {
        fp.sprintf(false, "Act  %s\n", print_flags(ch.act))
    }
    /*
if (ch.affected_by != 0)
{
 if (IS_NPC(ch))
 fprintf( fp, "AfBy %s\n", print_flags(ch.affected_by) );
 else
 fprintf( fp, "AfBy %s\n",
    print_flags((ch.affected_by & (~AFF_CHARM))) );
}
if (ch.detection != 0)
fprintf( fp, "Detect %s\n",   print_flags(ch.detection));
*/
    fp.sprintf(false, "Comm %s\n", print_flags(ch.comm))
    if (ch.wiznet.isNotEmpty()) {
        fp.sprintf(false, "Wizn %s\n", ch.wiznet.joinToString { it.shortName })
    }
    if (ch.invis_level != 0) {
        fp.sprintf(false, "Invi %d\n", ch.invis_level)
    }
    if (ch.incog_level != 0) {
        fp.sprintf(false, "Inco %d\n", ch.incog_level)
    }
    fp.sprintf(false, "Pos  %d\n", if (ch.position == Pos.Fighting) Pos.Standing else ch.position)
    if (ch.practice != 0) {
        fp.sprintf(false, "Prac %d\n", ch.practice)
    }
    if (ch.train != 0) {
        fp.sprintf(false, "Trai %d\n", ch.train)
    }
    fp.sprintf(false, "Alig  %d\n", ch.alignment)
    /*
if (ch.saving_throw != 0)
fprintf( fp, "Save  %d\n",  ch.saving_throw);
if (ch.hitroll != 0)
fprintf( fp, "Hit   %d\n",  ch.hitroll );
if (ch.damroll != 0)
fprintf( fp, "Dam   %d\n",  ch.damroll );
fprintf( fp, "ACs %d %d %d %d\n",
ch.armor[0],ch.armor[1],ch.armor[2],ch.armor[3]);
*/
    if (ch.wimpy != 0) {
        fp.sprintf(false, "Wimp  %d\n", ch.wimpy)
    }

    fp.sprintf(false, "Attr %d %d %d %d %d %d\n",
            ch.perm_stat[PrimeStat.Strength],
            ch.perm_stat[PrimeStat.Intelligence],
            ch.perm_stat[PrimeStat.Wisdom],
            ch.perm_stat[PrimeStat.Dexterity],
            ch.perm_stat[PrimeStat.Constitution],
            ch.perm_stat[PrimeStat.Charisma])

    /*
fprintf (fp, "AMod %d %d %d %d %d %d\n",
ch.mod_stat[PrimeStat.Strength],
ch.mod_stat[PrimeStat.Intelligence],
ch.mod_stat[PrimeStat.Wisdom],
ch.mod_stat[PrimeStat.Dexterity],
ch.mod_stat[PrimeStat.Constitution],
ch.mod_stat[PrimeStat.Charisma] );
*/

    fp.sprintf(false, "Pass %s~\n", pcData.pwd)
    if (pcData.bamfin.isNotEmpty()) {
        fp.sprintf(false, "Bin  %s~\n", pcData.bamfin)
    }
    if (pcData.bamfout.isNotEmpty()) {
        fp.sprintf(false, "Bout %s~\n", pcData.bamfout)
    }
    fp.sprintf(false, "Titl %s~\n", pcData.title)
    fp.sprintf(false, "Pnts %d\n", pcData.points)
    fp.sprintf(false, "TSex %d\n", pcData.true_sex)
    fp.sprintf(false, "LLev %d\n", pcData.last_level)
    fp.sprintf(false, "HMVP %d %d %d\n", pcData.perm_hit, pcData.perm_mana, pcData.perm_move)
    fp.sprintf(false, "CndC  %d %d %d %d %d %d\n",
            pcData.condition[0],
            pcData.condition[1],
            pcData.condition[2],
            pcData.condition[3],
            pcData.condition[4],
            pcData.condition[5])

    /* write alias */
    var pos = 0
    while (pos < nw_config.max_alias) {
        if (pcData.alias[pos] == null || pcData.alias_sub[pos] == null) {
            break
        }
        fp.sprintf(false, "Alias %s %s~\n", pcData.alias[pos], pcData.alias_sub[pos])
        pos++
    }

    Skill.skills
            .filter { pcData.learned[it.ordinal] > 0 }
            .forEach { fp.sprintf(false, "Sk %d '%s'\n", pcData.learned[it.ordinal], it.skillName) }

    var paf = ch.affected
    while (paf != null) {
        if (paf.type == Skill.doppelganger) {
            paf = paf.next
            continue
        }

        if (ch.pcdata != null && (paf.bitvector == AFF_CHARM || paf.duration < -1)) {
            paf = paf.next
            continue
        }

        fp.sprintf(false, "Affc '%s' %3d %3d %3d %3d %3d %10d\n",
                paf.type.skillName,
                paf.where,
                paf.level,
                paf.duration,
                paf.modifier,
                paf.location,
                paf.bitvector
        )
        paf = paf.next
    }

    /* quest done by chronos */

    if (pcData.questpoints != 0) {
        fp.sprintf(false, "QuestPnts %d\n", pcData.questpoints)
    }
    if (pcData.nextquest != 0) {
        fp.sprintf(false, "QuestNext %d\n", pcData.nextquest)
    }
    if (IS_QUESTOR(ch)) {
        fp.sprintf(false, "QuestCnt %d\n", pcData.countdown)
        fp.sprintf(false, "QuestMob %d\n", pcData.questmob)
        fp.sprintf(false, "QuestObj %d\n", pcData.questobj)
        fp.sprintf(false, "QuestGiv %d\n", pcData.questgiver)
    }

    fp.sprintf(false, "Relig %d\n", ch.religion)
    fp.sprintf(false, "Haskilled %d\n", pcData.has_killed)
    fp.sprintf(false, "Antkilled %d\n", pcData.anti_killed)

    /* character log info */
    fp.sprintf(false, "PlayLog 1\n")    /* 1 stands for version */
    for (l in 0 until nw_config.max_time_log) {
        val today = get_played_day(l)
        val d_time = get_played_time(ch, l)

        if (d_time != 0) {
            fp.sprintf(false, "%d %d\n", today, d_time)
        }
    }
    fp.sprintf(false, "-1\n")
    fp.sprintf(false, "End\n\n")
}

/* write a pet */

fun fwrite_pet(pet: CHAR_DATA, fp: TextBuffer) {
    fp.sprintf(false, "#PET\n")
    fp.sprintf(false, "Vnum %d\n", pet.pIndexData.vnum)
    fp.sprintf(false, "Name %s~\n", pet.name)
    fp.sprintf(false, "LogO %d\n", current_time)
    fp.sprintf(false, "Cab  %d\n", pet.cabal)
    if (pet.short_desc != pet.pIndexData.short_descr) {
        fp.sprintf(false, "ShD  %s~\n", pet.short_desc)
    }
    if (pet.long_desc != pet.pIndexData.long_descr) {
        fp.sprintf(false, "LnD  %s~\n", pet.long_desc)
    }
    if (pet.description != pet.pIndexData.description) {
        fp.sprintf(false, "Desc %s~\n", pet.description)
    }
    if (pet.race !== pet.pIndexData.race)
    /* serdar ORG_RACE */ {
        fp.sprintf(false, "Race %s~\n", pet.race.name)
    }
    fp.sprintf(false, "Sex  %d\n", pet.sex)
    if (pet.level != pet.pIndexData.level) {
        fp.sprintf(false, "Levl %d\n", pet.level)
    }
    fp.sprintf(false, "HMV  %d %d %d %d %d %d\n",
            pet.hit, pet.max_hit, pet.mana, pet.max_mana, pet.move, pet.max_move)
    if (pet.gold > 0) {
        fp.sprintf(false, "Gold %d\n", pet.gold)
    }
    if (pet.silver > 0) {
        fp.sprintf(false, "Silv %d\n", pet.silver)
    }
    if (pet.exp > 0) {
        fp.sprintf(false, "Exp  %d\n", pet.exp)
    }
    if (pet.act != pet.pIndexData.act) {
        fp.sprintf(false, "Act  %s\n", print_flags(pet.act))
    }
    if (pet.affected_by != pet.pIndexData.affected_by) {
        fp.sprintf(false, "AfBy %s\n", print_flags(pet.affected_by))
    }
    if (pet.comm != 0L) {
        fp.sprintf(false, "Comm %s\n", print_flags(pet.comm))
    }
    fp.sprintf(false, "Pos  %d\n", if (pet.position == Pos.Fighting) Pos.Standing else pet.position)
    if (pet.saving_throw != 0) {
        fp.sprintf(false, "Save %d\n", pet.saving_throw)
    }
    if (pet.alignment != pet.pIndexData.alignment) {
        fp.sprintf(false, "Alig %d\n", pet.alignment)
    }
    if (pet.hitroll != pet.pIndexData.hitroll) {
        fp.sprintf(false, "Hit  %d\n", pet.hitroll)
    }
    if (pet.damroll != pet.pIndexData.damage[DICE_BONUS]) {
        fp.sprintf(false, "Dam  %d\n", pet.damroll)
    }
    fp.sprintf(false, "ACs  %d %d %d %d\n",
            pet.armor[0], pet.armor[1], pet.armor[2], pet.armor[3])
    fp.sprintf(false, "Attr %d %d %d %d %d %d\n",
            pet.perm_stat[PrimeStat.Strength], pet.perm_stat[PrimeStat.Intelligence],
            pet.perm_stat[PrimeStat.Wisdom], pet.perm_stat[PrimeStat.Dexterity],
            pet.perm_stat[PrimeStat.Constitution], pet.perm_stat[PrimeStat.Charisma])
    fp.sprintf(false, "AMod %d %d %d %d %d %d\n",
            pet.mod_stat[PrimeStat.Strength], pet.mod_stat[PrimeStat.Intelligence],
            pet.mod_stat[PrimeStat.Wisdom], pet.mod_stat[PrimeStat.Dexterity],
            pet.mod_stat[PrimeStat.Constitution], pet.mod_stat[PrimeStat.Charisma])

    var paf = pet.affected
    while (paf != null) {
        if (paf.type == Skill.doppelganger) {
            paf = paf.next
            continue
        }
        fp.sprintf(false, "Affc '%s' %3d %3d %3d %3d %3d %10d\n",
                paf.type.skillName,
                paf.where, paf.level, paf.duration, paf.modifier, paf.location,
                paf.bitvector)
        paf = paf.next
    }

    fp.sprintf(false, "End\n")
}

/*
* Write an object and its contents.
*/
fun fwrite_content(ch: CHAR_DATA, objs: List<Obj>, fp: TextBuffer, iNest: Int) {
    objs.forEach { fwrite_obj(ch, it, fp, iNest) }
}

private fun fwrite_obj(ch: CHAR_DATA, obj: Obj, fp: TextBuffer, iNest: Int) {
    val clanItem = Clan.values().any { obj.pIndexData.vnum == it.obj_vnum }
    if (clanItem) {
        return
    }

    if (ch.level < 10 && obj.pIndexData.limit != -1
            || obj.item_type === ItemType.Key && obj.value[0] == 0L
            || obj.item_type === ItemType.Map && obj.value[0] == 0L
            || ch.level < obj.level - 3 && obj.item_type !== ItemType.Container
            || ch.level > obj.level + 35 && obj.pIndexData.limit > 1) {
        extract_obj(obj)
        return
    }

    if (obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM1
            || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM2
            || obj.pIndexData.vnum == OBJ_VNUM_QUEST_ITEM3
            || obj.pIndexData.vnum == OBJ_VNUM_EYED_SWORD) {
        if (!obj.short_desc.contains(ch.name)) {
            act("\$p vanishes!", ch, obj, null, TO_CHAR)
            extract_obj(obj)
            return
        }
    }

    fp.sprintf(false, "#O\n")
    fp.sprintf(false, "Vnum %d\n", obj.pIndexData.vnum)
    fp.sprintf(false, "Cond %d\n", obj.condition)

    if (!obj.pIndexData.new_format) {
        fp.sprintf(false, "Oldstyle\n")
    }
    if (obj.enchanted) {
        fp.sprintf(false, "Enchanted\n")
    }
    fp.sprintf(false, "Nest %d\n", iNest)

    /* these data are only used if they do not match the defaults */

    if (obj.name != obj.pIndexData.name) {
        fp.sprintf(false, "Name %s~\n", obj.name)
    }
    if (obj.short_desc != obj.pIndexData.short_descr) {
        fp.sprintf(false, "ShD  %s~\n", obj.short_desc)
    }
    if (obj.description != obj.pIndexData.description) {
        fp.sprintf(false, "Desc %s~\n", obj.description)
    }
    if (obj.extra_flags != obj.pIndexData.extra_flags) {
        fp.sprintf(false, "ExtF %d\n", obj.extra_flags)
    }
    if (obj.wear_flags != obj.pIndexData.wear_flags) {
        fp.sprintf(false, "WeaF %d\n", obj.wear_flags)
    }
    if (obj.item_type != obj.pIndexData.item_type) {
        fp.sprintf(false, "Ityp %d\n", obj.item_type)
    }
    if (obj.weight != obj.pIndexData.weight) {
        fp.sprintf(false, "Wt   %d\n", obj.weight)
    }

    /* variable data */

    fp.sprintf(false, "WLoc %d\n", obj.wear_loc)
    if (obj.level != obj.pIndexData.level) {
        fp.sprintf(false, "Lev  %d\n", obj.level)
    }
    if (obj.timer != 0) {
        fp.sprintf(false, "Time %d\n", obj.timer)
    }
    fp.sprintf(false, "Cost %d\n", obj.cost)
    if (obj.value[0] != obj.pIndexData.value[0]
            || obj.value[1] != obj.pIndexData.value[1]
            || obj.value[2] != obj.pIndexData.value[2]
            || obj.value[3] != obj.pIndexData.value[3]
            || obj.value[4] != obj.pIndexData.value[4]) {
        fp.sprintf(false, "Val  %d %d %d %d %d\n", obj.value[0], obj.value[1], obj.value[2], obj.value[3], obj.value[4])
    }

    when (obj.item_type) {
        ItemType.Potion, ItemType.Scroll -> {
            if (obj.value[1] > 0) {
                fp.sprintf(false, "Spell 1 '%s'\n", Skill.skills[obj.value[1].toInt()].skillName)
            }

            if (obj.value[2] > 0) {
                fp.sprintf(false, "Spell 2 '%s'\n", Skill.skills[obj.value[2].toInt()].skillName)
            }

            if (obj.value[3] > 0) {
                fp.sprintf(false, "Spell 3 '%s'\n", Skill.skills[obj.value[3].toInt()].skillName)
            }
        }

        ItemType.Pill, ItemType.Staff, ItemType.Wand -> if (obj.value[3] > 0) {
            fp.sprintf(false, "Spell 3 '%s'\n", Skill.skills[obj.value[3].toInt()].skillName)
        }
        else -> {
        }
    }

    var paf = obj.affected
    while (paf != null) {
        fp.sprintf(false, "Affc '%s' %3d %3d %3d %3d %3d %10d\n",
                paf.type.skillName, paf.where, paf.level, paf.duration, paf.modifier, paf.location, paf.bitvector)
        paf = paf.next
    }

    var ed: EXTRA_DESC_DATA? = obj.extra_desc
    while (ed != null) {
        fp.sprintf(false, "ExDe %s~ %s~\n", ed.keyword, ed.description)
        ed = ed.next
    }
    fp.sprintf(false, "End\n\n")
    if (obj.contains.isNotEmpty()) {
        fwrite_content(ch, obj.contains, fp, iNest + 1)
    }

}

/** Load a char and inventory into a new ch structure. */
fun load_char_obj(con: Connection, name: String): CHAR_DATA {
    val ch = new_char()
    val pcData = PC_DATA()

    ch.pcdata = pcData
    con.ch = ch
    ch.name = name
    ch.id = nextMobId()
    ch.race = Race.HUMAN
    ch.hometown = HomeTown.Midgaard
    ch.ethos = Ethos.Neutral
    ch.affected_by = 0
    ch.act = PLR_NOSUMMON or PLR_NOCANCEL
    ch.comm = COMM_COMBINE or COMM_PROMPT

    pcData.perm_hit = 20
    pcData.perm_mana = 100
    pcData.perm_move = 100

    ch.invis_level = 0
    ch.practice = 0
    ch.train = 0
    ch.hitroll = 0
    ch.damroll = 0
    ch.trust = 0
    ch.wimpy = 0
    ch.saving_throw = 0
    ch.progtypes = 0
    ch.extracted = false
    ch.prompt = DEFAULT_PROMPT

    pcData.points = 0
    pcData.confirm_delete = false
    pcData.confirm_remort = false
    pcData.pwd = ""
    pcData.bamfin = ""
    pcData.bamfout = ""
    pcData.title = ""
    pcData.time_flag = 0
    ch.perm_stat.fill(13)

    pcData.condition[COND_THIRST] = 48
    pcData.condition[COND_FULL] = 48
    pcData.condition[COND_HUNGER] = 48
    pcData.condition[COND_BLOODLUST] = 48
    pcData.condition[COND_DESIRE] = 48

    pcData.nextquest = 0
    pcData.questpoints = 0
    pcData.questgiver = 0
    pcData.countdown = 0
    pcData.questobj = 0
    pcData.questmob = 0
    ch.religion = Religion.None
    pcData.has_killed = 0
    pcData.anti_killed = 0
    ch.timer = 0
    ch.hunting = null
    ch.endur = 0
    ch.riding = false
    ch.mount = null
    ch.in_mind = ""

    val fileName = nw_config.lib_player_dir + "/" + capitalize(name)
    val file = File(fileName)
    if (file.exists()) try {
        val fp = DikuTextFile(file)
        Arrays.fill(rgObjNest, null)
        ch.new_character = false
        loop@ while (true) {
            val letter = fp.fread_letter()
            if (letter == '*') {
                fp.fread_to_eol()
                continue
            }

            if (letter != '#') {
                bug("Load_char_obj: # not found.")
                break
            }

            val word = fp.word()
            when {
                eq(word, "PLAYER") -> fread_char(ch, fp)
                eq(word, "OBJECT") -> fread_obj(ch, fp)
                eq(word, "O") -> fread_obj(ch, fp)
                eq(word, "PET") -> fread_pet(ch, fp)
                eq(word, "END") -> break@loop
                else -> {
                    bug("Load_char_obj: bad section.")
                    break@loop
                }
            }
        }
    } catch (e: IOException) {
        log_string("load char obj error:" + e.message)
    }

    if (ch.new_character) return ch

    /* initialize race */
    ch.size = ch.race.size
    ch.attack = AttackType.Punch

    ch.affected_by = ch.affected_by or ch.race.aff
    ch.imm_flags = ch.imm_flags or ch.race.imm
    ch.res_flags = ch.res_flags or ch.race.res
    ch.vuln_flags = ch.vuln_flags or ch.race.vuln
    ch.form = ch.race.form
    ch.parts = ch.race.parts

    if (pcData.condition[COND_BLOODLUST] < 48 && ch.clazz !== Clazz.VAMPIRE) {
        pcData.condition[COND_BLOODLUST] = 48
    }

    /**
     * Add the bonus time now, because we don't allow old players
     * to be loaded with limited equipments!
     */
    if (IS_SET(pcData.time_flag, TLP_NOLOG)) {
        val add_time = nw_config.max_time_log + nw_config.min_time_limit / nw_config.max_time_log
        pcData.log_time.fill(add_time)
        pcData.time_flag = REMOVE_BIT(pcData.time_flag, TLP_NOLOG)
    }

    return ch
}

/** Read in a char. */
fun fread_char(ch: CHAR_DATA, fp: DikuTextFile) {
    log_string("Loading ${ch.name}")

    val pcData = ch.pcdata!!

    var fPlayLog = false
    var count = 0
    pcData.bank_s = 0
    pcData.bank_g = 0

    while (true) {
        var word = fp.word()
        if (word.isEmpty()) {
            word = "End"
        }
        when (word) {
            "*" -> fp.fread_to_eol()
            "Act" -> ch.act = fp.long()
            "AffectedBy", "AfBy" -> fp.long()
            "Alignment", "Alig" -> ch.alignment = fp.int()
            "AntKilled" -> pcData.anti_killed = fp.int()
            "Alia" -> if (count >= nw_config.max_alias) {
                fp.fread_to_eol()
            } else {
                pcData.alias[count] = fp.word()
                pcData.alias_sub[count] = fp.word()
                count++
            }
            "Alias" -> if (count >= nw_config.max_alias) {
                fp.fread_to_eol()
            } else {
                pcData.alias[count] = fp.word()
                pcData.alias_sub[count] = fp.string()
                count++
            }
            "AC", "Armor" -> fp.fread_to_eol()
            "ACs" -> {
                for (i in 0..3) {
                    ch.armor[i] = fp.int()
                }
            }
            "AffD" -> {
                val paf = Affect()
                paf.type = Skill.lookupNN(fp.word())
                paf.level = fp.int()
                paf.duration = fp.int()
                paf.modifier = fp.int()
                paf.location = Affect.of(fp.int(), { bugf(fp, "affect: $it") })
                paf.bitvector = fp.int().toLong()
                paf.next = ch.affected
                ch.affected = paf
            }
            "Affc" -> {
                val paf = Affect()
                paf.type = Skill.lookupNN(fp.word())
                paf.where = fp.int()
                paf.level = fp.int()
                paf.duration = fp.int()
                paf.modifier = fp.int()
                paf.location = Affect.of(fp.int(), { bugf(fp, "affect: $it") })
                paf.bitvector = fp.int().toLong()
                paf.next = ch.affected
                ch.affected = paf

            }
            "AttrMod", "AMod" -> PrimeStat.values().forEach { fp.int() } // ignored
            "AttrPerm", "Attr" -> ch.perm_stat.fill { fp.int() }
            "Bamfin" -> pcData.bamfin = fp.string()
            "Banks" -> pcData.bank_s = fp.int()
            "Bankg" -> pcData.bank_g = fp.int()
            "Bamfout" -> pcData.bamfout = fp.string()
            "Bin" -> pcData.bamfin = fp.string()
            "Bout" -> pcData.bamfout = fp.string()
            "Cla", "Class" -> ch.clazz = Clazz.lookupNN(fp.word())
            "Cab" -> ch.cabal = Clan.of(fp.int()) ?: bugf(fp, "Illegal clan!")
            "Condition", "Cond" -> {
                pcData.condition[0] = fp.int()
                pcData.condition[1] = fp.int()
                pcData.condition[2] = fp.int()
            }
            "CndC" -> {
                pcData.condition[0] = fp.int()
                pcData.condition[1] = fp.int()
                pcData.condition[2] = fp.int()
                pcData.condition[3] = fp.int()
                pcData.condition[4] = fp.int()
                pcData.condition[5] = fp.int()
            }
            "Cnd" -> {
                pcData.condition[0] = fp.int()
                pcData.condition[1] = fp.int()
                pcData.condition[2] = fp.int()
                pcData.condition[3] = fp.int()
            }
            "Comm" -> ch.comm = fp.long()

            "Damroll" -> fp.int()
            "Dam" -> fp.int()
            "Description", "Desc" -> fp.string()
            "Dead" -> pcData.death = fp.int()
            "Detect" -> fp.long()

            "End" -> {
                /* adjust hp mana move up  -- here for speed's sake
                int percent;

                    percent = (current_time - lastlogoff) * 25 / ( 2 * 60 * 60);

                percent = UMIN(percent,100);

                    if (percent > 0 && !IS_AFFECTED(ch,AFF_POISON)
                    &&  !IS_AFFECTED(ch,AFF_PLAGUE))
                    {
                        ch.hit += (ch.max_hit - ch.hit) * percent / 100;
                        ch.mana    += (ch.max_mana - ch.mana) * percent / 100;
                        ch.move    += (ch.max_move - ch.move)* percent / 100;
                    }
                */
                ch.played = pcData.played

                /* if this is an old player, give some played time */
                if (!fPlayLog) {
                    pcData.time_flag = SET_BIT(pcData.time_flag, TLP_NOLOG)
                }
            }
            "Exp" -> ch.exp = fp.int()
            "Etho" -> ch.ethos = Ethos.fromInt(fp.int())

            "Gold" -> ch.gold = fp.int()
            "Group", "Gr" -> fp.word()
            "Hitroll" -> fp.int()
            "Hit" -> fp.int()
            "Home" -> ch.hometown = HomeTown.of(fp.int())
            "Haskilled" -> pcData.has_killed = fp.int()
            "HpManaMove", "HMV" -> {
                ch.hit = fp.int()
                ch.mana = fp.int()
                ch.move = fp.int()
            }
            "HpManaMovePerm", "HMVP" -> {
                pcData.perm_hit = fp.int()
                pcData.perm_mana = fp.int()
                pcData.perm_move = fp.int()
            }

            "Id" -> ch.id = fp.int()
            "InvisLevel" -> ch.invis_level = fp.int()
            "Inco" -> ch.incog_level = fp.int()
            "Invi" -> ch.invis_level = fp.int()

            "LastLevel", "LLev" -> pcData.last_level = fp.int()
            "Level", "Lev", "Levl" -> ch.level = fp.int()
            "LogO" -> fp.long()
            "LongDescr", "LnD" -> ch.long_desc = fp.string()

            "Name" -> ch.name = fp.string()
            "Note" -> pcData.last_note = fp.long()
            "Not" -> {
                pcData.last_note = fp.long()
                pcData.last_idea = fp.long()
                pcData.last_penalty = fp.long()
                pcData.last_news = fp.long()
                pcData.last_changes = fp.long()
            }

            "Password", "Pass" -> pcData.pwd = fp.string()
            "Played", "Plyd" -> pcData.played = fp.int()
            "Points", "Pnts" -> pcData.points = fp.int()
            "Position", "Pos" -> ch.position = Pos.of(fp.int())
            "Practice", "Prac" -> ch.practice = fp.int()
            "Prompt", "Prom" -> ch.prompt = fp.string()
            "PlayLog" -> {
                val d_start = get_played_day(nw_config.max_time_log - 1)
                val d_stop = get_played_day(0)
                var l = 0
                while (l < MAX_TIME_LOG) {
                    pcData.log_time[l] = 0
                    l++
                }

                fp.int()   /* read the version */
                while (!fp.feof()) {
                    val d = fp.int()
                    if (d < 0) {
                        break
                    }
                    val t = fp.int()
                    if (d in d_start..d_stop) {
                        l = d_stop - d
                        pcData.log_time[l] += t
                    }
                }
                fPlayLog = true
            }

            "QuestCnt" -> pcData.countdown = fp.int()
            "QuestMob" -> pcData.questmob = fp.int()
            "QuestObj" -> pcData.questobj = fp.int()
            "QuestGiv" -> pcData.questgiver = fp.int()
            "QuestPnts" -> pcData.questpoints = fp.int()
            "QuestNext" -> pcData.nextquest = fp.int()
            "Ques" -> ch.quest = fp.long()

            "Relig" -> ch.religion = Religion.fromInt(fp.int())
            "Race" -> {
                ch.race = Race.lookupNN(fp.string())
                ch.race = ch.race
            }
            "Room" -> ch.room = get_room_index(fp.int()) ?: get_room_index_nn(ROOM_VNUM_LIMBO)

            "SavingThrow" -> fp.int()
            "Save" -> fp.int()
            "Scro" -> ch.lines = fp.int()
            "Sex" -> ch.sex = Sex.of(fp.int())!!
            "ShortDescr", "ShD" -> ch.short_desc = fp.string()
            "Silv" -> ch.silver = fp.int()
            "Skill", "Sk" -> {
                val value = fp.int()
                val temp = fp.word()
                val sn = Skill.lookupNN(temp)
                pcData.learned[sn.ordinal] = value
            }

            "trueSex", "TSex" -> pcData.true_sex = Sex.of(fp.int())!!
            "Trai" -> ch.train = fp.int()
            "Trust", "Tru" -> ch.trust = fp.int()
            "Title", "Titl" -> {
                pcData.title = fp.string()
                if (pcData.title[0] != '.' && pcData.title[0] != ','
                        && pcData.title[0] != '!' && pcData.title[0] != '?') {
                    pcData.title = " " + pcData.title
                }
            }

            "Vnum" -> ch.pIndexData = get_mob_index_nn(fp.int())
            "Wimpy" -> ch.wimpy = fp.int()
            "Wimp" -> ch.wimpy = fp.int()
            "Wizn" -> ch.wiznet.addAll(Wiznet.ofList(fp.word()))
            else -> {
                bug("Fread_char: no match.")
                fp.fread_to_eol()
            }
        }
    }
}

/* load a pet from the forgotten reaches */
fun fread_pet(ch: CHAR_DATA, fp: DikuTextFile) {
    var lastlogoff = current_time

    /* first entry had BETTER be the vnum or we barf */
    var word = if (fp.feof()) "END" else fp.word()
    val pet = if (eq(word, "Vnum")) {
        val vnum = fp.int()
        create_mobile(get_mob_index_nn(if (get_mob_index(vnum) == null) {
            bug("Fread_pet: bad vnum %d.", vnum)
            MOB_VNUM_FIDO
        } else vnum))
    } else {
        bug("Fread_pet: no vnum in file.")
        create_mobile(get_mob_index_nn(MOB_VNUM_FIDO))
    }

    while (true) {
        word = if (fp.feof()) "END" else fp.word()

        when (word) {
            "*" -> fp.fread_to_eol()
            "Act" -> pet.act = fp.long()
            "AfBy" -> pet.affected_by = fp.long()
            "Alig" -> pet.alignment = fp.int()

            "ACs" -> for (i in 0..3) pet.armor[i] = fp.int()
            "AffD" -> {
                val paf = Affect()
                paf.type = Skill.lookupNN(fp.word())
                paf.level = fp.int()
                paf.duration = fp.int()
                paf.modifier = fp.int()
                paf.location = Affect.of(fp.int(), { bugf(fp, "affect: $it") })
                paf.bitvector = fp.int().toLong()
                paf.next = pet.affected
                pet.affected = paf
            }
            "Affc" -> {
                val paf = Affect()
                paf.type = Skill.lookupNN(fp.word())
                paf.where = fp.int()
                paf.level = fp.int()
                paf.duration = fp.int()
                paf.modifier = fp.int()
                paf.location = Affect.of(fp.int(), { bugf(fp, "affect: $it") })
                paf.bitvector = fp.int().toLong()
                paf.next = pet.affected
                pet.affected = paf
            }
            "AMod" -> pet.mod_stat.fill { fp.int() }
            "Attr" -> pet.perm_stat.fill { fp.int() }

            "Cab" -> pet.cabal = Clan.of(fp.int()) ?: bugf(fp, "Illegal clan!")
            "Comm" -> pet.comm = fp.long()

            "Dam" -> pet.damroll = fp.int()
            "Desc" -> pet.description = fp.string()


            "End" -> {
                pet.leader = ch
                pet.master = ch
                ch.pet = pet
                /* adjust hp mana move up  -- here for speed's sake */
                var percent = ((current_time - lastlogoff) * 25 / (2 * 60 * 60)).toInt()

                if (percent > 0 && !IS_AFFECTED(ch, AFF_POISON) && !IS_AFFECTED(ch, AFF_PLAGUE)) {
                    percent = Math.min(percent, 100)
                    pet.hit += (pet.max_hit - pet.hit) * percent / 100
                    pet.mana += (pet.max_mana - pet.mana) * percent / 100
                    pet.move += (pet.max_move - pet.move) * percent / 100
                }
                pet.room = get_room_index_nn(ROOM_VNUM_LIMBO)
                return
            }
            "Exp" -> pet.exp = fp.int()
            "Gold" -> pet.gold = fp.int()

            "Hit" -> pet.hitroll = fp.int()

            "HMV" -> {
                pet.hit = fp.int()
                pet.max_hit = fp.int()
                pet.mana = fp.int()
                pet.max_mana = fp.int()
                pet.move = fp.int()
                pet.max_move = fp.int()
            }

            "Levl" -> pet.level = fp.int()
            "LnD" -> pet.long_desc = fp.string()
            "LogO" -> lastlogoff = fp.long()


            "Name" -> pet.name = fp.string()

            "Pos" -> pet.position = Pos.of(fp.int())

            "Race" -> {
                pet.race = Race.lookupNN(fp.string())
                pet.race = pet.race
            }

            "Save" -> pet.saving_throw = fp.int()
            "Sex" -> pet.sex = Sex.of(fp.int())!!
            "ShD" -> pet.short_desc = fp.string()
            "Silv" -> pet.silver = fp.int()
        }
    }
}


fun fread_obj(ch: CHAR_DATA, fp: DikuTextFile) {

    var obj: Obj? = null
    var first = true  /* used to counter fp offset */
    var new_format = false
    var make_new = false // update object

    var word = if (fp.feof()) "End" else fp.word()
    if (eq(word, "Vnum")) {
        first = false  /* fp will be in right place */

        val vnum = fp.int()
        if (get_obj_index(vnum) == null) {
            bug("Fread_obj: bad vnum %d.", vnum)
        } else {
            obj = create_object_nocount(get_obj_index_nn(vnum), -1)
            new_format = true
        }

    }

    if (obj == null)
    /* either not found or old style */ {
        obj = Obj()
        obj.name = ""
        obj.short_desc = ""
        obj.description = ""
    }

    var fNest = false
    var fVnum = true
    var iNest = 0
    val buf = TextBuffer()
    while (true) {
        if (first) {
            first = false
        } else {
            word = if (fp.feof()) "End" else fp.word()
        }
        when (word) {
            "*" -> fp.fread_to_eol()

            "AffD" -> {
                val paf = Affect()
                paf.type = Skill.lookupNN(fp.word())
                paf.level = fp.int()
                paf.duration = fp.int()
                paf.modifier = fp.int()
                paf.location = Affect.of(fp.int(), { bugf(fp, "affect: $it") })
                paf.bitvector = fp.int().toLong()
                paf.next = obj!!.affected
                obj.affected = paf
            }
            "Affc" -> {
                val paf = Affect()
                paf.type = Skill.lookupNN(fp.word())
                paf.where = fp.int()
                paf.level = fp.int()
                paf.duration = fp.int()
                paf.modifier = fp.int()
                paf.location = Affect.of(fp.int(), { bugf(fp, "affect: $it") })
                paf.bitvector = fp.int().toLong()
                paf.next = obj!!.affected
                obj.affected = paf
            }
            "Cond" -> {
                obj!!.condition = fp.int()
                if (obj.condition < 1) {
                    obj.condition = 100
                }
            }
            "Cost" -> obj!!.cost = fp.int()
            "Description" -> obj!!.description = fp.string()
            "Desc" -> obj!!.description = fp.string()
            "Enchanted" -> obj!!.enchanted = true
            "ExtF", "ExtraFlags" -> obj!!.extra_flags = fp.long()
            "ExtraDescr", "ExDe" -> {
                val ed = EXTRA_DESC_DATA()
                ed.keyword = fp.string()
                ed.description = fp.string()
                ed.next = obj!!.extra_desc
                obj.extra_desc = ed
            }
            "End" -> {
                obj!!
                if (!fNest || !fVnum) {
                    bug("Fread_obj: incomplete object.")
                    return
                } else if (obj.pIndexData.limit != -1 && get_total_played(ch) < nw_config.min_time_limit) {
                    sprintf(buf, "Ignoring limited %d for %s.", obj.pIndexData.vnum, ch.name)
                    log_string(buf)
                    extract_obj(obj)
                    rgObjNest[iNest] = null
                    return
                }
                if (!new_format) {
                    Index.OBJECTS.add(obj)
                    obj.pIndexData.count++
                }

                if (!obj.pIndexData.new_format && obj.item_type == ItemType.Armor && obj.value[1] == 0L) {
                    obj.value[1] = obj.value[0]
                    obj.value[2] = obj.value[0]
                }
                if (make_new) {
                    val wear = obj.wear_loc
                    extract_obj(obj)

                    obj = create_object(obj.pIndexData, 0)
                    obj.wear_loc = wear
                }
                if (iNest == 0 || rgObjNest[iNest - 1] == null) {
                    obj_to_char(obj, ch)
                } else {
                    //TODO: obj_to_obj(obj, rgObjNest[iNest - 1])
                }
            }

            "ItemType", "Ityp" -> obj!!.item_type = ItemType.of(fp.int(), { bugf(fp, "itemType: $it") })

            "Lev", "Level" -> obj!!.level = fp.int()
            "Name" -> obj!!.name = fp.string()
            "Nest" -> {
                iNest = fp.int()
                if (iNest < 0 || iNest >= MAX_NEST) {
                    bug("Fread_obj: bad nest %d.", iNest)
                } else {
                    rgObjNest[iNest] = obj
                    fNest = true
                }
            }
            "Oldstyle" -> {
                if (obj!!.pIndexData.new_format) {
                    make_new = true
                }
            }

            "Quality" -> obj!!.condition = fp.int()
            "ShortDescr", "ShD" -> obj!!.short_desc = fp.string()
            "Spell" -> {
                val iValue = fp.int()
                val sn = Skill.lookupNN(fp.word())
                when {
                    iValue < 0 || iValue > 3 -> bug("Fread_obj: bad iValue %d.", iValue)
                    else -> obj!!.value[iValue] = sn.ordinal.toLong()
                }
            }

            "Time", "Timer" -> obj!!.timer = fp.int()
            "Values", "Vals" -> {
                obj!!.value[0] = fp.int().toLong()
                obj.value[1] = fp.int().toLong()
                obj.value[2] = fp.int().toLong()
                obj.value[3] = fp.int().toLong()
                if (obj.item_type == ItemType.Weapon && obj.value[0] == 0L) {
                    obj.weaponType = obj.pIndexData.weaponType
                }
            }
            "Val" -> {
                obj!!.value[0] = fp.int().toLong()
                obj.value[1] = fp.int().toLong()
                obj.value[2] = fp.int().toLong()
                obj.value[3] = fp.int().toLong()
                obj.value[4] = fp.int().toLong()
            }
            "Vnum" -> {
                val vnum = fp.int()
                val index = get_obj_index(vnum)
                if (obj == null || index == null) {
                    bug("Fread_obj: bad vnum %d.", vnum)
                } else {
                    obj.pIndexData = index
                    fVnum = true
                }
            }

            "WearFlags", "WeaF" -> obj!!.wear_flags = fp.long()
            "Weight" -> obj!!.weight = fp.int()
            "WLoc" -> obj!!.wear_loc = Wear.of(fp.int(), { bugf(fp, "wear: $it") })
            "Wt" -> obj!!.weight = fp.int()
            "Wear", "Wearloc" -> obj!!.wear_loc = Wear.ofOld(fp.int())
            else -> {
                bug("Fread_obj: no match.")
                fp.fread_to_eol()
            }
        }
    }
}
