package com.michaelflisar.rxbus.exceptions;

/**
 * Created by flisar on 03.05.2016.
 */
public class RXBusEventIsNullException extends RuntimeException
{
    public static void checkEvent(Object event)
    {
        if (event == null)
            throw new RXBusEventIsNullException();
    }

    public RXBusEventIsNullException()
    {
        super("You can't send a null event!");
    }
}
