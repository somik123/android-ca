package edu.iss.team1ca;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.somik.team1ca.R;

public class EndGameActivity extends AppCompatActivity {
    private WebView congratsBanner;
    private AnimationDrawable congratulationsStartAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_game);


        //Initialise congratulations animation
        congratsBanner = findViewById(R.id.congratulations);

        Intent intent = getIntent();
        int time = intent.getIntExtra("time", 1000000);
        System.out.println(time);
        TextView text = findViewById(R.id.timeMessage);
        if (text != null) {
            text.setText(getString(R.string.round_score) + String.valueOf(time));
        }
        TextView isTop = findViewById(R.id.isTopRecord);
        int currentTop = getTopScore();
        if (currentTop > time) {
            saveTopScore(time);
            isTop.setText(getString(R.string.best_score) + time);
            congratsBanner.loadUrl("https://stickers.wiki/static/stickers/cbs273_farsisticker/file_845069.gif");
        } else {
            isTop.setText(getString(R.string.best_score) + currentTop);
            congratsBanner.loadUrl("https://stickers.wiki/static/stickers/cbs273_farsisticker/file_845102.gif");
        }


        Button BackBtn = findViewById(R.id.Back);
        BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EndGameActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    protected void saveTopScore(int score) {
        // Write
        SharedPreferences pref = getSharedPreferences("user_credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("topScore", score);
        editor.apply();
    }

    protected int getTopScore() {
        // Read
        SharedPreferences pref = getSharedPreferences("user_credentials", MODE_PRIVATE);
        int score = pref.getInt("topScore", 120);
        return score;
    }

    @Override
    public void onBackPressed() {
        // Bring to MainActivity
        Intent intent = new Intent(EndGameActivity.this, MainActivity.class);
        startActivity(intent);
    }
}