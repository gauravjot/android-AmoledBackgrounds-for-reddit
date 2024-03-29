package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.droidheat.amoledbackgrounds.adapters.HomeViewPagerAdapter;
import com.droidheat.amoledbackgrounds.utils.AppUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
	
	private ViewPager2 viewPager;
	MenuItem prevMenuItem;
	
	private final NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener
					= new NavigationBarView.OnItemSelectedListener() {
		
		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			int itemId = item.getItemId();
			if (itemId == R.id.navigation_home) {
				viewPager.setCurrentItem(0);
				return true;
			} else if (itemId == R.id.navigation_settings) {
				viewPager.setCurrentItem(2);
				return true;
			} else if (itemId == R.id.navigation_notifications) {
				viewPager.setCurrentItem(1);
				return true;
			}
			return false;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		Objects.requireNonNull(getSupportActionBar()).setTitle("Amoled Backgrounds");
		
		try {
			SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			boolean showChangelog = sharedPref.getBoolean("changelog" + pInfo.versionName, true);
			if (showChangelog) {
				(new AppUtils()).changelog(this).show();
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putBoolean("changelog" + pInfo.versionName, false);
				editor.apply();
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		
		viewPager = findViewById(R.id.viewpager);
		setupViewPager(viewPager);
		viewPager.setOffscreenPageLimit(3);
		final BottomNavigationView navigation = findViewById(R.id.navigation);
		navigation.setOnItemSelectedListener(mOnNavigationItemSelectedListener);
		
		viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				if (prevMenuItem != null)
					prevMenuItem.setChecked(false);
				else
					navigation.getMenu().getItem(0).setChecked(false);
				
				if (position == 2) {
					Objects.requireNonNull(getSupportActionBar()).setTitle("Settings");
				} else if (position == 1) {
					Objects.requireNonNull(getSupportActionBar()).setTitle("Downloads");
				} else {
					Objects.requireNonNull(getSupportActionBar()).setTitle("Amoled Backgrounds");
				}
				
				navigation.getMenu().getItem(position).setChecked(true);
				prevMenuItem = navigation.getMenu().getItem(position);
			}
		});
		
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
	
	private void setupViewPager(ViewPager2 viewPager) {
		HomeViewPagerAdapter adapter =
						new HomeViewPagerAdapter(getSupportFragmentManager(),
										getLifecycle());
		Fragment wallpaperFragment = new WallpaperFragment();
		Fragment settingsFragment = new SettingsFragment();
		Fragment downloadsFragment = new DownloadsFragment();
		adapter.addFragment(wallpaperFragment);
		adapter.addFragment(downloadsFragment);
		adapter.addFragment(settingsFragment);
		viewPager.setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		
		assert searchView != null;
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@SuppressLint("RestrictedApi")
			@Override
			public boolean onQueryTextSubmit(String s) {
				Intent intent = new Intent(MainActivity.this, SearchActivity.class);
				intent.putExtra(SearchManager.QUERY, s);
				startActivity(intent);
				Objects.requireNonNull(getSupportActionBar()).collapseActionView();
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String s) {
				return false;
			}
		});
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_rate) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(this.getString(R.string.play_store_url)));
			startActivity(browserIntent);
		} else if (item.getItemId() == R.id.action_github) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gauravjot/android-AmoledBackgrounds-for-reddit"));
			startActivity(browserIntent);
		} else if (item.getItemId() == R.id.action_share) {
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT,
							"Hey! Check out this android app at: " + this.getString(R.string.play_store_url));
			sendIntent.setType("text/plain");
			startActivity(sendIntent);
		}
		return true;
	}
}
