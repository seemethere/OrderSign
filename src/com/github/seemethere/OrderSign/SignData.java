package com.github.seemethere.OrderSign;
/*
 * SignData.java
 *
 * Copyright 2014 seemethere
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 *
 */

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
                return "";
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
