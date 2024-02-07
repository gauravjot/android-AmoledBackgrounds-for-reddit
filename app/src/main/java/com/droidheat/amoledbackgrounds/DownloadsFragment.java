package com.droidheat.amoledbackgrounds;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.droidheat.amoledbackgrounds.adapters.MyDownloadsAdapter;

public class DownloadsFragment extends Fragment {

  public DownloadsFragment() {
  }

  MyDownloadsAdapter myDownloadsAdapter;
  GridView gridView;
  Button permissionBtn;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_downloads, container, false);

    gridView = view.findViewById(R.id.gridView);
    permissionBtn = view.findViewById(R.id.button_permission);

    myDownloadsAdapter = new MyDownloadsAdapter(getActivity());
    gridView.setAdapter(myDownloadsAdapter);

    final String PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    if (ContextCompat.checkSelfPermission(requireActivity(), PERMISSION) != PackageManager.PERMISSION_GRANTED) {
      gridView.setVisibility(View.INVISIBLE);
      requestPermissionLauncher.launch(PERMISSION);
    } else {
      permissionBtn.setVisibility(View.INVISIBLE);
      gridView.setVisibility(View.VISIBLE);
    }

    permissionBtn.setOnClickListener(v -> requestPermissionLauncher.launch(PERMISSION));

    return view;
  }

  private final ActivityResultLauncher<String> requestPermissionLauncher =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
              permissionBtn.setVisibility(View.INVISIBLE);
              gridView.setVisibility(View.VISIBLE);
            } else {
              Toast.makeText(getActivity(), "Read Permission Denied", Toast.LENGTH_SHORT).show();
              // Explain to the user that the feature is unavailable because the
              // features requires a permission that the user has denied. At the
              // same time, respect the user's decision. Don't link to system
              // settings in an effort to convince the user to change their
              // decision.
            }
          });

}
