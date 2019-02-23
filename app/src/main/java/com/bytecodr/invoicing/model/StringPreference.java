package com.bytecodr.invoicing.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class StringPreference extends RealmObject {

    @PrimaryKey
    public String name;

    public String value;

    public StringPreference() {
    }

    public StringPreference(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
