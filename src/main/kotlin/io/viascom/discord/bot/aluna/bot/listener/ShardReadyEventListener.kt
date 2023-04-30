package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.event.EventPublisher
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent

class ShardReadyEventListener(private val discordReadyEventPublisher: EventPublisher, private val discordBot: DiscordBot) : CoroutineEventListener {

    private var allShardsReady: Boolean = false

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is ReadyEvent -> onReady(event)
        }
    }

    private suspend fun onReady(event: ReadyEvent) = withContext(AlunaDispatchers.Internal) {
        //Publish DiscordAllShardsReadyEvent as soon as all shards are connected. If subset is used, this is only triggered on the last node.
        if ((event.jda.shardInfo.shardId + 1) == (event.jda.shardInfo.shardTotal) && !allShardsReady) {
            allShardsReady = true
            discordReadyEventPublisher.publishDiscordAllShardsReadyEvent(event, event.jda.shardManager!!)
            event.jda.shardManager!!.removeEventListener(this)
        }
    }


}