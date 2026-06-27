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
package vip.mate.starter.excel.advice;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import vip.mate.starter.excel.annotation.ExcelExport;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Intercepts @ExcelExport return values and writes them as XLSX.
 *
 * @author mateaix
 */
@Slf4j
@ControllerAdvice
public class ExcelResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(ExcelExport.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {
        ExcelExport annotation = returnType.getMethodAnnotation(ExcelExport.class);
        if (annotation == null || !(body instanceof List<?> dataList)) {
            return body;
        }

        HttpServletResponse servletResponse =
                ((ServletServerHttpResponse) response).getServletResponse();

        String encodedFileName = URLEncoder.encode(annotation.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        servletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.setHeader("Content-Disposition",
                "attachment;filename=" + encodedFileName + ".xlsx");
        servletResponse.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try {
            EasyExcel.write(servletResponse.getOutputStream(), annotation.dataClass())
                    .sheet(annotation.sheetName())
                    .doWrite(dataList);
        } catch (IOException e) {
            log.error("Excel export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Excel export failed", e);
        }
        return null;
    }
}
