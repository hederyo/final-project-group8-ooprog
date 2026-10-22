package pharmacy.common.model;

import pharmacy.common.ui.Menu;

/**
 * Abstract base class representing a user in the pharmacy system.
 *
 * Demonstrates:
 * - Encapsulation: Private state with guarded accessors; immutable userId and role; password hidden.
 * - Abstraction: Abstract base class defining role-specific behaviors (createMenu, toFileLine).
 * - Polymorphism: Subclasses dynamically instantiate their respective menu and format their file lines.
 */
public abstract class User implements Storable {

    private final String userId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String contactNumber;
    private AccountStatus status;

    protected User(String userId, String username, String password, String fullName,
                   String email, String contactNumber, AccountStatus status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.status = status;
    }

    @Override
    public String getId() {
        return userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public boolean checkPassword(String attempt) {
        return attempt != null && this.password.equals(attempt);
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public abstract Role getRole();

    public abstract Menu createMenu();

    @Override
    public abstract String toFileLine();

    @Override
    public String toString() {
        return String.format("%s [%s] - %s (%s, %s)",
                userId, getRole(), fullName, username, status);
    }
}
