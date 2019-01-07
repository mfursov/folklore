@file:Suppress("FunctionName", "FunctionName", "LocalVariableName")

package net.sf.nightworks

import net.sf.nightworks.Tables.act_flags
import net.sf.nightworks.Tables.affect_flags
import net.sf.nightworks.Tables.form_flags
import net.sf.nightworks.Tables.imm_flags
import net.sf.nightworks.Tables.off_flags
import net.sf.nightworks.Tables.part_flags
import net.sf.nightworks.Tables.res_flags
import net.sf.nightworks.Tables.vuln_flags
import net.sf.nightworks.act.mob.assign_mob_prog
import net.sf.nightworks.act.obj.assign_obj_prog
import net.sf.nightworks.model.*
import net.sf.nightworks.util.*
import java.io.File
import java.io.IOException

var help_greeting = ""
val time_info = TimeData()
val weather = WeatherInfo()
val kill_table = Array(MAX_LEVEL) { KILL_DATA() }

var top_affect: Int = 0
var top_ed: Int = 0
var top_reset: Int = 0

private var isLoading: Boolean = false

fun boot_db() {
    isLoading = true

    // Set time and weather.
    setTimeAndWeather()

    top_affected_room = null
    reboot_counter = -1
    time_sync = 0
    max_newbies = MAX_NEWBIES
    max_oldies = MAX_OLDIES
    iNumPlayers = 0
    try {
        read_classes()
        read_races()
        readAreas()
        fix_exits()
        load_limited_objects()
        log_string("Total non-immortal levels > 5: $total_levels")

        isLoading = false
        area_update()
        load_notes()
        load_socials()
    } catch (e: Exception) {
        e.printStackTrace()
        exit(1)
    }
}

private fun read_races() {
    log_string("Loading races")
    val listFile = DikuTextFile(nw_config.etc_races_list)
    while (true) {
        val raceFile = listFile.word()
        if (raceFile[0] == '#') {
            continue
        }
        if (raceFile[0] == '$') {
            break
        }
        log_string("Loading $raceFile")
        val fp = DikuTextFile(nw_config.lib_races_dir + "/" + raceFile)
        var race: Race? = null
        label@ while (!fp.feof()) {
            val word = fp.word()
            when (word) {
                "#RACE" -> {
                    if (race != null) bugf(fp, "2 #RACE tokens in race file!")
                    race = read_race(fp)
                }
                "#PCRACE" -> {
                    if (race == null) bugf(fp, "Error: #PCRACE before #RACE")
                    read_pcrace(fp, race)
                }
                "#$" -> break@label
                else -> bugf(fp, "Unknown token in race file: $word")
            }
        }
    }
    Race.validate()
}

private fun read_race(fp: DikuTextFile): Race {
    var word = fp.word()
    if (word != "Name") bugf(fp, "read_race: first token is not 'Name'")

    val name = fp.string()
    if (name.toLowerCase() != name) bugf(fp, "read_race: race name is not lowercase $name")

    val race = Race.lookup(name) ?: Race(name)
    while (true) {
        word = if (fp.feof()) "End" else fp.word()
        when (word) {
            "Aff" -> race.aff = fp.flags(affect_flags)
            "Act" -> race.act = fp.flags(act_flags)
            "End" -> return race
            "Form" -> race.form = fp.flags(form_flags)
            "Flags" -> fp.flags(res_flags)
            "Imm" -> race.imm = fp.flags(imm_flags)
            "Off" -> race.off = fp.flags(off_flags)
            "Parts" -> race.parts = fp.flags(part_flags)
            "Res" -> race.res = fp.flags(res_flags)
            "Vuln" -> race.vuln = fp.flags(vuln_flags)
            else -> bugf(fp, "unknown keyword: $word")
        }
    }
}

private fun read_pcrace(fp: DikuTextFile, race: Race) {
    while (true) {
        val word = if (fp.feof()) "End" else fp.word()
        when (word) {
            "Align" -> race.align = Align.of(fp.word())
            "BonusSkills" -> {
                var skills = fp.string()
                val skillsList = mutableListOf<Skill>()
                while (skills.isNotEmpty()) {
                    val oneSkill = StringBuilder()
                    skills = one_argument(skills, oneSkill)
                    val skill = Skill.lookupNN(oneSkill.toString())
                    skillsList.add(skill)
                }
                skillsList.sort()
                race.skills = skillsList.toTypedArray()
            }
            "Class" -> {
                val name = fp.word()
                val expMult = fp.int()
                val clazz = Clazz.lookupNN(name)
                val mod = RaceToClassModifier(clazz, expMult)
                race.addClassModifier(clazz, mod)
            }
            "End" -> {
                if (race.who_name.isEmpty()) {
                    throw RuntimeException("pc_race who_name undefined")
                }
                return
            }
            "HPBonus" -> race.hp_bonus = fp.int()
            "ManaBonus" -> race.mana_bonus = fp.int()
            "MaxStats" -> for (s in PrimeStat.values()) race.max_stats[s] = fp.int()
            "Points" -> race.points = fp.int()
            "PracBonus" -> race.prac_bonus = fp.int()
            "Size" -> race.size = Size.of(fp.word()) { bugf(fp, "Param: $it") }!!
            "Slang" -> race.language = Language.of(fp.word())!!
            "ShortName" -> race.who_name = fp.string()
            "Skill" -> fp.string_eol()
            "Stats" -> for (s in PrimeStat.values()) race.stats[s] = fp.int()

            else -> bugf(fp, "unknown word: $word")
        }
    }
}

private fun read_classes() {
    log_string("Loading classes")
    val listFile = DikuTextFile(nw_config.etc_classes_list)
    while (true) {
        val classFile = listFile.word()
        if (classFile[0] == '$') {
            break
        }
        log_string("Reading class $classFile")
        val fp = DikuTextFile(nw_config.lib_classes_dir + "/" + classFile)
        var clazz: Clazz? = null
        label@ while (!fp.feof()) {
            val word = fp.word()
            when (word) {
                "#CLASS" -> {
                    if (clazz != null) {
                        throw RuntimeException("2 #CLASS tokens in clazz file!")
                    }
                    clazz = read_class(fp)
                }
                "#POSE" -> {
                    if (clazz == null) {
                        throw RuntimeException("Error: #POSE before #CLASS")
                    }
                    val pose = read_class_pose(fp)
                    clazz.poses.add(pose)
                }
                "#$" -> break@label
                else -> bugf(fp, "Unknown token in clazz file: $word")
            }
        }
    }
    Clazz.validate()
}

private fun read_class(fp: DikuTextFile): Clazz {
    var word = fp.word()
    if (word != "Name") {
        throw bugf(fp, "Class not first element!'")
    }
    val name = fp.string()
    val clazz = Clazz.lookup(name) ?: Clazz(name)

    while (true) {
        word = if (fp.feof()) "End" else fp.word()
        when (word) {
            "AddExp" -> clazz.points = fp.int()
            "Align" -> clazz.align = Align.of(fp.word())
            "End" -> return clazz
            "Ethos" -> clazz.ethos = Ethos.fromString(fp.word())
            "GuildRoom" -> clazz.guildVnums.add(fp.int())
            "HPRate" -> clazz.hp_rate = fp.int()
            "ManaRate" -> clazz.mana_rate = fp.int()
            "PrimeStat" -> clazz.attr_prime = PrimeStat.of(fp.word())
            "Skill" -> {
                val skill = Skill.lookupNN(fp.word())
                val level = fp.int()
                val rating = fp.int()
                val mod = fp.int()
                skill.skill_level[clazz.id] = level
                skill.rating[clazz.id] = rating
                skill.mod[clazz.id] = mod
            }
            "Sex" -> clazz.sex = Sex.of(fp.word()) { bugf(fp, "Param: $it") }
            "SkillAdept" -> clazz.skill_adept = fp.int()
            "SchoolWeapon" -> clazz.weapon = fp.int()
            "ShortName" -> clazz.who_name = fp.string()
            "StatMod" -> clazz.stats.fill { fp.int() }
            "Thac0_00" -> clazz.thac0_00 = fp.int()
            "Thac0_32" -> clazz.thac0_32 = fp.int()
            "Title" -> {
                val level = fp.int()
                if (level < 0 || level > MAX_LEVEL) {
                    throw RuntimeException("load_class: invalid level: $level")
                }
                val sex = Sex.of(fp.word()) { bugf(fp, "sex: $it") }
                val title = fp.string()
                if (sex.isMale()) {
                    clazz.maleTitles[level] = title
                } else {
                    clazz.femaleTitles[level] = title
                }
            }
            else -> bugf(fp, "load_class: Unknown keyword: $word")
        }
    }
}

private fun read_class_pose(fp: DikuTextFile): Pose {
    val pose = Pose()
    while (true) {
        val word = if (fp.feof()) "End" else fp.word()
        when (word) {
            "End" -> return pose
            "Others" -> pose.message_to_room = fp.string()
            "Self" -> pose.message_to_char = fp.string()
            else -> bugf(fp, "read_class_pose: Unknown keyword: $word")
        }
    }
}

