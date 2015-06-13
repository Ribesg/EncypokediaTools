package fr.ribesg.android.encypokedia.tools.webcrawler

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.select.Elements

/**
 * @author Ribesg
 */
object PokemonDbCrawler {

    private val URL_POKEDEX_BASE = "http://pokemondb.net"
    private val URL_POKEDEX_HOME = URL_POKEDEX_BASE + "/pokedex/all"

    fun crawl(): JsonObject = jsonObject(
        "pokemons" to crawlPokemons()
    )

    fun crawlPokemons(): JsonObject {
        val res = jsonObject()

        val dexDoc = Jsoup.connect(URL_POKEDEX_HOME).get()
        val dexLines: Elements = dexDoc.select("#pokedex > tbody > tr")
        var previousNum = -1
        dexLines.forEach { dexLine ->
            val lineColumns = dexLine.children()

            // Column #1: Number
            val num = lineColumns[0].attr("data-sort-value").toInt()

            if (previousNum != num) {
                // Column #2: Link
                val url = URL_POKEDEX_BASE + lineColumns[1].child(0).attr("href")

                // Parse pokemon page
                print("\r\t#$num")
                res.add(num.toString(), crawlPokemon(url))

                // Don't overload server
                Thread.sleep(400)
            }
            previousNum = num
        }

        return res
    }

    fun crawlPokemon(url: String): JsonObject {
        val pokeDoc = Jsoup.connect(url).get()

        val name = pokeDoc.getElementsByTag("h1").first().text()

        val panels = pokeDoc.select(
            ".tabset-basics > .svtabs-panel-list > li"
        )
        val formesNames = if (panels.size() > 1) {
            pokeDoc.select(
                ".tabset-basics > .svtabs-tab-list > li"
            ).map { tab ->
                tab.child(0).text()
            }
        } else null
        val formes = jsonObject()
        var basics: Elements? = null
        var basicsData: Elements? = null
        for (i in 0..panels.size() - 1) {
            val forme = panels.select(
                ":root > .colset > .col"
            )

            val pokedexData = forme[1].child(1).child(0).children()
            if (i == 0) {
                basics = forme
                basicsData = pokedexData
            }

            val typeElements = pokedexData[1].child(1)
            val type1 = typeElements.child(0).text()
            val type2 = try {
                typeElements.child(1).text()
            } catch (e: IndexOutOfBoundsException) {
                null // No second type
            }
            val species = pokedexData[2].child(1).text()
            val height = pokedexData[3].child(1).text()
            val weight = pokedexData[4].child(1).text()
            val abilities = jsonArray()
            val hiddenAbilities = jsonArray()
            pokedexData[5].child(1).children().forEach {
                if ("a".equals(it.tagName())) {
                    abilities.add(jsonObject(
                        "name" to it.text(),
                        "desc" to it.attr("title")
                    ))
                } else if ("small".equals(it.tagName())) {
                    val a = it.child(0)
                    hiddenAbilities.add(jsonObject(
                        "name" to a.text(),
                        "desc" to a.attr("title")
                    ))
                }
            }

            val baseStats = forme[3].child(1).child(1).children()
            val baseHp = baseStats[0].child(1).text()
            val minHp = baseStats[0].child(3).text()
            val maxHp = baseStats[0].child(4).text()
            val baseAtk = baseStats[1].child(1).text()
            val minAtk = baseStats[1].child(3).text()
            val maxAtk = baseStats[1].child(4).text()
            val baseDef = baseStats[2].child(1).text()
            val minDef = baseStats[2].child(3).text()
            val maxDef = baseStats[2].child(4).text()
            val baseSpa = baseStats[3].child(1).text()
            val minSpa = baseStats[3].child(3).text()
            val maxSpa = baseStats[3].child(4).text()
            val baseSpd = baseStats[4].child(1).text()
            val minSpd = baseStats[4].child(3).text()
            val maxSpd = baseStats[4].child(4).text()
            val baseSpe = baseStats[5].child(1).text()
            val minSpe = baseStats[5].child(3).text()
            val maxSpe = baseStats[5].child(4).text()

            val tabName = formesNames?.get(i) ?: null
            val formKey = formeKeyFromFormeName(tabName)

            formes.add(formKey, jsonObject(
                "data" to jsonObject(
                    "type1" to type1,
                    "type2" to type2,
                    "species" to species,
                    "height" to height,
                    "weight" to weight,
                    "abilities" to abilities,
                    "hiddenAbilities" to hiddenAbilities
                ),
                "stats" to jsonObject(
                    "hp" to jsonObject(
                        "base" to baseHp,
                        "min" to minHp,
                        "max" to maxHp
                    ),
                    "atk" to jsonObject(
                        "base" to baseAtk,
                        "min" to minAtk,
                        "max" to maxAtk
                    ),
                    "def" to jsonObject(
                        "base" to baseDef,
                        "min" to minDef,
                        "max" to maxDef
                    ),
                    "spa" to jsonObject(
                        "base" to baseSpa,
                        "min" to minSpa,
                        "max" to maxSpa
                    ),
                    "spd" to jsonObject(
                        "base" to baseSpd,
                        "min" to minSpd,
                        "max" to maxSpd
                    ),
                    "spe" to jsonObject(
                        "base" to baseSpe,
                        "min" to minSpe,
                        "max" to maxSpe
                    )
                )
            ))
        }

        basics!!
        basicsData!!

        val jpName = if (basicsData.size() >= 8) {
            basicsData[7].child(1).text()
        } else {
            name // Pikachu, etc
        }

        val training = basics[2].child(0).child(0).child(1).child(0).children()
        val evYield = training[0].child(1).text()
        val catchRate = training[1].child(1).text().replace("\\(.*\\)".toRegex(), "").trim()
        val baseHappiness = training[2].child(1).text().replace("\\(.*\\)".toRegex(), "").trim()
        val baseExp = training[3].child(1).text()
        val growthRate = training[4].child(1).text()

        val breeding = basics[2].child(0).child(1).child(1).child(0).children()
        val eggGroups = jsonArray()
        breeding[0].child(1).children().forEach {
            eggGroups.add(it.text().toJson())
        }
        val genderProb = breeding[1].child(1).text().trim()
        val eggCycles = breeding[2].child(1).text().replace("\\(.*\\)".toRegex(), "").trim()

        val pokedexDesc = pokeDoc.getElementById("dex-flavor").nextElementSibling()
            .nextElementSibling().child(0).children()
        var xDesc: String? = null
        var yDesc: String? = null
        for (elem in pokedexDesc.reverse()) {
            when (elem.child(0).text()) {
                "X" -> xDesc = elem.child(1).text()
                "Y" -> yDesc = elem.child(1).text()
            }
            if (xDesc != null && yDesc != null) break
        }

        return jsonObject(
            "name" to name,
            "names" to jsonObject(
                "jp" to jpName
            ),
            "formes" to formes,
            "training" to jsonObject(
                "evYield" to evYield,
                "catchRate" to catchRate,
                "baseHappiness" to baseHappiness,
                "baseExp" to baseExp,
                "growthRate" to growthRate
            ),
            "breeding" to jsonObject(
                "eggGroups" to eggGroups,
                "genderProb" to genderProb,
                "eggCycles" to eggCycles
            ),
            "pokedex" to jsonObject(
                "en" to jsonObject(
                    "X" to xDesc,
                    "Y" to yDesc
                )
            )
        )
    }

