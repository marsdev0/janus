package com.marsdev.janus.common;

import com.marsdev.janus.common.response.ApiResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResult<Void>> handleOther(Throwable e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(ErrorCode.INTERNAL_ERROR.getErrCode(), ErrorCode.INTERNAL_ERROR.getErrMsg()));
    }
}
