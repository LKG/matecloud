/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.starter.sms.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vip.mate.starter.sms.spi.SmsSender;

/**
 * Holds all {@link SmsSender} providers keyed by {@link SmsSender#type()}.
 *
 * @author mateaix
 */
public class SmsSenderRegistry {

    private final Map<String, SmsSender> byType = new LinkedHashMap<>();

    public SmsSenderRegistry(List<SmsSender> senders) {
        if (senders != null) {
            for (SmsSender sender : senders) {
                byType.put(sender.type(), sender);
            }
        }
    }

    public SmsSender get(String type) {
        SmsSender sender = byType.get(type);
        if (sender == null) {
            throw new SmsException("No SMS provider registered for type '" + type
                    + "'. Registered: " + byType.keySet());
        }
        return sender;
    }

    public boolean has(String type) {
        return byType.containsKey(type);
    }

    /** All registered senders (for descriptor listing). */
    public Collection<SmsSender> all() {
        return byType.values();
    }
}
