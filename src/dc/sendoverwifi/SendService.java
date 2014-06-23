package dc.sendoverwifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import android.app.IntentService;
import android.content.Intent;

public class SendService extends IntentService {

    public SendService() {
	super("SendService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

	//Uruchomienie wysyłania plików w równoległych wątkach	
	ArrayList<TransferTask> tq = intent.getExtras().getParcelableArrayList("tasks");
	for (int i = 0; i < tq.size(); i++) {
	    Thread t = new Thread(new TCPSend(tq.get(i).getIP(), tq.get(i).getFile()));
	    t.setPriority(Thread.MIN_PRIORITY);
	    t.start();
	}
    }

    //Zamiana stringa na tablicę bajtów
    private static byte[] stringToByte(String str) {
	char[] buffer = str.toCharArray();
	byte[] b = new byte[buffer.length];
	for (int i = 0; i < b.length; i++) {
	    b[i] = (byte) buffer[i];
	}
	return b;
    }

    //Klasa to wysyłania plików
    public class TCPSend implements Runnable {

	private InetAddress IP;
	private File file;

	public TCPSend(InetAddress IP, File file) {
	    this.IP = IP;
	    this.file = file;
	}

	public void run() {
	    try {
		Socket s = new Socket(IP, 10251); // Utworzenia połączenia
		OutputStream os = null;
		FileInputStream fis = null;

		byte[] mybytearray = new byte[8192];
		try {
		    os = s.getOutputStream();

		    fis = new FileInputStream(file); // Plik do wysłania
		    int count;

		    byte[] filename = stringToByte(file // Nazwa pliku
			    .getName());

		    byte[] size = stringToByte(Math.round(file.length()) + "");

		    os.write(filename.length);
		    os.write(filename); // Wysłanie nazwy pliku
		    os.write(size.length);
		    os.write(size); // Wysłanie rozmiaru pliku
		    while ((count = fis.read(mybytearray)) >= 0) { // Wysłanie pliku
			os.write(mybytearray, 0, count);
		    }
		} finally {
		    fis.close(); //Zwolnienie pamięci
		    os.close();
		    s.close();
		}
	    } catch (Exception e) {
	    }
	}
    }
}
