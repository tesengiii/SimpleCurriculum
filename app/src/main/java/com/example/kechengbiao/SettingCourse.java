package com.example.kechengbiao;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class SettingCourse extends AppCompatActivity {

    TextView courseTime,courseName,teacher,classroom,confirm,cancel;
    private courseDataClass dt;
    int msum,asum,esum,weekSum,courseSum;
    ArrayList<ToggleButton> selectWeekList = new ArrayList<>();//选择第几周上课按钮列表
    RadioButton oddWeek,doubleWeek,allWeek;
    LinearLayout shangkeshijian;
    boolean isAdd;
    RadioGroup rg_week;
    DBOpenHelper dbOpenHelper;//sqlite helper类的子类
    SQLiteDatabase db;
    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_course);
        context = this;
        Intent intent = getIntent();
        getInitData();
        getUI();
        courseSum = msum+asum+esum;//有多少节课
        LinearLayout container = (LinearLayout) findViewById(R.id.selectWeekContainer);//第几周上课选择卡的容器
        initWheelData();
        //设置那一周上课的选择卡
        for (int i = 0; i < weekSum; i+=6) {

            LinearLayout cardContainer = new LinearLayout(this);
            cardContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    dip2px(this,40)));
            cardContainer.setOrientation(LinearLayout.HORIZONTAL);
            cardContainer.setPadding(dip2px(this,10),dip2px(this,5),dip2px(this,10),0);
            container.addView(cardContainer);
            for (int j = 0; j < 6; j++) {
                //添加空的占位控件
                if(i+j==weekSum){
                    for (int k = 0; k < 6-weekSum%6; k++) {
                        TextView kong = new TextView(this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1);
                        lp.setMargins(5,0,5,0);
                        kong.setLayoutParams(lp);
                        cardContainer.addView(kong);
                    }
                    break;
                }
                //添加周选择卡
                ToggleButton card = new ToggleButton(this);
                card.setTextOn(""+(i+1+j));
                card.setTextOff(""+(i+1+j));
                card.setTextColor(this.getColor(R.color.defaultWhite));

                card.setChecked(true);
                card.setBackground(this.getDrawable(R.drawable.bg_togglebutton));
                card.setTag(j+i);//数组从0开始
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1);
                lp.setMargins(5,0,5,0);
                card.setLayoutParams(lp);
                //周选择卡选择状态发生变更后 改变选择的周
                card.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        int tag = (int)compoundButton.getTag();
                        String copy;
                        if(b){

                            copy = dt.week.substring(0,tag) + "1" +dt.week.substring(tag+1);

                        }
                        else{
                            copy = dt.week.substring(0,tag) + "0" +dt.week.substring(tag+1);
                        }
                        dt.week = copy;

                    }
                });
                cardContainer.addView(card);
                selectWeekList.add(card);
            }
        }
        isAdd = intent.getBooleanExtra("isAdd",false);

        //如果是添加新的课程
        if(isAdd){
            dt.beginTime = intent.getIntExtra("beginTime",0);
            dt.period = intent.getIntExtra("period",0);
            dt.sum = 1;
            courseTime.setText(dt.beginTime+"-"+dt.beginTime);
            allWeek.setChecked(true);
            dt.week = "";
            for (int j = 0; j < weekSum; j++) {
                dt.week += "1";
            }

        }//如果是编辑已有的课程
        else{
            dt.beginTime = intent.getIntExtra("beginTime",0);
            dt.sum = intent.getIntExtra("sum",0);
            dt.id = intent.getIntExtra("id",0);
            dt.name = intent.getStringExtra("name");
            dt.teacher = intent.getStringExtra("teacher");
            dt.classroom = intent.getStringExtra("classroom");
            dt.week = intent.getStringExtra("week");
            dt.period = intent.getIntExtra("period",0);
            Log.d(TAG, "onCreate: "+dt.period);
            classroom.setText(dt.teacher);
            teacher.setText(dt.teacher);
            courseName.setText(dt.name);
            courseTime.setText(dt.beginTime+"-"+(dt.beginTime+dt.sum-1));

            for (int i = 0; i < weekSum; i++) {
                if(dt.week.substring(i,i+1).equals("1")){
                    selectWeekList.get(i).setChecked(true);
                }
                else{
                    selectWeekList.get(i).setChecked(false);
                }
            }

        }
    }



    int monitorBegin,monitorEnd;//接受滑动选择框的返回值
    //获取界面UI控件 并且绑定部分点击事件
    private void getUI(){
        classroom = findViewById(R.id.et_classroom);
        teacher = findViewById(R.id.et_teacherName);
        courseTime = findViewById(R.id.tv_courseTime);
        courseName = findViewById(R.id.et_courseName);
        //确认按钮
        confirm = findViewById(R.id.stCourse_confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            //todo 编辑数据有bug
            @Override
            public void onClick(View view) {
                if(courseName.length()!=0){

                    dt.name = courseName.getText().toString();
                }
                if(teacher.length()!=0){
                    dt.teacher = teacher.getText().toString();
                }
                if(classroom.length()!=0){
                    dt.classroom = classroom.getText().toString();
                }
                if(dt.name==null){
                    Toast.makeText(context,"请输入课程名称",Toast.LENGTH_SHORT).show();
                }
                //保存数据
                else{
                    ContentValues values = new ContentValues();
                    values.put("name",dt.name);
                    values.put("teacher",dt.teacher);
                    values.put("classroom",dt.classroom);
                    values.put("begintime",dt.beginTime);
                    values.put("sum",dt.sum);
                    values.put("week",dt.week);
                    values.put("color",dt.color);
                    values.put("period",dt.period);
                    //添加新的课程为插入
                    if(isAdd){
                        db.insert("coursedata",null,values);
                    }
                    //修改已知课程根据id修改
                    else{
                        Log.d(TAG, "onClick: "+dt.sum);
                        db.update("coursedata",values,"id=?",new String[]{String.valueOf(dt.id)});
                    }

                    //todo 是否返回值
                    setResult(0);
                    finish();
                }
            }
        });
        //取消按钮
        cancel = findViewById(R.id.stCourse_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        oddWeek = findViewById(R.id.oddWeek);
        doubleWeek = findViewById(R.id.doubleWeek);
        allWeek = findViewById(R.id.allWeek);
        rg_week = findViewById(R.id.select_week);
        //单周 双周 全选 选择框 监听函数
        rg_week.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                //单周
                if(i==oddWeek.getId()){
                    for (int j = 0; j < selectWeekList.size(); j++) {
                        if(j%2==0){
                            selectWeekList.get(j).setChecked(true);

                        }
                        else{
                            selectWeekList.get(j).setChecked(false);

                        }
                    }

                }
                //双周
                else if(i==doubleWeek.getId()){

                    for (int j = 0; j < selectWeekList.size(); j++) {
                        if(j%2!=0){
                            selectWeekList.get(j).setChecked(true);

                        }
                        else{
                            selectWeekList.get(j).setChecked(false);

                        }
                    }
                }
                //全选
                else{
                    for (int j = 0; j < selectWeekList.size(); j++) {
                        selectWeekList.get(j).setChecked(true);

                    }
                }
            }
        });
        shangkeshijian = findViewById(R.id.shangkeshijian);//设置上课第几节
        //设置选择节数框
        shangkeshijian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final WheelView beginWheel,endWheel;
                View outerView = LayoutInflater.from(context).inflate(R.layout.dialog_course_time,null);
                beginWheel = outerView.findViewById(R.id.wheel_begin_jie);
                endWheel = outerView.findViewById(R.id.wheel_end_jie);
                beginWheel.setOffset(1);
                beginWheel.setItems(bj);
                beginWheel.setSelection(dt.beginTime-1);
                beginWheel.setOnWheelViewListener(new WheelView.OnWheelViewListener(){
                    public void onSelected(int selectedIndex, String item) {
                        monitorBegin = selectedIndex+1;
                        if(monitorBegin>monitorEnd){
                            endWheel.setSelection(monitorBegin-1);
                        }
                    }
                });
                endWheel.setOffset(1);
                endWheel.setItems(bj);
                endWheel.setSelection(dt.beginTime+dt.sum-2);
                endWheel.setOnWheelViewListener(new WheelView.OnWheelViewListener(){
                    public void onSelected(int selectedIndex, String item) {
                        monitorEnd = selectedIndex+1;
                        if(monitorBegin>monitorEnd){
                            beginWheel.setSelection(monitorEnd-2);
                        }
                    }
                });
                //生成弹出框
                AlertDialog alert;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(outerView);
                builder.setCancelable(false);
                alert = builder.create();
                //设置布局中的取消按钮
                outerView.findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                });
                //设置确定按钮
                outerView.findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dt.beginTime = monitorBegin-1;//减去偏移值
                        dt.sum = monitorEnd  -monitorBegin+1;
                        courseTime.setText(""+dt.beginTime+"-"+(dt.sum+dt.beginTime-1));
                        //实现点击按钮的作用
                        alert.dismiss();
                    }
                });
                alert.show();

            }
        });
    }


    //滑动选择框的数据初始化
    ArrayList<String> bj = new ArrayList<>();
    private void initWheelData(){
        for (int i = 1; i <= courseSum; i++) {
            bj.add(String.valueOf(i));
        }
    }
    //课程数据类
    private class courseDataClass {
        public String name, teacher, classroom, week, color;
        public int beginTime,sum,period ,id;
    }


    //获取数据库数据
    private void getInitData(){
        dt = new courseDataClass();
        dbOpenHelper = new DBOpenHelper(this,"data.db",null,1);
        db = dbOpenHelper.getWritableDatabase();

        //获取有多少节课
        Cursor cursor = db.query("setting",
                null,
                "name=?",
                new String[]{"msum"},
                null,
                null,
                null);
        cursor.moveToFirst();
        msum = cursor.getInt(cursor.getColumnIndexOrThrow("state"));
        cursor = db.query("setting",
                null,
                "name=?",
                new String[]{"asum"},
                null,
                null,
                null);
        cursor.moveToFirst();
        asum = cursor.getInt(cursor.getColumnIndexOrThrow("state"));
        cursor = db.query("setting",
                null,
                "name=?",
                new String[]{"esum"},
                null,
                null,
                null);
        cursor.moveToFirst();
        esum = cursor.getInt(cursor.getColumnIndexOrThrow("state"));
        cursor = db.query("setting",
                null,
                "name=?",
                new String[]{"weeksum"},
                null,
                null,
                null);
        cursor.moveToFirst();
        weekSum = cursor.getInt(cursor.getColumnIndexOrThrow("state"));
    }

    private static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


}