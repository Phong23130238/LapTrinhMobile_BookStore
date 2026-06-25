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

        holder.tvOrderId.setText(order.getOrderId());
        holder.tvOrderDate.setText(order.getOrderDate());
        holder.tvCustomerName.setText("Khách hàng: " + order.getCustomerName());

        DecimalFormat formatter = new DecimalFormat("###,###,###");
        holder.tvTotalAmount.setText("Tổng tiền: " + formatter.format(order.getTotalAmount()) + " đ");

        holder.tvPaymentStatus.setText(order.getPaymentStatus());
        holder.tvShippingStatus.setText(order.getShippingStatus());

        // Thay đổi màu sắc nhãn tùy theo trạng thái thanh toán để tăng tính trực quan
        if (order.getPaymentStatus().contains("Chưa")) {
            holder.tvPaymentStatus.setBackgroundColor(0xFFD32F2F); // Màu đỏ danger
        } else {
            holder.tvPaymentStatus.setBackgroundColor(0xFF388E3C); // Màu xanh lá thành công
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
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