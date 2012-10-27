package business.hull.primitives;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public final class RectangularBox extends Primitive
{
    private static final float size = 1 / FastMath.sqrt(2);

    public RectangularBox()
    {
        super();
        // extruded = true;
        Geometry geometry = new Geometry("", new Box(size, size, size));

        // Half wireframe, half opaque material.
        geometry.setMaterial(wireframeMaterial);
        attachChild(geometry);
        geometry = geometry.clone();
        geometry.setMaterial(transparentMaterial);
        geometry.setQueueBucket(Bucket.Transparent);
        attachChild(geometry);
    }

    @Override
    /**
     * For squares, consider infinite norm squared.
     */
    public float getPointValue(final Vector3f worldPoint)
    {
        Vector3f v = worldToLocal(worldPoint, null);

        float infiniteNorm = Math.max(Math.abs(v.x), Math.max(Math.abs(v.z), Math.abs(v.y)));
        return (infiniteNorm - size);
    }

    @Override
    public Vector3f getPointNormal(final Vector3f worldPoint)
    {
        Vector3f v = worldToLocal(worldPoint, null);

        // Determine on which side of the cube the point is, and determine the 
        // unit vector bearing the normal. f'(x) = 2f(x).
        if (Math.abs(v.z) > Math.abs(v.y) && Math.abs(v.z) > Math.abs(v.x))
        {
            v.set(0, 0, v.z);
        } else if (Math.abs(v.y) > Math.abs(v.x))
        {
            v.set(0, v.y, 0);
        } else
        {
            v.set(v.x, 0, 0);
        }

        // Turn back to world then substract origin point to get the vector.
        localToWorld(v, v).subtractLocal(getWorldTranslation()).normalizeLocal();

        return v;
    }
}
