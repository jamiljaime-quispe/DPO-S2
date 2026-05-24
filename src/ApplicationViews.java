import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.OccupancyChartView;
import Presentation.Views.SignupView;

/**
 * Holds view objects while the application is being wired.
 * <p>
 * This helper keeps the application setup readable by grouping related objects or startup steps instead of
 * leaving all details in one long method.
 * </p>
 */
public class ApplicationViews {
    private LoginView loginView;
    private SignupView signupView;
    private MainMenuView mainMenuView;
    private OccupancyChartView occupancyChartView;

    /**
     * Gets the login view.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current login view
     */
    public LoginView getLoginView() {
        return loginView;
    }

    /**
     * Sets the login view.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param loginView login view that will be shown or updated
     */
    public void setLoginView(LoginView loginView) {
        this.loginView = loginView;
    }

    /**
     * Gets the signup view.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current signup view
     */
    public SignupView getSignupView() {
        return signupView;
    }

    /**
     * Sets the signup view.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param signupView signup view that will be shown or updated
     */
    public void setSignupView(SignupView signupView) {
        this.signupView = signupView;
    }

    /**
     * Gets the main menu view.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current main menu view
     */
    public MainMenuView getMainMenuView() {
        return mainMenuView;
    }

    /**
     * Sets the main menu view.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param mainMenuView main menu view that will be shown or updated
     */
    public void setMainMenuView(MainMenuView mainMenuView) {
        this.mainMenuView = mainMenuView;
    }

    /**
     * Gets the occupancy chart view.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current occupancy chart view
     */
    public OccupancyChartView getOccupancyChartView() {
        return occupancyChartView;
    }

    /**
     * Sets the occupancy chart view.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param occupancyChartView occupancy chart view that will be shown or updated
     */
    public void setOccupancyChartView(OccupancyChartView occupancyChartView) {
        this.occupancyChartView = occupancyChartView;
    }
}
