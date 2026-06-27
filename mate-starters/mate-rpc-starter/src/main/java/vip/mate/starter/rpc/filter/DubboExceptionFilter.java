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
package vip.mate.starter.rpc.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import vip.mate.base.exception.BizException;

/**
 * Dubbo provider-side exception filter.
 *
 * @author mateaix
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER)
public class DubboExceptionFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            Result result = invoker.invoke(invocation);
            if (result.hasException()) {
                Throwable exception = result.getException();
                if (exception instanceof BizException bizEx) {
                    log.warn("Dubbo BizException in {}.{}: [{}] {}",
                            invoker.getInterface().getSimpleName(),
                            invocation.getMethodName(),
                            bizEx.getCode(),
                            bizEx.getMsg());
                } else {
                    log.error("Dubbo unexpected exception in {}.{}: {}",
                            invoker.getInterface().getSimpleName(),
                            invocation.getMethodName(),
                            exception.getMessage(),
                            exception);
                }
            }
            return result;
        } catch (RpcException e) {
            log.error("Dubbo RpcException in {}.{}: {}",
                    invoker.getInterface().getSimpleName(),
                    invocation.getMethodName(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }
}
