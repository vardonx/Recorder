package com.example.vardon.recorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.DeleteObjectRequest;
import com.alibaba.sdk.android.oss.model.DeleteObjectResult;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RecordingsFragment extends Fragment{
    private ListView listView;
    private List<Recording> recordingList = new ArrayList<>();
    private Button btn;
    private Recording selected;
    private String fileName;
    private Boolean isPlaying;
    private MediaPlayer mediaPlayer;
    private boolean isSeekBarChanging;
    private SeekBar seekBar;
    private Timer timer;
    private Chronometer chronometer;
    private long pauseTime;
    private Boolean isReplay = false;
    private MainActivity activity;
    private String url;
    SQLiteDatabase db;
    OSS oss;
    ImageButton cloud;
    boolean data;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.recordings_fragment, container, false);

        listView = (ListView) view.findViewById(R.id.list_view);

        activity = (MainActivity) getActivity();
        oss = activity.getOss();
        create_list1(1);
        show_list();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                selected = recordingList.get(position);

           showPlayDialog();

                play();

            }
        });


            listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    menu.add(0, 0, 0, "上传云端");
                    menu.add(0, 1, 0, "分享");
                    menu.add(0, 2, 0, "重命名");
                    menu.add(0, 3, 0, "删除");

                }
            });


        return view;
        }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        data= false;
        cloud = getActivity().findViewById(R.id.cloud);
        cloud.setActivated(false);
        cloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!data) {
                    Toast.makeText(getActivity(), "查看已上传文件", Toast.LENGTH_SHORT).show();
                    //显示云端list
                    v.setActivated(true);



                    //如何读数据？
                    recordingList.clear();
                    create_list1(2);
                    show_list();


                    //开
                    data=true;

                }else{
                    Toast.makeText(getActivity(), "查看本地文件", Toast.LENGTH_SHORT).show();
                    //显示本地list
                    v.setActivated(false);
                    recordingList.clear();
                    create_list1(1);
                    show_list();
                    //关
                    data=false;

                }
            }
        });
    }


    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        int position = info.position;
        String fileName = recordingList.get(position).getName();


        switch (item.getItemId()) {
            //上传云端
            case 0:
                if (data == false)
                    upLoad(recordingList.get(position));
                else
                    dowmLoad(recordingList.get(position));

                break;
            case 1:
                // 分享操作
                share(fileName);
                break;

            case 2:
                // 重命名操作
                showSetNameDialog(fileName);
                break;

            case 3:
                // 删除操作
                deleteItem(fileName);
                break;

            default:
                break;
        }

        return super.onContextItemSelected(item);

    }

    private void upLoad(final Recording recording){
        final String fileName = recording.getName();
        final String  time = recording.getTime();
        final String date = recording.getDate();
        String uoloadFilePath = Environment.getExternalStorageDirectory() + "/recorder"  + "/" + fileName;
        Log.d("sss",uoloadFilePath);
        PutObjectRequest put = new PutObjectRequest("vardon", fileName, uoloadFilePath);


            // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });

        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");

                Log.d("ETag", result.getETag());
                Log.d("RequestId", result.getRequestId());

                activity.saveToDB(2, fileName, time, date);
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });

    }


    private void dowmLoad(final Recording recording){


        GetObjectRequest get = new GetObjectRequest("vardon", recording.getName());
                //设置下载进度回调
        get.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize+"  total_size: " + totalSize, false);
            }
        });
        OSSAsyncTask task = oss.asyncGetObject(get, new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
            @Override
            public void onSuccess(GetObjectRequest request, GetObjectResult result) {
                // 请求成功
                InputStream inputStream = result.getObjectContent();

                byte[] buffer = new byte[2048*2048];
                int len;
                try {
                    OutputStream outputStream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/recorder/"
                            + recording.getName(), true);
                    while ((len = inputStream.read(buffer)) != -1) {
                        // 处理下载的数据
                        outputStream.write(buffer, 0, len);
                    }
                    inputStream.close();
                    outputStream.close();
                    activity.saveToDB(1, recording.getName(), recording.getTime(), recording.getDate());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(GetObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }

    private void showPlayDialog(){
        View view = LayoutInflater.from(getContext()).inflate(R.layout.play_dialog,null,false);
        final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).create();

        TextView nameText = (TextView) view.findViewById(R.id.name_text);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        chronometer = (Chronometer) view.findViewById(R.id.chronometer_play);
        TextView timeText = (TextView) view.findViewById(R.id.file_time_text);
        btn = (Button) view.findViewById(R.id.play_btn);
        nameText.setText(selected.getName());
        timeText.setText(selected.getTime());
        isPlaying = true;
        isReplay = false;
        dialog.show();

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mediaPlayer.release();
                timer.cancel();

            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isReplay == true){

                   play();
                   isReplay = false;
                   btn.setBackgroundResource(R.drawable.pause);
                   //isPlaying = true;

                }else {

                    if (isPlaying == true) {
                        btn.setBackgroundResource(R.drawable.play);
                        isPlaying = false;
                        mediaPlayer.pause();//暂停状态
                        chronometer.stop();
                        pauseTime = SystemClock.elapsedRealtime();
                        timer.purge();//移除所有任务;

                    } else {
                        btn.setBackgroundResource(R.drawable.pause);
                        isPlaying = true;
                        mediaPlayer.start();
                        chronometer.setBase(chronometer.getBase() + (SystemClock.elapsedRealtime() - pauseTime));
                        chronometer.start();

                    }
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new MySeekBar());

    }

    public void deleteItem(final String name){


        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("确定删除?");
        builder.setTitle("提示");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(data == false) {
                    String whereclause = "name = ?" ;
                    db.delete("audio", whereclause, new String[]{name});
                    File file = new File(Environment.getExternalStorageDirectory() + "/recorder", name);
                    file.delete();
                    show_list();
                    create_list1(1);
                }
                else{
                    //从数据库中删除
                    String whereclause = "name = ?" ;
                    db.delete("cloud", whereclause, new String[] {name});

                    //从云端删除
                    DeleteObjectRequest delete = new DeleteObjectRequest("vardon", name);
                        // 异步删除
                    OSSAsyncTask deleteTask = oss.asyncDeleteObject(delete, new OSSCompletedCallback<DeleteObjectRequest, DeleteObjectResult>() {
                        @Override
                        public void onSuccess(DeleteObjectRequest request, DeleteObjectResult result) {
                            Log.d("asyncCopyAndDelObject", "success!");
                        }

                        @Override
                        public void onFailure(DeleteObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                            // 请求异常
                            if (clientExcepion != null) {
                                // 本地异常如网络异常等
                                clientExcepion.printStackTrace();
                            }
                            if (serviceException != null) {
                                // 服务异常
                                Log.e("ErrorCode", serviceException.getErrorCode());
                                Log.e("RequestId", serviceException.getRequestId());
                                Log.e("HostId", serviceException.getHostId());
                                Log.e("RawMessage", serviceException.getRawMessage());
                            }
                        }

                    });
                    show_list();
                    create_list1(2);
                }

            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create().show();
    }

    public void show_list(){
        final MyAdapter myAdapter = new MyAdapter(getActivity(),
                R.layout.list_view_item, recordingList);
        listView.setAdapter(myAdapter);

    }

    public void play(){
        try{
            fileName = selected.getName();
            mediaPlayer = new MediaPlayer();

            if(data == false) {
                File file = new File(Environment.getExternalStorageDirectory() + "/recorder",fileName );
                FileInputStream fis = new FileInputStream(file);
                mediaPlayer.setDataSource(fis.getFD());
                mpHadPrepared();
            }
            else {


                final Handler handler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        Bundle data = msg.getData();
                        url = data.getString("url");
                        try {
                            mediaPlayer.setDataSource(url);
                            mpHadPrepared();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d("sss",url);


                    }
                };
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            String url = oss.presignConstrainedObjectURL("vardon", fileName, 30 * 24 * 60 * 60);
                            Message msg = new Message();
                            Bundle data = new Bundle();
                            data.putString("url",url);
                            msg.setData(data);
                            handler.sendMessage(msg);

                        } catch (ClientException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();


            }


        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void shareCloudURL(final String fileName){

        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle data = msg.getData();
                String url = data.getString("url");
                Intent textIntent = new Intent(Intent.ACTION_SEND);
                textIntent.setType("text/plain");
                textIntent.putExtra(Intent.EXTRA_TEXT, url);
                startActivity(Intent.createChooser(textIntent, "分享"));



            }
        };
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    String url = oss.presignConstrainedObjectURL("vardon", fileName, 30 * 24 * 60 * 60);
                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("url",url);
                    msg.setData(data);
                    handler.sendMessage(msg);

                } catch (ClientException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void mpHadPrepared(){
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekBar.setMax(mediaPlayer.getDuration());
        mediaPlayer.start();
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();


        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isSeekBarChanging){
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            btn.setBackgroundResource(R.drawable.play);
                            chronometer.stop();
                            isReplay = true;
                            timer.cancel();
                        }
                    });
                }
            }
        },0,50);
    }




    private void share(String fileName){



        if(data == false) {
            String path = Environment.getExternalStorageDirectory() + "/recorder" + "/" + fileName;


            Log.d("sss", path);
            Intent textIntent = new Intent(Intent.ACTION_SEND);
            textIntent.setType("audio/mpeg");
            textIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
            startActivity(Intent.createChooser(textIntent, "分享"));
        }
        else{
            shareCloudURL(fileName);
        }
    }







    private void create_list1(int i) {
        recordingList.clear();
        MainActivity activity = (MainActivity) getActivity();
        db  = activity.getDB();
        Cursor cursor;
        if(i == 1)
            cursor = db.query("audio", null, null, null,
                    null, null, null);
        else
            cursor = db.query("cloud", null, null, null,
                    null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                recordingList.add(new Recording(name,time,date));
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    public class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }

        //滚动时暂停后台定时器
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
            chronometer.stop();

        }
        //滑动结束后新设置值
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            mediaPlayer.seekTo(seekBar.getProgress());

            chronometer.setBase(SystemClock.elapsedRealtime()-seekBar.getProgress());
            chronometer.start();
            isPlaying = true;
            mediaPlayer.start();
            btn.setBackgroundResource(R.drawable.pause);

        }
    }
            //修改名字Dialog
    public void showSetNameDialog(final String OldFileName){
        View view = LayoutInflater.from(getContext()).inflate(R.layout.rename_file_dialog,null,false);
        final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).create();

        final EditText reNameText = (EditText)view.findViewById(R.id.rename_text);
        Button btn = (Button)view.findViewById(R.id.rename_btn);

        reNameText.setHint(OldFileName.replace(".mp3",""));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                reName(reNameText, OldFileName);
                dialog.dismiss();

            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                reName(reNameText, OldFileName);
            }
        });


        dialog.show();
    }
    //文件重命名
    public int reName(EditText reNameText, String oldFileName){
        String newFileName = reNameText.getText().toString();
        if(newFileName.length() !=0){

            newFileName = newFileName+".mp3";
            File file = new File(Environment.getExternalStorageDirectory() + "/recorder",newFileName);
            if(file.exists()){
                Toast.makeText(getActivity(), "此文件已存在，请重新命名！" ,
                        Toast.LENGTH_SHORT).show();
                return 0;
            }
            File oleFile = new File(Environment.getExternalStorageDirectory() + "/recorder",oldFileName);
            File newFile = new File(Environment.getExternalStorageDirectory() + "/recorder",newFileName);
            //执行重命名
            oleFile.renameTo(newFile);
        }
        else {
            newFileName = reNameText.getHint().toString()+".mp3";

        }

        //修改数据库
        updateDB(oldFileName, newFileName);

        show_list();
        create_list1(1);
        return 0;
    }
            //修改数据库
    public void updateDB(String oldName, String newName){
        ContentValues values = new ContentValues();

        values.put("name", newName);

        db.update("audio", values, "name = ?", new String[] { oldName });
    }

}
