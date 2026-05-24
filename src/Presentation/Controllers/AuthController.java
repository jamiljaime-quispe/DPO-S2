package Presentation.Controllers;

import Business.Entities.Reservation;
import Business.Services.ConfigService;
import Business.Services.ReservationService;
import Business.Services.UserService;
import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.SignupView;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for user authentication (login, signup, logout, account deletion).
 * Runs credential verification in a background thread to keep the EDT responsive.
 */
public class AuthController {
    private static final Logger LOGGER = Logger.getLogger(AuthController.class.getName());

    private LoginView loginView;
    private SignupView signupView;
    private UserService userService;
    private MainController mainController;
    private MainMenuView mainMenuView;
    private ConfigService configService;
    private ReservationService reservationService;

    /**
     * Constructs the controller with login and signup views.
     *
     * @param loginView  login window
     * @param signupView signup window
     * @param userService service used for user accounts
     */
    public AuthController(LoginView loginView, SignupView signupView, UserService userService) {
        this.loginView = loginView;
        this.signupView = signupView;
        this.userService = userService;
    }

    /**
     * Constructs the controller with the login view.
     *
     * @param loginView login window
     * @param userService service used for user accounts
     */
    public AuthController(LoginView loginView, UserService userService) {
        this.loginView = loginView;
        this.userService = userService;
    }

    /** Starts the login process using the data entered in the login view. */
    public void handleLogin() {
        // Grab data from view on EDT
        String id = getLoginIdentifier();
        String password = getLoginPassword();

        // Provide UI feedback
        setLoginLoading(true);

        // Execute background task
        createLoginWorker(id, password).execute();
    }

