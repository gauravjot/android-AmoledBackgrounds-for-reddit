package com.droidheat.amoledbackgrounds;

import android.app.job.JobScheduler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

public class AutomaticWallpaperActivity extends AppCompatActivity {

    int selectedSort = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automatic_wallpaper);

        selectedSort = new SharedPrefsUtils(this).readSharedPrefsInt("auto_sort",-1);

        // Switch
        SwitchCompat enable = findViewById(R.id.ed_switch);
        enable.setChecked(new SharedPrefsUtils(this).readSharedPrefsBoolean("daily_wallpaper",false));
        enable.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    (new SharedPrefsUtils(AutomaticWallpaperActivity.this))
                            .writeSharedPrefs("auto_sort", selectedSort);
                    (new SharedPrefsUtils(AutomaticWallpaperActivity.this))
                            .writeSharedPrefs("daily_wallpaper", true);
                    DailyWallpaperUtils dailyWallpaperUtils = new DailyWallpaperUtils();
                    dailyWallpaperUtils.applyAsync(AutomaticWallpaperActivity.this);
                    AppUtils.scheduleJob(AutomaticWallpaperActivity.this);
                    Toast.makeText(AutomaticWallpaperActivity.this, "Wallpaper will be set daily from tomorrow!",
                            Toast.LENGTH_LONG).show();
                } else {
                    getSystemService(JobScheduler.class).cancelAll();
                    Toast.makeText(AutomaticWallpaperActivity.this, "Daily Wallpaper is off now!",
                            Toast.LENGTH_LONG).show();
                    (new SharedPrefsUtils(AutomaticWallpaperActivity.this))
                            .writeSharedPrefs("daily_wallpaper", false);
                }
            }
        });

        // Spinner/Choose options
        Spinner spinner = findViewById(R.id.options_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.set_auto_sort_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedSort);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSort = position;
                new SharedPrefsUtils(AutomaticWallpaperActivity.this).writeSharedPrefs("auto_sort", selectedSort);
                DailyWallpaperUtils dailyWallpaperUtils = new DailyWallpaperUtils();
                dailyWallpaperUtils.applyAsync(AutomaticWallpaperActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
