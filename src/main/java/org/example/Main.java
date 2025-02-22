package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        InMemoryDB db = new InMemoryDBImpl();

        System.out.println("===== Level 1: Basic Operations =====");
        db.set("A", "field1", "value1");
        db.set("A", "field2", "value2");
        System.out.println("Get field1: " + db.get("A", "field1")); // Expected: Optional[value1]
        System.out.println("Delete field1: " + db.delete("A", "field1")); // Expected: true
        System.out.println("Get field1 after delete: " + db.get("A", "field1")); // Expected: Optional.empty()

        System.out.println("\n===== Level 2: Scanning Fields =====");
        db.set("B", "BC", "E");
        db.set("B", "BD", "F");
        System.out.println("Scan B: " + db.scan("B")); // Expected: ["BC(E)", "BD(F)"]
        System.out.println("Scan by prefix BC: " + db.scanByPrefix("B", "BC")); // Expected: ["BC(E)"]

        System.out.println("\n===== Level 3: TTL Expiration =====");
        db.setAtWithTtl("C", "X", "100", 1, 10); // Expires at 11
        db.setAtWithTtl("C", "Y", "200", 5, 10); // Expires at 15
        System.out.println("Get C.X at t=9: " + db.getAt("C", "X", 9)); // Expected: Optional[100]
        System.out.println("Get C.X at t=12: " + db.getAt("C", "X", 12)); // Expected: Optional.empty() (Expired)
        System.out.println("Scan C at t=9: " + db.scanAt("C", 9)); // Expected: ["X(100)", "Y(200)"]
        System.out.println("Scan C at t=12: " + db.scanAt("C", 12)); // Expected: ["Y(200)"]

        System.out.println("\n===== Level 4: Backup & Restore =====");
        db.setAtWithTtl("D", "Z", "500", 10, 20);
        int recordsBackedUp = db.backup(15);
        System.out.println("Backup at t=15, records saved: " + recordsBackedUp); // Expected: 1
        db.setAt("D", "W", "600", 16);
        db.restore(20, 15);
        System.out.println("Scan D after restore at t=20: " + db.scanAt("D", 20)); // Expected: ["Z(500)"] if still valid

        System.out.println("\n===== Testing Completed Successfully =====");
    }
}