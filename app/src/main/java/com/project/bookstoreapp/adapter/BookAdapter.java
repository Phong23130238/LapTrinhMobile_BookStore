package com.project.bookstoreapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Book;
import java.text.DecimalFormat;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> bookList;

    public BookAdapter(List<Book> bookList) {
        this.bookList = bookList;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);
        if (book == null) return;

        holder.tvTitle.setText(book.getTitle());
        holder.tvAuthor.setText(book.getAuthor());

        // Định dạng tiền tệ VND
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvPrice.setText(formatter.format(book.getPrice()) + " đ");

        // Gắn ảnh tạm thời (sử dụng icon mặc định của app làm mock data)
        holder.ivCover.setImageResource(R.mipmap.ic_launcher);
        if (book.isHidden()) {
            // Nếu sách bị ẩn, làm mờ toàn bộ khung giao diện của cuốn sách đó đi 50%
            holder.itemView.setAlpha(0.5f);
        } else {
            // Sách hiển thị bình thường
            holder.itemView.setAlpha(1.0f);
        }
        // BẮT SỰ KIỆN CLICK
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(book); // Gửi cuốn sách bị bấm về cho Activity
            }
        });
    }

    @Override
    public int getItemCount() {
        if (bookList != null) {
            return bookList.size();
        }
        return 0;
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvAuthor, tvPrice;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivItemBookCover);
            tvTitle = itemView.findViewById(R.id.tvItemBookTitle);
            tvAuthor = itemView.findViewById(R.id.tvItemBookAuthor);
            tvPrice = itemView.findViewById(R.id.tvItemBookPrice);
        }
    }

    // Bắt sự kiện click vào cuốn sách
    public interface OnItemClickListener {
        void onItemClick(Book book);
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

}