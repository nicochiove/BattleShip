package com.codeoftheweb.salvo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;
    private LocalDateTime creationDate;

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<GamePlayer> gamePlayers= new HashSet<>();

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Score> scores= new HashSet<>();

    public Game(){
        this.creationDate= LocalDateTime.now();
    }

    public Game(LocalDateTime date){
        this.creationDate= date;
    }

    @JsonIgnore
    public long getId() {
        return id;
    }

    @JsonIgnore
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    @JsonIgnore
    public Set<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(Set<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public void addGamePlayer(GamePlayer gamePlayer){
        this.gamePlayers.add(gamePlayer);
        gamePlayer.setGame(this);
    }

    public Map<String,Object> gameDTO(){
        Map<String,Object> dto= new HashMap<>();

        dto.put("id", this.id);
        dto.put("creationDate", this.creationDate);
        dto.put("gamePlayers", this.gamePlayers.stream().map(GamePlayer::gamePlayerDTO));
        if(this.getScores().size() == 2){
            dto.put("scores", this.scores.stream().map(Score::gameScoreDTO));
        }

        return dto;
    }

    public Set<Score> getScores() {
        return scores;
    }

    public void setScores(Set<Score> scores) {
        this.scores = scores;
    }

    public void addScore(Score score){
        this.scores.add(score);
        score.setGame(this);
    }
}
