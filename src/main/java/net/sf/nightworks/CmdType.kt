package net.sf.nightworks

import net.sf.nightworks.act.comm.do_delete
import net.sf.nightworks.act.comm.do_group
import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.act.db.do_areas
import net.sf.nightworks.act.info.do_help
import net.sf.nightworks.act.db.do_dump
import net.sf.nightworks.act.db.do_memory
import net.sf.nightworks.act.info.do_compare
import net.sf.nightworks.act.info.do_exits
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.act.info.do_scan
import net.sf.nightworks.act.info.do_score
import net.sf.nightworks.act.info.do_where
import net.sf.nightworks.act.info.do_who
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Pos

typealias DoFun = (ch: CHAR_DATA, arg: String) -> Unit
typealias DoFun0 = () -> Unit
typealias DoFun1 = (ch: CHAR_DATA) -> Unit

enum class CmdType constructor(
        val names: Array<String>,
        val do_fun: DoFun,
        val position: Pos,
        val level: Int,
        val log: Int,
        val extra: Int) {

    /*
    * Common movement commands.
    */
    do_north("north", ::do_north, Pos.Standing, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),
    do_east("east", ::do_east, Pos.Standing, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),
    do_south("south", ::do_south, Pos.Standing, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),
    do_west("west", ::do_west, Pos.Standing, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),
    do_up("up", ::do_up, Pos.Standing, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),
    do_down("down", ::do_down, Pos.Standing, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),

    /*
    * Common other commands.
    * Placed here so one and two letter abbreviations work.
    */
    do_at("at", ::do_at, Pos.Dead, L6, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_cast("cast", ::do_cast, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_claw("claw", ::do_claw, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_crecall("crecall", ::do_crecall, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_auction("auction", ::do_auction, Pos.Sleeping, 0, LOG_NORMAL, CMD_GHOST),
    do_buy("buy", ::do_buy, Pos.Resting, 0, LOG_NORMAL, 0),
    do_channels("channels", ::do_channels, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_exits("exits", ::do_exits, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_estimate("estimate", ::do_estimate, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),

    do_goto("goto", ::do_goto, Pos.Dead, L8, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_glist("glist", ::do_glist, Pos.Dead, 0, LOG_NEVER, 0),
    do_group("group", ::do_group, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_inventory("inventory", ::do_inventory, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_kill(arrayOf("kill", "hit"), ::do_kill, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_look("look", ::do_look, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_order("order", ::do_order, Pos.Resting, 0, LOG_NORMAL, 0),
    do_pracnew("practice", ::do_pracnew, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_practice("prac_old", ::do_practice, Pos.Sleeping, ML, LOG_NORMAL, CMD_KEEP_HIDE),
    do_rest("rest", ::do_rest, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_repair("repair", ::do_repair, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_sit("sit", ::do_sit, Pos.Sleeping, 0, LOG_NORMAL, 0),
    do_smithing("smithing", ::do_smithing, Pos.Resting, 0, LOG_NORMAL, 0),
    do_stand("stand", ::do_stand, Pos.Sleeping, 0, LOG_NORMAL, 0),
    do_tell("tell", ::do_tell, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),
    do_unlock("unlock", ::do_unlock, Pos.Resting, 0, LOG_NORMAL, 0),
    do_wizhelp("wizhelp", ::do_wizhelp, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),

    /*
    * Informational commands.
    */
    do_affects("affects", ::do_affects, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_areas("areas", ::do_areas, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_balance("balance", ::do_balance, Pos.Standing, 0, LOG_NORMAL, 0),
    do_changes("changes", ::do_changes, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_commands("commands", ::do_commands, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_compare("compare", ::do_compare, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_consider("consider", ::do_consider, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_concentrate("concentrate", ::do_concentrate, Pos.Standing, 0, LOG_NORMAL, 0),
    do_count("count", ::do_count, Pos.Sleeping, HE, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_credits("credits", ::do_credits, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_deposit("deposit", ::do_deposit, Pos.Standing, 0, LOG_NORMAL, 0),
    do_equipment("equipment", ::do_equipment, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_escape("escape", ::do_escape, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_examine("examine", ::do_examine, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),
    do_help("help", ::do_help, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_idea("idea", ::do_idea, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_motd("motd", ::do_motd, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_news("news", ::do_news, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_raffects("raffects", ::do_raffects, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_read("read", ::do_read, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_report("report", ::do_report, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_rules("rules", ::do_rules, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_scan("scan", ::do_scan, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_nscore("score", ::do_nscore, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_skills("skills", ::do_skills, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_speak("speak", ::do_speak, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_socials("socials", ::do_socials, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_show("show", ::do_show, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_spells("spells", ::do_spells, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_time("time", ::do_time, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_weather("weather", ::do_weather, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_who("who", ::do_who, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_whois("whois", ::do_whois, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_withdraw("withdraw", ::do_withdraw, Pos.Standing, 0, LOG_NORMAL, 0),
    do_wizlist("wizlist", ::do_wizlist, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_worth("worth", ::do_worth, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),

    /*
    * Configuration commands.
    */
    do_alia("alia", ::do_alia, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_alias("alias", ::do_alias, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_clear("clear", ::do_clear, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    // do_clear("cls",  ActInfo::do_clear, Pos.Dead, 0, LOG_NORMAL, 1, CMD_KEEP_HIDE | CMD_GHOST),
    do_autolist("autolist", ::do_autolist, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_color("color", ::do_color, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_autoassist("autoassist", ::do_autoassist, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_autoexit("autoexit", ::do_autoexit, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_autogold("autogold", ::do_autogold, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_autoloot("autoloot", ::do_autoloot, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_autosac("autosac", ::do_autosac, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_autosplit("autosplit", ::do_autosplit, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_brief("brief", ::do_brief, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_combine("combine", ::do_combine, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_compact("compact", ::do_compact, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_description("description", ::do_description, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_delet("delet", ::do_delet, Pos.Dead, 0, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_delete("delete", ::do_delete, Pos.Standing, 0, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_identify("identify", ::do_identify, Pos.Standing, 0, LOG_NORMAL, CMD_GHOST),
    do_nocancel("nocancel", ::do_nocancel, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_nofollow("nofollow", ::do_nofollow, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_noloot("noloot", ::do_noloot, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_nosummon("nosummon", ::do_nosummon, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_outfit("outfit", ::do_outfit, Pos.Resting, 0, LOG_NORMAL, 0),
    do_tick("tick", ::do_tick, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_password("password", ::do_password, Pos.Dead, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),
    do_prompt("prompt", ::do_prompt, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_quest("quest", ::do_quest, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),
    do_scroll("scroll", ::do_scroll, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_title("title", ::do_title, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_unalias("unalias", ::do_unalias, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_wimpy("wimpy", ::do_wimpy, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),

    /*
    * Communication commands.
    */
    do_bear_call("bearcall", ::do_bear_call, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_cb("cb", ::do_cb, Pos.Sleeping, 0, LOG_NORMAL, CMD_GHOST),
    do_deaf("deaf", ::do_deaf, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_emote(arrayOf("emote", ","), ::do_emote, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),
    do_pmote("pmote", ::do_pmote, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),
    do_gtell(arrayOf("gtell", ";"), ::do_gtell, Pos.Dead, 0, LOG_NORMAL, CMD_GHOST),
    do_note("note", ::do_note, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_pose("pose", ::do_pose, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),
    do_pray("pray", ::do_pray, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_quiet("quiet", ::do_quiet, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_reply("reply", ::do_reply, Pos.Sleeping, 0, LOG_NORMAL, CMD_GHOST),
    do_replay("replay", ::do_replay, Pos.Sleeping, 0, LOG_NORMAL, CMD_GHOST),
    do_say(arrayOf("say", "'"), ::do_say, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),
    do_shout("shout", ::do_shout, Pos.Resting, 3, LOG_NORMAL, CMD_GHOST),
    do_warcry("warcry", ::do_warcry, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_unread("unread", ::do_unread, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_yell("yell", ::do_yell, Pos.Resting, 0, LOG_NORMAL, CMD_GHOST),

    /*
    * Object manipulation commands.
    */
    do_brandish("brandish", ::do_brandish, Pos.Resting, 0, LOG_NORMAL, 0),
    do_bury("bury", ::do_bury, Pos.Standing, 0, LOG_NORMAL, 0),
    do_butcher("butcher", ::do_butcher, Pos.Standing, 0, LOG_NORMAL, 0),
    do_close("close", ::do_close, Pos.Resting, 0, LOG_NORMAL, 0),
    do_detect_hidden("detect", ::do_detect_hidden, Pos.Resting, 0, LOG_NORMAL, 0),
    do_drag("drag", ::do_drag, Pos.Standing, 0, LOG_NORMAL, 0),
    do_drink("drink", ::do_drink, Pos.Resting, 0, LOG_NORMAL, 0),
    do_drop("drop", ::do_drop, Pos.Resting, 0, LOG_NORMAL, 0),
    do_eat("eat", ::do_eat, Pos.Resting, 0, LOG_NORMAL, 0),
    do_enchant("enchant", ::do_enchant, Pos.Resting, 0, LOG_NORMAL, 0),
    do_envenom("envenom", ::do_envenom, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_fill("fill", ::do_fill, Pos.Resting, 0, LOG_NORMAL, 0),
    do_fly("fly", ::do_fly, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_give("give", ::do_give, Pos.Resting, 0, LOG_NORMAL, 0),
    do_heal("heal", ::do_heal, Pos.Resting, 0, LOG_NORMAL, 0),
    do_layhands("layhands", ::do_layhands, Pos.Resting, 0, LOG_NORMAL, 0),
    do_list("list", ::do_list, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_lock("lock", ::do_lock, Pos.Resting, 0, LOG_NORMAL, 0),
    do_lore("lore", ::do_lore, Pos.Resting, 0, LOG_NORMAL, 0),
    do_open("open", ::do_open, Pos.Resting, 0, LOG_NORMAL, 0),
    do_pick("pick", ::do_pick, Pos.Resting, 0, LOG_NORMAL, 0),
    do_pour("pour", ::do_pour, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_put("put", ::do_put, Pos.Resting, 0, LOG_NORMAL, 0),
    do_quaff("quaff", ::do_quaff, Pos.Resting, 0, LOG_NORMAL, 0),
    do_recite("recite", ::do_recite, Pos.Resting, 0, LOG_NORMAL, 0),
    do_remove("remove", ::do_remove, Pos.Resting, 0, LOG_NORMAL, 0),
    do_request("request", ::do_request, Pos.Standing, 0, LOG_NORMAL, 0),
    do_sell("sell", ::do_sell, Pos.Resting, 0, LOG_NORMAL, 0),
    do_get(arrayOf("get", "take"), ::do_get, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_sacrifice(arrayOf("sacrifice", "junk"), ::do_sacrifice, Pos.Resting, 0, LOG_NORMAL, 0),
    do_trophy("trophy", ::do_trophy, Pos.Standing, 0, LOG_NORMAL, 0),
    do_value("value", ::do_value, Pos.Resting, 0, LOG_NORMAL, 0),
    do_wear(arrayOf("wear", "hold", "wield"), ::do_wear, Pos.Resting, 0, LOG_NORMAL, 0),
    do_zap("zap", ::do_zap, Pos.Resting, 0, LOG_NORMAL, 0),

    /*
    * Combat commands.
    */
    do_ambush("ambush", ::do_ambush, Pos.Standing, 0, LOG_NORMAL, 0),
    do_assassinate("assassinate", ::do_assassinate, Pos.Standing, 0, LOG_NORMAL, 0),
    do_bash("bash", ::do_bash, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_bash_door("bashdoor", ::do_bash_door, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_backstab(arrayOf("backstab", "bs"), ::do_backstab, Pos.Standing, 0, LOG_NORMAL, 0),
    do_vbite("bite", ::do_vbite, Pos.Standing, 0, LOG_NORMAL, 0),
    do_blindness_dust("blindness", ::do_blindness_dust, Pos.Fighting, 0, LOG_ALWAYS, 0),
    do_vtouch("touch", ::do_vtouch, Pos.Standing, 0, LOG_NORMAL, 0),
    do_berserk("berserk", ::do_berserk, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_bloodthirst("bloodthirst", ::do_bloodthirst, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_blackjack("blackjack", ::do_blackjack, Pos.Standing, 0, LOG_NORMAL, 0),
    do_caltraps("caltraps", ::do_caltraps, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_explode("explode", ::do_explode, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_camouflage("camouflage", ::do_camouflage, Pos.Standing, 0, LOG_NORMAL, 0),
    do_circle("circle", ::do_circle, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_cleave("cleave", ::do_cleave, Pos.Standing, 0, LOG_NORMAL, 0),

    do_dirt("dirt", ::do_dirt, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_disarm("disarm", ::do_disarm, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_dishonor("dishonor", ::do_dishonor, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_dismount("dismount", ::do_dismount, Pos.Standing, 0, LOG_NORMAL, 0),
    do_flee("flee", ::do_flee, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_guard("guard", ::do_guard, Pos.Standing, 0, LOG_NORMAL, 0),

    do_kick("kick", ::do_kick, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_lash("lash", ::do_lash, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_lion_call("lioncall", ::do_lion_call, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_make("make", ::do_make, Pos.Standing, 0, LOG_NORMAL, 0),
    do_mount("mount", ::do_mount, Pos.Standing, 0, LOG_NORMAL, 0),
    do_murde("murde", ::do_murde, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_murder("murder", ::do_murder, Pos.Fighting, 0, LOG_ALWAYS, 0),
    do_nerve("nerve", ::do_nerve, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_poison_smoke("poison", ::do_poison_smoke, Pos.Fighting, 0, LOG_ALWAYS, 0),
    do_rescue("rescue", ::do_rescue, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_resistance("resistance", ::do_resistance, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_truesight("truesight", ::do_truesight, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_shield("shield", ::do_shield, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_spellbane("spellbane", ::do_spellbane, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_strangle("strangle", ::do_strangle, Pos.Standing, 0, LOG_NORMAL, 0),
    do_tame("tame", ::do_tame, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_throw("throw", ::do_throw, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_tiger("tiger", ::do_tiger, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_trip("trip", ::do_trip, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_target("target", ::do_target, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_vampire("vampire", ::do_vampire, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_vanish("vanish", ::do_vanish, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_weapon("weapon", ::do_weapon, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_blink("blink", ::do_blink, Pos.Fighting, 0, LOG_NORMAL, CMD_KEEP_HIDE),

    /*
    * Miscellaneous commands.
    */
    do_endure("endure", ::do_endure, Pos.Standing, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_follow("follow", ::do_follow, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_gain("gain", ::do_gain, Pos.Standing, 0, LOG_NORMAL, 0),
    do_enter(arrayOf("enter", "leave", "go"), ::do_enter, Pos.Standing, 0, LOG_NORMAL, CMD_GHOST),
    do_fade("fade", ::do_fade, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_herbs("herbs", ::do_herbs, Pos.Standing, 0, LOG_NORMAL, 0),
    do_hara("hara", ::do_hara, Pos.Standing, 0, LOG_NORMAL, 0),

    do_hide("hide", ::do_hide, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_human("human", ::do_human, Pos.Standing, 0, LOG_NORMAL, 0),
    do_hunt("hunt", ::do_hunt, Pos.Standing, 0, LOG_NORMAL, 0),
    do_qui("qui", ::do_qui, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_quit("quit", ::do_quit, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_recall("recall", ::do_recall, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_rent("rent", ::do_rent, Pos.Dead, 0, LOG_NORMAL, 0),
    do_save("save", ::do_save, Pos.Dead, 0, LOG_NORMAL, CMD_GHOST),
    do_sleep("sleep", ::do_sleep, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_slist("slist", ::do_slist, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_sneak("sneak", ::do_sneak, Pos.Standing, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_split("split", ::do_split, Pos.Resting, 0, LOG_NORMAL, 0),
    do_steal("steal", ::do_steal, Pos.Standing, 0, LOG_NORMAL, 0),
    do_train("train", ::do_train, Pos.Resting, 0, LOG_NORMAL, 0),
    do_visible("visible", ::do_visible, Pos.Sleeping, 0, LOG_NORMAL, 0),
    do_wake("wake", ::do_wake, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE),
    do_wanted("wanted", ::do_wanted, Pos.Standing, 0, LOG_ALWAYS, 0),
    do_where("where", ::do_where, Pos.Resting, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),

    /*
    * Immortal commands.
    */
    do_advance("advance", ::do_advance, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_set("set", ::do_set, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_dump("dump", ::do_dump, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_rename("rename", ::do_rename, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_violate("violate", ::do_violate, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_track("track", ::do_track, Pos.Standing, 0, LOG_NORMAL, 0),
    do_trust("trust", ::do_trust, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),

    do_deny("deny", ::do_deny, Pos.Dead, L1, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_disconnect("disconnect", ::do_disconnect, Pos.Dead, L3, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_flag("flag", ::do_flag, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_freeze("freeze", ::do_freeze, Pos.Dead, L7, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_premort("premort", ::do_premort, Pos.Dead, L8, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_protect("protect", ::do_protect, Pos.Dead, L1, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_reboo("reboo", ::do_reboo, Pos.Dead, L1, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_reboot("reboot", ::do_reboot, Pos.Dead, L1, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_induct("induct", ::do_induct, Pos.Dead, 0, LOG_ALWAYS, 0),
    do_grant("grant", ::do_grant, Pos.Dead, L2, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_smite("smite", ::do_smite, Pos.Dead, L7, LOG_ALWAYS, 0),
    do_limited("limited", ::do_limited, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_slookup("lookupRace", ::do_slookup, Pos.Dead, L2, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_popularity("popularity", ::do_popularity, Pos.Dead, L2, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_shutdow("shutdow", ::do_shutdow, Pos.Dead, L1, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_shutdown("shutdown", ::do_shutdown, Pos.Dead, L1, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_sockets("sockets", ::do_sockets, Pos.Dead, L4, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_wizlock("wizlock", ::do_wizlock, Pos.Dead, L2, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_affrooms("affrooms", ::do_affrooms, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_force("force", ::do_force, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_load("load", ::do_load, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_newlock("newlock", ::do_newlock, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_noaffect("noaffect", ::do_noaffect, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_nochannels("nochannels", ::do_nochannels, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_notitle("notitle", ::do_notitle, Pos.Dead, L7, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_noemote("noemote", ::do_noemote, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_noshout("noshout", ::do_noshout, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_notell("notell", ::do_notell, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_pecho("pecho", ::do_pecho, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_purge("purge", ::do_purge, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_restore("restore", ::do_restore, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_sla("sla", ::do_sla, Pos.Dead, L3, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_slay("slay", ::do_slay, Pos.Dead, L3, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_transfer(arrayOf("teleport", "transfer"), ::do_transfer, Pos.Dead, L7, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_bamfin("poofin", ::do_bamfin, Pos.Dead, L8, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_bamfout("poofout", ::do_bamfout, Pos.Dead, L8, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_echo("gecho", ::do_echo, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_holylight("holylight", ::do_holylight, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_incognito("incognito", ::do_incognito, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_invis(arrayOf("invis", "wizinvis"), ::do_invis, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_log("log", ::do_log, Pos.Dead, L1, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_memory("memory", ::do_memory, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_mwhere("mwhere", ::do_mwhere, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_owhere("owhere", ::do_owhere, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_peace("peace", ::do_peace, Pos.Dead, L5, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_penalty("penalty", ::do_penalty, Pos.Dead, L7, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_recho("echo", ::do_recho, Pos.Dead, L6, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_return("return", ::do_return, Pos.Dead, L6, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_snoop("snoop", ::do_snoop, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_stat("stat", ::do_stat, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_string("string", ::do_string, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_switch("switch", ::do_switch, Pos.Dead, L6, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_vnum("vnum", ::do_vnum, Pos.Dead, L4, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_zecho("zecho", ::do_zecho, Pos.Dead, L4, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_cabal_scan("cabal_scan", ::do_cabal_scan, Pos.Standing, 0, LOG_NEVER, CMD_KEEP_HIDE or CMD_GHOST),
    do_clone("clone", ::do_clone, Pos.Dead, L5, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_wiznet("wiznet", ::do_wiznet, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_immtalk(arrayOf("immtalk", ":"), ::do_immtalk, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_imotd("imotd", ::do_imotd, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_smote("smote", ::do_smote, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_prefi("prefi", ::do_prefi, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_prefix("prefix", ::do_prefix, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_objlist("objlist", ::do_objlist, Pos.Dead, ML, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_settraps("settraps", ::do_settraps, Pos.Standing, 0, LOG_NORMAL, 0),
    do_slook("slook", ::do_slook, Pos.Sleeping, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_learn("learn", ::do_learn, Pos.Standing, 0, LOG_NORMAL, 0),
    do_teach("teach", ::do_teach, Pos.Standing, 0, LOG_NORMAL, 0),
    do_camp("camp", ::do_camp, Pos.Standing, 0, LOG_NORMAL, 0),
    do_dig("dig", ::do_dig, Pos.Standing, 0, LOG_NORMAL, 0),
    do_tail("tail", ::do_tail, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_push("push", ::do_push, Pos.Standing, 0, LOG_NORMAL, 0),
    do_demand("demand", ::do_demand, Pos.Standing, 0, LOG_NORMAL, 0),
    do_bandage("bandage", ::do_bandage, Pos.Fighting, 0, LOG_NORMAL, 0),
    do_shoot("shoot", ::do_shoot, Pos.Standing, 0, LOG_NORMAL, 0),
    do_maximum("maximum", ::do_maximum, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_find("find", ::do_find, Pos.Dead, ML, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_score("nscore", ::do_score, Pos.Dead, 0, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_katana("katana", ::do_katana, Pos.Standing, 0, LOG_NORMAL, 0),
    do_control("control", ::do_control, Pos.Standing, 0, LOG_NORMAL, 0),
    do_ititle("ititle", ::do_ititle, Pos.Dead, IM, LOG_NORMAL, CMD_KEEP_HIDE or CMD_GHOST),
    do_sense("sense", ::do_sense, Pos.Resting, 0, LOG_NORMAL, 0),
    do_judge("judge", ::do_judge, Pos.Resting, 0, LOG_ALWAYS, CMD_KEEP_HIDE),
    do_remor("remor", ::do_remor, Pos.Standing, 0, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST),
    do_remort("remort", ::do_remort, Pos.Standing, 0, LOG_ALWAYS, CMD_KEEP_HIDE or CMD_GHOST);

    @Suppress("unused")
    constructor(name: String, do_fun: DoFun0, position: Pos, level: Int, log: Int, extra: Int)
            : this(arrayOf<String>(name), { _, _ -> do_fun() }, position, level, log, extra)

    @Suppress("unused")
    constructor(name: String, do_fun: DoFun1, position: Pos, level: Int, log: Int, extra: Int)
            : this(arrayOf<String>(name), { ch, _ -> do_fun(ch) }, position, level, log, extra)

    @Suppress("unused")
    constructor(name: String, do_fun: DoFun, position: Pos, level: Int, log: Int, extra: Int)
            : this(arrayOf<String>(name), do_fun, position, level, log, extra)

}
