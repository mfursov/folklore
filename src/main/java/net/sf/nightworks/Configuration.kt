package net.sf.nightworks

class Configuration
/*
* Data files used by the server.
*
* AREA_LIST contains a list of areas to boot.
* All files are read in completely at bootup.
* Most output files (bug, idea, typo, shutdown) are append-only.
*
* The null_FILE is held open so that we have a stream handle in reserve,
*   so players can go ahead and telnet to all the other descriptors.
* Then we close it whenever we need to open a file (e.g. a save file).
*/
internal constructor() {
    var port_num: Int = 0

    var lib_area_dir: String
    var lib_races_dir: String
    var lib_classes_dir: String
    var lib_player_dir: String
    var lib_god_dir: String

    var pl_temp_file: String

    var etc_area_list: String
    var etc_races_list: String
    var etc_classes_list: String

    var var_astat_file: String

    var note_bug_file: String
    var note_typo_file: String
    var note_note_file: String
    var note_idea_file: String
    var note_news_file: String
    var note_penalty_file: String
    var note_changes_file: String

    var max_alias: Int = 0
    var max_time_log: Int = 0
    var min_time_limit: Int = 0
    init {
        //TODO: read from system properties, use hardcoded values as default
        port_num = 4000

        lib_area_dir = "./lib/areas"
        lib_races_dir = "./lib/races"
        lib_classes_dir = "./lib/classes"
        lib_player_dir = "./lib/players"
        lib_god_dir = "./lib/gods"

        pl_temp_file = "./lib/players/player.tmp"

        var_astat_file = "./var/area_stat.txt"

        etc_area_list = "./etc/areas.list"
        etc_races_list = "./etc/races.list"
        etc_classes_list = "./etc/classes.list"

        note_bug_file = "./lib/notes/bugs.txt"
        note_typo_file = "./lib/notes/typos.txt"
        note_note_file = "./lib/notes/notes.not"
        note_idea_file = "./lib/notes/ideas.not"
        note_news_file = "./lib/notes/news.not"
        note_penalty_file = "./lib/notes/penalty.not"
        note_changes_file = "./lib/notes/changes.not"

        max_alias = 20
        max_time_log = 14
        min_time_limit = 600
    }
}


