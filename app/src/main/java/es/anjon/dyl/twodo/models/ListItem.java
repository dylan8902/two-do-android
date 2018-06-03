package es.anjon.dyl.twodo.models;

public class ListItem {

    String title;
    Boolean checked;

    public ListItem() {

    }

    public ListItem(String title, Boolean checked) {
        this.title = title;
        this.checked = checked;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

}
