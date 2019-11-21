package com.example.musicmedia;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {


    private static final int INTERNAL_TIME = 500;
    final MediaPlayer mp = new MediaPlayer();
    String song_path = "";
    private SeekBar seekBar;
    private TextView currentTV;
    private TextView totalTV;
    boolean isStop = true;
    private boolean isSeekBarChanging;//互斥变量，防止进度条与定时器冲突。
    private int currentposition;//当前音乐播放的进度
    private Timer timer;
    private ArrayList<String> list;
    private File[] songFiles;


    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            // 展示给进度条和当前时间
            int progress = mp.getCurrentPosition();
            seekBar.setProgress(progress);
            currentTV.setText(formatTime(progress));
            // 继续定时发送数据
            updateProgress();
            return true;
        }
    });

    //3、使用formatTime方法对时间格式化：
    private String formatTime(int length) {
        Date date = new Date(length);
        //时间格式化工具
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String totalTime = sdf.format(date);
        return totalTime;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        totalTV = findViewById(R.id.music_total_time);
        currentTV = findViewById(R.id.music_current_time);
        seekBar = (SeekBar) findViewById(R.id.music_seekbar);
        seekBar.setOnSeekBarChangeListener(new MySeekBar());



        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
            return;
        }

        //判断是否是AndroidN以及更高的版本 N=24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        list = new ArrayList<String>();   //音乐列表
        File sdpath = Environment.getExternalStorageDirectory(); //获得手机SD卡路径
        File path = new File("/sdcard/kgmusic/download/");      //获得SD卡的mp3文件夹
        //返回以.mp3结尾的文件 (自定义文件过滤)
        songFiles = path.listFiles(new MyFilter(".mp3"));
        for (File file : songFiles) {
            String str = file.getAbsolutePath();
            String str1 = str.substring(str.indexOf("/music/")+7,str.indexOf(".mp3"));
            list.add(str1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_single_choice,
                list);
        ListView li = (ListView) findViewById(R.id.listView1);
        li.setAdapter(adapter);
        li.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        li.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                song_path = ((TextView) view).getText().toString();
                currentposition = position;
                changeMusic(currentposition);
                try {
                    mp.reset();    //重置
                    mp.setDataSource(song_path);
                    mp.prepare();     //准备
                    mp.start(); //播放
                    seekBar.setMax(mp.getDuration());
                    isStop = false;
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (!isSeekBarChanging) {
                                seekBar.setProgress(mp.getCurrentPosition());

                            }
                        }
                    }, 0, 50);
                } catch (Exception e) {
                }
            }

        });

        //暂停和播放
        final ImageButton btnpause = (ImageButton) findViewById(R.id.btn_pause);
        btnpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (song_path.isEmpty())
                    Toast.makeText(getApplicationContext(), "请先选择一首歌", Toast.LENGTH_SHORT).show();
                if (mp.isPlaying()) {
                    mp.pause();  //暂停
                    isStop = true;
                    btnpause.setImageResource(android.R.drawable.ic_media_play);
                } else if (!song_path.isEmpty()) {
                    mp.start();   //继续播放
                    isStop = false;
                    btnpause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });
        //MidiaPlayer
        //上一曲和下一曲
        final ImageButton previous = (ImageButton) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMusic(--currentposition);
            }
        });
        final ImageButton next = (ImageButton) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMusic(++currentposition);
            }
        });

    }

    private void changeMusic(int position) {
        if (position < 0) {
            currentposition = position = list.size() - 1;
        } else if (position > list.size() - 1) {
            currentposition = position = 0;
        }
        song_path = songFiles[position].getAbsolutePath();

        try {
            // 切歌之前先重置，释放掉之前的资源
            mp.reset();
            // 设置播放源
            mp.setDataSource(song_path);
            // 开始播放前的准备工作，加载多媒体资源，获取相关信息
            mp.prepare();

            // 开始播放
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekBar.setProgress(0);//将进度条初始化
        seekBar.setMax(mp.getDuration());//设置进度条最大值为歌曲总时间
        totalTV.setText(formatTime(mp.getDuration()));//显示歌曲总时长

        updateProgress();//更新进度条
    }

    private void updateProgress() {
        // 使用Handler每间隔1s发送一次空消息，通知进度条更新
        Message msg = Message.obtain();// 获取一个现成的消息
        // 使用MediaPlayer获取当前播放时间除以总时间的进度
        int progress = mp.getCurrentPosition();
        msg.arg1 = progress;
        mHandler.sendMessageDelayed(msg, INTERNAL_TIME);
    }


    public class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }

        /*滚动时,应当暂停后台定时器*/
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
        }

        /*滑动结束后，重新设置值*/
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            mp.seekTo(seekBar.getProgress());
        }

    }

    protected void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            mp.stop();
            mp.release();
        }
        Toast.makeText(getApplicationContext(), "退出", Toast.LENGTH_SHORT).show();
    }
}


