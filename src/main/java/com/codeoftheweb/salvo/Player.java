package com.codeoftheweb.salvo;

import java.util.*;
import javax.persistence.*;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.Id;

@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;
    private String userName;
    private String password;

    @OneToMany(mappedBy = "player", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<GamePlayer> gamePlayers= new HashSet<>();

    @OneToMany(mappedBy = "player", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Score> scores= new HashSet<>();

    //CONSTRUCTORES
    public Player() { }

    public Player(String name){
        this.userName= name;
    }

    public Player(String name, String pass){
        this.userName= name;
        this.password= pass;
    }
    //GETTERS & SETTERS
    public long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Set<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(Set<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public Set<Score> getScores() {
        return scores;
    }

    public void setScores(Set<Score> scores) {
        this.scores = scores;
    }

    public void addScore(Score score){
        this.scores.add(score);
        score.setPlayer(this);
    }

    public Optional<Score> getScore(Game game){
        return this.scores.stream().filter(score -> score.getGame().getId() == (game.getId())).findFirst();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    //DTOs
    public Map<String,Object> playerDTO(){
        Map<String,Object> dto= new HashMap<>();

        dto.put("id", this.id);
        dto.put("usrName", this.userName);

        return dto;
    }

    public Map<String,Object> leaderboardDTO(){
        Map<String,Object> dto= new LinkedHashMap<>();

        int wins=0,losses=0,ties=0;

        for(Score score : this.scores){
            switch (score.getScore()){
                case WIN:
                    wins++;
                    break;
                case LOSS:
                    losses++;
                    break;
                case TIE:
                    ties++;
                    break;
            }
        }

        scoreCalc(wins,ties,losses);

        dto.put("player", this.userName);
        dto.put("score", scoreCalc(wins,ties,losses));
        dto.put("totalWins", wins);
        dto.put("totalTies", ties);
        dto.put("totalLosses", losses);

        return dto;
    }

    //METHODS
    private double scoreCalc(int wins, int ties, int losses){
        return wins*Score.winPoints + ties*Score.tiePoints + losses*Score.lossPoints;
    }


}
