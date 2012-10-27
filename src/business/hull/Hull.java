package business.hull;

import business.misc.MeshUtils;
import business.misc.OctreeNode;
import business.misc.Vector3i;
import business.hull.primitives.Primitive;
import business.hull.tasks.AdaptiveDualContouringTask;
import business.hull.tasks.OctreeConstructionTask;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

public class Hull extends Node
{

    private Geometry meshGeometry = null;
    private ArrayList<Primitive> primitives = new ArrayList<>();

    /**
     * Adds an primitive to the hull.
     */
    public int attach(Primitive primitive)
    {
        int returnValue = attachChild(primitive);
        //Also store the primitive in a list for further computing.
        primitives.add(primitive);
        return returnValue;
    }

    public int detach(Primitive primitive)
    {
        primitives.remove(primitive);
        return detachChild(primitive);
    }

    public void generateMesh()
    {
        if (!primitives.isEmpty())
        {
            // Save local rotation
            Quaternion q1 = getParent().getLocalRotation().clone();
            Quaternion q2 = getParent().getParent().getLocalRotation().clone();
            // Align to Zero rotation.
            getParent().setLocalRotation(Quaternion.IDENTITY);
            getParent().getParent().setLocalRotation(Quaternion.IDENTITY);
            setLocalRotation(Quaternion.IDENTITY);


            // Discard previous mesh then reload it 
            if (meshGeometry != null)
            {
                detachChild(meshGeometry);
            }

            // Build the hull again.
            Mesh mesh = buildPreviewMesh();

            // Re-attach the hull
            meshGeometry = new Geometry("OurMesh", mesh);
            // meshGeometry.setMaterial(Primitive.showNormalsWireframeMaterial);
            //meshGeometry.setMaterial(Primitive.showNormalsMaterial);
             meshGeometry.setMaterial(Primitive.simpleLightMaterial);
            // meshGeometry.setMaterial(Primitive.simpleLightWireframeMaterial);

            attachChild(meshGeometry);
            meshGeometry.setShadowMode(ShadowMode.CastAndReceive);



            // Restore rotation
            getParent().setLocalRotation(q1);
            getParent().getParent().setLocalRotation(q2);

            // Hide all frames.
            for (Primitive a : primitives)
            {
                a.setCullHint(CullHint.Always);
            }
        }
    }

    /**
     * Removes and returns the last added Primitive.
     */
    Primitive popFrame()
    {
        return primitives.remove(primitives.size() - 1);
    }

