package es.anjon.dyl.twodo.models;

import com.google.firebase.auth.FirebaseUser;

public class User {

    String uuid;
    String name;
    String email;

    public User() {

    }

    public User(FirebaseUser fbUser) {
        this.uuid = fbUser.getUid();
        this.name = fbUser.getDisplayName();
        this.email = fbUser.getEmail();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return getUuid().equals(user.getUuid());
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

}
