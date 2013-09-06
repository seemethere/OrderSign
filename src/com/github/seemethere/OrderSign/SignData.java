package com.github.seemethere.OrderSign;

public class SignData {

    private String name = "";
    private String line1 = "";
    private String line2 = "";
    private String line3 = "";
    private String line4 = "";
    private String permission = null;
    private double cost = 0;

    public SignData(String name,
                    String line1,
                    String line2,
                    String line3,
                    String line4,
                    String permission,
                    Double cost) {
        this.name = name;
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.line4 = line4;
        this.cost = cost;
        // Permissions are not necessary by default but may be added
        if (permission != null) {
            this.permission = permission;
        }
    }

    public String getLine(int line) {
        switch (line) {
            case 1:
                return line1;
            case 2:
                return line2;
            case 3:
                return line3;
            case 4:
                return line4;
            default:
                return null;
        }
    }

    public String getPermission() {
        return permission;
    }

    //Really not necessary right now, but could potentially be useful later on
    public String getName() {
        return name;
    }

    public double getCost() {
        return cost;
    }
}
