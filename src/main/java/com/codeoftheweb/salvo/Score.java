package com.codeoftheweb.salvo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private Player player;

    public enum scoreType {WIN, TIE, LOSS;}

    private scoreType score;

    private LocalDateTime finishDate;

    static public double winPoints= 1;
    static public double lossPoints= 0;
    static public double tiePoints= 0.5;

    public Score() {
    }

    public Score(Player player, Game game, LocalDateTime finishDate, scoreType score){
        this.player= player;
        this.game= game;
        this.finishDate= finishDate;
        this.score= score;
    }

    @JsonIgnore
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonIgnore
    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    @JsonIgnore
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @JsonIgnore
    public LocalDateTime getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(LocalDateTime finishDate) {
        this.finishDate = finishDate;
    }

    @JsonIgnore
    public scoreType getScore() {
        return score;
    }

    public void setScore(scoreType score) {
        this.score = score;
    }

    public Map<String, Object> scoreDTO(){
        Map<String,Object> dto= new LinkedHashMap<>();

        dto.put("id", this.id);
        dto.put("finishDate", this.finishDate);
        dto.put("score", this.score);

        return dto;
    }
}


