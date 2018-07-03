package com.shoppay.hhhlsj;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.shoppay.hhhlsj.adapter.RechargeAdapter;
import com.shoppay.hhhlsj.bean.RechargeMsg;
import com.shoppay.hhhlsj.bean.SystemQuanxian;
import com.shoppay.hhhlsj.bean.VipInfo;
import com.shoppay.hhhlsj.bean.VipInfoMsg;
import com.shoppay.hhhlsj.bean.VipRecharge;
import com.shoppay.hhhlsj.card.ReadCardOpt;
import com.shoppay.hhhlsj.card.ReadCardOptHandler;
import com.shoppay.hhhlsj.http.InterfaceBack;
import com.shoppay.hhhlsj.tools.ActivityStack;
import com.shoppay.hhhlsj.tools.BluetoothUtil;
import com.shoppay.hhhlsj.tools.CommonUtils;
import com.shoppay.hhhlsj.tools.DateUtils;
import com.shoppay.hhhlsj.tools.DayinUtils;
import com.shoppay.hhhlsj.tools.DialogUtil;
import com.shoppay.hhhlsj.tools.LogUtils;
import com.shoppay.hhhlsj.tools.NoDoubleClickListener;
import com.shoppay.hhhlsj.tools.PreferenceHelper;
import com.shoppay.hhhlsj.tools.RechargeDialog;
import com.shoppay.hhhlsj.tools.StringUtil;
import com.shoppay.hhhlsj.tools.UrlTools;
import com.shoppay.hhhlsj.view.MyGridViews;
import com.shoppay.hhhlsj.wxcode.MipcaActivityCapture;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by songxiaotao on 2017/6/30.
 */

public class VipRechargeActivity extends Activity implements View.OnClickListener {
    private RelativeLayout rl_left, rl_rechage, rl_chose;
    private EditText et_money;
    private TextView tv_title, tv_vipname, tv_vipyue, tv_jifen, tv_dengji, et_vipcard, tv_cardmian, tv_chose;
    private MyGridViews myGridViews;
    private Context ac;
    private String state = "现金";
    private String editString;
    private Dialog dialog;
    private Dialog paydialog;
    private RechargeAdapter adapter;
    private VipRecharge recharge;
    private boolean isSuccess = false;
    private boolean isMoney = true, isWx = false, isZhifubao = false, isYinlian = false;
    private RadioButton rb_money, rb_wx, rb_zhifubao, rb_isYinlian;
    private Handler mhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 8:
                    String card = (String) msg.obj;
                    et_vipcard.setText(card);
                    ontainVipInfo(card);
                    break;
            }
        }
    };
    private RadioGroup mRadiogroup;
    private RelativeLayout rl_right;
    private String orderAccount;
    private SystemQuanxian sysquanxian;
    private MyApplication app;
    private List<RechargeMsg> list;
    private RechargeMsg rechargeMsg;
    VipInfo info;
    private boolean isChose = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viprecharge);
        ac = MyApplication.context;
        app = (MyApplication) getApplication();
        sysquanxian = app.getSysquanxian();
        dialog = DialogUtil.loadingDialog(VipRechargeActivity.this, 1);
        paydialog = DialogUtil.payloadingDialog(VipRechargeActivity.this, 1);
        PreferenceHelper.write(MyApplication.context, "shoppay", "viptoast", "未查询到会员");
        ActivityStack.create().addActivity(VipRechargeActivity.this);
        initView();
        rechargeChoseList("no");
        mRadiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.rb_money:
                        isMoney = true;
                        isWx = false;
                        isYinlian = false;
                        isZhifubao = false;
                        break;
                    case R.id.rb_zhifubao:
                        isZhifubao = true;
                        isMoney = false;
                        isWx = false;
                        isYinlian = false;
                        break;
                    case R.id.rb_yinlian:
                        isYinlian = true;
                        isMoney = false;
                        isWx = false;
                        isZhifubao = false;
                        break;
                    case R.id.rb_wx:
                        isWx = true;
                        isMoney = false;
                        isYinlian = false;
                        isZhifubao = false;
                        break;
                }
            }
        });


