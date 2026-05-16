package Presentation.Controllers;

import Business.Entities.Reservation;
import Business.Services.ReservationService;
import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.SignupView;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class AuthController {
    private LoginView loginView;
    private SignupView signupView;
    private UserService userService;
    private MainController mainController;
    private MainMenuView mainMenuView;
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
            @Override
            protected Integer doInBackground() throws Exception {
                // Background thread: Calls UserService in the same package
                return userService.authenticate(id, password);
            }

            @Override
            protected void done() {
                try {
                    int success = get();
                    loginView.setLoadingState(false);

                    // Load admin screen
                    if (success == 1) {
                        loginProcedure(success);
                    } else
                    // Load regular user screen
                    if (success == 2) {
                        loginProcedure(success);
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
            if (mode == 2) {
                showAdminCancellationNotifications();
            }
        });

        System.out.println("Switching to Main Menu...");

        System.out.println("Login Success");
    }

    private void showAdminCancellationNotifications() {
        if (reservationService == null || userService.lastLoggedInUserId <= 0)
            return;
        List<Reservation> cancelled = reservationService.getCancelledByAdminNotNotified(userService.lastLoggedInUserId);
        if (cancelled.isEmpty())
            return;

        StringBuilder message = new StringBuilder(
                "The following reservation(s) were cancelled by an administrator:\n\n");
        for (Reservation r : cancelled) {
            String spaceCode = r.getParkingSpace() != null ? r.getParkingSpace().getId() : "unknown";
            String plate = r.getVehicle() != null ? r.getVehicle().getLicensePlate() : "unknown";
            message.append("• Space ").append(spaceCode).append(", vehicle ").append(plate).append("\n");
        }
        message.append("\nYou may book a new space from Manage slot booking.");

        JOptionPane.showMessageDialog(mainMenuView,
                message.toString(),
                "Reservation cancelled by admin",
                JOptionPane.INFORMATION_MESSAGE);
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

        // 1. Hide the Main Menu
        mainMenuView.setVisible(false);

        // 2. Clear the old login data for security reasons
        loginView.clearFields();

        // 3. Reset the Login View (just in case it was left in a "Connecting..." state)
        loginView.setLoadingState(false);

        // 4. Show the Login View
        loginView.setVisible(true);
    }

    /** @deprecated Stub kept for review; use {@link #handleDeleteAccount()} instead. */
    public void deleteAccount(int userId) {
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

    public void setMainMenuController(MainController mainController, MainMenuView mainMenuView,
                                      ReservationService reservationService) {
        this.mainController = mainController;
        this.mainMenuView = mainMenuView;
        this.reservationService = reservationService;
    }

    public void setSignupView(SignupView signupView) {
        this.signupView = signupView;
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

        // 3. The Password Match Check
        if (!Objects.equals(password, confirmPassword)) {
            JOptionPane.showMessageDialog(signupView,
                    "Passwords do not match.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. If the code gets here, we are good to go!
        System.out.println("Validation passed. Starting registration...");
        signupView.setLoadingState(true);
        createRegistrationWorker(username, email, password).execute();
    }

    SwingWorker<Boolean, Void> createRegistrationWorker(String username, String email, String password) {
        return new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return userService.register(username, email, password);
            }

            @Override
            protected void done() {
                signupView.setLoadingState(false);
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(signupView,
                                "Account created successfully! You can now log in.",
                                "Registration Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        handleBackToLogin();
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