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
package vip.mate.starter.sso.spi.model;

/**
 * JS-SDK config payload returned to the front end so it can call
 * {@code wx.config} / {@code wx.agentConfig} (WeChat Work H5 免登 / 能力调用).
 *
 * @param corpId    企业 ID(wx.config 的 appId,wx.agentConfig 的 corpid)
 * @param agentId   应用 AgentId(wx.agentConfig 需要)
 * @param timestamp 时间戳(秒)
 * @param nonceStr  随机串
 * @param signature SHA1 签名
 * @param url       签名所用的页面 URL
 *
 * @author mateaix
 */
public record JsSdkConfig(
        String corpId,
        String agentId,
        long timestamp,
        String nonceStr,
        String signature,
        String url) {
}
