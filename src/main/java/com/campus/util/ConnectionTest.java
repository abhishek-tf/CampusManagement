package com.campus.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Throwaway connectivity check: opens a connection via DBConnection and prints DB info. */
public class ConnectionTest {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE(), VERSION()")) {
            rs.next();
            System.out.println("CONNECTED OK");
            System.out.println("  database : " + rs.getString(1));
            System.out.println("  version  : " + rs.getString(2));
        } catch (Exception e) {
            System.out.println("CONNECTION FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
