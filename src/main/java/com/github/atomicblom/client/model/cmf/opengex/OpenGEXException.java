package com.github.atomicblom.client.model.cmf.opengex;

public class OpenGEXException extends RuntimeException
{

    public OpenGEXException(String message)
    {
        super(message);
    }

    public OpenGEXException(String message, Throwable innerException)
    {
        super(message, innerException);
    }
}
