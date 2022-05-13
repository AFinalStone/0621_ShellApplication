package com.example.shellapplication.second;


import android.os.Parcel;
import android.os.Parcelable;

public class Person implements Parcelable {
    String name;
    int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.age);
    }

    public void readFromParcel(Parcel source) {
        this.name = source.readString();
        this.age = source.readInt();
    }

    protected Person(Parcel in) {
        this.name = in.readString();
        this.age = in.readInt();
    }

    public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel source) {
            return new Person(source);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
}
