package es.anjon.dyl.twodo.models;

import java.util.Map;

public class List {

    String title;
    Map<String, Boolean> items;

    public List(String title, Map<String,Boolean> items) {
        this.title = title;
        this.items = items;
    }

}
