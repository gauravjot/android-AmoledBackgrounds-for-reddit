package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class WallpaperFragment extends Fragment {

    private HashMap<String, String> currentMetadata = new HashMap<>();
    private MyGridWallpaperAdapter myGridWallpaperAdapter;
    private ProgressBar progressBar;
    private String currentSort = "hot", currentString;
    private int currentPage = 0;
    private boolean isSortChanged = false;
    private GridView gridView;
    private GrabItemsAsyncTask grabItemsAsyncTask = new GrabItemsAsyncTask();
    private RelativeLayout relativeLayout;

    public WallpaperFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallpaper, container, false);

        gridView = view.findViewById(R.id.gridView);
        myGridWallpaperAdapter = new MyGridWallpaperAdapter(getActivity());
        gridView.setAdapter(myGridWallpaperAdapter);

        relativeLayout = view.findViewById(R.id.relative_reload);
        relativeLayout.setVisibility(View.INVISIBLE);

        view.findViewById(R.id.button_reload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                relativeLayout.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                (new GrabItemsAsyncTask()).execute(currentString);
            }
        });

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (currentPage > 0 && !Objects.equals(currentMetadata.get("after"), "null")) {
                    try {
                        if (firstVisibleItem + visibleItemCount >= totalItemCount - 8) {
                            if (grabItemsAsyncTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                                grabItemsAsyncTask.cancel(true);
                            }
                            // End has been reached
                            progressBar.setVisibility(View.VISIBLE);
                            int i = currentPage * 25;
                            String value = currentSort + ((currentSort.contains("?") ? "&" : "?")) + "after=" + currentMetadata.get("after") +
                                    "&count=" + Integer.toString(i);
                            grabItemsAsyncTask = new GrabItemsAsyncTask();
                            grabItemsAsyncTask.execute(value);
                        }
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "wallpaper loading failed, try scrolling up and down", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        progressBar = view.findViewById(R.id.progress_circular);

        Spinner spinner = view.findViewById(R.id.options_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(Objects.requireNonNull(getActivity()),
                R.array.options_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                progressBar.setVisibility(View.VISIBLE);
                currentPage = 0;
                isSortChanged = true;
                gridView.setSelection(0);
                gridView.setVisibility(View.INVISIBLE);
                if (grabItemsAsyncTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                    grabItemsAsyncTask.cancel(true);
                }
                grabItemsAsyncTask = new GrabItemsAsyncTask();
                switch (position) {
                    case 0:
                        //hot
                        currentSort = "hot.json";
                        grabItemsAsyncTask.execute(currentSort);
                        break;
                    case 1:
                        //new
                        currentSort = "new.json";
                        grabItemsAsyncTask.execute(currentSort);
                        break;
                    case 2:
                        //top day
                        currentSort = "top.json?t=day";
                        grabItemsAsyncTask.execute(currentSort);
                        break;
                    case 3:
                        //top week
                        currentSort = "top.json?t=week";
                        grabItemsAsyncTask.execute(currentSort);
                        break;
                    case 4:
                        //top month
                        currentSort = "top.json?t=month";
                        grabItemsAsyncTask.execute(currentSort);
                        break;
                    case 5:
                        //top year
                        currentSort = "top.json?t=year";
                        grabItemsAsyncTask.execute(currentSort);
                        break;
                    case 6:
                        //top all
                        currentSort = "top.json?t=all";
                        grabItemsAsyncTask.execute(currentSort);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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

    @SuppressLint("StaticFieldLeak")
    private class GrabItemsAsyncTask extends AsyncTask<String, Integer, Double> {

        private ArrayList<HashMap<String, String>> arrayList;

        @Override
        protected Double doInBackground(String... params) {
            currentString = params[0];
            String url = "https://www.reddit.com/r/Amoledbackgrounds/" + currentString;
            arrayList = (new UtilsJSON()).grabPostsAsArrayList(url.trim());
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Double result) {
            if (arrayList != null && !arrayList.isEmpty()) {
                if (!currentMetadata.equals(arrayList.get(arrayList.size() - 1))) {
                    currentPage = currentPage + 1;
                    currentMetadata.clear();
                    currentMetadata.putAll(arrayList.get(arrayList.size() - 1));
                    arrayList.remove(arrayList.size() - 1);
                    if (isSortChanged) {
                        myGridWallpaperAdapter.removeItems();
                        isSortChanged = false;
                    }
                    myGridWallpaperAdapter.addItems(arrayList);
                    progressBar.setVisibility(View.INVISIBLE);
                    gridView.setVisibility(View.VISIBLE);
                    relativeLayout.setVisibility(View.INVISIBLE);
                }
            } else {
                relativeLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }
}
