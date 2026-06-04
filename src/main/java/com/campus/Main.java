package com.campus;

import com.campus.config.AppConfig;
import com.campus.menu.MainMenu;

/**
 * Application entry point. Prints the banner and starts the menu loop; all
 * service wiring is owned by {@link AppConfig}, so this class stays trivial.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║    " + AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION);
        System.out.println("║    Secure Payment & Expense Management");
        System.out.println("╚════════════════════════════════════════════════╝");

        new MainMenu().start();
    }
}
