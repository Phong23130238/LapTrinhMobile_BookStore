package com.project.bookstoreapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder> {

    private final Context context;
    private final List<CartItem> selectedItems;
    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public CheckoutAdapter(Context context, List<CartItem> selectedItems) {
        this.context = context;
        this.selectedItems = selectedItems;
    }

    @NonNull
    @Override
    public CheckoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CheckoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutViewHolder holder, int position) {
        CartItem item = selectedItems.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvAuthor.setText(item.getAuthor());
        // Hiển thị tổng tiền ở vị trí giá, thay vì hiển thị giá đơn vị
        holder.tvPrice.setText("Tổng: " + formatPrice(item.getSubtotal()));
        holder.tvQuantity.setText("x" + item.getQuantity());

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_book_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return selectedItems != null ? selectedItems.size() : 0;
    }

    private String formatPrice(long price) {
        return VND_FORMAT.format(price) + " đ";
    }

    static class CheckoutViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvAuthor, tvPrice, tvQuantity;

        CheckoutViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover     = itemView.findViewById(R.id.ivBookCover);
            tvTitle     = itemView.findViewById(R.id.tvBookTitle);
            tvAuthor    = itemView.findViewById(R.id.tvBookAuthor);
            tvPrice     = itemView.findViewById(R.id.tvBookPrice);
            tvQuantity  = itemView.findViewById(R.id.tvQuantity);
            
            // Ẩn các nút không cần thiết ở trang checkout
            View cbSelect = itemView.findViewById(R.id.cbSelectItem);
            if(cbSelect != null) cbSelect.setVisibility(View.GONE);
            
            View btnIncrease = itemView.findViewById(R.id.btnIncrease);
            if(btnIncrease != null) btnIncrease.setVisibility(View.GONE);
            
            View btnDecrease = itemView.findViewById(R.id.btnDecrease);
            if(btnDecrease != null) btnDecrease.setVisibility(View.GONE);
            
            View tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            if(tvOriginalPrice != null) tvOriginalPrice.setVisibility(View.GONE);
        }
    }
}
