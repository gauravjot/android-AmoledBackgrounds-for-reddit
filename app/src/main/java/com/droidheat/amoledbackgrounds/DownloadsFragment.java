package com.droidheat.amoledbackgrounds;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
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
	
	GridView gridView;
	Button permissionBtn;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_downloads, container, false);
		
		gridView = view.findViewById(R.id.gridView);
		permissionBtn = view.findViewById(R.id.button_permission);
		
		// Choose appropriate permission based on Android version
		String PERMISSION;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			PERMISSION = Manifest.permission.READ_MEDIA_IMAGES;
		} else {
			PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;
		}
		// Check if permission is granted
		if (ContextCompat.checkSelfPermission(requireActivity(), PERMISSION) != PackageManager.PERMISSION_GRANTED) {
			// Hide empty gridview
			gridView.setVisibility(View.INVISIBLE);
			// Request permission
			requestPermissionLauncher.launch(PERMISSION);
		} else {
			// Permission already granted; load images
			loadImages();
		}
		
		permissionBtn.setOnClickListener(v -> requestPermissionLauncher.launch(PERMISSION));
		
		return view;
	}
	
	private void loadImages() {
		MyDownloadsAdapter myDownloadsAdapter = new MyDownloadsAdapter(getActivity());
		gridView.setAdapter(myDownloadsAdapter);
		permissionBtn.setVisibility(View.INVISIBLE);
		gridView.setVisibility(View.VISIBLE);
	}
	
	private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
		if (isGranted) {
			loadImages();
		} else {
			Toast.makeText(requireActivity(), "Read Permission Denied. If you wish, you can go into Settings > Apps > AmoledBackgrounds > Permissions.", Toast.LENGTH_LONG).show();
		}
	});
	
}
