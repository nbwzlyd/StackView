package com.music.lyd.stackview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

import wiget.StackView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private StackItemAdapter mAdapter;

    StackView mStackView;
    private ArrayList<Integer> mViewIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStackView = (StackView) findViewById(R.id.stackview);


        mViewIds.add(R.drawable.s1);
        mViewIds.add(R.drawable.s2);
        mViewIds.add(R.drawable.s3);
        mViewIds.add(R.drawable.s4);
        mViewIds.add(R.drawable.s5);
        mViewIds.add(R.drawable.s6);
        mViewIds.add(R.drawable.s7);
        mViewIds.add(R.drawable.s8);
        mViewIds.add(R.drawable.s9);
        mAdapter = new StackItemAdapter();
        mStackView.setAdapter(mAdapter);

        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStackView.startLoop();
            }
        });


    }

    private class StackItemAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mViewIds.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.stack_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.imageView.setImageResource(mViewIds.get(position));
            return convertView;
        }
    }

    public static class ViewHolder {
        public ImageView imageView;
    }
}
