package es.anjon.dyl.twodo.models;

public class TwoDoList {

    public static final String COLLECTION_NAME = "lists";
    String id;
    String title;

    public TwoDoList() {

    }

    public TwoDoList(String title) {
        this.title = title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
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
