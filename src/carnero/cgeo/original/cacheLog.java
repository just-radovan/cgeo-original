package carnero.cgeo.original;

import carnero.cgeo.original.libs.App;
import carnero.cgeo.original.models.Cache;
import carnero.cgeo.original.libs.LogForm;
import carnero.cgeo.original.libs.Settings;
import carnero.cgeo.original.libs.Base;
import carnero.cgeo.original.models.TrackableLog;
import carnero.cgeo.original.models.CacheLog;
import carnero.cgeo.original.libs.Warning;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class cacheLog extends LogForm {
	private App app = null;
	private Activity activity = null;
	private Resources res = null;
	private LayoutInflater inflater = null;
	private Base base = null;
	private Settings settings = null;
	private Warning warning = null;
	private Cache cache = null;
	private ArrayList<Integer> types = new ArrayList<Integer>();
	private ProgressDialog waitDialog = null;
	private String cacheid = null;
	private String geocode = null;
	private String text = null;
	private boolean alreadyFound = false;
	private String viewstate = null;
	private String viewstate1 = null;
	private Boolean gettingViewstate = true;
	private ArrayList<TrackableLog> trackables = null;
	private Calendar date = Calendar.getInstance();
	private int typeSelected = 1;
	private int attempts = 0;
	private boolean progressBar = false;
	private Button post = null;
	private Button save = null;
	private Button clear = null;
	private boolean tbChanged = false;
	// constants
	private final static int LOG_SIGNATURE = 0x1;
	private final static int LOG_TIME = 0x2;
	private final static int LOG_DATE = 0x4;
	private final static int LOG_DATE_TIME = 0x6;
	private final static int LOG_SIGNATURE_DATE_TIME = 0x7;
	// handlers
	private Handler showProgressHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (progressBar == true) {
				base.showProgress(activity, true);
			}
		}
	};
	private Handler loadDataHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (types.contains(typeSelected) == false) {
				typeSelected = types.get(0);
				setType(typeSelected);

				warning.showToast(res.getString(R.string.info_log_type_changed));
			}

			if ((viewstate == null || viewstate.length() == 0) && attempts < 2) {
				warning.showToast(res.getString(R.string.err_log_load_data_again));

				loadData thread;
				thread = new loadData(cacheid);
				thread.start();

				return;
			} else if ((viewstate == null || viewstate.length() == 0) && attempts >= 2) {
				warning.showToast(res.getString(R.string.err_log_load_data));
				base.showProgress(activity, false);

				return;
			}

			gettingViewstate = false; // we're done, user can post log

			if (post == null) {
				post = (Button) findViewById(R.id.post);
			}
			post.setEnabled(true);
			post.setOnClickListener(new postListener());

			// add trackables
			if (trackables != null && trackables.isEmpty() == false) {
				if (inflater == null) {
					inflater = activity.getLayoutInflater();
				}

				final LinearLayout inventoryView = (LinearLayout) findViewById(R.id.inventory);
				inventoryView.removeAllViews();

				for (TrackableLog tb : trackables) {
					LinearLayout inventoryItem = (LinearLayout) inflater.inflate(R.layout.visit_trackable, null);

					((TextView) inventoryItem.findViewById(R.id.trackcode)).setText(tb.trackCode);
					((TextView) inventoryItem.findViewById(R.id.name)).setText(tb.name);
					((TextView) inventoryItem.findViewById(R.id.action)).setText(Base.logTypesTrackable.get(0));

					inventoryItem.setId(tb.id);
					final String tbCode = tb.trackCode;
					inventoryItem.setClickable(true);
					registerForContextMenu(inventoryItem);
					inventoryItem.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							final Intent trackablesIntent = new Intent(activity, trackableDetail.class);
							trackablesIntent.putExtra("geocode", tbCode);
							activity.startActivity(trackablesIntent);
						}
					});
					inventoryItem.findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							openContextMenu(view);
						}
					});

					inventoryView.addView(inventoryItem);
				}

				if (inventoryView.getChildCount() > 0) {
					((LinearLayout) findViewById(R.id.inventory_box)).setVisibility(View.VISIBLE);
				}
				if (inventoryView.getChildCount() > 1 && inventoryView.getChildCount() <= 20) {
					final LinearLayout inventoryChangeAllView = (LinearLayout) findViewById(R.id.inventory_changeall);

					Button changeButton = (Button) inventoryChangeAllView.findViewById(R.id.changebutton);
					registerForContextMenu(changeButton);
					changeButton.setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							openContextMenu(view);
						}
					});

					((LinearLayout) findViewById(R.id.inventory_changeall)).setVisibility(View.VISIBLE);
				}
			}

			base.showProgress(activity, false);
		}
	};

	private Handler postLogHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				warning.showToast(res.getString(R.string.info_log_posted));

				if (waitDialog != null) {
					waitDialog.dismiss();
				}

				finish();
				return;
			} else if (msg.what == 2) {
				warning.showToast(res.getString(R.string.info_log_saved));

				if (waitDialog != null) {
					waitDialog.dismiss();
				}

				finish();
				return;
			} else if (msg.what >= 1000) {
				if (msg.what == 1001) {
					warning.showToast(res.getString(R.string.warn_log_text_fill));
				} else if (msg.what == 1002) {
					warning.showToast(res.getString(R.string.err_log_failed_server));
				} else {
					warning.showToast(res.getString(R.string.err_log_post_failed));
				}
			} else {
				if (Base.errorRetrieve.get(msg.what) != null) {
					warning.showToast(res.getString(R.string.err_log_post_failed_because) + " " + Base.errorRetrieve.get(msg.what) + ".");
				} else {
					warning.showToast(res.getString(R.string.err_log_post_failed));
				}
			}

			if (waitDialog != null) {
				waitDialog.dismiss();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		res = this.getResources();
		app = (App) this.getApplication();
		settings = new Settings(this, getSharedPreferences(Settings.preferences, 0));
		base = new Base(app, settings, getSharedPreferences(Settings.preferences, 0));
		warning = new Warning(this);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.visit);
		base.setTitle(activity, res.getString(R.string.log_new_log));

		// google analytics
		base.sendAnal(activity, "/visit");

		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			cacheid = extras.getString("id");
			geocode = extras.getString("geocode");
			text = extras.getString("text");
			alreadyFound = extras.getBoolean("found");
		}

		if ((cacheid == null || cacheid.length() == 0) && geocode != null && geocode.length() > 0) {
			cacheid = app.getCacheid(geocode);
		}
		if ((geocode == null || geocode.length() == 0) && cacheid != null && cacheid.length() > 0) {
			geocode = app.getGeocode(cacheid);
		}

		cache = app.getCacheByGeocode(geocode);

		if (cache.name != null && cache.name.length() > 0) {
			base.setTitle(activity, res.getString(R.string.log_new_log) + " " + cache.name);
		} else {
			base.setTitle(activity, res.getString(R.string.log_new_log) + " " + cache.geocode.toUpperCase());
		}

		app.setAction(geocode);

		if (cache == null) {
			warning.showToast(res.getString(R.string.err_detail_cache_forgot_visit));

			finish();
			return;
		}

		init();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		settings.load();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu subMenu = null;

		subMenu = menu.addSubMenu(0, 0, 0, res.getString(R.string.log_add)).setIcon(android.R.drawable.ic_menu_add);
		subMenu.add(0, LOG_DATE_TIME, 0, res.getString(R.string.log_date_time));
		subMenu.add(0, LOG_DATE, 0, res.getString(R.string.log_date));
		subMenu.add(0, LOG_TIME, 0, res.getString(R.string.log_time));
		subMenu.add(0, LOG_SIGNATURE, 0, res.getString(R.string.init_signature));
		subMenu.add(0, LOG_SIGNATURE_DATE_TIME, 0, res.getString(R.string.log_date_time) + " & " + res.getString(R.string.init_signature));

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (settings.getSignature() == null) {
			menu.findItem(LOG_SIGNATURE).setVisible(false);
			menu.findItem(LOG_SIGNATURE_DATE_TIME).setVisible(false);
		} else {
			menu.findItem(LOG_SIGNATURE).setVisible(true);
			menu.findItem(LOG_SIGNATURE_DATE_TIME).setVisible(true);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if ((id >= LOG_SIGNATURE && id <= LOG_SIGNATURE_DATE_TIME)) {
			addSignature(id);

			return true;
		}

		return false;
	}

	public void addSignature(int id) {
		EditText text = null;
		String textContent = null;
		String dateString = null;
		String timeString = null;
		StringBuilder addText = new StringBuilder();

		text = (EditText) findViewById(R.id.log);
		textContent = text.getText().toString();
		dateString = Base.dateOut.format(new Date());
		timeString = Base.timeOut.format(new Date());

		if ((id & LOG_DATE) == LOG_DATE) {
			addText.append(dateString);
			if ((id & LOG_TIME) == LOG_TIME) {
				addText.append(" | ");
			}
		}

		if ((id & LOG_TIME) == LOG_TIME) {
			addText.append(timeString);
		}

		if ((id & LOG_SIGNATURE) == LOG_SIGNATURE && settings.getSignature() != null) {
			String findCount = "";
			if (addText.length() > 0) {
				addText.append("\n");
			}

			if (settings.getSignature().contains("[NUMBER]") == true) {
				final HashMap<String, String> params = new HashMap<String, String>();
				final String page = base.request(false, "www.geocaching.com", "/my/", "GET", params, false, false, false).getData();
				int current = base.parseFindCount(page);

				if (current >= 0) {
					findCount = "" + (current + 1);
				}
			}

			String signature = settings.getSignature()
					.replaceAll("\\[DATE\\]", dateString)
					.replaceAll("\\[TIME\\]", timeString)
					.replaceAll("\\[USER\\]", settings.getUsername())
					.replaceAll("\\[NUMBER\\]", findCount);

			addText.append(signature);
		}

		final String addTextDone;
		if (textContent.length() > 0 && addText.length() > 0 ) {
			addTextDone = textContent + "\n" + addText.toString();
		} else {
			addTextDone = textContent + addText.toString();
		}

		text.setText(addTextDone, TextView.BufferType.NORMAL);
		text.setSelection(text.getText().toString().length());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
		super.onCreateContextMenu(menu, view, info);
		final int viewId = view.getId();

		if (viewId == R.id.type) {
			for (final int typeOne : types) {
				menu.add(viewId, typeOne, 0, Base.logTypes2.get(typeOne));
				Log.w(Settings.tag, "Addig " + typeOne + " " + Base.logTypes2.get(typeOne));
			}
		} else if (viewId == R.id.changebutton) {
			final int textId = ((TextView) findViewById(viewId)).getId();

			menu.setHeaderTitle(res.getString(R.string.log_tb_changeall));
			for (final int logTbAction : Base.logTypesTrackable.keySet()) {
				menu.add(textId, logTbAction, 0, Base.logTypesTrackable.get(logTbAction));
			}
		} else {
			final int realViewId = ((LinearLayout) findViewById(viewId)).getId();

			for (final TrackableLog tb : trackables) {
				if (tb.id == realViewId) {
					menu.setHeaderTitle(tb.name);
				}
			}
			for (final int logTbAction : Base.logTypesTrackable.keySet()) {
				menu.add(realViewId, logTbAction, 0, Base.logTypesTrackable.get(logTbAction));
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int group = item.getGroupId();
		final int id = item.getItemId();

		if (group == R.id.type) {
			setType(id);

			return true;
		} else if (group == R.id.changebutton) {
			try {
				final String logTbAction = Base.logTypesTrackable.get(id);
				if (logTbAction != null) {
					final LinearLayout inventView = (LinearLayout) findViewById(R.id.inventory);
					for (int count = 0; count < inventView.getChildCount(); count++) {
						final LinearLayout tbView = (LinearLayout) inventView.getChildAt(count);
						if (tbView == null) {
							return false;
						}

						final TextView tbText = (TextView) tbView.findViewById(R.id.action);
						if (tbText == null) {
							return false;
						}
						tbText.setText(logTbAction);
					}
					for (TrackableLog tb : trackables) {
						tb.action = id;
					}
					tbChanged = true;
					return true;
				}
			} catch (Exception e) {
				Log.e(Settings.tag, "cgeovisit.onContextItemSelected: " + e.toString());
			}
		} else {
			try {
				final String logTbAction = Base.logTypesTrackable.get(id);
				if (logTbAction != null) {
					final LinearLayout tbView = (LinearLayout) findViewById(group);
					if (tbView == null) {
						return false;
					}

					final TextView tbText = (TextView) tbView.findViewById(R.id.action);
					if (tbText == null) {
						return false;
					}

					for (TrackableLog tb : trackables) {
						if (tb.id == group) {
							tbChanged = true;

							tb.action = id;
							tbText.setText(logTbAction);

							Log.i(Settings.tag, "Trackable " + tb.trackCode + " (" + tb.name + ") has new action: #" + id);
						}
					}

					return true;
				}
			} catch (Exception e) {
				Log.e(Settings.tag, "cgeovisit.onContextItemSelected: " + e.toString());
			}
		}

		return false;
	}

	public void init() {
		if (geocode != null) {
			app.setAction(geocode);
		}

		types.clear();

		if (cache.type.equals("event") || cache.type.equals("mega") || cache.type.equals("cito") || cache.type.equals("lostfound")) {
			types.add(Base.LOG_WILL_ATTEND);
			types.add(Base.LOG_NOTE);
			types.add(Base.LOG_ATTENDED);
			types.add(Base.LOG_NEEDS_ARCHIVE);
		} else if (cache.type.equals("earth")) {
			types.add(Base.LOG_FOUND_IT);
			types.add(Base.LOG_DIDNT_FIND_IT);
			types.add(Base.LOG_NOTE);
			types.add(Base.LOG_NEEDS_MAINTENANCE);
			types.add(Base.LOG_NEEDS_ARCHIVE);
		} else if (cache.type.equals("webcam")) {
			types.add(Base.LOG_WEBCAM_PHOTO_TAKEN);
			types.add(Base.LOG_DIDNT_FIND_IT);
			types.add(Base.LOG_NOTE);
			types.add(Base.LOG_NEEDS_ARCHIVE);
			types.add(Base.LOG_NEEDS_MAINTENANCE);
		} else {
			types.add(Base.LOG_FOUND_IT);
			types.add(Base.LOG_DIDNT_FIND_IT);
			types.add(Base.LOG_NOTE);
			types.add(Base.LOG_NEEDS_ARCHIVE);
			types.add(Base.LOG_NEEDS_MAINTENANCE);
		}
		if (cache.owner.equalsIgnoreCase(settings.getUsername()) == true) {
			types.add(Base.LOG_OWNER_MAINTENANCE);
			types.add(Base.LOG_TEMP_DISABLE_LISTING);
			types.add(Base.LOG_ENABLE_LISTING);
			types.add(Base.LOG_ARCHIVE);
			types.remove(new Integer(Base.LOG_UPDATE_COORDINATES));
			if (cache.type.equals("event") || cache.type.equals("mega") || cache.type.equals("cito") || cache.type.equals("lostfound")) {
				types.add(Base.LOG_ANNOUNCEMENT);
			}
		}

		final CacheLog log = app.loadLogOffline(geocode);
		if (log != null) {
			typeSelected = log.type;
			date.setTime(new Date(log.date));
			text = log.log;
		} else if (settings.getSignature() != null && settings.getSignature().length() > 0) {
			addSignature(LOG_SIGNATURE);
		}

		if (types.contains(typeSelected) == false) {
			if (alreadyFound == true) {
				typeSelected = Base.LOG_NOTE;
			} else {
				typeSelected = types.get(0);
			}
			setType(typeSelected);
		}

		Button typeButton = (Button) findViewById(R.id.type);
		registerForContextMenu(typeButton);
		typeButton.setText(Base.logTypes2.get(typeSelected));
		typeButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				openContextMenu(view);
			}
		});

		Button dateButton = (Button) findViewById(R.id.date);
		dateButton.setText(Base.dateOutShort.format(date.getTime()));
		dateButton.setOnClickListener(new cgeovisitDateListener());

		EditText logView = (EditText) findViewById(R.id.log);
		if (logView.getText().length() == 0 && text != null && text.length() > 0) {
			logView.setText(text);
		}

		if (post == null) {
			post = (Button) findViewById(R.id.post);
		}
		if (viewstate == null || viewstate.length() == 0) {
			post.setEnabled(false);
			post.setOnTouchListener(null);
			post.setOnClickListener(null);

			loadData thread;
			thread = new loadData(cacheid);
			thread.start();
		} else {
			post.setEnabled(true);
			post.setOnClickListener(new postListener());
		}

		if (save == null) {
			save = (Button) findViewById(R.id.save);
		}
		save.setOnClickListener(new saveListener());

		if (clear == null) {
			clear = (Button) findViewById(R.id.clear);
		}
		clear.setOnClickListener(new clearListener());
	}

	@Override
	public void setDate(Calendar dateIn) {
		date = dateIn;

		final Button dateButton = (Button) findViewById(R.id.date);
		dateButton.setText(Base.dateOutShort.format(date.getTime()));
	}

	public void setType(int type) {
		final Button typeButton = (Button) findViewById(R.id.type);

		if (Base.logTypes2.get(type) != null) {
			typeSelected = type;
		}
		if (Base.logTypes2.get(typeSelected) == null) {
			typeSelected = 1;
		}
		typeButton.setText(Base.logTypes2.get(typeSelected));

		if (type == 2 && tbChanged == false) {
			// TODO: change action
		} else if (type != 2 && tbChanged == false) {
			// TODO: change action
		}

		if (post == null) {
			post = (Button) findViewById(R.id.post);
		}
	}

	private class cgeovisitDateListener implements View.OnClickListener {

		public void onClick(View arg0) {
			Dialog dateDialog = new date(activity, (cacheLog) activity, date);
			dateDialog.setCancelable(true);
			dateDialog.show();
		}
	}

	private class postListener implements View.OnClickListener {

		public void onClick(View arg0) {
			if (gettingViewstate == false) {
				waitDialog = ProgressDialog.show(activity, null, res.getString(R.string.log_saving), true);
				waitDialog.setCancelable(true);

				String log = ((EditText) findViewById(R.id.log)).getText().toString();
				Thread thread = new postLog(postLogHandler, log);
				thread.start();
			} else {
				warning.showToast(res.getString(R.string.err_log_load_data_still));
			}
		}
	}

	private class saveListener implements View.OnClickListener {

		public void onClick(View arg0) {
			String log = ((EditText) findViewById(R.id.log)).getText().toString();
			final boolean status = app.saveLogOffline(geocode, date.getTime(), typeSelected, log);
			if (save == null) {
				save = (Button) findViewById(R.id.save);
			}
			save.setOnClickListener(new saveListener());

			if (status == true) {
				warning.showToast(res.getString(R.string.info_log_saved));
				app.saveVisitDate(geocode);
			} else {
				warning.showToast(res.getString(R.string.err_log_post_failed));
			}
		}
	}

	private class clearListener implements View.OnClickListener {

		public void onClick(View arg0) {
			app.clearLogOffline(geocode);

			if (alreadyFound == true) {
				typeSelected = Base.LOG_NOTE;
			} else {
				typeSelected = types.get(0);
			}
			date.setTime(new Date());
			text = null;

			setType(typeSelected);

			Button dateButton = (Button) findViewById(R.id.date);
			dateButton.setText(Base.dateOutShort.format(date.getTime()));
			dateButton.setOnClickListener(new cgeovisitDateListener());

			EditText logView = (EditText) findViewById(R.id.log);
			if (text != null && text.length() > 0) {
				logView.setText(text);
			} else {
				logView.setText("");
			}

			if (clear == null) {
				clear = (Button) findViewById(R.id.clear);
			}
			clear.setOnClickListener(new clearListener());

			warning.showToast(res.getString(R.string.info_log_cleared));
		}
	}

	private class loadData extends Thread {

		private String cacheid = null;

		public loadData(String cacheidIn) {
			cacheid = cacheidIn;

			if (cacheid == null) {
				warning.showToast(res.getString(R.string.err_detail_cache_forgot_visit));

				finish();
				return;
			}
		}

		@Override
		public void run() {
			final HashMap<String, String> params = new HashMap<String, String>();

			showProgressHandler.sendEmptyMessage(0);
			gettingViewstate = true;
			attempts++;

			try {
				if (cacheid != null && cacheid.length() > 0) {
					params.put("ID", cacheid);
				} else {
					loadDataHandler.sendEmptyMessage(0);
					return;
				}

				final String page = base.request(false, "www.geocaching.com", "/seek/log.aspx", "GET", params, false, false, false).getData();

				viewstate = base.findViewstate(page, 0);
				viewstate1 = base.findViewstate(page, 1);
				trackables = base.parseTrackableLog(page);

				final ArrayList<Integer> typesPre = base.parseTypes(page);
				if (typesPre.size() > 0) {
					types.clear();
					types.addAll(typesPre);
					types.remove(new Integer(Base.LOG_UPDATE_COORDINATES));
				}
				typesPre.clear();
			} catch (Exception e) {
				Log.e(Settings.tag, "cgeovisit.loadData.run: " + e.toString());
			}

			loadDataHandler.sendEmptyMessage(0);
		}
	}

	private class postLog extends Thread {

		Handler handler = null;
		String log = null;

		public postLog(Handler handlerIn, String logIn) {
			handler = handlerIn;
			log = logIn;
		}

		@Override
		public void run() {
			int ret = -1;

			ret = postLogFn(log);

			handler.sendEmptyMessage(ret);
		}
	}

	public int postLogFn(String log) {
		int status = -1;

		try {
			status = base.postLog(app, geocode, cacheid, viewstate, viewstate1, typeSelected, date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE), log, trackables);

			if (status == 1) {
				CacheLog logNow = new CacheLog();
				logNow.author = settings.getUsername();
				logNow.date = date.getTimeInMillis();
				logNow.type = typeSelected;
				logNow.log = log;

				cache.logs.add(0, logNow);
				app.addLog(geocode, logNow);

				if (typeSelected == Base.LOG_FOUND_IT) {
					app.markFound(geocode);
					if (cache != null) {
						cache.found = true;
					}
				}

				if (cache != null) {
					app.putCacheInCache(cache);
				} else {
					app.removeCacheFromCache(geocode);
				}
			}

			if (status == 1) {
				app.clearLogOffline(geocode);
			}

			return status;
		} catch (Exception e) {
			Log.e(Settings.tag, "cgeovisit.postLogFn: " + e.toString());
		}

		return 1000;
	}

	public void goHome(View view) {
		base.goHome(activity);
	}
}