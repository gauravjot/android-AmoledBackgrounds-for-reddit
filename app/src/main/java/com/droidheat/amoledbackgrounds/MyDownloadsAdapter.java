package com.droidheat.amoledbackgrounds;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.droidheat.amoledbackgrounds.Utils.FunctionUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class MyDownloadsAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, String>> arrayList;
    private String currentWallpaper = "", currentlyInAsync = "";

    MyDownloadsAdapter(Context c) {
        context = c;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            arrayList = new ArrayList<>();
        } else {
            arrayList = new ArrayList<>(getItems());
        }
    }

    private ArrayList<HashMap<String,String>> getItemsLegacy() {
        ArrayList<HashMap<String,String>> result = new ArrayList<>(); //ArrayList cause you don't know how many files there is

        try {
            File folder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES); //This is just to cast to a File type since you pass it as a String
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

    private ArrayList<HashMap<String, String>> getItems() {

        ArrayList<HashMap<String, String>> result = new ArrayList<>(); //ArrayList cause you don't know how many files there is

        if (Build.VERSION.SDK_INT >= 29) {
            String[] projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.RELATIVE_PATH
            };
            String selection = "";
            String[] selectionArgs = new String[]{};
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " ASC";

            try (Cursor cursor = context.getApplicationContext().getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            )) {
                // Cache column indices.
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int durationColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    int width = cursor.getInt(durationColumn);
                    int height = cursor.getInt(sizeColumn);
                    String path = cursor.getString(pathColumn);

                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    if (name.contains("_amoled_droidheat")) {
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put("title", name);
                        hashMap.put("path", "/storage/emulated/0/" + path + name);
                        hashMap.put("uri", contentUri.toString());
                        result.add(hashMap);
                    }
                }
            }
        } else {
        File folder = new File("/storage/emulated/0/Pictures"); //This is just to cast to a File type since you pass it as a String
            File[] filesInFolder = folder.listFiles(); // This returns all the folders and files in your path
            if (filesInFolder != null) {
                for (File file : filesInFolder) { //For each of the entries do:
                    if (!file.isDirectory()) { //check that it's not a dir
                        if (file.getName().contains("_amoled")) {
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("title", file.getName());
                            hashMap.put("path", file.getPath());
                            hashMap.put("uri", file.toURI().toString());
                            result.add(hashMap);
                        }
                    }
                }
            }
        }
        Collections.reverse(result);
        result.addAll(getItemsLegacy());
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
            convertView = layoutInflater.inflate(R.layout.my_downloads_list_item, null);
        }

        final TextView textView = convertView.findViewById(R.id.title);
        final ImageView imageView = convertView.findViewById(R.id.preview);
        final ImageView imageView1 = convertView.findViewById(R.id.download);
        final ImageView options = convertView.findViewById(R.id.options);
        imageView1.setImageResource(R.drawable.ic_wallpaper_black_24dp);

        textView.setText((Objects.requireNonNull(arrayList.get(position).get("title"))).split("_t3_")[0].replace("_", " "));

        Picasso.get().load(Uri.parse(arrayList.get(position).get("uri"))).resize(480, 0).into(imageView);

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

        final PopupMenu popupMenu = new PopupMenu(context, options);
        popupMenu.getMenu().add(0, R.id.popup_share, 1, "Share");

        if (Build.VERSION.SDK_INT < 29) {
            popupMenu.getMenu().add(0, R.id.popup_delete, 1, "Delete");
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_share:
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Here is a wallpaper from " + R.string.app_name);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(arrayList.get(position).get("uri")));
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(Intent.createChooser(shareIntent, "Send to"));
                        return true;
                    case R.id.popup_delete:
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Do you want to delete " + arrayList.get(position).get("title"))
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (Build.VERSION.SDK_INT >= 29) {
                                            // Remove a specific media item.
                                            ContentResolver resolver = context.getApplicationContext().getContentResolver();
                                            Uri imageUri = Uri.parse(arrayList.get(position).get("uri"));
                                            String selection = "";
                                            String[] selectionArgs = new String[]{};
                                            resolver.delete(
                                                    imageUri,
                                                    selection,
                                                    selectionArgs);
                                        } else {
                                            File file = new File(Objects.requireNonNull(arrayList.get(position).get("path")));
                                            if(!file.delete()) {
                                                Toast.makeText(context,"Unable to delete wallpaper. Do it manually.",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        }
                                        refresh();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                        // Create the AlertDialog object and return it
                        builder.create();
                        builder.show();
                        return true;
                    default:
                        return false;
                }
            }
        });

        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //popupMenu.show();
            }
        });

        options.setVisibility(View.INVISIBLE);

        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {

            }
        });

        return convertView;
    }

    public void refresh() {
        arrayList.clear();
        arrayList.addAll(getItems());
        notifyDataSetChanged();
    }

    class SetWallpaperAsyncTask extends AsyncTask<String, Integer, String> {

        String title = "";

        @Override
        protected String doInBackground(String... strings) {
                title = strings[0];
                currentlyInAsync = title;
                return (new FunctionUtils()).changeWallpaper(context,strings[1]);
        }

        @Override
        protected void onPostExecute(String s) {
            currentlyInAsync = "";
            if (s == "success") {
                Toast.makeText(context, "Wallpaper set!", Toast.LENGTH_SHORT).show();
                currentWallpaper = title;
            } else {
                Toast.makeText(context, "Failed to set wallpaper.", Toast.LENGTH_SHORT).show();
            }
            notifyDataSetChanged();
        }
    }

}
