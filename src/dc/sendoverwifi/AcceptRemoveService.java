package dc.sendoverwifi;

import java.io.File;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;

public class AcceptRemoveService extends IntentService {

    private int progress;
    private File file;
    private NotificationCompat.Builder noti;

    public AcceptRemoveService() {
	super("AcceptRemoveService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

	final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

	final int notiID = intent.getExtras().getInt("notiID");
	String filename = intent.getExtras().getString("filename");

	if (!intent.getExtras().getBoolean("save")) { // Jeżeli użytkownik nie chce odebrać pliku
	    mNotificationManager.cancel(notiID);
	    File file = new File(Environment.getExternalStorageDirectory() + File.separator + "WiFiDownloads" + File.separator + filename);
	    file.delete();
	    return;
	}

	final int size = intent.getExtras().getInt("size");

	noti = new NotificationCompat.Builder(this);
	noti.setSmallIcon(R.drawable.tick).setContentTitle("Downloading").setContentText("working..");
	noti.setPriority(NotificationCompat.PRIORITY_DEFAULT);
	progress = 0;
	file = new File(Environment.getExternalStorageDirectory() + File.separator + "WiFiDownloads" + File.separator + filename);

	Thread t = new Thread(new Runnable() {

	    @Override
	    public void run() {
		while (size > progress) {
		    progress = Math.round(file.length());
		    noti.setProgress(size, progress, false);
		    mNotificationManager.notify(notiID, noti.build());
		}
		noti.setContentText("Done!");
		noti.setAutoCancel(true);
		mNotificationManager.notify(notiID, noti.build());
	    }
	});
	t.setPriority(Thread.MIN_PRIORITY);
	t.start();

    }
}
