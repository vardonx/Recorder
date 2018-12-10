package com.example.vardon.recorder;
import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.example.vardon.recorder.BottomTab.OnTabChangeListener;
import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
   // private Button record_view;
    //private Button saved_view;
    private ViewPager mViewPager;
    //TextView mtitle;
    BottomTab bottomTab = new BottomTab();
    List<Fragment> mFragments = new ArrayList<>();
    LinearLayout bottom;
    ImageButton cloud;
    String[] bottomTitle={"Record","Save"};
    private OSS oss;
    SQLiteDatabase db;
    private static SimpleDBHelper dbHelper;
    private static String[] PERMISSION = {      //需要动态申请的权限
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        requestPermissions(MainActivity.this);
        intiOSS();

        //replaceFragment(new RecordFragment());
        //saved_view = (Button)findViewById(R.id.saved_view);
        //record_view = (Button)findViewById(R.id.record_view);
        dbHelper = new SimpleDBHelper(this, 3);
        dbHelper.getWritableDatabase();
        mFragments.add(new RecordFragment());
        mFragments.add(new RecordingsFragment());
        mViewPager = findViewById(R.id.viewpager);
        bottom = findViewById(R.id.bottomContain);
        //mtitle = findViewById(R.id.title);
        cloud=findViewById(R.id.cloud);
        mViewPager.setAdapter(new MyFragmentAdapter(getSupportFragmentManager()));
        bottomTab.addBottomTab(bottom,bottomTitle);
        bottomTab.changeColor(0); //初始化 默认页面
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            public void onPageSelected(int position) {
                bottomTab.changeColor(position);
                mViewPager.setCurrentItem(position);
                replaceFragment(mFragments.get(position));
                //修改对应fragment的标题
                if(bottomTitle[position].equals("Save")) cloud.setVisibility(View.VISIBLE);
                else cloud.setVisibility(View.INVISIBLE);

            }
            public void onPageScrolled(int arg0, float arg1, int arg2) {}
            public void onPageScrollStateChanged(int arg0) {}
        });
        OnTabChangeListener onTabChangeListener=new OnTabChangeListener() {
            @Override
            public void onTabChange(int position) {
                //切换对应的fragment
                mViewPager.setCurrentItem(position);
                replaceFragment(mFragments.get(position));
                if(bottomTitle[position].equals("Save")) cloud.setVisibility(View.VISIBLE);
                else cloud.setVisibility(View.INVISIBLE);
            }
        };
        bottomTab.setOnTabChangeListener(onTabChangeListener) ;
        //saved_view.setOnClickListener(this);
        //record_view.setOnClickListener(this);

    }


    public void intiOSS(){
        String endpoint = "http://oss-cn-shenzhen.aliyuncs.com";
        final String AccessKeyId = "";
        final String AccessKeySecret = "";
        String stsServer = "http://39.108.225.146:8080";
            //使用OSSAuthCredentialsProvider。token过期可以及时更新
        OSSCredentialProvider credentialProvider = new OSSAuthCredentialsProvider(stsServer);
            //该配置类如果不设置，会有默认配置
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求数，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider, conf);
    }


    public OSS getOss(){
        return this.oss;
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.viewpager, fragment);
        transaction.commit();
    }


   /* public void onClick(View v) {
        switch (v.getId()){
            case R.id.record_view:
                changeBotton(saved_view, record_view);
                replaceFragment(new RecordFragment());
                break;
            case R.id.saved_view:
                changeBotton(record_view, saved_view);
                replaceFragment(new RecordingsFragment());;
                break;
        }
    }
    public void changeBotton(Button before, Button after){
        after.setBackgroundResource(R.drawable.bg);
        before.setBackgroundResource(R.drawable.btn_bg);
        after.setEnabled(false);
        before.setEnabled(true);
    }*/
    public static SQLiteDatabase getDB() {
        return dbHelper.getWritableDatabase();
    }

    public static void requestPermissions(Activity activity) {  //动态申请权限（API>=23）
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,PERMISSION,
                    1);
        }
    }

    public void saveToDB(int i,String fileName, String time, String date){
        db = getDB();
        ContentValues values = new ContentValues();

        values.put("name", fileName);
        values.put("time", time);
        values.put("date",date);
        if(i == 1)
            db.insert("audio", null, values);
        else
            db.insert("cloud", null, values);

    }

    class MyFragmentAdapter extends FragmentStatePagerAdapter {

        public MyFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }
    }
}

