package dc.sendoverwifi;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

public class MainActivity extends ActionBarActivity implements TabListener {

    private Context context;
    public ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    public android.support.v7.app.ActionBar actionBar;
    public ArrayList<Fragment> mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);

	context = this;

	viewPager = (ViewPager) findViewById(R.id.mainPager);
	actionBar = getSupportActionBar();

	String[] tabs = { "Transfer" };

	mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
	viewPager.setAdapter(mAdapter);
	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	mFragments = new ArrayList<Fragment>();
	mFragments.add(new TransferFragment());

	// Adding Tabs
	for (String tab_name : tabs) {
	    actionBar.addTab(actionBar.newTab().setText(tab_name).setTabListener(this));
	}

	viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

	    @Override
	    public void onPageSelected(int position) {
		actionBar.setSelectedNavigationItem(position);
	    }

	    @Override
	    public void onPageScrolled(int arg0, float arg1, int arg2) {
	    }

	    @Override
	    public void onPageScrollStateChanged(int arg0) {
	    }
	});

	if (!isMyServiceRunning("dc.sendoverwifi.TCPService")) {
	    startService(new Intent(this, ReceiveService.class));
	}
	if (!isMyServiceRunning("dc.sendoverwifi.AnswerService")) {
	    startService(new Intent(this, UdpScanAnswerService.class));
	}

	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	if (!prefs.contains("hostname")) {
	    final Dialog hostDialog = new Dialog(this);
	    LinearLayout layout = new LinearLayout(this);
	    final EditText hostEdit = new EditText(this);
	    Button hostButton = new Button(this);
	    hostEdit.setHint("Hostname to identify your device");
	    hostDialog.setTitle("Set hostname:");
	    hostButton.setText("Set");
	    hostButton.setOnClickListener(new OnClickListener() {
		@Override
		public void onClick(View v) {
		    if (hostEdit.getText().toString().trim().equals("")) {
			Toast.makeText(MainActivity.this, "Enter a hostname", Toast.LENGTH_SHORT).show();
			return;
		    }
		    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
		    editor.putString("hostname", hostEdit.getText().toString().trim());
		    editor.apply();
		    hostDialog.dismiss();
		}
	    });
	    layout.setOrientation(LinearLayout.VERTICAL);
	    layout.addView(hostEdit);
	    layout.addView(hostButton);
	    hostDialog.setContentView(layout);
	    hostDialog.show();
	}

    }

    @Override
    public void onStart() {
	super.onStart();
	EasyTracker.getInstance(context).activityStart(this);
    }

    @Override
    public void onStop() {
	super.onStop();
	EasyTracker.getInstance(context).activityStop(this);
    }

    private boolean isMyServiceRunning(String serviceName) {
	ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	    if (serviceName.equals(service.service.getClassName())) {
		return true;
	    }
	}
	return false;
    }

    private class TabsPagerAdapter extends FragmentPagerAdapter {

	public TabsPagerAdapter(FragmentManager fm) {
	    super(fm);
	}

	@Override
	public Fragment getItem(int index) {
	    return mFragments.get(index);
	}

	@Override
	public int getCount() {
	    // get item count - equal to number of tabs
	    return 1;
	}

    }

    @Override
    public void onTabReselected(Tab arg0, FragmentTransaction arg1) {

    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction arg1) {
	viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main, menu);
	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

	switch (item.getItemId()) {
	case R.id.menu_prefs:
	    startActivity(new Intent(this, PrefsActivity.class));
	    break;

	default:
	    break;
	}

	return true;
    }
}
