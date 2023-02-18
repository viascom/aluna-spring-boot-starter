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

package io.viascom.discord.bot.aluna.bot

import java.time.Duration

interface InteractionScopedObject {
    /**
     * Unique id for this object.
     * It's recommended to use NanoId.generate()
     */
    var uniqueId: String

    /**
     * Bean timout delay before it gets destroyed
     */
    var beanTimoutDelay: Duration

    /**
     * Should interaction execution use the bean created during auto complete request if present.
     */
    var beanUseAutoCompleteBean: Boolean

    /**
     * Should observers be removed if the bean gets destroyed.
     */
    var beanRemoveObserverOnDestroy: Boolean

    /**
     * Should observer timeouts be reset if bean timeout gets reset.
     */
    var beanResetObserverTimeoutOnBeanExtend: Boolean

    /**
     * Should onDestroy be called if the bean gets destroyed.
     */
    var beanCallOnDestroy: Boolean

    /**
     * If true, this instance got freshly created by Spring
     */
    var freshInstance: Boolean

}
