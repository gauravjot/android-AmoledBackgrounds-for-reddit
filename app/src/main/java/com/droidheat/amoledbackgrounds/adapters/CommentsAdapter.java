package com.droidheat.amoledbackgrounds.adapters;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.droidheat.amoledbackgrounds.R;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class CommentsAdapter extends BaseAdapter {

  ArrayList<HashMap<String, String>> arrayList1 = new ArrayList<>();
  Context c;

  CommentsAdapter(Context context) {
    c = context;
  }

  @Override
  public int getCount() {
    return arrayList1.size();
  }

  public void resetWithItems(ArrayList<HashMap<String, String>> a) {
    arrayList1.clear();
    arrayList1.addAll(a);
    notifyDataSetChanged();
  }

  @Override
  public Object getItem(int position) {
    return arrayList1.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    if (convertView == null) {
      LayoutInflater layoutInflater = LayoutInflater.from(c);
      convertView = layoutInflater.inflate(R.layout.my_comment_item, null);
    }

    HashMap<String, String> hashMap = arrayList1.get(position);

    String data = hashMap.get("body");
    //data = data.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
    //data = data.replaceAll("\\+", "%2B");
//        try {
//            data = java.net.URLDecoder.decode(data, StandardCharsets.UTF_8.name());
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }

    data = Html.escapeHtml(data);

    data = "&lt;div style=quot;padding-left:" + (Integer.parseInt(hashMap.get("parent")) * 5)
            + "quot;&gt;" + data + "&lt;/dic&gt;";

    data = decodeHTML(data);

    TextView textView = convertView.findViewById(R.id.textView);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      textView.setText(hashMap.get("author") + "\n" + trimTrailingWhitespace(Html.fromHtml(data, Html.FROM_HTML_OPTION_USE_CSS_COLORS)));
    } else {
      textView.setText(hashMap.get("author") + "\n" + Html.fromHtml(data));
    }

    return convertView;
  }

  @SuppressWarnings("StatementWithEmptyBody")
  private CharSequence trimTrailingWhitespace(CharSequence source) {

    if (source == null)
      return "";

    int i = source.length();

    // loop back to the first non-whitespace character
    while (--i >= 0 && Character.isWhitespace(source.charAt(i))) {
    }

    return source.subSequence(0, i + 1);
  }

  private String decodeHTML(String data) {
    data = data.replaceAll("&amp;", "&");
    data = data.replaceAll("&amp;", "&");
    data = data.replaceAll("&lt;", "<");
    data = data.replaceAll("&gt;", ">");
    data = data.replaceAll("&quot;", "\"");
    data = data.replaceAll("&#39;", "'");
    data = data.replaceAll("&#10;", "");
    return data;
  }
}
