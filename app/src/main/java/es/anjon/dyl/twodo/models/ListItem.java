package es.anjon.dyl.twodo.models;

import android.graphics.Color;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.Exclude;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class ListItem implements Comparable<ListItem> {

    String id;
    String title;
    String prioirty;
    Boolean checked;
    Date dueDate;

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

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getShortDueDate() {
        if (getDueDate() == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd", Locale.UK);
        return sdf.format(dueDate);
    }

    public String getShortDueMonth() {
        if (getDueDate() == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.UK);
        return sdf.format(dueDate).toUpperCase();
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

    @Exclude
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListItem listItem = (ListItem) o;

        return getId().equals(listItem.getId());
    }

    @Exclude
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static Comparator<ListItem> orderByTitle() {
        return new Comparator<ListItem>() {
            public int compare(ListItem item1, ListItem item2) {
                String title1 = item1.getTitle();
                if (title1 == null) {
                    title1 = "P4";
                }
                String title2 = item2.getTitle();
                if (title2 == null) {
                    title2 = "P4";
                }
                return title1.compareTo(title2);
            }
        };
    }

    public static Comparator<ListItem> orderByPriority() {
        return new Comparator<ListItem>() {
            public int compare(ListItem item1, ListItem item2) {
                String priority1 = item1.getPrioirty();
                if (priority1 == null) {
                    priority1 = "P4";
                }
                String priority2 = item2.getPrioirty();
                if (priority2 == null) {
                    priority2 = "P4";
                }
                return priority1.compareTo(priority2);
            }
        };
    }

    public static Comparator<ListItem> orderByDueDate() {
        final Calendar cal = Calendar.getInstance();
        return new Comparator<ListItem>() {
            public int compare(ListItem item1, ListItem item2) {
                cal.set(3000, 1, 1);
                Date dueDate1 = item1.getDueDate();
                if (dueDate1 == null) {
                    dueDate1 = cal.getTime();
                }
                Date dueDate2 = item2.getDueDate();
                if (dueDate2 == null) {
                    dueDate2 = cal.getTime();
                }
                return dueDate1.compareTo(dueDate2);
            }
        };
    }

}
