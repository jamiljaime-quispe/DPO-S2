package Presentation.Controllers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Mouse listener that sends parking status table clicks back to the main controller.
 */
class ParkingSlotsClickListener extends MouseAdapter {
    private MainController controller;

    /**
     * Creates the listener.
     *
     * @param controller controller that handles parking table clicks
     */
    ParkingSlotsClickListener(MainController controller) {
        this.controller = controller;
    }

    /**
     * Handles a table click.
     *
     * @param event mouse event
     */
    @Override
    public void mouseClicked(MouseEvent event) {
        handleMouseClicked(event);
    }

    /**
     * Sends the click to the controller when it exists.
     *
     * @param event mouse event
     */
    private void handleMouseClicked(MouseEvent event) {
        if (controller != null) {
            controller.handleParkingSlotsTableClick(event);
        }
    }
}
