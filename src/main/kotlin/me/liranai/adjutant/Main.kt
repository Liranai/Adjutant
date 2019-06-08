package me.liranai.adjutant

import com.charleskorn.kaml.Yaml
import discord4j.core.DiscordClientBuilder
import kotlinx.serialization.parse
import me.liranai.adjutant.config.AdjutantConfig
import java.io.FileNotFoundException
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths

/**
 * The main entry point of the bot
 */
fun main() {
    val config = try {
        parseConfig()
    } catch (exception: Exception) {
        throw RuntimeException("Unable to read config", exception)
    }
    val bot = AdjutantDiscordBot(config)
    val client = DiscordClientBuilder(config.security.tokens.discord).build()
    bot.registerListeners(client.eventDispatcher)
    client.login().block()
}

/**
 * Reads and parses the config
 */
private fun parseConfig(): AdjutantConfig {
    val configFile = Paths.get("config.yml")
    if (!Files.exists(configFile))
        throw FileNotFoundException()
    val lines = Files.readAllLines(configFile).joinToString("\n")
    return Yaml.default.parse(AdjutantConfig.serializer(), lines)
}
