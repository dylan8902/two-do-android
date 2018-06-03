package es.anjon.dyl.twodo.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class TwoDoList {

    public static final String COLLECTION_NAME = "lists";
    String id;
    String title;
    List<ListItem> items;

    public TwoDoList() {

    }

    public TwoDoList(String title) {
        this.title = title;
        this.items = new ArrayList<>();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setItems(List<ListItem> items) {
        this.items = items;
    }

    @Exclude
    public void addItem(ListItem item) {
        this.items.add(item);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<ListItem> getItems() {
        if (items == null) {
            return new ArrayList<>();
        } else {
            return items;
        }
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TwoDoList twoDoList = (TwoDoList) o;

        return getId().equals(twoDoList.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
