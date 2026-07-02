package com.project.bookstoreapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
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
        // Khai báo biến item ở đây và sử dụng thống nhất
        final CartItem currentItem = cartItems.get(position);

        // Tên & tác giả
        holder.tvTitle.setText(currentItem.getTitle());
        holder.tvAuthor.setText(currentItem.getAuthor());

        // Giá bán
        holder.tvPrice.setText(formatPrice(currentItem.getPrice()));

        // Giá gốc
        if (currentItem.getOriginalPrice() > currentItem.getPrice()) {
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(formatPrice(currentItem.getOriginalPrice()));
            holder.tvOriginalPrice.setPaintFlags(
                    holder.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            holder.tvOriginalPrice.setVisibility(View.GONE);
        }

        // Số lượng
        holder.tvQuantity.setText(String.valueOf(currentItem.getQuantity()));

        // Ảnh bìa
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            Glide.with(context).load(currentItem.getImageUrl())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        // Checkbox
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(currentItem.isSelected());
        holder.cbSelect.setOnCheckedChangeListener((btn, isChecked) -> {
            currentItem.setSelected(isChecked);
            if (listener != null) listener.onCartChanged();
        });

        // Nút giảm
        holder.btnDecrease.setOnClickListener(v -> {
            if (currentItem.getQuantity() > 1) {
                int newQty = currentItem.getQuantity() - 1;
                currentItem.setQuantity(newQty);
                holder.tvQuantity.setText(String.valueOf(newQty));
                if (listener != null) {
                    listener.onQuantityChanged(currentItem, newQty);
                    if (currentItem.isSelected()) listener.onCartChanged();
                }
            }
        });

        // Nút tăng
        holder.btnIncrease.setOnClickListener(v -> {
            int newQty = currentItem.getQuantity() + 1;
            currentItem.setQuantity(newQty);
            holder.tvQuantity.setText(String.valueOf(newQty));
            if (listener != null) {
                listener.onQuantityChanged(currentItem, newQty);
                if (currentItem.isSelected()) listener.onCartChanged();
            }
        });

        // Nút Xóa (Đã fix lỗi IndexOutOfBounds)
        holder.btnDeleteCartItem.setOnClickListener(v -> {
            String documentId = currentItem.getCartItemId();
            if (documentId != null && !documentId.isEmpty()) {
                FirebaseFirestore.getInstance().collection("carts")
                        .document(documentId)
                        .delete()
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(context, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        ImageButton btnDecrease, btnIncrease, btnDeleteCartItem; // Khai báo thêm btnDeleteCartItem

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect           = itemView.findViewById(R.id.cbSelectItem);
            ivCover            = itemView.findViewById(R.id.ivBookCover);
            tvTitle            = itemView.findViewById(R.id.tvBookTitle);
            tvAuthor           = itemView.findViewById(R.id.tvBookAuthor);
            tvPrice            = itemView.findViewById(R.id.tvBookPrice);
            tvOriginalPrice    = itemView.findViewById(R.id.tvOriginalPrice);
            tvQuantity         = itemView.findViewById(R.id.tvQuantity);
            btnDecrease        = itemView.findViewById(R.id.btnDecrease);
            btnIncrease        = itemView.findViewById(R.id.btnIncrease);
            btnDeleteCartItem  = itemView.findViewById(R.id.btnDeleteCartItem); // Ánh xạ nút xóa
        }
    }
}