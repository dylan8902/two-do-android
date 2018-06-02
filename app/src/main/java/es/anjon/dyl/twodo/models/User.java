package es.anjon.dyl.twodo.models;

import com.google.firebase.auth.FirebaseUser;

public class User {

    String uuid;
    String name;
    String email;
    String pairingId;

    public User(FirebaseUser fbUser) {
        this.uuid = fbUser.getUid();
        this.name = fbUser.getDisplayName();
        this.email = fbUser.getEmail();
        this.pairingId = "testpair1";
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPairingId() {
        return pairingId;
    }

    public String getListsCollectionPath() {
        return "pairs/" + getPairingId() + "/lists";
    }

}
