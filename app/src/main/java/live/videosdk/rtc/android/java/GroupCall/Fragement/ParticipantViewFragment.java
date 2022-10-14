package live.videosdk.rtc.android.java.GroupCall.Fragement;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.Participant;
import live.videosdk.rtc.android.Stream;
import live.videosdk.rtc.android.java.GroupCall.Activity.GroupCallActivity;
import live.videosdk.rtc.android.java.GroupCall.Listener.ParticipantChangeListener;
import live.videosdk.rtc.android.java.GroupCall.Utils.ParticipantState;
import live.videosdk.rtc.android.java.R;
import live.videosdk.rtc.android.lib.PeerConnectionUtils;
import live.videosdk.rtc.android.listeners.ParticipantEventListener;

public class ParticipantViewFragment extends Fragment {

    GridLayout participantGridLayout;
    int position;
    Meeting meeting;
    ParticipantChangeListener participantChangeListener;
    ParticipantState participantState;
    EglBase.Context eglContext;

    private List<Participant> participants;
    private List<List<Participant>> finalParticipantList;

    TabLayoutMediator tabLayoutMediator;
    ViewPager2 viewPager2;
    TabLayout tabLayout;
    private boolean screenShareFlag =false;

    public ParticipantViewFragment() {
        // Required empty public constructor
    }

    public ParticipantViewFragment(Meeting meeting, int position)
    {
        this.meeting=meeting;
        this.position=position;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view= inflater.inflate(R.layout.fragment_view, container, false);

        participantGridLayout = view.findViewById(R.id.participantGridLayout);

        viewPager2= getActivity().findViewById(R.id.view_pager_video_grid);
        tabLayout = getActivity().findViewById(R.id.tab_layout_dots);

        eglContext = PeerConnectionUtils.getEglContext();

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        participantGridLayout.setOnTouchListener(((GroupCallActivity)getActivity()).getOnTouchListener());

        participantChangeListener=new ParticipantChangeListener() {
            @Override
            public void onChangeParticipant(List<List<Participant>> participantList) {
                finalParticipantList=participantList;
                if (position < participantList.size()) {
                        participants = participantList.get(position);
                        updateGridLayout();
                        showInGUI();
                        tabLayoutMediator=new TabLayoutMediator(tabLayout, viewPager2,true,
                                (tab, position) -> Log.d("TAG", "onCreate: ")
                        );

                        if(tabLayoutMediator.isAttached()) {
                            tabLayoutMediator.detach();
                        }

                        tabLayoutMediator.attach();

                        if(participantList.size() == 1){
                            tabLayout.setVisibility(View.GONE);
                        }else{
                            tabLayout.setVisibility(View.VISIBLE);
                        }

                }
            }

             @Override
             public void onPresenterChanged(boolean screenShare) {
                screenShareFlag=screenShare;
                 updateGridLayout();
                 showInGUI();
             }
         };

         participantState=ParticipantState.getInstance(meeting);
         participantState.addParticipantChangeListener(participantChangeListener);

    }

