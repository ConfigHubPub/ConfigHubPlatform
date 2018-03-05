/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.utils;

public class Pair<A, B>
        implements Comparable<Pair<A, B>>
{
    public A car;
    public B cdr;

    public Pair(A a, B b)
    {
        car = a;
        cdr = b;
    }

    public boolean bothValuesSet()
    {
        return !(null == car || null == cdr);
    }

    public String toString()
    {
        return "(" + car + " . " + cdr + ")";
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Pair)) { return false; }

        Pair p = (Pair)o;
        return (car == null ? p.car == null : car.equals(p.car)) && (cdr == null ? p.cdr == null : cdr.equals(p.cdr));
    }

    public int hashCode()
    {
        return (car == null ? 0 : car.hashCode()) ^ (cdr == null ? 0 : cdr.hashCode());
    }

    public int compareTo(Pair<A, B> p)
    {
        int primary = ((Comparable<A>)car).compareTo(p.car);
        if (primary != 0) { return primary; }
        try
        {
            return ((Comparable<B>)cdr).compareTo(p.cdr);
        }
        catch (ClassCastException cce)
        {
            // If B is uncomparable, sort only on A.
            return 0;
        }
    }

    /**
     * For JSTL
     */
    public A getCar()
    { return car; }

    public B getCdr() { return cdr; }
}
