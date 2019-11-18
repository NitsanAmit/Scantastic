package com.namit.scantastic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.namit.scantastic.models.Document;

import java.util.List;

class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.ViewHolder>  {

    private final List<Document> documents;
    private Context context;

    public DocumentsAdapter(Context context, List<Document> documents) {
        this.context = context;
        this.documents = documents;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        return new DocumentsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document document = documents.get(position);
        if(document.getPages().isEmpty()){
            return;
        }
        if(document.getTitle() != null){
            holder.title.setText(document.getTitle());
        }else{
            holder.title.setText(String.format(context.getString(R.string.txt_doc), position));
        }
        if(document.getDate() != null){
            holder.date.setText(document.getDate());
        }else{
            holder.date.setText("");
        }
        if(document.getTopic() != null){
            holder.topic.setText(document.getTopic());
        }else{
            holder.topic.setText("");
        }
        Glide.with(context)
                .load(document.getPages().get(0).getBitmap())
                .into(holder.imageView);

    }

    @Override
    public int getItemCount() {
        return documents.size();
    }



    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView title;
        TextView topic;
        TextView date;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            topic = itemView.findViewById(R.id.txt_topic);
            date = itemView.findViewById(R.id.txt_date);
            imageView = itemView.findViewById(R.id.img_document);
        }

    }
}
