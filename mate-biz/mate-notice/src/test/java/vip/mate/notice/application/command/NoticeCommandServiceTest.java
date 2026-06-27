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
package vip.mate.notice.application.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import vip.mate.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.notice.domain.adapter.port.INoticeAdapterFactory;
import vip.mate.notice.domain.adapter.repository.INoticeRepository;
import vip.mate.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;
import vip.mate.notice.types.enums.NoticeStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeCommandServiceTest {

    private INoticeRepository repository;
    private INoticeAdapterFactory factory;
    private INoticeAdapter adapter;
    private NoticeCommandService service;

    @BeforeEach
    void setUp() {
        repository = mock(INoticeRepository.class);
        factory = mock(INoticeAdapterFactory.class);
        adapter = mock(INoticeAdapter.class);
        service = new NoticeCommandService(repository, factory);
    }

    @Test
    void send_adapterSucceeds_marksSuccessAndPersistsTwice() {
        when(factory.getAdapter(NoticeChannel.SMS)).thenReturn(adapter);
        // Snapshot the aggregate's status at the moment save() is called —
        // ArgumentCaptor captures references, so by the time the test reads
        // the captured value the status has already been mutated to SUCCESS.
        java.util.concurrent.atomic.AtomicReference<NoticeStatus> statusAtSave =
                new java.util.concurrent.atomic.AtomicReference<>();
        org.mockito.Mockito.doAnswer(inv -> {
            NoticeAggregate agg = inv.getArgument(0);
            statusAtSave.set(agg.getStatus());
            return null;
        }).when(repository).save(any(NoticeAggregate.class));
        when(adapter.send(any(NoticeAggregate.class))).thenReturn(true);

        String id = service.send(NoticeChannel.SMS, "13800138000",
                BusinessType.VERIFY_CODE, "code 123456");

        assertNotNull(id);
        verify(repository).save(any(NoticeAggregate.class));
        ArgumentCaptor<NoticeAggregate> updateCaptor = ArgumentCaptor.forClass(NoticeAggregate.class);
        verify(repository).update(updateCaptor.capture());

        assertEquals(NoticeStatus.PENDING, statusAtSave.get());
        assertEquals(NoticeStatus.SUCCESS, updateCaptor.getValue().getStatus());
    }

    @Test
    void send_adapterReturnsFalse_marksFailed() {
        when(factory.getAdapter(NoticeChannel.SMS)).thenReturn(adapter);
        when(adapter.send(any(NoticeAggregate.class))).thenReturn(false);

        service.send(NoticeChannel.SMS, "13800138000",
                BusinessType.VERIFY_CODE, "code");

        ArgumentCaptor<NoticeAggregate> captor = ArgumentCaptor.forClass(NoticeAggregate.class);
        verify(repository).update(captor.capture());
        assertEquals(NoticeStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void send_adapterThrows_marksFailedWithErrorMessage() {
        when(factory.getAdapter(NoticeChannel.SMS)).thenReturn(adapter);
        when(adapter.send(any(NoticeAggregate.class)))
                .thenThrow(new RuntimeException("gateway timeout"));

        service.send(NoticeChannel.SMS, "13800138000",
                BusinessType.VERIFY_CODE, "code");

        ArgumentCaptor<NoticeAggregate> captor = ArgumentCaptor.forClass(NoticeAggregate.class);
        verify(repository).update(captor.capture());
        assertEquals(NoticeStatus.FAILED, captor.getValue().getStatus());
        assertEquals("gateway timeout", captor.getValue().getResultMessage());
    }

    @Test
    void retry_canRetry_incrementsAndDispatches() {
        NoticeAggregate failed = NoticeAggregate.create(
                NoticeChannel.SMS, "13800138000", BusinessType.VERIFY_CODE, "x");
        failed.markAsFailed("first attempt failed");

        when(repository.findById(failed.getId())).thenReturn(failed);
        when(factory.getAdapter(NoticeChannel.SMS)).thenReturn(adapter);
        when(adapter.send(any(NoticeAggregate.class))).thenReturn(true);

        service.retry(failed.getId());

        assertEquals(1, failed.getRetryCount());
        assertEquals(NoticeStatus.SUCCESS, failed.getStatus());
        verify(repository, times(1)).update(failed);
    }

    @Test
    void retry_unknownId_doesNothing() {
        when(repository.findById("missing")).thenReturn(null);

        service.retry("missing");

        verify(repository, never()).update(any());
    }
}
