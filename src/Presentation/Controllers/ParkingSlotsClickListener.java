package Presentation.Controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Mouse listener that sends parking status table clicks back to the main controller.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
 */
class ParkingSlotsClickListener extends MouseAdapter {
    private MainController controller;

    /**
     * Creates the listener.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param controller controller that handles parking table clicks
     */
    ParkingSlotsClickListener(MainController controller) {
        this.controller = controller;
    }

    /**
     * Handles mouse clicked.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param event mouse event
     */
    @Override
    public void mouseClicked(MouseEvent event) {
        handleMouseClicked(event);
    }

    /**
     * Handles mouse clicked.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     *
     * @param event mouse event
     */
    private void handleMouseClicked(MouseEvent event) {
        if (controller != null) {
            controller.handleParkingSlotsTableClick(event);
        }
    }
}
