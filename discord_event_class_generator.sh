#!/usr/bin/bash

# Copyright 2022 Viascom Ltd liab. Co
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

#sh ./discord_event_class_generator.sh ../JDA > src/main/kotlin/io/viascom/discord/bot/aluna/event/DiscordEvents.kt

set -e

PATH_TO_JDA=$1
EVENTS=$(gfind "$PATH_TO_JDA" -name '*Event.java' -type f -printf "%f\n" | cut -d'.' -f1 | awk '{print "class On"$1"(source: Any, @Nonnull val event: "$1") : ApplicationEvent(source)"}')
SORTED_EVENTS=$(echo "$EVENTS" | sort)

HEADER=$(cat <<-END
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
END
)

# shellcheck disable=SC2028
echo $"$HEADER\n\n\n$SORTED_EVENTS" | sed 's/event: GenericRoleUpdateEvent/event: GenericRoleUpdateEvent<*>/g' \
| sed 's/event: GenericEmojiUpdateEvent/event: GenericEmojiUpdateEvent<*>/g' \
| sed 's/event: GenericGuildStickerUpdateEvent/event: GenericGuildStickerUpdateEvent<*>/g' \
| sed 's/event: GenericChannelUpdateEvent/event: GenericChannelUpdateEvent<*>/g' \
| sed 's/event: GenericForumTagUpdateEvent/event: GenericForumTagUpdateEvent<*>/g' \
| sed 's/event: GenericSelfUpdateEvent/event: GenericSelfUpdateEvent<*>/g' \
| sed 's/event: GenericUserUpdateEvent/event: GenericUserUpdateEvent<*>/g' \
| sed 's/event: GenericStageInstanceUpdateEvent/event: GenericStageInstanceUpdateEvent<*>/g' \
| sed 's/event: UpdateEvent/event: UpdateEvent<*,*>/g' \
| sed 's/event: GenericContextInteractionEvent/event: GenericContextInteractionEvent<*>/g' \
| sed 's/event: GenericGuildUpdateEvent/event: GenericGuildUpdateEvent<*>/g' \
| sed 's/event: GenericGuildMemberUpdateEvent/event: GenericGuildMemberUpdateEvent<*>/g'