private fun setTimeAndWeather() {
    val hour = (current_time - 650336715L).toInt() / (PULSE_TICK / PULSE_PER_SCD)
    time_info.minute = 0
    time_info.hour = hour % 24
    val day = hour / 24
    time_info.day = day % 35
    val month = day / 35
    time_info.month = month % 17
    time_info.year = month / 17

    weather.sunlight = when {
        time_info.hour < 5 -> SUN_DARK
        time_info.hour < 6 -> SUN_RISE
        time_info.hour < 19 -> SUN_LIGHT
        time_info.hour < 20 -> SUN_SET
        else -> SUN_DARK
    }

    weather.change = 0
    weather.mmhg = 960 + when {
        time_info.month in 7..12 -> number_range(1, 50)
        else -> number_range(1, 80)
    }
    weather.sky = when {
        weather.mmhg <= 980 -> SKY_LIGHTNING
        weather.mmhg <= 1000 -> SKY_RAINING
        weather.mmhg <= 1020 -> SKY_CLOUDY
        else -> SKY_CLOUDLESS
    }
}

private fun readAreas() {
    log_string("Loading areas")
    val listFile = DikuTextFile(nw_config.etc_area_list)
    while (true) {
        val areaFile = listFile.word()
        if (areaFile[0] == '$') {
            break
        }

        if (areaFile[0] == '-') {
            bugf(listFile, "unsupported mode '-'")
        }

        log_string("Reading area $areaFile")
        val fp = DikuTextFile(nw_config.lib_area_dir + "/" + areaFile)
        var area: Area? = null
        loop@ while (true) {
            if (fp.fread_letter() != '#') {
                bugf(fp, "Boot_db: # not found.")
            }
            val word = fp.word()
            when (word) {
                "$" -> break@loop
                "AREA" -> area = load_area(fp)
                "HELPS" -> load_helps(fp)
                "MOBILES" -> load_mobiles(fp)
                "OBJOLD" -> load_old_obj(fp)
                "OBJECTS" -> load_objects(fp)
                "RESETS" -> load_resets(fp, area!!)
                "ROOMS" -> load_rooms(fp, area!!)
                "SHOPS" -> load_shops(fp)
                "OMPROGS" -> load_omprogs(fp)
                "OLIMITS" -> load_olimits(fp)
                "SPECIALS" -> load_specials(fp)
                "PRACTICERS" -> load_practicer(fp)
                "RESETMESSAGE" -> area!!.resetMessage = fp.string()
                "FLAG" -> area!!.area_flag = fp.long()
                "SOCIALS" -> log_string("WARN: social definition in area file not supported more:" + fp.buildCurrentStateInfo())
                else -> bugf(fp, "Boot_db: bad section name.")
            }
        }
    }
}

private fun load_area(fp: DikuTextFile): Area {
    fp.string() // file name

    val name = fp.string()
    fp.fread_letter()

    val low_range = fp.int()
    val high_range = fp.int()
    fp.fread_letter()

    val writer = fp.word()
    val credits = fp.string()
    val min_vnum = fp.int()
    val max_vnum = fp.int()
    val age = 15
    val nplayer = 0
    val empty = false

    val area = Area(name, writer, credits, age, nplayer, low_range, high_range, min_vnum, max_vnum, empty, 0)
    Index.AREAS.add(area)
    return area
}

private fun load_helps(fp: DikuTextFile) {
    while (true) {
        val level = fp.int()
        val keyword = fp.string()
        if (keyword[0] == '$') {
            break
        }
        var text = fp.string()
        if (text.isNotEmpty() && text[0] == '.') {
            text = text.substring(1)
        }
        if (eq(keyword, "greeting")) {
            help_greeting = text
        }
        Index.HELP.add(HELP_DATA(level, keyword, text))
    }
}


private fun load_old_obj(fp: DikuTextFile) {
    while (true) {
        var letter = fp.fread_letter()
        if (letter != '#') {
            bugf(fp, "Load_objects: # not found.")
        }

        val vnum = fp.int()
        if (vnum == 0) {
            break
        }

        if (get_obj_index(vnum) != null) {
            bugf(fp, "Load_objects: vnum $vnum duplicated.")
        }

        val pObjIndex = ObjProto()
        pObjIndex.vnum = vnum
        pObjIndex.new_format = false
        pObjIndex.reset_num = 0
        pObjIndex.name = fp.string()
        pObjIndex.short_descr = upfirst(fp.string())
        pObjIndex.description = upfirst(fp.string())
        /* Action description */
        fp.string()

        pObjIndex.material = ""

        pObjIndex.item_type = ItemType.of(fp.int()) { bugf(fp, "itemType: $it") }
        pObjIndex.extra_flags = fp.long()
        pObjIndex.wear_flags = fp.long()
        pObjIndex.value[0] = fp.long()
        pObjIndex.value[1] = fp.long()
        pObjIndex.value[2] = fp.long()
        pObjIndex.value[3] = fp.long()
        pObjIndex.value[4] = 0
        pObjIndex.level = 0
        pObjIndex.condition = 100
        pObjIndex.weight = fp.int()
        pObjIndex.cost = fp.int()   /* Unused */
        /* Cost per day */
        fp.int()
        pObjIndex.limit = -1

        if (pObjIndex.item_type === ItemType.Weapon) {
            if (is_name("two", pObjIndex.name)
                    || is_name("two-handed", pObjIndex.name)
                    || is_name("claymore", pObjIndex.name)) {
                pObjIndex.value[4] = SET_BIT(pObjIndex.value[4], WEAPON_TWO_HANDS).toInt().toLong()
            }
        }

        loop@ while (true) {
            letter = fp.fread_letter()
            when (letter) {
                'A' -> {
                    val paf = Affect()
                    paf.where = TO_OBJECT
                    paf.type = Skill.reserved
                    paf.level = 20 /* RT temp fix */
                    paf.duration = -1
                    paf.location = Affect.of(fp.int()) { bugf(fp, "affect: $it") }
                    paf.modifier = fp.int()
                    paf.next = pObjIndex.affected
                    pObjIndex.affected = paf
                    top_affect++
                }
                'E' -> {
                    val ed = EXTRA_DESC_DATA()
                    ed.keyword = fp.string()
                    ed.description = fp.string()
                    ed.next = pObjIndex.extra_descr
                    pObjIndex.extra_descr = ed
                    top_ed++
                }
                else -> {
                    fp.ungetc()
                    break@loop
                }
            }
        }

        // fix armors
        if (pObjIndex.item_type === ItemType.Armor) {
            //todo: check code below
            pObjIndex.value[1] = pObjIndex.value[0]
            pObjIndex.value[2] = pObjIndex.value[1]
        }

        // Translate spell "slot numbers" to internal "skill numbers."
        when (pObjIndex.item_type) {
            ItemType.Pill, ItemType.Potion, ItemType.Scroll -> {
                pObjIndex.value[1] = slot_lookup_skill_num(pObjIndex.value[1].toInt()).toLong()
                pObjIndex.value[2] = slot_lookup_skill_num(pObjIndex.value[2].toInt()).toLong()
                pObjIndex.value[3] = slot_lookup_skill_num(pObjIndex.value[3].toInt()).toLong()
                pObjIndex.value[4] = slot_lookup_skill_num(pObjIndex.value[4].toInt()).toLong()
            }

            ItemType.Staff, ItemType.Wand -> pObjIndex.value[3] = slot_lookup_skill_num(pObjIndex.value[3].toInt()).toLong()
            else -> {
            }
        }
        Index.OBJ_INDEX[vnum] = pObjIndex
    }
}

