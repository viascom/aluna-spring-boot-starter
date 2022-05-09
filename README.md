<div align="center">
<img src="./logo.png"
         alt="FoxHttp Logo">
</div>

<h4 align="center">Fast and easy way to use JDA in your Spring-Boot Project</h4>

<p align="center">
  <a href="https://github.com/viascom/aluna-spring-boot-starter/releases"><img src="https://img.shields.io/github/v/release/viascom/aluna-spring-boot-starter?include_prereleases&label=version"
         alt="Maven central"></a>
  <a href=""><img src="https://img.shields.io/badge/JDA--Version-5.0.0--alpha.11-blue.svg"
              alt="JDA-Version "></a>
  <a href="http://www.apache.org/licenses/"><img src="https://img.shields.io/badge/license-Apache_2.0-blue.svg"
         alt="license Apache 2.0"></a>
</p>
<br>

## Versions

| Library     |                                         Version                                          |
|-------------|:----------------------------------------------------------------------------------------:|
| JDA         | <img src="https://img.shields.io/badge/5.0.0--alpha.11-orange.svg" alt="5.0.0-alpha.11"> |
| Spring Boot |        <img src="https://img.shields.io/badge/2.6.7-brightgreen.svg" alt="2.6.7">        |
| kotlin      |       <img src="https://img.shields.io/badge/1.6.21-brightgreen.svg" alt="1.6.21">       |
| gson        |        <img src="https://img.shields.io/badge/2.9.0-brightgreen.svg" alt="2.9.0">        |
| jnanoid     |        <img src="https://img.shields.io/badge/2.0.0-brightgreen.svg" alt="2.0.0">        |
| emoji-java  |        <img src="https://img.shields.io/badge/5.1.1-brightgreen.svg" alt="5.1.1">        |

## Quick Start

### Create a command

```kotlin
@Command
class PingCommand : DiscordCommand(
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
  discord:
    token: <insert your token here>

logging:
  level:
    io.viascom.discord.bot.aluna: DEBUG
    io.viascom.discord.bot.aluna.event.EventPublisher: INFO #Set to DEBUG to show all published events
    io.viascom.discord.bot.aluna.bot.handler.AlunaLocalizationFunction: INFO #Set to DEBUG to show translation keys for interactions
```
