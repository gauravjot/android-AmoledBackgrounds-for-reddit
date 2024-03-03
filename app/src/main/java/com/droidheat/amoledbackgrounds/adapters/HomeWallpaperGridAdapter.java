package com.droidheat.amoledbackgrounds.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.droidheat.amoledbackgrounds.activities.DownloadActivity;
import com.droidheat.amoledbackgrounds.R;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class HomeWallpaperGridAdapter extends BaseAdapter {
	
	private final ArrayList<HashMap<String, String>> arrayList;
	private final Context context;
	
	public HomeWallpaperGridAdapter(Context c) {
		arrayList = new ArrayList<>();
		context = c;
	}
	
	public void addItems(ArrayList<HashMap<String, String>> a) {
		arrayList.addAll(a);
		this.notifyDataSetChanged();
	}
	
	public void removeItems() {
		arrayList.clear();
		this.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return arrayList.size();
	}
	
	@Override
	public Object getItem(int position) {
		return arrayList.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
		final HashMap<String, String> wallpaper = arrayList.get(position);
		
		if (convertView == null) {
			final LayoutInflater layoutInflater = LayoutInflater.from(context);
			convertView = layoutInflater.inflate(R.layout.my_grid_wallpaper_item, null);
		}
		
		final ImageView preview = convertView.findViewById(R.id.preview);
		final TextView textView = convertView.findViewById(R.id.textView);
		final TextView textAuthor = convertView.findViewById(R.id.textAuthor);
		final TextView textResolution = convertView.findViewById(R.id.resolution);
		final TextView textScore = convertView.findViewById(R.id.score);
		final TextView textFlair = convertView.findViewById(R.id.post_flair);
		final TextView textComment = convertView.findViewById(R.id.comments);
		
		/*
		 * Getting when wallpaper was uploaded
		 */
		Date date = new java.util.Date(Long.parseLong(Objects.requireNonNull(wallpaper.get("created_utc"))) * 1000L);
		@SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-4"));
		String formattedDate = sdf.format(date);
		CharSequence ago = null;
		try {
			ago = DateUtils.getRelativeTimeSpanString(Objects.requireNonNull(sdf.parse(formattedDate)).getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		/*
		 * Loading image
		 */
		Picasso.get().load(wallpaper.get("preview")).resize(560, 0).into(preview);
		
		/*
		 * TextViews
		 */
		textView.setText(Objects.requireNonNull(wallpaper.get("title")).replaceAll("\\(.*?\\) ?", "").replaceAll("\\[.*?\\] ?", "")
						.replaceAll("\\{[^}]*\\}", "").replaceAll("-", "").replaceAll("&amp;", "&").trim());
		textAuthor.setText(String.format("by %s \n%s", wallpaper.get("author"), ago));
		textResolution.setText(String.format("%sx%s", wallpaper.get("width"), wallpaper.get("height")));
		textScore.setText(wallpaper.get("score"));
		textFlair.setText(wallpaper.get("flair"));
		textComment.setText(wallpaper.get("comments"));
		if (Objects.equals(wallpaper.get("flair"), "null")) {
			textFlair.setVisibility(View.INVISIBLE);
		} else {
			textFlair.setVisibility(View.VISIBLE);
		}
		
		convertView.setOnClickListener(v -> {
			Intent intent = new Intent(context, DownloadActivity.class);
			intent.putExtra("map", wallpaper);
			context.startActivity(intent);
		});
		
		return convertView;
	}
	
}