//        PreferenceHelper.write(getApplicationContext(), "PayOk", "time", "false");
//        //动态注册广播接收器
//        msgReceiver = new MsgReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.example.communication.RECEIVER");
//        registerReceiver(msgReceiver, intentFilter);
    }

    private void rechargeChoseList(final String type) {

        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
        params.put("Memlevel", info.getLevelName());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "MemRechargeRule");
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    Log.d("xxDengjiS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
                        String data = jso.getString("vdata");
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<RechargeMsg>>() {
                        }.getType();
                        list = gson.fromJson(data, listType);
                        if (type.equals("no")) {

                        } else {
                            RechargeDialog.rechargeChoseDialog(ac, list, 1, new InterfaceBack() {
                                @Override
                                public void onResponse(Object response) {
                                    rechargeMsg = (RechargeMsg) response;
                                    rl_chose.setVisibility(View.GONE);
                                    isChose = true;
                                    tv_chose.setText("充值" + StringUtil.twoNum(rechargeMsg.RechargeMoney) + "元送" + StringUtil.twoNum(rechargeMsg.GiveMoney) + "元");
                                }

                                @Override
                                public void onErrorResponse(Object msg) {

                                }
                            });
                        }
                    } else {
                        if (type.equals("no")) {

                        } else {
                            Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    if (type.equals("no")) {

                    } else {
                        Toast.makeText(ac, "获取会员等级失败，请重新登录", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (type.equals("no")) {

                } else {
                    Toast.makeText(ac, "获取会员等级失败，请重新登录", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void ontainVipInfo(String card) {
        dialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
        params.put("MemCard", card);
        LogUtils.d("xxparams", params.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "GetMem");
        LogUtils.d("xxurl", url);
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    dialog.dismiss();
                    LogUtils.d("xxVipinfoS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
                        Gson gson = new Gson();
                        VipInfoMsg infomsg = gson.fromJson(new String(responseBody, "UTF-8"), VipInfoMsg.class);
                        info = infomsg.getVdata().get(0);
                        tv_vipname.setText(info.getMemName());
                        tv_vipyue.setText(info.getMemMoney());
                        tv_jifen.setText(info.getMemPoint());
                        tv_dengji.setText(info.getLevelName());
                        PreferenceHelper.write(ac, "shoppay", "memid", info.getMemID());
                        PreferenceHelper.write(ac, "shoppay", "vipcar", et_vipcard.getText().toString());
                        PreferenceHelper.write(ac, "shoppay", "Discount", info.getDiscount());
                        PreferenceHelper.write(ac, "shoppay", "DiscountPoint", info.getDiscountPoint());
                        PreferenceHelper.write(ac, "shoppay", "jifen", info.getMemPoint());
                        isSuccess = true;
                    } else {
                        tv_vipname.setText("");
                        tv_vipyue.setText("");
                        tv_jifen.setText("");
                        tv_dengji.setText("");
                        isSuccess = false;
                        PreferenceHelper.write(ac, "shoppay", "memid", "123");
                        PreferenceHelper.write(ac, "shoppay", "vipcar", "123");
                    }
                } catch (Exception e) {
                    tv_vipname.setText("");
                    tv_vipyue.setText("");
                    tv_jifen.setText("");
                    tv_dengji.setText("");
                    isSuccess = false;
                    PreferenceHelper.write(ac, "shoppay", "memid", "123");
                    PreferenceHelper.write(ac, "shoppay", "vipcar", "123");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                dialog.dismiss();
                tv_vipname.setText("");
                tv_vipyue.setText("");
                tv_jifen.setText("");
                tv_dengji.setText("");
                isSuccess = false;
                PreferenceHelper.write(ac, "shoppay", "memid", "123");
                PreferenceHelper.write(ac, "shoppay", "vipcar", "123");
            }
        });
    }


    private void initView() {
        rl_left = (RelativeLayout) findViewById(R.id.rl_left);
        rl_rechage = (RelativeLayout) findViewById(R.id.viprecharge_rl_recharge);
        et_vipcard = (TextView) findViewById(R.id.viprecharge_et_cardnum);
        tv_cardmian = (TextView) findViewById(R.id.viprecharge_et_cardmian);
        rl_chose = (RelativeLayout) findViewById(R.id.rl_chose);
        et_money = (EditText) findViewById(R.id.et_money);
        tv_chose = (TextView) findViewById(R.id.viprecharge_et_zengsong);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_vipname = (TextView) findViewById(R.id.viprecharge_et_name);
        tv_vipyue = (TextView) findViewById(R.id.viprecharge_et_yue);
        tv_jifen = (TextView) findViewById(R.id.viprecharge_et_jifen);
        tv_dengji = (TextView) findViewById(R.id.viprecharge_et_dengji);
        myGridViews = (MyGridViews) findViewById(R.id.gridview);
        mRadiogroup = (RadioGroup) findViewById(R.id.radiogroup);
        tv_title.setText("会员充值");
        rl_right = (RelativeLayout) findViewById(R.id.rl_right);
        rl_right.setOnClickListener(this);

        rb_isYinlian = (RadioButton) findViewById(R.id.rb_yinlian);
        rb_money = (RadioButton) findViewById(R.id.rb_money);
        rb_zhifubao = (RadioButton) findViewById(R.id.rb_zhifubao);
        rb_wx = (RadioButton) findViewById(R.id.rb_wx);
        if (sysquanxian.isweixin == 0) {
            rb_wx.setVisibility(View.GONE);
        }
        if (sysquanxian.iszhifubao == 0) {
            rb_zhifubao.setVisibility(View.GONE);
        }
        if (sysquanxian.isyinlian == 0) {
            rb_isYinlian.setVisibility(View.GONE);
        }
        if (sysquanxian.isxianjin == 0) {
            rb_money.setVisibility(View.GONE);
        }
        tv_chose.setOnClickListener(this);
        rl_chose.setOnClickListener(this);
        rl_left.setOnClickListener(this);
        rl_rechage.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View view) {
                if (et_vipcard.getText().toString().equals("")
                        || et_vipcard.getText().toString() == null) {
                    Toast.makeText(getApplicationContext(), "请输入会员卡号",
                            Toast.LENGTH_SHORT).show();
                } else if (et_money.getText().toString().equals("") && !isChose) {
                    Toast.makeText(getApplicationContext(), "请输入或选择充值金额",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (isSuccess) {
                        if (CommonUtils.checkNet(getApplicationContext())) {
                            if (isWx) {
                                if (sysquanxian.iswxpay == 0) {
                                    Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                                    mipca.putExtra("type", "pay");
                                    startActivityForResult(mipca, 222);
                                } else {
                                    vipRecharge(DateUtils.getCurrentTime("yyyyMMddHHmmss"));
                                }
                            } else if (isZhifubao) {
                                if (sysquanxian.iszfbpay == 0) {
                                    Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                                    mipca.putExtra("type", "pay");
                                    startActivityForResult(mipca, 222);
                                } else {
                                    vipRecharge(DateUtils.getCurrentTime("yyyyMMddHHmmss"));
                                }
                            } else {
                                vipRecharge(DateUtils.getCurrentTime("yyyyMMddHHmmss"));
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "请检查网络是否可用",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MyApplication.context, PreferenceHelper.readString(MyApplication.context, "shoppay", "viptoast", "未查询到会员"), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 111:
                if (resultCode == RESULT_OK) {
                    et_vipcard.setText(data.getStringExtra("codedata"));
                }
                break;
            case 222:
                if (resultCode == RESULT_OK) {
                    pay(data.getStringExtra("codedata"));
                }
                break;
        }
    }

    private void pay(String codedata) {
        paydialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams map = new RequestParams();
        map.put("auth_code", codedata);
        map.put("UserID", PreferenceHelper.readString(ac, "shoppay", "UserID", ""));
//        （1会员充值7商品消费9快速消费11会员充次）
        map.put("ordertype", 1);
        orderAccount = DateUtils.getCurrentTime("yyyyMMddHHmmss");
        map.put("account", orderAccount);
        if (isChose) {
            map.put("money", rechargeMsg.RechargeMoney);
            map.put("giveMoney", rechargeMsg.GiveMoney);
        } else {
            map.put("money", et_money.getText().toString());
            map.put("giveMoney", "");
        }
//        0=现金 1=银联 2=微信 3=支付宝
        if (isMoney) {
            map.put("payType", 0);
        } else if (isWx) {
            map.put("payType", 2);
        } else if (isYinlian) {
            map.put("payType", 1);
        } else {
            map.put("payType", 3);
        }
        client.setTimeout(120 * 1000);
        LogUtils.d("xxparams", map.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "PayOnLine");
        LogUtils.d("xxurl", url);
        client.post(url, map, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    paydialog.dismiss();
                    LogUtils.d("xxpayS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {

                        JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
                        DayinUtils.dayin(jsonObject.getString("printContent"));
                        if (jsonObject.getInt("printNumber") == 0) {
                        } else {
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (bluetoothAdapter.isEnabled()) {
                                BluetoothUtil.connectBlueTooth(MyApplication.context);
                                BluetoothUtil.sendData(DayinUtils.dayin(jsonObject.getString("printContent")), jsonObject.getInt("printNumber"));
                            } else {
                            }
                        }
                        vipRecharge(orderAccount);
                    } else {
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    paydialog.dismiss();
                    Toast.makeText(ac, "支付失败，请稍后再试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                paydialog.dismiss();
                Toast.makeText(ac, "支付失败，请稍后再试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_right:
                Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                startActivityForResult(mipca, 111);
                break;
            case R.id.rl_left:
                finish();
                break;
            case R.id.viprecharge_et_zengsong:
                if (list == null || list.size() == 0) {
                    rechargeChoseList("yes");
                } else {
                    RechargeDialog.rechargeChoseDialog(ac, list, 1, new InterfaceBack() {
                        @Override
                        public void onResponse(Object response) {
                            rechargeMsg = (RechargeMsg) response;
                            rl_chose.setVisibility(View.GONE);
                            isChose = true;
                            tv_chose.setText("充值" + StringUtil.twoNum(rechargeMsg.RechargeMoney) + "元送" + StringUtil.twoNum(rechargeMsg.GiveMoney) + "元");
                        }

                        @Override
                        public void onErrorResponse(Object msg) {

                        }
                    });
                }
                break;
            case R.id.rl_chose:
                if (list == null || list.size() == 0) {
                    rechargeChoseList("yes");
                } else {
                    RechargeDialog.rechargeChoseDialog(ac, list, 1, new InterfaceBack() {
                        @Override
                        public void onResponse(Object response) {
                            rechargeMsg = (RechargeMsg) response;
                            rl_chose.setVisibility(View.GONE);
                            tv_chose.setText("充值" + StringUtil.twoNum(rechargeMsg.RechargeMoney) + "元送" + StringUtil.twoNum(rechargeMsg.GiveMoney) + "元");
                        }

                        @Override
                        public void onErrorResponse(Object msg) {

                        }
                    });
                }
                break;
        }
    }


    private void vipRecharge(String ordernum) {
        dialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams map = new RequestParams();
        map.put("MemID", PreferenceHelper.readString(ac, "shoppay", "memid", ""));
        map.put("rechargeAccount", ordernum);
        if (isChose) {
            map.put("money", rechargeMsg.RechargeMoney);
            map.put("giveMoney", rechargeMsg.GiveMoney);
        } else {
            map.put("money", et_money.getText().toString());
            map.put("giveMoney", "");
        }
//        0=现金 1=银联 2=微信 3=支付宝
        if (isMoney) {
            map.put("payType", 0);
        } else if (isWx) {
            map.put("payType", 2);
        } else if (isYinlian) {
            map.put("payType", 1);
        } else {
            map.put("payType", 3);
        }

        LogUtils.d("xxparams", map.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "MemRechargeMoney");
        LogUtils.d("xxurl", url);
        client.post(url, map, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    dialog.dismiss();
                    LogUtils.d("xxviprechargeS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {

                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_LONG).show();
                        JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
                        DayinUtils.dayin(jsonObject.getString("printContent"));
                        if (jsonObject.getInt("printNumber") == 0) {
                            finish();
                        } else {
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (bluetoothAdapter.isEnabled()) {
                                BluetoothUtil.connectBlueTooth(MyApplication.context);
                                BluetoothUtil.sendData(DayinUtils.dayin(jsonObject.getString("printContent")), jsonObject.getInt("printNumber"));
                                ActivityStack.create().finishActivity(VipRechargeActivity.class);
                            } else {
                                ActivityStack.create().finishActivity(VipRechargeActivity.class);
                            }
                        }
                    } else {
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(ac, "会员充值失败，请重新登录", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                dialog.dismiss();
                Toast.makeText(ac, "会员充值失败，请重新登录", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        new ReadCardOptHandler(mhandler);
    }

    @Override
    protected void onStop() {
        try {
            new ReadCardOpt().overReadCard();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onStop();
    }


    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 广播接收器
     *
     * @author len
     */
    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //拿到进度，更新UI
//            String state = intent.getStringExtra("success");
//            Log.d("MsgReceiver", "MsgReceiver" + state);
//            if (state == null || state.equals("")) {
//
//            } else {
//                if (state.equals("success")) {
//                    weixinDialog.dismiss();
//                     vipRecharge();
//                } else {
//                    String msg = intent.getStringExtra("msg");
//                    Toast.makeText(ac,msg,Toast.LENGTH_SHORT).show();
//
//                }
//            }
        }

    }

    @Override
    protected void onDestroy() {
        // TODO 自动生成的方法存根
        super.onDestroy();
//        if (intent != null) {
//
//            stopService(intent);
//        }
//
//        //关闭闹钟机制启动service
//        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        int anHour =2 * 1000; // 这是一小时的毫秒数 60 * 60 * 1000
//        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
//        Intent i = new Intent(this, AlarmReceiver.class);
//        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
//        manager.cancel(pi);
//        //注销广播
//        unregisterReceiver(msgReceiver);
    }
}
