package com.example.vardon.recorder;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.graphics.Color.alpha;

public class BottomTab {

    //1、定义接口和接口回调的方法
    public interface  OnTabChangeListener{
        void onTabChange(int position);
    }
    //2、创建接口变量，作为接口对象
    OnTabChangeListener mOnTabChangeListener;

    List<TextView> mTextViews = new ArrayList<>();
    public void addBottomTab(LinearLayout container, String[] bottomTitleArr){
        for(int i = 0 ; i<bottomTitleArr.length;i++){
            TextView childView = (TextView) View.inflate(container.getContext(), R.layout.activity_bottom, null);
            mTextViews.add(childView);
            //给TextView添加文本
            childView.setText(bottomTitleArr[i]);
            int w = 0;
            int h = LinearLayout.LayoutParams.MATCH_PARENT;
            //创建params对象
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w,h);
            //weight设置为1，均分父容器宽度
            params.weight = 1;
            container.addView(childView,params);
            final int j=i;
            childView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //3、在需要通过接口回调传值的地方调用一次接口回调方法
                    mOnTabChangeListener.onTabChange(j);
                }
            });
        }
    }

    public void setOnTabChangeListener(OnTabChangeListener onTabChangeListener) {
        mOnTabChangeListener = onTabChangeListener;
    }

    public void changeColor(int position){
        //还原所有颜色
        for(TextView textView : mTextViews) textView.setBackgroundColor(alpha(0));

        //让当前fragment位置的tab改变颜色
        mTextViews.get(position).setBackgroundResource(R.color.colorPrimaryDark);
    }



}
