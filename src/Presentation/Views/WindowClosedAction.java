package Presentation.Views;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Window listener that runs a simple action after a dialog has closed.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
public class WindowClosedAction extends WindowAdapter {
    private Runnable action;

    /**
     * Creates the listener with the action to run.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param action action to run after the window closes
     */
    public WindowClosedAction(Runnable action) {
        this.action = action;
    }

    /**
     * Handles window closed.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param event window event
     */
    @Override
    public void windowClosed(WindowEvent event) {
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
