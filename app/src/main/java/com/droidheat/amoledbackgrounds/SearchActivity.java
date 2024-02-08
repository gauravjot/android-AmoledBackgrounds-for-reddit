package com.droidheat.amoledbackgrounds;

import android.app.SearchManager;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;


import android.os.Handler;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.droidheat.amoledbackgrounds.utils.FetchUtils;
import com.droidheat.amoledbackgrounds.adapters.HomeWallpaperGridAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {
	
	HomeWallpaperGridAdapter homeWallpaperGridAdapter;
	String currentQuery = "";
	Boolean isNewSearch = true;
	HashMap<String, String> currentMetadata;
	int currentPage = 0;
	ProgressBar progressBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		
		setSupportActionBar(findViewById(R.id.toolbar));
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle("");
		
		progressBar = findViewById(R.id.progressBar);
		
		// Get the intent, verify the action and get the query
		Intent intent = getIntent();
		final String query = intent.getStringExtra(SearchManager.QUERY);
		doSearch(query);
		
		final EditText editText = findViewById(R.id.search_bar);
		editText.setText(query);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		editText.setOnKeyListener((v, keyCode, event) -> {
			if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
							(keyCode == KeyEvent.KEYCODE_ENTER)) {
				// Perform action on key press
				doSearch(editText.getText().toString());
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editText.getWindowToken(),
								0);
				editText.setFocusableInTouchMode(false);
				editText.setFocusable(false);
				editText.setFocusableInTouchMode(true);
				editText.setFocusable(true);
				return true;
			}
			return false;
		});
	}
	
	
	private void doSearch(String query) {
		homeWallpaperGridAdapter = new HomeWallpaperGridAdapter(this);
		GridView gridView = findViewById(R.id.gridView);
		gridView.setAdapter(homeWallpaperGridAdapter);
		((TextView) findViewById(R.id.text34231)).setText("Searching for '" + query + "'");
		
		fetchSearchResults(query);
		progressBar.setVisibility(View.VISIBLE);
		
		gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (currentPage > 0 && !Objects.equals(currentMetadata.get("after"), "null")) {
					try {
						if (firstVisibleItem + visibleItemCount >= totalItemCount - 8) {
							// TODO: If Executor is already fetching, cancel the previous one
							// End has been reached
							int i = currentPage * 25;
							String value = currentQuery + "&after=" + currentMetadata.get("after") +
											"&count=" + i;
							fetchSearchResults(value);
							progressBar.setVisibility(View.VISIBLE);
						}
					} catch (Exception e) {
						Toast.makeText(SearchActivity.this, "wallpaper loading failed, try scrolling up and down", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
	}
	
	private void fetchSearchResults(String query) {
		Handler handler = new Handler();
		Executors.newSingleThreadExecutor().execute(() -> {
			if (!query.split("&")[0].equals(currentQuery)) {
				currentQuery = query.split("&")[0];
				isNewSearch = true;
				currentPage = 0;
			}
			
			String url = "https://www.reddit.com/r/Amoledbackgrounds/search.json?q=" + query + "&restrict_sr=1";
			ArrayList<HashMap<String, String>> arrayList =
							(new FetchUtils()).grabPostsAsArrayList(getBaseContext(), url.trim());
			
			handler.post(() -> {
				if (arrayList != null && !arrayList.isEmpty()) {
					currentMetadata = arrayList.get(arrayList.size() - 1);
					currentPage = currentPage + 1;
					if (arrayList.size() > 1) {
						((TextView) findViewById(R.id.text34231)).setText("Showing search results for '" + query.split("&")[0] + "'");
					} else {
						((TextView) findViewById(R.id.text34231)).setText("No items found for '" + query.split("&")[0] + "'");
					}
					arrayList.remove(arrayList.size() - 1);
					if (isNewSearch) {
						homeWallpaperGridAdapter.removeItems();
						isNewSearch = false;
					}
					homeWallpaperGridAdapter.addItems(arrayList);
					progressBar.setVisibility(View.GONE);
				}
			});
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		}
		return true;
	}
	
}