    @Override
    public void onResume() {
        if(position < finalParticipantList.size()) {

            List<Participant> currentParticipants = finalParticipantList.get(position);
            for (int i = 0; i < currentParticipants.size(); i++) {
                Participant participant = currentParticipants.get(i);
                if (!participant.isLocal()) {
                    for (Map.Entry<String, Stream> entry : participant.getStreams().entrySet()) {
                        Stream stream = entry.getValue();
                        if (stream.getKind().equalsIgnoreCase("video"))
                            stream.resume();
                    }
                }
            }
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        if(position < finalParticipantList.size()) {
            List<Participant> otherParticipants = new ArrayList<>();

            for (int i = 0; i < finalParticipantList.size(); i++) {
                if (position == i) {
                    continue;
                }
                otherParticipants = finalParticipantList.get(i);
            }

            for (int i = 0; i < otherParticipants.size(); i++) {
                Participant participant = otherParticipants.get(i);
                if (!participant.isLocal()) {
                    for (Map.Entry<String, Stream> entry : participant.getStreams().entrySet()) {
                        Stream stream = entry.getValue();
                        if (stream.getKind().equalsIgnoreCase("video")) {
                            stream.pause();
                        }
                    }
                }
            }
        }
        super.onPause();
    }

    // Call where View ready.
    private void showInGUI()  {

        for(int i = 0; i< participants.size(); i++) {

            Participant participant= participants.get(i);

            View participantView=LayoutInflater.from(getContext())
                    .inflate(R.layout.item_participant, participantGridLayout, false);

            TextView tvName = participantView.findViewById(R.id.tvName);
            TextView txtParticipantName=participantView.findViewById(R.id.txtParticipantName);

            SurfaceViewRenderer svrParticipant = participantView.findViewById(R.id.svrParticipantView);
            try {
                svrParticipant.init(eglContext, null);
            }catch (Exception e){
                Log.e("Error", "showInGUI: "+ e.getMessage() );
            }

            if(participant.getId() == meeting.getLocalParticipant().getId())
            {
                tvName.setText("You");
            }else {
                tvName.setText(participant.getDisplayName());
            }
            txtParticipantName.setText(participant.getDisplayName().substring(0, 1));

            ImageView ivMicStatus = participantView.findViewById(R.id.ivMicStatus);

            for (Map.Entry<String, Stream> entry : participant.getStreams().entrySet()) {
                Stream stream = entry.getValue();
                if (stream.getKind().equalsIgnoreCase("video")) {
                    svrParticipant.setVisibility(View.VISIBLE);

                    VideoTrack videoTrack = (VideoTrack) stream.getTrack();
                    videoTrack.addSink(svrParticipant);

                    break;
                } else if (stream.getKind().equalsIgnoreCase("audio")) {
                    ivMicStatus.setImageResource(R.drawable.ic_mic_on);
                }

            }

            participant.addEventListener(new ParticipantEventListener() {
                @Override
                public void onStreamEnabled(Stream stream) {
                    if (stream.getKind().equalsIgnoreCase("video")) {
                        svrParticipant.setVisibility(View.VISIBLE);

                        VideoTrack videoTrack = (VideoTrack) stream.getTrack();
                        videoTrack.addSink(svrParticipant);

                    } else if (stream.getKind().equalsIgnoreCase("audio")) {
                        ivMicStatus.setImageResource(R.drawable.ic_mic_on);
                    }
                }

                @Override
                public void onStreamDisabled(Stream stream) {
                    if (stream.getKind().equalsIgnoreCase("video")) {
                        VideoTrack track = (VideoTrack) stream.getTrack();
                        if (track != null) track.removeSink(svrParticipant);

                        svrParticipant.clearImage();
                        svrParticipant.setVisibility(View.GONE);

                    } else if (stream.getKind().equalsIgnoreCase("audio")) {
                        ivMicStatus.setImageResource(R.drawable.ic_mic_off);
                    }
                }
            });

            participantGridLayout.addView(participantView);
        }
    }

    @Override
    public void onDestroy() {
        if(participantChangeListener!= null ) {
            participantState.removeParticipantChangeListener(participantChangeListener);
        }

        for(int i=0;i<participantGridLayout.getChildCount();i++) {
            View view = participantGridLayout.getChildAt(i);
            SurfaceViewRenderer surfaceViewRenderer = view.findViewById(R.id.svrParticipantView);
            if(surfaceViewRenderer!=null) {
                surfaceViewRenderer.clearImage();
                surfaceViewRenderer.setVisibility(View.GONE);
                surfaceViewRenderer.release();
            }
        }

        participantGridLayout.removeAllViews();

        super.onDestroy();
    }

    public void updateGridLayout()
    {
        for(int i=0;i<participantGridLayout.getChildCount();i++) {
            View view = participantGridLayout.getChildAt(i);
            SurfaceViewRenderer surfaceViewRenderer = view.findViewById(R.id.svrParticipantView);
            if(surfaceViewRenderer!=null) {
                surfaceViewRenderer.clearImage();
                surfaceViewRenderer.setVisibility(View.GONE);
                surfaceViewRenderer.release();
            }
        }

        participantGridLayout.removeAllViews();

        if(screenShareFlag)
        {
            participantGridLayout.setColumnCount(2);
            participantGridLayout.setRowCount(1);
        }else
        {
            if(participants.size() == 1){
                participantGridLayout.setColumnCount(1);
                participantGridLayout.setRowCount(1);
            }
            else if(participants.size() == 2) {
                participantGridLayout.setColumnCount(1);
                participantGridLayout.setRowCount(2);
            }else{
                participantGridLayout.setColumnCount(2);
                participantGridLayout.setRowCount(2);
            }
        }

    }

}