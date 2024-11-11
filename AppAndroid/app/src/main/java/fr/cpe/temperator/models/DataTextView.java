package fr.cpe.temperator.models;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import fr.cpe.temperator.R;
import lombok.Getter;
import lombok.Setter;

// Class pour gérer les éléments de la liste
@Setter
@Getter
public class DataTextView extends RecyclerView.ViewHolder {
    private TextView textView;

    public DataTextView(View view) {
        super(view);
        this.setTextView(view.findViewById(R.id.item_text));
    }

}
