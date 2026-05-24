package Presentation.Views;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Window listener that runs a simple action when a window starts closing.
 */
public class WindowClosingAction extends WindowAdapter {
    private Runnable action;

    /**
     * Creates the listener with the action to run.
     *
     * @param action action to run while the window is closing
     */
    public WindowClosingAction(Runnable action) {
        this.action = action;
    }

    /**
     * Runs the configured action while the window is closing.
     *
     * @param event window event
     */
    @Override
    public void windowClosing(WindowEvent event) {
        runAction();
    }

    /** Runs the stored action if it exists. */
    private void runAction() {
        if (action != null) {
            action.run();
        }
    }
}
