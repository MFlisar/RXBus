package com.michaelflisar.rxbus.exceptions;

/**
 * Created by flisar on 03.05.2016.
 */
public class RXBusKeyIsNullException extends RuntimeException
{
    public static void checkKey(Object key)
    {
        if (key == null)
            throw new RXBusKeyIsNullException();
    }

    public RXBusKeyIsNullException()
    {
        super("You can't use a null key!");
    }
}
