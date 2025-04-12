package com.example.docxpert.features.mergepdf;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.docxpert.R;
import com.example.docxpert.utils.SafUtils;

import java.util.List;

public class PdfListAdapter extends RecyclerView.Adapter<PdfListAdapter.PdfViewHolder> {
    private static final String TAG = "PdfListAdapter";
    private final Context context;
    private final List<Uri> pdfUris;

    public PdfListAdapter(Context context, List<Uri> pdfUris) {
        this.context = context;
        this.pdfUris = pdfUris;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        Uri uri = pdfUris.get(position);
        String fileName = SafUtils.getFileName(context, uri);
        holder.fileNameText.setText(fileName);
        
        holder.removeButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                pdfUris.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
                notifyItemRangeChanged(adapterPosition, getItemCount());
                
                if (context instanceof MergePdfActivity) {
                    ((MergePdfActivity) context).updateMergeButtonState();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfUris.size();
    }

    public static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText;
        ImageButton removeButton;

        PdfViewHolder(View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
} 