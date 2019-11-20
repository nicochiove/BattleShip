package com.codeoftheweb.salvo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Grid {
    static private List<String> letters= Arrays.asList("A","B","C","D","E","F","G","H","I","J");
    static private List<String> numbers= Arrays.asList("1","2","3","4","5","6","7","8","9","10");
    private List<String> cells= new ArrayList<>();

    public Grid() {
        for(int i=0; i<letters.size();i++){
            for(int j=0; j<numbers.size(); j++){
                cells.add(letters.get(i) + numbers.get(j));
            }
        }
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
}
