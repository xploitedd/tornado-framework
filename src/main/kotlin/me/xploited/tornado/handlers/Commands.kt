package me.xploited.tornado.handlers

import me.xploited.tornado.config
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.reflect.full.findAnnotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Command(
    val name: String,
    val description: String = "",
    val minArgs: Int = 0,
    val guildOnly: Boolean = false,
    val requireVoice: Boolean = false,
    val permissions: Array<Permission> = [],
    val aliases: Array<String> = []
)

interface CommandExecutor { fun execute(args: Array<String>?, message: Message) }

private val PREFIX = config.prefix
internal object Commands : ListenerAdapter() {
    // store the command and the information about the command
    // it needs this because the permissions (along with other things) are specified by the Command annotation
    private val commands: MutableMap<String, Pair<Command, CommandExecutor>> = mutableMapOf()

    fun registerCommand(executor: CommandExecutor) {
        // throw NPE if the annotation does not exists
        val command = executor::class.findAnnotation<Command>()!!
        val commandPair = Pair(command, executor)
        commands[command.name] = commandPair
        // also add the command aliases
        for (alias in command.aliases)
            commands[alias] = commandPair
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message
        val pair = parseCommand(message.contentRaw)
        // if it is a command
        if (pair != null) {
            val channel = event.channel
            val commandName = pair.first
            if (!commands.containsKey(commandName)) {
                channel.sendMessage("The command $commandName was not found!").queue()
                return
            }

            val (commandInfo, commandExecutor) = commands[commandName]!!
            if (!event.isFromGuild && (commandInfo.guildOnly || commandInfo.requireVoice)) {
                channel.sendMessage("This command must be executed from inside a guild!").queue()
                return
            }

            val member = event.member
            if (member != null) {
                // guild checks (permissions and voice channel)
                val permissions = commandInfo.permissions.filter { !member.hasPermission(it) }
                if (permissions.isNotEmpty()) {
                    channel.sendMessage("You don't have enough permissions to execute this command!\n" +
                            "Missing Permissions: ${permissions.joinToString()}").queue()
                    return
                }

                if (commandInfo.requireVoice && (member.voiceState == null || !member.voiceState!!.inVoiceChannel())) {
                    channel.sendMessage("This command requires you to be in a voice channel.\n" +
                            "Please join one or check if voice is enabled!").queue()
                    return
                }
            }

            val args = pair.second
            val minArgs = commandInfo.minArgs
            if (minArgs > 0 && minArgs > args?.size ?: 0) {
                channel.sendMessage("The command requires at least $minArgs argument(s)\nUsage: $PREFIX$commandName args...").queue()
                return
            }

            // finally execute the command
            commandExecutor.execute(pair.second, message)
        }
    }

    private fun parseCommand(content: String): Pair<String, Array<String>?>? {
        if (content.startsWith(PREFIX) && content.length > 1) {
            val firstSpace = content.indexOf(' ')
            // check if there was a first space or if after the space is anything
            val command: String
            var args: Array<String>? = null
            if (firstSpace == -1 || firstSpace + 1 == content.length) {
                command = content.substring(1).trimEnd()
            } else {
                command = content.substring(1, firstSpace)
                args = content.substring(firstSpace + 1).split(' ').toTypedArray()
            }

            return Pair(command, args)
        }

        return null
    }
}