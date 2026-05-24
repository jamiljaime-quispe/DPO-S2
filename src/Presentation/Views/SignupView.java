package Presentation.Views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Registration screen for new users. Collects username, email, password, and confirmation; delegates
 * validation and account creation to {@link Presentation.Controllers.AuthController}.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
public class SignupView extends JFrame {
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton signupButton;
    private JButton backButton;

    /**
     * Creates the signup window.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     */
    public SignupView() {
        setTitle("Sign Up - Parking System");
        setSize(930, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Handles init components.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    public void initComponents() {
        JPanel mainPanel = createMainPanel();
        mainPanel.add(createBrandPanel());
        mainPanel.add(createSignupCard());
        setContentPane(mainPanel);
    }

    /**
     * Creates main panel.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created main panel
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(new Color(245, 247, 250));
        return panel;
    }

    /**
     * Creates brand panel.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created brand panel
     */
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

    /**
     * Creates brand logo.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created brand logo
     */
    private JLabel createBrandLogo() {
        JLabel logo = new JLabel("P", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 90), 3, true));
        logo.setBounds(120, 150, 120, 110);
        return logo;
    }

    /**
     * Creates brand title.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created brand title
     */
    private JLabel createBrandTitle() {
        JLabel brandTitle = new JLabel("Create an account", SwingConstants.CENTER);
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setBounds(20, 285, 320, 40);
        return brandTitle;
    }

    /**
     * Creates brand subtitle.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created brand subtitle
     */
    private JLabel createBrandSubtitle() {
        JLabel brandSub = new JLabel("Join the Parking System", SwingConstants.CENTER);
        brandSub.setFont(new Font("SansSerif", Font.PLAIN, 15));
        brandSub.setForeground(new Color(220, 230, 245));
        brandSub.setBounds(20, 330, 320, 30);
        return brandSub;
    }

    /**
     * Creates signup card.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created signup card
     */
    private JPanel createSignupCard() {
        JPanel card = createFormCard();
        addSignupHeader(card);
        createSignupFields();
        addSignupFields(card);
        addSignupButtons(card);
        return card;
    }

    /**
     * Creates form card.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created form card
     */
    private JPanel createFormCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(32, 50, 32, 50)));
        card.setBounds(425, 30, 440, 560);
        return card;
    }

    /**
     * Adds signup header.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param card card used by this operation
     */
    private void addSignupHeader(JPanel card) {
        JLabel heading = createCardHeading("Sign up");
        JLabel subHeading = createCardSubheading("Fill in your details to register");
        card.add(heading);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(subHeading);
        card.add(Box.createRigidArea(new Dimension(0, 22)));
    }

    /**
     * Creates card heading.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param text text used by this operation
     * @return the created card heading
     */
    private JLabel createCardHeading(String text) {
        JLabel heading = new JLabel(text);
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(new Color(40, 40, 50));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return heading;
    }

    /**
     * Creates card subheading.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param text text used by this operation
     * @return the created card subheading
     */
    private JLabel createCardSubheading(String text) {
        JLabel subHeading = new JLabel(text);
        subHeading.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subHeading.setForeground(new Color(120, 125, 135));
        subHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return subHeading;
    }

    /**
     * Creates signup fields.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     */
    private void createSignupFields() {
        usernameField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        styleField(usernameField);
        styleField(emailField);
        styleField(passwordField);
        styleField(confirmPasswordField);
    }

    /**
     * Adds signup fields.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param card card used by this operation
     */
    private void addSignupFields(JPanel card) {
        addField(card, "Username", usernameField, 14);
        addField(card, "Email", emailField, 14);
        addField(card, "Password", passwordField, 14);
        addField(card, "Confirm password", confirmPasswordField, 24);
    }

    /**
     * Adds field.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param card card used by this operation
     * @param label label used by this operation
     * @param field field used by this operation
     * @param bottomGap bottom gap used by this operation
     */
    private void addField(JPanel card, String label, JTextField field, int bottomGap) {
        card.add(fieldLabel(label));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(field);
        card.add(Box.createRigidArea(new Dimension(0, bottomGap)));
    }

    /**
     * Adds signup buttons.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param card card used by this operation
     */
    private void addSignupButtons(JPanel card) {
        signupButton = primaryButton("Create account");
        backButton = linkButton("<- Back to login");
        card.add(signupButton);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(backButton);
    }

    /**
     * Handles field label.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param text text used by this operation
     * @return the result of the operation
     */
    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(40, 40, 50));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /**
     * Handles style field.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param f f used by this operation
     */
    private void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(340, 38));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }

    /**
     * Handles primary button.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param text text used by this operation
     * @return the result of the operation
     */
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

    /**
     * Handles link button.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param text text used by this operation
     * @return the result of the operation
     */
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

    /**
     * Gets the username typed by the user.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current username
     */
    public String getUsername() {
        return usernameField.getText().trim();
    }

    /**
     * Gets the email typed by the user.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current email
     */
    public String getEmail() {
        return emailField.getText().trim();
    }

    /**
     * Gets the password typed by the user.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current password
     */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    /**
     * Gets the repeated password typed by the user.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current confirm password
     */
    public String getConfirmPassword() {
        return new String(confirmPasswordField.getPassword());
    }

    /**
     * Handles clear form.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     */
    public void clearForm() {
        usernameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    /**
     * Sets the loading state.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param isLoading true while the screen is waiting for an operation to finish
     */
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

    /**
     * Adds registration listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action to run when the user submits the signup form
     */
    public void addRegistrationListener(ActionListener listener) {
        signupButton.addActionListener(listener);
        confirmPasswordField.addActionListener(listener);
    }

    /**
     * Adds back to login listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action to run when the user returns to login
     */
    public void addBackToLoginListener(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    /**
     * Shows error.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     *
     * @param title dialog title
     * @param message message to show
     */
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
