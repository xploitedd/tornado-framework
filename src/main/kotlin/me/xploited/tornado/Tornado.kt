package me.xploited.tornado

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.xploited.tornado.handlers.*
import me.xploited.tornado.handlers.Commands
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder

val config = ConfigurationHandler.readConfig("config", Configuration::class.java)
class Tornado {
    val bot: JDA = JDABuilder(config.discordToken).build()
    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()

    init {
        // load default config
        bot.addEventListener(Commands)
        AudioSourceManagers.registerRemoteSources(playerManager)
    }

    fun registerCommand(executor: CommandExecutor) = Commands.registerCommand(executor)
}