package com.ke.screencapture;

public interface ErrorMessage {
    String ERROR_PERMISSION_DENIED = "screen capture permission denied";
    String ERROR_PERMISSION_LOW_VERSION_SYSTEM = "the system version is lower than 5.0";
    String ERROR_PARAMS_ILLEGAL_START = "the start params illegal:";
    String ERROR_PREPARE_ENCODER = "prepare encoder exception:";
}
