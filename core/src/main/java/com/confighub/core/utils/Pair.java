/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
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
