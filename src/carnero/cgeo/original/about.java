package carnero.cgeo.original;

import carnero.cgeo.original.libs.App;
import carnero.cgeo.original.libs.Settings;
import carnero.cgeo.original.libs.Base;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class about extends Activity {
	private Activity activity = null;
	private Resources res = null;
	private Settings settings = null;
	private Base base = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		res = this.getResources();
		settings = new Settings(this, this.getSharedPreferences(Settings.preferences, 0));
		base = new Base((App) this.getApplication(), settings, this.getSharedPreferences(Settings.preferences, 0));

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.about);
		base.setTitle(activity, res.getString(R.string.about));

		// google analytics
		base.sendAnal(activity, "/about");

		init();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		settings.load();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void init() {
		try {
			PackageManager manager = this.getPackageManager();
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);

			((TextView) findViewById(R.id.version)).setText(info.versionName);

			manager = null;
		} catch (Exception e) {
			Log.e(Settings.tag, "cgeoabout.init: Failed to obtain package version.");
		}
	}

	public void goHome(View view) {
		base.goHome(activity);
	}
}
