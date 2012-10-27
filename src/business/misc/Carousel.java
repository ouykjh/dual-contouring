package business.misc;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;

/**
 * A rotating carousel to display and choose elements (nodes).
 */
public class Carousel<T extends Node> extends Node
{

    private ArrayList<T> elements = new ArrayList<>();
    private float radius = 10f;
    private int currentIndex = 0;

    /**
     * Adds an element to the list and refreshes the nodes.
     */
    public void addElements(ArrayList<T> elementsToAdd)
    {
        elements.addAll(elementsToAdd);

        refresh();
    }

    /**
     * Refreshes the nodes on the carousel.
     */
    private void refresh()
    {
        detachAllChildren();
        // Add a small rotation to see the node "from above".
        setLocalRotation(Quaternion.IDENTITY.clone().fromAngles(0, 0.002f, 0));

        // Prepare a rotation matrix and a simple vector.
        Quaternion q = new Quaternion(Quaternion.IDENTITY);
        q.fromAngles(FastMath.TWO_PI / elements.size(), 0, 0);
        Matrix3f m = q.toRotationMatrix();
        Vector3f vector = new Vector3f(0, 0, -radius);

        for (T element : elements)
        {
            attachChild(element);
            element.setLocalTranslation(vector);
            element.rotateUpTo(vector.normalize());
            // Rotate the vector each time.
            m.multLocal(vector);
        }
    }

    /**
     * Updates the carousel (orient it selected node - front, and make the
     * selected node rotate. )
     */
    public void update(float tpf)
    {
        // On updating, make the carousel reach the right position:
        float angleTarget = -currentIndex * FastMath.TWO_PI / elements.size();
        // substract current position:
        angleTarget -= getLocalRotation().toAngles(null)[0];

        // Simplify too large rotations
        if (angleTarget > FastMath.PI)
        {
            angleTarget = angleTarget - FastMath.TWO_PI;
        } else if (angleTarget < -FastMath.PI)
        {
            angleTarget = FastMath.TWO_PI + angleTarget;
        }

        // Avoid useless movements.
        if (FastMath.abs(angleTarget) > 0.01f)
        {
            rotate(angleTarget * tpf * 5, 0, 0);
        }

        // Rotate the active node.
        elements.get(currentIndex).rotate(0, 0, tpf);
    }

    /**
     * Returns a new instance of the currently selected element.
     */
    public T getInstanceOfCurrent()
    {
        return (T) elements.get(currentIndex).clone();
    }

    /**
     * Increment the index.
     */
    public void increment(int step)
    {
        currentIndex += step;
        currentIndex %= elements.size();
        // If the modulus is negative:
        if (currentIndex < 0)
        {
            currentIndex += elements.size();
        }
    }
}
