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
 * Controller for user authentication (login, signup, logout, account deletion). Runs credential
 * verification in a background thread to keep the EDT responsive.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
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
     * Constructs the controller with the login view.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param loginView login window
     * @param userService service used for user accounts
     */
    public AuthController(LoginView loginView, UserService userService) {
        this.loginView = loginView;
        this.userService = userService;
    }

    /**
     * Starts the login process using the data entered in the login view.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
    public void handleLogin() {
        // Grab data from view on EDT
        String id = getLoginIdentifier();
        String password = getLoginPassword();

        // Provide UI feedback
        setLoginLoading(true);

        // Execute background task
        createLoginWorker(id, password).execute();
    }

    /**
     * Creates the background worker that validates login credentials.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param ID ID used by this operation
     * @param password password entered by the user
     * @return the created login worker
     */
    private SwingWorker<Integer, Void> createLoginWorker(String id, String password) {
        return new SwingWorker<>() {
            private List<Reservation> pendingNotifications = new ArrayList<>();

            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the result of the operation
             * @throws Exception if the operation cannot be completed correctly
             */
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

            /**
             * Opens the next screen or reports login errors.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Handles login procedure.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param mode mode used by this operation
     */
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

    /**
     * Handles signup.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
    public void handleSignup() {
        LOGGER.info("Signup clicked.");

        // Disable multiple spamming presses
        setLoginLoading(true);

        // 1. Hide the login window
        hideLoginView();

        // 2. Open the sign up page
        showSignupView();
    }

    /**
     * Handles logout.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
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

    /**
     * Handles delete account.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
    public void handleDeleteAccount() {
        if (!confirmDeleteAccount()) return;

        new SwingWorker<Void, Void>() {
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the result of the operation
             */
            @Override
            protected Void doInBackground() {
                deleteCurrentUser();
                return null;
            }

            /**
             * Returns to login or reports an account deletion error.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Sets the main menu controller and view used after login.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param mainController main controller that coordinates the related screen action
     * @param mainMenuView main menu view that will be shown or updated
     */
    public void setMainMenuController(MainController mainController, MainMenuView mainMenuView) {
        this.mainController = mainController;
        this.mainMenuView = mainMenuView;
    }

    /**
     * Sets the signup view used by this controller.
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
     * Sets the service used to read application configuration.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param configService config service used to apply the needed project logic
     */
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Sets the service used for reservation notifications.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param reservationService reservation service used to apply the needed project logic
     */
    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Builds admin cancellation message.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param reservation reservation used by this operation
     * @return the built admin cancellation message
     */
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

    /**
     * Returns from the signup window to the login window.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
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

    /**
     * Handles registration submission.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
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

    /**
     * Creates the background worker that saves a new account and logs it in.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param username username entered or stored for the user
     * @param email email entered or stored for the user
     * @param password password entered by the user
     * @return the created registration worker
     */
    SwingWorker<Integer, Void> createRegistrationWorker(String username, String email, String password) {
        return new SwingWorker<>() {
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the result of the operation
             * @throws Exception if the operation cannot be completed correctly
             */
            @Override
            protected Integer doInBackground() throws Exception {
                boolean registered = registerUser(username, email, password);
                if (!registered) {
                    return 0;
                }

                return authenticateUser(username, password);
            }

            /**
             * Finishes the worker task on the Swing screen thread.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Gets the login identifier entered by the user.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current login identifier
     */
    private String getLoginIdentifier() {
        return loginView.getUsernameOrEmail();
    }

    /**
     * Gets the login password entered by the user.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current login password
     */
    private String getLoginPassword() {
        return loginView.getPassword();
    }

    /**
     * Sets the login view loading state.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param loading true while the screen is waiting for an operation to finish
     */
    private void setLoginLoading(boolean loading) {
        loginView.setLoadingState(loading);
    }

    /**
     * Handles authenticate user.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param ID ID used by this operation
     * @param password password entered by the user
     * @return the result of the operation
     */
    private int authenticateUser(String id, String password) {
        return userService.authenticate(id, password);
    }

    /**
     * Checks whether config service exists.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean hasConfigService() {
        return configService != null;
    }

    /**
     * Gets the configured admin password.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current configured admin password
     */
    private String getConfiguredAdminPassword() {
        return configService.getAdminPassword();
    }

    /**
     * Handles clear user session.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearUserSession() {
        userService.clearSession();
    }

    /**
     * Handles clear main session state.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearMainSessionState() {
        if (mainController != null) {
            mainController.clearSessionState();
        }
    }

    /**
     * Checks whether reservation service exists.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean hasReservationService() {
        return reservationService != null;
    }

    /**
     * Loads pending admin cancellation notifications.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     *
     * @return the loaded pending admin cancellation notifications
     */
    private List<Reservation> loadPendingAdminCancellationNotifications() {
        return reservationService.getCancelledByAdminNotNotified(getCurrentUserId());
    }

    /**
     * Gets the current logged-in user ID.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current current user ID
     */
    private int getCurrentUserId() {
        return userService.getLastLoggedInUserId();
    }

    /**
     * Shows admin cancellation warning.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param reservation reservation used by this operation
     */
    private void showAdminCancellationWarning(Reservation reservation) {
        mainMenuView.showWarning("Reservation Cancelled", buildAdminCancellationMessage(reservation));
    }

    /**
     * Handles mark reservation notified.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param reservation reservation used by this operation
     */
    private void markReservationNotified(Reservation reservation) {
        reservationService.markNotified(reservation);
    }

    /**
     * Shows an error in the login view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param title title used by this operation
     * @param message message shown to the user or written to the log
     */
    private void showLoginError(String title, String message) {
        loginView.showError(title, message);
    }

    /**
     * Handles log login failure.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param e e used by this operation
     */
    private void logLoginFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Login failed unexpectedly.", e);
    }

    /**
     * Hides the login view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void hideLoginView() {
        loginView.setVisible(false);
    }

    /**
     * Disposes the login view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void disposeLoginView() {
        loginView.dispose();
    }

    /**
     * Handles configure main menu.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param mode mode used by this operation
     */
    private void configureMainMenu(int mode) {
        mainMenuView.setMode(mode, userService.getLastLoggedInUsername());
        mainController.setMode(mode);
    }

    /**
     * Shows main menu later.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showMainMenuLater() {
        SwingUtilities.invokeLater(() -> mainMenuView.setVisible(true));
    }

    /**
     * Shows the signup view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showSignupView() {
        signupView.setVisible(true);
    }

    /**
     * Handles reset main menu content.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void resetMainMenuContent() {
        mainMenuView.resetDisplayedContent();
    }

    /**
     * Hides the main menu view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void hideMainMenuView() {
        mainMenuView.setVisible(false);
    }

    /**
     * Clears fields in the login view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearLoginFields() {
        loginView.clearFields();
    }

    /**
     * Shows the login view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showLoginView() {
        loginView.setVisible(true);
    }

    /**
     * Confirms delete account.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return the answer chosen by the user
     */
    private boolean confirmDeleteAccount() {
        return mainMenuView.confirmDeleteAccount();
    }

    /**
     * Deletes current user.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void deleteCurrentUser() {
        userService.deleteCurrentUser();
    }

    /**
     * Shows main info.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param title title used by this operation
     * @param message message shown to the user or written to the log
     */
    private void showMainInfo(String title, String message) {
        mainMenuView.showInfo(title, message);
    }

    /**
     * Shows main error.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param title title used by this operation
     * @param message message shown to the user or written to the log
     */
    private void showMainError(String title, String message) {
        mainMenuView.showError(title, message);
    }

    /**
     * Handles log account deletion failure.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param e e used by this operation
     */
    private void logAccountDeletionFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Account deletion failed.", e);
    }

    /**
     * Handles clear signup form.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearSignupForm() {
        signupView.clearForm();
    }

    /**
     * Hides the signup view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void hideSignupView() {
        signupView.setVisible(false);
    }

    /**
     * Gets the username entered in the signup form.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current signup username
     */
    private String getSignupUsername() {
        return signupView.getUsername();
    }

    /**
     * Gets the email entered in the signup form.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current signup email
     */
    private String getSignupEmail() {
        return signupView.getEmail();
    }

    /**
     * Gets the password entered in the signup form.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current signup password
     */
    private String getSignupPassword() {
        return signupView.getPassword();
    }

    /**
     * Gets the repeated password entered in the signup form.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current signup confirm password
     */
    private String getSignupConfirmPassword() {
        return signupView.getConfirmPassword();
    }

    /**
     * Shows an error in the signup view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param title title used by this operation
     * @param message message shown to the user or written to the log
     */
    private void showSignupError(String title, String message) {
        signupView.showError(title, message);
    }

    /**
     * Handles passwords match.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param password password entered by the user
     * @param confirmPassword password entered by the user
     * @return the result of the operation
     */
    private boolean passwordsMatch(String password, String confirmPassword) {
        return Objects.equals(password, confirmPassword);
    }

    /**
     * Checks whether password valid.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param password password entered by the user
     * @return true when the condition is met, false otherwise
     */
    private boolean isPasswordValid(String password) {
        return userService.validatePassword(password);
    }

    /**
     * Checks whether email valid.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param email email entered or stored for the user
     * @return true when the condition is met, false otherwise
     */
    private boolean isEmailValid(String email) {
        return userService.isEmailValid(email);
    }

    /**
     * Sets the signup view loading state.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param loading true while the screen is waiting for an operation to finish
     */
    private void setSignupLoading(boolean loading) {
        signupView.setLoadingState(loading);
    }

    /**
     * Handles register user.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param username username entered or stored for the user
     * @param email email entered or stored for the user
     * @param password password entered by the user
     * @return the result of the operation
     */
    private boolean registerUser(String username, String email, String password) {
        return userService.register(username, email, password);
    }

    /**
     * Handles log registration failure.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param e e used by this operation
     */
    private void logRegistrationFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Registration failed unexpectedly.", e);
    }
}
