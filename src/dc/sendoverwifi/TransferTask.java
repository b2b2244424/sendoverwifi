package dc.sendoverwifi;

import java.io.File;
import java.net.InetAddress;

import android.os.Parcel;
import android.os.Parcelable;

//Klasa do przekazywania w intencie informacji o pliku do przesłania
//TODO ogarnąć, ulepszyć

public class TransferTask implements Parcelable {
    private InetAddress IP;
    private File file;

    public InetAddress getIP() {
	return IP;
    }

    public void setIP(InetAddress iP) {
	IP = iP;
    }

    public File getFile() {
	return file;
    }

    public void setFile(File file) {
	this.file = file;
    }

    public TransferTask() {
	;
    };

    public TransferTask(Parcel in) {
	readFromParcel(in);
    }

    public TransferTask(InetAddress IP, File file) {
	this.IP = IP;
	this.file = file;
    }

    private void readFromParcel(Parcel in) {
	IP = (InetAddress) in.readValue(InetAddress.class.getClassLoader());
	file = (File) in.readValue(File.class.getClassLoader());
    }

    @Override
    public int describeContents() {
	return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
	arg0.writeValue(IP);
	arg0.writeValue(file);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
	public TransferTask createFromParcel(Parcel in) {
	    return new TransferTask(in);
	}

	public TransferTask[] newArray(int size) {
	    return new TransferTask[size];
	}
    };

}