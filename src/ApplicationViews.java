import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.OccupancyChartView;
import Presentation.Views.SignupView;

/**
 * Holds view objects while the application is being wired.
 */
public class ApplicationViews {
    private LoginView loginView;
    private SignupView signupView;
    private MainMenuView mainMenuView;
    private OccupancyChartView occupancyChartView;

    /** Gets the login view. */
    public LoginView getLoginView() {
        return loginView;
    }

    /** Sets the login view. */
    public void setLoginView(LoginView loginView) {
        this.loginView = loginView;
    }

    /** Gets the signup view. */
    public SignupView getSignupView() {
        return signupView;
    }

    /** Sets the signup view. */
    public void setSignupView(SignupView signupView) {
        this.signupView = signupView;
    }

    /** Gets the main menu view. */
    public MainMenuView getMainMenuView() {
        return mainMenuView;
    }

    /** Sets the main menu view. */
    public void setMainMenuView(MainMenuView mainMenuView) {
        this.mainMenuView = mainMenuView;
    }

    /** Gets the occupancy chart view. */
    public OccupancyChartView getOccupancyChartView() {
        return occupancyChartView;
    }

    /** Sets the occupancy chart view. */
    public void setOccupancyChartView(OccupancyChartView occupancyChartView) {
        this.occupancyChartView = occupancyChartView;
    }
}
