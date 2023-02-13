/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.types;

/**
 * EIMEmailComparable
 *
 * @author Denis Meyer
 */
import com.eim.mail.EIMEmailMessage;
import java.util.Comparator;

public class EIMEmailComparable implements Comparator<EIMEmailMessage> {

    @Override
    public int compare(EIMEmailMessage o1, EIMEmailMessage o2) {
        return o1.getEnvelope().getDateSent().compareTo(o2.getEnvelope().getDateSent());
    }
}
