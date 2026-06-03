package com.campus;

import com.campus.menu.MainMenu;

public class Main {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║    Campus Payment Platform v1.0.0             ║");
        System.out.println("║    Secure Payment & Expense Management         ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        MainMenu mainMenu = new MainMenu();
        mainMenu.start();
    }
}
