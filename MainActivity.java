package com.be.asiacuisine;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.IWoyouService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

	WebView mWebView;
	Bitmap mBitmap;
	JsObject mJso = new JsObject();


	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();
		// 设置编码

		mWebView.clearCache(true);

		mWebView.loadUrl("about:blank");


		mWebView.getSettings().setDefaultTextEncodingName("utf-8");
		// 支持js
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebChromeClient(new WebChromeClient());
		// 设置背景颜色 透明
		mWebView.setBackgroundColor(Color.rgb(96, 96, 96));
		mWebView.setWebViewClient(new WebViewClientDemo());//添加一个页面相应监听类
		// 载入包含js的html
		mWebView.loadData("", "text/html", null);
		mWebView.loadUrl("http://asiacuisine.be/datainterface/index-v2.php?restaurant_id=8&language=zh");


		Intent intent = new Intent();
		intent.setPackage("woyou.aidlservice.jiuiv5");
		intent.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
		startService(intent);//启动打印服务
		bindService(intent, connService, Context.BIND_AUTO_CREATE);

		mJso.startResetTimer(180);
	}

	class WebViewClientDemo extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// 当打开新链接时，使用当前的 WebView，不会使用系统其他浏览器
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			/**
			 * 注册JavascriptInterface，其中"lee"的名字随便取，如果你用"lee"，那么在html中只要用  lee.方法名()
			 * 即可调用MyJavascriptInterface里的同名方法，参数也要一致
			 */
			mWebView.addJavascriptInterface(mJso, "lee");
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			mWebView.loadUrl("file:///android_asset/404.html");

		}

	}

	class JsObject {
		private MediaPlayer mediaPlayer;
		private Boolean isPlayMedia = false;
		private int mResetTimeout = 0;
		private boolean mIsTimerStarted = false;
		private long mTimerStartTime = 0;
		@JavascriptInterface
		public void funAndroid(final String i, final String j, final String k, final String l) {
			Toast.makeText(getApplicationContext(), "打印通知：有新预订或确认的消息",	Toast.LENGTH_SHORT).show();

			if( mBitmap == null ){
				mBitmap = BitmapFactory.decodeResource(getResources(), R.raw.logo);
			}
			if (!l.equals("ConfirmDeliveryTime")) {//打印餐馆确定的送餐时间，不需要再打印图标
				try {
					woyouService.setAlignment(1, callback);
					woyouService.printBitmap(mBitmap, callback);
					// woyouService.printBitmap(mBitmap2, callback);
					woyouService.lineWrap(3, null);
				} catch (RemoteException e) {
					//
					e.printStackTrace();
				}
			}


			try {
				//woyouService.printerSelfChecking(callback);//这里使用的AIDL方式打印
				woyouService.setAlignment(0, callback);
				woyouService.setFontSize(30, callback);// float fontsize, in ICallback callback);
				woyouService.printText(i, callback);

				woyouService.setAlignment(1, callback);
				woyouService.printText(j, callback);

				if (l.equals("Chinese"))//l代表languange
				{
					woyouService.setFontSize(38, callback);// float fontsize, in ICallback callback);
				}
				woyouService.setAlignment(0, callback);
				woyouService.printText(k, callback);

				woyouService.lineWrap(3, null);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@JavascriptInterface
		//播放音乐
		public void playSoundNoti(final String soundName) {
			stopSoundNoti();
			//防止重复播放
			synchronized (this)
			{
				if(!isPlayMedia) {
					Toast.makeText(getApplicationContext(), "New order, please check! 有新预订, 请查看!",	Toast.LENGTH_SHORT).show();
					//请在这里添加播放声音的代码
					isPlayMedia = true;
					mediaPlayer = null;
					//根据soundName 选择播放资源。  默认oppo
					int soundID = R.raw.oppo;
					if (soundName == null);
					else if (soundName.equals("bar.mp3"))
					{
						soundID = R.raw.bar;
					}
					else if (soundName.equals("iphone.mp3"))
					{
						soundID = R.raw.iphone;
					}
					else if (soundName.equals("oppo.mp3"))
					{
						soundID = R.raw.oppo;
					}
					else if (soundName.equals("ringtone.mp3"))
					{
						soundID = R.raw.ringtone;
					}
					mediaPlayer = MediaPlayer.create(MainActivity.this, soundID);
					mediaPlayer.setLooping(false); //是否需要重复播放
					mediaPlayer.start();
					mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mp) {
							Log.d("tag", "播放完毕");
							//根据需要添加自己的代码。。。
							mp.stop();
							isPlayMedia = false;
							mediaPlayer = null;
						}
					});
				}
			}
		}
		@JavascriptInterface
		//停止播放   短音乐无需调用
		public void stopSoundNoti() {
			if (mediaPlayer != null && isPlayMedia == true)
			{
				mediaPlayer.stop();
				isPlayMedia = false;
			}
		}


		@JavascriptInterface
		public void resetBrowser() {
			StopResetTimer();
			mTimerStartTime = System.currentTimeMillis();//重置超时时间
			//Toast.makeText(getApplicationContext(), "refresh clicked3",	Toast.LENGTH_SHORT).show();
			finish();
			startActivity(getIntent());
		}

		@JavascriptInterface
		public void checkBrowserActive() {
			//网站每 一段时间 会调用一次这个函数
			//如果超过3分钟没有调用这个函数，则 调用上面的 resetBrowser() 刷新程序
			mTimerStartTime = System.currentTimeMillis();//重置超时时间
		}
		//开启定时器函数，并设置超时时间   timeout 单位s
		//重复调用修改超时时间
		@JavascriptInterface
		public void startResetTimer(int timeout) {
			//重复调用设置超时时间
			mResetTimeout = timeout * 1000;  //单位是毫秒
			if(mIsTimerStarted)
			{
				return ;
			}
			mIsTimerStarted=true;
			mTimerStartTime = System.currentTimeMillis();
			Thread thread = new Thread(){
				public void run(){
//					System.out.println("Thread Running");
					while(mIsTimerStarted)
					{
						if(System.currentTimeMillis() - mTimerStartTime > mResetTimeout)
						{
							resetBrowser();
						}
						try {
							Thread.sleep(1000);  //判读间隔时间1s
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			thread.start();
		}
		//关闭定时监控
		@JavascriptInterface
		public void StopResetTimer() {
			mIsTimerStarted = false;
		}
	}

	public void initViews() {
		mWebView = (WebView) findViewById(R.id.wv_view);
	}

	private IWoyouService woyouService;

	private ServiceConnection connService = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			woyouService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			woyouService = IWoyouService.Stub.asInterface(service);
		}
	};

	ICallback callback = new ICallback.Stub() {

		@Override
		public void onRunResult(boolean success) throws RemoteException {
		}

		@Override
		public void onReturnString(final String value) throws RemoteException {
		}

		@Override
		public void onRaiseException(int code, final String msg)
				throws RemoteException {
		}
	};

}
