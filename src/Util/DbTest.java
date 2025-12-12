package Util;

public class DbTest {
    public static void main(String[] args) {
        System.out.println("Initializing DB (will execute sql/schema.sql if present)...");
        DB.initDatabase();
        System.out.println("Running smoke test...");
        boolean ok = DB.smokeTest();
        System.out.println("DB smoke test: " + (ok ? "OK" : "FAILED"));
        if (!ok) System.exit(2);
    }
}
