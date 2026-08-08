/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author geyan
 * @date 2026/7/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JanusException extends RuntimeException {

    private final int httpStatus;

    private final int errCode;

    private final String errMsg;

    public JanusException(ErrorCode errorCode) {
        super(errorCode.getErrMsg());
        this.httpStatus = errorCode.getHttpStatus();
        this.errCode = errorCode.getErrCode();
        this.errMsg = errorCode.getErrMsg();
    }

    public JanusException(ErrorCode errorCode, String errMsg) {
        super(errMsg);
        this.httpStatus = errorCode.getHttpStatus();
        this.errCode = errorCode.getErrCode();
        this.errMsg = errMsg;
    }
}
