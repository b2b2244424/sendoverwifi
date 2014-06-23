package dc.sendoverwifi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ar.com.daidalos.afiledialog.FileChooserDialog;
import ar.com.daidalos.afiledialog.FileChooserDialog.OnFileSelectedListener;

public class TransferFragment extends Fragment {

    private Context context;
    private Button btHost;
    private Button btFile;
    private Button btAddTask;
    private Button btSend;
    private TextView textHost;
    private TextView textSize;
    private TextView textFile;
    private ListView transferListView;
    private List<InetAddress> hosts;
    private List<String> hostname;
    private List<String> transferQueueString;
    private ArrayList<TransferTask> transferQueue;
    private File file;
    private InetAddress IP;
    private ListView listView;
    private Dialog hostDialog;
    private DatagramSocket udpSocket;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

	context = getActivity();
	View v = inflater.inflate(R.layout.fragment_transfer, container, false);

	btHost = (Button) v.findViewById(R.id.transfer_button_host);
	btFile = (Button) v.findViewById(R.id.transfer_button_file);
	btAddTask = (Button) v.findViewById(R.id.transfer_button_addtask);
	btSend = (Button) v.findViewById(R.id.transfer_button_send);
	textHost = (TextView) v.findViewById(R.id.transfer_text_host);
	textFile = (TextView) v.findViewById(R.id.transfer_text_filename);
	textSize = (TextView) v.findViewById(R.id.transfer_text_filesize);
	transferListView = (ListView) v.findViewById(R.id.transfer_listview);
	listView = new ListView(context);
	hostDialog = new Dialog(context);

	SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(transferListView,
		new SwipeDismissListViewTouchListener.OnDismissCallback() {

		    @Override
		    public void onDismiss(ListView listView, int[] reverseSortedPositions) {
			for (int position : reverseSortedPositions) {
			    transferQueue.remove(position);
			    transferQueueString.remove(position);
			    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1,
				    transferQueueString);
			    transferListView.setAdapter(adapter);

			}
		    }

		});

	transferListView.setOnTouchListener(touchListener);
	transferListView.setOnScrollListener(touchListener.makeScrollListener());

	btFile.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View arg0) {
		FileChooserDialog fileDialog = new FileChooserDialog(context);
		fileDialog.addListener(new OnFileSelectedListener() {
		    @Override
		    public void onFileSelected(Dialog source, File folder, String name) {
			source.dismiss();
		    }

		    @Override
		    public void onFileSelected(Dialog source, File file) {
			source.dismiss();
			TransferFragment.this.file = file;
			textFile.setText(file.getName());
			String stringSize = "";
			float size = file.length();
			if (size >= 1000) {
			    size = size / 1000;
			    if (size >= 1000) {
				size = size / 1000;
				stringSize = (float) Math.round(size * 100) / 100 + " MB";
			    } else {
				stringSize = (float) Math.round(size * 100) / 100 + " kB";
			    }
			} else {
			    stringSize = size + " B";
			}
			textSize.setText(stringSize);
		    }
		});
		fileDialog.show();
	    }
	});

	btHost.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View arg0) {
		hosts = new ArrayList<InetAddress>();
		hostname = new ArrayList<String>();
		Thread t = new Thread(new UDPBroadcast());
		t.start();
		hostDialog.show();
	    }
	});

	btAddTask.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View arg0) {
		if (IP == null) {
		    Toast.makeText(context, "Select a host", Toast.LENGTH_SHORT).show();
		    return;
		}
		if (file == null) {
		    Toast.makeText(context, "Select a file", Toast.LENGTH_SHORT).show();
		    return;
		}

		transferQueue.add(new TransferTask(IP, file));
		transferQueueString.add(textHost.getText() + ": " + file.getName());
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, transferQueueString);
		transferListView.setAdapter(adapter);
	    }
	});

	btSend.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View arg0) {
		Intent intent = new Intent(context, SendService.class);
		intent.putParcelableArrayListExtra("tasks", transferQueue);
		context.startService(intent);
		transferQueue.clear();
		transferQueueString.clear();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, transferQueueString);
		transferListView.setAdapter(adapter);
	    }
	});

	listView.setOnItemClickListener(new OnItemClickListener() {

	    @Override
	    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		hostDialog.dismiss();
		textHost.setText(hostname.get(position));
		IP = hosts.get(position);
	    }
	});

	hostDialog.setTitle("Select a host:");
	hostDialog.setContentView(listView);
	Thread scan = new Thread(new UDPScan());
	scan.start();

	transferQueue = new ArrayList<TransferTask>();
	transferQueueString = new ArrayList<String>();

	return v;
    }

    private String getBroadcastAddress() throws IOException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException {

	WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

	Method[] wmMethods = myWifiManager.getClass().getDeclaredMethods();
	for (Method method : wmMethods) {
	    if (method.getName().equals("isWifiApEnabled")) {
		if ((Boolean) method.invoke(myWifiManager)) {
		    return "192.168.43.255";
		}
	    }
	}

	DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
	if (myDhcpInfo == null) {
	    Log.d("dc.sendoverwifi", "Could not get broadcast address");
	    return null;
	}
	int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask) | ~myDhcpInfo.netmask;
	byte[] quads = new byte[4];
	for (int k = 0; k < 4; k++)
	    quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	Log.d("dc.sendoverwifi", InetAddress.getByAddress(quads).getHostAddress());
	return InetAddress.getByAddress(quads).getHostAddress();
    }

    // Sprawdzanie dostępnych hostów (wysłanie pakietu UDP)
    public class UDPBroadcast implements Runnable {

	public void run() {
	    DatagramSocket s = null;
	    try {

		String messageStr = "Hello";
		int server_port = 10250;
		s = new DatagramSocket();
		InetAddress local = InetAddress.getByName(getBroadcastAddress());
		int msg_length = messageStr.length();
		byte[] message = messageStr.getBytes();
		DatagramPacket p = new DatagramPacket(message, msg_length, local, server_port);
		s.send(p);
	    } catch (Exception e) {
	    } finally {
		s.close();
	    }
	}
    }

    // Sprawdzanie dostępnych hostów (odbieranie pakietów UDP)
    public class UDPScan implements Runnable {

	ArrayAdapter<String> adapter;

	public void run() {
	    udpSocket = null;
	    try {
		udpSocket = new DatagramSocket(10251); // Stworzenie socketa
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String thisHostname = prefs.getString("hostname", "unknown"); // Pobranie z bazy nazwy hosta
		while (true) { // Nieskończona pętla
		    byte[] receiveData = new byte[1024];
		    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		    udpSocket.receive(receivePacket); // Block, aż to odebrania danych		    

		    String data = new String(receivePacket.getData()).trim();
		    //if (!data.equals(thisHostname)) {
		    hosts.add(receivePacket.getAddress()); // Dodanie adresu
		    hostname.add(data.trim()); // hosta do tablicy
		    adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, hostname);
		    //}
		    getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
			    listView.setAdapter(adapter);
			}
		    });

		}
	    } catch (SocketException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    } finally {
		if (udpSocket != null) {
		    udpSocket.close();
		}
	    }
	}
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	if (udpSocket != null) {
	    udpSocket.close();
	}
    }
}
