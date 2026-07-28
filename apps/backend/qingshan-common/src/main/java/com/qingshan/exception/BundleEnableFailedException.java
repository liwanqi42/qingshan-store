package com.qingshan.exception;

/**
 * 套装启用失败异常
 */
public class BundleEnableFailedException extends BaseException {

    public BundleEnableFailedException(){}

    public BundleEnableFailedException(String msg){
        super(msg);
    }
}
