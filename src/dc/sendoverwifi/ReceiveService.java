package dc.sendoverwifi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;

public class ReceiveService extends IntentService {

    //Service do odbierania plików

    public ReceiveService() {
	super("TCPService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
	ServerSocket server = null;
	Socket client = null;
	try {
	    server = new ServerSocket(10251); //Zbindowanie serwera do portu TCP
	    while (true) {
		client = server.accept(); //Akceptowanie połączeń w loopie
		Runnable connectionHandler = new ConnectionHandler(client);
		Thread t = new Thread(connectionHandler);//Obsłużenie połączenia w oddzielnym wątku (multiconnection)
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    try {
		server.close();
		startService(new Intent(this, ReceiveService.class));
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	}
    }

    //Wątek odbierania plików
    public class ConnectionHandler implements Runnable {

	private Socket client;

	public ConnectionHandler(Socket client) {
	    this.client = client;
	}

	@Override
	public void run() {
	    InputStream is = null;
	    FileOutputStream fos = null;

	    byte[] mybytearray = new byte[87380];
	    try {
		is = client.getInputStream();

		int count;
		String name = "";
		String size = "";
		int length = is.read(); //Pobranie długości nazwy pliku (max 255)
		for (count = 0; count < length; count++) {
		    name = name + (char) is.read();
		}
		length = is.read(); //Pobranie nazwy pliku
		for (count = 0; count < length; count++) {
		    size = size + (char) is.read();
		}

		int notiID = new Random().nextInt(); //Losowanie ID notyfikacji

		//Stworzenie notyfikacji
		Intent declineIntent = new Intent(ReceiveService.this, AcceptRemoveService.class);
		declineIntent.putExtra("filename", name);
		declineIntent.putExtra("notiID", notiID);
		declineIntent.putExtra("save", false);
		PendingIntent declinePi = PendingIntent.getService(ReceiveService.this, notiID, declineIntent, PendingIntent.FLAG_ONE_SHOT);

		Intent acceptIntent = new Intent(ReceiveService.this, AcceptRemoveService.class);
		acceptIntent.putExtra("filename", name);
		acceptIntent.putExtra("size", Integer.valueOf(size));
		acceptIntent.putExtra("notiID", notiID);
		acceptIntent.putExtra("save", true);
		PendingIntent acceptPi = PendingIntent.getService(ReceiveService.this, notiID + 1, acceptIntent,
			PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder noti = new NotificationCompat.Builder(ReceiveService.this);
		noti.setSmallIcon(R.drawable.tick).setContentTitle(client.getInetAddress().getHostName() + " sends a file")
			.setContentText("Name: " + name);

		noti.addAction(R.drawable.tick, "Accept", acceptPi).addAction(R.drawable.no, "Decline", declinePi);
		noti.setPriority(NotificationCompat.PRIORITY_MAX);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(notiID, noti.build());

		//Stworzenie pliku
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + "WiFiDownloads");
		file.mkdirs();
		file = new File(Environment.getExternalStorageDirectory() + File.separator + "WiFiDownloads" + File.separator + name);

		file.createNewFile();
		fos = new FileOutputStream(file);
		while ((count = is.read(mybytearray)) >= 0) { //Odbieranie pliku
		    if (!file.exists()) { //Jeżeli plik został odrzucony (usunięty) przerwij odbieranie
			break;
		    }
		    fos.write(mybytearray, 0, count);
		}
		fos.close();
		is.close();
		client.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}