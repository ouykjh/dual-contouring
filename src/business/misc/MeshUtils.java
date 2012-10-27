package business.misc;

import business.hull.Hull;
import business.hull.airframes.Primitive;
import com.jme3.math.Vector3f;
import java.util.ArrayList;


public class MeshUtils
{
    /**
     * Returns a list of triangles containing this vertex.
     */
    public static ArrayList<Vector3i> getAdjacentTriangles(int vertex, ArrayList<Vector3i> triangles)
    {
        ArrayList<Vector3i> result = new ArrayList<>();

        for (Vector3i triangle : triangles)
        {
            if (triangle.x == vertex || triangle.y == vertex || triangle.z == vertex)
            {
                // Todo: clone?
                result.add(triangle);
            }
        }
        return result;
    }

    /**
     * Removes vertices unused by the triangles.
     */
    public static void cleanVertices(ArrayList<Vector3i> triangles, ArrayList<Vector3f> vertices)
    {
        // Track how many were deleted at the beginning of the list.
        int offset = 0;

        // For each vertex in the list:
        int n = vertices.size();
        for (int i = 0; i < n; i++)
        {
            // Determine if the vertex is present.
            boolean found = false;
            for (Vector3i triangle : triangles)
            {
                // Also shift the index by the current offset.
                //found =

                if (triangle.contains(i))
                {
                    found = triangle.replace(i, i - offset);
                }
            }
            //If the vertex was not found, remove it and increment the offset.
            if (!found)
            {
                vertices.remove(i - offset);
                offset++;
            }
        }
    }

    /**
     * Computes and returns the faceted normals, computed from the faces
     * orientation. When a face is linked to an existing normal, if this one is
     * too different from the normal of the face, the vertex is duplicated to
     * allow sharp angles in the normals.
     */
    public static Vector3f[] facetedNormalsFromFaces(ArrayList<Vector3i> triangles, ArrayList<Vector3f> vertices, ArrayList<Primitive> primitives, float sharpAngle)
    {
        ArrayList<Vector3f> mainNormals = new ArrayList<>();
        ArrayList<Vector3f> normals = new ArrayList<>();

        // Iterate on each vertex.
        int n = vertices.size();
        for (int vertex = 0; vertex < n; vertex++)
        {
            int endOfVertexList = vertices.size() - 1;

            // First off, list triangles adjacent to the targeted vertex.
            ArrayList<Vector3i> adjacentTriangles = getAdjacentTriangles(vertex, triangles);

            // For each adjacent triangle, process the normals.
            ArrayList<Vector3f> currentNormals = new ArrayList<>();
            currentNormals.add(Hull.getNormalAt(vertices.get(vertex), primitives));
            for (Vector3i triangle : adjacentTriangles)
            {
                // Process the normal from the geometry, at the center of the triangle.
                Vector3f triangleCenter = new Vector3f(vertices.get(triangle.x));
                triangleCenter.addLocal(vertices.get(triangle.y));
                triangleCenter.addLocal(vertices.get(triangle.z));
                triangleCenter.divideLocal(3);
                Vector3f normal = Hull.getNormalAt(triangleCenter, primitives);

                int bestIndex = -1;
                float bestAngle = Float.MAX_VALUE;
                // Find, among the existing normals, the closest to the current one.
                for (int index = 0; index < currentNormals.size(); index++)
                {
                    float angle = currentNormals.get(index).normalize().angleBetween(normal.normalize());

                    // If the normals angle is deemed non-sharp and smallest for now, save the candidate.
                    if (angle < bestAngle && angle < sharpAngle)
                    {
                        bestIndex = index;
                        bestAngle = angle;
                    }
                }

                // If a satisfying normal was found, link the vertex to it.
                if (bestIndex >= 0)
                {
                    // If the index is 0, let the vertex alone.
                    if (bestIndex != 0)
                    {
                        triangle.replace(vertex, endOfVertexList + bestIndex);
                    }
                    // add the current normal, to average all that.
                    //  currentNormals.get(bestIndex).add(normal);
                } else
                {
                    // If not, the current vertex is duplicated with the new normal.
                    if (!currentNormals.isEmpty())
                    {
                        triangle.replace(vertex, vertices.size());
                        vertices.add(vertices.get(vertex).clone());
                    }
                    currentNormals.add(normal);
                }
            }

            // Add the main normal for the current vertex.
            mainNormals.add(currentNormals.get(0).normalizeLocal());

            // Drop (normalized) additional normals to an array.
            for (int i = 1; i < currentNormals.size(); i++)
            {
                normals.add(currentNormals.get(i).normalizeLocal());
            }
            //normals.addAll(currentNormals);
        }

        mainNormals.addAll(normals);
        return mainNormals.toArray(new Vector3f[0]);
    }
}