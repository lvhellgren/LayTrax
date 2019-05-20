package com.exelor.laytrax;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class RunnerFragment extends Fragment implements View.OnClickListener {

    private String tag;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.tag = this.getClass().getSimpleName();
        ((MainActivity) getActivity()).headerText.setText("Tracking Service is Running");

        this.view = inflater.inflate(R.layout.runner_fragment, container, false);

        Button button = (Button) this.view.findViewById(R.id.stop_button);
        button.setOnClickListener(this);

        return this.view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.stop_button:
                stopTrackingService();
                showConnectorFragment();
                break;
        }
    }

    private void stopTrackingService() {
        try {
            Intent intent = new Intent(getActivity(), TrackingService.class);
            if (getActivity().stopService(intent)) {
                Log.d(tag, "Service stopped");
                Toast.makeText(getActivity(), "Service stopped", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(tag, "Could not stop the service");
            }

        } catch (Exception e) {
            Log.e(tag, e.toString());
        }
    }

    private void showConnectorFragment() {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }
}
