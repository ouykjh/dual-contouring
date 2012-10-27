/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.misc;

import com.jme3.math.Vector3f;

/**
 *
 * @author Stophe
 */
public class Vector3i implements Cloneable
{

    public int x = 0;
    public int y = 0;
    public int z = 0;

    public Vector3i()
    {
    }

    public Vector3i(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object that)
    {
        return that.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode()
    {
        return (x & 0b1111111111) + ((y & 0b1111111111) << 10) + ((z & 0b1111111111) << 20);
    }

    @Override
    public Vector3i clone()
    {
        Vector3i newVector = new Vector3i(x, y, z);
        return newVector;
    }

    public void set(Vector3i that)
    {
        this.x = that.x;
        this.y = that.y;
        this.z = that.z;
    }

    public Vector3f toVector3f()
    {
        return new Vector3f(x, y, z);
    }

    public boolean contains(int i)
    {
        return x == i || y == i || z == i;
    }

    @Override
    public String toString()
    {
        return "[" + x + ',' + y + ',' + z + ']';
    }

    public void addLocal(int i, int j, int k)
    {
        x += i;
        y += j;
        z += k;
    }

    public boolean replace(int i, int newVertex)
    {
        boolean found = false;

        if (x == i)
        {
            found = true;
            x = newVertex;
        }
        if (y == i)
        {
            found = true;
            y = newVertex;
        }
        if (z == i)
        {
            found = true;
            z = newVertex;
        }

        return found;
    }
}
