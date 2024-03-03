package com.droidheat.amoledbackgrounds.fragments;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.droidheat.amoledbackgrounds.R;
import com.droidheat.amoledbackgrounds.utils.DailyWallpaperUtils;
import com.droidheat.amoledbackgrounds.utils.AppUtils;
import com.droidheat.amoledbackgrounds.utils.SharedPrefsUtils;

public class SettingsFragment extends Fragment {
	
	public SettingsFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	int selectedSort;
	int check = 0;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
													 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_settings, container, false);
		
		selectedSort = new SharedPrefsUtils(getContext()).readSharedPrefsInt("auto_sort", -1);
		
		// Switch Daily Wallpaper
		SwitchCompat enable = view.findViewById(R.id.ed_switch);
		enable.setChecked(new SharedPrefsUtils(getContext()).readSharedPrefsBoolean("daily_wallpaper", false));
		enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				(new SharedPrefsUtils(getContext()))
								.writeSharedPrefs("auto_sort", selectedSort);
				(new SharedPrefsUtils(getContext()))
								.writeSharedPrefs("daily_wallpaper", true);
				DailyWallpaperUtils dailyWallpaperUtils = new DailyWallpaperUtils();
				dailyWallpaperUtils.applyAsync(getContext());
				(new AppUtils()).scheduleJob(getContext());
				Toast.makeText(getContext(), "Wallpaper will be set daily from tomorrow!",
								Toast.LENGTH_LONG).show();
			} else {
				requireContext().getSystemService(JobScheduler.class).cancelAll();
				Toast.makeText(getContext(), "Daily Wallpaper is off now!",
								Toast.LENGTH_LONG).show();
				(new SharedPrefsUtils(getContext()))
								.writeSharedPrefs("daily_wallpaper", false);
			}
		});
		
		// Spinner/Choose options
		final Spinner spinner = view.findViewById(R.id.options_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
						R.array.set_auto_sort_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(selectedSort);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (++check > 1) {
					selectedSort = position;
					new SharedPrefsUtils(getContext()).writeSharedPrefs("auto_sort", selectedSort);
					if (new SharedPrefsUtils(getContext()).readSharedPrefsBoolean("daily_wallpaper", false)) {
						DailyWallpaperUtils dailyWallpaperUtils = new DailyWallpaperUtils();
						dailyWallpaperUtils.applyAsync(getContext());
					}
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			
			}
		});
		
		// Switch Lower Thumbnail Quality
		SwitchCompat thumb = view.findViewById(R.id.low_wallpaper_quality_switch);
		thumb.setChecked(new SharedPrefsUtils(getContext()).readSharedPrefsBoolean("lower_thumbnail_quality", false));
		thumb.setOnCheckedChangeListener((buttonView, isChecked) -> (new SharedPrefsUtils(getContext()))
						.writeSharedPrefs("lower_thumbnail_quality", isChecked));
		
		// Privacy Policy
		view.findViewById(R.id.privacy_policy).setOnClickListener(view12 -> {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://droidheat.nzran.com/amoledbackgrounds/privacy_policy.html"));
			startActivity(browserIntent);
		});
		
		// Changelog
		view.findViewById(R.id.changelog).setOnClickListener(view1 -> ((new AppUtils()).changelog(getContext())).show());
		
		// Version ID
		try {
			PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
			((TextView) view.findViewById(R.id.version)).setText(pInfo.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			((TextView) view.findViewById(R.id.version)).setText(R.string.version_unknown);
		}
		
		return view;
	}
	
	
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
	}
	
}
