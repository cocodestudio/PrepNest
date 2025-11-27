package com.cocode.prepnest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.cocode.prepnest.databinding.SheetSingleItemSelectBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class DetailsListAdapter extends BaseAdapter {

    private final ArrayList<HashMap<String, Object>> list;
    private final LayoutInflater inflater;
    private final String selectedId;

    public DetailsListAdapter(Context context, ArrayList<HashMap<String, Object>> list, String selectedId) {
        this.list = list;
        this.inflater = LayoutInflater.from(context);
        this.selectedId = selectedId;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            SheetSingleItemSelectBinding binding = SheetSingleItemSelectBinding.inflate(
                    inflater,
                    parent,
                    false
            );

            convertView = binding.getRoot();
            holder = new ViewHolder(binding);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.binding.text.setText(Objects.requireNonNull(list.get(position).get("text")).toString());

        if (selectedId.equals(Objects.requireNonNull(list.get(position).get("id")).toString())) {
            holder.binding.selectedCircle.setVisibility(View.VISIBLE);
        } else {
            holder.binding.selectedCircle.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        SheetSingleItemSelectBinding binding;

        ViewHolder(SheetSingleItemSelectBinding binding) {
            this.binding = binding;
        }
    }
}
