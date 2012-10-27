package business.misc;

import business.hull.Hull;
import business.hull.primitives.Primitive;
import com.jme3.math.Vector3f;
import java.util.ArrayList;

public class DualContouring
{
    private static final int MAX_ITERATIONS = 100;
    private static int sumIterations = 0;
    private static int numberOfIterations = 0;
    private static float FORCE_TRESHOLD = 0.00001f;
    private static float forceRatio = 0.75f;
       public final static int intersections[][][] =
    {{{0,0,0},{1,0,0}},{{1,0,0},{1,1,0}},{{1,1,0},{0,1,0}},{{0,1,0},{0,0,0}},{{0,0,1},{1,0,1}},{{1,0,1},{1,1,1}},{{1,1,1},{0,1,1}},{{0,1,1},{0,0,1}},{{0,0,0},{0,0,1}},{{1,0,0},{1,0,1}},{{1,1,0},{1,1,1}},{{0,1,0},{0,1,1}}};
    
    public final static int edgeTable[] =
    {
        0x0, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
        0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
        0x190, 0x99, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
        0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
        0x230, 0x339, 0x33, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
        0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
        0x3a0, 0x2a9, 0x1a3, 0xaa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
        0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
        0x460, 0x569, 0x663, 0x76a, 0x66, 0x16f, 0x265, 0x36c,
        0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
        0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff, 0x3f5, 0x2fc,
        0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
        0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55, 0x15c,
        0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
        0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc,
        0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
        0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
        0xcc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
        0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
        0x15c, 0x55, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
        0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
        0x2fc, 0x3f5, 0xff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
        0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
        0x36c, 0x265, 0x16f, 0x66, 0x76a, 0x663, 0x569, 0x460,
        0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
        0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa, 0x1a3, 0x2a9, 0x3a0,
        0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
        0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33, 0x339, 0x230,
        0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
        0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99, 0x190,
        0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
        0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0
    };

    /**
     * For the Adaptive Dual Contouring, return the index of the current cube,
     * given its max and min bound.
     */
    public static int getCubeIndex(ArrayList<Primitive> primitives, Vector3f minBound, Vector3f maxBound)
    {
        int cubeIndex = 0;
        Vector3f position = new Vector3f(minBound);

        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 1;
        }

        position.x = maxBound.x;
        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 2;
        }

        position.y = maxBound.y;
        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 4;
        }

        position.x = minBound.x;
        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 8;
        }

        position.y = minBound.y;
        position.z = maxBound.z;
        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 16;
        }

        position.x = maxBound.x;
        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 32;
        }

        position.y = maxBound.y;
        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 64;
        }

        position.x = minBound.x;
        if (Hull.getValueAt(position, primitives) < 0)
        {
            cubeIndex |= 128;
        }

        return cubeIndex;
    }

    public static int getCubeIndex(ArrayList<Primitive> primitives, OctreeNode octree)
    {
        return getCubeIndex(primitives, octree.getMinBound(), octree.getMaxBound());
    }

    /**
     * Computes the vertex for the cube, from Hermite data. Uses Leonardo
     * Augusto Schmitz's excellent method, with exact normal at intersection
     * points, to reduce complexity.
     *
     * @param intersectionPoints : An arraylist containing the positions of
     * intersections with the isosurface.
     * @param intersectionNormals : An arraylist containing the normal to the
     * surface at each of these points.
     * @return the approximated vertex for this cube.
     */
    public static Vector3f vertexFromParticle(ArrayList<Vector3f> intersectionPoints, ArrayList<Vector3f> intersectionNormals, float treshold)
    {
        treshold *= treshold;

        // Center the particle on the masspoint.
        Vector3f masspoint = new Vector3f();
        for (Vector3f v : intersectionPoints)
        {
            masspoint.addLocal(v);
        }
        masspoint.divideLocal(intersectionPoints.size());
        Vector3f particlePosition = new Vector3f(masspoint);

        // Start iterating:
        Vector3f force = new Vector3f();
        int iteration;
        for (iteration = 0; iteration < MAX_ITERATIONS; iteration++)
        {
            force.set(0, 0, 0);

            // For each intersection point:
            for (int i = 0; i < intersectionPoints.size(); i++)
            {
                Vector3f planePoint = intersectionPoints.get(i);
                Vector3f planeNormal = intersectionNormals.get(i);

                // Compute distance vector to plane.
                // To do that, compute the normal.dot(AX).
                float d = planeNormal.dot(particlePosition.subtract(planePoint));

                force.addLocal(planeNormal.mult(-d));
            }

            // Average the force over all the intersection points, and multiply 
            // with a ratio and some damping to avoid instabilities.
            float damping = 1f - ((float) iteration) / MAX_ITERATIONS;

            force.multLocal(forceRatio * damping / intersectionPoints.size());

            // Apply the force.
            particlePosition.addLocal(force);

            // If the force was almost null, break.
            if (force.lengthSquared() < treshold)
            {
                break;
            }
        }

        // Compute average number of iterations:
        sumIterations += iteration;
        numberOfIterations++;
        // System.out.println(1f * sumIterations / numberOfIterations);

        return particlePosition;
    }
}
