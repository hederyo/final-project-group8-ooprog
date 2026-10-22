package pharmacy.common.model;

import pharmacy.common.ui.Menu;
import pharmacy.common.ui.PharmacistMenu;

/**
 * Represents a registered pharmacist with a professional PRC license.
 *
 * Demonstrates:
 * - Inheritance: Extends User, adding the specialized licenseNumber field.
 * - Polymorphism: Overrides createMenu() to return a PharmacistMenu, getRole(), and toFileLine().
 */
public class Pharmacist extends User {

    private String licenseNumber;

    public Pharmacist(String userId, String username, String password, String fullName,
                      String email, String contactNumber, String licenseNumber, AccountStatus status) {
        super(userId, username, password, fullName, email, contactNumber, status);
        this.licenseNumber = licenseNumber;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    @Override
    public Role getRole() {
        return Role.PHARMACIST;
    }

    @Override
    public Menu createMenu() {
        return new PharmacistMenu();
    }

    @Override
    public String toFileLine() {
        return String.join("|",
                getId(),
                getRole().name(),
                getUsername(),
                getPassword(),
                getFullName(),
                getEmail(),
                getContactNumber(),
                licenseNumber == null ? "" : licenseNumber,
                getStatus().name()
        );
    }
}
