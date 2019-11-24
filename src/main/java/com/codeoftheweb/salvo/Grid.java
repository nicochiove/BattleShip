package com.codeoftheweb.salvo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Grid {
    static private List<String> letters= Arrays.asList("A","B","C","D","E","F","G","H","I","J");
    static private List<String> numbers= Arrays.asList("1","2","3","4","5","6","7","8","9","10");
    private List<String> cells= new ArrayList<>();
    private List<String> c1= new ArrayList<>();
    private List<String> c2= new ArrayList<>();
    private List<String> c3= new ArrayList<>();
    private List<String> c4= new ArrayList<>();

    public Grid() {
        for(int i=0; i<letters.size();i++){
            for(int j=0; j<numbers.size(); j++){
                cells.add(letters.get(i) + numbers.get(j));
            }
        }
        c1= createQuadrant(letters.subList(0,4),numbers.subList(0,4));
        c2= createQuadrant(letters.subList(0,4),numbers.subList(5,9));
        c3= createQuadrant(letters.subList(5,9),numbers.subList(0,4));
        c4= createQuadrant(letters.subList(5,9),numbers.subList(5,9));
    }

    public List<String> getCells() {
        return cells;
    }

    static public List<String> getLetters() {
        return letters;
    }

    static public List<String> getNumbers() {
        return numbers;
    }

    public List<String> getC1() {
        return c1;
    }

    public List<String> getC2() {
        return c2;
    }

    public List<String> getC3() {
        return c3;
    }

    public List<String> getC4() {
        return c4;
    }

    private List<String> createQuadrant(List<String> letters, List<String> numbers){
        List<String> rtn= new ArrayList<>();

        for(int i=0; i<letters.size(); i++){
            for(int j=0; j<numbers.size(); j++){
                rtn.add(letters.get(i) + numbers.get(j));
            }
        }
        return rtn;
    }

    public List<String> createDiagonals(List<String> list){
        List<String> rtn = new ArrayList<>();

        for(int i=0; i<list.size(); i++){
            if(i % 2 == 0){
                rtn.add(list.get(i));
            }
        }

        return rtn;
    }
}
