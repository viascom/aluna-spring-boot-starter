/*
 * Copyright 2022 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.viascom.discord.bot.aluna.event

import net.dv8tion.jda.api.events.*
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.GenericChannelEvent
import net.dv8tion.jda.api.events.channel.forum.ForumTagAddEvent
import net.dv8tion.jda.api.events.channel.forum.ForumTagRemoveEvent
import net.dv8tion.jda.api.events.channel.forum.GenericForumTagEvent
import net.dv8tion.jda.api.events.channel.forum.update.ForumTagUpdateEmojiEvent
import net.dv8tion.jda.api.events.channel.forum.update.ForumTagUpdateModeratedEvent
import net.dv8tion.jda.api.events.channel.forum.update.ForumTagUpdateNameEvent
import net.dv8tion.jda.api.events.channel.forum.update.GenericForumTagUpdateEvent
import net.dv8tion.jda.api.events.channel.update.*
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent
import net.dv8tion.jda.api.events.emoji.GenericEmojiEvent
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateNameEvent
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateRolesEvent
import net.dv8tion.jda.api.events.emoji.update.GenericEmojiUpdateEvent
import net.dv8tion.jda.api.events.guild.*
import net.dv8tion.jda.api.events.guild.invite.GenericGuildInviteEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent
import net.dv8tion.jda.api.events.guild.member.*
import net.dv8tion.jda.api.events.guild.member.update.*
import net.dv8tion.jda.api.events.guild.override.GenericPermissionOverrideEvent
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideUpdateEvent
import net.dv8tion.jda.api.events.guild.update.*
import net.dv8tion.jda.api.events.guild.voice.*
import net.dv8tion.jda.api.events.http.HttpRequestEvent
import net.dv8tion.jda.api.events.interaction.GenericAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.*
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.*
import net.dv8tion.jda.api.events.message.react.*
import net.dv8tion.jda.api.events.role.GenericRoleEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.role.RoleDeleteEvent
import net.dv8tion.jda.api.events.role.update.*
import net.dv8tion.jda.api.events.self.*
import net.dv8tion.jda.api.events.session.*
import net.dv8tion.jda.api.events.stage.GenericStageInstanceEvent
import net.dv8tion.jda.api.events.stage.StageInstanceCreateEvent
import net.dv8tion.jda.api.events.stage.StageInstanceDeleteEvent
import net.dv8tion.jda.api.events.stage.update.GenericStageInstanceUpdateEvent
import net.dv8tion.jda.api.events.stage.update.StageInstanceUpdatePrivacyLevelEvent
import net.dv8tion.jda.api.events.stage.update.StageInstanceUpdateTopicEvent
import net.dv8tion.jda.api.events.sticker.GenericGuildStickerEvent
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent
import net.dv8tion.jda.api.events.sticker.update.*
import net.dv8tion.jda.api.events.thread.GenericThreadEvent
import net.dv8tion.jda.api.events.thread.ThreadHiddenEvent
import net.dv8tion.jda.api.events.thread.ThreadRevealedEvent
import net.dv8tion.jda.api.events.thread.member.GenericThreadMemberEvent
import net.dv8tion.jda.api.events.thread.member.ThreadMemberJoinEvent
import net.dv8tion.jda.api.events.thread.member.ThreadMemberLeaveEvent
import net.dv8tion.jda.api.events.user.GenericUserEvent
import net.dv8tion.jda.api.events.user.UserActivityEndEvent
import net.dv8tion.jda.api.events.user.UserActivityStartEvent
import net.dv8tion.jda.api.events.user.UserTypingEvent
import net.dv8tion.jda.api.events.user.update.*
import net.dv8tion.jda.api.hooks.SubscribeEvent
import org.springframework.context.ApplicationEvent

class OnApplicationCommandUpdatePrivilegesEvent(source: Any, val event: ApplicationCommandUpdatePrivilegesEvent) : ApplicationEvent(source)
class OnApplicationUpdatePrivilegesEvent(source: Any, val event: ApplicationUpdatePrivilegesEvent) : ApplicationEvent(source)
class OnButtonInteractionEvent(source: Any, val event: ButtonInteractionEvent) : ApplicationEvent(source)
class OnChannelCreateEvent(source: Any, val event: ChannelCreateEvent) : ApplicationEvent(source)
class OnChannelDeleteEvent(source: Any, val event: ChannelDeleteEvent) : ApplicationEvent(source)
class OnChannelUpdateAppliedTagsEvent(source: Any, val event: ChannelUpdateAppliedTagsEvent) : ApplicationEvent(source)
class OnChannelUpdateArchiveTimestampEvent(source: Any, val event: ChannelUpdateArchiveTimestampEvent) : ApplicationEvent(source)
class OnChannelUpdateArchivedEvent(source: Any, val event: ChannelUpdateArchivedEvent) : ApplicationEvent(source)
class OnChannelUpdateAutoArchiveDurationEvent(source: Any, val event: ChannelUpdateAutoArchiveDurationEvent) : ApplicationEvent(source)
class OnChannelUpdateBitrateEvent(source: Any, val event: ChannelUpdateBitrateEvent) : ApplicationEvent(source)
class OnChannelUpdateDefaultReactionEvent(source: Any, val event: ChannelUpdateDefaultReactionEvent) : ApplicationEvent(source)
class OnChannelUpdateDefaultThreadSlowmodeEvent(source: Any, val event: ChannelUpdateDefaultThreadSlowmodeEvent) : ApplicationEvent(source)
class OnChannelUpdateFlagsEvent(source: Any, val event: ChannelUpdateFlagsEvent) : ApplicationEvent(source)
class OnChannelUpdateInvitableEvent(source: Any, val event: ChannelUpdateInvitableEvent) : ApplicationEvent(source)
class OnChannelUpdateLockedEvent(source: Any, val event: ChannelUpdateLockedEvent) : ApplicationEvent(source)
class OnChannelUpdateNSFWEvent(source: Any, val event: ChannelUpdateNSFWEvent) : ApplicationEvent(source)
class OnChannelUpdateNameEvent(source: Any, val event: ChannelUpdateNameEvent) : ApplicationEvent(source)
class OnChannelUpdateParentEvent(source: Any, val event: ChannelUpdateParentEvent) : ApplicationEvent(source)
class OnChannelUpdatePositionEvent(source: Any, val event: ChannelUpdatePositionEvent) : ApplicationEvent(source)
class OnChannelUpdateRegionEvent(source: Any, val event: ChannelUpdateRegionEvent) : ApplicationEvent(source)
class OnChannelUpdateSlowmodeEvent(source: Any, val event: ChannelUpdateSlowmodeEvent) : ApplicationEvent(source)
class OnChannelUpdateTopicEvent(source: Any, val event: ChannelUpdateTopicEvent) : ApplicationEvent(source)
class OnChannelUpdateTypeEvent(source: Any, val event: ChannelUpdateTypeEvent) : ApplicationEvent(source)
class OnChannelUpdateUserLimitEvent(source: Any, val event: ChannelUpdateUserLimitEvent) : ApplicationEvent(source)
class OnCommandAutoCompleteInteractionEvent(source: Any, val event: CommandAutoCompleteInteractionEvent) : ApplicationEvent(source)

@Deprecated(
    "Use OnSessionDisconnectEvent",
    replaceWith = ReplaceWith("OnSessionDisconnectEvent", "io.viascom.discord.bot.aluna.event.OnSessionDisconnectEvent"),
    DeprecationLevel.ERROR
)
class OnDisconnectEvent(source: Any, val event: SessionDisconnectEvent) : ApplicationEvent(source)
class OnEmojiAddedEvent(source: Any, val event: EmojiAddedEvent) : ApplicationEvent(source)
class OnEmojiRemovedEvent(source: Any, val event: EmojiRemovedEvent) : ApplicationEvent(source)
class OnEmojiUpdateNameEvent(source: Any, val event: EmojiUpdateNameEvent) : ApplicationEvent(source)
class OnEmojiUpdateRolesEvent(source: Any, val event: EmojiUpdateRolesEvent) : ApplicationEvent(source)
class OnEvent(source: Any, val event: Event) : ApplicationEvent(source)
class OnExceptionEvent(source: Any, val event: ExceptionEvent) : ApplicationEvent(source)
class OnForumTagAddEvent(source: Any, val event: ForumTagAddEvent) : ApplicationEvent(source)
class OnForumTagRemoveEvent(source: Any, val event: ForumTagRemoveEvent) : ApplicationEvent(source)
class OnForumTagUpdateEmojiEvent(source: Any, val event: ForumTagUpdateEmojiEvent) : ApplicationEvent(source)
class OnForumTagUpdateModeratedEvent(source: Any, val event: ForumTagUpdateModeratedEvent) : ApplicationEvent(source)
class OnForumTagUpdateNameEvent(source: Any, val event: ForumTagUpdateNameEvent) : ApplicationEvent(source)
class OnGatewayPingEvent(source: Any, val event: GatewayPingEvent) : ApplicationEvent(source)
class OnGenericAutoCompleteInteractionEvent(source: Any, val event: GenericAutoCompleteInteractionEvent) : ApplicationEvent(source)
class OnGenericChannelEvent(source: Any, val event: GenericChannelEvent) : ApplicationEvent(source)
class OnGenericChannelUpdateEvent(source: Any, val event: GenericChannelUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericCommandInteractionEvent(source: Any, val event: GenericCommandInteractionEvent) : ApplicationEvent(source)
class OnGenericComponentInteractionCreateEvent(source: Any, val event: GenericComponentInteractionCreateEvent) : ApplicationEvent(source)
class OnGenericContextInteractionEvent(source: Any, val event: GenericContextInteractionEvent<*>) : ApplicationEvent(source)
class OnGenericEmojiEvent(source: Any, val event: GenericEmojiEvent) : ApplicationEvent(source)
class OnGenericEmojiUpdateEvent(source: Any, val event: GenericEmojiUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericEvent(source: Any, val event: GenericEvent) : ApplicationEvent(source)
class OnGenericForumTagEvent(source: Any, val event: GenericForumTagEvent) : ApplicationEvent(source)
class OnGenericForumTagUpdateEvent(source: Any, val event: GenericForumTagUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildEvent(source: Any, val event: GenericGuildEvent) : ApplicationEvent(source)
class OnGenericGuildInviteEvent(source: Any, val event: GenericGuildInviteEvent) : ApplicationEvent(source)
class OnGenericGuildMemberEvent(source: Any, val event: GenericGuildMemberEvent) : ApplicationEvent(source)
class OnGenericGuildMemberUpdateEvent(source: Any, val event: GenericGuildMemberUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildStickerEvent(source: Any, val event: GenericGuildStickerEvent) : ApplicationEvent(source)
class OnGenericGuildStickerUpdateEvent(source: Any, val event: GenericGuildStickerUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildUpdateEvent(source: Any, val event: GenericGuildUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildVoiceEvent(source: Any, val event: GenericGuildVoiceEvent) : ApplicationEvent(source)

@Deprecated(
    "Use OnGuildVoiceUpdateEvent",
    replaceWith = ReplaceWith("OnGuildVoiceUpdateEvent", "io.viascom.discord.bot.aluna.event.OnGuildVoiceUpdateEvent"),
    DeprecationLevel.ERROR
)
class OnGenericGuildVoiceUpdateEvent(source: Any, val event: GuildVoiceUpdateEvent) : ApplicationEvent(source)
class OnGenericInteractionCreateEvent(source: Any, val event: GenericInteractionCreateEvent) : ApplicationEvent(source)
class OnGenericMessageEvent(source: Any, val event: GenericMessageEvent) : ApplicationEvent(source)
class OnGenericMessageReactionEvent(source: Any, val event: GenericMessageReactionEvent) : ApplicationEvent(source)
class OnGenericPermissionOverrideEvent(source: Any, val event: GenericPermissionOverrideEvent) : ApplicationEvent(source)
class OnGenericPrivilegeUpdateEvent(source: Any, val event: GenericPrivilegeUpdateEvent) : ApplicationEvent(source)
class OnGenericRoleEvent(source: Any, val event: GenericRoleEvent) : ApplicationEvent(source)
class OnGenericRoleUpdateEvent(source: Any, val event: GenericRoleUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericSelfUpdateEvent(source: Any, val event: GenericSelfUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericSessionEvent(source: Any, val event: GenericSessionEvent) : ApplicationEvent(source)
class OnGenericStageInstanceEvent(source: Any, val event: GenericStageInstanceEvent) : ApplicationEvent(source)
class OnGenericStageInstanceUpdateEvent(source: Any, val event: GenericStageInstanceUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericThreadEvent(source: Any, val event: GenericThreadEvent) : ApplicationEvent(source)
class OnGenericThreadMemberEvent(source: Any, val event: GenericThreadMemberEvent) : ApplicationEvent(source)
class OnGenericUserEvent(source: Any, val event: GenericUserEvent) : ApplicationEvent(source)
class OnGenericUserPresenceEvent(source: Any, val event: GenericUserPresenceEvent) : ApplicationEvent(source)
class OnGenericUserUpdateEvent(source: Any, val event: GenericUserUpdateEvent<*>) : ApplicationEvent(source)
class OnGuildAvailableEvent(source: Any, val event: GuildAvailableEvent) : ApplicationEvent(source)
class OnGuildBanEvent(source: Any, val event: GuildBanEvent) : ApplicationEvent(source)
class OnGuildInviteCreateEvent(source: Any, val event: GuildInviteCreateEvent) : ApplicationEvent(source)
class OnGuildInviteDeleteEvent(source: Any, val event: GuildInviteDeleteEvent) : ApplicationEvent(source)
class OnGuildJoinEvent(source: Any, val event: GuildJoinEvent) : ApplicationEvent(source)
class OnGuildLeaveEvent(source: Any, val event: GuildLeaveEvent) : ApplicationEvent(source)
class OnGuildMemberJoinEvent(source: Any, val event: GuildMemberJoinEvent) : ApplicationEvent(source)
class OnGuildMemberRemoveEvent(source: Any, val event: GuildMemberRemoveEvent) : ApplicationEvent(source)
class OnGuildMemberRoleAddEvent(source: Any, val event: GuildMemberRoleAddEvent) : ApplicationEvent(source)
class OnGuildMemberRoleRemoveEvent(source: Any, val event: GuildMemberRoleRemoveEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateAvatarEvent(source: Any, val event: GuildMemberUpdateAvatarEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateBoostTimeEvent(source: Any, val event: GuildMemberUpdateBoostTimeEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateEvent(source: Any, val event: GuildMemberUpdateEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateNicknameEvent(source: Any, val event: GuildMemberUpdateNicknameEvent) : ApplicationEvent(source)
class OnGuildMemberUpdatePendingEvent(source: Any, val event: GuildMemberUpdatePendingEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateTimeOutEvent(source: Any, val event: GuildMemberUpdateTimeOutEvent) : ApplicationEvent(source)
class OnGuildReadyEvent(source: Any, val event: GuildReadyEvent) : ApplicationEvent(source)
class OnGuildStickerAddedEvent(source: Any, val event: GuildStickerAddedEvent) : ApplicationEvent(source)
class OnGuildStickerRemovedEvent(source: Any, val event: GuildStickerRemovedEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateAvailableEvent(source: Any, val event: GuildStickerUpdateAvailableEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateDescriptionEvent(source: Any, val event: GuildStickerUpdateDescriptionEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateNameEvent(source: Any, val event: GuildStickerUpdateNameEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateTagsEvent(source: Any, val event: GuildStickerUpdateTagsEvent) : ApplicationEvent(source)
class OnGuildTimeoutEvent(source: Any, val event: GuildTimeoutEvent) : ApplicationEvent(source)
class OnGuildUnavailableEvent(source: Any, val event: GuildUnavailableEvent) : ApplicationEvent(source)
class OnGuildUnbanEvent(source: Any, val event: GuildUnbanEvent) : ApplicationEvent(source)
class OnGuildUpdateAfkChannelEvent(source: Any, val event: GuildUpdateAfkChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateAfkTimeoutEvent(source: Any, val event: GuildUpdateAfkTimeoutEvent) : ApplicationEvent(source)
class OnGuildUpdateBannerEvent(source: Any, val event: GuildUpdateBannerEvent) : ApplicationEvent(source)
class OnGuildUpdateBoostCountEvent(source: Any, val event: GuildUpdateBoostCountEvent) : ApplicationEvent(source)
class OnGuildUpdateBoostTierEvent(source: Any, val event: GuildUpdateBoostTierEvent) : ApplicationEvent(source)
class OnGuildUpdateCommunityUpdatesChannelEvent(source: Any, val event: GuildUpdateCommunityUpdatesChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateDescriptionEvent(source: Any, val event: GuildUpdateDescriptionEvent) : ApplicationEvent(source)
class OnGuildUpdateExplicitContentLevelEvent(source: Any, val event: GuildUpdateExplicitContentLevelEvent) : ApplicationEvent(source)
class OnGuildUpdateFeaturesEvent(source: Any, val event: GuildUpdateFeaturesEvent) : ApplicationEvent(source)
class OnGuildUpdateIconEvent(source: Any, val event: GuildUpdateIconEvent) : ApplicationEvent(source)
class OnGuildUpdateLocaleEvent(source: Any, val event: GuildUpdateLocaleEvent) : ApplicationEvent(source)
class OnGuildUpdateMFALevelEvent(source: Any, val event: GuildUpdateMFALevelEvent) : ApplicationEvent(source)
class OnGuildUpdateMaxMembersEvent(source: Any, val event: GuildUpdateMaxMembersEvent) : ApplicationEvent(source)
class OnGuildUpdateMaxPresencesEvent(source: Any, val event: GuildUpdateMaxPresencesEvent) : ApplicationEvent(source)
class OnGuildUpdateNSFWLevelEvent(source: Any, val event: GuildUpdateNSFWLevelEvent) : ApplicationEvent(source)
class OnGuildUpdateNameEvent(source: Any, val event: GuildUpdateNameEvent) : ApplicationEvent(source)
class OnGuildUpdateNotificationLevelEvent(source: Any, val event: GuildUpdateNotificationLevelEvent) : ApplicationEvent(source)
class OnGuildUpdateOwnerEvent(source: Any, val event: GuildUpdateOwnerEvent) : ApplicationEvent(source)
class OnGuildUpdateRulesChannelEvent(source: Any, val event: GuildUpdateRulesChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateSplashEvent(source: Any, val event: GuildUpdateSplashEvent) : ApplicationEvent(source)
class OnGuildUpdateSystemChannelEvent(source: Any, val event: GuildUpdateSystemChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateVanityCodeEvent(source: Any, val event: GuildUpdateVanityCodeEvent) : ApplicationEvent(source)
class OnGuildUpdateVerificationLevelEvent(source: Any, val event: GuildUpdateVerificationLevelEvent) : ApplicationEvent(source)
class OnGuildVoiceDeafenEvent(source: Any, val event: GuildVoiceDeafenEvent) : ApplicationEvent(source)
class OnGuildVoiceGuildDeafenEvent(source: Any, val event: GuildVoiceGuildDeafenEvent) : ApplicationEvent(source)
class OnGuildVoiceGuildMuteEvent(source: Any, val event: GuildVoiceGuildMuteEvent) : ApplicationEvent(source)

@Deprecated(
    "Use OnGuildVoiceUpdateEvent",
    replaceWith = ReplaceWith("OnGuildVoiceUpdateEvent", "io.viascom.discord.bot.aluna.event.OnGuildVoiceUpdateEvent"),
    DeprecationLevel.ERROR
)
class OnGuildVoiceJoinEvent(source: Any, val event: GuildVoiceUpdateEvent) : ApplicationEvent(source)

@Deprecated(
    "Use OnGuildVoiceUpdateEvent",
    replaceWith = ReplaceWith("OnGuildVoiceUpdateEvent", "io.viascom.discord.bot.aluna.event.OnGuildVoiceUpdateEvent"),
    DeprecationLevel.ERROR
)
class OnGuildVoiceLeaveEvent(source: Any, val event: GuildVoiceUpdateEvent) : ApplicationEvent(source)

@Deprecated(
    "Use OnGuildVoiceUpdateEvent",
    replaceWith = ReplaceWith("OnGuildVoiceUpdateEvent", "io.viascom.discord.bot.aluna.event.OnGuildVoiceUpdateEvent"),
    DeprecationLevel.ERROR
)
class OnGuildVoiceMoveEvent(source: Any, val event: GuildVoiceUpdateEvent) : ApplicationEvent(source)
class OnGuildVoiceMuteEvent(source: Any, val event: GuildVoiceMuteEvent) : ApplicationEvent(source)
class OnGuildVoiceRequestToSpeakEvent(source: Any, val event: GuildVoiceRequestToSpeakEvent) : ApplicationEvent(source)
class OnGuildVoiceSelfDeafenEvent(source: Any, val event: GuildVoiceSelfDeafenEvent) : ApplicationEvent(source)
class OnGuildVoiceSelfMuteEvent(source: Any, val event: GuildVoiceSelfMuteEvent) : ApplicationEvent(source)
class OnGuildVoiceStreamEvent(source: Any, val event: GuildVoiceStreamEvent) : ApplicationEvent(source)
class OnGuildVoiceSuppressEvent(source: Any, val event: GuildVoiceSuppressEvent) : ApplicationEvent(source)
class OnGuildVoiceUpdateEvent(source: Any, val event: GuildVoiceUpdateEvent) : ApplicationEvent(source)
class OnGuildVoiceVideoEvent(source: Any, val event: GuildVoiceVideoEvent) : ApplicationEvent(source)
class OnHttpRequestEvent(source: Any, val event: HttpRequestEvent) : ApplicationEvent(source)
class OnMessageBulkDeleteEvent(source: Any, val event: MessageBulkDeleteEvent) : ApplicationEvent(source)
class OnMessageContextInteractionEvent(source: Any, val event: MessageContextInteractionEvent) : ApplicationEvent(source)
class OnMessageDeleteEvent(source: Any, val event: MessageDeleteEvent) : ApplicationEvent(source)
class OnMessageEmbedEvent(source: Any, val event: MessageEmbedEvent) : ApplicationEvent(source)
class OnMessageReactionAddEvent(source: Any, val event: MessageReactionAddEvent) : ApplicationEvent(source)
class OnMessageReactionRemoveAllEvent(source: Any, val event: MessageReactionRemoveAllEvent) : ApplicationEvent(source)
class OnMessageReactionRemoveEmojiEvent(source: Any, val event: MessageReactionRemoveEmojiEvent) : ApplicationEvent(source)
class OnMessageReactionRemoveEvent(source: Any, val event: MessageReactionRemoveEvent) : ApplicationEvent(source)
class OnMessageReceivedEvent(source: Any, val event: MessageReceivedEvent) : ApplicationEvent(source)
class OnMessageUpdateEvent(source: Any, val event: MessageUpdateEvent) : ApplicationEvent(source)
class OnModalInteractionEvent(source: Any, val event: ModalInteractionEvent) : ApplicationEvent(source)
class OnPermissionOverrideCreateEvent(source: Any, val event: PermissionOverrideCreateEvent) : ApplicationEvent(source)
class OnPermissionOverrideDeleteEvent(source: Any, val event: PermissionOverrideDeleteEvent) : ApplicationEvent(source)
class OnPermissionOverrideUpdateEvent(source: Any, val event: PermissionOverrideUpdateEvent) : ApplicationEvent(source)
class OnRawGatewayEvent(source: Any, val event: RawGatewayEvent) : ApplicationEvent(source)
class OnReadyEvent(source: Any, val event: ReadyEvent) : ApplicationEvent(source)

@Deprecated(
    "Use OnSessionRecreateEvent",
    replaceWith = ReplaceWith("OnSessionRecreateEvent", "io.viascom.discord.bot.aluna.event.OnSessionRecreateEvent"),
    DeprecationLevel.ERROR
)
class OnReconnectedEvent(source: Any, val event: SessionRecreateEvent) : ApplicationEvent(source)

@Deprecated(
    "Use OnSessionResumeEvent",
    replaceWith = ReplaceWith("OnSessionResumeEvent", "io.viascom.discord.bot.aluna.event.OnSessionResumeEvent"),
    DeprecationLevel.ERROR
)
class OnResumedEvent(source: Any, val event: SessionResumeEvent) : ApplicationEvent(source)
class OnRoleCreateEvent(source: Any, val event: RoleCreateEvent) : ApplicationEvent(source)
class OnRoleDeleteEvent(source: Any, val event: RoleDeleteEvent) : ApplicationEvent(source)
class OnRoleUpdateColorEvent(source: Any, val event: RoleUpdateColorEvent) : ApplicationEvent(source)
class OnRoleUpdateHoistedEvent(source: Any, val event: RoleUpdateHoistedEvent) : ApplicationEvent(source)
class OnRoleUpdateIconEvent(source: Any, val event: RoleUpdateIconEvent) : ApplicationEvent(source)
class OnRoleUpdateMentionableEvent(source: Any, val event: RoleUpdateMentionableEvent) : ApplicationEvent(source)
class OnRoleUpdateNameEvent(source: Any, val event: RoleUpdateNameEvent) : ApplicationEvent(source)
class OnRoleUpdatePermissionsEvent(source: Any, val event: RoleUpdatePermissionsEvent) : ApplicationEvent(source)
class OnRoleUpdatePositionEvent(source: Any, val event: RoleUpdatePositionEvent) : ApplicationEvent(source)
class OnSelectMenuInteractionEvent(source: Any, val event: SelectMenuInteractionEvent) : ApplicationEvent(source)
class OnSelfUpdateAvatarEvent(source: Any, val event: SelfUpdateAvatarEvent) : ApplicationEvent(source)
class OnSelfUpdateDiscriminatorEvent(source: Any, val event: SelfUpdateDiscriminatorEvent) : ApplicationEvent(source)
class OnSelfUpdateMFAEvent(source: Any, val event: SelfUpdateMFAEvent) : ApplicationEvent(source)
class OnSelfUpdateNameEvent(source: Any, val event: SelfUpdateNameEvent) : ApplicationEvent(source)
class OnSelfUpdateVerifiedEvent(source: Any, val event: SelfUpdateVerifiedEvent) : ApplicationEvent(source)
class OnSessionDisconnectEvent(source: Any, val event: SessionDisconnectEvent) : ApplicationEvent(source)
class OnSessionInvalidateEvent(source: Any, val event: SessionInvalidateEvent) : ApplicationEvent(source)
class OnSessionRecreateEvent(source: Any, val event: SessionRecreateEvent) : ApplicationEvent(source)
class OnSessionResumeEvent(source: Any, val event: SessionResumeEvent) : ApplicationEvent(source)
class OnShutdownEvent(source: Any, val event: ShutdownEvent) : ApplicationEvent(source)
class OnSlashCommandInteractionEvent(source: Any, val event: SlashCommandInteractionEvent) : ApplicationEvent(source)
class OnStageInstanceCreateEvent(source: Any, val event: StageInstanceCreateEvent) : ApplicationEvent(source)
class OnStageInstanceDeleteEvent(source: Any, val event: StageInstanceDeleteEvent) : ApplicationEvent(source)
class OnStageInstanceUpdatePrivacyLevelEvent(source: Any, val event: StageInstanceUpdatePrivacyLevelEvent) : ApplicationEvent(source)
class OnStageInstanceUpdateTopicEvent(source: Any, val event: StageInstanceUpdateTopicEvent) : ApplicationEvent(source)
class OnStatusChangeEvent(source: Any, val event: StatusChangeEvent) : ApplicationEvent(source)
class OnSubscribeEvent(source: Any, val event: SubscribeEvent) : ApplicationEvent(source)
class OnThreadHiddenEvent(source: Any, val event: ThreadHiddenEvent) : ApplicationEvent(source)
class OnThreadMemberJoinEvent(source: Any, val event: ThreadMemberJoinEvent) : ApplicationEvent(source)
class OnThreadMemberLeaveEvent(source: Any, val event: ThreadMemberLeaveEvent) : ApplicationEvent(source)
class OnThreadRevealedEvent(source: Any, val event: ThreadRevealedEvent) : ApplicationEvent(source)
class OnUnavailableGuildJoinedEvent(source: Any, val event: UnavailableGuildJoinedEvent) : ApplicationEvent(source)
class OnUnavailableGuildLeaveEvent(source: Any, val event: UnavailableGuildLeaveEvent) : ApplicationEvent(source)
class OnUpdateEvent(source: Any, val event: UpdateEvent<*, *>) : ApplicationEvent(source)
class OnUserActivityEndEvent(source: Any, val event: UserActivityEndEvent) : ApplicationEvent(source)
class OnUserActivityStartEvent(source: Any, val event: UserActivityStartEvent) : ApplicationEvent(source)
class OnUserContextInteractionEvent(source: Any, val event: UserContextInteractionEvent) : ApplicationEvent(source)
class OnUserTypingEvent(source: Any, val event: UserTypingEvent) : ApplicationEvent(source)
class OnUserUpdateActivitiesEvent(source: Any, val event: UserUpdateActivitiesEvent) : ApplicationEvent(source)
class OnUserUpdateActivityOrderEvent(source: Any, val event: UserUpdateActivityOrderEvent) : ApplicationEvent(source)
class OnUserUpdateAvatarEvent(source: Any, val event: UserUpdateAvatarEvent) : ApplicationEvent(source)
class OnUserUpdateDiscriminatorEvent(source: Any, val event: UserUpdateDiscriminatorEvent) : ApplicationEvent(source)
class OnUserUpdateFlagsEvent(source: Any, val event: UserUpdateFlagsEvent) : ApplicationEvent(source)
class OnUserUpdateNameEvent(source: Any, val event: UserUpdateNameEvent) : ApplicationEvent(source)
class OnUserUpdateOnlineStatusEvent(source: Any, val event: UserUpdateOnlineStatusEvent) : ApplicationEvent(source)