package fr.ribesg.android.encypokedia.tools

import com.github.salomonbrys.kotson.obj
import com.google.gson.GsonBuilder
import fr.ribesg.android.encypokedia.tools.webcrawler.PokebipCrawler
import fr.ribesg.android.encypokedia.tools.webcrawler.PokemonDbCrawler
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Ribesg
 */
fun main(args: Array<String>) {

    // Get english data
    println("Crawling data from pokemondb.net...")
    val res = PokemonDbCrawler.crawl()
    println("\r\tDone.")

    // Get french data
    println("Crawling data from pokebip.fr...")
    val fr = PokebipCrawler.crawl()
    println("\r\tDone.")

    // Merge
    println("Merging...")
    val pkmnFr = fr["pokemons"].obj
    res["pokemons"].obj.entrySet().forEach {
        val num = it.key
        val pkmnData = it.value.obj
        print("\r\t#$num          ")
        pkmnData["names"].obj.add("fr", pkmnFr[num].obj["name"])
        pkmnData["pokedex"].obj.add("fr", pkmnFr[num].obj["pokedex"])
    }
    println("\r\tDone.")

    // Produce output
    println("Creating output files...")
    val outputFolder = Paths.get("out")
    if (Files.isDirectory(outputFolder)) {
        for (f in Files.newDirectoryStream(outputFolder)) {
            Files.delete(f)
        }
    } else {
        Files.createDirectories(outputFolder)
    }
    val gson = GsonBuilder().setPrettyPrinting().create()
    res["pokemons"].obj.entrySet().forEach { entry ->
        print("\r\t#${entry.key}          ")
        Files.newBufferedWriter(outputFolder.resolve(entry.key + ".json"), StandardCharsets.UTF_8).use { out ->
            out.write(gson.toJson(entry.value))
        }
    }
    println("\r\tDone.")
}
