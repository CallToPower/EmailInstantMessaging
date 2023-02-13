/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.exceptions;

/**
 * EIMCouldNotWriteToFileException
 *
 * @author Denis Meyer
 */
public class EIMEmptyIMAPFolderException extends Exception {

    public EIMEmptyIMAPFolderException() {
    }

    public EIMEmptyIMAPFolderException(String s) {
        super(s);
    }
}
