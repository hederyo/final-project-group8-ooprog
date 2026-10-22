package pharmacy.auth;

import pharmacy.common.data.DataFiles;
import pharmacy.common.data.DataStore;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.NotFoundException;
import pharmacy.common.model.AccountStatus;
import pharmacy.common.model.Admin;
import pharmacy.common.model.Pharmacist;
import pharmacy.common.model.Role;
import pharmacy.common.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless data access repository for User entities, reading users.txt on every request.
 *
 * Demonstrates:
 * - Stateless persistence: No local caching, guaranteeing single source of truth across operations.
 * - Polymorphic object creation: Instantiates Admin or Pharmacist based on stored role column.
 */
public class UserRepository {

    private final DataStore dataStore;

    public UserRepository() {
        this.dataStore = DataStore.getInstance();
    }

    /**
     * Reads all user records from users.txt.
     *
     * @return list of User entities (Admins and Pharmacists)
     * @throws DataAccessException if an I/O error or file corruption occurs
     */
    public List<User> findAll() throws DataAccessException {
        List<String[]> lines = dataStore.readAll(DataFiles.USERS);
        List<User> users = new ArrayList<>();
        int lineNum = 0;

        for (String[] cols : lines) {
            lineNum++;
            // Expected 9 columns: userId|role|username|password|fullName|email|contactNumber|licenseNumber|status
            if (cols.length != 9) {
                throw new DataAccessException(
                        "Corrupted line " + lineNum + " in " + DataFiles.USERS +
                        ": expected 9 columns, found " + cols.length);
            }

            String userId = cols[0].trim();
            String roleStr = cols[1].trim();
            String username = cols[2].trim();
            String password = cols[3];
            String fullName = cols[4].trim();
            String email = cols[5].trim();
            String contactNumber = cols[6].trim();
            String licenseNumber = cols[7].trim();
            String statusStr = cols[8].trim();

            Role role;
            AccountStatus status;
            try {
                role = Role.valueOf(roleStr);
                status = AccountStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                throw new DataAccessException(
                        "Corrupted line " + lineNum + " in " + DataFiles.USERS +
                        ": invalid role or status enum value.", e);
            }

            if (role == Role.ADMIN) {
                users.add(new Admin(userId, username, password, fullName, email, contactNumber, status));
            } else if (role == Role.PHARMACIST) {
                users.add(new Pharmacist(userId, username, password, fullName, email, contactNumber, licenseNumber, status));
            } else {
                throw new DataAccessException(
                        "Corrupted line " + lineNum + " in " + DataFiles.USERS +
                        ": unrecognized role " + roleStr);
            }
        }

        return users;
    }

    /**
     * Finds a user by their unique ID.
     */
    public User findById(String userId) throws NotFoundException, DataAccessException {
        for (User user : findAll()) {
            if (user.getId().equalsIgnoreCase(userId)) {
                return user;
            }
        }
        throw new NotFoundException("User " + userId + " not found.");
    }

    /**
     * Finds a user by username or email (case-insensitive).
     *
     * @return User if found, or null if no match exists
     */
    public User findByLogin(String usernameOrEmail) throws DataAccessException {
        if (usernameOrEmail == null) {
            return null;
        }
        String query = usernameOrEmail.trim();
        for (User user : findAll()) {
            if (user.getUsername().equalsIgnoreCase(query) || user.getEmail().equalsIgnoreCase(query)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Checks if a username is already taken, optionally excluding a specific user ID (for updates).
     */
    public boolean usernameTaken(String username, String excludeUserId) throws DataAccessException {
        if (username == null) {
            return false;
        }
        for (User user : findAll()) {
            if (excludeUserId != null && user.getId().equalsIgnoreCase(excludeUserId)) {
                continue;
            }
            if (user.getUsername().equalsIgnoreCase(username.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an email is already taken, optionally excluding a specific user ID.
     */
    public boolean emailTaken(String email, String excludeUserId) throws DataAccessException {
        if (email == null) {
            return false;
        }
        for (User user : findAll()) {
            if (excludeUserId != null && user.getId().equalsIgnoreCase(excludeUserId)) {
                continue;
            }
            if (user.getEmail().equalsIgnoreCase(email.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a PRC license number is already taken by a pharmacist, excluding a specific user ID.
     */
    public boolean licenseTaken(String licenseNumber, String excludeUserId) throws DataAccessException {
        if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
            return false;
        }
        for (User user : findAll()) {
            if (excludeUserId != null && user.getId().equalsIgnoreCase(excludeUserId)) {
                continue;
            }
            if (user instanceof Pharmacist p) {
                if (p.getLicenseNumber() != null && p.getLicenseNumber().equalsIgnoreCase(licenseNumber.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Saves a user record, updating it if it exists or appending if new.
     */
    public void save(User user) throws DataAccessException {
        List<User> users = findAll();
        boolean replaced = false;

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equalsIgnoreCase(user.getId())) {
                users.set(i, user);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            users.add(user);
        }

        dataStore.writeAll(DataFiles.USERS, users);
    }

    /**
     * Deletes a user by their user ID.
     */
    public void delete(String userId) throws DataAccessException {
        List<User> users = findAll();
        users.removeIf(u -> u.getId().equalsIgnoreCase(userId));
        dataStore.writeAll(DataFiles.USERS, users);
    }

    /**
     * Generates the next sequential user ID (e.g. U-0002).
     */
    public String nextId() throws DataAccessException {
        return dataStore.nextId(DataFiles.USERS, "U-", 4);
    }
}
