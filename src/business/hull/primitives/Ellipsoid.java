package business.hull.primitives;

import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;

public final class Ellipsoid extends Primitive
{

    public Ellipsoid()
    {
        super();

        // Half wireframe, half opaque material.
        Geometry geometry = new Geometry("", new Sphere(10, 10, 1f));
        geometry.setMaterial(wireframeMaterial);
        attachChild(geometry);
        geometry = geometry.clone();
        geometry.setMaterial(transparentMaterial);
        geometry.setQueueBucket(Bucket.Transparent);
        attachChild(geometry);
    }

    @Override
    public float getPointValue(final Vector3f worldPoint)
    {
        Vector3f v = worldToLocal(worldPoint, null);
        
        // Value = norm 2.
        return v.length() - 1;
    }

    @Override
    public Vector3f getPointNormal(final Vector3f worldPoint)
    {
        // Turn the point to local coordinates
        Vector3f v = worldToLocal(worldPoint, null);

        v.divideLocal(getLocalScale());

        // Turn back to world then substract origin point to get the vector.
        localToWorld(v, v).subtractLocal(getWorldTranslation()).normalizeLocal();

        return v;
    }
}