    /**
     * Computes which points are inside the hull
     */
    private Mesh buildPreviewMesh()
    {
         long start = System.currentTimeMillis();

        // First get the bounding box. 
        updateWorldBound();

        BoundingBox bound = (BoundingBox) getWorldBound();
        Vector3f maxBound = bound.getMax(null);
        Vector3f originPoint = bound.getMin(null);
        originPoint.x = Math.min(originPoint.x, -maxBound.x);
        originPoint.y = Math.min(originPoint.y, -maxBound.y);
        originPoint.z = Math.min(originPoint.z, -maxBound.z);


        // Thread Pool
        ForkJoinPool pool = new ForkJoinPool();

        // Create an octree from the data
        OctreeNode octree = new OctreeNode(originPoint, maxBound);
        OctreeConstructionTask dcOctreeTask = new OctreeConstructionTask(octree, primitives, 3, 6);
        pool.invoke(dcOctreeTask);

        // Contour the octree.
        AdaptiveDualContouringTask adaptiveTask = new AdaptiveDualContouringTask(octree, primitives);
        pool.invoke(adaptiveTask);

        // Retrieve computed data.
        ArrayList<Vector3f> verticesList = dcOctreeTask.getVertices();
        ArrayList<Vector3i> triangles = adaptiveTask.getTriangles();

        int numberOfVerticesBefore = verticesList.size();
        int numberOfTrianglesBefore = triangles.size();

        // Compute normals both from data and triangles.
        Vector3f normals[] = MeshUtils.facetedNormalsFromFaces(triangles, verticesList, primitives, (float) Math.toRadians(10));

        // Drop the triangles to an array.
        int index = 0;
        int[] triangleList = new int[3 * triangles.size()];
        for (Vector3i v : triangles)
        {
            triangleList[index++] = v.x;
            triangleList[index++] = v.y;
            triangleList[index++] = v.z;
        }

        // Finally, make the mesh itself:
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(verticesList.toArray(new Vector3f[0])));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(triangleList));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        mesh.updateBound();
        mesh.setStatic();

        long timeTaken = System.currentTimeMillis() - start;
        System.out.println(String.format("%d Vertices, %d Triangles in %d Milliseconds",verticesList.size(), triangles.size(), timeTaken));

        return mesh;
    }

    /**
     * Interpolates the intersection point from CSG values at both corners.
     */
    public static Vector3f interpolateIntersection(ArrayList<Primitive> primitives, Vector3f p1, Vector3f p2, float v1, float v2)
    {
        // If one of the values is too small, snap to the other point.
        if (Math.abs(v1) < 0.001f)
        {
            return p1.clone();
        }
        if (Math.abs(v2) < 0.001f)
        {
            return p2.clone();
        }
        // Also, if the two values are too close, return p1.
        if (Math.abs(v2 - v1) < 0.001f)
        {
            return p1.clone();
        }

        v1 = Math.abs(v1);
        v2 = Math.abs(v2);

        Vector3f intersectionPoint = new Vector3f();

        float v = v1 + v2;
        intersectionPoint.x = (v2 * p1.x + v1 * p2.x) / v;
        intersectionPoint.y = (v2 * p1.y + v1 * p2.y) / v;
        intersectionPoint.z = (v2 * p1.z + v1 * p2.z) / v;

        return intersectionPoint;
    }

    /**
     * Recursively interpolates the intersection point from CSG values at each
     * point.
     */
    public static Vector3f exactIntersection(ArrayList<Primitive> primitives, Vector3f p1, Vector3f p2, float v1, float v2, int depth)
    {
        Vector3f p = interpolateIntersection(primitives, p1, p2, v1, v2);
        if (depth == 0)
        {
            return p;
        } else
        {
            float newValue = Hull.getValueAt(p, primitives);
            if ((newValue < 0 && v1 < 0) || (newValue > 0 && v1 > 0))
            {
                return (exactIntersection(primitives, p, p2, newValue, v2, depth - 1));
            } else
            {
                return (exactIntersection(primitives, p1, p, v1, newValue, depth - 1));
            }
        }
    }

    /**
     * Returns true iff the node contains at least one intersection, after
     * sampling at the MaxDepth level.
     */
    public static boolean containsIntersection(OctreeNode octreeNode, int maxDepth, ArrayList<Primitive> primitives)
    {
        // First off, check if the node is null. 
        if (octreeNode == null)
        {
            return false;
        }

        // Subdivide to the finest possible level.
        // The subdivision level equals 2^n, where n is max-current depth.
        int divisionLevel = 1 << (maxDepth - octreeNode.getDepth());

        Vector3f startPoint = octreeNode.getMinBound();
        Vector3f offset = octreeNode.getMaxBound().subtract(startPoint).divideLocal(divisionLevel);
        boolean sign = getValueAt(startPoint, primitives) > 0;
        Vector3f currentPoint = new Vector3f();
        for (int i = 0; i < divisionLevel + 1; i++)
        {
            for (int j = 0; j < divisionLevel + 1; j++)
            {
                for (int k = 0; k < divisionLevel + 1; k++)
                {
                    currentPoint.x = startPoint.x + i * offset.x;
                    currentPoint.y = startPoint.y + j * offset.y;
                    currentPoint.z = startPoint.z + k * offset.z;

                    if (getValueAt(currentPoint, primitives) > 0 != sign)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Returns the function's normal at the given point. */
    public static Vector3f getNormalAt(Vector3f p, ArrayList<Primitive> primitives)
    {
        float bestValue = Float.MAX_VALUE;
        Primitive bestPrimitive = null;


        // Find the primitive which surface is the closest to the isosurface.
        for (Primitive primitive : primitives)
        {
            float newValue = Math.abs(primitive.getPointValue(p));
            if (newValue < bestValue)
            {
                bestValue = newValue;
                bestPrimitive = primitive;
            }
        }
        
        // Get the normal from that primitive.
        Vector3f result = bestPrimitive.getPointNormal(p);

        return result;
    }

    /** Returns the function's value at the given point. */
    public static float getValueAt(Vector3f p, ArrayList<Primitive> primitives)
    {
        float bestValue = Float.MAX_VALUE;

        // Find the primitive which surface is the closest to the isosurface.
        for (Primitive primitive : primitives)
        {
            bestValue = Math.min(primitive.getPointValue(p), bestValue);
        }
        
        return bestValue;
    }

    public boolean isEmpty()
    {
        return primitives.isEmpty();
    }
}
