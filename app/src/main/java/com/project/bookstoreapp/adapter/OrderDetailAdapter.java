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
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.activity.BookDetailActivity;
import android.content.Intent;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> items;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    public OrderDetailAdapter(Context context, List<Map<String, Object>> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);

        String title = item.containsKey("title") ? String.valueOf(item.get("title")) : "Đang tải...";
        String imageUrl = item.containsKey("imageUrl") ? String.valueOf(item.get("imageUrl")) : "";
        
        double price = 0;
        if (item.containsKey("price")) {
            Object p = item.get("price");
            if (p instanceof Number) {
                price = ((Number) p).doubleValue();
            } else if (p instanceof String) {
                try { price = Double.parseDouble((String) p); } catch (Exception ignored) {}
            }
        }
        
        int quantity = 1;
        if (item.containsKey("quantity")) {
            Object q = item.get("quantity");
            if (q instanceof Number) {
                quantity = ((Number) q).intValue();
            } else if (q instanceof String) {
                try { quantity = Integer.parseInt((String) q); } catch (Exception ignored) {}
            }
        }

        holder.tvBookTitle.setText(title);
        holder.tvBookPrice.setText(formatter.format(price) + " đ");
        holder.tvBookQuantity.setText("x" + quantity);

        if (!imageUrl.isEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .into(holder.ivBookCover);
        } else {
            holder.ivBookCover.setImageResource(R.drawable.ic_book_placeholder);
        }

        // Fetch from Firestore if title or imageUrl is missing
        if (item.containsKey("bookId") && (!item.containsKey("title") || !item.containsKey("imageUrl") || imageUrl.isEmpty())) {
            String bookId = String.valueOf(item.get("bookId"));
            FirebaseFirestore.getInstance().collection("books").document(bookId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fetchedTitle = documentSnapshot.getString("title");
                        String fetchedImage = documentSnapshot.getString("imageUrl");
                        
                        boolean updated = false;
                        if (fetchedTitle != null) {
                            item.put("title", fetchedTitle);
                            holder.tvBookTitle.setText(fetchedTitle);
                            updated = true;
                        }
                        if (fetchedImage != null) {
                            item.put("imageUrl", fetchedImage);
                            Glide.with(context)
                                .load(fetchedImage)
                                .placeholder(R.drawable.ic_book_placeholder)
                                .error(R.drawable.ic_book_placeholder)
                                .into(holder.ivBookCover);
                            updated = true;
                        }
                        // Don't call notifyItemChanged here to avoid infinite loops if it recycles while loading,
                        // updating the view holder directly is fine for this simple case.
                    }
                });
        }

        // Click to open BookDetailActivity
        holder.itemView.setOnClickListener(v -> {
            if (item.containsKey("bookId")) {
                String bookId = String.valueOf(item.get("bookId"));
                Intent intent = new Intent(context, BookDetailActivity.class);
                intent.putExtra("BOOK_ID", bookId);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBookCover;
        TextView tvBookTitle, tvBookPrice, tvBookQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBookCover = itemView.findViewById(R.id.ivBookCover);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvBookPrice = itemView.findViewById(R.id.tvBookPrice);
            tvBookQuantity = itemView.findViewById(R.id.tvBookQuantity);
        }
    }
}
