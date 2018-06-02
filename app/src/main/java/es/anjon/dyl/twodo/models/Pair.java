package es.anjon.dyl.twodo.models;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import com.google.firebase.firestore.Exclude;

import static android.nfc.NdefRecord.createMime;

public class Pair {

    private static final String MIME_TYPE = "application/vnd.es.anjon.dyl.twodo";
    public static final String COLLECTION_NAME = "pairs";
    public static final String SHARED_PREFS_KEY = "pair";
    String id;
    User from;
    User to;

    public Pair() {

    }

    public Pair(User to, NdefMessage msg) {
        this.id = new String(msg.getRecords()[0].getPayload());
        this.to = to;
    }

    public Pair(User from) {
        this.from = from;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getFrom() {
        return from;
    }

    public void setFrom(User from) {
        this.from = from;
    }

    public User getTo() {
        return to;
    }

    public void setTo(User to) {
        this.to = to;
    }

    @Exclude
    public NdefMessage createNfcMessage() {
        return new NdefMessage(new NdefRecord[] {
                createMime(MIME_TYPE, getId().getBytes()),
                NdefRecord.createApplicationRecord("es.anjon.dyl.twodo")
        });
    }

    @Exclude
    public String getListsCollectionPath() {
        return COLLECTION_NAME + "/" + getId() + TwoDoList.COLLECTION_NAME;
    }

    @Exclude
    public boolean isComplete() {
        return ((getTo() != null) && (getFrom() != null));
    }

}
