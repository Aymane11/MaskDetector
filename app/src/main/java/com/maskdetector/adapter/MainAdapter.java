package com.maskdetector.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.maskdetector.R;
import com.maskdetector.database.models.City;

import java.util.ArrayList;
import java.util.List;

public class MainAdapter extends ListAdapter<City, MainAdapter.MainViewHolder> {
    private List<City> originalList = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClicked(City city);
    }

    private final OnItemClickListener clickListener;

    public MainAdapter(OnItemClickListener clickListener) {
        super(DIFF_CALLBACK);

        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_main, parent, false);
        return new MainViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }

    public void setData(@Nullable List<City> list) {
        originalList = list;
        super.submitList(list);
    }

    public void filter(String query) {
        List<City> filteredList = new ArrayList<>();
        for (City city : originalList) {
            if (city.getCity_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(city);
            }
        }
        submitList(filteredList);
    }

    static class MainViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textCount;
        private City city;

        MainViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            itemView.setOnClickListener(v -> listener.onItemClicked(city));
            textTitle = itemView.findViewById(R.id.textTitle);
            textCount = itemView.findViewById(R.id.textCount);
        }

        @SuppressLint("SetTextI18n")
        void bindTo(City city) {
            this.city = city;

            textTitle.setText(city.getCity_name());
            textCount.setText(city.getCenters().size()+"");
        }
    }

    private static final DiffUtil.ItemCallback<City> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<City>() {
                @Override
                public boolean areItemsTheSame(@NonNull City oldData,
                                               @NonNull City newData) {
                    return oldData.getId().equals(newData.getId());
                }
                @SuppressLint("DiffUtilEquals")
                @Override
                public boolean areContentsTheSame(@NonNull City oldData,
                                                  @NonNull City newData) {
                    return oldData.equals(newData);
                }
            };
}
