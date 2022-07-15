package edu.iss.team1ca;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.somik.team1ca.R;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private List<Bitmap> pickedImages = new ArrayList<>();
    private List<Bitmap> coveredImages = new ArrayList<>();
    private Bitmap[] bitmapArray = new Bitmap[12];
    private int firstClickId = -1;
    private int secondClickId = -1;
    private int[] viewId_list;
    private Bitmap[] displayedImages = new Bitmap[12];
    private int counter;
    private int clickCounter = 0;
    private Chronometer chronometer;
    TextView matchCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        setContentView(R.layout.activity_game);
        matchCounter = findViewById(R.id.matchCounter);
        getPickedImages();
        this.pickedImages.addAll(this.pickedImages);
        this.coveredImages = this.pickedImages;
        Collections.shuffle(coveredImages);
        for (int i = 0; i < 12; i++) {
            bitmapArray[i] = coveredImages.get(i);
        }
        viewId_list = new int[]{
                R.id.img00, R.id.img01, R.id.img02,
                R.id.img10, R.id.img11, R.id.img12,
                R.id.img20, R.id.img21, R.id.img22,
                R.id.img30, R.id.img31, R.id.img32,
        };
        Bitmap coveredPic = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder5);
        for (int i = 0; i < 12; i++) {
            displayedImages[i] = coveredPic;
        }

        loadImages();

        MediaPlayer mpsuccess = MediaPlayer.create(this, R.raw.gamecomplete);
        for (int j = 0; j < 12; j++) {
            ImageView v = (ImageView) findViewById(viewId_list[j]);
            v.setImageBitmap(displayedImages[j]);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (firstClickId != -1 && firstClickId != v.getId()) {
                        for (int l = 0; l < 12; l++) {
                            if (viewId_list[l] == v.getId()) {
                                secondClickId = l;
                                displayedImages[secondClickId] = coveredImages.get(secondClickId);
                                loadImages();
                                break;
                            }
                        }
                        if (bitmapArray[firstClickId] == bitmapArray[secondClickId]) {
                            if (counter < 6)
                                mpsuccess.start();
                            v.setEnabled(false);
                            counter++;
                            matchCounter.setText(counter + " of 6 matches");
                            if (counter == 6) {
                                mpsuccess.release();
                                chronometer.stop();
                                endGame();
                            }
                        } else {
                            ImageView v1 = findViewById(viewId_list[firstClickId]);
                            v1.setEnabled(true);
                            clickCounter++;
                            displayedImages[firstClickId] = coveredPic;
                            displayedImages[secondClickId] = coveredPic;
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    loadImages();
                                }
                            }, 1000);
                        }
                        firstClickId = -1;
                        secondClickId = -1;
                    } else {
                        for (int k = 0; k < 12; k++) {
                            if (viewId_list[k] == v.getId()) {
                                firstClickId = k;
                                v.setEnabled(false);
                                if (clickCounter == 0) {
                                    chronometer = (Chronometer) findViewById(R.id.timer);
                                    chronometer.start();
                                    chronometer.setBase(SystemClock.elapsedRealtime());// reset
                                }
                                clickCounter++;
                                displayedImages[firstClickId] = coveredImages.get(firstClickId);
                                loadImages();
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    private void loadImages() {
        for (int i = 0; i < 12; i++) {
            ImageView v = (ImageView) findViewById(viewId_list[i]);
            v.setImageBitmap(displayedImages[i]);
        }
    }

    private void getPickedImages() {
        for (int i = 0; i < 6; i++) {
            String name = "image" + i;
            FileInputStream fileInputStream;
            Bitmap bitmap = null;
            try {
                fileInputStream = getApplicationContext().openFileInput(name);
                bitmap = BitmapFactory.decodeStream(fileInputStream);
                pickedImages.add(bitmap);
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void endGame() {
        int temp0 = Integer.parseInt(chronometer.getText().toString().split(":")[0]);
        int temp1 = Integer.parseInt(chronometer.getText().toString().split(":")[1]);
        int temp = temp0 * 60 + temp1;
        String message = String.format("%s %d %s", getString(R.string.well_done_game_complete), temp, getString(R.string.seconds));
        MediaPlayer mpcongrat = MediaPlayer.create(this, R.raw.congrats);
        mpcongrat.start();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(GameActivity.this, EndGameActivity.class);
                intent.putExtra("time", temp);
                startActivity(intent);
            }
        }, 4000);
    }


}