package com.project.bookstoreapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Order;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        String displayId = (order.getDisplayId() != null) ? order.getDisplayId() : "N/A";
        // Trong hàm onBindViewHolder của OrderAdapter
        holder.tvItemDisplayId.setText(order.getDisplayId()); // Gắn dữ liệu displayId vào TextView

        // 1. BẢO VỆ LỖI NULL NGÀY THÁNG
        String dateStr = order.getCreatedAt();
        if (dateStr == null || dateStr.isEmpty()) {
            holder.tvOrderDate.setText("Chưa cập nhật ngày");
        } else {
            Date date = null;
            try {
                date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(dateStr);
            } catch (Exception e) {
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateStr);
                } catch (Exception e2) {
                }
            }
            if (date != null) {
                holder.tvOrderDate.setText(new SimpleDateFormat("dd/MM/yyyy - HH:mm").format(date));
            } else {
                holder.tvOrderDate.setText(dateStr);
            }
        }

        // Tình trạng thanh toán
        String paymentMethod = order.getPaymentMethod();
        if ("banking".equalsIgnoreCase(paymentMethod)) {
            holder.tvPaymentStatus.setText("Đã thanh toán");
            holder.tvPaymentStatus.setTextColor(0xFF047857); // Green Text
            holder.cardPaymentStatus.setCardBackgroundColor(0xFFD1FAE5); // Green BG
        } else {
            if ("delivered".equalsIgnoreCase(order.getStatus())) {
                holder.tvPaymentStatus.setText("Đã thanh toán");
                holder.tvPaymentStatus.setTextColor(0xFF047857);
                holder.cardPaymentStatus.setCardBackgroundColor(0xFFD1FAE5);
            } else {
                holder.tvPaymentStatus.setText("Chưa thanh toán");
                holder.tvPaymentStatus.setTextColor(0xFFEF4444); // Red Text
                holder.cardPaymentStatus.setCardBackgroundColor(0xFFFEE2E2); // Red BG
            }
        }

        // Tổng tiền
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvTotalAmount.setText("Tổng tiền: " + formatter.format(order.getTotalPrice()) + " đ");

        // 3. BẢO VỆ TRẠNG THÁI STATUS (Dùng toLowerCase để chống lỗi in hoa/in thường)
        String statusText = "Chờ xác nhận"; // Mặc định nếu null
        int statusColorText = 0xFFD97706; // Orange Text
        int statusColorBg = 0xFFFEF3C7;   // Orange BG

        if (order.getStatus() != null) {
            switch(order.getStatus().toLowerCase()) {
                case "pending": 
                    statusText = "Chờ xác nhận"; 
                    statusColorText = 0xFFD97706; 
                    statusColorBg = 0xFFFEF3C7; 
                    break;
                case "confirmed": 
                    statusText = "Đã xác nhận"; 
                    statusColorText = 0xFF0284C7; // Blue
                    statusColorBg = 0xFFE0F2FE;
                    break;
                case "shipping": 
                    statusText = "Đang giao"; 
                    statusColorText = 0xFF7C3AED; // Purple
                    statusColorBg = 0xFFEDE9FE;
                    break;
                case "delivered": 
                    statusText = "Đã giao hàng"; 
                    statusColorText = 0xFF047857; // Green
                    statusColorBg = 0xFFD1FAE5;
                    break;
                case "cancelled": 
                    statusText = "Đã hủy"; 
                    statusColorText = 0xFFBE123C; // Rose
                    statusColorBg = 0xFFFFE4E6;
                    break;
            }
        }
        holder.tvShippingStatus.setText(statusText);
        holder.tvShippingStatus.setTextColor(statusColorText);
        holder.cardShippingStatus.setCardBackgroundColor(statusColorBg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemDisplayId, tvOrderDate, tvTotalAmount, tvPaymentStatus, tvShippingStatus;
        MaterialCardView cardPaymentStatus, cardShippingStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemDisplayId = itemView.findViewById(R.id.tvItemDisplayId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvShippingStatus = itemView.findViewById(R.id.tvShippingStatus);
            cardPaymentStatus = itemView.findViewById(R.id.cardPaymentStatus);
            cardShippingStatus = itemView.findViewById(R.id.cardShippingStatus);
        }
    }

    // Hàm dùng để cập nhật danh sách khi tìm kiếm hoặc lọc
    public void filterList(List<Order> filteredList) {
        this.orderList = filteredList;
        notifyDataSetChanged();
    }
}