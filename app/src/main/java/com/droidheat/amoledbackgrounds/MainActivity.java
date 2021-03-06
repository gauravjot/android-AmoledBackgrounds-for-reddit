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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    MenuItem prevMenuItem;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    viewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_settings:
                    viewPager.setCurrentItem(2);
                    return true;
                case R.id.navigation_notifications:
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
        getSupportActionBar().setTitle("Amoled Backgrounds");

        try {
            SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            boolean showChangelog = sharedPref.getBoolean("changelog"+pInfo.versionName,true);
            if (showChangelog) {
                (new AppUtils()).changelog(this).show();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("changelog"+pInfo.versionName,false);
                editor.apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        viewPager.setOffscreenPageLimit(3);
        final BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (prevMenuItem != null)
                    prevMenuItem.setChecked(false);
                else
                    navigation.getMenu().getItem(0).setChecked(false);

                if (i == 2) {
                    getSupportActionBar().setTitle("Settings");
                } else if (i == 1) {
                    getSupportActionBar().setTitle("Downloads");
                } else {
                    getSupportActionBar().setTitle("Amoled Backgrounds");
                }

                navigation.getMenu().getItem(i).setChecked(true);
                prevMenuItem = navigation.getMenu().getItem(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupViewPager(ViewPager viewPager)
    {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public boolean onQueryTextSubmit(String s) {
                Intent intent = new Intent(MainActivity.this,SearchActivity.class);
                intent.putExtra(SearchManager.QUERY,s);
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
        } else if (item.getItemId() == R.id.action_droidheat) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://droidheat.com"));
            startActivity(browserIntent);
        } else if (item.getItemId() == R.id.action_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey! Check out this android app at: " + this.getString(R.string.play_store_url));
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
//        else if (item.getItemId() == R.id.action_auto_wallpaper) {
//            Intent intent = new Intent(MainActivity.this,AutomaticWallpaperActivity.class);
//            startActivity(intent);
//        }
        return true;
    }
}
