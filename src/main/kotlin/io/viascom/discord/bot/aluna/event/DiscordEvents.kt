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
import javax.annotation.Nonnull

class OnApplicationCommandUpdatePrivilegesEvent(source: Any, @Nonnull val event: ApplicationCommandUpdatePrivilegesEvent) : ApplicationEvent(source)
class OnApplicationUpdatePrivilegesEvent(source: Any, @Nonnull val event: ApplicationUpdatePrivilegesEvent) : ApplicationEvent(source)
class OnButtonInteractionEvent(source: Any, @Nonnull val event: ButtonInteractionEvent) : ApplicationEvent(source)
class OnChannelCreateEvent(source: Any, @Nonnull val event: ChannelCreateEvent) : ApplicationEvent(source)
class OnChannelDeleteEvent(source: Any, @Nonnull val event: ChannelDeleteEvent) : ApplicationEvent(source)
class OnChannelUpdateAppliedTagsEvent(source: Any, @Nonnull val event: ChannelUpdateAppliedTagsEvent) : ApplicationEvent(source)
class OnChannelUpdateArchiveTimestampEvent(source: Any, @Nonnull val event: ChannelUpdateArchiveTimestampEvent) : ApplicationEvent(source)
class OnChannelUpdateArchivedEvent(source: Any, @Nonnull val event: ChannelUpdateArchivedEvent) : ApplicationEvent(source)
class OnChannelUpdateAutoArchiveDurationEvent(source: Any, @Nonnull val event: ChannelUpdateAutoArchiveDurationEvent) : ApplicationEvent(source)
class OnChannelUpdateBitrateEvent(source: Any, @Nonnull val event: ChannelUpdateBitrateEvent) : ApplicationEvent(source)
class OnChannelUpdateDefaultReactionEvent(source: Any, @Nonnull val event: ChannelUpdateDefaultReactionEvent) : ApplicationEvent(source)
class OnChannelUpdateDefaultThreadSlowmodeEvent(source: Any, @Nonnull val event: ChannelUpdateDefaultThreadSlowmodeEvent) : ApplicationEvent(source)
class OnChannelUpdateFlagsEvent(source: Any, @Nonnull val event: ChannelUpdateFlagsEvent) : ApplicationEvent(source)
class OnChannelUpdateInvitableEvent(source: Any, @Nonnull val event: ChannelUpdateInvitableEvent) : ApplicationEvent(source)
class OnChannelUpdateLockedEvent(source: Any, @Nonnull val event: ChannelUpdateLockedEvent) : ApplicationEvent(source)
class OnChannelUpdateNSFWEvent(source: Any, @Nonnull val event: ChannelUpdateNSFWEvent) : ApplicationEvent(source)
class OnChannelUpdateNameEvent(source: Any, @Nonnull val event: ChannelUpdateNameEvent) : ApplicationEvent(source)
class OnChannelUpdateParentEvent(source: Any, @Nonnull val event: ChannelUpdateParentEvent) : ApplicationEvent(source)
class OnChannelUpdatePositionEvent(source: Any, @Nonnull val event: ChannelUpdatePositionEvent) : ApplicationEvent(source)
class OnChannelUpdateRegionEvent(source: Any, @Nonnull val event: ChannelUpdateRegionEvent) : ApplicationEvent(source)
class OnChannelUpdateSlowmodeEvent(source: Any, @Nonnull val event: ChannelUpdateSlowmodeEvent) : ApplicationEvent(source)
class OnChannelUpdateTopicEvent(source: Any, @Nonnull val event: ChannelUpdateTopicEvent) : ApplicationEvent(source)
class OnChannelUpdateTypeEvent(source: Any, @Nonnull val event: ChannelUpdateTypeEvent) : ApplicationEvent(source)
class OnChannelUpdateUserLimitEvent(source: Any, @Nonnull val event: ChannelUpdateUserLimitEvent) : ApplicationEvent(source)
class OnCommandAutoCompleteInteractionEvent(source: Any, @Nonnull val event: CommandAutoCompleteInteractionEvent) : ApplicationEvent(source)
class OnDisconnectEvent(source: Any, @Nonnull val event: DisconnectEvent) : ApplicationEvent(source)
class OnEmojiAddedEvent(source: Any, @Nonnull val event: EmojiAddedEvent) : ApplicationEvent(source)
class OnEmojiRemovedEvent(source: Any, @Nonnull val event: EmojiRemovedEvent) : ApplicationEvent(source)
class OnEmojiUpdateNameEvent(source: Any, @Nonnull val event: EmojiUpdateNameEvent) : ApplicationEvent(source)
class OnEmojiUpdateRolesEvent(source: Any, @Nonnull val event: EmojiUpdateRolesEvent) : ApplicationEvent(source)
class OnEvent(source: Any, @Nonnull val event: Event) : ApplicationEvent(source)
class OnExceptionEvent(source: Any, @Nonnull val event: ExceptionEvent) : ApplicationEvent(source)
class OnForumTagAddEvent(source: Any, @Nonnull val event: ForumTagAddEvent) : ApplicationEvent(source)
class OnForumTagRemoveEvent(source: Any, @Nonnull val event: ForumTagRemoveEvent) : ApplicationEvent(source)
class OnForumTagUpdateEmojiEvent(source: Any, @Nonnull val event: ForumTagUpdateEmojiEvent) : ApplicationEvent(source)
class OnForumTagUpdateModeratedEvent(source: Any, @Nonnull val event: ForumTagUpdateModeratedEvent) : ApplicationEvent(source)
class OnForumTagUpdateNameEvent(source: Any, @Nonnull val event: ForumTagUpdateNameEvent) : ApplicationEvent(source)
class OnGatewayPingEvent(source: Any, @Nonnull val event: GatewayPingEvent) : ApplicationEvent(source)
class OnGenericAutoCompleteInteractionEvent(source: Any, @Nonnull val event: GenericAutoCompleteInteractionEvent) : ApplicationEvent(source)
class OnGenericChannelEvent(source: Any, @Nonnull val event: GenericChannelEvent) : ApplicationEvent(source)
class OnGenericChannelUpdateEvent(source: Any, @Nonnull val event: GenericChannelUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericCommandInteractionEvent(source: Any, @Nonnull val event: GenericCommandInteractionEvent) : ApplicationEvent(source)
class OnGenericComponentInteractionCreateEvent(source: Any, @Nonnull val event: GenericComponentInteractionCreateEvent) : ApplicationEvent(source)
class OnGenericContextInteractionEvent(source: Any, @Nonnull val event: GenericContextInteractionEvent<*>) : ApplicationEvent(source)
class OnGenericEmojiEvent(source: Any, @Nonnull val event: GenericEmojiEvent) : ApplicationEvent(source)
class OnGenericEmojiUpdateEvent(source: Any, @Nonnull val event: GenericEmojiUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericEvent(source: Any, @Nonnull val event: GenericEvent) : ApplicationEvent(source)
class OnGenericForumTagEvent(source: Any, @Nonnull val event: GenericForumTagEvent) : ApplicationEvent(source)
class OnGenericForumTagUpdateEvent(source: Any, @Nonnull val event: GenericForumTagUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildEvent(source: Any, @Nonnull val event: GenericGuildEvent) : ApplicationEvent(source)
class OnGenericGuildInviteEvent(source: Any, @Nonnull val event: GenericGuildInviteEvent) : ApplicationEvent(source)
class OnGenericGuildMemberEvent(source: Any, @Nonnull val event: GenericGuildMemberEvent) : ApplicationEvent(source)
class OnGenericGuildMemberUpdateEvent(source: Any, @Nonnull val event: GenericGuildMemberUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildStickerEvent(source: Any, @Nonnull val event: GenericGuildStickerEvent) : ApplicationEvent(source)
class OnGenericGuildStickerUpdateEvent(source: Any, @Nonnull val event: GenericGuildStickerUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildUpdateEvent(source: Any, @Nonnull val event: GenericGuildUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericGuildVoiceEvent(source: Any, @Nonnull val event: GenericGuildVoiceEvent) : ApplicationEvent(source)
class OnGenericGuildVoiceUpdateEvent(source: Any, @Nonnull val event: GenericGuildVoiceUpdateEvent) : ApplicationEvent(source)
class OnGenericInteractionCreateEvent(source: Any, @Nonnull val event: GenericInteractionCreateEvent) : ApplicationEvent(source)
class OnGenericMessageEvent(source: Any, @Nonnull val event: GenericMessageEvent) : ApplicationEvent(source)
class OnGenericMessageReactionEvent(source: Any, @Nonnull val event: GenericMessageReactionEvent) : ApplicationEvent(source)
class OnGenericPermissionOverrideEvent(source: Any, @Nonnull val event: GenericPermissionOverrideEvent) : ApplicationEvent(source)
class OnGenericPrivilegeUpdateEvent(source: Any, @Nonnull val event: GenericPrivilegeUpdateEvent) : ApplicationEvent(source)
class OnGenericRoleEvent(source: Any, @Nonnull val event: GenericRoleEvent) : ApplicationEvent(source)
class OnGenericRoleUpdateEvent(source: Any, @Nonnull val event: GenericRoleUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericSelfUpdateEvent(source: Any, @Nonnull val event: GenericSelfUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericStageInstanceEvent(source: Any, @Nonnull val event: GenericStageInstanceEvent) : ApplicationEvent(source)
class OnGenericStageInstanceUpdateEvent(source: Any, @Nonnull val event: GenericStageInstanceUpdateEvent<*>) : ApplicationEvent(source)
class OnGenericThreadEvent(source: Any, @Nonnull val event: GenericThreadEvent) : ApplicationEvent(source)
class OnGenericThreadMemberEvent(source: Any, @Nonnull val event: GenericThreadMemberEvent) : ApplicationEvent(source)
class OnGenericUserEvent(source: Any, @Nonnull val event: GenericUserEvent) : ApplicationEvent(source)
class OnGenericUserPresenceEvent(source: Any, @Nonnull val event: GenericUserPresenceEvent) : ApplicationEvent(source)
class OnGenericUserUpdateEvent(source: Any, @Nonnull val event: GenericUserUpdateEvent<*>) : ApplicationEvent(source)
class OnGuildAvailableEvent(source: Any, @Nonnull val event: GuildAvailableEvent) : ApplicationEvent(source)
class OnGuildBanEvent(source: Any, @Nonnull val event: GuildBanEvent) : ApplicationEvent(source)
class OnGuildInviteCreateEvent(source: Any, @Nonnull val event: GuildInviteCreateEvent) : ApplicationEvent(source)
class OnGuildInviteDeleteEvent(source: Any, @Nonnull val event: GuildInviteDeleteEvent) : ApplicationEvent(source)
class OnGuildJoinEvent(source: Any, @Nonnull val event: GuildJoinEvent) : ApplicationEvent(source)
class OnGuildLeaveEvent(source: Any, @Nonnull val event: GuildLeaveEvent) : ApplicationEvent(source)
class OnGuildMemberJoinEvent(source: Any, @Nonnull val event: GuildMemberJoinEvent) : ApplicationEvent(source)
class OnGuildMemberRemoveEvent(source: Any, @Nonnull val event: GuildMemberRemoveEvent) : ApplicationEvent(source)
class OnGuildMemberRoleAddEvent(source: Any, @Nonnull val event: GuildMemberRoleAddEvent) : ApplicationEvent(source)
class OnGuildMemberRoleRemoveEvent(source: Any, @Nonnull val event: GuildMemberRoleRemoveEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateAvatarEvent(source: Any, @Nonnull val event: GuildMemberUpdateAvatarEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateBoostTimeEvent(source: Any, @Nonnull val event: GuildMemberUpdateBoostTimeEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateEvent(source: Any, @Nonnull val event: GuildMemberUpdateEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateNicknameEvent(source: Any, @Nonnull val event: GuildMemberUpdateNicknameEvent) : ApplicationEvent(source)
class OnGuildMemberUpdatePendingEvent(source: Any, @Nonnull val event: GuildMemberUpdatePendingEvent) : ApplicationEvent(source)
class OnGuildMemberUpdateTimeOutEvent(source: Any, @Nonnull val event: GuildMemberUpdateTimeOutEvent) : ApplicationEvent(source)
class OnGuildReadyEvent(source: Any, @Nonnull val event: GuildReadyEvent) : ApplicationEvent(source)
class OnGuildStickerAddedEvent(source: Any, @Nonnull val event: GuildStickerAddedEvent) : ApplicationEvent(source)
class OnGuildStickerRemovedEvent(source: Any, @Nonnull val event: GuildStickerRemovedEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateAvailableEvent(source: Any, @Nonnull val event: GuildStickerUpdateAvailableEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateDescriptionEvent(source: Any, @Nonnull val event: GuildStickerUpdateDescriptionEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateNameEvent(source: Any, @Nonnull val event: GuildStickerUpdateNameEvent) : ApplicationEvent(source)
class OnGuildStickerUpdateTagsEvent(source: Any, @Nonnull val event: GuildStickerUpdateTagsEvent) : ApplicationEvent(source)
class OnGuildTimeoutEvent(source: Any, @Nonnull val event: GuildTimeoutEvent) : ApplicationEvent(source)
class OnGuildUnavailableEvent(source: Any, @Nonnull val event: GuildUnavailableEvent) : ApplicationEvent(source)
class OnGuildUnbanEvent(source: Any, @Nonnull val event: GuildUnbanEvent) : ApplicationEvent(source)
class OnGuildUpdateAfkChannelEvent(source: Any, @Nonnull val event: GuildUpdateAfkChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateAfkTimeoutEvent(source: Any, @Nonnull val event: GuildUpdateAfkTimeoutEvent) : ApplicationEvent(source)
class OnGuildUpdateBannerEvent(source: Any, @Nonnull val event: GuildUpdateBannerEvent) : ApplicationEvent(source)
class OnGuildUpdateBoostCountEvent(source: Any, @Nonnull val event: GuildUpdateBoostCountEvent) : ApplicationEvent(source)
class OnGuildUpdateBoostTierEvent(source: Any, @Nonnull val event: GuildUpdateBoostTierEvent) : ApplicationEvent(source)
class OnGuildUpdateCommunityUpdatesChannelEvent(source: Any, @Nonnull val event: GuildUpdateCommunityUpdatesChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateDescriptionEvent(source: Any, @Nonnull val event: GuildUpdateDescriptionEvent) : ApplicationEvent(source)
class OnGuildUpdateExplicitContentLevelEvent(source: Any, @Nonnull val event: GuildUpdateExplicitContentLevelEvent) : ApplicationEvent(source)
class OnGuildUpdateFeaturesEvent(source: Any, @Nonnull val event: GuildUpdateFeaturesEvent) : ApplicationEvent(source)
class OnGuildUpdateIconEvent(source: Any, @Nonnull val event: GuildUpdateIconEvent) : ApplicationEvent(source)
class OnGuildUpdateLocaleEvent(source: Any, @Nonnull val event: GuildUpdateLocaleEvent) : ApplicationEvent(source)
class OnGuildUpdateMFALevelEvent(source: Any, @Nonnull val event: GuildUpdateMFALevelEvent) : ApplicationEvent(source)
class OnGuildUpdateMaxMembersEvent(source: Any, @Nonnull val event: GuildUpdateMaxMembersEvent) : ApplicationEvent(source)
class OnGuildUpdateMaxPresencesEvent(source: Any, @Nonnull val event: GuildUpdateMaxPresencesEvent) : ApplicationEvent(source)
class OnGuildUpdateNSFWLevelEvent(source: Any, @Nonnull val event: GuildUpdateNSFWLevelEvent) : ApplicationEvent(source)
class OnGuildUpdateNameEvent(source: Any, @Nonnull val event: GuildUpdateNameEvent) : ApplicationEvent(source)
class OnGuildUpdateNotificationLevelEvent(source: Any, @Nonnull val event: GuildUpdateNotificationLevelEvent) : ApplicationEvent(source)
class OnGuildUpdateOwnerEvent(source: Any, @Nonnull val event: GuildUpdateOwnerEvent) : ApplicationEvent(source)
class OnGuildUpdateRulesChannelEvent(source: Any, @Nonnull val event: GuildUpdateRulesChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateSplashEvent(source: Any, @Nonnull val event: GuildUpdateSplashEvent) : ApplicationEvent(source)
class OnGuildUpdateSystemChannelEvent(source: Any, @Nonnull val event: GuildUpdateSystemChannelEvent) : ApplicationEvent(source)
class OnGuildUpdateVanityCodeEvent(source: Any, @Nonnull val event: GuildUpdateVanityCodeEvent) : ApplicationEvent(source)
class OnGuildUpdateVerificationLevelEvent(source: Any, @Nonnull val event: GuildUpdateVerificationLevelEvent) : ApplicationEvent(source)
class OnGuildVoiceDeafenEvent(source: Any, @Nonnull val event: GuildVoiceDeafenEvent) : ApplicationEvent(source)
class OnGuildVoiceGuildDeafenEvent(source: Any, @Nonnull val event: GuildVoiceGuildDeafenEvent) : ApplicationEvent(source)
class OnGuildVoiceGuildMuteEvent(source: Any, @Nonnull val event: GuildVoiceGuildMuteEvent) : ApplicationEvent(source)
class OnGuildVoiceJoinEvent(source: Any, @Nonnull val event: GuildVoiceJoinEvent) : ApplicationEvent(source)
class OnGuildVoiceLeaveEvent(source: Any, @Nonnull val event: GuildVoiceLeaveEvent) : ApplicationEvent(source)
class OnGuildVoiceMoveEvent(source: Any, @Nonnull val event: GuildVoiceMoveEvent) : ApplicationEvent(source)
class OnGuildVoiceMuteEvent(source: Any, @Nonnull val event: GuildVoiceMuteEvent) : ApplicationEvent(source)
class OnGuildVoiceRequestToSpeakEvent(source: Any, @Nonnull val event: GuildVoiceRequestToSpeakEvent) : ApplicationEvent(source)
class OnGuildVoiceSelfDeafenEvent(source: Any, @Nonnull val event: GuildVoiceSelfDeafenEvent) : ApplicationEvent(source)
class OnGuildVoiceSelfMuteEvent(source: Any, @Nonnull val event: GuildVoiceSelfMuteEvent) : ApplicationEvent(source)
class OnGuildVoiceStreamEvent(source: Any, @Nonnull val event: GuildVoiceStreamEvent) : ApplicationEvent(source)
class OnGuildVoiceSuppressEvent(source: Any, @Nonnull val event: GuildVoiceSuppressEvent) : ApplicationEvent(source)
class OnGuildVoiceUpdateEvent(source: Any, @Nonnull val event: GuildVoiceUpdateEvent) : ApplicationEvent(source)
class OnGuildVoiceVideoEvent(source: Any, @Nonnull val event: GuildVoiceVideoEvent) : ApplicationEvent(source)
class OnHttpRequestEvent(source: Any, @Nonnull val event: HttpRequestEvent) : ApplicationEvent(source)
class OnMessageBulkDeleteEvent(source: Any, @Nonnull val event: MessageBulkDeleteEvent) : ApplicationEvent(source)
class OnMessageContextInteractionEvent(source: Any, @Nonnull val event: MessageContextInteractionEvent) : ApplicationEvent(source)
class OnMessageDeleteEvent(source: Any, @Nonnull val event: MessageDeleteEvent) : ApplicationEvent(source)
class OnMessageEmbedEvent(source: Any, @Nonnull val event: MessageEmbedEvent) : ApplicationEvent(source)
class OnMessageReactionAddEvent(source: Any, @Nonnull val event: MessageReactionAddEvent) : ApplicationEvent(source)
class OnMessageReactionRemoveAllEvent(source: Any, @Nonnull val event: MessageReactionRemoveAllEvent) : ApplicationEvent(source)
class OnMessageReactionRemoveEmojiEvent(source: Any, @Nonnull val event: MessageReactionRemoveEmojiEvent) : ApplicationEvent(source)
class OnMessageReactionRemoveEvent(source: Any, @Nonnull val event: MessageReactionRemoveEvent) : ApplicationEvent(source)
class OnMessageReceivedEvent(source: Any, @Nonnull val event: MessageReceivedEvent) : ApplicationEvent(source)
class OnMessageUpdateEvent(source: Any, @Nonnull val event: MessageUpdateEvent) : ApplicationEvent(source)
class OnModalInteractionEvent(source: Any, @Nonnull val event: ModalInteractionEvent) : ApplicationEvent(source)
class OnPermissionOverrideCreateEvent(source: Any, @Nonnull val event: PermissionOverrideCreateEvent) : ApplicationEvent(source)
class OnPermissionOverrideDeleteEvent(source: Any, @Nonnull val event: PermissionOverrideDeleteEvent) : ApplicationEvent(source)
class OnPermissionOverrideUpdateEvent(source: Any, @Nonnull val event: PermissionOverrideUpdateEvent) : ApplicationEvent(source)
class OnRawGatewayEvent(source: Any, @Nonnull val event: RawGatewayEvent) : ApplicationEvent(source)
class OnReadyEvent(source: Any, @Nonnull val event: ReadyEvent) : ApplicationEvent(source)
class OnReconnectedEvent(source: Any, @Nonnull val event: ReconnectedEvent) : ApplicationEvent(source)
class OnResumedEvent(source: Any, @Nonnull val event: ResumedEvent) : ApplicationEvent(source)
class OnRoleCreateEvent(source: Any, @Nonnull val event: RoleCreateEvent) : ApplicationEvent(source)
class OnRoleDeleteEvent(source: Any, @Nonnull val event: RoleDeleteEvent) : ApplicationEvent(source)
class OnRoleUpdateColorEvent(source: Any, @Nonnull val event: RoleUpdateColorEvent) : ApplicationEvent(source)
class OnRoleUpdateHoistedEvent(source: Any, @Nonnull val event: RoleUpdateHoistedEvent) : ApplicationEvent(source)
class OnRoleUpdateIconEvent(source: Any, @Nonnull val event: RoleUpdateIconEvent) : ApplicationEvent(source)
class OnRoleUpdateMentionableEvent(source: Any, @Nonnull val event: RoleUpdateMentionableEvent) : ApplicationEvent(source)
class OnRoleUpdateNameEvent(source: Any, @Nonnull val event: RoleUpdateNameEvent) : ApplicationEvent(source)
class OnRoleUpdatePermissionsEvent(source: Any, @Nonnull val event: RoleUpdatePermissionsEvent) : ApplicationEvent(source)
class OnRoleUpdatePositionEvent(source: Any, @Nonnull val event: RoleUpdatePositionEvent) : ApplicationEvent(source)
class OnSelectMenuInteractionEvent(source: Any, @Nonnull val event: SelectMenuInteractionEvent) : ApplicationEvent(source)
class OnSelfUpdateAvatarEvent(source: Any, @Nonnull val event: SelfUpdateAvatarEvent) : ApplicationEvent(source)
class OnSelfUpdateDiscriminatorEvent(source: Any, @Nonnull val event: SelfUpdateDiscriminatorEvent) : ApplicationEvent(source)
class OnSelfUpdateMFAEvent(source: Any, @Nonnull val event: SelfUpdateMFAEvent) : ApplicationEvent(source)
class OnSelfUpdateNameEvent(source: Any, @Nonnull val event: SelfUpdateNameEvent) : ApplicationEvent(source)
class OnSelfUpdateVerifiedEvent(source: Any, @Nonnull val event: SelfUpdateVerifiedEvent) : ApplicationEvent(source)
class OnShutdownEvent(source: Any, @Nonnull val event: ShutdownEvent) : ApplicationEvent(source)
class OnSlashCommandInteractionEvent(source: Any, @Nonnull val event: SlashCommandInteractionEvent) : ApplicationEvent(source)
class OnStageInstanceCreateEvent(source: Any, @Nonnull val event: StageInstanceCreateEvent) : ApplicationEvent(source)
class OnStageInstanceDeleteEvent(source: Any, @Nonnull val event: StageInstanceDeleteEvent) : ApplicationEvent(source)
class OnStageInstanceUpdatePrivacyLevelEvent(source: Any, @Nonnull val event: StageInstanceUpdatePrivacyLevelEvent) : ApplicationEvent(source)
class OnStageInstanceUpdateTopicEvent(source: Any, @Nonnull val event: StageInstanceUpdateTopicEvent) : ApplicationEvent(source)
class OnStatusChangeEvent(source: Any, @Nonnull val event: StatusChangeEvent) : ApplicationEvent(source)
class OnSubscribeEvent(source: Any, @Nonnull val event: SubscribeEvent) : ApplicationEvent(source)
class OnThreadHiddenEvent(source: Any, @Nonnull val event: ThreadHiddenEvent) : ApplicationEvent(source)
class OnThreadMemberJoinEvent(source: Any, @Nonnull val event: ThreadMemberJoinEvent) : ApplicationEvent(source)
class OnThreadMemberLeaveEvent(source: Any, @Nonnull val event: ThreadMemberLeaveEvent) : ApplicationEvent(source)
class OnThreadRevealedEvent(source: Any, @Nonnull val event: ThreadRevealedEvent) : ApplicationEvent(source)
class OnUnavailableGuildJoinedEvent(source: Any, @Nonnull val event: UnavailableGuildJoinedEvent) : ApplicationEvent(source)
class OnUnavailableGuildLeaveEvent(source: Any, @Nonnull val event: UnavailableGuildLeaveEvent) : ApplicationEvent(source)
class OnUpdateEvent(source: Any, @Nonnull val event: UpdateEvent<*, *>) : ApplicationEvent(source)
class OnUserActivityEndEvent(source: Any, @Nonnull val event: UserActivityEndEvent) : ApplicationEvent(source)
class OnUserActivityStartEvent(source: Any, @Nonnull val event: UserActivityStartEvent) : ApplicationEvent(source)
class OnUserContextInteractionEvent(source: Any, @Nonnull val event: UserContextInteractionEvent) : ApplicationEvent(source)
class OnUserTypingEvent(source: Any, @Nonnull val event: UserTypingEvent) : ApplicationEvent(source)
class OnUserUpdateActivitiesEvent(source: Any, @Nonnull val event: UserUpdateActivitiesEvent) : ApplicationEvent(source)
class OnUserUpdateActivityOrderEvent(source: Any, @Nonnull val event: UserUpdateActivityOrderEvent) : ApplicationEvent(source)
class OnUserUpdateAvatarEvent(source: Any, @Nonnull val event: UserUpdateAvatarEvent) : ApplicationEvent(source)
class OnUserUpdateDiscriminatorEvent(source: Any, @Nonnull val event: UserUpdateDiscriminatorEvent) : ApplicationEvent(source)
class OnUserUpdateFlagsEvent(source: Any, @Nonnull val event: UserUpdateFlagsEvent) : ApplicationEvent(source)
class OnUserUpdateNameEvent(source: Any, @Nonnull val event: UserUpdateNameEvent) : ApplicationEvent(source)
class OnUserUpdateOnlineStatusEvent(source: Any, @Nonnull val event: UserUpdateOnlineStatusEvent) : ApplicationEvent(source)