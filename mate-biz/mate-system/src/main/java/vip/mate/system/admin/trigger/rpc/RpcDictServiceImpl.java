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
package vip.mate.system.admin.trigger.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RedissonClient;
import vip.mate.system.admin.application.query.IDictQueryService;
import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.service.IRpcDictService;
import vip.mate.base.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@DubboService(version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
@RequiredArgsConstructor
public class RpcDictServiceImpl implements IRpcDictService {

    private final IDictQueryService dictQueryService;
    private final RedissonClient redissonClient;

    @Override
    public Result<String> getDictValue(String dictType, String dictCode) {
        return Result.ok(dictQueryService.findLabel(dictType, dictCode));
    }

    @Override
    public Result<List<Map<String, String>>> getDictListByType(String dictType) {
        List<Map<String, String>> list = dictQueryService.findByType(dictType).stream()
                .map(this::toMap)
                .toList();
        return Result.ok(list);
    }

    @Override
    public Result<Boolean> refreshDictCache(String dictType) {
        String pattern = dictType != null ? "dict:" + dictType + ":*" : "dict:*";
        redissonClient.getKeys().deleteByPattern(pattern);
        log.info("Dict cache evicted, pattern={}", pattern);
        return Result.ok(true);
    }

    private Map<String, String> toMap(DictData d) {
        Map<String, String> map = new HashMap<>(4);
        map.put("label", d.getDictLabel());
        map.put("value", d.getDictValue());
        return map;
    }
}
