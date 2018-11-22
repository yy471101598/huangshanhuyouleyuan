package com.shoppay.hshyly;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.shoppay.hshyly.bean.VipInfo;
import com.shoppay.hshyly.bean.VipInfoMsg;
import com.shoppay.hshyly.card.ReadCardOptHandler;
import com.shoppay.hshyly.tools.BluetoothUtil;
import com.shoppay.hshyly.tools.DayinUtils;
import com.shoppay.hshyly.tools.DialogUtil;
import com.shoppay.hshyly.tools.LogUtils;
import com.shoppay.hshyly.tools.NoDoubleClickListener;
import com.shoppay.hshyly.tools.PreferenceHelper;
import com.shoppay.hshyly.tools.SoundPlayUtils;
import com.shoppay.hshyly.tools.UrlTools;
import com.shoppay.hshyly.wxcode.MipcaActivityCapture;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;

/**
 * Created by Administrator on 2018/7/2 0002.
 */

public class XiaoMuXfActivity extends Activity {
    @Bind(R.id.rl_left)
    RelativeLayout rlLeft;
    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(R.id.rl_right)
    RelativeLayout rlRight;
    @Bind(R.id.viprecharge_tv_cardnum)
    TextView viprechargeTvCardnum;
    @Bind(R.id.viprecharge_et_card)
    TextView viprechargeEtCard;
    @Bind(R.id.viprecharge_tv_cardmi)
    TextView viprechargeTvCardmi;
    @Bind(R.id.viprecharge_et_cardmian)
    TextView viprechargeEtCardmian;
    @Bind(R.id.viprecharge_tv_name)
    TextView viprechargeTvName;
    @Bind(R.id.viprecharge_et_name)
    TextView viprechargeEtName;
    @Bind(R.id.viprecharge_tv_yue)
    TextView viprechargeTvYue;
    @Bind(R.id.viprecharge_et_yue)
    TextView viprechargeEtYue;
    @Bind(R.id.viprecharge_tv_dengji)
    TextView viprechargeTvDengji;
    @Bind(R.id.viprecharge_et_dengji)
    TextView viprechargeEtDengji;
    @Bind(R.id.rl_hexiao)
    RelativeLayout rlHexiao;
    @Bind(R.id.viprecharge_et_xmname)
    TextView mViprechargeEtXmname;
    @Bind(R.id.rl_add)
    RelativeLayout img_add;
    @Bind(R.id.rl_del)
    RelativeLayout img_del;
    @Bind(R.id.tv_xmnum)
    TextView et_xmnum;
    @Bind(R.id.rl_confirm)
    RelativeLayout rl_confirm;
    private int xmnum = 1;
    private boolean isRunning = false;
    private boolean isdetory = false;
    private Activity ac;
    private Dialog dialog;
    private String cardNum = "";
    long firstTime = 0;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    VipInfo info = (VipInfo) msg.obj;
                    viprechargeEtCard.setText(info.getMemCard());
                    viprechargeEtName.setText(info.getMemName());
                    viprechargeEtYue.setText(info.getMemMoney());
                    viprechargeEtCardmian.setText(info.MemCardNumber);
                    viprechargeEtDengji.setText(info.getLevelName());
                    PreferenceHelper.write(ac, "shoppay", "memid", info.getMemID());
                    isSuccess = true;
                    break;
                case 2:
                    viprechargeEtName.setText("");
                    viprechargeEtYue.setText("");
                    viprechargeEtCardmian.setText("");
                    viprechargeEtDengji.setText("");
                    isSuccess = false;
                    break;

