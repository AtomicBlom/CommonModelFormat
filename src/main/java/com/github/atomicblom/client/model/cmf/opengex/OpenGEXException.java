package com.github.atomicblom.client.model.cmf.opengex;

/**
 * Created by codew on 4/11/2015.
 */
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
