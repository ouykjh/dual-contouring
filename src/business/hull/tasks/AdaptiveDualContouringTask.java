package business.hull.tasks;

import business.misc.OctreeNode;
import business.misc.Vector3i;
import business.hull.Hull;
import business.hull.primitives.Primitive;
import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;

/**
 * Constructs a mesh from a given octree.
 */
public class AdaptiveDualContouringTask extends RecursiveAction
{

    private static enum AXIS
    {

        X, Y, Z
    };
    private OctreeNode rootNode;
    private ArrayList<Primitive> primitives;
    private ArrayList<Vector3i> triangles;

    private AdaptiveDualContouringTask(OctreeNode rootNode, ArrayList<Primitive> primitives, ArrayList<Vector3i> triangles)
    {
        this.rootNode = rootNode;
        this.primitives = primitives;
        this.triangles = triangles;
    }

    public AdaptiveDualContouringTask(OctreeNode rootNode, ArrayList<Primitive> primitives)
    {
        this(rootNode, primitives, new ArrayList<Vector3i>());
    }

    /**
     * Recursive polygonisation function that handles cubes and, if the cube is
     * not a leaf of the octree, calls other functions on its subcells, edges
     * and faces.
     *
     * @param q is an octree node to process.
     */
    private void cellProc(OctreeNode q)
    {
        if (q != null && !q.isLeaf())
        {
            OctreeNode[] kids = q.getChildren();
            // 8 calls to cellProc
            for (OctreeNode child : kids)
            {
                cellProc(child);
            }

            // 12 calls to faceProc
            faceProc(kids[0], kids[1], AXIS.X);
            faceProc(kids[2], kids[3], AXIS.X);
            faceProc(kids[4], kids[5], AXIS.X);
            faceProc(kids[6], kids[7], AXIS.X);

            faceProc(kids[0], kids[2], AXIS.Y);
            faceProc(kids[1], kids[3], AXIS.Y);
            faceProc(kids[4], kids[6], AXIS.Y);
            faceProc(kids[5], kids[7], AXIS.Y);

            faceProc(kids[0], kids[4], AXIS.Z);
            faceProc(kids[1], kids[5], AXIS.Z);
            faceProc(kids[2], kids[6], AXIS.Z);
            faceProc(kids[3], kids[7], AXIS.Z);


            // 6 calls to edgeProc
            edgeProc(new OctreeNode[]
                    {
                        kids[0], kids[2], kids[4], kids[6]
                    }, AXIS.X);
            edgeProc(new OctreeNode[]
                    {
                        kids[1], kids[3], kids[5], kids[7]
                    }, AXIS.X);
            edgeProc(new OctreeNode[]
                    {
                        kids[0], kids[1], kids[4], kids[5]
                    }, AXIS.Y);
            edgeProc(new OctreeNode[]
                    {
                        kids[2], kids[3], kids[6], kids[7]
                    }, AXIS.Y);
            edgeProc(new OctreeNode[]
                    {
                        kids[0], kids[1], kids[2], kids[3]
                    }, AXIS.Z);
            edgeProc(new OctreeNode[]
                    {
                        kids[4], kids[5], kids[6], kids[7]
                    }, AXIS.Z);

        }
    }

