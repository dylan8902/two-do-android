package es.anjon.dyl.twodo.models;

import java.util.Map;

public class TwoDoList {

    String id;
    String title;
    Map<String, Boolean> items;

    public TwoDoList() {

    }

    public TwoDoList(String title, Map<String,Boolean> items) {
        this.title = title;
        this.items = items;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setItems(Map<String, Boolean> items) {
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Boolean> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return title;
    }

}
