package com.jonma.lrhealth;

import java.util.HashMap;

import com.jinoux.android.bledatawarehouse.BluetoothService;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.widget.Toast;

public class BleTool {
	private BluetoothAdapter bluetoothAdapter;
	private Context context;
	private BleScanCallBack m_bleScanCallBack;
	private BleServiceCallBack m_bleServiceCallBack;
	private BluetoothService m_bluetoothService;
	private BleConnectCallBack m_bleConnectCallBack;
	private static boolean connectstate = false; // 连接匹配状态（false:未开始连接）
	private Handler mHandler = new Handler();	
	private static final String LOGTAG = "test";

	public BleTool(Context context) {
		super();
		// TODO Auto-generated constructor stub
		this.context = context;

	}

	/* bl open and scan */
	/**
	 * 
	 * @return 0:successful
	 */
	public int openBle() {
		BluetoothManager bluetoothManager = (BluetoothManager) context
				.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			BluetoothAdapter.getDefaultAdapter().enable();
		}
		return 0;
	}

	/**
	 * 
	 * @param bleScanCallBack
	 * @param period scan time. stop scanning automatically when the time is up.
	 * @return
	 */
	public int startScan(BleScanCallBack bleScanCallBack, long period) {
		if(period == 0){
			period = 1000;
		}
		
		if (bluetoothAdapter == null) {
			return -1;
		}
		
		
		mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            	//bluetoothAdapter.stopLeScan(mLeScanCallback);
            	if(LRHealthApp.getInstance().scanIsDevice == 0){
            		AlertDialog dialog = new AlertDialog.Builder(context)
					.setMessage(context.getResources().getString(R.string.scanNoDevice))
					.setNegativeButton(context.getResources().getString(R.string.text_cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {

								}
							}).show();
//            		Window window = dialog.getWindow();
//            		window.setGravity(Gravity.CENTER);
//            		window.requestFeature(Window.FEATURE_CUSTOM_TITLE);
//            		window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
//            		dialog.show();
            	}
            }
        }, period);
		
		bluetoothAdapter.startLeScan(mLeScanCallback);
		m_bleScanCallBack = bleScanCallBack;
		return 0;
	}

	// Device scan callback.搜索到设备则执行 扫描通过回调告知哪个devie被扫描到
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			// boolean flag = true;
			if (m_bleScanCallBack != null) {
				m_bleScanCallBack.scanListening(device);
			}
		}
	};

	public interface BleScanCallBack {
		/**
		 * 
		 * @param device
		 *            the devie that have been founded.
		 */
		public void scanListening(BluetoothDevice device);

	}

	/* bl service */
	/**
	 * connect() can be called only when service is created successfully.
	 * 
	 * @param bleServiceCallBack
	 */
	@SuppressLint("InlinedApi")
	public void service_init(BleServiceCallBack bleServiceCallBack) {
		Intent gattServiceIntent = new Intent(context, BluetoothService.class);
		boolean bll = context.bindService(gattServiceIntent,
				mServiceConnection, context.BIND_AUTO_CREATE);
		if (bll) {
			Log.i(LOGTAG, "绑定服务gattServiceIntent成功");
		} else {
			Log.i(LOGTAG, "绑定服务gattServiceIntent失败");
		}
		//context.registerReceiver(mGattUpdateReceiver,	makeGattUpdateIntentFilter());
		if(m_bluetoothService == null){
			registerReceiver();
		}

		m_bleServiceCallBack = bleServiceCallBack;
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
		intentFilter
				.addAction(BluetoothService.ACTION_GATT_READCHARACTERISTICSUCCESS);
		return intentFilter;
	}

	// 通过回调通知service建立成功
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			m_bluetoothService = ((BluetoothService.LocalBinder) service)
					.getService();
			if (m_bluetoothService == null) {
				Log.d(LOGTAG, "m_bluetoothService is null");
			} else {
				Log.d(LOGTAG, "m_bluetoothService is not null");
				m_bleServiceCallBack.onBuild();

			}
			boolean ba = m_bluetoothService.initialize();
			if (!ba) {
				Log.i(LOGTAG, "Unable to initialize Bluetooth");
			} else {
				Log.i(LOGTAG, "initialize Bluetooth");
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			m_bluetoothService = null;
		}
	};

	public interface BleServiceCallBack {
		/**
		 * when service is created successfully, call this call back funciton
		 */
		public void onBuild();
	}

	/* connect */
	public void connect(String macAddr, BleConnectCallBack bleConnectCallBack) {
		// TODO Auto-generated method stub
		m_bluetoothService.gethandler(deviceHandler);
		Log.d(LOGTAG, "connect:"+macAddr);
		m_bluetoothService.connect(macAddr);
		m_bleConnectCallBack = bleConnectCallBack;
	}
	
	public void disconnect() {
		Log.i(LOGTAG, "disconnect");
		m_bluetoothService.disconnect();
	}
	

	// 接收广播 sevice 通过接收广播来知道是否连接成功，handler还可以根据连接是否成功做出相应动作
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			Log.i(LOGTAG, "action = " + action);
			if (BluetoothService.ACTION_GATT_READCHARACTERISTICSUCCESS
					.equals(action)) { // 连接成功 并读取characteristic成功
				// String connecttext = "disconnect";
				// connectButton.setText(connecttext);
				// ConnectProgressBarzt(false);
				if(m_bleConnectCallBack != null){
					m_bleConnectCallBack.onConnect();
				}
				connectstate = true;
			} else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) { // 模块已断开连接
				if (connectstate == true) { // 正在连接中
					// showAlertDialog(
					// "模块已关闭连接，请断开!",
					// getResources().getString(
					// R.string.alertOneButtonTitle), null, 0);
					Log.d(LOGTAG, "模块已关闭连接，请断开!");
					if(m_bleConnectCallBack != null){
						m_bleConnectCallBack.onConnectFailed();
					}
				}
			}
		}
	};

	public interface BleConnectCallBack {
		/**
		 * when connect successfully, call this call back funciton
		 */
		public void onConnect();
		
		public void onConnectFailed();
	}

	/* handler of service */
	@SuppressLint("HandlerLeak")
	public Handler deviceHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			// Log.i("what === "+msg.what);
			switch (msg.what) {
			case 0:
				Log.i(LOGTAG, "连接失败");
				connectstate = false;
				if(m_bleConnectCallBack != null){
					m_bleConnectCallBack.onConnectFailed();
				}
				break;
			case 1:
				String str = (String) msg.obj;
				// str = Tools.bytesToHexString(str.getBytes());
				Log.d(LOGTAG, "received data:" + str);
				break;
			case 2:
				// String ss = (String) msg.obj;
				// receivetop_text.setText("接收数据：已接收 "+ss+" 个字节");
				break;
			default:
				break;
			}
		}
	};

	public int stopScan() {
		if (bluetoothAdapter == null) {
			return -1;
		}

		bluetoothAdapter.stopLeScan(mLeScanCallback);
		return 0;
	}

	public BluetoothService getBleService() {
		return m_bluetoothService;
	}

	public void unregisterReceiver(){
		context.unregisterReceiver(mGattUpdateReceiver);
	}
	
	public void unbindService() {		
		context.unbindService(mServiceConnection);
	}
	
	public void registerReceiver(){
		context.registerReceiver(mGattUpdateReceiver,
				makeGattUpdateIntentFilter());
	}
}
