package es.anjon.dyl.twodo.models;

import android.graphics.Color;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.Exclude;

public class ListItem implements Comparable<ListItem> {

    String id;
    String title;
    String prioirty;
    Boolean checked;

    public ListItem() {

    }

    public ListItem(String title, String prioirty, Boolean checked) {
        this.title = title;
        this.prioirty = prioirty;
        this.checked = checked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrioirty() {
        return prioirty;
    }

    public void setPrioirty(String prioirty) {
        this.prioirty = prioirty;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    @Exclude
    public int getPrioirtyColour() {
        if (getPrioirty() == null) {
            return Color.TRANSPARENT;
        }
        switch (getPrioirty()) {
            case "P1":
                return Color.RED;
            case "P2":
                return Color.YELLOW;
            case "P3":
                return Color.BLUE;
            default:
                return Color.TRANSPARENT;
        }
    }

    @Exclude
    @Override
    public int compareTo(@NonNull ListItem o) {
        return this.getTitle().compareTo(o.getTitle());
    }

}
