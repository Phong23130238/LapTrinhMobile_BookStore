package com.project.bookstoreapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.StockLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockLogAdapter extends RecyclerView.Adapter<StockLogAdapter.VH> {

    private List<StockLog> logList;

    public StockLogAdapter(List<StockLog> logList) {
        this.logList = logList;
    }

    public void updateList(List<StockLog> newList) {
        this.logList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        StockLog log = logList.get(position);

        // Icon và màu delta
        int delta = log.getDelta();
        if (delta > 0) {
            h.tvDeltaIcon.setText("+");
            h.tvDeltaIcon.setBackgroundColor(0xFF047857);
            h.tvDelta.setText("+" + delta);
            h.tvDelta.setTextColor(0xFF047857);
        } else {
            h.tvDeltaIcon.setText("−");
            h.tvDeltaIcon.setBackgroundColor(0xFFBE123C);
            h.tvDelta.setText(String.valueOf(delta));
            h.tvDelta.setTextColor(0xFFBE123C);
        }

        // Tên sách
        h.tvBookTitle.setText(log.getBookTitle() != null ? log.getBookTitle() : "—");

        // Nguồn gốc
        h.tvSource.setText(log.getSourceDisplay() != null ? log.getSourceDisplay() : "—");

        // Thời gian
        h.tvTime.setText(formatDateTime(log.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    private String formatDateTime(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        String[] fmts = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };
        for (String fmt : fmts) {
            try {
                Date d = new SimpleDateFormat(fmt, Locale.getDefault()).parse(iso);
                if (d != null)
                    return new SimpleDateFormat("HH:mm  dd/MM/yyyy", Locale.getDefault()).format(d);
            } catch (ParseException ignored) {}
        }
        return iso.length() >= 10 ? iso.substring(0, 10) : iso;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDeltaIcon, tvBookTitle, tvSource, tvDelta, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDeltaIcon = itemView.findViewById(R.id.tvLogDeltaIcon);
            tvBookTitle = itemView.findViewById(R.id.tvLogBookTitle);
            tvSource    = itemView.findViewById(R.id.tvLogSource);
            tvDelta     = itemView.findViewById(R.id.tvLogDelta);
            tvTime      = itemView.findViewById(R.id.tvLogTime);
        }
    }
}
