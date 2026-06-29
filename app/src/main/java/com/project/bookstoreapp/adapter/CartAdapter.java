package com.project.bookstoreapp.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.CartItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface CartActionListener {
        void onCartChanged();
        void onQuantityChanged(CartItem item, int newQuantity);
    }

    private final Context context;
    private final List<CartItem> cartItems;
    private final CartActionListener listener;
    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public CartAdapter(Context context, List<CartItem> cartItems, CartActionListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        // Tên & tác giả
        holder.tvTitle.setText(item.getTitle());
        holder.tvAuthor.setText(item.getAuthor());

        // Giá bán
        holder.tvPrice.setText(formatPrice(item.getPrice()));

        // Giá gốc (chỉ hiện khi có giảm giá)
        if (item.getOriginalPrice() > item.getPrice()) {
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(formatPrice(item.getOriginalPrice()));
            holder.tvOriginalPrice.setPaintFlags(
                    holder.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            holder.tvOriginalPrice.setVisibility(View.GONE);
        }

        // Số lượng
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        // Ảnh bìa bằng Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        // Trạng thái checkbox (ngăn trigger listener khi bind)
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(item.isSelected());
        holder.cbSelect.setOnCheckedChangeListener((btn, isChecked) -> {
            item.setSelected(isChecked);
            if (listener != null) listener.onCartChanged();
        });

        // Nút giảm số lượng
        holder.btnDecrease.setOnClickListener(v -> {
            int current = item.getQuantity();
            if (current > 1) {
                int newQty = current - 1;
                item.setQuantity(newQty);
                holder.tvQuantity.setText(String.valueOf(newQty));
                if (listener != null) {
                    listener.onQuantityChanged(item, newQty);
                    if (item.isSelected()) listener.onCartChanged();
                }
            }
        });

        // Nút tăng số lượng
        holder.btnIncrease.setOnClickListener(v -> {
            int newQty = item.getQuantity() + 1;
            item.setQuantity(newQty);
            holder.tvQuantity.setText(String.valueOf(newQty));
            if (listener != null) {
                listener.onQuantityChanged(item, newQty);
                if (item.isSelected()) listener.onCartChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public long getSelectedTotal() {
        long total = 0;
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                total += item.getSubtotal();
            }
        }
        return total;
    }

    public java.util.ArrayList<CartItem> getSelectedItems() {
        java.util.ArrayList<CartItem> selected = new java.util.ArrayList<>();
        for (CartItem item : cartItems) {
            if (item.isSelected()) selected.add(item);
        }
        return selected;
    }

    private String formatPrice(long price) {
        return VND_FORMAT.format(price) + " đ";
    }

    // ---- ViewHolder ----
    static class CartViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivCover;
        TextView tvTitle, tvAuthor, tvPrice, tvOriginalPrice, tvQuantity;
        ImageButton btnDecrease, btnIncrease;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect       = itemView.findViewById(R.id.cbSelectItem);
            ivCover        = itemView.findViewById(R.id.ivBookCover);
            tvTitle        = itemView.findViewById(R.id.tvBookTitle);
            tvAuthor       = itemView.findViewById(R.id.tvBookAuthor);
            tvPrice        = itemView.findViewById(R.id.tvBookPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvQuantity     = itemView.findViewById(R.id.tvQuantity);
            btnDecrease    = itemView.findViewById(R.id.btnDecrease);
            btnIncrease    = itemView.findViewById(R.id.btnIncrease);
        }
    }
}