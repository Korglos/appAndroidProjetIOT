package fr.cpe.temperator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import fr.cpe.temperator.models.DataCapteur;
import fr.cpe.temperator.models.DataTextView;

// Adapter pour la liste des capteurs
public class DataTextAdaptater extends RecyclerView.Adapter<DataTextView> {

    private final List<DataCapteur> data;

    public DataTextAdaptater(List<DataCapteur> data) {
        this.data = data;
    }

    // Créé une nouvelle vue
    @NonNull
    @Override
    public DataTextView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.items_layout, parent, false);
        return new DataTextView(view);
    }

    // Remplace le contenu de la vue
    @Override
    public void onBindViewHolder(@NonNull DataTextView holder, int position) {
        holder.getTextView().setText(data.get(position).toString());
    }

    // Retourne la taille du dataset
    @Override
    public int getItemCount() {
        return data.size();
    }

    // Déplace un élément de la liste
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(data, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(data, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    // Retourne la liste des éléments après un déplacement
    public String onItemDragEnd() {
        StringBuilder result = new StringBuilder();
        for (DataCapteur dataCapteur : data) {
            result.append(dataCapteur.getId()).append(";");
        }
        return result.toString();
    }
}