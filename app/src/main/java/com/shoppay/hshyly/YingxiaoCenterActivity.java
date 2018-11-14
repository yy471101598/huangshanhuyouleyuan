package com.shoppay.hshyly;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.shoppay.hshyly.bean.FastShopZhehMoney;
import com.shoppay.hshyly.bean.JifenDk;
import com.shoppay.hshyly.bean.VipInfo;
import com.shoppay.hshyly.bean.YinxiaoCenter;
import com.shoppay.hshyly.http.InterfaceBack;
import com.shoppay.hshyly.tools.BluetoothUtil;
import com.shoppay.hshyly.tools.DateUtils;
import com.shoppay.hshyly.tools.DayinUtils;
import com.shoppay.hshyly.tools.DialogUtil;
import com.shoppay.hshyly.tools.LogUtils;
import com.shoppay.hshyly.tools.PreferenceHelper;
import com.shoppay.hshyly.tools.StringUtil;
import com.shoppay.hshyly.tools.UrlTools;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;

/**
 * Created by songxiaotao on 2018/1/23.
 */

public class YingxiaoCenterActivity extends Activity {
    @Bind(R.id.img_left)
    ImageView mImgLeft;
    @Bind(R.id.rl_left)
    RelativeLayout mRlLeft;
    @Bind(R.id.tv_title)
    TextView mTvTitle;
    @Bind(R.id.rl_right)
    RelativeLayout mRlRight;
    @Bind(R.id.boss_tv_starttime)
    TextView mBossTvStarttime;
    @Bind(R.id.boss_tv_endtime)
    TextView mBossTvEndtime;
    @Bind(R.id.tv_newvip)
    TextView mTvNewvip;
    @Bind(R.id.tv_yingshouheji)
    TextView mTvYingshouheji;
    @Bind(R.id.tv_money)
    TextView mTvMoney;
    @Bind(R.id.tv_qita)
    TextView mTvQita;
    @Bind(R.id.tv_zengsong)
    TextView mTvZengsong;
    @Bind(R.id.tv_zhifubao)
    TextView mTvZhifubao;
    @Bind(R.id.tv_yinlian)
    TextView mTvYinlian;
    @Bind(R.id.tv_weixin)
    TextView mTvWeixin;
    @Bind(R.id.tv_heji)
    TextView mTvHeji;
    @Bind(R.id.tv_xfyue)
    TextView mTvXfyue;
    @Bind(R.id.tv_xfqita)
    TextView mTvXfqita;
    @Bind(R.id.tv_xfmoney)
    TextView mTvXfmoney;
    @Bind(R.id.tv_xfzhifubao)
    TextView mTvXfzhifubao;
    @Bind(R.id.tv_xfyinlian)
    TextView mTvXfyinlian;
    @Bind(R.id.tv_xfweixin)
    TextView mTvXfweixin;
    @Bind(R.id.tv_xfheji)
    TextView mTvXfheji;
    @Bind(R.id.et_account)
    EditText et_account;
    @Bind(R.id.tv_txhz)
    TextView tv_txhz;
    @Bind(R.id.tv_txzje)
    TextView tv_txzje;
    @Bind(R.id.tv_kczje)
    TextView tv_kczje;
    @Bind(R.id.tv_yjxj)
    TextView tv_yjxj;
    @Bind(R.id.tv_zjyj)
    TextView tv_zjyj;
    @Bind(R.id.tv_thyj)
    TextView tv_thyj;
    private Activity ac;
    private Dialog dialog;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    YinxiaoCenter yxmsg= (YinxiaoCenter) msg.obj;
                    handlerYinxiaoMsg(yxmsg);

//                    JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
//                    if (jsonObject.getInt("printNumber") == 0) {
//                    } else {
//                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//                        if (bluetoothAdapter.isEnabled()) {
//                            BluetoothUtil.connectBlueTooth(MyApplication.context);
//                            BluetoothUtil.sendData(DayinUtils.dayin(jsonObject.getString("printContent")), jsonObject.getInt("printNumber"));
//                        } else {
//                        }
//                    }
                    break;
                case 2:
                    handlerYinxiaoMsgNull();
                    break;


            }
        }
    };
    private String editString="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yingxiaocenter);
        ButterKnife.bind(this);
        ac = this;
        dialog = DialogUtil.loadingDialog(ac, 1);
        mTvTitle.setText("会员销卡");
        mBossTvStarttime.setText(getStringDate());
        mBossTvEndtime.setText(getStringDate());
        obtainBoss();
        et_account.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (delayRun != null) {
                    //每次editText有变化的时候，则移除上次发出的延迟线程
                    handler.removeCallbacks(delayRun);
                }
                editString = editable.toString();

                //延迟800ms，如果不再输入字符，则执行该线程的run方法
                handler.postDelayed(delayRun, 800);

            }
        });
    }


    /**
     * 延迟线程，看是否还有下一个字符输入
     */
    private Runnable delayRun = new Runnable() {

        @Override
        public void run() {
            //在这里调用服务器的接口，获取数据
            obtainBoss();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (delayRun != null) {
            //每次editText有变化的时候，则移除上次发出的延迟线程
            handler.removeCallbacks(delayRun);
        }
    }

    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    private void obtainBoss() {
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams map = new RequestParams();

        map.put("StartDate",mBossTvStarttime.getText().toString());
        map.put("EndDate",mBossTvEndtime.getText().toString());
        map.put("UserAccount",et_account.getText().toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "RptOverallDataGet");
        LogUtils.d("xxurl", url);
        client.post(url, map, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    LogUtils.d("xxyingxiaoS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
//                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_LONG).show();
                        Gson gson=new Gson();
                        Type listType = new TypeToken<List<YinxiaoCenter>>() {
                        }.getType();
                        List<YinxiaoCenter> yinxiaoCenters = gson.fromJson(jso.getString("vdata"), listType);
                        Message msg = handler.obtainMessage();
                        msg.what = 1;
                        msg.obj = yinxiaoCenters.get(0);
                        handler.sendMessage(msg);
                    } else {

                        Message msg = handler.obtainMessage();
                        msg.what = 2;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    Message msg = handler.obtainMessage();
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Message msg = handler.obtainMessage();
                msg.what = 2;
                handler.sendMessage(msg);
            }
        });


    }

    private void handlerYinxiaoMsg(YinxiaoCenter yx) {
        mTvNewvip.setText(yx.getMemCount());
        mTvYingshouheji.setText(yx.TotalMoney);
        mTvMoney.setText(yx.getRechCash());
        mTvQita.setText(yx.getRechOthePayment());
        mTvZengsong.setText(yx.getRechGiveMoney());
        mTvZhifubao.setText(yx.getRechAlipay());
        mTvWeixin.setText(yx.getRechWeChatPay());
        mTvYinlian.setText(yx.getRechUnion());
        mTvHeji.setText(yx.getRechSubtotal());

        mTvXfmoney.setText(yx.getOrderCash());
        mTvXfyue.setText(yx.getOrderBalance());
        mTvXfqita.setText(yx.getOrderOthePayment());
        mTvXfzhifubao.setText(yx.getOrderAlipay());
        mTvXfweixin.setText(yx.getOrderWeChatPay());
        mTvXfyinlian.setText(yx.getOrderUnion());
        mTvXfheji.setText(yx.getOrderSubtotal());
        tv_txhz.setText(yx.SumDrawMoney);
        tv_txzje.setText(yx.SumDrawMoney);
        tv_kczje.setText(yx.SumDrawActualMoney);
        tv_yjxj.setText(yx.DepositTotal);
        tv_zjyj.setText(yx.DepositAdd);
        tv_thyj.setText(yx.DepositReturn);

    }

    private void handlerYinxiaoMsgNull() {
        mTvNewvip.setText("");
        mTvYingshouheji.setText("");
        mTvMoney.setText("");
        mTvQita.setText("");
        mTvZengsong.setText("");
        mTvZhifubao.setText("");
        mTvWeixin.setText("");
        mTvYinlian.setText("");
        mTvHeji.setText("");

        mTvXfmoney.setText("");
        mTvXfyue.setText("");
        mTvXfqita.setText("");
        mTvXfzhifubao.setText("");
        mTvXfweixin.setText("");
        mTvXfyinlian.setText("");
        mTvXfheji.setText("");
        tv_txhz.setText("");
        tv_txzje.setText("");
        tv_kczje.setText("");
        tv_yjxj.setText("");
        tv_zjyj.setText("");
        tv_thyj.setText("");

    }
    @OnClick({R.id.rl_left, R.id.boss_tv_starttime, R.id.boss_tv_endtime})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_left:
                finish();
                break;
            case R.id.boss_tv_starttime:
                DialogUtil.dateChoseDialog(YingxiaoCenterActivity.this, 1, new InterfaceBack() {
                    @Override
                    public void onResponse(Object response) {
                        String data = DateUtils.timeTodata((String) response);
                        String cru = DateUtils.timeTodata(DateUtils.getCurrentTime_Today());
                        Log.d("xxTime", data + ";" + cru + ";" + DateUtils.getCurrentTime_Today() + ";" + (String) response);
                        if (Double.parseDouble(data) <= Double.parseDouble(cru)) {
                            mBossTvStarttime.setText((String) response);
                            obtainBoss();
                        } else {
                            Toast.makeText(ac, "开始时间应小于当前时间", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onErrorResponse(Object msg) {
                        mBossTvStarttime.setText((String) msg);
                        obtainBoss();
                    }
                });
                break;
            case R.id.boss_tv_endtime:
                DialogUtil.dateChoseDialog(YingxiaoCenterActivity.this, 1, new InterfaceBack() {
                    @Override
                    public void onResponse(Object response) {

                        String data = DateUtils.timeTodata((String) response);
                        String cru = DateUtils.timeTodata(mBossTvStarttime.getText().toString());
                        Log.d("xxTime", data + ";" + cru + ";" + DateUtils.getCurrentTime_Today() + ";" + (String) response);
                        if (Double.parseDouble(data) >= Double.parseDouble(cru)) {
                            mBossTvEndtime.setText((String) response);
                            obtainBoss();
                        } else {
                            Toast.makeText(ac, "结束时间要大于起始时间", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onErrorResponse(Object msg) {
                        mBossTvEndtime.setText((String) msg);
                        obtainBoss();
                    }
                });
                break;
        }
    }

}
