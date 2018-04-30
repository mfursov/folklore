package net.sf.nightworks.model

import net.sf.nightworks.VNum

class Shop(

        /** Vnum of shop keeper mob  */
        val keeper: VNum,

        /** Item types shop will buy */
        val buy_type: Array<ItemType>,

        /** Cost multiplier for buying */
        val profit_buy: Int,

        /** Cost multiplier for selling */
        val profit_sell: Int,

        /** First opening hour */
        val open_hour: Int,

        /** First closing hour */
        val close_hour: Int
)
