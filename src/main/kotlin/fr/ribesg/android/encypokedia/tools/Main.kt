package fr.ribesg.android.encypokedia.tools

import com.google.gson.GsonBuilder
import fr.ribesg.android.encypokedia.tools.webcrawler.PokemonDbCrawler
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Ribesg
 */
fun main(args: Array<String>): Unit =
    Files.newBufferedWriter(Paths.get("out.json"), StandardCharsets.UTF_8).use { file ->
        file.write(
            GsonBuilder().setPrettyPrinting().create().toJson(
                PokemonDbCrawler.crawl()
            )
        )
    }
