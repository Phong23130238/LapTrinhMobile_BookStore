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

        // 1. BẢO VỆ NULL: Tên sách
        String title = book.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "Đang cập nhật tên sách";
        }
        holder.tvTitle.setText(title);

        // 2. BẢO VỆ NULL: Tên tác giả
        String author = book.getAuthor();
        if (author == null || author.trim().isEmpty()) {
            author = "Đang cập nhật tác giả";
        }
        holder.tvAuthor.setText(author);

        // Giá tiền (Kiểu double mặc định là 0.0 nên không bị lỗi Null)
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvPrice.setText(formatter.format(book.getPrice()) + " đ");

        // Gắn ảnh tạm thời
        // Tương lai: Nhóm bạn sẽ dùng thư viện Glide hoặc Picasso để load ảnh từ book.getImageUrl() vào ivCover
        holder.ivCover.setImageResource(R.mipmap.ic_launcher);

        // Code làm mờ sách bị ẩn (Phục vụ nhánh của Admin)
        if (book.isHidden()) {
            holder.itemView.setAlpha(0.5f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        // Bắt sự kiện click và đẩy ra ngoài qua Interface
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList != null ? bookList.size() : 0;
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

    public interface OnItemClickListener {
        void onItemClick(Book book);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}