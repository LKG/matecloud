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
package vip.mate.starter.sms.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.sms.annotation.SmsProvider;

/**
 * Base for {@link SmsSender} implementations: provider-type lookup (from
 * {@link SmsProvider}), a shared verification-code extractor, and admin-managed
 * config merging.
 *
 * @author mateaix
 */
public abstract class AbstractSmsSender implements SmsSender {

    /** Logical channel key used when consulting the {@link ChannelConfigStore}. */
    public static final String CHANNEL = "sms";

    private static final Pattern CODE_PATTERN = Pattern.compile("(\\d{4,6})");

    @Override
    public String type() {
        SmsProvider provider = getClass().getAnnotation(SmsProvider.class);
        return provider != null ? provider.value() : "unknown";
    }

    /** Extract the first 4–6 digit run from rendered content (verification-code fallback). */
    protected String extractCode(String content) {
        Matcher m = CODE_PATTERN.matcher(content == null ? "" : content);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Merge admin-managed config (from the {@link ChannelConfigStore}, if any) over
     * the provider's static defaults; non-blank stored values win.
     */
    protected Map<String, String> effectiveConfig(ChannelConfigStore store, Map<String, String> staticDefaults) {
        Map<String, String> merged = new LinkedHashMap<>(staticDefaults);
        if (store != null) {
            Map<String, String> stored = store.config(CHANNEL, type());
            if (stored != null) {
                stored.forEach((k, v) -> {
                    if (v != null && !v.isBlank()) {
                        merged.put(k, v);
                    }
                });
            }
        }
        return merged;
    }
}
