package com.namit.scantastic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.namit.scantastic.models.CapturedImage;

import java.util.Stack;

class PreviewImageAdapter extends RecyclerView.Adapter<PreviewImageAdapter.ViewHolder>  {

    private Stack<CapturedImage> images;
    private Context context;
    private ImageAdapterListener listener;

    public PreviewImageAdapter(Context context, Stack<CapturedImage> capturedImages, ImageAdapterListener clickListener) {
        this.context = context;
        this.images = capturedImages;
        this.listener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_img_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CapturedImage image = images.get(position);
        if(image.getCroppedBitmap() != null){
            Glide.with(context)
                    .load(image.getCroppedBitmap())
                    .into(holder.imageView);
        }else {
            Glide.with(context)
                    .load(image.getOriginalUri())
                    .into(holder.imageView);
        }
        holder.imageView.setOnClickListener(view -> listener.imageClicked(image, position));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }



    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_preview);
        }
    }
}
