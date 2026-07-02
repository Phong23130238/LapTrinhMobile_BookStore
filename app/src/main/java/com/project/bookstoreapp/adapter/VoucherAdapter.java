package com.project.bookstoreapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.activity.AddEditVoucherActivity;
import com.project.bookstoreapp.model.Voucher;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private Context context;
    private List<Voucher> voucherList;
    private NumberFormat vndFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public VoucherAdapter(Context context, List<Voucher> voucherList) {
        this.context = context;
        this.voucherList = voucherList;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);
        
        holder.tvVoucherCode.setText(voucher.getCode());
        holder.tvDiscountPercent.setText("Giảm " + voucher.getDiscountPercent() + "%");
        
        String maxDisc = vndFormat.format(voucher.getMaxDiscount()) + " đ";
        String minOrder = vndFormat.format(voucher.getMinOrderValue()) + " đ";
        holder.tvDiscountDetails.setText("Tối đa " + maxDisc + " - Đơn từ " + minOrder);
        
        if (voucher.isActive()) {
            holder.tvVoucherStatus.setText("Trạng thái: Hoạt động");
            holder.tvVoucherStatus.setTextColor(context.getResources().getColor(R.color.green_success, context.getTheme()));
        } else {
            holder.tvVoucherStatus.setText("Trạng thái: Vô hiệu hóa");
            holder.tvVoucherStatus.setTextColor(context.getResources().getColor(R.color.red_danger, context.getTheme()));
        }
        
        if (voucher.getExpiredAt() != null && !voucher.getExpiredAt().isEmpty()) {
            holder.tvExpiredAt.setText("HSD: " + voucher.getExpiredAt());
            holder.tvExpiredAt.setVisibility(View.VISIBLE);
        } else {
            holder.tvExpiredAt.setVisibility(View.GONE);
        }

        holder.btnEditVoucher.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditVoucherActivity.class);
            intent.putExtra("VOUCHER_CODE", voucher.getCode());
            intent.putExtra("DISCOUNT_PERCENT", voucher.getDiscountPercent());
            intent.putExtra("MAX_DISCOUNT", voucher.getMaxDiscount());
            intent.putExtra("MIN_ORDER_VALUE", voucher.getMinOrderValue());
            intent.putExtra("IS_ACTIVE", voucher.isActive());
            intent.putExtra("EXPIRED_AT", voucher.getExpiredAt());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvVoucherCode, tvDiscountPercent, tvDiscountDetails, tvVoucherStatus, tvExpiredAt;
        ImageButton btnEditVoucher;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVoucherCode = itemView.findViewById(R.id.tvVoucherCode);
            tvDiscountPercent = itemView.findViewById(R.id.tvDiscountPercent);
            tvDiscountDetails = itemView.findViewById(R.id.tvDiscountDetails);
            tvVoucherStatus = itemView.findViewById(R.id.tvVoucherStatus);
            tvExpiredAt = itemView.findViewById(R.id.tvExpiredAt);
            btnEditVoucher = itemView.findViewById(R.id.btnEditVoucher);
        }
    }
}
