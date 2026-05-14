package com.egogame.vehiclehailer.action;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Map;

/**
 * 发送任意Intent动作 — 通用扩展机制
 *
 * 支持动态构建任意Intent，可设置：
 * - Action
 * - Package + Class（显式Intent）
 * - Data URI
 * - Extra参数（支持String/Integer/Boolean/Map）
 * - 多种发送方式（startActivity / startService / sendBroadcast）
 */
public class SendIntentAction extends VehicleActionBase {

    private static final String TAG = "SendIntentAction";

    /** 发送方式 */
    public enum DeliveryMethod {
        START_ACTIVITY,
        START_SERVICE,
        SEND_BROADCAST
    }

    // 构造参数
    private String intentAction;
    private String targetPackage;
    private String targetClass;
    private String dataUri;
    private DeliveryMethod deliveryMethod = DeliveryMethod.START_SERVICE;
    private Map<String, Object> extras;
    private int[] flags;

    // 内部缓存
    private Context context;

    public SendIntentAction(Context context, String intentAction, DeliveryMethod method) {
        super(ActionType.SEND_INTENT.name());
        this.context = context;
        this.intentAction = intentAction;
        this.deliveryMethod = method != null ? method : DeliveryMethod.START_SERVICE;
    }

    public SendIntentAction(Context context, String intentAction, String targetPackage,
                            String targetClass, DeliveryMethod method) {
        this(context, intentAction, method);
        this.targetPackage = targetPackage;
        this.targetClass = targetClass;
    }

    public SendIntentAction setDataUri(String dataUri) {
        this.dataUri = dataUri;
        return this;
    }

    public SendIntentAction setExtras(Map<String, Object> extras) {
        this.extras = extras;
        return this;
    }

    public SendIntentAction setFlags(int... flags) {
        this.flags = flags;
        return this;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SEND_INTENT;
    }

    @Override
    public String getDescription() {
        return "发送Intent: " + (intentAction != null ? intentAction : "无Action");
    }

    @Override
    public boolean execute() {
        if (context == null || TextUtils.isEmpty(intentAction)) {
            Log.e(TAG, "执行失败: context或action为空");
            return false;
        }

        try {
            Intent intent = new Intent();

            if (!TextUtils.isEmpty(intentAction)) {
                intent.setAction(intentAction);
            }

            if (!TextUtils.isEmpty(targetPackage) && !TextUtils.isEmpty(targetClass)) {
                intent.setComponent(new ComponentName(targetPackage, targetClass));
            }

            if (!TextUtils.isEmpty(dataUri)) {
                intent.setData(Uri.parse(dataUri));
            }

            putExtras(intent, extras);

            if (flags != null && flags.length > 0) {
                for (int flag : flags) {
                    intent.addFlags(flag);
                }
            }

            switch (deliveryMethod) {
                case START_ACTIVITY:
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    break;
                case START_SERVICE:
                    context.startService(intent);
                    break;
                case SEND_BROADCAST:
                    context.sendBroadcast(intent);
                    break;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "发送Intent失败: action=" + intentAction
                    + ", pkg=" + targetPackage, e);
            return false;
        }
    }

    private void putExtras(Intent intent, Map<String, Object> extras) {
        if (extras == null || extras.isEmpty()) return;

        for (Map.Entry<String, Object> entry : extras.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) continue;

            if (value instanceof String) {
                intent.putExtra(key, (String) value);
            } else if (value instanceof Integer) {
                intent.putExtra(key, (Integer) value);
            } else if (value instanceof Boolean) {
                intent.putExtra(key, (Boolean) value);
            } else if (value instanceof Long) {
                intent.putExtra(key, (Long) value);
            } else if (value instanceof Float) {
                intent.putExtra(key, (Float) value);
            } else if (value instanceof Double) {
                intent.putExtra(key, (Double) value);
            } else {
                Log.w(TAG, "不支持的Extra类型: " + value.getClass().getName() + " for key=" + key);
            }
        }
    }
}