private fun load_resets(fp: DikuTextFile, area: Area) {
    if (Index.AREAS.isEmpty()) bugf(fp, "Load_resets: no #AREA seen yet.")

    while (true) {
        val pRoomIndex: ROOM_INDEX_DATA
        val pexit: EXIT_DATA?
        val temp_index: ObjProto

        val letter = fp.fread_letter()
        if (letter == 'S') {
            break
        }

        if (letter == '*') {
            fp.fread_to_eol()
            continue
        }

        val pReset = RESET_DATA()
        pReset.command = letter
        /* if_flag */
        fp.int()
        pReset.arg1 = fp.int()
        pReset.arg2 = fp.int()
        pReset.arg3 = if (letter == 'G' || letter == 'R') 0 else fp.int()
        pReset.arg4 = if (letter == 'P' || letter == 'M') fp.int() else 0
        fp.fread_to_eol()

        // Validate parameters.
        // We're calling the index functions for the side effect.

        when (letter) {
            'M' -> {
                get_mob_index_nn(pReset.arg1)
                get_room_index_nn(pReset.arg3)
            }

            'O' -> {
                temp_index = get_obj_index_nn(pReset.arg1)
                temp_index.reset_num++
                get_room_index_nn(pReset.arg3)
            }

            'P' -> {
                temp_index = get_obj_index_nn(pReset.arg1)
                temp_index.reset_num++
                get_obj_index_nn(pReset.arg3)
            }

            'G', 'E' -> {
                temp_index = get_obj_index_nn(pReset.arg1)
                temp_index.reset_num++
            }

            'D' -> {
                pRoomIndex = get_room_index_nn(pReset.arg1)

                pexit = pRoomIndex.exit[pReset.arg2]
                if (pReset.arg2 < 0
                        || pReset.arg2 > 5
                        || pexit == null
                        || !IS_SET(pexit.exit_info, EX_IS_DOOR)) {
                    bugf(fp, "Load_resets: 'D': exit %d not door.", pReset.arg2)
                }

                if (pReset.arg3 < 0 || pReset.arg3 > 2) {
                    bugf(fp, "Load_resets: 'D': bad 'locks': %d.", pReset.arg3)
                }
            }

            'R' -> {
                get_room_index_nn(pReset.arg1)

                if (pReset.arg2 < 0 || pReset.arg2 > 6) {
                    bugf(fp, "Load_resets: 'R': bad exit %d.", pReset.arg2)
                }
            }
            else -> bugf(fp, "Load_resets: bad command '%c'.", letter)
        }
        if (area.reset_first == null) {
            area.reset_first = pReset
        }
        pReset.next = null
        top_reset++
    }
}

fun load_rooms(fp: DikuTextFile, area: Area) {
    while (true) {
        var letter = fp.fread_letter()
        if (letter != '#') {
            bugf(fp, "Load_rooms: # not found.")
        }

        val vnum = fp.int()
        if (vnum == 0) {
            break
        }

        isLoading = false
        if (get_room_index(vnum) != null) {
            bugf(fp, "Load_rooms: vnum %d duplicated.", vnum)
        }
        isLoading = true

        val pRoomIndex = ROOM_INDEX_DATA()
        pRoomIndex.area = area
        pRoomIndex.vnum = vnum
        pRoomIndex.name = fp.string()
        pRoomIndex.description = fp.string()
        /* Area number */
        fp.int()
        pRoomIndex.room_flags = fp.long()

        if (vnum in 3000..3399) {
            pRoomIndex.room_flags = SET_BIT(pRoomIndex.room_flags, ROOM_LAW)
        }

        pRoomIndex.sector_type = fp.int()
        if (pRoomIndex.sector_type < 0) {
            log_string("Invalid room sector_type=${pRoomIndex.sector_type} room vnum: ${pRoomIndex.vnum}")
            pRoomIndex.sector_type = 0
        }

        pRoomIndex.light = 0
        var door = 0
        while (door <= 5) {
            pRoomIndex.exit[door] = null
            door++
        }

        /* defaults */
        pRoomIndex.heal_rate = 100
        pRoomIndex.mana_rate = 100
        pRoomIndex.affected = null
        pRoomIndex.affected_by = 0
        pRoomIndex.aff_next = null

        while (true) {
            letter = fp.fread_letter()

            if (letter == 'S') {
                break
            }

            when (letter) {
                'H' -> pRoomIndex.heal_rate = fp.int() /* healing room */
                'M' -> pRoomIndex.mana_rate = fp.int() /* mana room */
                'D' -> {
                    door = fp.int()
                    if (door < 0 || door > 5) {
                        bugf(fp, "load_rooms: vnum %d has bad door number.", vnum)
                    }

                    val pexit = EXIT_DATA()
                    pexit.description = fp.string()
                    pexit.keyword = fp.string()
                    pexit.exit_info = 0
                    val locks = fp.int()
                    when (locks) {
                        1 -> pexit.exit_info = EX_IS_DOOR
                        2 -> pexit.exit_info = EX_IS_DOOR or EX_PICK_PROOF
                        3 -> pexit.exit_info = EX_IS_DOOR or EX_NO_PASS
                        4 -> pexit.exit_info = EX_IS_DOOR or EX_NO_PASS or EX_PICK_PROOF
                        5 -> pexit.exit_info = EX_NO_FLEE
                    }
                    pexit.key = fp.int()
                    pexit.vnum = fp.int()
                    pRoomIndex.exit[door] = pexit
                }
                'E' -> {
                    val ed = EXTRA_DESC_DATA()
                    ed.keyword = fp.string()
                    ed.description = fp.string()
                    ed.next = pRoomIndex.extra_descr
                    pRoomIndex.extra_descr = ed
                    top_ed++
                }
                'O' -> {
                    if (pRoomIndex.owner != null) {
                        bugf(fp, "Load_rooms: duplicate owner.")
                    }

                    pRoomIndex.owner = fp.string()
                }
                else -> bugf(fp, "Load_rooms: vnum %d has flag not 'DES'.", vnum)
            }
        }
        Index.ROOM_INDEX[vnum] = pRoomIndex
    }
}

/**
 * Snarf a shop section.
 */
fun load_shops(fp: DikuTextFile) {
    while (true) {
        val keeper = fp.int()
        if (keeper == 0) {
            break
        }
        val def: (Int) -> Nothing = { bugf(fp, "item: $it") }
        val buy_type = arrayOf(
                ItemType.of(fp.int(), def),
                ItemType.of(fp.int(), def),
                ItemType.of(fp.int(), def),
                ItemType.of(fp.int(), def),
                ItemType.of(fp.int(), def))

        val profit_buy = fp.int()
        val profit_sell = fp.int()
        val open_hour = fp.int()
        val close_hour = fp.int()
        fp.fread_to_eol()

        val pShop = Shop(keeper, buy_type, profit_buy, profit_sell, open_hour, close_hour)
        val pMobIndex = get_mob_index_nn(keeper)
        pMobIndex.pShop = pShop
        Index.SHOPS.add(pShop)
    }
}

/*
* Snarf spec proc declarations.
*/

fun load_specials(fp: DikuTextFile) {
    while (true) {
        val pMobIndex: MOB_INDEX_DATA
        val letter = fp.fread_letter()
        when (letter) {
            'S' -> return
            '*' -> {
            }
            'M' -> {
                pMobIndex = get_mob_index_nn(fp.int())
                pMobIndex.spec_fun = spec_lookup(fp.word())
                if (pMobIndex.spec_fun == null) {
                    bugf(fp, "Load_specials: 'M': vnum %d.", pMobIndex.vnum)
                }
            }
            else -> bugf(fp, "Load_specials: letter '%c' not *MS.", letter)
        }
        fp.fread_to_eol()
    }
}

/**
 * Translate all room exits from virtual to real.
 * Has to be done after all rooms are read in.
 * Check for bad reverse exits.
 */
private fun fix_exits() {
    for (room in Index.ROOM_INDEX.values) {
        var door = 0
        while (door <= 5) {
            val pexit = room.exit[door]
            if (pexit != null) {
                val room_vnum = if (pexit.vnum == ROOM_VNUM_BLOCKED_EXIT) ROOM_VNUM_VOID else pexit.vnum
                val to_room = get_room_index(room_vnum)
                if (to_room == null) {
                    bug("Invalid exit! Room not found: ${pexit.vnum}, room: ${room.vnum} area: ${room.area.name}")
                    exit(1)
                }
                pexit.to_room = to_room
            }
            door++
        }
    }

    for (pRoomIndex in Index.ROOM_INDEX.values) {
        var door = 0
        while (door <= 5) {
            val pexit = pRoomIndex.exit[door]
            val to_room = pexit?.to_room
            val toRoomExits = to_room?.exit
            val pexit_rev = if (toRoomExits == null) null else toRoomExits[rev_dir[door]]
            if (pexit != null && to_room != null && pexit_rev != null && pexit_rev.to_room != pRoomIndex
                    && (pRoomIndex.vnum < 1200 || pRoomIndex.vnum > 1299)) {
                bug("Fix_exits: ${pRoomIndex.vnum}:$door . ${to_room.vnum}:${rev_dir[door]} . ${pexit_rev.to_room.vnum}.")
            }
            door++
        }
    }
}

/**
 * Repopulate areas periodically.
 */
