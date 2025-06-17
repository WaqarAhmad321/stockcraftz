package com.bigsteppers.stockcraftz.model;

public class LeaderboardEntry {
    private String username;
    private int xp;

    public LeaderboardEntry(String username, int xp) {
        this.username = username;
        this.xp = xp;
    }

    public String getUsername() {
        return username;
    }

    public int getXp() {
        return xp;
    }
}
