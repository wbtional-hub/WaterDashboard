package com.waterdashboard.sqlsecurity;

public class SqlSecurityException extends IllegalArgumentException {

    private final String code;

    public SqlSecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
