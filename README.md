<div align="center">
<img src="./logo.png"
         alt="Aluna Logo">
</div>

<h4 align="center">Fast and easy way to use JDA in your Spring-Boot Project</h4>

<p align="center">
  <a href="https://github.com/viascom/aluna-spring-boot-starter/releases"><img src="https://img.shields.io/maven-metadata/v.svg?label=maven-central&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fviascom%2Fdiscord%2Fbot%2Faluna-spring-boot-starter%2Fmaven-metadata.xml"
         alt="Maven central"></a>
  <a href=""><img src="https://img.shields.io/badge/JDA--Version-5.0.0--beta.2-blue.svg"
              alt="JDA-Version "></a>
  <a href="http://www.apache.org/licenses/"><img src="https://img.shields.io/badge/license-Apache_2.0-blue.svg"
         alt="license Apache 2.0"></a>
</p>
<br>

## Versions

*These are the versions of all exposed dependencies.*

| Library     |                                       Version                                       |
|-------------|:-----------------------------------------------------------------------------------:|
| JDA         | <img src="https://img.shields.io/badge/5.0.0-beta.2-yellow.svg" alt="5.0.0-beta.2"> |
| Spring Boot |     <img src="https://img.shields.io/badge/3.0.0-brightgreen.svg" alt="3.0.0">      |
| Kotlin      |    <img src="https://img.shields.io/badge/1.7.22-brightgreen.svg" alt="1.7.22">     |
| emoji-java  |     <img src="https://img.shields.io/badge/5.1.1-brightgreen.svg" alt="5.1.1">      |

## Download

Gradle:
```gradle
dependencies {
  implementation 'io.viascom.discord.bot:aluna-spring-boot-starter:0.0.38_5.0.0-beta.2'
}
```

Maven:
```xml
<dependency>
  <groupId>io.viascom.discord.bot</groupId>
    <artifactId>aluna-spring-boot-starter</artifactId>
    <version>0.0.38_5.0.0-beta.2</version>
</dependency>
```

## Getting Started

### Create a command

```kotlin
@Interaction
class PingCommand : DiscordCommand(
    "ping",
    "Send a ping"
) {
    override fun execute(event: SlashCommandInteractionEvent) {
        event.reply("Pong\nYour locale is:${this.userLocale}").queue()
    }
}
```

### React to a Button

```kotlin
event.reply("Pong\nYour locale is:${this.userLocale}").setComponents(ActionRow.of(primaryButton("hi", "Hi")))
    .queueAndRegisterInteraction(hook, this)


override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
    logger.debug(this.hashCode().toString())
    if (event.componentId == "hi") {
        event.editMessage("Oh hi :)").removeActionRows().queue()
    }

    return true
}
```

## Configuration Properties

### Basic configuration example

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

## Versioning

This project is developed by [Viascom](https://github.com/viascom) using the [Semantic Versioning specification](https://semver.org). For the versions available, see the [releases on this repository](https://github.com/viascom/aluna-spring-boot-starter/releases).

## Authors

* **Patrick Bösch** - *Initial work* - [itsmefox](https://github.com/itsmefox)
* **Nikola Stanković** - *Initial work* - [nik-sta](https://github.com/nik-sta)

See also the list of [contributors](https://github.com/viascom/aluna-spring-boot-starter/contributors) who participated in this project. 💕

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md) file.

If you like aluna-spring-boot-starter you can show support by starring ⭐ this repository.

# Licence
[Apache License, Version 2.0, January 2004](http://www.apache.org/licenses/LICENSE-2.0)
