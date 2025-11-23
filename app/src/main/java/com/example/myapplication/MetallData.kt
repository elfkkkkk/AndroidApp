package com.example.myapplication

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.Attribute

@Root(name = "Metall", strict = false)
data class MetallData @JvmOverloads constructor(
    @field:Attribute(name = "FromDate", required = false)
    var fromDate: String? = null,

    @field:Attribute(name = "ToDate", required = false)
    var toDate: String? = null,

    @field:Attribute(name = "name", required = false)
    var name: String? = null,

    @field:ElementList(entry = "Record", inline = true, required = false)
    var records: List<GoldRecord> = emptyList()
)

@Root(name = "Record", strict = false)
data class GoldRecord @JvmOverloads constructor(
    @field:Attribute(name = "Date", required = false)
    var date: String? = null,

    @field:Attribute(name = "Code", required = false)
    var code: Int = 0,

    @field:Element(name = "Buy", required = false)
    var buy: String? = null,

    @field:Element(name = "Sell", required = false)
    var sell: String? = null
) {
    // преобразование строки в число
    fun getGoldPrice(): Double {
        return try {
            buy?.replace(",", ".")?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}