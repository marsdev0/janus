/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.common;

import com.marsdev.janus.common.response.ApiResult;
import com.marsdev.janus.reply.adapter.model.GatewayError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * @author geyan
 * @date 2026/7/31
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JanusException.class)
    public ResponseEntity<ApiResult<Void>> handleBiz(JanusException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResult.fail(e.getErrCode(), e.getErrMsg()));
    }

    /** 上游错误（已归一化为 GatewayError）：保留上游 HTTP 状态码 + 归一化错误码 + 原始 body */
    @ExceptionHandler(GatewayError.class)
    public ResponseEntity<ApiResult<Void>> handleGateway(GatewayError e) {
        String msg = e.getCode() + ": " + e.getRawBody();
        return ResponseEntity.status(e.getStatus())
                .body(ApiResult.fail(e.getStatus(), msg));
    }

    /** 上游连接失败/超时（未拿到 HTTP 响应）→ 502 Bad Gateway */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiResult<Void>> handleUpstreamConn(WebClientRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResult.fail(HttpStatus.BAD_GATEWAY.value(), "Upstream unreachable: " + e.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResult<Void>> handleOther(Throwable e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(ErrorCode.INTERNAL_ERROR.getErrCode(), ErrorCode.INTERNAL_ERROR.getErrMsg()));
    }
}
