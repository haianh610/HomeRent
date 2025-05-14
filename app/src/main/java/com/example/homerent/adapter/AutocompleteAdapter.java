package com.example.homerent.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homerent.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import java.util.ArrayList;
import java.util.List;

public class AutocompleteAdapter extends RecyclerView.Adapter<AutocompleteAdapter.ViewHolder> {

    private List<AutocompletePrediction> predictions = new ArrayList<>();
    private final LayoutInflater inflater;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPredictionClick(AutocompletePrediction prediction);
    }

    public AutocompleteAdapter(Context context, OnItemClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged") // Acceptable here as list changes completely
    public void setPredictions(List<AutocompletePrediction> predictions) {
        this.predictions = (predictions != null) ? predictions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_autocomplete_suggestion, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutocompletePrediction prediction = predictions.get(position);
        holder.bind(prediction);
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPrimaryText;
        TextView tvSecondaryText;
        AutocompletePrediction currentPrediction;

        ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            tvPrimaryText = itemView.findViewById(R.id.tvPrimaryText);
            tvSecondaryText = itemView.findViewById(R.id.tvSecondaryText);

            itemView.setOnClickListener(v -> {
                if (listener != null && currentPrediction != null) {
                    listener.onPredictionClick(currentPrediction);
                }
            });
        }

        void bind(AutocompletePrediction prediction) {
            this.currentPrediction = prediction;
            // Hiển thị text chính và phụ, có thể highlight phần khớp nếu muốn
            tvPrimaryText.setText(getHighlightedText(prediction.getPrimaryText(null)));
            tvSecondaryText.setText(getHighlightedText(prediction.getSecondaryText(null)));
        }

        // Helper to bold the matched parts (optional)
        private SpannableStringBuilder getHighlightedText(SpannableString style) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            // The Places SDK provides the CharacterStyle directly with offsets
            // For simplicity here, we just return the styled text if available
            // A more complex implementation would parse the offsets.
            if (style instanceof Spannable) {
                builder.append((Spannable) style);
            } else if (style != null) {
                // Fallback or handle other CharacterStyle types if necessary
                // This part might need adjustment based on how Places SDK returns styled text
                // Usually, the full text is obtained via prediction.getFullText(null);
                // And matched parts via prediction.getPrimaryTextMatchedSubstrings() etc.
                // For now, let's just append the style's string representation if possible.
                Log.w("AutocompleteAdapter", "Unhandled CharacterStyle type: " + style.getClass().getName());
                // Just append the plain text for now
                builder.append(currentPrediction.getPrimaryText(null)); // Or getSecondaryText
            }

            // --- Simpler approach: Just show the text without highlighting ---
            // tvPrimaryText.setText(prediction.getPrimaryText(null));
            // tvSecondaryText.setText(prediction.getSecondaryText(null));
            // ---------------------------------------------------------------

            return builder; // Return empty or handle differently if style is null/not Spannable
        }
        // Overload/Alternative simpler bind without highlighting
        void bindSimple(AutocompletePrediction prediction) {
            this.currentPrediction = prediction;
            tvPrimaryText.setText(prediction.getPrimaryText(null));
            tvSecondaryText.setText(prediction.getSecondaryText(null));
        }

    }
}