package com.project.bookstoreapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.model.Order;
import java.text.DecimalFormat;
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

        holder.tvOrderId.setText("#DH" + order.getOrderId());

        // 1. BẢO VỆ LỖI NULL NGÀY THÁNG (Bị thiếu ở Order #1 trong DB)
        String date = order.getCreatedAt();
        if (date == null || date.isEmpty()) {
            date = "Chưa cập nhật ngày";
        }
        holder.tvOrderDate.setText(date);

        // Tạm thời hiển thị Mã KH. Khi nối với Firebase, bạn có thể gọi API User để lấy tên.
        holder.tvCustomerName.setText("Mã KH: " + order.getUserId());

        // Tổng tiền (Nếu DB thiếu, kiểu double trong Java sẽ mặc định là 0.0 nên không lo lỗi Null)
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvTotalAmount.setText("Tổng tiền: " + formatter.format(order.getTotalPrice()) + " đ");

        // 2. BẢO VỆ PHƯƠNG THỨC THANH TOÁN
        if ("banking".equalsIgnoreCase(order.getPaymentMethod())) {
            holder.tvPaymentStatus.setText("Đã TT (VNPay)");
            holder.tvPaymentStatus.setBackgroundColor(0xFF388E3C);
        } else {
            holder.tvPaymentStatus.setText("COD (Chưa TT)");
            holder.tvPaymentStatus.setBackgroundColor(0xFFD32F2F);
        }

        // 3. BẢO VỆ TRẠNG THÁI STATUS (Dùng toLowerCase để chống lỗi in hoa/in thường)
        String statusText = "Chờ xác nhận"; // Mặc định nếu null
        if (order.getStatus() != null) {
            switch(order.getStatus().toLowerCase()) {
                case "pending": statusText = "Chờ xác nhận"; break;
                case "confirmed": statusText = "Đã xác nhận"; break;
                case "shipping": statusText = "Đang giao"; break;
                case "delivered": statusText = "Đã giao"; break;
                case "cancelled": statusText = "Đã hủy"; break;
            }
        }
        holder.tvShippingStatus.setText(statusText);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvCustomerName, tvTotalAmount, tvPaymentStatus, tvShippingStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvShippingStatus = itemView.findViewById(R.id.tvShippingStatus);
        }
    }
}