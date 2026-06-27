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
package vip.mate.notice.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.notice.application.command.NoticeCommandService;
import vip.mate.notice.application.query.INoticeQueryService;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;

@RestController
@RequestMapping("/api/v1/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeCommandService noticeCommandService;
    private final INoticeQueryService noticeQueryService;

    /**
     * Notice-center record list — paginated, newest first, with optional
     * channel / status / businessType / target-keyword filters.
     */
    @GetMapping
    public Result<PageResult<INoticeQueryService.NoticeView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String keyword) {
        return Result.ok(noticeQueryService.page(pageNum, pageSize, channel, status, businessType, keyword));
    }

    @PostMapping("/send")
    public Result<String> send(@RequestParam NoticeChannel channel,
                               @RequestParam String target,
                               @RequestParam BusinessType businessType,
                               @RequestParam String content) {
        String id = noticeCommandService.send(channel, target, businessType, content);
        return Result.ok(id);
    }

    @PostMapping("/retry")
    public Result<Void> retry(@RequestParam String noticeId) {
        noticeCommandService.retry(noticeId);
        return Result.ok();
    }
}