fun area_update() {
    for (pArea in Index.AREAS) {
        if (++pArea.age < 3) {
            continue
        }
        // Check age and reset.
        // Note: Mud School resets every 3 minutes (not 15).
        if (!pArea.empty && (pArea.nPlayers == 0 || pArea.age >= 15) || pArea.age >= 31) {
            reset_area(pArea)
            var str = pArea.name + " has just been reset."
            wiznet(str, null, null, Wiznet.Resets, null, 0)

            str = if (pArea.resetMessage.isEmpty()) pArea.resetMessage + "\n" else "You hear some squeaking sounds...\n"
            Index.CONNECTIONS
                    .filter { it.state == ConnectionState.CON_PLAYING && IS_AWAKE(it.ch!!) && it.ch!!.room.area == pArea }
                    .forEach { send_to_char(str, it.ch!!) }

            pArea.age = number_range(0, 3)
            var pRoomIndex = get_room_index(200)
            if (pRoomIndex != null && pArea == pRoomIndex.area) {
                pArea.age = 15 - 2
            }
            pRoomIndex = get_room_index(210)
            if (pRoomIndex != null && pArea == pRoomIndex.area) {
                pArea.age = 15 - 2
            }
            pRoomIndex = get_room_index(220)
            if (pRoomIndex != null && pArea == pRoomIndex.area) {
                pArea.age = 15 - 2
            }
            pRoomIndex = get_room_index(230)
            if (pRoomIndex != null && pArea == pRoomIndex.area) {
                pArea.age = 15 - 2
            }
            pRoomIndex = get_room_index(ROOM_VNUM_SCHOOL)
            if (pRoomIndex != null && pArea == pRoomIndex.area) {
                pArea.age = 15 - 2
            } else if (pArea.nPlayers == 0) {
                pArea.empty = true
            }
        }
    }
}

private fun reset_area(pArea: Area) {
    if (weather.sky == SKY_RAINING) {
        Index.CONNECTIONS
                .filter { it.pc != null }
                .map { it.pc!! }
                .filter { it.ch.room.area == pArea && get_skill(it.ch, Skill.track) > 50 && !IS_SET(it.ch.room.room_flags, ROOM_INDOORS) }
                .forEach { send_to_char("Rain devastates the tracks on the ground.\n", it.ch) }
        for (i in pArea.min_vnum..pArea.max_vnum) {
            val room = get_room_index(i) ?: continue
            if (IS_SET(room.room_flags, ROOM_INDOORS)) {
                continue
            }
            room_record("erased", room, -1)
            if (randomPercent() < 50) {
                room_record("erased", room, -1)
            }
        }
    }
    var last = true
    var level = 0
    var pReset = pArea.reset_first
    loop@ while (pReset != null) {
        var mob: CHAR_DATA? = null
        val pRoomIndex: ROOM_INDEX_DATA?
        val pMobIndex: MOB_INDEX_DATA?
        val pObjIndex: ObjProto?
        val pObjToIndex: ObjProto?
        val cabal_item: ObjProto?
        var pexit: EXIT_DATA?
        var obj: Obj? = null
        val obj_to: Obj?
        var count: Int
        val limit: Int

        when (pReset.command) {
            'M' -> {
                pMobIndex = get_mob_index(pReset.arg1)
                if (pMobIndex == null) {
                    bug("Reset_area: 'M': bad vnum %d.", pReset.arg1)
                    pReset = pReset.next
                    continue@loop
                }

                pRoomIndex = get_room_index(pReset.arg3)
                if (pRoomIndex == null) {
                    bug("Reset_area: 'R': bad vnum %d.", pReset.arg3)
                    pReset = pReset.next
                    continue@loop
                }

                if (pMobIndex.count >= pReset.arg2) {
                    last = false
                } else {
                    count = 0
                    for (m in pRoomIndex.people) {
                        if (m.pIndexData == pMobIndex) {
                            count++
                            if (count >= pReset.arg4) {
                                last = false
                                break
                            }
                        }
                    }

                    if (count < pReset.arg4) {
                        mob = create_mobile(pMobIndex)

                        // Check for pet shop.
                        val pRoomIndexPrev = get_room_index(pRoomIndex.vnum - 1)
                        if (pRoomIndexPrev != null && IS_SET(pRoomIndexPrev.room_flags, ROOM_PET_SHOP)) {
                            mob.act = SET_BIT(mob.act, ACT_PET)
                        }

                        // set area
                        //todo: mob.zone = pRoomIndex.area
                        char_to_room(mob, pRoomIndex)
                        level = URANGE(0, mob.level - 2, LEVEL_HERO - 1)
                        last = true
                    }
                }
            }

            'O' -> {
                pObjIndex = get_obj_index(pReset.arg1)
                if (pObjIndex == null) {
                    bug("Reset_area: 'O': bad vnum %d.", pReset.arg1)
                    pReset = pReset.next
                    continue@loop
                }

                pRoomIndex = get_room_index(pReset.arg3)
                if (pRoomIndex == null) {
                    bug("Reset_area: 'R': bad vnum %d.", pReset.arg3)
                    pReset = pReset.next
                    continue@loop
                }

                if (pArea.nPlayers > 0 || count_obj_list(pObjIndex, pRoomIndex.objects) > 0) {
                    last = false
                } else {

                    val ci_vnum = when (pObjIndex.vnum) {
                        OBJ_VNUM_RULER_STAND -> Clan.Ruler.obj_vnum
                        OBJ_VNUM_INVADER_SKULL -> Clan.Invader.obj_vnum
                        OBJ_VNUM_SHALAFI_ALTAR -> Clan.Shalafi.obj_vnum
                        OBJ_VNUM_CHAOS_ALTAR -> Clan.Chaos.obj_vnum
                        OBJ_VNUM_KNIGHT_ALTAR -> Clan.Knight.obj_vnum
                        OBJ_VNUM_LIONS_ALTAR -> Clan.Lion.obj_vnum
                        OBJ_VNUM_BATTLE_THRONE -> Clan.BattleRager.obj_vnum
                        OBJ_VNUM_HUNTER_ALTAR -> Clan.Hunter.obj_vnum
                        else -> 0
                    }

                    cabal_item = get_obj_index(ci_vnum)
                    if (ci_vnum != 0 && cabal_item!!.count > 0) {
                        last = false
                    } else if (pObjIndex.limit != -1 && pObjIndex.count >= pObjIndex.limit) {
                        last = false
                    } else {
                        obj = create_object(pObjIndex, Math.min(number_fuzzy(level), LEVEL_HERO - 1))
                        obj.cost = 0
                        obj_to_room(obj, pRoomIndex)
                        last = true
                    }
                }
            }

            'P' -> {
                pObjIndex = get_obj_index(pReset.arg1)
                if (pObjIndex == null) {
                    bug("Reset_area: 'P': bad vnum %d.", pReset.arg1)
                    pReset = pReset.next
                    continue@loop
                }

                pObjToIndex = get_obj_index(pReset.arg3)
                if (pObjToIndex == null) {
                    bug("Reset_area: 'P': bad vnum %d.", pReset.arg3)
                    pReset = pReset.next
                    continue@loop
                }

                limit = when {
                    pReset.arg2 > 50 -> 6 /* old format */
                    pReset.arg2 == -1 -> 999 /* no limit */
                    else -> pReset.arg2
                }

                obj_to = get_obj_type(pObjToIndex)
                count = if (obj_to == null) -1 else count_obj_list(pObjIndex, obj_to.contains)
                if (pArea.nPlayers > 0
                        || obj_to == null
                        || obj_to.in_room == null && !last
                        || pObjIndex.count >= limit && number_range(0, 4) != 0
                        || count > pReset.arg4) {
                    last = false
                } else {
                    if (pObjIndex.limit != -1 && pObjIndex.count >= pObjIndex.limit) {
                        last = false
                        log_string("Resetting area: [P] OBJ limit reached\n")
                    } else {
                        while (count < pReset.arg4) {
                            obj = create_object(pObjIndex, number_fuzzy(obj_to.level))
                            obj_to_obj(obj, obj_to)
                            count++
                            if (pObjIndex.count >= limit) {
                                break
                            }
                        }
                        /* fix object lock state! */
                        obj_to.value[1] = obj_to.pIndexData.value[1]
                        last = true
                    }
                }
            }

            'G', 'E' -> {
                pObjIndex = get_obj_index(pReset.arg1)
                if (pObjIndex == null) {
                    bug("Reset_area: 'E' or 'G': bad vnum %d.", pReset.arg1)
                    pReset = pReset.next
                    continue@loop
                }

                if (last) {
                    if (mob == null) {
                        bug("Reset_area: 'E' or 'G': null mob for vnum %d.", pReset.arg1)
                        last = false
                    } else {
                        if (mob.pIndexData.pShop != null) {
                            var olevel = 0

                            if (!pObjIndex.new_format) {
                                when (pObjIndex.item_type) {
                                    ItemType.Pill, ItemType.Potion, ItemType.Scroll -> {
                                        olevel = MAX_LEVEL - 7
                                        for (i in 1..4) {
                                            if (pObjIndex.value[i] > 0) {
                                                for (j in 0 until MAX_CLASS) {
                                                    olevel = Math.min(olevel, Skill.skills[pObjIndex.value[i].toInt()].skill_level[j])
                                                }
                                            }
                                        }
                                        olevel = Math.max(0, olevel * 3 / 4 - 2)
                                    }
                                    ItemType.Wand -> olevel = number_range(10, 20)
                                    ItemType.Staff -> olevel = number_range(15, 25)
                                    ItemType.Armor -> olevel = number_range(5, 15)
                                    ItemType.Weapon -> olevel = number_range(5, 15)
                                    ItemType.Treasure -> olevel = number_range(10, 20)
                                    else -> {
                                    }
                                }
                            }

                            obj = create_object(pObjIndex, olevel)
                            obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_INVENTORY)
                        } else {
                            if (pObjIndex.limit == -1 || pObjIndex.count < pObjIndex.limit) {
                                obj = create_object(pObjIndex, Math.min(number_fuzzy(level), LEVEL_HERO - 1))
                            }
                        }

                        if (obj != null) {
                            obj_to_char(obj, mob)
                            if (pReset.command == 'E') {
                                val iWear = Wear.ofOld(pReset.arg3)
                                if (iWear != Wear.None) {
                                    equip_char(mob, obj, iWear)
                                }
                            }
                            last = true
                        }
                    }
                }
            }

            'D' -> {
                pRoomIndex = get_room_index(pReset.arg1)
                if (pRoomIndex == null) {
                    bug("Reset_area: 'D': bad vnum %d.", pReset.arg1)
                    pReset = pReset.next
                    continue@loop
                }
                pexit = pRoomIndex.exit[pReset.arg2]
                if (pexit != null) {
                    when (pReset.arg3) {
                        0 -> {
                            pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_CLOSED)
                            pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_LOCKED)
                        }

                        1 -> {
                            pexit.exit_info = SET_BIT(pexit.exit_info, EX_CLOSED)
                            pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_LOCKED)
                        }

                        2 -> {
                            pexit.exit_info = SET_BIT(pexit.exit_info, EX_CLOSED)
                            pexit.exit_info = SET_BIT(pexit.exit_info, EX_LOCKED)
                        }
                    }
                    last = true
                }
            }

            'R' -> {
                pRoomIndex = get_room_index(pReset.arg1)
                if (pRoomIndex == null) {
                    bug("Reset_area: 'R': bad vnum %d.", pReset.arg1)
                    pReset = pReset.next
                    continue@loop
                }

                run {
                    var d0 = 0
                    while (d0 < pReset!!.arg2 - 1) {
                        val d1 = number_range(d0, pReset!!.arg2 - 1)
                        pexit = pRoomIndex.exit[d0]
                        pRoomIndex.exit[d0] = pRoomIndex.exit[d1]
                        pRoomIndex.exit[d1] = pexit
                        d0++
                    }
                }
            }
            else -> bug("Reset_area: bad command %c.", pReset.command)
        }
        pReset = pReset.next
    }
}

