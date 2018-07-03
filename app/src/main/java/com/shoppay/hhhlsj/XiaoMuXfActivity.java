package com.shoppay.hhhlsj;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
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
import com.shoppay.hhhlsj.bean.VipInfo;
import com.shoppay.hhhlsj.card.ReadCardOptHandler;
import com.shoppay.hhhlsj.tools.BluetoothUtil;
import com.shoppay.hhhlsj.tools.DayinUtils;
import com.shoppay.hhhlsj.tools.DialogUtil;
import com.shoppay.hhhlsj.tools.LogUtils;
import com.shoppay.hhhlsj.tools.PreferenceHelper;
import com.shoppay.hhhlsj.tools.SoundPlayUtils;
import com.shoppay.hhhlsj.tools.UrlTools;
import com.shoppay.hhhlsj.wxcode.MipcaActivityCapture;

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
    private Activity ac;
    private Dialog dialog;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 8:
                    String card = (String) msg.obj;
                    viprechargeEtCard.setText(card);
                    ontainVipInfo(card);
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
        ac = this;
        dialog = DialogUtil.loadingDialog(ac, 1);
        tvTitle.setText("项目消费");
        SoundPlayUtils.init(ac);

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

    private void ontainVipInfo(String card) {
        dialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
        params.put("MemCard", card);
        params.put("UserID", PreferenceHelper.readString(ac, "shoppay", "UserID", ""));
        params.put("UserShopID", PreferenceHelper.readString(ac, "shoppay", "ShopID", ""));
        LogUtils.d("xxparams", params.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "ScanExpense");
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
                        isSuccess = true;
                        SoundPlayUtils.play(1);
                        JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
                        if (jsonObject.getInt("printNumber") == 0) {
                            finish();
                        } else {
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (bluetoothAdapter.isEnabled()) {
                                BluetoothUtil.connectBlueTooth(MyApplication.context);
                                BluetoothUtil.sendData(DayinUtils.dayin(jsonObject.getString("printContent")), jsonObject.getInt("printNumber"));
                                finish();
                            } else {
                                finish();
                            }
                        }
                    } else {
                        SoundPlayUtils.play(2);
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                        viprechargeEtName.setText("");
                        viprechargeEtYue.setText("");
                        viprechargeEtCardmian.setText("");
                        viprechargeEtDengji.setText("");
                        isSuccess = false;
                        dialog.dismiss();
                        PreferenceHelper.write(ac, "shoppay", "memid", "123");
                    }
                } catch (Exception e) {
//                 viprechargeEtName.setText("");
                    SoundPlayUtils.play(2);
                    viprechargeEtYue.setText("");
                    viprechargeEtCardmian.setText("");
                    viprechargeEtDengji.setText("");
                    isSuccess = false;
                    dialog.dismiss();
                    Toast.makeText(MyApplication.context, "结算失败，请重新结算",
                            Toast.LENGTH_SHORT).show();
                    PreferenceHelper.write(ac, "shoppay", "memid", "123");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//              viprechargeEtName.setText("");
                SoundPlayUtils.play(2);
                viprechargeEtYue.setText("");
                viprechargeEtCardmian.setText("");
                viprechargeEtDengji.setText("");
                isSuccess = false;
                dialog.dismiss();
                PreferenceHelper.write(ac, "shoppay", "memid", "123");
                Toast.makeText(MyApplication.context, "结算失败，请重新结算",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @OnClick({R.id.rl_left, R.id.rl_right, R.id.rl_hexiao})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rl_left:
                finish();
                break;
            case R.id.rl_right:
                Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                startActivityForResult(mipca, 111);
                break;
            case R.id.rl_hexiao:
                Intent hexiao = new Intent(ac, MipcaActivityCapture.class);
                startActivityForResult(hexiao, 222);
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
                    ontainVipInfo(data.getStringExtra("codedata"));
                    break;
                case 222:
                    ontainVipInfo(data.getStringExtra("codedata"));
                    break;
            }
        }
    }
}
