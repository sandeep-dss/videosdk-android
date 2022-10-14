package live.videosdk.rtc.android.java.Common.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.snackbar.Snackbar;

import live.videosdk.rtc.android.java.GroupCall.Activity.GroupCallActivity;
import live.videosdk.rtc.android.java.Common.Activity.CreateOrJoinActivity;
import live.videosdk.rtc.android.java.OneToOneCall.OneToOneCallActivity;
import live.videosdk.rtc.android.java.Common.Listener.ResponseListener;
import live.videosdk.rtc.android.java.Common.Utils.HelperClass;
import live.videosdk.rtc.android.java.Common.Utils.NetworkUtils;
import live.videosdk.rtc.android.java.R;

public class CreateMeetingFragment extends Fragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create_meeting, container, false);

        EditText etName = view.findViewById(R.id.etName);

        Button btnJoin = view.findViewById(R.id.btnJoin);

        String[] meetingType = getContext().getResources().getStringArray(R.array.meeting_options);

        final String[] selectedMeetingType = new String[1];

        ArrayAdapter arrayAdapter = new ArrayAdapter(getContext(), R.layout.dropdown_item, meetingType);

        AutoCompleteTextView autocompleteTV = (AutoCompleteTextView) view.findViewById(R.id.autoCompleteTextView);
        autocompleteTV.setAdapter(arrayAdapter);
        autocompleteTV.setDropDownBackgroundDrawable(
                ResourcesCompat.getDrawable(
                        getContext().getResources(),
                        R.drawable.et_style,
                        null
                )
        );

        autocompleteTV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedMeetingType[0] = meetingType[i];
            }
        });


        btnJoin.setOnClickListener(v -> {
            if ("".equals(etName.getText().toString())) {
                Toast.makeText(getContext(), "Please Enter Name", Toast.LENGTH_SHORT).show();
            } else {
                NetworkUtils networkUtils = new NetworkUtils(getContext());
                if (networkUtils.isNetworkAvailable()) {
                    networkUtils.getToken(new ResponseListener() {
                        @Override
                        public void onResponse(String token) {
                            networkUtils.createMeeting(token, new ResponseListener() {
                                @Override
                                public void onResponse(String meetingId) {
                                    Intent intent = null;
                                    if (!TextUtils.isEmpty(selectedMeetingType[0])) {
                                        if (selectedMeetingType[0].equals("One to One Meeting")) {
                                            intent = new Intent((CreateOrJoinActivity) getActivity(), OneToOneCallActivity.class);
                                        } else {
                                            intent = new Intent((CreateOrJoinActivity) getActivity(), GroupCallActivity.class);
                                        }
                                        intent.putExtra("token", token);
                                        intent.putExtra("meetingId", meetingId);
                                        intent.putExtra("webcamEnabled", ((CreateOrJoinActivity) getActivity()).isWebcamEnabled());
                                        intent.putExtra("micEnabled", ((CreateOrJoinActivity) getActivity()).isMicEnabled());
                                        intent.putExtra("participantName", etName.getText().toString().trim());
                                        intent.putExtra("createMeeting", true);
                                        startActivity(intent);
                                        ((CreateOrJoinActivity) getActivity()).finish();
                                    } else {
                                        Toast.makeText(getContext(),
                                                "Please Choose Meeting Type", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onMeetingTimeChanged(int meetingTime) {

                                }

                            });
                        }

                        @Override
                        public void onMeetingTimeChanged(int meetingTime) {

                        }
                    });

                } else {
                    Snackbar snackbar = Snackbar.make(view.findViewById(R.id.createMeetingLayout), "No Internet Connection", Snackbar.LENGTH_LONG);
                    HelperClass.setSnackNarStyle(snackbar.getView(), 0);
                    snackbar.show();
                }
            }

        });
        return view;
    }
}