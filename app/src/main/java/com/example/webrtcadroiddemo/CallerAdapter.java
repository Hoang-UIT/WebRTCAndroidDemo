package com.example.webrtcadroiddemo;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.webrtcandroiddemo.firebase.BaseActivity;

import java.util.ArrayList;

public class CallerAdapter extends RecyclerView.Adapter<CallerItemView> {

    private BaseActivity callerListActivity;
    private ArrayList<Caller> callers;

    public void setMainActivity(BaseActivity activity) {
        this.callerListActivity = activity;
    }

    public CallerAdapter(ArrayList<Caller> callers) {
        this.callers = callers;
    }

    @NonNull
    @Override
    public CallerItemView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.caller_item_view, parent, false);
        CallerItemView callerItemView = new CallerItemView(view);
        callerItemView.setMainActivity(this.callerListActivity);
        return callerItemView;
    }

    @Override
    public void onBindViewHolder(@NonNull CallerItemView holder, int position) {
        Caller caller = callers.get(position);
        holder.setCaller(caller);
    }

    @Override
    public int getItemCount() {
        return callers.size();
    }
}
