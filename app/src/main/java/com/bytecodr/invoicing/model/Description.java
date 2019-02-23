package com.bytecodr.invoicing.model;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Description extends RealmObject implements Parcelable {

    @PrimaryKey
    public long id;
    public String title;
    public String description;

    public boolean pendingUpdate = false;
    public boolean pendingDelete = false;

    protected Description(Parcel in) {
        id = in.readLong();
        title = in.readString();
        description = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(description);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Description> CREATOR = new Creator<Description>() {
        @Override
        public Description createFromParcel(Parcel in) {
            return new Description(in);
        }

        @Override
        public Description[] newArray(int size) {
            return new Description[size];
        }
    };

    public Description() {
    }
}
