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
package vip.mate.system.admin.trigger.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vip.mate.base.result.Result;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 在线用户管理 — 基于 Sa-Token 会话
 *
 * @author mateaix
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/online-users")
@SaCheckLogin
public class OnlineUserController {

    /**
     * 在线用户列表
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        // 获取所有已登录的 loginId
        // Sa-Token searchSessionId 返回 Session key 列表，格式: "satoken:login:session:xxx"
        List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);

        List<Map<String, Object>> users = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Set<String> seenLoginIds = new HashSet<>();

        for (String sessionId : sessionIds) {
            try {
                SaSession session;
                try { session = StpUtil.getSessionBySessionId(sessionId); }
                catch (Exception ignored) { continue; }
                if (session == null) continue;

                String loginId = String.valueOf(session.getLoginId());
                // 去重：同一用户多设备只显示一次
                if (seenLoginIds.contains(loginId)) continue;
                seenLoginIds.add(loginId);

                String username = session.getString("username");
                String realName = session.getString("realName");

                // 关键词过滤
                if (keyword != null && !keyword.isBlank()) {
                    String kw = keyword.toLowerCase();
                    boolean match = (username != null && username.toLowerCase().contains(kw))
                            || (realName != null && realName.toLowerCase().contains(kw))
                            || loginId.contains(kw);
                    if (!match) continue;
                }

                // 获取该用户所有 token（多设备）。getTokenValueListByLoginId 返回的是
                // 插入顺序（最旧在前），且可能包含已失效但尚未 GC 的条目，所以这里：
                //  - 只统计仍有效（剩余 timeout > 0）的 token 作为“设备数”；
                //  - 取剩余有效期最大的那个（= 最新登录的设备）作为本行的代表，
                //    用于展示剩余有效期 / 登录时间，避免读到最旧 token 而显示“已过期”。
                List<String> tokens = StpUtil.getTokenValueListByLoginId(loginId);
                int activeCount = 0;
                String freshestToken = null;
                long maxTimeout = -1;
                for (String t : tokens) {
                    long to;
                    try { to = StpUtil.getTokenTimeout(t); }
                    catch (Exception ignored) { continue; }
                    if (to > 0) {
                        activeCount++;
                        if (to > maxTimeout) {
                            maxTimeout = to;
                            freshestToken = t;
                        }
                    }
                }

                // 登录时间：用最新 token 推算（now - 已用时长）。session.getCreateTime() 是
                // 账号会话的首次创建时间，多次登录会复用，不能代表本次登录时间。
                long configTimeout = SaManager.getConfig().getTimeout();
                long loginMillis;
                if (freshestToken != null && maxTimeout > 0 && configTimeout > 0) {
                    long elapsedSec = Math.max(0, configTimeout - maxTimeout);
                    loginMillis = System.currentTimeMillis() - elapsedSec * 1000L;
                } else {
                    loginMillis = session.getCreateTime();
                }

                Map<String, Object> user = new LinkedHashMap<>();
                user.put("userId", loginId);
                user.put("username", username != null ? username : "");
                user.put("realName", realName != null ? realName : "");
                user.put("tokenCount", activeCount);
                user.put("tokenValue", freshestToken != null ? freshestToken : "");
                user.put("loginTime", sdf.format(new Date(loginMillis)));
                user.put("timeout", maxTimeout);
                users.add(user);
            } catch (Exception e) {
                // 会话可能已过期，跳过
                log.debug("Skip invalid session: {}", sessionId);
            }
        }

        // 按登录时间倒序
        users.sort((a, b) -> String.valueOf(b.get("loginTime")).compareTo(String.valueOf(a.get("loginTime"))));

        // 手动分页
        int total = users.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", users.subList(from, to));
        result.put("total", total);
        return Result.ok(result);
    }

    /**
     * 强踢下线（按 loginId 踢出所有设备）
     */
    @PostMapping("/{userId}/kick")
    public Result<Void> kick(@PathVariable String userId) {
        StpUtil.kickout(userId);
        log.info("[admin] Kicked user offline: userId={}", userId);
        return Result.ok();
    }

    /**
     * 踢出指定 token（单设备）
     */
    @PostMapping("/kick-token")
    public Result<Void> kickToken(@RequestParam String tokenValue) {
        StpUtil.kickoutByTokenValue(tokenValue);
        log.info("[admin] Kicked token offline: {}", tokenValue);
        return Result.ok();
    }
}