/**
 * Create an instance of a mobile.
 */
fun create_mobile(pMobIndex: MOB_INDEX_DATA): CHAR_DATA {
    val mob = new_char()
    mob.pIndexData = pMobIndex
    mob.name = pMobIndex.player_name
    mob.id = nextMobId()
    mob.short_desc = pMobIndex.short_descr
    mob.long_desc = pMobIndex.long_descr
    mob.description = pMobIndex.description
    mob.spec_fun = pMobIndex.spec_fun
    mob.progtypes = pMobIndex.progtypes
    mob.riding = false
    mob.mount = null
    mob.hunting = null
    mob.endur = 0
    mob.in_mind = ""
    mob.cabal = Clan.None
    mob.clazz = Clazz.MOB_CLASS


    val wealth = number_range(pMobIndex.wealth / 2, 3 * pMobIndex.wealth / 2)
    mob.gold = number_range(wealth / 200, wealth / 100)
    mob.silver = wealth - mob.gold * 100

    mob.group = pMobIndex.group
    mob.act = pMobIndex.act or ACT_IS_NPC
    mob.comm = COMM_NOCHANNELS or COMM_NOSHOUT or COMM_NOTELL
    mob.affected_by = pMobIndex.affected_by
    mob.alignment = pMobIndex.alignment
    mob.level = pMobIndex.level
    mob.hitroll = mob.level / 2 + pMobIndex.hitroll
    mob.damroll = pMobIndex.damage[DICE_BONUS]
    mob.max_hit = dice(pMobIndex.hit[DICE_NUMBER], pMobIndex.hit[DICE_TYPE]) + pMobIndex.hit[DICE_BONUS]
    mob.hit = mob.max_hit
    mob.max_mana = dice(pMobIndex.mana[DICE_NUMBER], pMobIndex.mana[DICE_TYPE]) + pMobIndex.mana[DICE_BONUS]
    mob.mana = mob.max_mana
    mob.damage[DICE_NUMBER] = pMobIndex.damage[DICE_NUMBER]
    mob.damage[DICE_TYPE] = pMobIndex.damage[DICE_TYPE]
    mob.attack = pMobIndex.attack

    mob.status = 0
    if (mob.attack == AttackType.None) {
        mob.attack = when (random(3)) {
            0 -> AttackType.Slash
            1 -> AttackType.Pound
            else -> AttackType.Pierce
        }
    }
    System.arraycopy(pMobIndex.ac, 0, mob.armor, 0, 4)

    mob.off_flags = pMobIndex.off_flags
    mob.imm_flags = pMobIndex.imm_flags
    mob.res_flags = pMobIndex.res_flags
    mob.vuln_flags = pMobIndex.vuln_flags
    mob.start_pos = pMobIndex.start_pos
    mob.default_pos = pMobIndex.default_pos
    mob.sex = Sex.of(pMobIndex.sexOption) ?: /* random sex */ Sex.of(number_range(1, 2))!!
    mob.race = pMobIndex.race
    mob.form = pMobIndex.form
    mob.parts = pMobIndex.parts
    mob.size = pMobIndex.size
    mob.material = pMobIndex.material
    mob.progtypes = pMobIndex.progtypes
    mob.extracted = false

    /* computed on the spot */
    mob.perm_stat.fill(Math.min(25, 11 + mob.level / 4))

    if (IS_SET(mob.act, ACT_WARRIOR)) {
        mob.perm_stat[PrimeStat.Strength] += 3
        mob.perm_stat[PrimeStat.Constitution] += 2
        mob.perm_stat[PrimeStat.Intelligence] -= 1
    }

    if (IS_SET(mob.act, ACT_THIEF)) {
        mob.perm_stat[PrimeStat.Dexterity] += 3
        mob.perm_stat[PrimeStat.Intelligence] += 1
        mob.perm_stat[PrimeStat.Wisdom] -= 1
    }

    if (IS_SET(mob.act, ACT_CLERIC)) {
        mob.perm_stat[PrimeStat.Wisdom] += 3
        mob.perm_stat[PrimeStat.Strength] += 1
        mob.perm_stat[PrimeStat.Dexterity] -= 1
    }

    if (IS_SET(mob.act, ACT_MAGE)) {
        mob.perm_stat[PrimeStat.Intelligence] += 3
        mob.perm_stat[PrimeStat.Dexterity] += 1
        mob.perm_stat[PrimeStat.Strength] -= 1
    }

    if (IS_SET(mob.off_flags, OFF_FAST)) {
        mob.perm_stat[PrimeStat.Dexterity] += 2
    }

    mob.perm_stat[PrimeStat.Strength] += mob.size - Size.Medium
    mob.perm_stat[PrimeStat.Constitution] += (mob.size - Size.Medium) / 2


    val af = Affect()
    /* let's get some spell action */
    if (IS_AFFECTED(mob, AFF_SANCTUARY)) {
        af.type = Skill.sanctuary
        af.level = mob.level
        af.duration = -1
        af.bitvector = AFF_SANCTUARY
        affect_to_char(mob, af)
    }

    if (IS_AFFECTED(mob, AFF_HASTE)) {
        af.type = Skill.haste
        af.level = mob.level
        af.duration = -1
        af.location = Apply.Dex
        af.modifier = 1 + (if (mob.level >= 18) 1 else 0) + (if (mob.level >= 25) 1 else 0) + if (mob.level >= 32) 1 else 0
        af.bitvector = AFF_HASTE
        affect_to_char(mob, af)
    }

    if (IS_AFFECTED(mob, AFF_PROTECT_EVIL)) {
        af.type = Skill.protection_evil
        af.level = mob.level
        af.duration = -1
        af.location = Apply.Saves
        af.modifier = -1
        af.bitvector = AFF_PROTECT_EVIL
        affect_to_char(mob, af)
    }

    if (IS_AFFECTED(mob, AFF_PROTECT_GOOD)) {
        af.type = Skill.protection_good
        af.level = mob.level
        af.duration = -1
        af.location = Apply.Saves
        af.modifier = -1
        af.bitvector = AFF_PROTECT_GOOD
        affect_to_char(mob, af)
    }

    mob.position = mob.start_pos

    if (mob.gold > mob.level) {
        mob.gold = dice(6, mob.level)
    }

    /* link the mob to the world list */
    Index.CHARS.add(mob)
    pMobIndex.count++
    return mob
}

/* duplicate a mobile exactly -- except inventory */