    /**
     * Recursive polygonisation function that handles faces and, if no cube is a
     * leaf of the octree, calls other functions on its edges and subfaces.
     *
     * @param q1 is the left-, bottom- or whatever-most octree node.
     * @param q2 is its neighbour.
     * @param axis is the axis of the face.
     */
    private void faceProc(OctreeNode q1, OctreeNode q2, AXIS axis)
    {
        // If all nodes are leaves, or one is empty, bail out.
        if (q1 != null && q2 != null && (!q1.isLeaf() || !q2.isLeaf()))
        {
            OctreeNode[] kids1 = q1.getChildren();
            OctreeNode[] kids2 = q2.getChildren();

            // First determine the orientation of the face.
            switch (axis)
            {
                case X:
                    // X axis
                    // 4 calls to faceProc.
                    faceProc(kids1[1], kids2[0], AXIS.X);
                    faceProc(kids1[3], kids2[2], AXIS.X);
                    faceProc(kids1[5], kids2[4], AXIS.X);
                    faceProc(kids1[7], kids2[6], AXIS.X);

                    // 4 calls to edgeProc
                    edgeProc(new OctreeNode[]
                            {
                                kids1[1], kids2[0], kids1[5], kids2[4]
                            }, AXIS.Y);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[3], kids2[2], kids1[7], kids2[6]
                            }, AXIS.Y);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[1], kids2[0], kids1[3], kids2[2]
                            }, AXIS.Z);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[5], kids2[4], kids1[7], kids2[6]
                            }, AXIS.Z);

                    break;
                case Y:
                    // Y axis
                    // 4 calls to faceProc.
                    faceProc(kids1[2], kids2[0], AXIS.Y);
                    faceProc(kids1[3], kids2[1], AXIS.Y);
                    faceProc(kids1[6], kids2[4], AXIS.Y);
                    faceProc(kids1[7], kids2[5], AXIS.Y);

                    // 4 calls to edgeProc
                    edgeProc(new OctreeNode[]
                            {
                                kids1[2], kids2[0], kids1[6], kids2[4]
                            }, AXIS.X);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[3], kids2[1], kids1[7], kids2[5]
                            }, AXIS.X);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[2], kids1[3], kids2[0], kids2[1]
                            }, AXIS.Z);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[6], kids1[7], kids2[4], kids2[5]
                            }, AXIS.Z);

                    break;
                case Z:
                    // Z axis
                    // 4 calls to faceProc.
                    faceProc(kids1[4], kids2[0], AXIS.Z);
                    faceProc(kids1[5], kids2[1], AXIS.Z);
                    faceProc(kids1[6], kids2[2], AXIS.Z);
                    faceProc(kids1[7], kids2[3], AXIS.Z);

                    // 4 calls to edgeProc
                    edgeProc(new OctreeNode[]
                            {
                                kids1[4], kids1[5], kids2[0], kids2[1]
                            }, AXIS.Y);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[6], kids1[7], kids2[2], kids2[3]
                            }, AXIS.Y);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[4], kids1[6], kids2[0], kids2[2]
                            }, AXIS.X);
                    edgeProc(new OctreeNode[]
                            {
                                kids1[5], kids1[7], kids2[1], kids2[3]
                            }, AXIS.X);

                    break;
            }
        }
    }

    /**
     * Recursive polygonisation function that handles edges and, if no cube is a
     * leaf of the octree, calls itself on its subedges. Also creates a Quad if
     * needed.
     *
     * Octrees are oriented as follow: If the edge is along the X axis, q1 is
     * y=0 and z=0, q2 is y=1, q3 is z=1, q4 is y=1 and z=1.
     * 
     *
     * @param axis is the axis of the edge.
     */
    private void edgeProc(OctreeNode q[], AXIS axis)
    {
        // If one of the nodes is null, bail out.
        if (q[0] != null && q[1] != null && q[2] != null && q[3] != null)
        {
            // If all cubes are leaves, stop recursion.
            if (q[0].isLeaf() && q[1].isLeaf() && q[2].isLeaf() && q[3].isLeaf())
            {
                // If they all bear a vertex and there is an intersection on the
                // edge, woot, generate a quad, kthxbai.
                if (q[0].getVertexIndex() != -1 && q[1].getVertexIndex() != -1 && q[2].getVertexIndex() != -1 && q[3].getVertexIndex() != -1)
                {
                    // To check if there is an intersection on the edge, check each central edge - as they can be of different size.
                    int corners1[] = null, corners2[] = null;
                    switch (axis)
                    {
                        case X:
                            corners1 = new int[]
                            {
                                6, 4, 2, 0
                            };
                            corners2 = new int[]
                            {
                                7, 5, 3, 1
                            };

                            break;
                        case Y:
                            corners1 = new int[]
                            {
                                5, 4, 1, 0
                            };
                            corners2 = new int[]
                            {
                                7, 6, 3, 2
                            };

                            break;
                        case Z:
                            corners1 = new int[]
                            {
                                3, 2, 1, 0
                            };
                            corners2 = new int[]
                            {
                                7, 6, 5, 4
                            };

                            break;
                    }

                    float v1, v2;
                    boolean intersectionFound = false;
                    for (int i = 0; i < 4 && !intersectionFound; i++)
                    {
                        v1 = Hull.getValueAt(q[i].getCorner(corners1[i]), primitives);
                        v2 = Hull.getValueAt(q[i].getCorner(corners2[i]), primitives);
                        // Check if the signs are different.
                        if (v1 < 0 && v2 >= 0 || v1 > 0 && v2 <= 0)
                        {
                            intersectionFound = true;

                            // If so, create a quad with the right triangle orientation.
                            if (axis == axis.Y)
                            {
                                v1 = -v1;
                            }

                            if (v1 < 0)
                            {
                                triangles.add(new Vector3i(q[0].getVertexIndex(), q[1].getVertexIndex(), q[2].getVertexIndex()));
                                triangles.add(new Vector3i(q[2].getVertexIndex(), q[1].getVertexIndex(), q[3].getVertexIndex()));
                            } else
                            {
                                triangles.add(new Vector3i(q[2].getVertexIndex(), q[1].getVertexIndex(), q[0].getVertexIndex()));
                                triangles.add(new Vector3i(q[3].getVertexIndex(), q[1].getVertexIndex(), q[2].getVertexIndex()));
                            }
                        }
                    }


                }
            } else
            {
                // If not all cubes are leaves, make 2 calls to edgeProc.
                OctreeNode[] kids1 = q[0].getChildren();
                OctreeNode[] kids2 = q[1].getChildren();
                OctreeNode[] kids3 = q[2].getChildren();
                OctreeNode[] kids4 = q[3].getChildren();

                switch (axis)
                {
                    case X:
                        edgeProc(new OctreeNode[]
                                {
                                    kids1[6], kids2[4], kids3[2], kids4[0]
                                }, AXIS.X);
                        edgeProc(new OctreeNode[]
                                {
                                    kids1[7], kids2[5], kids3[3], kids4[1]
                                }, AXIS.X);
                        break;
                    case Y:
                        edgeProc(new OctreeNode[]
                                {
                                    kids1[5], kids2[4], kids3[1], kids4[0]
                                }, AXIS.Y);
                        edgeProc(new OctreeNode[]
                                {
                                    kids1[7], kids2[6], kids3[3], kids4[2]
                                }, AXIS.Y);
                        break;
                    case Z:
                        edgeProc(new OctreeNode[]
                                {
                                    kids1[3], kids2[2], kids3[1], kids4[0]
                                }, AXIS.Z);
                        edgeProc(new OctreeNode[]
                                {
                                    kids1[7], kids2[6], kids3[5], kids4[4]
                                }, AXIS.Z);
                        break;
                }
            }
        }
    }

    @Override
    public void compute()
    {
        // TODO: parallelize :D
        cellProc(rootNode);
    }

    /**
     * @return the triangles
     */
    public ArrayList<Vector3i> getTriangles()
    {
        return triangles;
    }
}