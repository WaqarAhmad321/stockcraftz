package com.bigsteppers.stockcraftz.model;

public class User {
    private final int id;
    private final String username;
    private final UserRole role;
    private double balance;
    private int rank;

    public User(int id, String username, double balance, UserRole role) {
        this.id = id;
        this.username = username;
        this.balance = balance;
        this.role = role;
    }

    public User withRank(int rank) {
        this.rank = rank;
        return this;
    }

    // getters & setters

    public int id() {
        return id;
    }
}
