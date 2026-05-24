package Presentation.Views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Login screen shown at application startup.
 * Displays a form with username/email and password fields and routes events to {@link Presentation.Controllers.AuthController}.
 */
public class LoginView extends JFrame {
    private JTextField userField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signupButton;

    /** Creates the login window. */
    public LoginView() {
        setTitle("Login - Parking System");
        setSize(930, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /** Builds the login window components. */
    public void initComponents() {
        JPanel mainPanel = createMainPanel();
        mainPanel.add(createBrandPanel());
        mainPanel.add(createLoginCard());
        setContentPane(mainPanel);
        setVisible(true);
    }

    /** Creates the base panel for the login window. */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(new Color(245, 247, 250));
        return panel;
    }

    /** Creates the blue brand section shown on the left. */
    private JPanel createBrandPanel() {
        JPanel brand = new JPanel();
        brand.setLayout(null);
        brand.setBackground(new Color(33, 99, 168));
        brand.setBounds(0, 0, 360, 620);
        brand.add(createBrandLogo());
        brand.add(createBrandTitle());
        brand.add(createBrandSubtitle());
        return brand;
    }

    /** Creates the square logo label used by the brand panel. */
    private JLabel createBrandLogo() {
        JLabel logo = new JLabel("P", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 90), 3, true));
        logo.setBounds(120, 170, 120, 110);
        return logo;
    }

    /** Creates the main brand title. */
    private JLabel createBrandTitle() {
        JLabel brandTitle = new JLabel("Parking System", SwingConstants.CENTER);
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setBounds(20, 305, 320, 40);
        return brandTitle;
    }

    /** Creates the brand subtitle. */
    private JLabel createBrandSubtitle() {
        JLabel brandSub = new JLabel("Welcome back", SwingConstants.CENTER);
        brandSub.setFont(new Font("SansSerif", Font.PLAIN, 16));
        brandSub.setForeground(new Color(220, 230, 245));
        brandSub.setBounds(20, 350, 320, 30);
        return brandSub;
    }

    /** Creates the white card containing the login form. */
    private JPanel createLoginCard() {
        JPanel card = createFormCard(435, 70, 420, 480, 40);
        addLoginHeader(card);
        createLoginFields();
        addLoginFields(card);
        addLoginButtons(card);
        return card;
    }

    /** Creates a form card with consistent visual styling. */
    private JPanel createFormCard(int x, int y, int width, int height, int verticalPadding) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(verticalPadding, 50, verticalPadding, 50)));
        card.setBounds(x, y, width, height);
        return card;
    }

    /** Adds the title and subtitle to the login card. */
    private void addLoginHeader(JPanel card) {
        JLabel heading = createCardHeading("Sign in");
        JLabel subHeading = createCardSubheading("Please enter your credentials");
        card.add(heading);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(subHeading);
        card.add(Box.createRigidArea(new Dimension(0, 30)));
    }

    /** Creates the heading shown at the top of the card. */
    private JLabel createCardHeading(String text) {
        JLabel heading = new JLabel(text);
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(new Color(40, 40, 50));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return heading;
    }

    /** Creates the smaller subtitle shown below the heading. */
    private JLabel createCardSubheading(String text) {
        JLabel subHeading = new JLabel(text);
        subHeading.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subHeading.setForeground(new Color(120, 125, 135));
        subHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return subHeading;
    }

    /** Creates and styles the login form fields. */
    private void createLoginFields() {
        userField = new JTextField();
        passwordField = new JPasswordField();
        styleField(userField);
        styleField(passwordField);
    }

    /** Adds the login form fields to the card. */
    private void addLoginFields(JPanel card) {
        card.add(fieldLabel("Username or Email"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(userField);
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(fieldLabel("Password"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(passwordField);
        card.add(Box.createRigidArea(new Dimension(0, 28)));
    }

    /** Creates and adds the login buttons to the card. */
    private void addLoginButtons(JPanel card) {
        loginButton = primaryButton("Login");
        signupButton = linkButton("Don't have an account?  Sign up");
        card.add(loginButton);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(signupButton);
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
        f.setPreferredSize(new Dimension(320, 40));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
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
        b.setPreferredSize(new Dimension(320, 44));
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

    /** Gets the username or email typed by the user. */
    public String getUsernameOrEmail() {
        return userField.getText().trim();
    }

    /** Gets the password typed by the user. */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    /** Enables or disables the login form while work is running. */
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

    /**
     * Adds a listener to the login action.
     *
     * @param listener action to run when the user submits login credentials
     */
    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
        passwordField.addActionListener(listener);
    }

    /**
     * Adds a listener to the signup navigation action.
     *
     * @param listener action to run when the user clicks the signup link
     */
    public void addSignupNavigationListener(ActionListener listener) {
        signupButton.addActionListener(listener);
    }

    /** Clears the login fields. */
    public void clearFields() {
        userField.setText("");
        passwordField.setText("");
    }

    /**
     * Shows an error message owned by the login window.
     *
     * @param title   dialog title
     * @param message message to show
     */
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a startup error before the normal windows can be opened.
     *
     * @param message message to show
     */
    public static void showStartupError(String message) {
        JOptionPane.showMessageDialog(null, message, "Startup Error", JOptionPane.ERROR_MESSAGE);
    }
}
