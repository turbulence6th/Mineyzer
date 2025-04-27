package com.codeyzer.mine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Player {
    private String id;
    
    private String username;
    private int score;
    
    public Player() {
        // Boş constructor
    }
    
    public Player(String username) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.score = 0;
    }
    
    public void increaseScore(int amount) {
        if (amount > 0) {
           this.score += amount;
        }
    }
    
    public void decreaseScore(int amount) {
        if (amount > 0) {
            this.score -= amount;
            if (this.score < 0) {
                this.score = 0;
            }
        }
    }
} 