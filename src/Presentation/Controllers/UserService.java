package Presentation.Controllers;

import Business.Entities.User;
import Business.Services.PasswordUtil;
import Persistence.DatabaseManager;
import Persistence.IMPL.UserDAOImpl;

public class UserService {
    private final UserDAOImpl userDAO;
    private String lastLoggedInUsername;
    private int lastLoggedInUserId;

    public UserService(DatabaseManager db) {
        this.userDAO = new UserDAOImpl(db);
    }

    /**
     * Returns 1 for admin, 2 for regular user, 0 for invalid credentials.
     */
    public Integer authenticate(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null)
            return 0;

        User user = userDAO.findByUsername(usernameOrEmail);
        if (user == null) {
            user = userDAO.findByEmail(usernameOrEmail);
        }
        if (user == null)
            return 0;
        if (!PasswordUtil.verify(password, user.getPassword()))
            return 0;

        lastLoggedInUsername = user.getUsername();
        try {
            lastLoggedInUserId = Integer.parseInt(user.getId());
        } catch (NumberFormatException | NullPointerException e) {
            lastLoggedInUserId = 0;
        }
        return "ADMIN".equals(user.getUserType()) ? 1 : 2;
    }

    public String getLastLoggedInUsername() {
        return lastLoggedInUsername;
    }

    public int getLastLoggedInUserId() {
        return lastLoggedInUserId;
    }

    public void deleteCurrentUser() {
        if (lastLoggedInUserId > 0) {
            userDAO.delete(lastLoggedInUserId);
            lastLoggedInUserId = 0;
            lastLoggedInUsername = null;
        }
    }

    public boolean register(String username, String email, String password) {
        if (userDAO.findByUsername(username) != null)
            return false;
        if (userDAO.findByEmail(email) != null)
            return false;

        Business.Entities.Client newUser = new Business.Entities.Client(
                null, username, email, PasswordUtil.hash(password), "CLIENT", new java.util.ArrayList<>());
        userDAO.save(newUser);
        return true;
    }
}
