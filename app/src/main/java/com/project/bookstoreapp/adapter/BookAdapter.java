package com.project.bookstoreapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.activity.BookDetailActivity;
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

        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvPrice.setText(formatter.format(book.getPrice()) + " đ");

        // Gắn ảnh tạm thời (sử dụng icon mặc định của app làm mock data)
        holder.ivCover.setImageResource(R.mipmap.ic_launcher);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy bối cảnh (context) từ itemView để chạy Intent
                Context context = holder.itemView.getContext();

                // Chuẩn bị mở BookDetailActivity
                Intent intent = new Intent(context, BookDetailActivity.class);

                // Nhét ID của quyển sách này vào Intent
                // LƯU Ý: Chữ "BOOK_ID" là chiếc chìa khóa bí mật, bên kia phải dùng đúng chữ này để mở
                intent.putExtra("BOOK_ID", book.getId());

                // Mở màn hình chi tiết
                context.startActivity(intent);
            }
        });
        // ==========================================
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
}