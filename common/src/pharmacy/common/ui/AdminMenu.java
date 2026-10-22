package pharmacy.common.ui;

import pharmacy.auth.ui.ProfileMenu;
import pharmacy.auth.ui.UserManagementMenu;
import pharmacy.common.exception.PharmacyException;
import pharmacy.common.model.User;
import pharmacy.common.session.Session;
import pharmacy.inventory.ui.CatalogMenu;
import pharmacy.inventory.ui.StockAuditMenu;
import pharmacy.orders.ui.AnalyticsMenu;
import pharmacy.orders.ui.OrderAdminMenu;

/**
 * Root administrative navigation menu.
 *
 * Demonstrates:
 * - Inheritance: Extends BaseMenu to reuse option rendering, looping, and error trapping.
 * - Integration: Wires cross-member submenus into a unified admin dashboard.
 */
public class AdminMenu extends BaseMenu {

    @Override
    protected String getTitle() {
        User user = Session.getInstance().getCurrentUser();
        return "Admin Menu — " + (user != null ? user.getFullName() : "Administrator");
    }

    @Override
    protected String[] getOptions() {
        return new String[] {
            "Medicine catalog",
            "Stock & inventory",
            "Manage users",
            "Orders & prescriptions",
            "Sales & analytics",
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
            case 1 -> new CatalogMenu().show();
            case 2 -> new StockAuditMenu().show();
            case 3 -> new UserManagementMenu().show();
            case 4 -> new OrderAdminMenu().show();
            case 5 -> new AnalyticsMenu().show();
            case 6 -> new ProfileMenu().show();
        }
    }
}
