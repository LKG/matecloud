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
package vip.mate.base.constant;

import java.util.List;

/**
 * 网关 → 下游服务透传的认证上下文头 —— 全仓唯一出处。
 *
 * <p>三方必须引用同一份常量, 否则就是「网关发 A 名、服务读 B 名」的断线:</p>
 * <ul>
 *   <li><b>网关出口注入</b> ({@code HeaderRelayFilter}): 认证通过后按本清单写入下游请求头</li>
 *   <li><b>网关入口剥离</b> ({@code SecurityHeaderFilter}): 外部请求携带的同名头一律剥掉 (防伪造),
 *       新增头必须同步进 {@link #ALL}</li>
 *   <li><b>服务侧解析</b> (各服务拦截器): 只信网关注入的这套头</li>
 * </ul>
 *
 * @author mateaix
 */
public final class AuthHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_NAME = "X-User-Name";
    public static final String TENANT_ID = "X-Tenant-Id";
    /** 部门 (分权分域第二层)。 */
    public static final String DEPT_ID = "X-Dept-Id";
    /** 工作区 (分权分域第三层, 资源隔离单元)。 */
    public static final String WORKSPACE_ID = "X-Workspace-Id";
    /** 角色编码, 逗号分隔。 */
    public static final String ROLES = "X-Roles";
    /** 数据范围 (ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM)。 */
    public static final String DATA_SCOPE = "X-Data-Scope";

    /**
     * 链路追踪 ID。<b>不是</b>认证上下文, 故<b>不</b>进 {@link #ALL}:
     * <ul>
     *   <li>入口<b>不剥离</b> —— 允许前端/上游续传同一 traceId 做端到端关联;</li>
     *   <li>网关若缺失则生成, 并显式透传给下游 (见 {@code TracePropagationFilter}, 同时注入
     *       W3C {@code traceparent} 让下游 OTel 接续同一条 trace, 全链路单一 traceId);</li>
     *   <li>服务间 Dubbo 调用走 RpcContext attachment, 不走本头。</li>
     * </ul>
     */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 全部认证上下文头 —— 入口剥离与出口注入共用的同一份清单。 */
    public static final List<String> ALL = List.of(
            USER_ID, USER_NAME, TENANT_ID, DEPT_ID, WORKSPACE_ID, ROLES, DATA_SCOPE);

    private AuthHeaders() {
    }
}
