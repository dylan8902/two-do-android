package es.anjon.dyl.twodo.models;

public class ListItem {

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

}
