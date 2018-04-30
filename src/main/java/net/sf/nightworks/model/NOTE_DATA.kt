package net.sf.nightworks.model

data class NOTE_DATA(
        var type: Int = 0,
        var sender: String = "",
        var date: String = "",
        var to_list: String = "",
        var subject: String = "",
        var text: String = "",
        var timestamp: Long = 0L
)

