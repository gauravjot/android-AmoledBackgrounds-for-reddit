package com.droidheat.amoledbackgrounds;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

        selectedSort = new SharedPrefsUtils(getContext()).readSharedPrefsInt("auto_sort",-1);

        // Switch
        SwitchCompat enable = view.findViewById(R.id.ed_switch);
        enable.setChecked(new SharedPrefsUtils(getContext()).readSharedPrefsBoolean("daily_wallpaper",false));
        enable.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    (new SharedPrefsUtils(getContext()))
                            .writeSharedPrefs("auto_sort", selectedSort);
                    (new SharedPrefsUtils(getContext()))
                            .writeSharedPrefs("daily_wallpaper", true);
                    DailyWallpaperUtils dailyWallpaperUtils = new DailyWallpaperUtils();
                    dailyWallpaperUtils.applyAsync(getContext());
                    AppUtils.scheduleJob(getContext());
                    Toast.makeText(getContext(), "Wallpaper will be set daily from tomorrow!",
                            Toast.LENGTH_LONG).show();
                } else {
                    getContext().getSystemService(JobScheduler.class).cancelAll();
                    Toast.makeText(getContext(), "Daily Wallpaper is off now!",
                            Toast.LENGTH_LONG).show();
                    (new SharedPrefsUtils(getActivity()))
                            .writeSharedPrefs("daily_wallpaper", false);
                }
            }
        });

        // Spinner/Choose options
        final Spinner spinner = view.findViewById(R.id.options_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
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

        view.findViewById(R.id.privacy_policy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://droidheat.com/amoledwallpapers/privacy_policy.html"));
                startActivity(browserIntent);
            }
        });

        view.findViewById(R.id.changelog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((new AppUtils()).changelog(getContext())).show();
            }
        });

        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) view.findViewById(R.id.version)).setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            ((TextView) view.findViewById(R.id.version)).setText("version-unknown");
        }

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