fun clone_mobile(parent: CHAR_DATA?, clone: CHAR_DATA?) {
    if (parent == null || clone == null || parent.pcdata != null) {
        return
    }

    // start fixing values
    clone.name = parent.name
    clone.short_desc = parent.short_desc
    clone.long_desc = parent.long_desc
    clone.description = parent.description
    clone.group = parent.group
    clone.sex = parent.sex
    clone.clazz = parent.clazz
    clone.race = parent.race
    clone.level = parent.level
    clone.trust = 0
    clone.timer = parent.timer
    clone.wait = parent.wait
    clone.hit = parent.hit
    clone.max_hit = parent.max_hit
    clone.mana = parent.mana
    clone.max_mana = parent.max_mana
    clone.move = parent.move
    clone.max_move = parent.max_move
    clone.gold = parent.gold
    clone.silver = parent.silver
    clone.exp = parent.exp
    clone.act = parent.act
    clone.comm = parent.comm
    clone.imm_flags = parent.imm_flags
    clone.res_flags = parent.res_flags
    clone.vuln_flags = parent.vuln_flags
    clone.invis_level = parent.invis_level
    clone.affected_by = parent.affected_by
    clone.position = parent.position
    clone.practice = parent.practice
    clone.train = parent.train
    clone.saving_throw = parent.saving_throw
    clone.alignment = parent.alignment
    clone.hitroll = parent.hitroll
    clone.damroll = parent.damroll
    clone.wimpy = parent.wimpy
    clone.form = parent.form
    clone.parts = parent.parts
    clone.size = parent.size
    clone.material = parent.material
    clone.extracted = parent.extracted
    clone.off_flags = parent.off_flags
    clone.attack = parent.attack
    clone.start_pos = parent.start_pos
    clone.default_pos = parent.default_pos
    clone.spec_fun = parent.spec_fun
    clone.progtypes = parent.progtypes
    clone.status = parent.status
    clone.hunting = null
    clone.endur = 0
    clone.in_mind = ""
    clone.cabal = Clan.None

    for (i in 0 until clone.armor.size) clone.armor[i] = parent.armor[i]
    for (i in 0 until clone.damage.size) clone.damage[i] = parent.damage[i]

    clone.perm_stat.copyFrom(parent.perm_stat)
    clone.mod_stat.copyFrom(parent.mod_stat)

    /* now add the affects */
    var paf: Affect? = parent.affected
    while (paf != null) {
        affect_to_char(clone, paf)
        paf = paf.next
    }
}

/**
 * Create an object with modifying the count
 */
fun create_object(pObjIndex: ObjProto, level: Int): Obj = create_object_org(pObjIndex, level, true)

/**
 * for player load/quit
 * Create an object and do not modify the count
 */
fun create_object_nocount(pObjIndex: ObjProto, level: Int): Obj? = create_object_org(pObjIndex, level, false)

/**
 * Create an instance of an object.
 */
fun create_object_org(pObjIndex: ObjProto?, level: Int, count: Boolean): Obj {
    if (pObjIndex == null) {
        bug("Create_object: null pObjIndex.")
        exit(1)
    }

    val obj = Obj()

    obj.pIndexData = pObjIndex
    obj.in_room = null
    obj.enchanted = false

    for (c in Clan.values()) {
        if (pObjIndex.vnum == c.obj_vnum) {
            /* todo:
                if ( count_obj_list( pObjIndex, object_list) > 0 )
                  return null
                */
            c.obj_ptr = obj
            break
        }
    }
    if (obj.pIndexData.limit != -1 && obj.pIndexData.count >= obj.pIndexData.limit) {
        obj.level = if (pObjIndex.new_format) pObjIndex.level else Math.max(0, level)
    }

    obj.name = pObjIndex.name
    obj.short_desc = pObjIndex.short_descr
    obj.description = pObjIndex.description
    obj.material = pObjIndex.material
    obj.item_type = pObjIndex.item_type
    obj.weaponType = pObjIndex.weaponType
    obj.extra_flags = pObjIndex.extra_flags
    obj.wear_flags = pObjIndex.wear_flags
    obj.value[0] = pObjIndex.value[0]
    obj.value[1] = pObjIndex.value[1]
    obj.value[2] = pObjIndex.value[2]
    obj.value[3] = pObjIndex.value[3]
    obj.value[4] = pObjIndex.value[4]
    obj.weight = pObjIndex.weight
    obj.extracted = false
    obj.progtypes = pObjIndex.progtypes
    obj.from = "" /* used with body parts */
    obj.pit = OBJ_VNUM_PIT /* default for corpse decaying */
    obj.altar = ROOM_VNUM_ALTAR /* default for corpses */
    obj.condition = pObjIndex.condition

    obj.cost = when {
        level == -1 || pObjIndex.new_format -> pObjIndex.cost
        else -> number_fuzzy(10) * number_fuzzy(level) * number_fuzzy(level)
    }

    // Mess with object properties.
    when (obj.item_type) {
        ItemType.Light -> if (obj.value[2] == 999L) obj.value[2] = -1
        ItemType.Furniture,
        ItemType.Trash,
        ItemType.Container,
        ItemType.DrinkContainer,
        ItemType.Key,
        ItemType.Food,
        ItemType.Boat,
        ItemType.CorpseNpc,
        ItemType.CorpsePc,
        ItemType.Fountain,
        ItemType.Map,
        ItemType.Clothing,
        ItemType.Portal -> if (!pObjIndex.new_format) obj.cost /= 5

        ItemType.Treasure,
        ItemType.WarpStone,
        ItemType.RoomKey,
        ItemType.Gem,
        ItemType.Jewelry,
        ItemType.Tattoo -> {
        }

        ItemType.Jukebox -> {
            var j = 0
            while (j < 5) {
                obj.value[j] = -1L
                j++
            }
        }

        ItemType.Scroll -> if (level != -1 && !pObjIndex.new_format) obj.value[0] = number_fuzzy(obj.value[0].toInt()).toLong()

        ItemType.Wand, ItemType.Staff -> {
            if (level != -1 && !pObjIndex.new_format) {
                obj.value[0] = number_fuzzy(obj.value[0].toInt()).toLong()
                obj.value[1] = number_fuzzy(obj.value[1].toInt()).toLong()
                obj.value[2] = obj.value[1]
            }
            if (!pObjIndex.new_format) {
                obj.cost *= 2
            }
        }

        ItemType.Weapon -> if (level != -1 && !pObjIndex.new_format) {
            obj.value[1] = number_fuzzy(number_fuzzy(level / 4 + 2)).toLong()
            obj.value[2] = number_fuzzy(number_fuzzy(3 * level / 4 + 6)).toLong()
        }

        ItemType.Armor -> if (level != -1 && !pObjIndex.new_format) {
            obj.value[0] = number_fuzzy(level / 5 + 3).toLong()
            obj.value[1] = number_fuzzy(level / 5 + 3).toLong()
            obj.value[2] = number_fuzzy(level / 5 + 3).toLong()
        }

        ItemType.Potion, ItemType.Pill -> if (level != -1 && !pObjIndex.new_format) {
            obj.value[0] = number_fuzzy(number_fuzzy(obj.value[0].toInt())).toLong()
        }

        ItemType.Money -> if (!pObjIndex.new_format) obj.value[0] = obj.cost.toLong()
        else -> bug("Read_object: vnum %d bad type.", pObjIndex.vnum)
    }

    var paf = pObjIndex.affected
    while (paf != null) {
        if (paf.location == Apply.SpellAffect) {
            affect_to_obj(obj, paf)
        }
        paf = paf.next
    }
    Index.OBJECTS.add(obj)
    if (count) {
        pObjIndex.count++
    }
    return obj
}

/**
 * Duplicate an object exactly -- except contents
 */
fun clone_object(parent: Obj?, clone: Obj?) {
    if (parent == null || clone == null) {
        return
    }

    /* start fixing the object */
    clone.name = parent.name
    clone.short_desc = parent.short_desc
    clone.description = parent.description
    clone.item_type = parent.item_type
    clone.weaponType = parent.weaponType
    clone.extra_flags = parent.extra_flags
    clone.wear_flags = parent.wear_flags
    clone.weight = parent.weight
    clone.cost = parent.cost
    clone.level = parent.level
    clone.condition = parent.condition
    clone.material = parent.material
    clone.timer = parent.timer
    clone.from = parent.from
    clone.extracted = parent.extracted
    clone.pit = parent.pit
    clone.altar = parent.altar

    for (i in 0 until clone.value.size) clone.value[i] = parent.value[i]

    // affects
    clone.enchanted = parent.enchanted

    var paf = parent.affected
    while (paf != null) {
        affect_to_obj(clone, paf)
        paf = paf.next
    }

    /* extended desc */
    var ed: EXTRA_DESC_DATA? = parent.extra_desc
    while (ed != null) {
        val ed_new = EXTRA_DESC_DATA()
        ed_new.keyword = ed.keyword
        ed_new.description = ed.description
        ed_new.next = clone.extra_desc
        clone.extra_desc = ed_new
        ed = ed.next
    }
}

