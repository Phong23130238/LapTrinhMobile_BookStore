package com.project.bookstoreapp.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Book;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.VH> {

    private static final int THRESHOLD_LOW   = 5;  // Sắp hết: < 5
    private static final int THRESHOLD_OK    = 10; // Còn hàng: >= 10

    private List<Book> bookList;

    public InventoryAdapter(List<Book> bookList) {
        this.bookList = bookList;
    }

    public void updateList(List<Book> newList) {
        this.bookList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_stock, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Book book = bookList.get(position);

        // Ảnh bìa thu nhỏ
        if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(book.getImageUrl())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .centerCrop()
                    .into(h.ivCover);
        } else {
            h.ivCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        // Tên & ID
        h.tvTitle.setText(book.getTitle() != null ? book.getTitle() : "—");
        String bid = book.getBookId();
        h.tvBookId.setText(bid != null ? "#" + bid.substring(0, Math.min(6, bid.length())) : "#—");

        // Số lượng tồn
        int stock = book.getStock();
        h.tvStock.setText(String.valueOf(stock));

        // Trạng thái & màu nền hàng
        if (stock == 0) {
            h.tvStatus.setText("Hết hàng");
            h.tvStatus.setTextColor(0xFFBE123C);
            h.tvStatus.setBackgroundColor(0xFFFFE4E6);
            h.tvStock.setTextColor(0xFFBE123C);
            h.itemView.setBackgroundColor(0xFFFFF1F2); // Nền đỏ nhạt toàn hàng
        } else if (stock < THRESHOLD_LOW) {
            h.tvStatus.setText("Sắp hết");
            h.tvStatus.setTextColor(0xFFB45309);
            h.tvStatus.setBackgroundColor(0xFFFEF3C7);
            h.tvStock.setTextColor(0xFFB45309);
            h.itemView.setBackgroundColor(0xFFFFFBEB); // Nền vàng nhạt
        } else {
            h.tvStatus.setText("Còn hàng");
            h.tvStatus.setTextColor(0xFF047857);
            h.tvStatus.setBackgroundColor(0xFFD1FAE5);
            h.tvStock.setTextColor(0xFF047857);
            h.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return bookList != null ? bookList.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView  tvBookId, tvTitle, tvStock, tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            ivCover   = itemView.findViewById(R.id.ivInventoryCover);
            tvBookId  = itemView.findViewById(R.id.tvInventoryBookId);
            tvTitle   = itemView.findViewById(R.id.tvInventoryTitle);
            tvStock   = itemView.findViewById(R.id.tvInventoryStock);
            tvStatus  = itemView.findViewById(R.id.tvInventoryStatus);
        }
    }
}
