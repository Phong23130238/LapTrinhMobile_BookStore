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
import com.project.bookstoreapp.model.BookStat;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BookStatAdapter extends RecyclerView.Adapter<BookStatAdapter.ViewHolder> {
    private Context context;
    private List<BookStat> bookList;
    private final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public BookStatAdapter(Context context, List<BookStat> bookList) {
        this.context = context;
        this.bookList = bookList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookStat book = bookList.get(position);

        holder.tvBookTitle.setText(book.getTitle());
        holder.tvBookPrice.setText(VND.format(book.getPrice()) + " đ");
        holder.tvBookStock.setText("Tồn kho: " + book.getStock());
        
        if (book.getSoldInMonth() > 0) {
            holder.tvBookSold.setText("Đã bán (tháng này): " + book.getSoldInMonth());
            holder.tvBookSold.setTextColor(context.getResources().getColor(R.color.green_success));
        } else {
            holder.tvBookSold.setText("Chưa bán được trong tháng");
            holder.tvBookSold.setTextColor(context.getResources().getColor(R.color.gray_text));
        }

        if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(book.getImageUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.ivBookImage);
        } else {
            holder.ivBookImage.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @Override
    public int getItemCount() {
        return bookList == null ? 0 : bookList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBookImage;
        TextView tvBookTitle, tvBookPrice, tvBookStock, tvBookSold;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBookImage = itemView.findViewById(R.id.ivBookImage);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvBookPrice = itemView.findViewById(R.id.tvBookPrice);
            tvBookStock = itemView.findViewById(R.id.tvBookStock);
            tvBookSold = itemView.findViewById(R.id.tvBookSold);
        }
    }
}
