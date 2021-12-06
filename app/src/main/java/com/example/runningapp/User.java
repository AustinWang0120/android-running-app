package com.example.runningapp;

public class User {

    public String gender;
    public float height;
    public float weight;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String gender, float height, float weight) {
        this.gender = gender;
        this.height = height;
        this.weight = weight;
    }
}
