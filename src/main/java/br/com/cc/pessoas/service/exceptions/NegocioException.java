package br.com.cc.pessoas.service.exceptions;

public class NegocioException extends RuntimeException{
    private final long serialVersionUID = 1L;

    public NegocioException(String message){
        super(message);
    }
}