    /** Creates the background worker that validates login credentials. */
    private SwingWorker<Integer, Void> createLoginWorker(String id, String password) {
        return new SwingWorker<>() {
            private List<Reservation> pendingNotifications = new ArrayList<>();

            /** Checks credentials away from the EDT. */
            @Override
            protected Integer doInBackground() throws Exception {
                int result = authenticateUser(id, password);

                // Admin: verify password against config.json (UserService skips it by design)
                if (result == 1 && hasConfigService()) {
                    String adminPass = getConfiguredAdminPassword();
                    if (adminPass != null && !password.equals(adminPass)) {
                        clearUserSession();
                        return 0;
                    }
                }

                // Regular user: collect unread cancellation notifications
                if (result == 2 && hasReservationService()) {
                    pendingNotifications = loadPendingAdminCancellationNotifications();
                }

                return result;
            }

            /** Opens the next screen or reports login errors. */
            @Override
            protected void done() {
                try {
                    int success = get();
                    setLoginLoading(false);

                    if (success == 1 || success == 2) {
                        loginProcedure(success);
                        for (Reservation r : pendingNotifications) {
                            showAdminCancellationWarning(r);
                            markReservationNotified(r);
                        }
                    } else {
                        showLoginError("Error", "Invalid credentials");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    setLoginLoading(false);
                    logLoginFailure(e);
                    showLoginError("Error", "Login failed: " + e.getMessage());
                }
            }
        };
    }

    /** Switches from the login window to the main menu. */
    private void loginProcedure(int mode) {
        // 1. Hide the current window
        hideLoginView();
        disposeLoginView(); // Destroys the login window to free memory

        // 2. Configure the Main Menu BEFORE showing it
        configureMainMenu(mode);

        // 3. Open the new window
        showMainMenuLater();

        LOGGER.info("Switching to Main Menu.");
        LOGGER.info("Login success.");
    }

    /** Opens the signup window. */
    public void handleSignup() {
        LOGGER.info("Signup clicked.");

        // Disable multiple spamming presses
        setLoginLoading(true);

        // 1. Hide the login window
        hideLoginView();

        // 2. Open the sign up page
        showSignupView();
    }

    /** Logs out the current user and returns to the login window. */
    public void logout() {
        LOGGER.info("Logging out.");

        // 1. Clear controller/view data that could still contain the previous user.
        clearMainSessionState();

        // 2. Clear session state from the business service.
        clearUserSession();

        // 3. Reset and hide the Main Menu.
        resetMainMenuContent();
        hideMainMenuView();

        // 4. Clear old form data for security reasons.
        clearLoginFields();
        clearSignupForm();

        // 5. Reset the Login View in case it was left in a loading state.
        setLoginLoading(false);

        // 6. Show the Login View.
        showLoginView();
    }

    /** Starts account deletion after the user confirms the action. */
    public void handleDeleteAccount() {
        if (!confirmDeleteAccount()) return;

        new SwingWorker<Void, Void>() {
            /** Deletes the current account away from the EDT. */
            @Override
            protected Void doInBackground() {
                deleteCurrentUser();
                return null;
            }

            /** Returns to login or reports an account deletion error. */
            @Override
            protected void done() {
                try {
                    get();
                    showMainInfo("Account Deleted", "Your account has been deleted.");
                    logout();
                } catch (InterruptedException | ExecutionException e) {
                    logAccountDeletionFailure(e);
                    showMainError("Account Deletion", "Could not delete account: " + e.getMessage());
                }
            }
        }.execute();
    }

    /** Sets the main menu controller and view used after login. */
    public void setMainMenuController(MainController mainController, MainMenuView mainMenuView) {
        this.mainController = mainController;
        this.mainMenuView = mainMenuView;
    }

    /** Sets the signup view used by this controller. */
    public void setSignupView(SignupView signupView) {
        this.signupView = signupView;
    }

    /** Sets the service used to read application configuration. */
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    /** Sets the service used for reservation notifications. */
    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** Builds the warning shown when an admin cancelled or moved a reservation. */
    private String buildAdminCancellationMessage(Reservation reservation) {
        String originalSpace = reservation.getPreviousSpaceCode();
        if (originalSpace == null || originalSpace.isBlank()) {
            originalSpace = reservation.getParkingSpace() != null
                    ? reservation.getParkingSpace().getId()
                    : "unknown";
        }

        StringBuilder message = new StringBuilder()
                .append("Your reservation for space \"")
                .append(originalSpace)
                .append("\" was cancelled by an administrator.");

        if (reservation.isActive() && reservation.getParkingSpace() != null) {
            message.append("\n\nA substitute reservation has been assigned to space \"")
                    .append(reservation.getParkingSpace().getId())
                    .append("\".");
        } else {
            message.append("\n\nNo equivalent space was available, so the reservation has been removed.");
        }

        return message.toString();
    }

    /** Returns from the signup window to the login window. */
    public void handleBackToLogin() {
        // 1. Clear the fields so it's clean if they come back
        clearSignupForm();

        // 2. Hide sign up page
        hideSignupView();

        // 3. Force the login view to reset its buttons and cursor
        setLoginLoading(false);

        // 4. Show login window
        showLoginView();
    }

    /** Validates the signup form and starts account creation. */
    public void handleRegistrationSubmission() {
        // 1. Grab the data
        String username = getSignupUsername();
        String email = getSignupEmail();
        String password = getSignupPassword();
        String confirmPassword = getSignupConfirmPassword();

        // 2. The "Everything" Check
        // If ANY of these are empty, show one message and stop.
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showSignupError("Input Error", "No fields can be left empty.");
            return;
        }

        // 3. Password match check
        if (!passwordsMatch(password, confirmPassword)) {
            showSignupError("Input Error", "Passwords do not match.");
            return;
        }

        // 4. Password policy check
        if (!isPasswordValid(password)) {
            showSignupError("Weak Password",
                    "Password must be at least 8 characters and include\nan uppercase letter, a lowercase letter, and a digit.\nExample: Test1234");
            return;
        }

        // 5. Email format check
        if (!isEmailValid(email)) {
            showSignupError("Invalid Email", "Please enter a valid email address.");
            return;
        }

        // 6. If the code gets here, we are good to go!
        LOGGER.info("Validation passed. Starting registration.");
        setSignupLoading(true);
        createRegistrationWorker(username, email, password).execute();
    }

