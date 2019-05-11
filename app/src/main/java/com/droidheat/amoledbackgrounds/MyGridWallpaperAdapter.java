package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MyGridWallpaperAdapter extends BaseAdapter {

    private ArrayList<HashMap<String,String>> arrayList;
    private Context context;

    MyGridWallpaperAdapter(Context c) {
        arrayList = new ArrayList<>();
        context = c;
    }

    public void addItems(ArrayList<HashMap<String,String>> a) {
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

        final HashMap<String,String> wallpaper = arrayList.get(position);

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

        Picasso.get().load(wallpaper.get("image")).resize(560,0).into(preview);
        textView.setText(wallpaper.get("title").replaceAll("\\(.*?\\) ?", "").replaceAll("\\[.*?\\] ?", "")
                .replaceAll("\\{[^}]*\\}", ""));
        if (Objects.equals(wallpaper.get("author_flair"), "null")) {
            textAuthor.setText(String.format("u/%s", wallpaper.get("author")));
        } else {
            textAuthor.setText(String.format("u/%s • %s", wallpaper.get("author"), wallpaper.get("author_flair")));
        }
        textResolution.setText(String.format("%sx%s", wallpaper.get("width"), wallpaper.get("height")));
        textScore.setText(wallpaper.get("score"));
        textFlair.setText(wallpaper.get("flair"));
        textComment.setText(wallpaper.get("comments"));
        if (Objects.equals(wallpaper.get("flair"), "null")) {
            textFlair.setVisibility(View.INVISIBLE);
        } else {
            textFlair.setVisibility(View.VISIBLE);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context,DownloadActivity.class);
                intent.putExtra("map",wallpaper);
                context.startActivity(intent);
            }
        });

        return convertView;
    }

}
