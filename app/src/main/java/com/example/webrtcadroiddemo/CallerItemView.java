package com.example.webrtcadroiddemo;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.webrtcandroiddemo.firebase.BaseActivity;

public class CallerItemView extends RecyclerView.ViewHolder {

    private BaseActivity callerListActivity;
    private TextView txtIsOnline;
    private TextView txtName;
    private Caller caller;


    public void setMainActivity(BaseActivity activity) {
        this.callerListActivity = activity;
    }

    public void setCaller(Caller caller) {
        this.caller = caller;
        if (caller.getOnline()) {
            txtIsOnline.setText("Online");
            txtIsOnline.setTextColor(Color.rgb(0,135,0));
        } else {
            txtIsOnline.setText("Offline");
            txtIsOnline.setTextColor(Color.rgb(255,0,0));
        }
        txtName.setText(caller.getName());
    }

    public CallerItemView(View itemView) {
        super(itemView);
        txtIsOnline = itemView.findViewById(R.id.txtCallerId);
        txtName = itemView.findViewById(R.id.txtUserName);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallerItemView.this.callerListActivity.callUser(CallerItemView.this.caller);
            }
        });
    }
}
