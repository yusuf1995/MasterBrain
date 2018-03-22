package com.example.haydarcayir.masterbrain;

public class PlayerScore {

    String name;
    String score;

    public PlayerScore(){

    }
    public PlayerScore(String name,String score){
        this.name=name;
        this.score=score;

    }

    public void setScore(String score){
        this.score=score;

    }
    public void setName(String name){
        this.name=name;
    }
    public String getName(){
        return name;
    }
    public String getScore(){
        return score;
    }



}
