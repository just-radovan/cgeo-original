package carnero.cgeo;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.ContextMenu;
import android.widget.LinearLayout;
import java.util.Locale;

public class cgeo extends Activity {
	private Resources res = null;
    private cgeoapplication app = null;
	private Context context = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	private TextView navType = null;
	private TextView navAccuracy = null;
	private TextView navSatellites = null;
	private TextView navLocation = null;
	private TextView filterTitle = null;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;
		res = this.getResources();
        app = (cgeoapplication)this.getApplication();
		app.setAction(null);
        settings = new cgSettings(this, getSharedPreferences(cgSettings.preferences, 0));
        base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
        warning = new cgWarning(this);

		setTitle("c:geo");
		try {
			if (settings.transparent == true) {
				Log.i(cgSettings.tag, "Setting up desktop home.");

				setContentView(R.layout.main_transparent_all);
			} else {
				Log.i(cgSettings.tag, "Setting up blocks home.");

				setTheme(R.style.cgeo);
				if (settings.skin == 1) setContentView(R.layout.main_blocks_light);
				else setContentView(R.layout.main_blocks_dark);
			}
		} catch (Exception e) {
			Log.i(cgSettings.tag, "Failed to set mainscreen theme.");
		}

        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            
            Log.i(cgSettings.tag, "Starting " + info.packageName + " " + info.versionCode + " a.k.a " + info.versionName + "...");

