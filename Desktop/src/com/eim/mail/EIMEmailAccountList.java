/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import java.util.ArrayList;

/**
 * EIMEmailAccountList
 *
 * @author Denis Meyer
 */
public abstract class EIMEmailAccountList extends ArrayList<EIMEmailAccount> {

    public enum STATUS {

        ADDED,
        REMOVED
    }

    public abstract void onChange(EIMEmailAccount acc, STATUS status);

    @Override
    public boolean add(EIMEmailAccount e) {
        boolean add = super.add(e);
        onChange(e, STATUS.ADDED);
        return add;
    }

    @Override
    public EIMEmailAccount remove(int i) {
        EIMEmailAccount removed = super.remove(i);
        onChange(removed, STATUS.REMOVED);
        return removed;
    }

    @Override
    public boolean remove(Object e) {
        boolean removed = super.remove(e);
        onChange((EIMEmailAccount) e, STATUS.REMOVED);
        return removed;
    }
}
