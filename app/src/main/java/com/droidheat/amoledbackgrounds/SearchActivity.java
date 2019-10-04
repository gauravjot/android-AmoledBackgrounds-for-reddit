package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.method.KeyListener;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity {

    MyGridWallpaperAdapter myGridWallpaperAdapter;
    String currentQuery = "";
    Boolean isNewSearch = true;
    HashMap<String, String> currentMetadata;
    int currentPage = 0;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        progressBar = findViewById(R.id.progressBar);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        final String query = intent.getStringExtra(SearchManager.QUERY);
        doSearch(query);

        final EditText editText = findViewById(R.id.search_bar);
        editText.setText(query);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
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
            }
        });
    }


    private void doSearch(String query) {
        myGridWallpaperAdapter = new MyGridWallpaperAdapter(this);
        GridView gridView = findViewById(R.id.gridView);
        gridView.setAdapter(myGridWallpaperAdapter);
        ((TextView) findViewById(R.id.text34231)).setText("Searching for '" + query + "'");

        final GrabItemsAsyncTask[] grabItemsAsyncTask = {new GrabItemsAsyncTask()};
        grabItemsAsyncTask[0].execute(query);
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
                            if (grabItemsAsyncTask[0].getStatus().equals(AsyncTask.Status.RUNNING)) {
                                grabItemsAsyncTask[0].cancel(true);
                            }
                            // End has been reached
                            int i = currentPage * 25;
                            String value = currentQuery + "&after=" + currentMetadata.get("after") +
                                    "&count=" + i;
                            grabItemsAsyncTask[0] = new GrabItemsAsyncTask();
                            grabItemsAsyncTask[0].execute(value);
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        Toast.makeText(SearchActivity.this, "wallpaper loading failed, try scrolling up and down", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private class GrabItemsAsyncTask extends AsyncTask<String, Integer, Double> {

        private ArrayList<HashMap<String, String>> arrayList;
        private String query;

        @Override
        protected Double doInBackground(String... params) {
            query = params[0];
            if (!query.split("&")[0].equals(currentQuery)) {
                currentQuery = query.split("&")[0];
                isNewSearch = true;
                currentPage = 0;
            }

            String url = "https://www.reddit.com/r/Amoledbackgrounds/search.json?q=" + query + "&restrict_sr=1";
            arrayList = (new UtilsJSON()).grabPostsAsArrayList(url.trim());
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Double result) {
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
                    myGridWallpaperAdapter.removeItems();
                    isNewSearch = false;
                }
                myGridWallpaperAdapter.addItems(arrayList);
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

}
