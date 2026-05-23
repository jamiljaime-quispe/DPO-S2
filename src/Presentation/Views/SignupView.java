package Presentation.Views;

import Presentation.Controllers.AuthController;

import javax.swing.*;
import java.awt.*;

/**
 * Registration screen for new users.
 * Collects username, email, password, and confirmation; delegates validation and account creation to {@link Presentation.Controllers.AuthController}.
 */
public class SignupView extends JFrame {
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton signupButton;
    private JButton backButton;
    private AuthController controller;

    /** Creates the signup window. */
    public SignupView() {
        setTitle("Sign Up - Parking System");
        setSize(930, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /** Builds the signup window components. */
    public void initComponents() {

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(new Color(245, 247, 250));

        JPanel brand = new JPanel();
        brand.setLayout(null);
        brand.setBackground(new Color(33, 99, 168));
        brand.setBounds(0, 0, 360, 620);

        JLabel logo = new JLabel("P", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 90), 3, true));
        logo.setBounds(120, 150, 120, 110);
        brand.add(logo);

        JLabel brandTitle = new JLabel("Create an account", SwingConstants.CENTER);
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setBounds(20, 285, 320, 40);
        brand.add(brandTitle);

        JLabel brandSub = new JLabel("Join the Parking System", SwingConstants.CENTER);
        brandSub.setFont(new Font("SansSerif", Font.PLAIN, 15));
        brandSub.setForeground(new Color(220, 230, 245));
        brandSub.setBounds(20, 330, 320, 30);
        brand.add(brandSub);

        mainPanel.add(brand);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(32, 50, 32, 50)));
        card.setBounds(425, 30, 440, 560);

        JLabel heading = new JLabel("Sign up");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(new Color(40, 40, 50));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subHeading = new JLabel("Fill in your details to register");
        subHeading.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subHeading.setForeground(new Color(120, 125, 135));
        subHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        styleField(usernameField);
        styleField(emailField);
        styleField(passwordField);
        styleField(confirmPasswordField);

        signupButton = primaryButton("Create account");
        backButton = linkButton("<- Back to login");

        card.add(heading);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(subHeading);
        card.add(Box.createRigidArea(new Dimension(0, 22)));
        card.add(fieldLabel("Username"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(usernameField);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(fieldLabel("Email"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(emailField);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(fieldLabel("Password"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(passwordField);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(fieldLabel("Confirm password"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(confirmPasswordField);
        card.add(Box.createRigidArea(new Dimension(0, 24)));
        card.add(signupButton);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(backButton);

        mainPanel.add(card);

        signupButton.addActionListener(e -> controller.handleRegistrationSubmission());
        confirmPasswordField.addActionListener(e -> controller.handleRegistrationSubmission());
        backButton.addActionListener(e -> controller.handleBackToLogin());

        setContentPane(mainPanel);
    }

    /** Creates a label for a form field. */
    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(40, 40, 50));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Applies the standard text-field style. */
    private void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(340, 38));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }

    /** Creates a primary action button. */
    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(33, 99, 168));
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(340, 44));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Creates a link-style button. */
    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(new Color(33, 99, 168));
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    /** Gets the username typed by the user. */
    public String getUsername() {
        return usernameField.getText().trim();
    }

    /** Gets the email typed by the user. */
    public String getEmail() {
        return emailField.getText().trim();
    }

    /** Gets the password typed by the user. */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    /** Gets the repeated password typed by the user. */
    public String getConfirmPassword() {
        return new String(confirmPasswordField.getPassword());
    }

    /** Clears the signup form. */
    public void clearForm() {
        usernameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    /** Enables or disables the signup form while work is running. */
    public void setLoadingState(boolean isLoading) {
        if (isLoading) {
            signupButton.setEnabled(false);
            backButton.setEnabled(false);
            signupButton.setText("Registering...");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            signupButton.setEnabled(true);
            backButton.setEnabled(true);
            signupButton.setText("Create account");
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /** Sets the authentication controller used by this view. */
    public void setController(AuthController controller) {
        this.controller = controller;
    }

    /**
     * Shows an error message owned by the sign-up window.
     *
     * @param title   dialog title
     * @param message message to show
     */
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
