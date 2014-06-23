package dc.sendoverwifi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

//Service do odpowiadania na wyszukiwanie hostów (UDP 10250)
public class UdpScanAnswerService extends IntentService {

    public UdpScanAnswerService() {
	super("AnswerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
	Thread scan = new Thread(new UDPAnswer());
	scan.setPriority(Thread.MIN_PRIORITY);
	scan.start();
    }

    public class UDPAnswer implements Runnable {

	public void run() {
	    DatagramSocket udpSocket = null;
	    DatagramSocket udpSender = null;
	    try { // Stworzenie socketa
		udpSocket = new DatagramSocket(10250);
		udpSender = new DatagramSocket();
		while (true) { // Nieskończona pętla

		    byte[] receiveData = new byte[1024];
		    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		    udpSocket.receive(receivePacket); // Block, aż to odebrania danych

		    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(UdpScanAnswerService.this);
		    String messageStr = prefs.getString("hostname", "unknown"); // Pobranie z bazy nazwy hosta

		    int msg_length = messageStr.length();
		    byte[] message = messageStr.getBytes();

		    InetAddress sourceIP = receivePacket.getAddress();
		    DatagramPacket p = new DatagramPacket(message, msg_length, sourceIP, 10251);
		    if (prefs.getBoolean("isVisible", true)) {
			udpSender.send(p);
			udpSender.send(p);
			udpSender.send(p);
		    }
		}
	    } catch (SocketException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    } finally {
		if (udpSocket != null) {
		    udpSocket.close();
		}
		if (udpSender != null) {
		    udpSender.close();
		}

	    }
	}
    }
}
