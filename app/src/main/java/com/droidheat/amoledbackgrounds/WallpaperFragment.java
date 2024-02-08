package com.droidheat.amoledbackgrounds;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
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

import com.droidheat.amoledbackgrounds.utils.FetchUtils;
import com.droidheat.amoledbackgrounds.adapters.HomeWallpaperGridAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;

public class WallpaperFragment extends Fragment {
	
	private final HashMap<String, String> currentMetadata = new HashMap<>();
	private HomeWallpaperGridAdapter homeWallpaperGridAdapter;
	private ProgressBar progressBar;
	private String currentSort = "hot";
	private int currentPage = 0;
	private boolean isSortChanged = false;
	private GridView gridView;
	private RelativeLayout relativeLayout;
	private boolean isFetching = false;
	
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
		homeWallpaperGridAdapter = new HomeWallpaperGridAdapter(getActivity());
		gridView.setAdapter(homeWallpaperGridAdapter);
		
		relativeLayout = view.findViewById(R.id.relative_reload);
		relativeLayout.setVisibility(View.INVISIBLE);
		
		view.findViewById(R.id.button_reload).setOnClickListener(v -> {
			relativeLayout.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.VISIBLE);
			fetchWallpapers("");
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
							if (isFetching) {
								return;
							}
							// End has been reached
							progressBar.setVisibility(View.VISIBLE);
							int i = currentPage * 25;
							String uriPath = currentSort + ((currentSort.contains("?") ?
											"&" :
											"?")) + "after=" + currentMetadata.get("after") +
											"&count=" + i;
							fetchWallpapers(uriPath);
						}
					} catch (Exception e) {
						Toast.makeText(getActivity(), "wallpaper loading failed, try scrolling up and down", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		
		progressBar = view.findViewById(R.id.progress_circular);
		
		Spinner spinner = view.findViewById(R.id.options_spinner);
		ArrayAdapter<CharSequence> adapter =
						ArrayAdapter.createFromResource(Objects.requireNonNull(requireActivity()),
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
				// TODO: If it is already fetching, we need to cancel the previous fetch
				switch (position) {
					case 0:
						currentSort = "hot.json";
						break;
					case 1:
						currentSort = "new.json";
						break;
					case 2:
						currentSort = "top.json?t=day";
						break;
					case 3:
						currentSort = "top.json?t=week";
						break;
					case 4:
						currentSort = "top.json?t=month";
						break;
					case 5:
						currentSort = "top.json?t=year";
						break;
					case 6:
						currentSort = "top.json?t=all";
						break;
				}
				fetchWallpapers(currentSort);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			
			}
		});
		
		return view;
	}
	
	
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
	}
	
	private void fetchWallpapers(String uriPath) {
		Handler handler = new Handler();
		Executors.newSingleThreadExecutor().execute(() -> {
			isFetching = true;
			String url = "https://www.reddit.com/r/Amoledbackgrounds/" + uriPath;
			ArrayList<HashMap<String, String>> arrayList =
							(new FetchUtils()).grabPostsAsArrayList(getContext(), url.trim());
			
			handler.post(() -> {
				isFetching = false;
				if (arrayList != null && !arrayList.isEmpty()) {
					if (!currentMetadata.equals(arrayList.get(arrayList.size() - 1))) {
						currentPage = currentPage + 1;
						currentMetadata.clear();
						currentMetadata.putAll(arrayList.get(arrayList.size() - 1));
						arrayList.remove(arrayList.size() - 1);
						if (isSortChanged) {
							homeWallpaperGridAdapter.removeItems();
							isSortChanged = false;
						}
						homeWallpaperGridAdapter.addItems(arrayList);
						progressBar.setVisibility(View.INVISIBLE);
						gridView.setVisibility(View.VISIBLE);
						relativeLayout.setVisibility(View.INVISIBLE);
					}
				} else {
					relativeLayout.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.INVISIBLE);
				}
			});
		});
	}
}
