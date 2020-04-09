package com.droidheat.amoledbackgrounds.Utils;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Objects;

public class FunctionUtils {

    public String changeWallpaper(Context context, String pathOfNewWallpaper) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission error","Read permission is not granted");
        } else {
            Log.d("Permission error","Read permission is granted");
        }

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        Bitmap bitmap = getBitmap(context,pathOfNewWallpaper);
        if (bitmap != null) {
            try {
                wallpaperManager.setBitmap(bitmap);
                return "success";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "failure";
    }

    public String getFilePath(Context context, String fileName) {
        String path = "/storage/emulated/0/Pictures";
        Log.d("PATH",path);
        return path + "/" + fileName;
    }

    public String purifyRedditFileTitle(String rawFileTitle, String redditUniqueName) {
        // rawFileName      = wallpaper.get("title")
        // redditUniqueName = wallpaper.get("name")

        String titleStr = Objects.requireNonNull(rawFileTitle).replaceAll("\\(.*?\\) ?", "").replaceAll("\\[.*?\\] ?", "")
                .replaceAll("\\{[^}]*\\}", "");
        titleStr = titleStr.replaceAll("\\u00A0", " ").trim();
        titleStr = titleStr.replaceAll(" ", "_") + "_" + redditUniqueName;
        titleStr = titleStr.substring(0, Math.min(titleStr.length(), 50));
        titleStr = validFileNameConvert(titleStr);
        return  titleStr;
    }

    public String purifyRedditFileExtension(String rawImageName) {
        return Objects.requireNonNull(rawImageName)
                .substring(Objects.requireNonNull(rawImageName).lastIndexOf("."))
                .trim();

    }

    private String validFileNameConvert(String string) {
        String newS = string.replaceAll("[\\\\/:*?\"<>|]", "");
        return newS.replace(".","").replaceAll("%","")+ "_amoled_droidheat";
    }

    private Bitmap getBitmap(Context context, String pathOfNewWallpaper) {

        if (Build.VERSION.SDK_INT >= 29) {
            String[] projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.RELATIVE_PATH
            };
            String selection = MediaStore.Images.Media.DISPLAY_NAME + " == ?";
            String[] selectionArgs = new String[]  {
                    pathOfNewWallpaper.substring(pathOfNewWallpaper.lastIndexOf("/")).replace("/","")
                            .trim()
            };
            final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs, MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                    // You can replace '0' by 'cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)'
                    // Note that now, you read the column '_ID' and not the column 'DATA'
                    Uri imageUri= ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt(0));

                    // now that you have the media URI, you can decode it to a bitmap
                    try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(imageUri, "r")) {
                        if (pfd != null) {
                            return BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                        }
                    } catch (IOException ex) {
                        return null;
                    }
            }
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(pathOfNewWallpaper, options);
        }
        return null;
    }

}
