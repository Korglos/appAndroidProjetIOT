package fr.cpe.temperator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

// Class pour gérer le déplacement des éléments de la liste
public class DataTextItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private final DataTextAdaptater mAdapter;

    public DataTextItemTouchHelperCallback(DataTextAdaptater adapter) {
        mAdapter = adapter;
    }

    // Désactive le swipe
    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    // Active le déplacement
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    // Déplace un élément de la liste
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        mAdapter.onItemMove(fromPosition, toPosition);
        return true;
    }

    // Désactive le swipe
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // No swipe action
    }

    // Envoie un message UDP pour mettre à jour les données
    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        ((TemperatorActivity) recyclerView.getContext()).sendUdpMessage(mAdapter.onItemDragEnd(), false);
    }
}