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
        String id = loginView.getUsernameOrEmail();
        String password = loginView.getPassword();

        // Provide UI feedback
        loginView.setLoadingState(true);

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
                int result = userService.authenticate(id, password);

                // Admin: verify password against config.json (UserService skips it by design)
                if (result == 1 && configService != null) {
                    String adminPass = configService.getAdminPassword();
                    if (adminPass != null && !password.equals(adminPass)) {
                        userService.clearSession();
                        return 0;
                    }
                }

                // Regular user: collect unread cancellation notifications
                if (result == 2 && reservationService != null) {
                    pendingNotifications = reservationService.getCancelledByAdminNotNotified(
                            userService.getLastLoggedInUserId());
                }

                return result;
            }

            /** Opens the next screen or reports login errors. */
            @Override
            protected void done() {
                try {
                    int success = get();
                    loginView.setLoadingState(false);

                    if (success == 1 || success == 2) {
                        loginProcedure(success);
                        for (Reservation r : pendingNotifications) {
                            mainMenuView.showWarning("Reservation Cancelled", buildAdminCancellationMessage(r));
                            reservationService.markNotified(r);
                        }
                    } else {
                        loginView.showError("Error", "Invalid credentials");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    loginView.setLoadingState(false);
                    LOGGER.log(Level.WARNING, "Login failed unexpectedly.", e);
                    loginView.showError("Error", "Login failed: " + e.getMessage());
                }
            }
        };
    }

    /** Switches from the login window to the main menu. */
    private void loginProcedure(int mode) {
        // 1. Hide the current window
        loginView.setVisible(false);
        loginView.dispose(); // Destroys the login window to free memory

        // 2. Configure the Main Menu BEFORE showing it
        mainMenuView.setMode(mode, userService.getLastLoggedInUsername());

        // 3. Open the new window
        SwingUtilities.invokeLater(() -> {
            mainMenuView.setVisible(true);
        });

        LOGGER.info("Switching to Main Menu.");
        LOGGER.info("Login success.");
    }

    /** Opens the signup window. */
    public void handleSignup() {
        LOGGER.info("Signup clicked.");

        // Disable multiple spamming presses
        loginView.setLoadingState(true);

        // 1. Hide the login window
        loginView.setVisible(false);

        // 2. Open the sign up page
        signupView.setVisible(true);
    }

    /** Logs out the current user and returns to the login window. */
    public void logout() {
        LOGGER.info("Logging out.");

        // 1. Clear session state from memory
        userService.clearSession();

        // 2. Reset and hide the Main Menu
        mainMenuView.resetDisplayedContent();
        mainMenuView.setVisible(false);

        // 2. Clear the old login data for security reasons
        loginView.clearFields();

        // 3. Reset the Login View (just in case it was left in a "Connecting..." state)
        loginView.setLoadingState(false);

        // 4. Show the Login View
        loginView.setVisible(true);
    }

    /** Starts account deletion after the user confirms the action. */
    public void handleDeleteAccount() {
        if (!mainMenuView.confirmDeleteAccount()) return;

        new SwingWorker<Void, Void>() {
            /** Deletes the current account away from the EDT. */
            @Override
            protected Void doInBackground() {
                userService.deleteCurrentUser();
                return null;
            }

            /** Returns to login or reports an account deletion error. */
            @Override
            protected void done() {
                try {
                    get();
                    mainMenuView.showInfo("Account Deleted", "Your account has been deleted.");
                    logout();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Account deletion failed.", e);
                    mainMenuView.showError("Account Deletion", "Could not delete account: " + e.getMessage());
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
        signupView.clearForm();

        // 2. Hide sign up page
        signupView.setVisible(false);

        // 3. Force the login view to reset its buttons and cursor
        loginView.setLoadingState(false);

        // 4. Show login window
        loginView.setVisible(true);
    }

    /** Validates the signup form and starts account creation. */
    public void handleRegistrationSubmission() {
        // 1. Grab the data
        String username = signupView.getUsername();
        String email = signupView.getEmail();
        String password = signupView.getPassword();
        String confirmPassword = signupView.getConfirmPassword();

        // 2. The "Everything" Check
        // If ANY of these are empty, show one message and stop.
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            signupView.showError("Input Error", "No fields can be left empty.");
            return;
        }

        // 3. Password match check
        if (!Objects.equals(password, confirmPassword)) {
            signupView.showError("Input Error", "Passwords do not match.");
            return;
        }

        // 4. Password policy check
        if (!userService.validatePassword(password)) {
            signupView.showError("Weak Password",
                    "Password must be at least 8 characters and include\nan uppercase letter, a lowercase letter, and a digit.\nExample: Test1234");
            return;
        }

        // 5. Email format check
        if (!userService.isEmailValid(email)) {
            signupView.showError("Invalid Email", "Please enter a valid email address.");
            return;
        }

        // 6. If the code gets here, we are good to go!
        LOGGER.info("Validation passed. Starting registration.");
        signupView.setLoadingState(true);
        createRegistrationWorker(username, email, password).execute();
    }

    /** Creates the background worker that saves a new account and logs it in. */
    SwingWorker<Integer, Void> createRegistrationWorker(String username, String email, String password) {
        return new SwingWorker<>() {
            /** Creates the account and authenticates it away from the EDT. */
            @Override
            protected Integer doInBackground() throws Exception {
                boolean registered = userService.register(username, email, password);
                if (!registered) {
                    return 0;
                }

                return userService.authenticate(username, password);
            }

            /** Opens the main menu or reports registration errors. */
            @Override
            protected void done() {
                signupView.setLoadingState(false);
                try {
                    int mode = get();
                    if (mode == 2) {
                        signupView.clearForm();
                        signupView.setVisible(false);
                        loginView.setLoadingState(false);
                        loginProcedure(mode);
                    } else {
                        signupView.showError("Registration Failed", "Username or email already in use.");
                    }
                } catch (Exception e) {
                    signupView.showError("Error", "Registration failed: " + e.getMessage());
                    LOGGER.log(Level.WARNING, "Registration failed unexpectedly.", e);
                }
            }
        };
    }
}
