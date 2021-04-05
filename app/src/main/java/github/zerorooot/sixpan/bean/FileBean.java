package github.zerorooot.sixpan.bean;


import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class FileBean implements Serializable, Parcelable {
    private String path;
    private String name;
    private boolean directory;
    private long size;
    private boolean deleted;
    private String identity;
    private long atime;
    private String dateTime;
    private String mime;

    private String parentPath;
    private String sizeString;
    private boolean select;
    private String message;

    protected FileBean(Parcel in) {
        path = in.readString();
        name = in.readString();
        directory = in.readByte() != 0;
        size = in.readLong();
        deleted = in.readByte() != 0;
        identity = in.readString();
        atime = in.readLong();
        dateTime = in.readString();
        mime = in.readString();
        parentPath = in.readString();
        sizeString = in.readString();
        select = in.readByte() != 0;
        message = in.readString();
    }

    public static final Creator<FileBean> CREATOR = new Creator<FileBean>() {
        @Override
        public FileBean createFromParcel(Parcel in) {
            return new FileBean(in);
        }

        @Override
        public FileBean[] newArray(int size) {
            return new FileBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(name);
        dest.writeByte((byte) (directory ? 1 : 0));
        dest.writeLong(size);
        dest.writeByte((byte) (deleted ? 1 : 0));
        dest.writeString(identity);
        dest.writeLong(atime);
        dest.writeString(dateTime);
        dest.writeString(mime);
        dest.writeString(parentPath);
        dest.writeString(sizeString);
        dest.writeByte((byte) (select ? 1 : 0));
        dest.writeString(message);
    }
}
