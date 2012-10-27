package business.appstates;

import business.MainApplication;
import business.hull.Hull;
import business.hull.primitives.Primitive;
import business.misc.Carousel;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

public class EditHullState extends AbstractAppState
{

    /**
     * Fraction of the screen reserved for the carousel.
     */
    private static final float splitScreenRatio = 0.2f;
    private Camera camera2;
    private Node hullNode = new Node("Hull");
    private Node previewNode = new Node("Preview");
    private Hull hull;
    private Carousel<Primitive> carousel;
    private Primitive currentPrimitive;
    private InputListener inputListener;

    @Override
    public void stateAttached(AppStateManager stateManager)
    {
        super.stateAttached(stateManager);

        // Set everything up.
        setInput();
        createScene();
        setupCams();
    }

    /**
     * Sets up input: listeners, mappings, cursors...
     */
    private void setInput()
    {
        // Activate the cursor 
        MainApplication.getInstance().getInputManager().setCursorVisible(true);

        // Clear mappings.
        MainApplication.getInstance().getInputManager().clearMappings();
        // Mouse axes.
        MainApplication.getInstance().getInputManager().addMapping("MouseRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        MainApplication.getInstance().getInputManager().addMapping("MouseLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        MainApplication.getInstance().getInputManager().addMapping("MouseUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        MainApplication.getInstance().getInputManager().addMapping("MouseDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        MainApplication.getInstance().getInputManager().addMapping("MouseWheelUp", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        MainApplication.getInstance().getInputManager().addMapping("MouseWheelDown", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        // Mouse buttons.
        MainApplication.getInstance().getInputManager().addMapping("LButton", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        MainApplication.getInstance().getInputManager().addMapping("RButton", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        // Keyboard.
        MainApplication.getInstance().getInputManager().addMapping("LCtrl", new KeyTrigger(KeyInput.KEY_LCONTROL));
        MainApplication.getInstance().getInputManager().addMapping("LShift", new KeyTrigger(KeyInput.KEY_LSHIFT));

        // Set relevant listeners.
        MainApplication.getInstance().getInputManager().addListener(new InputListener(), "MouseLeft", "MouseRight",
                "MouseDown", "MouseUp", "MouseWheelUp", "MouseWheelDown", "LButton", "RButton", "LCtrl", "LShift");
    }

    /**
     * Populates the scene.
     */
    private void createScene()
    {
        // Translate the main node 0,0,0 to simplify computations for the marching cubes.
        Node tempNode = new Node();
        MainApplication.getInstance().getRootNode().attachChild(tempNode);
        tempNode.attachChild(hullNode);
        hullNode.setLocalTranslation(0, 0, 0);

        // The hull node has only a hull and a preview (to place primitives) attached.
        hull = new Hull();
        hullNode.attachChild(hull);
        hullNode.attachChild(previewNode);

        // Create a carousel with all primitives types.
        carousel = new Carousel<>();
        carousel.addElements(Primitive.listOfInstances);
        currentPrimitive = carousel.getInstanceOfCurrent();

        // Attach the carousel.
        MainApplication.getInstance().getRootNode().attachChild(carousel);
        carousel.setLocalTranslation(0, 0, 30f);

        // Add a sun (on the right)
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.LightGray);
        sun.setDirection(new Vector3f(-1f, -1f, -1f).normalizeLocal());
        MainApplication.getInstance().getRootNode().addLight(sun);

        // Add an ambient light to see a minimum of things
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.7f));
        MainApplication.getInstance().getRootNode().addLight(al);
    }

    /**
     * Creates a split view: one preview node on the left of the screen, one
     * hull node in the rest.
     */
    private void setupCams()
    {
        // Set up the main camera.
        MainApplication.getInstance().getCamera().setLocation(new Vector3f(0, 0, 20));
        MainApplication.getInstance().getCamera().lookAt(hull.getWorldTranslation(), Vector3f.UNIT_Y);

        // Setup second, smaller PiP view
        camera2 = MainApplication.getInstance().getCamera().clone();
        camera2.setLocation(new Vector3f(0, 0, -2));

        // Set up the ratio
        float currentRatio = camera2.getFrustumTop() / camera2.getFrustumRight();
        camera2.setViewPort(0f, splitScreenRatio * currentRatio, 0f, 1f);
        camera2.setFrustumPerspective(30, splitScreenRatio, 1, 1000);
        camera2.lookAt(carousel.getWorldTranslation(), Vector3f.UNIT_Y);

        // Create the view and the scene.
        ViewPort viewPort2 = MainApplication.getInstance().getRenderManager().createMainView("Primitive Carousel", camera2);
        viewPort2.setClearFlags(true, true, true);
        viewPort2.attachScene(carousel);
    }

    /**
     * This global listener wraps the analog and action listeners for the scene.
     */
    private class InputListener implements AnalogListener, ActionListener
    {

        private boolean leftButtonDown = false;
        private boolean clickedPreviewPart = false;
        private boolean ctrlDown = false;
        private boolean shiftDown = false;

        /**
         * This listener is fired on analog events (mouse displacement,
         * scrolling...)
         */
        @Override
        public void onAnalog(String name, float value, float tpf)
        {
            // Check if the cursor is over the preview part of the screen for the current event.
            boolean isOverPreviewPart = MainApplication.getInstance().getInputManager().getCursorPosition().getX() < camera2.getWidth() * camera2.getViewPortRight();

            // If hovering over the hull, place the current primitive at the collision point.
            if (!leftButtonDown)
            {
                Vector3f contactPoint = new Vector3f();
                Vector3f contactNormal = new Vector3f();
                Node nodeAimed = MainApplication.getNodeClicked(MainApplication.getInstance().getCamera(), contactPoint, contactNormal, hull);
                // If something was aimed at:
                if (nodeAimed != null)
                {
                    // Attach the preview frame.to the hullnode - not the
                    // Hull itself, to avoid selecting the preview after that.
                    previewNode.attachChild(currentPrimitive);

                    // Convert coordinates to local system
                    Vector3f localPosition = hullNode.worldToLocal(contactPoint, null);
                    currentPrimitive.setLocalTranslation(localPosition);

                    //  contactNormal=hullNode.worldToLocal(contactNormal, null);
                    currentPrimitive.lookAt(contactNormal.negate(), contactNormal.cross(Vector3f.UNIT_X).normalizeLocal());
                } else
                {
                    // If nothing was aimed at, either there *is* nothing and we can add 
                    // the frame at the center of the scene.
                    if (hull.isEmpty())
                    {
                        previewNode.attachChild(currentPrimitive);
                        currentPrimitive.setLocalTranslation(Vector3f.ZERO);
                        currentPrimitive.setLocalRotation(Matrix3f.IDENTITY);
                    } else
                    {
                        // Either there is something, in which case we wait
                        // for the user to aim it, and do nothing otherwise.
                        previewNode.detachChild(currentPrimitive);
                        hull.detach(currentPrimitive);
                    }
                }
            }

            int step = 1;
            switch (name)
            {
                case "MouseWheelUp":
                    step = -step;
                case "MouseWheelDown":
                    if (isOverPreviewPart)
                    {
                        // Rotate the carousel
                        carousel.increment(step);

                        // Copy it to the current node.
                        previewNode.detachAllChildren();
                        currentPrimitive = carousel.getInstanceOfCurrent();

                    } else
                    {
                        if (ctrlDown)
                        {
                            // When mousescrolling over the hull with control down, elongate the current part:
                            currentPrimitive.scale(1, 1, 1 - step * 0.1f);
                        } else if (shiftDown)
                        {
                            // When mousescrolling over the hull, change X/Y ratio:
                            currentPrimitive.scale(1 - step * 0.1f, 1 + step * 0.1f, 1);
                        } else
                        {
                            // When mousescrolling over the hull, rescale the current part:
                            currentPrimitive.scale(1 - step * 0.1f);
                        }
                    }
                    break;
                case "MouseLeft":
                    value = -value;
                case "MouseRight":
                    // If the user has clicked, somewhere NOT on the preview part, rotate the hull.
                    if (leftButtonDown && !clickedPreviewPart)
                    {
                        hullNode.rotate(0, value * 3, 0);
                    }
                    break;
                case "MouseDown":
                    // If the user has clicked, somewhere NOT on the preview part, rotate the hull.
                    if (leftButtonDown && !clickedPreviewPart)
                    {
                        // Don't allow more that ~90° degrees.
                        if (hullNode.getParent().getLocalRotation().toAngles(null)[0] > FastMath.HALF_PI)
                        {
                            break;
                        }
                        hullNode.getParent().rotate(value * 3, 0, 0);
                    }
                    break;
                case "MouseUp":
                    if (leftButtonDown && !clickedPreviewPart)
                    {
                        // Don't allow more that ~90° degrees.
                        if (hullNode.getParent().getLocalRotation().toAngles(null)[0] < -FastMath.HALF_PI)
                        {
                            break;
                        }
                        hullNode.getParent().rotate(-value * 3, 0, 0);
                    }
                    break;
            }
        }

        /**
         * This listener is fired on non-analog events (keystrokes, clicks...)
         */
        @Override
        public void onAction(String name, boolean isPressed, float tpf)
        {
            clickedPreviewPart = MainApplication.getInstance().getInputManager().getCursorPosition().getX() < camera2.getWidth() * splitScreenRatio;
            switch (name)
            {
                case "LShift":
                    shiftDown = isPressed;
                    break;
                case "LCtrl":
                    ctrlDown = isPressed;
                    break;
                case "LButton":
                    leftButtonDown = isPressed;
                    break;
                case "RButton":
                    // On click:
                    if (isPressed)
                    {
                        // If we clicked the preview part:
                        if (clickedPreviewPart)
                        {
                            // do nothing
                        } else
                        {
                            // On right-clicking on the hull part, place the part where it is now.
                            Vector3f contactPoint = new Vector3f();
                            Vector3f contactNormal = new Vector3f();
                            Node nodeClicked;
                            nodeClicked = MainApplication.getNodeClicked(MainApplication.getInstance().getCamera(), contactPoint, contactNormal, hull);

                            // If we clicked somewhere on a non-empty hull, add the primitive to it.
                            boolean reGenerate = false;

                            if (nodeClicked != null)
                            {
                                // Convert coordinates to local system
                                Vector3f localPosition = hullNode.worldToLocal(contactPoint, null);
                                currentPrimitive.setLocalTranslation(localPosition);

                                //  contactNormal=hullNode.worldToLocal(contactNormal, null);
                                currentPrimitive.lookAt(contactNormal.negate(), contactNormal.cross(Vector3f.UNIT_X).normalizeLocal());

                                reGenerate = true;
                            } else
                            {
                                // If we clicked anywhere on an empty hull:
                                if (hull.isEmpty())
                                {
                                    // If nothing was aimed at, center it.
                                    currentPrimitive.setLocalTranslation(Vector3f.ZERO);
                                    currentPrimitive.setLocalRotation(Matrix3f.IDENTITY);
                                    reGenerate = true;
                                }
                            }

                            if (reGenerate)
                            {
                                // Add the preview and regenerate.
                                previewNode.detachAllChildren();
                                hull.attach(currentPrimitive);
                                hull.generateMesh();
                                // Reload the current primitive.
                                currentPrimitive = carousel.getInstanceOfCurrent();
                            }
                        }
                    }

                    break;
            }

        }
    }

    @Override
    public void update(float tpf)
    {
        super.update(tpf);

        // Update the carousel
        carousel.update(tpf);
    }
}
