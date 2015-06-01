package com.jonma.lrhealth;

import java.util.HashMap;

import com.jinoux.android.bledatawarehouse.BluetoothService;
import com.jonma.tool.CustomDialog;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class BleTool {
	private Context context;
	private BleScanCallBack m_bleScanCallBack;
	private BleServiceCallBack m_bleServiceCallBack;
	private BluetoothService m_bluetoothService;
	private BleConnectCallBack m_bleConnectCallBack;
	private BleConnectCallBackWhenSenddata m_bleConnectCallBackWhenSenddata;

//	private static boolean application.connectStatus = false; // ����ƥ��״̬��false:δ��ʼ���ӣ�
	private boolean connectIsUncon = false;
	private Handler mHandler = new Handler();	
	private static final String LOGTAG = "###";
	private LRHealthApp application; 
	
	private long connectTime = 0;
	private Handler con_Handler = new Handler();

	public BleTool(Context context) {
		super();
		// TODO Auto-generated constructor stub
		this.context = context;
		application = LRHealthApp.getInstance();
	}

	/* bl open and scan */
	/**
	 * 
	 * @return 0:successful
	 */
	public int openBle() {
		application.bluetoothManager = (BluetoothManager) context
				.getSystemService(Context.BLUETOOTH_SERVICE);
		application.bluetoothAdapter = application.bluetoothManager.getAdapter();
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			BluetoothAdapter.getDefaultAdapter().enable();
			return 1;//new open
		}
		return 0;//have opend
	}
	
	public Boolean isOpened() {
		return BluetoothAdapter.getDefaultAdapter().isEnabled();		
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
		
		if (application.bluetoothAdapter == null) {
			return -1;
		}
		
		m_bleScanCallBack = bleScanCallBack;		
		
		mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            	//application.bluetoothAdapter.stopLeScan(mLeScanCallback);
            	//Log.i("===", "scanButtionClickTimes:" + LRHealthApp.getInstance().scanButtionClickTimes + "scanIsDevice:" + LRHealthApp.getInstance().scanIsDevice);
            	if(LRHealthApp.getInstance().scanIsDevice == 0 && LRHealthApp.getInstance().scanButtionClickTimes <= 1){
            		m_bleScanCallBack.scanNoDevice();
            		
            		LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);    
            		final View loginLayout = inflater.inflate(R.layout.dialoggeneral, null);            		
            		CustomDialog.Builder customBuilder = new CustomDialog.Builder(context);
            		customBuilder.setView(loginLayout)
            			.setMessage(context.getResources().getString(R.string.scanNoDevice))
            			.setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener(){
            				public void onClick(DialogInterface dialog, int which) {
            					//TODO:
            					dialog.dismiss(); 
            				}
            			});
            		Dialog scanDialog = customBuilder.create();
            		scanDialog.show();
            	}
            	
            	LRHealthApp.getInstance().scanButtionClickTimes--;
            }
        }, period);
		
		application.bluetoothAdapter.startLeScan(mLeScanCallback);
		
		return 0;
	}
	
	public int reStartScan() {	
		if (application.bluetoothAdapter == null) {
			return -1;
		}	
		
		application.bluetoothAdapter.startLeScan(mLeScanCallback);
		
		return 0;
	}
	
	public int firstStartScan() {	
		if (application.bluetoothAdapter == null) {
			return -1;
		}	
		
		application.bluetoothAdapter.startLeScan(mLeScanCallback);
		
		return 0;
	}

	// Device scan callback.�������豸��ִ�� ɨ��ͨ���ص���֪�ĸ�devie��ɨ�赽
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
		
		public void scanNoDevice();

	}

	/* bl service */
	/**
	 * connect() can be called only when service is created successfully.
	 * 
	 * @param bleServiceCallBack
	 */
	@SuppressLint("InlinedApi")
	public void service_init(BleServiceCallBack bleServiceCallBack) {
		Log.i("===", "service_init");
		m_bleServiceCallBack = bleServiceCallBack;

		Intent gattServiceIntent = new Intent(context, BluetoothService.class);
		boolean bll = context.bindService(gattServiceIntent,
				mServiceConnection, context.BIND_AUTO_CREATE);
		if (bll) {
			//Log.i(LOGTAG, "�󶨷���gattServiceIntent�ɹ�");
		} else {
			//Log.i(LOGTAG, "�󶨷���gattServiceIntentʧ��");
		}
		//context.registerReceiver(mGattUpdateReceiver,	makeGattUpdateIntentFilter());
		if(m_bluetoothService == null){
			registerReceiver();
		}

		if(m_bleServiceCallBack == null){
			Log.i("@@@init", "m_bleServiceCallBack null");
		}else{
			Log.i("@@@init", "m_bleServiceCallBack not null");

		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
		intentFilter
				.addAction(BluetoothService.ACTION_GATT_READCHARACTERISTICSUCCESS);
		return intentFilter;
	}

	// ͨ���ص�֪ͨservice�����ɹ�
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
				if(m_bleServiceCallBack == null){
					Log.i("***", "m_bleServiceCallBack null");
				}
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
		if (application.bluetoothAdapter == null) {
			application.bluetoothAdapter = application.bluetoothManager.getAdapter();
			return;
		}
		
		stopScan();
		
		m_bluetoothService.gethandler(deviceHandler);
		Log.d(LOGTAG, "connect:"+macAddr);
		m_bluetoothService.connect(macAddr);
		connectTime = System.currentTimeMillis();
		m_bleConnectCallBack = bleConnectCallBack;
		
		connectIsUncon = false;
		Log.i("===", "new connect");

		con_Handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            	Log.i("===", "time: " + (System.currentTimeMillis()-connectTime));
            	if(application.connectStatus == false && ( (System.currentTimeMillis()-connectTime) >= 15000)){
            		if(m_bleConnectCallBack != null){
            			Log.i("!!!", "connect timeout");
            			//disconnect();
						//m_bleConnectCallBack.onConnectFailed();
            			m_bleConnectCallBack.onConnectTimeout();
					}
            	}
            	application.reConnStatusNum--;
            }
        }, 15000);
	}
	
	public void reConnectWhenSenddata(String macAddr, BleConnectCallBackWhenSenddata bleConnectCallBackWhenSenddata) {
//		if (application.bluetoothAdapter == null) {
//			application.bluetoothAdapter = application.bluetoothManager.getAdapter();
//			return;
//		}
	
		Log.d(LOGTAG, "connect:"+macAddr);
		m_bluetoothService.connect(macAddr);
		m_bleConnectCallBackWhenSenddata = bleConnectCallBackWhenSenddata;
	}
	
	public void disconnectReSenddata(BleConnectCallBackWhenSenddata bleConnectCallBackWhenSenddata) {
		Log.i(LOGTAG, "disconnect");
		
//		application.bluetoothManager = (BluetoothManager) context
//				.getSystemService(Context.BLUETOOTH_SERVICE);
//		application.bluetoothAdapter = application.bluetoothManager.getAdapter();
//		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
//			BluetoothAdapter.getDefaultAdapter().enable();
//		}
		
		m_bleConnectCallBackWhenSenddata = bleConnectCallBackWhenSenddata;
		connectIsUncon = true;
		m_bluetoothService.disconnect();
		connectTime = System.currentTimeMillis();
		Log.i("===", "re-set time");
	}
	
	public void disconnect() {
		Log.i(LOGTAG, "disconnect");
		connectIsUncon = true;
		m_bluetoothService.disconnect();
		connectTime = System.currentTimeMillis();
		Log.i("===", "re-set time");
	}	

	// ���չ㲥 sevice ͨ�����չ㲥��֪���Ƿ����ӳɹ���handler�����Ը��������Ƿ�ɹ�������Ӧ����
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			Log.i("$$$", "action = " + action);
			if (BluetoothService.ACTION_GATT_READCHARACTERISTICSUCCESS
					.equals(action)) { // ���ӳɹ� ����ȡcharacteristic�ɹ�
				// String connecttext = "disconnect";
				// connectButton.setText(connecttext);
				// ConnectProgressBarzt(false);
				if(m_bleConnectCallBack != null){
					m_bleConnectCallBack.onConnect();
					if(application.reSendEn){						
						m_bleConnectCallBackWhenSenddata.onConnect();
						application.reSendEn = false;
					}
				}
				application.connectStatus = true;
			} else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) { // ģ���ѶϿ�����
				Log.i("test", (application.connectStatus == true)?"true":"false");
				connectIsUncon = true;
				//if (application.connectStatus == true) { // ����������
					// showAlertDialog(
					// "ģ���ѹر����ӣ���Ͽ�!",
					// getResources().getString(
					// R.string.alertOneButtonTitle), null, 0);
					Log.d(LOGTAG, "ģ���ѹر����ӣ���Ͽ�!");
					if(m_bleConnectCallBack != null){
						m_bleConnectCallBack.onDisconnect();
						if(application.reSendEn){						
							m_bleConnectCallBackWhenSenddata.onDisconnect();
							application.reSendEn = false;
						}
					}
				//}
			}
		}
	};

	public interface BleConnectCallBack {
		/**
		 * when connect successfully, call this call back funciton
		 */
		public void onConnect();
		
		public void onConnectFailed();
		
		public void onDisconnect();
		
		public void onConnectTimeout();

	}
	
	public interface BleConnectCallBackWhenSenddata {
		/**
		 * when connect successfully, call this call back funciton
		 */
		public void onConnect();
		public void onDisconnect();
	}

	/* handler of ble service */
	@SuppressLint("HandlerLeak")
	public Handler deviceHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.d("$$$", "msg:" + msg.what);
			switch (msg.what) {
			case 0:
				Log.i(LOGTAG, "����ʧ��");
				connectIsUncon = true;
				application.connectStatus = false;
				if(m_bleConnectCallBack != null){
					connectTime = System.currentTimeMillis();
					Log.i("===", "re-set time");
					m_bleConnectCallBack.onConnectFailed();
				}
				break;
			case 1://send data call back
				String str = (String) msg.obj;
				// str = Tools.bytesToHexString(str.getBytes());
				Log.d(LOGTAG, "received data:" + str);
				application.sendStatusBoolean= true; 
				break;
			case 2:
				// String ss = (String) msg.obj;
				// receivetop_text.setText("�������ݣ��ѽ��� "+ss+" ���ֽ�");
				break;
			default:
				break;
			}
		}
	};

	public int stopScan() {
		Log.i("===", "stop scan");
		if (application.bluetoothAdapter == null) {
			return -1;
		}

		application.bluetoothAdapter.stopLeScan(mLeScanCallback);
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
