/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.types;

/**
 * EIMPair
 *
 * @author Denis Meyer
 * @param <L>
 * @param <R>
 */
public class EIMPair<L, R> {

    public L left;
    public R right;

    public EIMPair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int hashCode() {
        return left.hashCode() ^ right.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof EIMPair)) {
            return false;
        }
        EIMPair pairo = (EIMPair) o;
        return this.left.equals(pairo.left)
                && this.right.equals(pairo.right);
    }
}
