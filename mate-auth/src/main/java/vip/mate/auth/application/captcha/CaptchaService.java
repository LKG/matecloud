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
package vip.mate.auth.application.captcha;

import com.xingyuv.captcha.model.common.ResponseModel;
import com.xingyuv.captcha.model.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

/**
 * Captcha verification service — delegates to xingyuv/captcha-plus for
 * slider-puzzle generation and trajectory verification.
 *
 * <p>The captcha-plus library auto-registers the challenge endpoints:
 * <ul>
 *   <li>GET  /captcha/get   — fetch background + jigsaw images + token</li>
 *   <li>POST /captcha/check — verify slider position, return captchaVerification</li>
 * </ul>
 * The gateway rewrites {@code /api/v1/auth/captcha/**} → {@code /captcha/**}
 * on mate-auth so the frontend uses a consistent path prefix.
 *
 * <p>At login time the client sends the one-time {@code captchaVerification}
 * token; this service validates it against the captcha-plus Redis store.
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final com.xingyuv.captcha.service.CaptchaService captchaPlusService;

    /**
     * Verify the one-time captchaVerification token produced by the frontend
     * after a successful slider interaction. Throws {@link BizException} when
     * the token is missing, expired, or tampered with.
     */
    public void verifyAndConsume(String captchaVerification) {
        if (captchaVerification == null || captchaVerification.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "请先完成滑块验证");
        }
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaVerification(captchaVerification);
        ResponseModel result = captchaPlusService.verification(vo);
        if (!"0000".equals(result.getRepCode())) {
            log.warn("[captcha] verification failed: code={} msg={}", result.getRepCode(), result.getRepMsg());
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(), "验证码校验失败，请重新验证");
        }
    }
}
