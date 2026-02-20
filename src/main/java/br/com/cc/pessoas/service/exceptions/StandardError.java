package br.com.cc.pessoas.service.exceptions;

public class StandardError {

    private String message;
    private String path;

    public StandardError(String message, String path) {
        this.message = message;
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
