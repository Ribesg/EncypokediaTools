package fr.ribesg.android.encypokedia.tools.webcrawler

import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.select.Elements

/**
 * @author Ribesg
 */
object PokebipCrawler {

    private val URL_POKEDEX_BASE = "http://www.pokebip.com/pokemon/pokedex/"
    private val URL_POKEDEX_HOME = URL_POKEDEX_BASE + "6G_liste_des_pokemon.html"

    fun crawl(): JsonObject = jsonObject(
        "pokemons" to crawlPokemons()
    )

    fun crawlPokemons(): JsonObject {
        val res = jsonObject()

        val dexDoc = Jsoup.connect(URL_POKEDEX_HOME).get()
        val dexLines: Elements = dexDoc.select("#g6_liste_pkmn > table.g6 > tbody > tr")
        dexLines.forEach { dexLine ->
            val lineColumns = dexLine.children()

            // Column #1: Number
            val num = lineColumns[0].child(0).text()

            // Column #3: Link
            val url = URL_POKEDEX_BASE + lineColumns[2].child(0).child(0).child(0).attr("href")

            // Parse pokemon page
            res.add(num, crawlPokemon(url))
        }

        return res
    }

    fun crawlPokemon(url: String): JsonObject {
        val pokeDoc = Jsoup.connect(url).get()

        // Names
        val namesElements = pokeDoc.select("#g6_entete > div > p")
        val frName = namesElements[0].child(0).text()

        // Desc
        val descElements = pokeDoc.select("#g6_descriptions > table > tbody > tr")
        var xDesc: String? = null
        var yDesc: String? = null
        for (elem in descElements.reverse()) {
            when (elem.child(0).text()) {
                "Version X" -> xDesc = elem.child(1).text()
                "Version Y" -> yDesc = elem.child(1).text()
            }
            if (xDesc != null && yDesc != null) break
        }

        return jsonObject(
            "name" to frName,
            "desc" to jsonObject(
                "X" to xDesc,
                "Y" to yDesc
            )
        )
    }

}
