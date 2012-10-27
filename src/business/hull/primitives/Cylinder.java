package business.hull.primitives;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;


public final class Cylinder extends Primitive
{

    static final private Mesh mesh;
    private static final float size = 1 / FastMath.sqrt(2);

    // Create the cylindric mesh
    static
    {
        mesh = new Mesh();
        // Vertices
        Vector3f[] vertices = new Vector3f[22];
        for (int i = 0; i < 10; i++)
        {
            float x = FastMath.cos(i * FastMath.TWO_PI / 10);
            float z = FastMath.sin(i * FastMath.TWO_PI / 10);
            vertices[2 * i] = new Vector3f(x, -size, z);
            vertices[2 * i + 1] = new Vector3f(x, size, z);
        }

        vertices[20] = new Vector3f(0, -size, 0);
        vertices[21] = new Vector3f(0, size, 0);


        // Indices:
        int[] indices = new int[120];
        int index = 0;
        for (int i = 0; i < 18; i += 2)
        {
            indices[index++] = i + 2;
            indices[index++] = i;
            indices[index++] = i + 1;

            indices[index++] = i + 1;
            indices[index++] = i + 3;
            indices[index++] = i + 2;
        }

        // Last rectangle:
        indices[index++] = 0;
        indices[index++] = 18;
        indices[index++] = 19;

        indices[index++] = 19;
        indices[index++] = 1;
        indices[index++] = 0;

        // Top and bottom
        for (int i = 0; i < 9; i++)
        {
            indices[index++] = 20;
            indices[index++] = 2 * i;
            indices[index++] = 2 * i + 2;


            indices[index++] = 2 * i + 1;
            indices[index++] = 21;
            indices[index++] = 2 * i + 3;
        }

        // Last top and bottom triangles
        indices[index++] = 20;
        indices[index++] = 18;
        indices[index++] = 0;

        indices[index++] = 19;
        indices[index++] = 21;
        indices[index++] = 1;

        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();
        mesh.setStatic();
    }

    public Cylinder()
    {
        super();

        // Half wireframe, half opaque material.
        Geometry geometry = new Geometry("", mesh);
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
        float length = Math.abs(v.y);
        float radius = FastMath.sqrt(v.x * v.x + v.z * v.z);

        return Math.max(length - size, radius - 1);
    }

    @Override
    public Vector3f getPointNormal(Vector3f worldPoint)
    {
        Vector3f v = new Vector3f();
        worldToLocal(worldPoint, v);

        // If the point is on top of the cylinder, up- or downward normal.
        if (Math.abs(v.y) - size > FastMath.sqrt(v.x * v.x + v.z * v.z) - 1)
        {
            if (v.y < 0)
            {
                v.set(0, -100f, 0);
            } else
            {
                v.set(0, 100f, 0);
            }
        } else
        {
            v.y = 0;
        }

        // Turn back to world then substract origin point to get the vector.
        localToWorld(v, v).subtractLocal(getWorldTranslation()).normalizeLocal();

        return v;
    }
}
