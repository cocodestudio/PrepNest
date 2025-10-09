package com.cocode.prepnest;

import android.content.Context;
import android.net.Uri;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {

    private final Context context;
    private final List<Uri> imageURIs;
    private final SparseArray<PhotoView> photoViewMap = new SparseArray<>();

    public ImagePagerAdapter(Context context, List<Uri> imageURIs) {
        this.context = context;
        this.imageURIs = imageURIs;
    }

    public PhotoView getPhotoViewAt(int position) {
        return photoViewMap.get(position);
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_pager_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Glide.with(context).load(imageURIs.get(position)).into(holder.photoView);
        holder.photoView.setMinimumScale(1.0f);
        holder.photoView.setMediumScale(2.5f);
        holder.photoView.setMaximumScale(8.0f);
        holder.photoView.setAllowParentInterceptOnEdge(true); // Enables smooth swipe when zoomed out

        photoViewMap.put(position, holder.photoView);
    }

    @Override
    public int getItemCount() {
        return imageURIs.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }
}