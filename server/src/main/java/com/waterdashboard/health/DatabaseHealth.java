package com.waterdashboard.health;

public record DatabaseHealth(
        String status,
        boolean postgresConnected,
        boolean postgisAvailable
) {

    public static DatabaseHealth up(boolean postgisAvailable) {
        return new DatabaseHealth(postgisAvailable ? "UP" : "DEGRADED", true, postgisAvailable);
    }

    public static DatabaseHealth down() {
        return new DatabaseHealth("DOWN", false, false);
    }
}

