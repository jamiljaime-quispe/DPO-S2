package Presentation.Views;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Window listener that runs a simple action when a window starts closing.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
public class WindowClosingAction extends WindowAdapter {
    private Runnable action;

    /**
     * Creates the listener with the action to run.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param action action to run while the window is closing
     */
    public WindowClosingAction(Runnable action) {
        this.action = action;
    }

    /**
     * Handles window closing.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param event window event
     */
    @Override
    public void windowClosing(WindowEvent event) {
        runAction();
    }

    /**
     * Handles run action.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void runAction() {
        if (action != null) {
            action.run();
        }
    }
}
