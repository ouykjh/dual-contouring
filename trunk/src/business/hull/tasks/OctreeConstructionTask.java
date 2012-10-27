package business.hull.tasks;

import business.misc.DualContouring;
import business.misc.OctreeNode;
import business.hull.Hull;
import business.hull.primitives.Primitive;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;

/**
 * Subdivides an octree into an adapted octree that best fits the data.
 *
 * @author chcarpen
 */
public class OctreeConstructionTask extends RecursiveAction
{

    private OctreeNode rootNode;
    private ArrayList<Primitive> primitives;
    private int minDepth;
    private int maxDepth;
    private ArrayList<Vector3f> vertices;

    private OctreeConstructionTask(OctreeNode rootNode, ArrayList<Primitive> primitives, int minDepth, int maxDepth, ArrayList<Vector3f> vertices)
    {
        this.rootNode = rootNode;
        this.primitives = primitives;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        this.vertices = vertices;
    }

    public OctreeConstructionTask(OctreeNode rootNode, ArrayList<Primitive> primitives, int minDepth, int maxDepth)
    {
        this(rootNode, primitives, minDepth, maxDepth, new ArrayList<Vector3f>());
    }

    /**
     * For the adaptive DC, this computes the best fitting vertex from an
     * arbitrary cube.
     */
    private Vector3f generateVertex(OctreeNode octreeNode)
    {
        ArrayList<Vector3f> cubePoints = new ArrayList<>();
        ArrayList<Vector3f> cubeNormals = new ArrayList<>();

        getIntersectionPoints(octreeNode.getMinBound(), octreeNode.getMaxBound(), cubePoints, cubeNormals);

        return DualContouring.vertexFromParticle(cubePoints, cubeNormals, octreeNode.getCubeDiagonal() / 1000);
    }

    /**
     * Computes - and fills arraylists with - the intersection points and
     * normals for the current cube.
     *
     * @param currentCube
     * @param cubePoints
     * @param cubeNormals
     */
    private void getIntersectionPoints(Vector3f minBound, Vector3f maxBound, ArrayList<Vector3f> cubePoints, ArrayList<Vector3f> cubeNormals)
    {
        int cubeIndex = DualContouring.getCubeIndex(primitives, minBound, maxBound);
        int edgeInfo = DualContouring.edgeTable[cubeIndex];

        // For each pair of corners:
        for (int i = 0; i < 12; i++)
        {
            // No need to process this edge if there is no intersection along it.
            if ((edgeInfo & (1 << i)) == 0)
            {
                continue;
            }

            int offset1[] = DualContouring.intersections[i][0];
            int offset2[] = DualContouring.intersections[i][1];

            // The first corner is set according to the intersection table.
            Vector3f corner1 = new Vector3f(minBound);
            if (offset1[0] != 0)
            {
                corner1.x = maxBound.x;
            }
            if (offset1[1] != 0)
            {
                corner1.y = maxBound.y;
            }
            if (offset1[2] != 0)
            {
                corner1.z = maxBound.z;
            }

            // Same with the second corner
            Vector3f corner2 = new Vector3f(minBound);
            if (offset2[0] != 0)
            {
                corner2.x = maxBound.x;
            }
            if (offset2[1] != 0)
            {
                corner2.y = maxBound.y;
            }
            if (offset2[2] != 0)
            {
                corner2.z = maxBound.z;
            }

            // Values at both vertices of the edge:
            float v1 = Hull.getValueAt(corner1, primitives);
            float v2 = Hull.getValueAt(corner2, primitives);

            // Interpolate the intersection point with the surface.
            // Vector3f intersectionPoint = interpolateIntersection(p1, p2, v1, v2);            
            Vector3f intersectionPoint = Hull.exactIntersection(primitives, corner1, corner2, v1, v2, 5);

            // Now compute the exact normal at that point.
            Vector3f currentNormal = Hull.getNormalAt(intersectionPoint, primitives);

            // Save both the intersection point and the normal at that point.
            cubePoints.add(intersectionPoint);
            cubeNormals.add(currentNormal);
        }
    }

    private void computeDirectly(OctreeNode octreeNode)
    {
        Vector3f vertex = null;
        // First off, check if the node should generate a vertex:
        int cubeIndex = DualContouring.getCubeIndex(primitives, octreeNode);
        if (cubeIndex != 0b00000000 && cubeIndex != 0b11111111)
        {
            vertex = generateVertex(octreeNode);

            // If there is a vertex, but not satisfactory, delete it.
            if (vertex != null)
            {
                // If the vertex is OK -- or at max depth, thus max precision,
                // add it.
                // OK here means with an aceptable distance to surface and inside the octreecube.
                if (octreeNode.getDepth() == maxDepth
                        || (octreeNode.contains(vertex) && Math.abs(Hull.getValueAt(vertex, primitives)) < octreeNode.getCubeDiagonal() / 1000))
                {
                    // TODO: subdivide if there are more intersection, deeper?
                    synchronized (OctreeConstructionTask.class)
                    {
                        octreeNode.setVertex(vertices.size());
                        vertices.add(vertex);
                    }
                } else
                {
                    // If it's not ok, and above max depth, delete the vertex.
                    vertex = null;
                }
            }
        }

        // If there is no vertex, or not OK:
        if (vertex == null)
        {
            if (octreeNode.getDepth() < maxDepth /*&& Hull.containsIntersection(octreeNode, maxDepth, primitives)*/)
            {
                // If we are above max depth and there is an intersection 
                // somewhere, subdivide.
                octreeNode.subdivide();
                for (OctreeNode child : octreeNode.getChildren())
                {
                    computeDirectly(child);
                }
            } else
            {
                // If we are at max depth, or if there's no intersection at any level,
                // It's an empty leaf. Do nothing.
            }
        }
    }

    @Override
    public void compute()
    {
        if (rootNode.getDepth() < minDepth)
        {
            // If we did not reach the min depth, subdivide and create new threads.
            rootNode.subdivide();
            OctreeConstructionTask[] tasks = new OctreeConstructionTask[8];

            for (int i = 0; i < rootNode.getChildren().length; i++)
            {
                tasks[i] = new OctreeConstructionTask(rootNode.getChildren()[i], primitives, minDepth, maxDepth, vertices);
            }

            invokeAll(tasks);
        } else
        {
            computeDirectly(rootNode);
        }
    }

    /**
     * @return the vertices
     */
    public ArrayList<Vector3f> getVertices()
    {
        return vertices;
    }
}
