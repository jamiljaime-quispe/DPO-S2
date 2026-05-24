package Presentation.Views;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Window listener that runs a simple action after a dialog has closed.
 */
public class WindowClosedAction extends WindowAdapter {
    private Runnable action;

    /**
     * Creates the listener with the action to run.
     *
     * @param action action to run after the window closes
     */
    public WindowClosedAction(Runnable action) {
        this.action = action;
    }

    /**
     * Runs the configured action after the window closes.
     *
     * @param event window event
     */
    @Override
    public void windowClosed(WindowEvent event) {
        runAction();
    }

    /** Runs the stored action if it exists. */
    private void runAction() {
        if (action != null) {
            action.run();
        }
    }
}
