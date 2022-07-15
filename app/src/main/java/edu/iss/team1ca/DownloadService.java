package edu.iss.team1ca;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadService extends Service {

    protected String providedURL;
    protected String downloadDir; // Get from intent
    protected Thread downloadThread; // Subprocess link
    protected List<String> imgURLs; // Image URLs
    protected List<String> imgPaths; // Image URLs

    protected int processID;

    public DownloadService() {
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {

        imgURLs = new ArrayList<>();
        imgPaths = new ArrayList<>();

        String action = intent.getAction();
        if (action != null) {
            providedURL = intent.getStringExtra("url");
            downloadDir = intent.getStringExtra("downloadDir");
            processID = intent.getIntExtra("processID", 0);

            if (processID != 0 && action.equals("download")) {
                // Cleanup comes first
                emptyDirectory();
                // Start downloading in new thread
                startDownloadThread();
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        processID = -1;
        super.onDestroy();
    }

    protected void startDownloadThread() {
        int currentProcessId = processID;
        downloadThread = new Thread(() -> {
            Boolean status = false;
            Intent intent = new Intent();
            intent.putExtra("processID", processID);

            // Get page from URL
            String pageData = readFromURL(providedURL);
            if (pageData.length() < 200) {
                intent.setAction("downloaded_failed");
                intent.putExtra("errorMsg", "Unable to download the page");
                sendBroadcast(intent);
                return;
            } else {
                intent.setAction("downloaded_page");
                sendBroadcast(intent);
            }

            // Process page and find image links
            getImageURLs(pageData);

            if (imgURLs.size() >= 20) {
                // Let MainActivity know everything is ok for downloading
                intent.setAction("downloading_20_files");
                sendBroadcast(intent);

                // Fill up the list of filenames array for download
                updateFileNames();

                // Download first 20 images
                status = downloadFirstTwentyImages(currentProcessId);
                if (!status) {
                    intent.setAction("downloaded_failed");
                    intent.putExtra("errorMsg", "Something went wrong while downloading the images.");
                    sendBroadcast(intent);
                }
            } else {
                // Let MainActivity know something went wrong
                intent.setAction("downloaded_failed");
                intent.putExtra("errorMsg", "Unable to find 20 images on selected website.");
                sendBroadcast(intent);
            }

        });
        downloadThread.start();
    }

    protected void updateFileNames() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                String tmp = "img" + String.valueOf(i) + String.valueOf(j) + ".jpg";
                imgPaths.add(tmp);
            }
        }
    }

    protected void getImageURLs(@NonNull String pageData) {

        List<String> tempImgURLs = new ArrayList<>();

        // Ensure page is not empty or error page
        if (pageData.length() > 100) {
            // Look for img tags
            String imgRegex = "<img src=\"http(.*?)\\.jpg\"";
            //String imgRegex = "<img src=\"http(.*?)\"";
            Matcher m = Pattern.compile(imgRegex).matcher(pageData);
            while (m.find()) {
                // Rebuild the image url
                String tmp = "http" + m.group(1) + ".jpg";
                //String tmp = "http" + m.group(1);
                tempImgURLs.add(tmp);
            }
            // Update image URL List
            imgURLs = tempImgURLs;
        }
    }

    protected String readFromURL(String fileUrl) {
        StringBuilder pageData = new StringBuilder();
        try {
            // Open URL for reading
            URL u = new URL(fileUrl);
            URLConnection conn = u.openConnection();
            BufferedReader bRead = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String strLine;
            // Read from URL and save to string
            while ((strLine = bRead.readLine()) != null) {
                pageData.append(strLine);
            }

            bRead.close();
            return pageData.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    protected Boolean downloadFirstTwentyImages(int currentProcessId) {
        Intent intent = new Intent();
        intent.putExtra("processID", processID);

        // Download first 20 images
        for (int k = 0; k < 20; k++) {
            String fileURL = imgURLs.get(k);
            String pathName = imgPaths.get(k);

            // Path to save the downloaded file
            File fullPath = new File(downloadDir, pathName);

            // Call downloader
            Boolean status = downloadFiles(fileURL, fullPath, currentProcessId);
            // Something went wrong
            if (!status) {
                return false;
            }

            // Notify main activity of download progress
            intent.setAction("downloaded_file");
            intent.putExtra("imgPath", fullPath.getAbsolutePath());
            intent.putExtra("imgCount", k);
            sendBroadcast(intent);
        }
        // Notify main activity of download completion status
        intent.setAction("downloaded_all_file");
        sendBroadcast(intent);

        return true;
    }

    protected void emptyDirectory() {
        // Remove all files in the directory
        File dir = new File(downloadDir);
        if (dir.isDirectory())
            for (File child : dir.listFiles())
                child.delete();
    }

    protected Boolean downloadFiles(String fileUrl, @NonNull File mTargetFile, int currentProcessId) {
        try {
            // Open URL stream
            URL u = new URL(fileUrl);
            InputStream is = u.openStream();

            // Open URL for reading
            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[1024];
            int length;

            // Create the directory for downloads
            File parent = mTargetFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }

            // Open file for writing
            FileOutputStream fos = new FileOutputStream(mTargetFile);

            // Read from URL and write to file
            while ((length = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
                // Check for interrupt
                if (currentProcessId != processID) {
                    // Close open files and connections
                    fos.close();
                    dis.close();
                    is.close();
                    return false;
                }
            }

            // Close open files and connections
            fos.close();
            dis.close();
            is.close();
            return true;

        } catch (MalformedURLException mue) {
            Log.e("SYNC getUpdate", "malformed url error", mue);
            return false;
        } catch (IOException ioe) {
            Log.e("SYNC getUpdate", "io error", ioe);
            return false;
        } catch (SecurityException se) {
            Log.e("SYNC getUpdate", "security error", se);
            return false;
        }
    }
}