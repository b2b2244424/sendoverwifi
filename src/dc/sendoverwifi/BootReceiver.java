package dc.sendoverwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//Włączenie service w momencie boota systemu
public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		context.startService(new Intent(context, ReceiveService.class));
		context.startService(new Intent(context, UdpScanAnswerService.class));
	}
}