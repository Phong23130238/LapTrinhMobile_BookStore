package com.project.bookstoreapp.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.project.bookstoreapp.R;
import com.project.bookstoreapp.adapter.InventoryPagerAdapter;

public class InventoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        MaterialToolbar toolbar = findViewById(R.id.toolbarInventory);
        TabLayout tabLayout = findViewById(R.id.tabLayoutInventory);
        ViewPager2 viewPager = findViewById(R.id.viewPagerInventory);

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        InventoryPagerAdapter pagerAdapter = new InventoryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Vô hiệu hóa lướt ViewPager để tránh xung đột với nội dung cuộn bên trong nếu có (tuỳ chọn)
        // viewPager.setUserInputEnabled(false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Tồn kho");
                    break;
                case 1:
                    tab.setText("Nhập kho");
                    break;
                case 2:
                    tab.setText("Lịch sử");
                    break;
            }
        }).attach();
    }
}
