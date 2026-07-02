package com.project.bookstoreapp.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.project.bookstoreapp.fragment.HistoryFragment;
import com.project.bookstoreapp.fragment.ImportFragment;
import com.project.bookstoreapp.fragment.StockFragment;

public class InventoryPagerAdapter extends FragmentStateAdapter {

    public InventoryPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StockFragment();
            case 1:
                return new ImportFragment();
            case 2:
                return new HistoryFragment();
            default:
                return new StockFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
