package Presentation.Controllers;

import java.util.List;
import java.util.Set;

/**
 * Stores rows and codes loaded by one booking refresh worker.
 */
class BookingLoadData {
    private Set<String> loadedCodes;
    private List<BookingRow> rows;

    /**
     * Creates one booking refresh result.
     *
     * @param loadedCodes codes found during the refresh
     * @param rows        rows to publish to the table
     */
    BookingLoadData(Set<String> loadedCodes, List<BookingRow> rows) {
        this.loadedCodes = loadedCodes;
        this.rows = rows;
    }

    /** Gets the loaded codes. */
    Set<String> getLoadedCodes() {
        return loadedCodes;
    }

    /** Gets the rows loaded for the table. */
    List<BookingRow> getRows() {
        return rows;
    }
}
