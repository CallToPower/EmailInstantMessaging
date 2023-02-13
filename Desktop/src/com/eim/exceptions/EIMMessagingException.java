/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.exceptions;

/**
 * EIMCouldNotWriteToFileException
 *
 * @author Denis Meyer
 */
public class EIMMessagingException extends Exception {

    public static enum STATUS {

        NORMAL,
        NO_CONNECTION_TO_STORE
    };
    
    public STATUS status;

    public EIMMessagingException() {
    }

    public EIMMessagingException(String s, STATUS status) {
        super(s);
        this.status = status;
    }
}
