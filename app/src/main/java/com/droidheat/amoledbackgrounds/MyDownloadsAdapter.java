package com.droidheat.amoledbackgrounds;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class MyDownloadsAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String,String>> arrayList;
    private String currentWallpaper = "", currentlyInAsync ="";

    MyDownloadsAdapter (Context c) {
        context = c;
        arrayList = new ArrayList<>(getItems());
    }

    private ArrayList<HashMap<String,String>> getItems() {
        ArrayList<HashMap<String,String>> result = new ArrayList<>(); //ArrayList cause you don't know how many files there is

        try {
            File folder = new File(Environment.getExternalStorageDirectory()
                    + "/AmoledBackgrounds"); //This is just to cast to a File type since you pass it as a String
            File[] filesInFolder = folder.listFiles(); // This returns all the folders and files in your path
            for (File file : filesInFolder) { //For each of the entries do:
                if (!file.isDirectory()) { //check that it's not a dir
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("title", file.getName());
                    hashMap.put("path", file.getPath());
                    hashMap.put("uri", file.toURI().toString());
                    result.add(hashMap);
                }
            }
            Collections.reverse(result);
        } catch (Exception ignore) {}
        return result;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.my_downloads_list_item,null);
        }

        final TextView textView = convertView.findViewById(R.id.title);
        final ImageView imageView = convertView.findViewById(R.id.preview);
        final ImageView imageView1  = convertView.findViewById(R.id.download);
        imageView1.setImageResource(R.drawable.ic_wallpaper_black_24dp);

        textView.setText((Objects.requireNonNull(arrayList.get(position).get("title"))).split("_t3_")[0].replace("_"," "));

        Picasso.get().load(Uri.parse(arrayList.get(position).get("uri"))).resize(480,0).into(imageView);

        if (currentWallpaper.equals(arrayList.get(position).get("title"))) {
            imageView1.setImageResource(R.drawable.ic_check_black_24dp);
        }

        final ProgressBar progressBar = convertView.findViewById(R.id.progress_circular);
        if (!currentlyInAsync.equals(arrayList.get(position).get("title"))) {
            progressBar.setVisibility(View.INVISIBLE);
            imageView1.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            imageView1.setVisibility(View.INVISIBLE);
        }

        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentWallpaper.equals(arrayList.get(position).get("title"))) {
                    progressBar.setVisibility(View.VISIBLE);
                    imageView1.setVisibility(View.INVISIBLE);
                    SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask();
                    String[] strings = new String[2];
                    strings[0] = arrayList.get(position).get("title");
                    strings[1] = arrayList.get(position).get("path");
                    setWallpaperAsyncTask.execute(strings);
                }
            }
        });

        return convertView;
    }

    public void refresh() {
        arrayList.clear();
        arrayList.addAll(getItems());
        notifyDataSetChanged();
    }

    class SetWallpaperAsyncTask extends AsyncTask<String,Integer,String> {

        String title ="";
        @Override
        protected String doInBackground(String... strings) {
            try {
                title = strings[0];
                currentlyInAsync = title;
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(strings[1], options);
                wallpaperManager.setBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(context,"Wallpaper set!",Toast.LENGTH_SHORT).show();
            currentlyInAsync = "";
            currentWallpaper = title;
            notifyDataSetChanged();
        }
    }

}
