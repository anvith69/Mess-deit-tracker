package com.example.miniproject.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miniproject.R;
import com.example.miniproject.data.Food;
import com.example.miniproject.util.NutritionCalc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shows the foods of the selected meal in a RecyclerView. */
public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.Holder> {

    public interface Listener {
        void onOpen(Food food);
        void onQuickLog(Food food);
        void onRemove(Food food);
        void onLongPress(Food food);
    }

    private final List<Food> items = new ArrayList<>();
    private final Map<Long, Integer> loggedCounts = new java.util.HashMap<>();
    private final Listener listener;

    public FoodAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Food> newItems, Map<Long, Integer> counts) {
        items.clear();
        items.addAll(newItems);
        loggedCounts.clear();
        loggedCounts.putAll(counts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Food f = items.get(position);
        h.name.setText(f.custom ? f.name + "  (custom)" : f.name);
        h.meta.setText(f.calories + " kcal  ·  " + NutritionCalc.grams(f.protein) + " protein");
        h.itemView.setOnClickListener(v -> listener.onOpen(f));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(f);
            return true;
        });
        h.quickLog.setOnClickListener(v -> listener.onQuickLog(f));

        // "Remove" and the "Logged" badge only appear once this food is logged today.
        Integer count = loggedCounts.get(f.id);
        boolean logged = count != null && count > 0;
        h.remove.setVisibility(logged ? View.VISIBLE : View.GONE);
        h.logged.setVisibility(logged ? View.VISIBLE : View.GONE);
        if (logged) h.logged.setText("✓ Logged today ×" + count);
        h.remove.setOnClickListener(v -> listener.onRemove(f));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView meta;
        final Button quickLog;
        final Button remove;
        final TextView logged;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.foodName);
            meta = itemView.findViewById(R.id.foodMeta);
            quickLog = itemView.findViewById(R.id.quickLog);
            remove = itemView.findViewById(R.id.removeLog);
            logged = itemView.findViewById(R.id.foodLogged);
        }
    }
}
