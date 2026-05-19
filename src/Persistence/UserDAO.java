package Persistence;

import Business.Entities.User;

public interface UserDAO {
	public void save(User user);

	public User findById(int id);

	public User findByUsername(String username);

	public User findByEmail(String email);

	public void delete(int id);

	public void update(User user);
}
