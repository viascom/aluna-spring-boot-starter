# aluna-spring-boot-starter



### Create a command
```kotlin
@Command
class PingCommand: DiscordCommand(
    "ping",
    "Send a ping"
) {
    override fun execute(event: SlashCommandInteractionEvent) {
        event.reply("Pong\nYour locale is:${this.userLocale}").queue()
    }
}
```


### React to Button
```kotlin
event.reply("Pong\nYour locale is:${this.userLocale}").addActionRows(ActionRow.of(Button.primary("hi", "Hi")))
    .queueAndRegisterInteraction(hook, this)


override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
    logger.debug(this.hashCode().toString())
    if (event.componentId == "hi") {
        event.editMessage("Oh hi :)").removeActionRows().queue()
    }

    return true
}
```

### Properties
```yaml
aluna:
  owner-ids: 172591119912140811
  discord:
    token: ---
    application-id: ---
    gatewayIntents:
    default-permissions:

logging:
  level:
    io.viascom.discord.bot.aluna: DEBUG
    io.viascom.discord.bot.aluna.event.EventPublisher: INFO #Set to DEBUG to show al published events
    io.viascom.discord.bot.aluna.bot.handler.AlunaLocalizationFunction: INFO #Set to DEBUG to show translation keys for interactions
```