                case 8:
                    String card = (String) msg.obj;
                    viprechargeEtCard.setText(card);
                    obtainVipInfo(card);
//                    if (!isdetory) {
//                        if (!isRunning) {
//                            String card = (String) msg.obj;
//                            if (cardNum.equals(card)) {
//                                long secndTime = System.currentTimeMillis();
//                                if (secndTime - firstTime > 5000) {
//                                    firstTime = secndTime;
//                                    viprechargeEtCard.setText(card);
//                                    ontainVipInfo(card);
//                                }
//
//                            } else {
//                                viprechargeEtCard.setText(card);
//                                ontainVipInfo(card);
//                            }
//                        }
//                    }
                    break;
            }
        }
    };
    private boolean isSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xmxiaofei);
        ButterKnife.bind(this);
        isRunning = false;
        isdetory = false;
        cardNum = "";
        firstTime = 0;
        ac = this;
        dialog = DialogUtil.loadingDialog(ac, 1);
        tvTitle.setText("项目消费");
        SoundPlayUtils.init(ac);
        String bindGoods = PreferenceHelper.readString(ac, "shoppay", "BindGoods", "");
        mViprechargeEtXmname.setText(bindGoods);

        rlRight.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View view) {
                Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                startActivityForResult(mipca, 111);
            }
        });

        rlHexiao.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View view) {
                Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                startActivityForResult(mipca, 222);
            }
        });
        rl_confirm.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View view) {
                if (isSuccess) {
                    ontainVipInfo(viprechargeEtCard.getText().toString(), false);
                } else {
                    Toast.makeText(ac, "会员信息不正确", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        new ReadCardOptHandler(handler);
    }

    @Override
    protected void onStop() {
        try {
            new ReadCardOptHandler().overReadCard();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isdetory = true;
    }


    private void obtainVipInfo(String card) {
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
                    LogUtils.d("xxVipinfoS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
                        dialog.dismiss();
                        Gson gson = new Gson();
                        VipInfoMsg infomsg = gson.fromJson(new String(responseBody, "UTF-8"), VipInfoMsg.class);
                        Message msg = handler.obtainMessage();
                        msg.what = 1;
                        msg.obj = infomsg.getVdata().get(0);
                        handler.sendMessage(msg);
                    } else {
                        dialog.dismiss();
                        Message msg = handler.obtainMessage();
                        msg.what = 2;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    dialog.dismiss();
                    Message msg = handler.obtainMessage();
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                dialog.dismiss();
                Message msg = handler.obtainMessage();
                msg.what = 2;
                handler.sendMessage(msg);
            }
        });
    }

    private void ontainVipInfo(String card, final boolean isHexiao) {
        dialog.show();
        firstTime = System.currentTimeMillis();
        cardNum = card;
        isRunning = true;
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
        params.put("MemCard", card);
        params.put("UserID", PreferenceHelper.readString(ac, "shoppay", "UserID", ""));
        params.put("UserShopID", PreferenceHelper.readString(ac, "shoppay", "ShopID", ""));
        if (isHexiao) {
            params.put("number", 1);
        } else {
            params.put("number", xmnum);
        }
        LogUtils.d("xxparams", params.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "ScanExpense");
        LogUtils.d("xxurl", url);
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    isRunning = false;
                    Log.d("xxXfS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
                        dialog.dismiss();
                        if (isHexiao) {
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<VipInfo>>() {
                            }.getType();
                            List<VipInfo> list = gson.fromJson(jso.getString("vdata"), listType);
                            VipInfo info = list.get(0);
                            viprechargeEtCard.setText(info.getMemCard());
                            viprechargeEtName.setText(info.getMemName());
                            viprechargeEtYue.setText(info.getMemMoney());
                            viprechargeEtCardmian.setText(info.MemCardNumber);
                            viprechargeEtDengji.setText(info.getLevelName());
                            PreferenceHelper.write(ac, "shoppay", "memid", info.getMemID());
                        }
                        SoundPlayUtils.play(1);
                        JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
                        if (jsonObject.getInt("printNumber") == 0) {
//                            finish();
                        } else {
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (bluetoothAdapter.isEnabled()) {
                                BluetoothUtil.connectBlueTooth(XiaoMuXfActivity.this);
                                BluetoothUtil.sendData(DayinUtils.dayin(jsonObject.getString("printContent")), jsonObject.getInt("printNumber"));
//                                finish();
                            } else {
//                                finish();
                            }
                        }
                        //成功后处理
                        isSuccess = false;
                        viprechargeEtCard.setText("");
                        viprechargeEtName.setText("");
                        viprechargeEtYue.setText("");
                        viprechargeEtCardmian.setText("");
                        viprechargeEtDengji.setText("");
                        xmnum = 1;
                        et_xmnum.setText("1");
                    } else {
                        SoundPlayUtils.play(2);
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                        if (isHexiao) {
                            viprechargeEtName.setText("");
                            viprechargeEtYue.setText("");
                            viprechargeEtCardmian.setText("");
                            viprechargeEtDengji.setText("");
                            PreferenceHelper.write(ac, "shoppay", "memid", "123");
                        }
                        dialog.dismiss();
                    }
                } catch (Exception e) {
//                 viprechargeEtName.setText("");
                    isRunning = false;
                    SoundPlayUtils.play(2);
                    if (isHexiao) {
                        viprechargeEtName.setText("");
                        viprechargeEtYue.setText("");
                        viprechargeEtCardmian.setText("");
                        viprechargeEtDengji.setText("");
                        PreferenceHelper.write(ac, "shoppay", "memid", "123");
                    }
                    dialog.dismiss();
                    Toast.makeText(MyApplication.context, "结算失败，请重新结算",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//              viprechargeEtName.setText("");
                SoundPlayUtils.play(2);
                isRunning = false;
                if (isHexiao) {
                    viprechargeEtName.setText("");
                    viprechargeEtYue.setText("");
                    viprechargeEtCardmian.setText("");
                    viprechargeEtDengji.setText("");
                    PreferenceHelper.write(ac, "shoppay", "memid", "123");
                }
                dialog.dismiss();
                Toast.makeText(MyApplication.context, "结算失败，请重新结算",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @OnClick({R.id.rl_left, R.id.rl_del, R.id.rl_add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_left:
                finish();
                break;
            case R.id.rl_add:
                xmnum = xmnum + 1;
                et_xmnum.setText(xmnum + "");


                break;
            case R.id.rl_del:
                if (xmnum == 1) {
                    xmnum = 1;
                    et_xmnum.setText(xmnum + "");
                } else {
                    xmnum = xmnum - 1;
                    et_xmnum.setText(xmnum + "");
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 111:
                    viprechargeEtCard.setText(data.getStringExtra("codedata"));
                    obtainVipInfo(data.getStringExtra("codedata"));
                    break;
                case 222:
                    ontainVipInfo(data.getStringExtra("codedata"), true);
                    break;
            }
        }
    }
}