            info = null;
            manager = null;
        } catch(Exception e) {
            Log.i(cgSettings.tag, "No info.");
        }

		(base.new loginThread()).start();

		init();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		init();
	}

	@Override
	public void onResume() {
		super.onResume();

        init();
	}

	@Override
	public void onDestroy() {
		if (geo != null) geo = app.removeGeo(geo);

		super.onDestroy();
	}

	@Override
	public void onStop() {
		if (geo != null) geo = app.removeGeo(geo);

		super.onStop();
	}

	@Override
	public void onPause() {
		if (geo != null) geo = app.removeGeo(geo);

		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, res.getString(R.string.menu_about)).setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, 1, 0, res.getString(R.string.menu_settings)).setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int id = item.getItemId();
		if (id == 0) {
			context.startActivity(new Intent(context, cgeoabout.class));
			
			return true;
		} else if (id == 1) {
			context.startActivity(new Intent(context, cgeoinit.class));

			return true;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(res.getString(R.string.menu_filter));
		
		menu.add(0, 0, 0, res.getString(R.string.all));
		int cnt = 1;
		for (String choice : base.cacheTypesInv.values()) {
			menu.add(0, cnt, 0, choice);
			cnt ++;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int id = item.getItemId();

		if (id == 0) {
			settings.setCacheType(null);
			setFilterTitle();

			return true;
		} else if (id > 0) {
			final Object[] types = base.cacheTypesInv.keySet().toArray();
			final String choice = (String)types[(id - 1)];

			if (choice == null) settings.setCacheType(null);
			else settings.setCacheType(choice);
			setFilterTitle();

			return true;
		}

		return false;
	}

	private void setFilterTitle() {
		if (filterTitle == null) filterTitle = (TextView)findViewById(R.id.filter_button_title);
		if (settings.cacheType != null) filterTitle.setText(base.cacheTypesInv.get(settings.cacheType));
		else filterTitle.setText(res.getString(R.string.all));
	}

	private void init() {
		settings.getLogin();
		settings.reloadCacheType();

		if (settings.cacheType != null && base.cacheTypesInv.containsKey(settings.cacheType) == false) settings.setCacheType(null);

		if (geo == null) geo = app.startGeo(context, geoUpdate, base, settings, warning, 0, 0);

		navType = (TextView)findViewById(R.id.nav_type);
		navAccuracy = (TextView)findViewById(R.id.nav_accuracy);
		navLocation = (TextView)findViewById(R.id.nav_location);

		final LinearLayout findOnMap = (LinearLayout)findViewById(R.id.map);
		findOnMap.setClickable(true);
		findOnMap.setOnClickListener(new cgeoFindOnMapListener());

		final LinearLayout findByOffline = (LinearLayout)findViewById(R.id.search_offline);
		findByOffline.setClickable(true);
		findByOffline.setOnClickListener(new cgeoFindByOfflineListener());

		final LinearLayout advanced = (LinearLayout)findViewById(R.id.advanced_button);
		advanced.setClickable(true);
		advanced.setOnClickListener(new cgeoSearchListener());

		final LinearLayout any = (LinearLayout)findViewById(R.id.any_button);
		any.setClickable(true);
		any.setOnClickListener(new cgeoPointListener());

		final LinearLayout filter = (LinearLayout)findViewById(R.id.filter_button);
		registerForContextMenu(filter);
		filter.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				openContextMenu(view);
			}
		});
		filter.setClickable(true);

		setFilterTitle();
	}

	private class update extends cgUpdateLoc {
		@Override
		public void updateLoc(cgGeo geo) {
			if (geo == null) return;

			try {
				if (navType == null || navLocation == null || navAccuracy == null) {
					navType = (TextView)findViewById(R.id.nav_type);
					navAccuracy = (TextView)findViewById(R.id.nav_accuracy);
					navSatellites = (TextView)findViewById(R.id.nav_satellites);
					navLocation = (TextView)findViewById(R.id.nav_location);
				}

				if (geo.latitudeNow != null && geo.longitudeNow != null) {
					LinearLayout findNearest = (LinearLayout)findViewById(R.id.nearest);
					findNearest.setClickable(true);
					findNearest.setOnClickListener(new cgeoFindNearestListener());

					String satellites = null;
					if (geo.satellitesVisible != null && geo.satellitesFixed != null && geo.satellitesFixed > 0) {
						satellites = res.getString(R.string.loc_sat) + ": " + geo.satellitesFixed + "/" + geo.satellitesVisible;
					} else if (geo.satellitesVisible != null) {
						satellites = res.getString(R.string.loc_sat) + ": 0/" + geo.satellitesVisible;
					} else {
						satellites = "";
					}
					navSatellites.setText(satellites);

					if (geo.gps == -1) {
						navType.setText(res.getString(R.string.loc_last));
					} else if (geo.gps == 0) {
						navType.setText(res.getString(R.string.loc_net));
					} else {
						navType.setText(res.getString(R.string.loc_gps));
					}

					if (geo.accuracyNow != null) {
						if (settings.units == settings.unitsImperial) {
							navAccuracy.setText("±" + String.format(Locale.getDefault(), "%.0f", (geo.accuracyNow * 3.2808399)) + " ft");
						} else {
							navAccuracy.setText("±" + String.format(Locale.getDefault(), "%.0f", geo.accuracyNow) + " m");
						}
					} else {
						navAccuracy.setText(null);
					}

					if (geo.altitudeNow != null) {
						String humanAlt;
						if (settings.units == settings.unitsImperial) {
							humanAlt = String.format("%.0f", (geo.altitudeNow * 3.2808399)) + " ft";
						} else {
							humanAlt = String.format("%.0f", geo.altitudeNow) + " m";
						}
						navLocation.setText(base.formatCoordinate(geo.latitudeNow, "lat", true) + " | " + base.formatCoordinate(geo.longitudeNow, "lon", true) + " | " + humanAlt);
					} else {
						navLocation.setText(base.formatCoordinate(geo.latitudeNow, "lat", true) + " | " + base.formatCoordinate(geo.longitudeNow, "lon", true));
					}
				} else {
					Button findNearest = (Button)findViewById(R.id.nearest);
					findNearest.setClickable(false);
					findNearest.setOnClickListener(null);

					navType.setText(null);
					navAccuracy.setText(null);
					navLocation.setText(res.getString(R.string.loc_trying));
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private class cgeoFindNearestListener implements View.OnClickListener {
		public void onClick(View arg0) {
			if (geo == null) return;

			final Intent cachesIntent = new Intent(context, cgeocaches.class);
			cachesIntent.putExtra("type", "nearest");
			cachesIntent.putExtra("latitude", geo.latitudeNow);
			cachesIntent.putExtra("longitude", geo.longitudeNow);
			cachesIntent.putExtra("cachetype", settings.cacheType);
			context.startActivity(cachesIntent);
		}
	}

	private class cgeoFindOnMapListener implements View.OnClickListener {
		public void onClick(View arg0) {
			context.startActivity(new Intent(context, cgeomap.class));
		}
	}

	private class cgeoFindByOfflineListener implements View.OnClickListener {
		public void onClick(View arg0) {
			final Intent cachesIntent = new Intent(context, cgeocaches.class);
			cachesIntent.putExtra("type", "offline");
			context.startActivity(cachesIntent);
		}
	}

	private class cgeoSearchListener implements View.OnClickListener {
		public void onClick(View arg0) {
			context.startActivity(new Intent(context, cgeoadvsearch.class));
		}
	}

	private class cgeoPointListener implements View.OnClickListener {
		public void onClick(View arg0) {
			context.startActivity(new Intent(context, cgeopoint.class));
		}
	}
}
