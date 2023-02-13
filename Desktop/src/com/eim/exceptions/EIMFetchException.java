/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.exceptions;

/**
 * EIMCouldNotWriteToFileException
 *
 * @author Denis Meyer
 */
public class EIMFetchException extends Exception {

    public static enum STATUS {

        NORMAL,
        NO_FOLDER_FOUND,
        NO_SENT_FOLDER_FOUND
    };
    
    public STATUS status;

    public EIMFetchException() {
    }

    public EIMFetchException(String s, STATUS status) {
        super(s);
        this.status = status;
    }
}
