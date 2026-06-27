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
package vip.mate.notice.infrastructure.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vip.mate.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.notice.domain.adapter.port.INoticeAdapterFactory;
import vip.mate.notice.infrastructure.adapter.email.EmailNoticeAdapter;
import vip.mate.notice.infrastructure.adapter.sms.SmsNoticeAdapter;
import vip.mate.notice.types.enums.NoticeChannel;

import java.util.EnumMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NoticeAdapterFactoryImpl implements INoticeAdapterFactory {

    private final SmsNoticeAdapter smsNoticeAdapter;
    private final EmailNoticeAdapter emailNoticeAdapter;

    private Map<NoticeChannel, INoticeAdapter> adapters;

    @jakarta.annotation.PostConstruct
    public void init() {
        adapters = new EnumMap<>(NoticeChannel.class);
        adapters.put(NoticeChannel.SMS, smsNoticeAdapter);
        adapters.put(NoticeChannel.EMAIL, emailNoticeAdapter);
    }

    @Override
    public INoticeAdapter getAdapter(NoticeChannel channel) {
        INoticeAdapter adapter = adapters.get(channel);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter for channel: " + channel);
        }
        return adapter;
    }
}
