package Presentation.Controllers;

import java.util.List;
import java.util.Set;

/**
 * Stores rows and codes loaded by one booking refresh worker.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
 */
class BookingLoadData {
    private Set<String> loadedCodes;
    private List<BookingRow> rows;

    /**
     * Creates one booking refresh result.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param loadedCodes codes found during the refresh
     * @param rows rows to publish to the table
     */
    BookingLoadData(Set<String> loadedCodes, List<BookingRow> rows) {
        this.loadedCodes = loadedCodes;
        this.rows = rows;
    }

    /**
     * Gets the loaded codes.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current loaded codes
     */
    Set<String> getLoadedCodes() {
        return loadedCodes;
    }

    /**
     * Gets the rows loaded for the table.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current rows
     */
    List<BookingRow> getRows() {
        return rows;
    }
}
