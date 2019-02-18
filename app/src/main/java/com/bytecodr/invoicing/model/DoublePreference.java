package com.bytecodr.invoicing.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DoublePreference extends RealmObject {

    @PrimaryKey
    public String name;

    public Double value;

    public DoublePreference() {
    }

    public DoublePreference(String name, Double value) {
        this.name = name;
        this.value = value;
    }
}
