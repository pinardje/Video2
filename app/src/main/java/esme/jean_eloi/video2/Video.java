package esme.jean_eloi.video2;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * Created by Jean-Eloi on 15/12/2017.
 */

public class Video extends Bluetooth {

    VideoView vidView;

    private Handler mHandlerVid = new Handler();


    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(getValuePotar() == 1){
                if(vidView.isPlaying())
                    vidView.pause();
                else
                    vidView.start();
            }
            Log.e("Is playing ? ", String.valueOf(vidView.isPlaying()));

            mHandlerVid.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        vidView = (VideoView) findViewById(R.id.myVideo);
        vidView.setVideoURI(vidUri);
        vidView.start();

        MediaController vidControl = new MediaController(this);
        vidControl.setAnchorView(vidView);
        vidView.setMediaController(vidControl);

        mHandlerVid.post(mRunnable);


    }

    String vidAddress = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
    Uri vidUri = Uri.parse(vidAddress);

    public void Back(View v) {
        //pour retourner a l’activite principale il suffit seulement d’appler la methode finish() qui vas tuer cette activite

        finish();

    }


    /*@Override
    protected void onResume() {
        super.onResume();
        VideoView vidView = (VideoView) findViewById(R.id.myVideo);
        if(getValuePotar() == 1){
            if(vidView.isPlaying()){
                vidView.pause();
            }else{
                vidView.resume();
            }
        }
    }*/
}
