@file:JvmName("AlunaOptionData")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.model

import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

interface CommandOption<T>

class StringOption(name: String, description: String, isRequired: Boolean = false, isAutoComplete: Boolean = false) :
    OptionData(OptionType.STRING, name, description, isRequired, isAutoComplete), CommandOption<String?>

class IntegerOption(name: String, description: String, isRequired: Boolean = false, isAutoComplete: Boolean = false) :
    OptionData(OptionType.INTEGER, name, description, isRequired, isAutoComplete), CommandOption<Int?>

class NumberOption(name: String, description: String, isRequired: Boolean = false, isAutoComplete: Boolean = false) :
    OptionData(OptionType.NUMBER, name, description, isRequired, isAutoComplete), CommandOption<Double?>

class BooleanOption(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.BOOLEAN, name, description, isRequired), CommandOption<Boolean?>

class UserOption(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.USER, name, description, isRequired), CommandOption<User?>

class RoleOption(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.ROLE, name, description, isRequired), CommandOption<Role?>

class ChannelOption(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.CHANNEL, name, description, isRequired), CommandOption<Channel?>

class MentionableOption(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.MENTIONABLE, name, description, isRequired), CommandOption<IMentionable?>

class AttachmentOption(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.ATTACHMENT, name, description, isRequired), CommandOption<Attachment?>