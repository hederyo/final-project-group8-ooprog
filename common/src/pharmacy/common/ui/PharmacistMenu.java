package pharmacy.common.ui;

import pharmacy.auth.ui.ProfileMenu;
import pharmacy.common.exception.PharmacyException;
import pharmacy.common.model.User;
import pharmacy.common.session.Session;
import pharmacy.inventory.ui.MedicineBrowserMenu;
import pharmacy.orders.ui.OrderMenu;

/**
 * Root pharmacist navigation menu.
 *
 * Demonstrates:
 * - Inheritance: Extends BaseMenu to inherit menu loops and error-catching behavior.
 * - Integration: Wires cross-member submenus into a unified pharmacist workspace.
 */
public class PharmacistMenu extends BaseMenu {

    @Override
    protected String getTitle() {
        User user = Session.getInstance().getCurrentUser();
        return "Pharmacist Menu — " + (user != null ? user.getFullName() : "Pharmacist");
    }

    @Override
    protected String[] getOptions() {
        return new String[] {
            "Medicines",
            "Dispensing orders",
            "My profile"
        };
    }

    @Override
    protected String getExitLabel() {
        return "Logout";
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> new MedicineBrowserMenu().show();
            case 2 -> new OrderMenu().show();
            case 3 -> new ProfileMenu().show();
        }
    }
}
