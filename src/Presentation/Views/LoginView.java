package Presentation.Views;

import Presentation.Controllers.AuthController;

import javax.swing.*;
import java.awt.*;

/**
 * Login screen shown at application startup.
 * Displays a form with username/email and password fields and routes events to {@link Presentation.Controllers.AuthController}.
 */
public class LoginView extends JFrame {
    private JTextField userField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signupButton;
    private AuthController controller;

    public LoginView() {
        setTitle("Login - Parking System");
        setSize(930, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

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
        logo.setBounds(120, 170, 120, 110);
        brand.add(logo);

        JLabel brandTitle = new JLabel("Parking System", SwingConstants.CENTER);
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setBounds(20, 305, 320, 40);
        brand.add(brandTitle);

        JLabel brandSub = new JLabel("Welcome back", SwingConstants.CENTER);
        brandSub.setFont(new Font("SansSerif", Font.PLAIN, 16));
        brandSub.setForeground(new Color(220, 230, 245));
        brandSub.setBounds(20, 350, 320, 30);
        brand.add(brandSub);

        mainPanel.add(brand);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(40, 50, 40, 50)));
        card.setBounds(435, 70, 420, 480);

        JLabel heading = new JLabel("Sign in");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(new Color(40, 40, 50));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subHeading = new JLabel("Please enter your credentials");
        subHeading.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subHeading.setForeground(new Color(120, 125, 135));
        subHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        userField = new JTextField();
        passwordField = new JPasswordField();
        styleField(userField);
        styleField(passwordField);

        loginButton = primaryButton("Login");
        signupButton = linkButton("Don't have an account?  Sign up");

        card.add(heading);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(subHeading);
        card.add(Box.createRigidArea(new Dimension(0, 30)));
        card.add(fieldLabel("Username or Email"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(userField);
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(fieldLabel("Password"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(passwordField);
        card.add(Box.createRigidArea(new Dimension(0, 28)));
        card.add(loginButton);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(signupButton);

        mainPanel.add(card);

        loginButton.addActionListener(e -> controller.handleLogin());
        passwordField.addActionListener(e -> controller.handleLogin());
        signupButton.addActionListener(e -> controller.handleSignup());

        setContentPane(mainPanel);
        setVisible(true);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(40, 40, 50));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(320, 40));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(33, 99, 168));
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(320, 44));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

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

    public String getUsernameOrEmail() {
        return userField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public void setLoadingState(boolean isLoading) {
        if (isLoading) {
            loginButton.setEnabled(false);
            signupButton.setEnabled(false);
            loginButton.setText("Connecting...");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            loginButton.setEnabled(true);
            signupButton.setEnabled(true);
            loginButton.setText("Login");
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void authenControllerSetter(AuthController controller) {
        this.controller = controller;
    }

    public void clearFields() {
        userField.setText("");
        passwordField.setText("");
    }
}