/**
 * Get an extra description from a list.
 */
fun get_extra_desc(name: String, edArg: EXTRA_DESC_DATA?): String? {
    var ed = edArg
    while (ed != null) {
        if (is_name(name, ed.keyword)) {
            return ed.description
        }
        ed = ed.next
    }
    return null
}

/**
 * Translates mob virtual number to its mob index struct.
 * Hash table lookupRace.
 */
fun get_mob_index_nn(vnum: Int): MOB_INDEX_DATA = get_mob_index(vnum) ?: throw IllegalArgumentException("Mob not found: $vnum")

fun get_mob_index(vnum: VNum): MOB_INDEX_DATA? = Index.MOB_INDEX[vnum]

/**
 * Translates mob virtual number to its obj index struct.
 * Hash table lookupRace.
 */
fun get_obj_index_nn(vnum: Int): ObjProto = get_obj_index(vnum) ?: throw IllegalArgumentException("Object not found: $vnum")

fun get_obj_index(vnum: Int): ObjProto? = Index.OBJ_INDEX[vnum]

/**
 * Translates mob virtual number to its room index struct.
 * Hash table lookupRace.
 */
fun get_room_index_nn(vnum: Int): ROOM_INDEX_DATA = get_room_index(vnum) ?: throw IllegalArgumentException("Room not found: $vnum")

fun get_room_index(vnum: Int): ROOM_INDEX_DATA? = Index.ROOM_INDEX[vnum]


/** Reports a bug and exits */
internal fun bugf(fp: DikuTextFile, str: String, vararg params: Any?): Nothing {
    bug(str + "\n" + fp.buildCurrentStateInfo(), params)
    exit(1)
}

private fun load_olimits(fp: DikuTextFile) {
    var ch = fp.fread_letter()
    while (ch != 'S') {
        when (ch) {
            'O' -> {
                val vnum = fp.int()
                val limit = fp.int()
                val pIndex = get_obj_index(vnum) ?: bugf(fp, "Load_olimits: bad vnum %d", vnum)
                pIndex.limit = limit
            }
            '*' -> fp.fread_to_eol()
            else -> bugf(fp, "Load_olimits: bad command '%c'", ch)
        }
        ch = fp.fread_letter()
    }
}

/**
 * Add the objects in players not logged on to object count
 */
private fun load_limited_objects() {
    val dir = File(nw_config.lib_player_dir)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val files = dir.listFiles()
    if (!dir.exists() || !dir.isDirectory || files == null) {
        bug("Load_limited_objects: unable to open player directory:" + dir.absolutePath, 0)
        exit(1)
    }
    for (i in 0 until files.size) {
        val file = files[i]
        if (file.name.length < 3 || file.isDirectory) {
            continue
        }
        try {
            val fp = DikuTextFile(file)
            var fReadLevel = false
            var tplayed = 0
            var letter = fp.fread_letter()
            loop@ while (!fp.feof()) {
                when (letter) {
                    'L' -> if (!fReadLevel) {
                        val word = fp.word()
                        if (eq(word, "evl") || eq(word, "ev") || eq(word, "evel")) {
                            val l = fp.int()
                            fReadLevel = true
                            total_levels += Math.max(0, l - 5)
                            log_string("[" + file.name + "]'s file +:" + Math.max(0, l - 5))
                        }
                    }
                    'P' -> {
                        val word = fp.word()
                        if (eq(word, "layLog")) {
                            fp.int()    /* read the version */
                            while (true) {
                                val d = fp.int()
                                if (d < 0) {
                                    break
                                }
                                val t = fp.int()
                                val d_start = get_played_day(nw_config.max_time_log - 1)
                                val d_stop = get_played_day(0)

                                if (d in d_start..d_stop) {
                                    tplayed += t
                                }
                            }
                        }
                    }
                    '#' -> {
                        val word = fp.word()
                        if (eq(word, "O") || eq(word, "OBJECT")) {
                            if (tplayed < nw_config.min_time_limit) {
                                log_string("Discarding the player " + file.name + "'s limited equipments.")
                                break@loop
                            }

                            fp.word()
                            isLoading = false
                            val vnum = fp.int()
                            val obj_index = get_obj_index(vnum)
                            if (obj_index != null) {
                                obj_index.count++
                            }
                            isLoading = true
                        }
                    }
                    else -> fp.fread_to_eol()
                }
                letter = fp.fread_letter()
            }
        } catch (e: IOException) {
            bug("Load_limited_objects: Can't open player file: " + file.absolutePath, 0)
        }
    }
}

private fun load_practicer(fp: DikuTextFile) {
    while (true) {
        val pMobIndex: MOB_INDEX_DATA
        val letter = fp.fread_letter()
        when (letter) {
            'S' -> return
            '*' -> {
            }
            'M' -> {
                pMobIndex = get_mob_index_nn(fp.int())
                val sg = SG.lookup(fp.word()) ?: SG.None
                pMobIndex.practicer = sg
                if (sg === SG.None) bugf(fp, "Load_practicers: 'M': vnum ${pMobIndex.vnum}.")
            }
            else -> bugf(fp, "Load_specials: letter '$letter' not *MS.")
        }
        fp.fread_to_eol()
    }
}

/** socials handling ported from SOG codebase, author: fjoe */
private fun load_socials() {
    val fp = DikuTextFile("etc" + "/" + "socials.conf")
    var social = Social()
    while (true) {
        val word = fp.word()
        if (word.isEmpty()) {
            break
        }
        if (word[0] == '#') {
            if (word.length > 1 && word[1] == '$') {
                break
            }
            continue
        }
        when (word) {
            "end" -> {
                if (!social.name.isEmpty()) {
                    Index.SOCIALS.add(social)
                } else {
                    bugf(fp, "social without name!")
                }
                social = Social()
            }
            "found_char" -> social.found_char = fp.string()
            "found_vict" -> social.found_victim = fp.string()
            "found_notvict" -> social.found_novictim = fp.string()
            "name" -> social.name = fp.word()
            "notfound_char" -> social.not_found_char = fp.string()
            "noarg_char" -> social.noarg_char = fp.string()
            "noarg_room" -> social.noarg_room = fp.string()
            "min_pos" -> social.minPos = Pos.of(fp.word()) { bugf(fp, "pos: $it") }
            "self_char" -> social.self_char = fp.string()
            "self_room" -> social.self_room = fp.string()
            else -> bugf(fp, "Loading socials: unknown keyword: $word")
        }
    }
}

