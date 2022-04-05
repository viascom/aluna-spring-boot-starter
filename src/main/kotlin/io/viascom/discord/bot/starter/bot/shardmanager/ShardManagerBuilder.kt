package io.viascom.discord.bot.starter.bot.shardmanager

import net.dv8tion.jda.api.sharding.ShardManager

interface ShardManagerBuilder {

    fun build(): ShardManager

}
