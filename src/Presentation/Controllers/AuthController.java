package Presentation.Controllers;

import Business.Entities.Reservation;
import Business.Services.ConfigService;
import Business.Services.ReservationService;
import Business.Services.UserService;
import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.SignupView;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Controller for user authentication (login, signup, logout, account deletion).
 * Runs credential verification in a background thread to keep the EDT responsive.
 */
public class AuthController {
    private LoginView loginView;
    private SignupView signupView;
    private UserService userService;
    private MainController mainController;
    private MainMenuView mainMenuView;
    private ConfigService configService;
    private ReservationService reservationService;

    // Fixed constructor to use SignupView instead of int
    public AuthController(LoginView loginView, SignupView signupView, UserService userService) {
        this.loginView = loginView;
        this.signupView = signupView;
        this.userService = userService;
    }

    public AuthController(LoginView loginView, UserService userService) {
        this.loginView = loginView;
        this.userService = userService;
    }

    public void handleLogin() {
        // Grab data from view on EDT
        String id = loginView.getUsernameOrEmail();
        String password = loginView.getPassword();

        // Provide UI feedback
        loginView.setLoadingState(true);

        // Execute background task
        createLoginWorker(id, password).execute();
    }

    private SwingWorker<Integer, Void> createLoginWorker(String id, String password) {
        return new SwingWorker<>() {
            private List<Reservation> pendingNotifications = new ArrayList<>();

            @Override
            protected Integer doInBackground() throws Exception {
                int result = userService.authenticate(id, password);

                // Admin: verify password against config.json (UserService skips it by design)
                if (result == 1 && configService != null) {
                    String adminPass = configService.getAdminPassword();
                    if (adminPass != null && !password.equals(adminPass)) {
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

            @Override
            protected void done() {
                try {
                    int success = get();
                    loginView.setLoadingState(false);

                    if (success == 1 || success == 2) {
                        loginProcedure(success);
                        for (Reservation r : pendingNotifications) {
                            String spaceCode = r.getParkingSpace() != null
                                    ? r.getParkingSpace().getId() : "unknown";
                            JOptionPane.showMessageDialog(mainMenuView,
                                    "Your reservation for space \"" + spaceCode
                                            + "\" was cancelled by an admin.",
                                    "Reservation Cancelled",
                                    JOptionPane.WARNING_MESSAGE);
                            reservationService.markNotified(r);
                        }
                    } else {
                        JOptionPane.showMessageDialog(loginView, "Invalid credentials", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    loginView.setLoadingState(false);
                    e.printStackTrace();
                }
            }
        };
    }

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

        System.out.println("Switching to Main Menu...");

        System.out.println("Login Success");
    }

    public void handleSignup() {
        System.out.println("Signup clicked");

        // Disable multiple spamming presses
        loginView.setLoadingState(true);

        // 1. Hide the login window
        loginView.setVisible(false);

        // 2. Open the sign up page
        signupView.setVisible(true);
    }

    public void logout() {
        System.out.println("Logging out...");

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

    public void handleDeleteAccount() {
        int confirm = JOptionPane.showConfirmDialog(mainMenuView,
                "Are you sure you want to delete your account?\nThis action cannot be undone.",
                "Delete Account",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                userService.deleteCurrentUser();
                return null;
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(mainMenuView,
                        "Your account has been deleted.",
                        "Account Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
                logout();
            }
        }.execute();
    }

    public void setMainMenuController(MainController mainController, MainMenuView mainMenuView) {
        this.mainController = mainController;
        this.mainMenuView = mainMenuView;
    }

    public void setSignupView(SignupView signupView) {
        this.signupView = signupView;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

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

    public void handleRegistrationSubmission() {
        // 1. Grab the data
        String username = signupView.getUsername();
        String email = signupView.getEmail();
        String password = signupView.getPassword();
        String confirmPassword = signupView.getConfirmPassword();

        // 2. The "Everything" Check
        // If ANY of these are empty, show one message and stop.
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(signupView,
                    "No fields can be left empty.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Password match check
        if (!Objects.equals(password, confirmPassword)) {
            JOptionPane.showMessageDialog(signupView,
                    "Passwords do not match.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Password policy check
        if (!userService.validatePassword(password)) {
            JOptionPane.showMessageDialog(signupView,
                    "Password must be at least 8 characters and include\nan uppercase letter, a lowercase letter, and a digit.\nExample: Test1234",
                    "Weak Password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Email format check
        if (!userService.isEmailValid(email)) {
            JOptionPane.showMessageDialog(signupView,
                    "Please enter a valid email address.",
                    "Invalid Email",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 6. If the code gets here, we are good to go!
        System.out.println("Validation passed. Starting registration...");
        signupView.setLoadingState(true);
        createRegistrationWorker(username, email, password).execute();
    }

    SwingWorker<Integer, Void> createRegistrationWorker(String username, String email, String password) {
        return new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                boolean registered = userService.register(username, email, password);
                if (!registered) {
                    return 0;
                }

                return userService.authenticate(username, password);
            }

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
                        JOptionPane.showMessageDialog(signupView,
                                "Username or email already in use.",
                                "Registration Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(signupView,
                            "Registration failed: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
    }
}