private fun load_mobiles(fp: DikuTextFile) {
    while (true) {
        var letter = fp.fread_letter()
        if (letter != '#') {
            bug("Load_mobiles: # not found.")
            exit(1)
        }

        val vnum = fp.int()
        if (vnum == 0) {
            break
        }

        isLoading = false
        if (get_mob_index(vnum) != null) {
            bug("Load_mobiles: vnum %d duplicated.", vnum)
            exit(1)
        }
        isLoading = true

        val pMobIndex = MOB_INDEX_DATA()
        pMobIndex.vnum = vnum
        pMobIndex.player_name = fp.string()
        pMobIndex.short_descr = fp.string()
        pMobIndex.long_descr = upfirst(fp.string())
        pMobIndex.description = upfirst(fp.string())
        pMobIndex.race = Race.lookupNN(fp.string())
        pMobIndex.act = fp.long() or ACT_IS_NPC or pMobIndex.race.act
        pMobIndex.affected_by = fp.long() or pMobIndex.race.aff
        pMobIndex.affected_by = REMOVE_BIT(pMobIndex.affected_by, C or D or E or F or G or Z or BIT_31)
        pMobIndex.pShop = null
        pMobIndex.alignment = fp.int()
        pMobIndex.group = fp.int()
        pMobIndex.level = fp.int()
        pMobIndex.hitroll = fp.int()

        /* read hit dice */
        pMobIndex.hit[DICE_NUMBER] = fp.int()
        /* 'd'          */
        fp.fread_letter()
        pMobIndex.hit[DICE_TYPE] = fp.int()
        /* '+'          */
        fp.fread_letter()
        pMobIndex.hit[DICE_BONUS] = fp.int()

        /* read mana dice */
        pMobIndex.mana[DICE_NUMBER] = fp.int()
        fp.fread_letter()
        pMobIndex.mana[DICE_TYPE] = fp.int()
        fp.fread_letter()
        pMobIndex.mana[DICE_BONUS] = fp.int()

        /* read damage dice */
        pMobIndex.damage[DICE_NUMBER] = fp.int()
        fp.fread_letter()
        pMobIndex.damage[DICE_TYPE] = fp.int()
        fp.fread_letter()
        pMobIndex.damage[DICE_BONUS] = fp.int()
        pMobIndex.attack = AttackType.of(fp.word()) { bugf(fp, "attack: $it") }

        /* read armor class */
        pMobIndex.ac[AC_PIERCE] = fp.int() * 10
        pMobIndex.ac[AC_BASH] = fp.int() * 10
        pMobIndex.ac[AC_SLASH] = fp.int() * 10
        pMobIndex.ac[AC_EXOTIC] = fp.int() * 10

        /* read flags and add in data from the race table */
        pMobIndex.off_flags = fp.long() or pMobIndex.race.off
        pMobIndex.imm_flags = fp.long() or pMobIndex.race.imm
        pMobIndex.res_flags = fp.long() or pMobIndex.race.res
        pMobIndex.vuln_flags = fp.long() or pMobIndex.race.vuln

        /* vital statistics */
        pMobIndex.start_pos = Pos.of(fp.word()) { bugf(fp, "pos: $it") }
        pMobIndex.default_pos = Pos.of(fp.word()) { bugf(fp, "pos: $it") }
        val sexStr = fp.word()
        pMobIndex.sexOption = if (sexStr == "either") 3 else Sex.of(sexStr) { bugf(fp, "sex: $sexStr") }.ordinal

        pMobIndex.wealth = fp.int()

        pMobIndex.form = fp.long() or pMobIndex.race.form
        pMobIndex.parts = fp.long() or pMobIndex.race.parts
        pMobIndex.size = Size.of(fp.word()) { bugf(fp, "param: $it") }!!
        pMobIndex.material = fp.word()
        pMobIndex.progtypes = 0

        while (true) {
            letter = fp.fread_letter()

            if (letter == 'F') {
                val word = fp.word()
                val vector = fp.long()

                when {
                    startsWith(word, "act") -> pMobIndex.act = REMOVE_BIT(pMobIndex.act, vector)
                    startsWith(word, "aff") -> pMobIndex.affected_by = REMOVE_BIT(pMobIndex.affected_by, vector)
                    startsWith(word, "off") -> pMobIndex.affected_by = REMOVE_BIT(pMobIndex.affected_by, vector)
                    startsWith(word, "imm") -> pMobIndex.imm_flags = REMOVE_BIT(pMobIndex.imm_flags, vector)
                    startsWith(word, "res") -> pMobIndex.res_flags = REMOVE_BIT(pMobIndex.res_flags, vector)
                    startsWith(word, "vul") -> pMobIndex.vuln_flags = REMOVE_BIT(pMobIndex.vuln_flags, vector)
                    startsWith(word, "for") -> pMobIndex.form = REMOVE_BIT(pMobIndex.form, vector)
                    startsWith(word, "par") -> pMobIndex.parts = REMOVE_BIT(pMobIndex.parts, vector)
                    else -> bugf(fp, "Flag remove: flag not found.")
                }
            } else {
                fp.ungetc()
                break
            }
        }

        Index.MOB_INDEX[vnum] = pMobIndex
        val kill_data = kill_table[URANGE(0, pMobIndex.level, MAX_LEVEL - 1)]
        kill_data.number++
    }

}

private fun load_objects(fp: DikuTextFile) {
    while (true) {
        var letter = fp.fread_letter()
        if (letter != '#') {
            bugf(fp, "Load_objects: # not found.")
        }

        val vnum = fp.int()
        if (vnum == 0) {
            break
        }

        if (get_obj_index(vnum) != null) {
            bugf(fp, "Load_objects: vnum %d duplicated.", vnum)
        }

        val pObj = ObjProto()
        pObj.vnum = vnum
        pObj.new_format = true
        pObj.reset_num = 0
        pObj.name = fp.string()
        pObj.short_descr = fp.string()
        pObj.description = fp.string()
        pObj.material = fp.string()

        pObj.item_type = ItemType.of(fp.word()) { bugf(fp, "itemType: $it") }
        pObj.extra_flags = fp.long()
        pObj.wear_flags = fp.long()
        when (pObj.item_type) {
            ItemType.Weapon -> {
                pObj.weaponType = WeaponType.of(fp.word()) { bugf(fp, "weapon: $it") }
                pObj.value[1] = fp.long()
                pObj.value[2] = fp.long()
                pObj.value[3] = AttackType.of(fp.word()) { bugf(fp, "attack: $it") }.ordinal.toLong()
                pObj.value[4] = fp.long()
            }
            ItemType.Container -> {
                pObj.value[0] = fp.long()
                pObj.value[1] = fp.long()
                pObj.value[2] = fp.long()
                pObj.value[3] = fp.long()
                pObj.value[4] = fp.long()
            }
            ItemType.DrinkContainer, ItemType.Fountain -> {
                pObj.value[0] = fp.long()
                pObj.value[1] = fp.long()
                pObj.value[2] = Liquid.lookup(fp.word()).ordinal.toLong()
                pObj.value[3] = fp.long()
                pObj.value[4] = fp.long()
            }
            ItemType.Wand, ItemType.Staff -> {
                pObj.value[0] = fp.long()
                pObj.value[1] = fp.long()
                pObj.value[2] = fp.long()
                pObj.value[3] = Skill.skill_num_lookup(fp.word()).toLong()
                pObj.value[4] = fp.long()
            }
            ItemType.Potion, ItemType.Pill, ItemType.Scroll -> {
                pObj.value[0] = fp.long()
                pObj.value[1] = Skill.skill_num_lookup(fp.word()).toLong()
                pObj.value[2] = Skill.skill_num_lookup(fp.word()).toLong()
                pObj.value[3] = Skill.skill_num_lookup(fp.word()).toLong()
                pObj.value[4] = Skill.skill_num_lookup(fp.word()).toLong()
            }
            else -> {
                pObj.value[0] = fp.long()
                pObj.value[1] = fp.long()
                pObj.value[2] = fp.long()
                pObj.value[3] = fp.long()
                pObj.value[4] = fp.long()
            }
        }
        pObj.level = fp.int()
        pObj.weight = fp.int()
        pObj.cost = fp.int()
        pObj.progtypes = 0
        pObj.limit = -1

        /* condition */
        letter = fp.fread_letter()
        pObj.condition = when (letter) {
            'P' -> 100
            'G' -> 90
            'A' -> 75
            'W' -> 50
            'D' -> 25
            'B' -> 10
            'R' -> 0
            else -> 100
        }

        while (true) {
            letter = fp.fread_letter()
            if (letter == 'A') {
                val paf = Affect()
                paf.where = TO_OBJECT
                paf.type = Skill.reserved
                paf.level = pObj.level
                paf.duration = -1
                paf.location = Affect.of(fp.int()) { bugf(fp, "affect: $it") }
                paf.modifier = fp.int()
                paf.next = pObj.affected
                pObj.affected = paf
                top_affect++
            } else if (letter == 'F') {
                val paf = Affect()
                letter = fp.fread_letter()
                paf.where = when (letter) {
                    'A' -> TO_AFFECTS
                    'I' -> TO_IMMUNE
                    'R' -> TO_RESIST
                    'V' -> TO_VULN
                    'D' -> TO_AFFECTS
                    else -> bugf(fp, "Load_objects: Bad where on flag set.")
                }
                paf.type = Skill.reserved
                paf.level = pObj.level
                paf.duration = -1
                paf.location = Affect.of(fp.int()) { bugf(fp, "affect: $it") }
                paf.modifier = fp.int()
                paf.bitvector = fp.long()
                paf.next = pObj.affected
                pObj.affected = paf
                top_affect++
            } else if (letter == 'E') {
                val ed = EXTRA_DESC_DATA()
                ed.keyword = fp.string()
                ed.description = fp.string()
                ed.next = pObj.extra_descr
                pObj.extra_descr = ed
                top_ed++
            } else {
                fp.ungetc()
                break
            }
        }
        Index.OBJ_INDEX[vnum] = pObj
    }
}

private fun load_omprogs(fp: DikuTextFile) {
    var progtype: String
    var progname: String
    while (true) {
        val pMobIndex: MOB_INDEX_DATA
        val pObjIndex: ObjProto
        val letter = fp.fread_letter()
        when (letter) {
            'S' -> return
            '*' -> {
            }
            'O' -> {
                pObjIndex = get_obj_index_nn(fp.int())
                progtype = fp.word()
                progname = fp.word()
                assign_obj_prog(pObjIndex, progtype, progname)
            }

            'M' -> {
                pMobIndex = get_mob_index_nn(fp.int())
                progtype = fp.word()
                progname = fp.word()
                assign_mob_prog(pMobIndex, progtype, progname)
            }
            else -> bugf(fp, "Load_omprogs: letter '$letter' not *IMS.")
        }
        fp.fread_to_eol()
    }
}

/**
 * Lookup a skill by slot number.
 * Used for object loading.
 */
private fun slot_lookup_skill_num(slot: Int): Int {
    if (slot <= 0) {
        return -1
    }
    Skill.skills
            .filter { slot == it.slot }
            .forEach { return it.ordinal }

    if (isLoading) {
        bug("Slot_lookup: bad slot: $slot.")
        exit(1)
    }
    return -1
}