    fun formeKeyFromFormeName(s: String?): String {
        return when {

        // Only one forme
            s == null            -> "base"

        // Mega evolutions
            s.contains("Mega")   -> when {
                s.endsWith('X') -> "mega-x" // X
                s.endsWith('Y') -> "mega-y" // Y
                else            -> "mega"   // Common
            }

        // Primal Goudron & Kyogre
            s.contains("Primal") -> "primal"

        // Pokemons with "Forme" forms
            s.contains("Forme")  -> when {
                s.contains("Normal")
                || s.contains("Altered")
                || s.contains("Land")
                || s.contains("Incarnate")
                || s.contains("Ordinary")
                     -> "base"
                else -> s.replace("Forme", "").trim()
            }

        // Cloaks of Wormadam
            s.contains("Cloak")  -> s.replace("Cloak", "").trim()

        // Rotom
            s.contains("Rotom")  -> when {
                s.equals("Rotom") -> "base"
                else              -> s.replace("Rotom", "").trim()
            }

        // Darmanitan
            s.contains("Mode")   -> when {
                s.contains("Standard") -> "base"
                else                   -> s.replace("Mode", "").trim()
            }

        // Kyurem
            s.contains("Kyurem") -> when {
                s.equals("Kyurem") -> "base"
                else               -> s.replace("Kyurem", "").trim()
            }

        // Meowstic
            s.equals("Male")     -> s
            s.equals("Female")   -> s

        // Pumpkaboo & Gourgeist
            s.contains("Size")   -> when {
                s.contains("Average") -> "base"
                else                  -> s.replace("Size", "").trim()
            }

        // All others
            else                 -> "base"
        }!!.toLowerCase()
    }

}
