package com.bytecodr.invoicing.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Description implements Parcelable {
    public String id;
    public String title;
    public String description;

    protected Description(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
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
}
