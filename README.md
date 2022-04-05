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


### Create an EventWaiter
```kotlin
eventWaiter.waitForInteraction("command:ping:" + author.id,
    ButtonInteractionEvent::class.java,
    hook = it,
    action = {
        if (it.componentId == "hi") {
            it.editMessage("Oh hi :)").removeActionRows().queue()
        }
    })
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
    io.viascom.discord.bot.starter.bot: DEBUG
```