    /** Creates the background worker that saves a new account and logs it in. */
    SwingWorker<Integer, Void> createRegistrationWorker(String username, String email, String password) {
        return new SwingWorker<>() {
            /** Creates the account and authenticates it away from the EDT. */
            @Override
            protected Integer doInBackground() throws Exception {
                boolean registered = registerUser(username, email, password);
                if (!registered) {
                    return 0;
                }

                return authenticateUser(username, password);
            }

            /** Opens the main menu or reports registration errors. */
            @Override
            protected void done() {
                setSignupLoading(false);
                try {
                    int mode = get();
                    if (mode == 2) {
                        clearSignupForm();
                        hideSignupView();
                        setLoginLoading(false);
                        loginProcedure(mode);
                    } else {
                        showSignupError("Registration Failed", "Username or email already in use.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    showSignupError("Error", "Registration failed: " + e.getMessage());
                    logRegistrationFailure(e);
                }
            }
        };
    }

    /** Gets the login identifier entered by the user. */
    private String getLoginIdentifier() {
        return loginView.getUsernameOrEmail();
    }

    /** Gets the login password entered by the user. */
    private String getLoginPassword() {
        return loginView.getPassword();
    }

    /** Sets the login view loading state. */
    private void setLoginLoading(boolean loading) {
        loginView.setLoadingState(loading);
    }

    /** Authenticates a user through the user service. */
    private int authenticateUser(String id, String password) {
        return userService.authenticate(id, password);
    }

    /** Checks whether configuration is available. */
    private boolean hasConfigService() {
        return configService != null;
    }

    /** Gets the configured admin password. */
    private String getConfiguredAdminPassword() {
        return configService.getAdminPassword();
    }

    /** Clears the logged-in user session. */
    private void clearUserSession() {
        userService.clearSession();
    }

    /** Clears all presentation state connected to the current session. */
    private void clearMainSessionState() {
        if (mainController != null) {
            mainController.clearSessionState();
        }
    }

    /** Checks whether reservation notifications are available. */
    private boolean hasReservationService() {
        return reservationService != null;
    }

    /** Loads pending admin cancellation notifications for the current user. */
    private List<Reservation> loadPendingAdminCancellationNotifications() {
        return reservationService.getCancelledByAdminNotNotified(getCurrentUserId());
    }

    /** Gets the current logged-in user ID. */
    private int getCurrentUserId() {
        return userService.getLastLoggedInUserId();
    }

    /** Shows one admin cancellation warning. */
    private void showAdminCancellationWarning(Reservation reservation) {
        mainMenuView.showWarning("Reservation Cancelled", buildAdminCancellationMessage(reservation));
    }

    /** Marks a reservation notification as shown. */
    private void markReservationNotified(Reservation reservation) {
        reservationService.markNotified(reservation);
    }

    /** Shows an error in the login view. */
    private void showLoginError(String title, String message) {
        loginView.showError(title, message);
    }

    /** Logs a login failure. */
    private void logLoginFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Login failed unexpectedly.", e);
    }

    /** Hides the login view. */
    private void hideLoginView() {
        loginView.setVisible(false);
    }

    /** Disposes the login view. */
    private void disposeLoginView() {
        loginView.dispose();
    }

    /** Configures the main menu for the logged-in user. */
    private void configureMainMenu(int mode) {
        mainMenuView.setMode(mode, userService.getLastLoggedInUsername());
        mainController.setMode(mode);
    }

    /** Shows the main menu on the EDT. */
    private void showMainMenuLater() {
        SwingUtilities.invokeLater(() -> mainMenuView.setVisible(true));
    }

    /** Shows the signup view. */
    private void showSignupView() {
        signupView.setVisible(true);
    }

    /** Resets the main menu content. */
    private void resetMainMenuContent() {
        mainMenuView.resetDisplayedContent();
    }

    /** Hides the main menu view. */
    private void hideMainMenuView() {
        mainMenuView.setVisible(false);
    }

    /** Clears fields in the login view. */
    private void clearLoginFields() {
        loginView.clearFields();
    }

    /** Shows the login view. */
    private void showLoginView() {
        loginView.setVisible(true);
    }

    /** Asks the user to confirm account deletion. */
    private boolean confirmDeleteAccount() {
        return mainMenuView.confirmDeleteAccount();
    }

    /** Deletes the current user account through the user service. */
    private void deleteCurrentUser() {
        userService.deleteCurrentUser();
    }

    /** Shows an informational message in the main menu. */
    private void showMainInfo(String title, String message) {
        mainMenuView.showInfo(title, message);
    }

    /** Shows an error in the main menu. */
    private void showMainError(String title, String message) {
        mainMenuView.showError(title, message);
    }

    /** Logs an account deletion failure. */
    private void logAccountDeletionFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Account deletion failed.", e);
    }

    /** Clears the signup form. */
    private void clearSignupForm() {
        signupView.clearForm();
    }

    /** Hides the signup view. */
    private void hideSignupView() {
        signupView.setVisible(false);
    }

    /** Gets the username entered in the signup form. */
    private String getSignupUsername() {
        return signupView.getUsername();
    }

    /** Gets the email entered in the signup form. */
    private String getSignupEmail() {
        return signupView.getEmail();
    }

    /** Gets the password entered in the signup form. */
    private String getSignupPassword() {
        return signupView.getPassword();
    }

    /** Gets the repeated password entered in the signup form. */
    private String getSignupConfirmPassword() {
        return signupView.getConfirmPassword();
    }

    /** Shows an error in the signup view. */
    private void showSignupError(String title, String message) {
        signupView.showError(title, message);
    }

    /** Checks whether both signup passwords match. */
    private boolean passwordsMatch(String password, String confirmPassword) {
        return Objects.equals(password, confirmPassword);
    }

    /** Checks the password policy through the user service. */
    private boolean isPasswordValid(String password) {
        return userService.validatePassword(password);
    }

    /** Checks email format through the user service. */
    private boolean isEmailValid(String email) {
        return userService.isEmailValid(email);
    }

    /** Sets the signup view loading state. */
    private void setSignupLoading(boolean loading) {
        signupView.setLoadingState(loading);
    }

    /** Registers a user through the user service. */
    private boolean registerUser(String username, String email, String password) {
        return userService.register(username, email, password);
    }

    /** Logs a registration failure. */
    private void logRegistrationFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Registration failed unexpectedly.", e);
    }
}
