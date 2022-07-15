package edu.iss.team1ca;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import org.somik.team1ca.R;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private List<Bitmap> images;

    // Constructor
    public ImageAdapter(Context c, List<Bitmap> images) {
        mContext = c;
        this.images = images;
    }

    public int getCount() {
        return 20;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public void addAll(List<Bitmap> images) {
        if (images != null) {
            this.images = new ArrayList<Bitmap>();
            this.images.addAll(images);
        } else {
            this.images = null;
        }
        notifyDataSetChanged();
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {

        ImageView imageView;
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int screenHeight = metrics.heightPixels - 50;
        int screenWidth = metrics.widthPixels - 50;

        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(screenWidth / 4, screenHeight / 8));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(5, 5, 5, 5);
        } else {
            imageView = (ImageView) convertView;
        }

        if (this.images == null) {
            imageView.setImageResource(R.drawable.placeholder4);
            imageView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            if (position < images.size()) {
                imageView.setImageBitmap(images.get(position));
                imageView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
        return imageView;
    }

}
