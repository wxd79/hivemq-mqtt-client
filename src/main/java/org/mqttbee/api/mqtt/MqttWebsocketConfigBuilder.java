/*
 * Copyright 2018 The MQTT Bee project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mqttbee.api.mqtt;

import org.mqttbee.annotations.NotNull;
import org.mqttbee.mqtt.MqttWebsocketConfigImpl;


/**
 * @author Christian Hoff
 */
public class MqttWebsocketConfigBuilder {
    private String serverPath = "";


    @NotNull
    public MqttWebsocketConfigBuilder serverPath(@NotNull final String serverPath) {
        // remove any leading slashes
        this.serverPath = serverPath.replaceAll("^/+", "");
        return this;
    }

    @NotNull
    public MqttWebsocketConfig build() {
        return new MqttWebsocketConfigImpl(serverPath);
    }
}
