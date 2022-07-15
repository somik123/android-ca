package edu.iss.team1ca;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.somik.team1ca.R;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    protected String downloadDir;
    protected BroadcastReceiver receiver;
    protected int processID;

    private HashMap<Integer, ImageView> selected_position = new HashMap<Integer, ImageView>();

    EditText txtUrl;
    Button btnSubmit;
    TextView txtProgress;

    private int curr = 0; // Current image box to update from service

    private ImageView progressBar;
    private AnimationDrawable loadingBarStartAnimation;
    private GridView gridview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Define where to download files to
        downloadDir = getExternalFilesDir("").toString() + "/ImageDownloads/";

        // Get the receivers up and running
        instantiateBroadcastReceiver();
        registerBroadcastReceiver();

        txtUrl = findViewById(R.id.txtUrl);
        btnSubmit = findViewById(R.id.btnSubmit);
        txtProgress = findViewById(R.id.txtProgress);

        // Initialize the gridview
        gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this, null));
        gridview.setOnItemClickListener(this);
        gridview.setEnabled(false);

        // Initialise progress bar animation
        progressBar = (ImageView) findViewById(R.id.progress_bar);
        progressBar.setBackgroundResource(R.drawable.progress_bar_start);
        loadingBarStartAnimation = (AnimationDrawable) progressBar.getBackground();
        loadingBarStartAnimation.start();

        // User clicked submit button
        btnSubmit.setOnClickListener(view -> {
            startImageDownloadService();
        });
        // User pressed enter
        txtUrl.setOnEditorActionListener((textView, i, keyEvent) -> {
            if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                startImageDownloadService();
            }
            return false;
        });

    }


    protected void startImageDownloadService() {
        // Hide keyboard
        hideSoftKeyboard();

        // Create new intent
        Intent intent = new Intent(MainActivity.this, DownloadService.class);

        // Stop the running servive (if any)
        stopService(intent);

        // Generate new process ID
        Random rand = new Random();
        processID = rand.nextInt(999999);

        // Update user
        txtProgress.setText(R.string.fetching_new_images);

        // Reset image position
        curr = 0;

        //clear images and disable gridview
        ImageAdapter adapter = (ImageAdapter) gridview.getAdapter();
        adapter.addAll(null);
        //clear selected image
        selected_position.clear();
        gridview.setEnabled(false);

        // Reset progress bar
        progressBar.setBackgroundResource(R.drawable.progress_bar_start);
        loadingBarStartAnimation = (AnimationDrawable) progressBar.getBackground();
        loadingBarStartAnimation.start();

        // Get user provided url
        String url = txtUrl.getText().toString();

        // Initiate the service
        intent.setAction("download");
        intent.putExtra("processID", processID);
        intent.putExtra("url", url);
        intent.putExtra("downloadDir", downloadDir);

        // Update user
        txtProgress.setText(R.string.starting_service);

        // Start the service
        startService(intent);
    }


    protected void instantiateBroadcastReceiver() {
        List<Bitmap> images = new ArrayList<Bitmap>();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int serviceProcessId = intent.getIntExtra("processID", 0);
                // Check if the broadcast is from latest service process
                if (serviceProcessId != processID) return;

                switch (action) {
                    case "downloaded_file":
                        String imgPath = intent.getStringExtra("imgPath");
                        if (curr < 20) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
                            images.add(bitmap);
                        }
                        curr++;
                        ImageAdapter adapter = (ImageAdapter) gridview.getAdapter();
                        adapter.addAll(images);
                        animateProgressBar();

                        if (curr < 20)
                            txtProgress.setText(String.format("%s %d %s", getString(R.string.downloading), curr, getString(R.string.of_20_images)));
                        else
                            txtProgress.setText(R.string.all_20_images_downloaded);
                        break;
                    case "downloaded_all_file":
                        // Re-enable gridview after all downloads are complete
                        txtProgress.setText(R.string.all_20_images_downloaded);
                        images.clear();
                        gridview.setEnabled(true);
                        MediaPlayer meow = MediaPlayer.create(MainActivity.this, R.raw.catmeow);
                        meow.start();
                        break;
                    case "downloading_20_files":
                        images.clear();
                        txtProgress.setText(R.string.images_found_downloading);
                        break;
                    case "downloaded_failed":
                        // calling a method in Activity
                        String errorMsg = intent.getStringExtra("errorMsg");
                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
    }

    protected void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("downloading_20_files");
        filter.addAction("downloaded_file");
        filter.addAction("downloaded_all_file");
        filter.addAction("downloaded_failed");
        registerReceiver(receiver, filter);
    }

    @Override
    public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
        // View v is the selected row
        ImageView img = (ImageView) v;
        if (selected_position.get(pos) == null) {
            img.setBackgroundColor(Color.RED);
            selected_position.put(pos, img);
            txtProgress.setText(selected_position.size() + " Item Selected");
        } else {
            img.setBackgroundColor(Color.TRANSPARENT);
            selected_position.remove(pos);
            txtProgress.setText(selected_position.size() + " Item Selected");
        }

        if (selected_position.size() == 6) {
            saveImgs();
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        }

    }

    private void saveImgs() {
        AtomicInteger i = new AtomicInteger();
        selected_position.forEach((key, value) -> {
            String imageName = "image" + i;
            FileOutputStream fileOutputStream;
            try {
                fileOutputStream = getApplicationContext().openFileOutput(imageName, Context.MODE_PRIVATE);
                Bitmap bitmap = ((BitmapDrawable) value.getDrawable()).getBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            i.getAndIncrement();
        });
    }

    protected void animateProgressBar() {
        if (curr % 2 != 0) return;
        int progress = (curr / 2) - 1;

        boolean update;
        switch (progress) {
            case 1:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_1);
                update = true;
                break;
            case 2:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_2);
                update = true;
                break;
            case 3:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_3);
                update = true;
                break;
            case 4:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_4);
                update = true;
                break;
            case 5:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_5);
                update = true;
                break;
            case 6:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_6);
                update = true;
                break;
            case 7:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_7);
                update = true;
                break;
            case 8:
                progressBar.setBackgroundResource(R.drawable.progress_bar_moving_8);
                update = true;
                break;
            case 9:
                progressBar.setBackgroundResource(R.drawable.progress_bar_finish);
                update = true;
                break;
            default:
                update = false;
        }
        if (update) {
            loadingBarStartAnimation = (AnimationDrawable) progressBar.getBackground();
            loadingBarStartAnimation.start();
        }
    }

    // Hides the soft keyboard
    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // Shows the soft keyboard
    public void showSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.showSoftInput(view, 0);
    }
}