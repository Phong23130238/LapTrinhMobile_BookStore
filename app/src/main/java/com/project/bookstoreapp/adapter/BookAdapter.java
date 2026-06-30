package com.project.bookstoreapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Book;
import java.text.DecimalFormat;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> bookList;
    private OnItemClickListener listener;

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

        // Giá tiền
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvPrice.setText(formatter.format(book.getPrice()) + " đ");

        // 3. SỬA LỖI ROBOT XANH: Xử lý hiển thị hình ảnh bằng Glide
        String urlAnh = book.getImageUrl();
        int imagePlaceholder = R.drawable.ic_book_placeholder; // Ảnh mặc định của project khi lỗi/trống

        if (urlAnh == null || urlAnh.trim().isEmpty()) {
            holder.ivCover.setImageResource(imagePlaceholder);
        } else {
            // Sửa lỗi link bị lặp mã nguồn từ JSON (Ví dụ cuốn số 2)
            if (urlAnh.contains("&imageUrl=http")) {
                urlAnh = urlAnh.split("&imageUrl=")[0];
            }

            Glide.with(holder.itemView.getContext())
                    .load(urlAnh.trim())
                    .placeholder(imagePlaceholder) // Ảnh hiển thị tạm lúc đang tải
                    .error(imagePlaceholder)       // Ảnh hiển thị nếu link hỏng (Ngăn robot xanh xuất hiện)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Tối ưu bộ nhớ đệm khi cuộn
                    .into(holder.ivCover);
        }

        // Code làm mờ sách bị ẩn
        if (book.isHidden()) {
            holder.itemView.setAlpha(0.5f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        // Bắt sự kiện click
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

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setFilteredList(List<Book> filteredList) {
        this.bookList = filteredList;
        notifyDataSetChanged();
    }
}