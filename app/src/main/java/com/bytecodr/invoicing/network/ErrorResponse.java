package com.bytecodr.invoicing.network;

public class ErrorResponse {

    private boolean hasCustomDescription;
    private Code code;
    private String description;

    public ErrorResponse(Code code) {
        this.code = code;
    }

    public ErrorResponse(Code code, String description) {
        this.hasCustomDescription = true;
        this.code = code;
        this.description = description;
    }

    private static String getDefaultDescription(Code code) {
        switch (code) {

            case CODE_NO_ERROR: {
                return null;
            }

            case CODE_UNKNOWN: {
                return "Unknown error";
            }

            case CODE_GENERIC: {
                return "Error";
            }

            case CODE_BAD_NETWORK: {
                return "Network error";
            }

            default: {
                return null;
            }
        }
    }

    public Code getCode() {
        return code;
    }

    public String getDescription() {
        if (hasCustomDescription) {
            return description;
        } else {
            return getDefaultDescription(code);
        }
    }

    public enum Code {
        CODE_NO_ERROR,
        CODE_UNKNOWN,
        CODE_GENERIC,
        CODE_BAD_NETWORK
    }
}
