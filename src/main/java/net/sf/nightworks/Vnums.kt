package net.sf.nightworks

typealias VNum = Int
/**
 * ************************************************************************
 * *
 * VALUES OF INTEREST TO AREA BUILDERS                   *
 * (Start of section ... start here)                     *
 * *
 * *************************************************************************
 */

/*
* Well known mob virtual numbers.
* Defined in #MOBILES.
*/
const val MOB_VNUM_FIDO = 3062
const val MOB_VNUM_SAGE = 3162
const val MOB_VNUM_SHADOW = 10
const val MOB_VNUM_SPECIAL_GUARD = 11
const val MOB_VNUM_BEAR = 12
const val MOB_VNUM_DEMON = 13
const val MOB_VNUM_NIGHTWALKER = 14
const val MOB_VNUM_STALKER = 15
const val MOB_VNUM_SQUIRE = 16
const val MOB_VNUM_MIRROR_IMAGE = 17
const val MOB_VNUM_UNDEAD = 18
const val MOB_VNUM_LION = 19
const val MOB_VNUM_WOLF = 20
const val MOB_VNUM_LESSER_GOLEM = 21
const val MOB_VNUM_STONE_GOLEM = 22
const val MOB_VNUM_IRON_GOLEM = 23
const val MOB_VNUM_ADAMANTITE_GOLEM = 24
const val MOB_VNUM_HUNTER = 25
const val MOB_VNUM_SUM_SHADOW = 26
const val MOB_VNUM_DOG = 27
const val MOB_VNUM_ELM_EARTH = 28
const val MOB_VNUM_ELM_AIR = 29
const val MOB_VNUM_ELM_FIRE = 30
const val MOB_VNUM_ELM_WATER = 31
const val MOB_VNUM_ELM_LIGHT = 32
const val MOB_VNUM_WEAPON = 33
const val MOB_VNUM_ARMOR = 34
const val MOB_VNUM_PATROLMAN = 2106
const val GROUP_VNUM_TROLLS = 2100
const val GROUP_VNUM_OGRES = 2101
/*
* Well known object virtual numbers.
* Defined in #OBJECTS.
*/
const val OBJ_VNUM_SILVER_ONE = 1
const val OBJ_VNUM_GOLD_ONE = 2
const val OBJ_VNUM_GOLD_SOME = 3
const val OBJ_VNUM_SILVER_SOME = 4
const val OBJ_VNUM_COINS = 5
const val OBJ_VNUM_CORPSE_NPC = 10
const val OBJ_VNUM_CORPSE_PC = 11
const val OBJ_VNUM_SEVERED_HEAD = 12
const val OBJ_VNUM_TORN_HEART = 13
const val OBJ_VNUM_SLICED_ARM = 14
const val OBJ_VNUM_SLICED_LEG = 15
const val OBJ_VNUM_GUTS = 16
const val OBJ_VNUM_BRAINS = 17
const val OBJ_VNUM_GRAVE_STONE = 19
const val OBJ_VNUM_MUSHROOM = 20
const val OBJ_VNUM_LIGHT_BALL = 21
const val OBJ_VNUM_SPRING = 22
const val OBJ_VNUM_DISC = 23
const val OBJ_VNUM_PORTAL = 25
const val OBJ_VNUM_ROSE = 1001
const val OBJ_VNUM_PIT = 3010
const val OBJ_VNUM_SCHOOL_VEST = 3703
const val OBJ_VNUM_SCHOOL_SHIELD = 3704
const val OBJ_VNUM_SCHOOL_BANNER = 3716
const val OBJ_VNUM_MAP = 3162
const val OBJ_VNUM_NMAP1 = 3385
const val OBJ_VNUM_NMAP2 = 3386
const val OBJ_VNUM_MAP_NEW_THALOS = 3167
const val OBJ_VNUM_MAP_NEW_OFCOL = 3162
const val OBJ_VNUM_MAP_MIDGAARD = 3164
const val OBJ_VNUM_MAP_TITAN = 3382
const val OBJ_VNUM_MAP_OLD_MIDGAARD = 5333
const val OBJ_VNUM_POTION_VIAL = 42
const val OBJ_VNUM_STEAK = 27
const val OBJ_VNUM_RANGER_STAFF = 28
const val OBJ_VNUM_RANGER_ARROW = 6
const val OBJ_VNUM_RANGER_BOW = 7
const val OBJ_VNUM_DEPUTY_BADGE = 70
const val OBJ_VNUM_RULER_BADGE = 70
const val OBJ_VNUM_RULER_SHIELD1 = 71
const val OBJ_VNUM_RULER_SHIELD2 = 72
const val OBJ_VNUM_RULER_SHIELD3 = 73
const val OBJ_VNUM_RULER_SHIELD4 = 74
const val OBJ_VNUM_LION_SHIELD = 31
const val OBJ_VNUM_CHAOS_BLADE = 87
const val OBJ_VNUM_DRAGONDAGGER = 80
const val OBJ_VNUM_DRAGONMACE = 81
const val OBJ_VNUM_PLATE = 82
const val OBJ_VNUM_DRAGONSWORD = 83
const val OBJ_VNUM_DRAGONLANCE = 99
const val OBJ_VNUM_BATTLE_PONCHO = 26
const val OBJ_VNUM_BATTLE_THRONE = 542
const val OBJ_VNUM_SHALAFI_ALTAR = 530
const val OBJ_VNUM_CHAOS_ALTAR = 551
const val OBJ_VNUM_INVADER_SKULL = 560
const val OBJ_VNUM_KNIGHT_ALTAR = 521
const val OBJ_VNUM_RULER_STAND = 510
const val OBJ_VNUM_LIONS_ALTAR = 501
const val OBJ_VNUM_HUNTER_ALTAR = 570
const val OBJ_VNUM_POTION_SILVER = 43
const val OBJ_VNUM_POTION_GOLDEN = 44
const val OBJ_VNUM_POTION_SWIRLING = 45
const val OBJ_VNUM_KATANA_SWORD = 98
const val OBJ_VNUM_EYED_SWORD = 88
const val OBJ_VNUM_FIRE_SHIELD = 92
const val OBJ_VNUM_MAGIC_JAR = 93
const val OBJ_VNUM_HAMMER = 6522
const val OBJ_VNUM_CHUNK_IRON = 6521
/* vnums for tattoos */
const val OBJ_VNUM_TATTOO_APOLLON = 51
const val OBJ_VNUM_TATTOO_ZEUS = 52
const val OBJ_VNUM_TATTOO_SIEBELE = 53
const val OBJ_VNUM_TATTOO_HEPHAESTUS = 54
const val OBJ_VNUM_TATTOO_EHRUMEN = 55
const val OBJ_VNUM_TATTOO_AHRUMAZDA = 56
const val OBJ_VNUM_TATTOO_DEIMOS = 57
const val OBJ_VNUM_TATTOO_PHOBOS = 58
const val OBJ_VNUM_TATTOO_ODIN = 59
const val OBJ_VNUM_TATTOO_MARS = 60
const val OBJ_VNUM_TATTOO_ATHENA = 61
const val OBJ_VNUM_TATTOO_GOKTENGRI = 62
const val OBJ_VNUM_TATTOO_HERA = 63
const val OBJ_VNUM_TATTOO_VENUS = 64
const val OBJ_VNUM_TATTOO_ARES = 65
const val OBJ_VNUM_TATTOO_PROMETHEUS = 66
const val OBJ_VNUM_TATTOO_EROS = 67
/** Exit exists (there is a description) but path is not available */
const val ROOM_VNUM_BLOCKED_EXIT = -1
const val ROOM_VNUM_VOID = 1
const val ROOM_VNUM_LIMBO = 2
const val ROOM_VNUM_TEMPLE = 3001
const val ROOM_VNUM_ALTAR = 3054
const val ROOM_VNUM_SCHOOL = 3700
const val ROOM_VNUM_BATTLE = 541


/* quest rewards */
const val OBJ_VNUM_QUEST_ITEM1 = 94
const val OBJ_VNUM_QUEST_ITEM2 = 95
const val OBJ_VNUM_QUEST_ITEM3 = 96
const val OBJ_VNUM_QUEST_ITEM4 = 30
const val OBJ_VNUM_QUEST_ITEM5 = 29
