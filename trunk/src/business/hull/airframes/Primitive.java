package business.hull.airframes;

import business.MainApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;

/**
 *
 * @author Stophe
 */
public abstract class Primitive extends Node
{
    public final static Material wireframeMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    public final static Material showNormalsMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");
    public final static Material showNormalsWireframeMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");
    public final static Material greenWireframeMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    public final static Material redWireframeMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    public final static Material transparentMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    public final static Material greenTransparentMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    public final static Material simpleLightMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
    public final static Material simpleLightWireframeMaterial = new Material(MainApplication.getInstance().getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
    public final static ArrayList<Primitive> listOfInstances = new ArrayList<>();

    static
    {
        wireframeMaterial.getAdditionalRenderState().setWireframe(true);

        showNormalsWireframeMaterial.getAdditionalRenderState().setWireframe(true);
        simpleLightWireframeMaterial.getAdditionalRenderState().setWireframe(true);

        greenWireframeMaterial.setColor("Color", ColorRGBA.Green);
        greenWireframeMaterial.getAdditionalRenderState().setWireframe(true);
        //greenWireframeMaterial.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);

        redWireframeMaterial.setColor("Color", ColorRGBA.Red);
        redWireframeMaterial.getAdditionalRenderState().setWireframe(true);

        transparentMaterial.setColor("Color", new ColorRGBA(1, 1, 1, 0.1f));
        transparentMaterial.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        greenTransparentMaterial.setColor("Color", new ColorRGBA(0, 1, 0, 0.1f));
        greenTransparentMaterial.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        simpleLightMaterial.setReceivesShadows(true);
        
        // Init all primitive types
        listOfInstances.add(new RectangularBox());
        listOfInstances.add(new Ellipsoid());
        listOfInstances.add(new Cylinder());
    }

    /**
     * Computes the field value at the given point. By using Node transformation
     * methods, rotations, scaling and positions are handled discretely.
     *
     * @param worldPoint
     * @return
     */
    public abstract float getPointValue(final Vector3f worldPoint);

    public abstract Vector3f getPointNormal(final Vector3f worldPoint);
}
