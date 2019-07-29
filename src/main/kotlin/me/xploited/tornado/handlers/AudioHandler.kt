package me.xploited.tornado.handlers

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import me.xploited.tornado.utils.EmbedBuilder
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.audio.UserAudio
import net.dv8tion.jda.api.entities.*
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

internal val audioManagers: MutableMap<Guild, GuildAudioManager> = mutableMapOf()
class GuildAudioManager internal constructor(val guild: Guild, val manager: AudioPlayerManager) {
    private val player: AudioPlayer = manager.createPlayer()
    private val scheduler = TrackScheduler(player)
    val handler = AudioPlayerHandler(player)

    init {
        guild.audioManager.receivingHandler = handler
        guild.audioManager.sendingHandler = handler
        player.addListener(scheduler)
    }

    fun play(identifier: String, member: Member, textChannel: TextChannel) {
        // load the specified song and handle the result
        manager.loadItemOrdered(this, identifier, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack?) {
                val queuePos = play(track!!, member, textChannel)
                if (queuePos != 0)
                    textChannel.sendMessage(getMusicEmbed(track.info.title,
                        "Added to the queue! Position: #$queuePos",
                        track.info.uri,
                        thumbnailUrl = track.thumnail,
                        author = EmbedBuilder.Author(track.info.author),
                        member = member)).queue()
            }

            override fun playlistLoaded(playlist: AudioPlaylist?) {
                textChannel.sendMessage("Loading playlist **${playlist!!.name}** ...").queue {
                    for (track in playlist.tracks)
                        play(track, member, textChannel)

                    textChannel.sendMessage(getMusicEmbed(playlist.name,
                        "Added playlist to the queue!",
                        playlist.selectedTrack.info.uri,
                        thumbnailUrl = playlist.thumnail,
                        member = member)).queue()
                }
            }

            override fun noMatches() { textChannel.sendMessage("404! We did not find anything.").queue() }

            override fun loadFailed(exception: FriendlyException?) { textChannel.sendMessage("An error occurred! ${exception!!.message}").queue() }
        })
    }

    fun nextTrack() { scheduler.nextTrack() }

    fun stopTrack() { player.stopTrack() }

    fun setPaused(paused: Boolean) { player.isPaused = paused }

    fun joinVoiceChannel(member: Member) {
        // joins the same voice channel as the specified member
        val audioManager = guild.audioManager
        if (!audioManager.isConnected && !audioManager.isAttemptingToConnect)
            audioManager.openAudioConnection(member.voiceState!!.channel)
    }

    private fun play(audioTrack: AudioTrack, member: Member, textChannel: TextChannel): Int {
        joinVoiceChannel(member)
        return scheduler.queue(audioTrack, member, textChannel)
    }

    class TrackScheduler(private val player: AudioPlayer) : AudioEventAdapter(), Iterable<Pair<AudioTrack, TrackScheduler.TrackInfo>> {
        var playing: TrackInfo? = null
            private set

        private val queue: ConcurrentLinkedQueue<Pair<AudioTrack, TrackInfo>> = ConcurrentLinkedQueue()

        fun queue(track: AudioTrack, member: Member, textChannel: TextChannel): Int {
            queue.offer(Pair(track, TrackInfo(member, textChannel)))
            if (playing != null)
                return queue.size

            nextTrack()
            return 0
        }

        fun nextTrack() {
            val (track, info) = queue.poll() ?: return
            playing = info
            player.startTrack(track, false)
        }

        override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
            playing!!.textChannel.sendMessage(getMusicEmbed(track!!.info.title,
                "Playing now!",
                track.info.uri,
                thumbnailUrl = track.thumnail,
                author = EmbedBuilder.Author(track.info.author),
                member = playing!!.member)).queue()
        }

        override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
            if (endReason?.mayStartNext == true)
                nextTrack()
            else if (endReason != AudioTrackEndReason.REPLACED) {
                playing = null
                queue.clear()
            }
        }

        override fun iterator(): Iterator<Pair<AudioTrack, TrackInfo>> = queue.iterator()

        data class TrackInfo(val member: Member, val textChannel: TextChannel)
    }

    class AudioPlayerHandler(private val player: AudioPlayer) : AudioSendHandler, AudioReceiveHandler {
        var allowReceiving: Boolean = false
        val receiveQueue: ConcurrentLinkedQueue<UserAudio> = ConcurrentLinkedQueue()
        var lastFrame: AudioFrame? = null

        override fun canReceiveUser(): Boolean = allowReceiving

        override fun handleUserAudio(userAudio: UserAudio) { receiveQueue.offer(userAudio) }

        override fun canProvide(): Boolean {
            provideIfNull()
            return lastFrame != null
        }

        override fun provide20MsAudio(): ByteBuffer? {
            provideIfNull()
            val data: ByteBuffer? = lastFrame?.let { ByteBuffer.wrap(it.data) }
            lastFrame = null
            return data
        }

        override fun isOpus(): Boolean = true

        private fun provideIfNull() {
            if (lastFrame == null)
                lastFrame = player.provide()
        }
    }
}

fun getMusicEmbed(title: String,
                  info: String,
                  url: String? = null,
                  color: EmbedBuilder.EmbedColor = EmbedBuilder.EmbedColor.SUCCESS,
                  thumbnailUrl: String? = null,
                  author: EmbedBuilder.Author? = null,
                  member: Member): MessageEmbed = EmbedBuilder().apply {

    this.title = title
    this.description = info
    this.url = url
    this.color = color
    this.thumbnail = thumbnailUrl?.let { EmbedBuilder.Image(it) }
    this.author = author
    this.footer = EmbedBuilder.Footer("by ${member.user.name}", member.user.avatarUrl)

}.build()

val AudioTrack.thumnail: String
    get() = "https://img.youtube.com/vi/${this.info.identifier}/hqdefault.jpg"

val AudioPlaylist.thumnail: String
    get() = this.selectedTrack.thumnail

fun getAudioManager(guild: Guild, manager: AudioPlayerManager): GuildAudioManager {
    if (audioManagers.containsKey(guild))
        return audioManagers[guild]!!

    val audioManager = GuildAudioManager(guild, manager)
    audioManagers[guild] = audioManager
    return audioManager
}