package com.droidheat.amoledbackgrounds;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class UtilsJSON {

    public ArrayList<HashMap<String,String>> grabPostsAsArrayList(Context context, String url) {
        ArrayList<HashMap<String,String>> arrayList = new ArrayList<>();

        try {
            String html = jsonGetRequest(url);
            Log.d("UtilsJSON", html);

            // html is JSON
            // Start of Organising JSON to ArrayList
            {
                JSONObject jsonObject = new JSONObject(html.trim());
                JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("children");

                for (int i=0; i < jsonArray.length(); i++) {
                    JSONObject dataObject = jsonArray.getJSONObject(i).getJSONObject("data");
                    HashMap<String, String> hashMap = new HashMap<>();

                    String mURl = dataObject.getString("url");
                    String flair = dataObject.getString("link_flair_text");

                    // Filtering out posts with images and bad flairs
                    if (
                           (mURl.substring(mURl.lastIndexOf(".") + 1).equals("png") ||
                            mURl.substring(mURl.lastIndexOf(".") + 1).equals("jpg") ||
                            mURl.substring(mURl.lastIndexOf(".") + 1).equals("jpeg")) &&
                            !flair.toUpperCase().contains("META") &&
                            !flair.toUpperCase().contains("PSA")
                      ) {
                        hashMap.put("image",mURl);
                        try {
                            int len = dataObject.getJSONObject("preview")
                                    .getJSONArray("images").getJSONObject(0)
                                    .getJSONArray("resolutions").length();
                            if (!(new SharedPrefsUtils(context)).readSharedPrefsBoolean("lower_thumbnail_quality",false)) {
                                JSONObject previewObject = dataObject.getJSONObject("preview")
                                        .getJSONArray("images").getJSONObject(0)
                                        .getJSONArray("resolutions").getJSONObject(len - 3);
                                hashMap.put("preview",previewObject.getString("url").replaceAll("&amp;","&"));
                            } else {
                                int i_n = 4;
                                if (len > 4) {
                                    i_n = 5;
                                }

                                JSONObject previewObject = dataObject.getJSONObject("preview")
                                        .getJSONArray("images").getJSONObject(0)
                                        .getJSONArray("resolutions").getJSONObject(len - i_n);
                                hashMap.put("preview",previewObject.getString("url").replaceAll("&amp;","&"));
                            }
                        } catch (Exception e) {
                            hashMap.put("preview",dataObject.getString("url"));
                            e.printStackTrace();}
                        hashMap.put("flair",flair);
                        hashMap.put("title",dataObject.getString("title"));
                        hashMap.put("created_utc",dataObject.getLong("created_utc")+"");
                        hashMap.put("name",dataObject.getString("name"));
                        hashMap.put("domain",dataObject.getString("domain"));
                        hashMap.put("score",dataObject.getString("score"));
                        hashMap.put("over_18",dataObject.getString("over_18"));
                        hashMap.put("author",dataObject.getString("author"));
                        hashMap.put("author_flair",dataObject.getString("author_flair_text"));
                        hashMap.put("postlink",dataObject.getString("permalink"));
                        hashMap.put("comments",dataObject.getString("num_comments"));
                        JSONObject imageObject = dataObject.getJSONObject("preview")
                                .getJSONArray("images").getJSONObject(0).getJSONObject("source");
                        hashMap.put("width",imageObject.getString("width"));
                        hashMap.put("height",imageObject.getString("height"));
                        // Adding data to arraylist
                        arrayList.add(hashMap);
                    }
                }

                // When all files are done in arraylist, lets add metadata hashmap we get from reddit at last
                // When populating UI with items we will exclude last entry or remove it which is metadata
                HashMap<String,String> metadata = new HashMap<>();
                metadata.put("after",jsonObject.getJSONObject("data").getString("after"));
                arrayList.add(metadata);
            }
        }
        catch (Exception e) { e.printStackTrace();}

        return arrayList;
    }

    public ArrayList<HashMap<String, String>> grabPostComments(String postURL) {
        ArrayList<HashMap<String,String>> arrayList = new ArrayList<>();
        try {
            String html = jsonGetRequest(postURL);

            // html is JSON
            // Start of Organising JSON to ArrayList
            {
                JSONArray fileArray = new JSONArray(html.trim());
                fileArray.remove(0);

                JSONArray commentArray = fileArray.getJSONObject(0).getJSONObject("data")
                        .getJSONArray("children");

                for (int i=0; i < commentArray.length(); i++) {
                    addCommentToArrayList(0, arrayList, commentArray, i);
                }
            }
        }
        catch (Exception e) {
            Log.d("commentAdapter",e.toString());
        }

        return arrayList;
    }

    private void addCommentToArrayList(int parent, ArrayList<HashMap<String,String>> arrayList, JSONArray jsonArray, int position) {
            try {
                JSONObject dataObject = jsonArray.getJSONObject(position).getJSONObject("data");
                HashMap<String, String> hashMap = new HashMap<>();

                hashMap.put("name",dataObject.getString("name"));
                hashMap.put("score",dataObject.getString("score"));
                hashMap.put("author",dataObject.getString("author"));
                hashMap.put("author_flair",dataObject.getString("author_flair_text"));
                hashMap.put("comment_link",dataObject.getString("permalink"));
                hashMap.put("body",dataObject.getString("body_html"));
                hashMap.put("parent", Integer.toString(parent));
                hashMap.put("utc",dataObject.getLong("created_utc")+"");
                arrayList.add(hashMap);

                if (!dataObject.getString("replies").equals("")) {
                    // Sub-comment exist
                    JSONArray repliesArray = dataObject.getJSONObject("replies")
                            .getJSONObject("data").getJSONArray("children");
                    parent++;
                    for (int j=0;j<repliesArray.length();j++) {
                        addCommentToArrayList(parent,arrayList, repliesArray, j);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    private static String jsonGetRequest(String urlQueryString) {
        String json = null;
        try {
            URL url = new URL(urlQueryString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("GET");
            connection.connect();
            InputStream inStream = connection.getInputStream();
            json = streamToString(inStream); // input stream to string
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return json;
    }
    private static String streamToString(InputStream inputStream) {
        return new Scanner(inputStream, "UTF-8").useDelimiter("\\Z").next();
    }
}
