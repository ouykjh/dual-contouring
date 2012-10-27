package business;

import business.appstates.EditHullState;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.shadow.PssmShadowRenderer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApplication extends SimpleApplication
{

    private static MainApplication instance = null;

    @Override
    public void simpleInitApp()
    {
        // Add shadows 
        PssmShadowRenderer pssmRenderer = new PssmShadowRenderer(assetManager, 512, 3);
        pssmRenderer.setDirection(new Vector3f(-1,-1, -1).normalizeLocal());
        pssmRenderer.setLambda(0.2f);
        pssmRenderer.setShadowIntensity(0.6f);
        pssmRenderer.setCompareMode(PssmShadowRenderer.CompareMode.Hardware);
        pssmRenderer.setFilterMode(PssmShadowRenderer.FilterMode.PCF8);
        
        // Disable the FlyCam
        flyCam.setEnabled(false);

        // Start the base state.
        EditHullState state = new EditHullState();
        stateManager.attach(state);
    }

    public static MainApplication getInstance()
    {
        return instance;
    }

    public static void main(String[] args)
    {
        // Set logger severity level
        Logger.getLogger("").setLevel(Level.WARNING);

        MainApplication app = new MainApplication();
        instance = app;
        app.start();
    }

    /**
     * Returns the closest node, among the list passed as argument, under the
     * mouse cursor. Also stores the contact point in the given vector (if not
     * null).
     */
    public static Node getNodeClicked(Camera camera, Vector3f contactPoint, Vector3f contactNormal, Node... nodes)
    {
        CollisionResults results = new CollisionResults();
        Vector2f click2d = MainApplication.getInstance().getInputManager().getCursorPosition();
        Vector3f click3d = camera.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
        Vector3f dir = camera.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();

        // 2. Aim the ray from cam loc to cam direction.
        Ray ray = new Ray(click3d, dir);


        // 3. Collect intersections between Ray and Shootables in results list.
        for (Node n : nodes)
        {
            n.collideWith(ray, results);
        }

        // 4. Debug if needed
        //System.out.println(results.getClosestCollision().getGeometry().getParent().getName());

        if (results.size() > 0)
        {
            if (contactPoint != null)
            {
                try
                {
                    contactPoint.set(results.getClosestCollision().getContactPoint());
                } catch (java.lang.IllegalArgumentException e)
                {

                    for (CollisionResult res : results)
                    {
                        System.err.println(res.getContactPoint());
                    }

                }
            }
            if (contactNormal != null)
            {
                contactNormal.set(results.getClosestCollision().getContactNormal());
            }
            return results.getClosestCollision().getGeometry().getParent();
        } else
        {
            return null;
        }
    }

    /**
     * Returns the closest node, among the list passed as argument, under the
     * mouse cursor.
     */
    public static Node getNodeClicked(Camera camera, Node... nodes)
    {
        return getNodeClicked(camera, null, null, nodes);
    }
}