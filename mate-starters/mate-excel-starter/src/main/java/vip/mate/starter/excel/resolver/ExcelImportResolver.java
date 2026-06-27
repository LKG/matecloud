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
package vip.mate.starter.excel.resolver;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import vip.mate.base.exception.BizException;
import vip.mate.starter.excel.annotation.ExcelImport;

import java.io.InputStream;
import java.util.List;

/**
 * Resolves controller method parameters annotated with @ExcelImport.
 *
 * @author mateaix
 */
@Slf4j
public class ExcelImportResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExcelImport.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        ExcelImport annotation = parameter.getParameterAnnotation(ExcelImport.class);
        if (annotation == null) {
            throw new BizException("EXCEL_IMPORT_ERROR", "Missing @ExcelImport annotation");
        }

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BizException("EXCEL_IMPORT_ERROR", "Not an HTTP request");
        }

        MultipartFile file = resolveMultipartFile(request);
        if (file == null || file.isEmpty()) {
            throw new BizException("EXCEL_IMPORT_ERROR", "No file uploaded");
        }

        try (InputStream is = file.getInputStream()) {
            List<?> data = EasyExcel.read(is)
                    .head(annotation.dataClass())
                    .headRowNumber(annotation.headRowNumber())
                    .sheet()
                    .doReadSync();
            log.debug("Excel import: read {} rows from {}", data.size(), file.getOriginalFilename());
            return data;
        }
    }

    private MultipartFile resolveMultipartFile(HttpServletRequest request) {
        if (request instanceof MultipartRequest multipartRequest) {
            var fileMap = multipartRequest.getMultiFileMap();
            if (!fileMap.isEmpty()) {
                List<MultipartFile> files = fileMap.values().iterator().next();
                if (!files.isEmpty()) {
                    return files.getFirst();
                }
            }
        }
        return null;
    }
}
