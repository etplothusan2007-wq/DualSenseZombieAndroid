package com.example.dualsensezombie

import android.app.*
import android.content.*
import android.hardware.usb.*
import android.os.*
import android.view.*
import android.widget.*
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var usb: UsbManager
    private var conn: UsbDeviceConnection? = null
    private var hid: UsbInterface? = null
    private lateinit var status: TextView
    private lateinit var arena: Arena
    private val permissionAction = "com.example.dualsensezombie.USB_PERMISSION"
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action != permissionAction) return
            val d = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    else @Suppress("DEPRECATION") i.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            if (i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && d != null) connect(d)
            else status.text = "USB permission denied"
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usb = getSystemService(USB_SERVICE) as UsbManager
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, IntentFilter(permissionAction), RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(receiver, IntentFilter(permissionAction))
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(12,12,12,12); setBackgroundColor(0xFF101820.toInt()) }
        status = TextView(this).apply { text="Connect DualSense using USB-C"; setTextColor(-1); textSize=15f }
        root.addView(status)
        arena = Arena(this); root.addView(arena, LinearLayout.LayoutParams(-1,0,1f))
        val row = LinearLayout(this)
        row.addView(button("CONNECT USB"){ findController() }, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(button("TEST R2"){ sendEffect(true,false) }, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(button("TEST L2"){ sendEffect(false,true) }, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(row)
        val row2 = LinearLayout(this)
        row2.addView(button("BOTH ON"){ sendEffect(true,true) }, LinearLayout.LayoutParams(0,-2,1f))
        row2.addView(button("TRIGGERS OFF"){ sendOff() }, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(row2)
        root.addView(TextView(this).apply { text="Left stick move • Right stick aim • R2 shoot • L2 aim • X dodge. Adaptive trigger USB output is experimental and not guaranteed on every Android device."; textSize=11f; setTextColor(0xFFB8C6D1.toInt()) })
        setContentView(root); findController()
    }
    private fun button(s:String, f:()->Unit)=Button(this).apply { text=s; textSize=11f; setOnClickListener{f()} }
    private fun findController() {
        val d=usb.deviceList.values.firstOrNull { it.vendorId==0x054C && (it.productId==0x0CE6 || it.productId==0x0DF2) }
        if(d==null){status.text="DualSense not found. Use a USB-C data cable.";return}
        if(usb.hasPermission(d)) connect(d) else {
            val flags=if(Build.VERSION.SDK_INT>=31) PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else 0
            usb.requestPermission(d,PendingIntent.getBroadcast(this,0,Intent(permissionAction).setPackage(packageName),flags))
            status.text="Allow USB access in Android popup"
        }
    }
    private fun connect(d:UsbDevice) {
        closeUsb()
        val h=(0 until d.interfaceCount).map{d.getInterface(it)}.firstOrNull{it.interfaceClass==UsbConstants.USB_CLASS_HID}
        if(h==null){status.text="No HID interface found";return}
        val c=usb.openDevice(d)
        if(c==null || !c.claimInterface(h,true)){status.text="Could not claim HID interface";c?.close();return}
        conn=c;hid=h;status.text="DualSense USB connected. Try TEST R2."
    }
    // Experimental USB HID output-report path. Controller firmware may require a different report format.
    private fun effectBlock():ByteArray = ByteArray(11).also { it[0]=0x21;it[1]=0xFC.toByte();it[2]=0x03;it[3]=0x49;it[4]=0x92.toByte();it[5]=0x24;it[6]=0x49;it[7]=0x92.toByte() }
    private fun sendEffect(r2:Boolean,l2:Boolean) {
        val c=conn;val h=hid;if(c==null||h==null){status.text="Connect controller first";return}
        val b=ByteArray(48);b[0]=0x02;b[1]=((if(r2)4 else 0) or (if(l2)8 else 0)).toByte()
        if(r2)effectBlock().copyInto(b,11);if(l2)effectBlock().copyInto(b,22)
        val n=c.controlTransfer(0x21,0x09,0x0202,h.id,b,b.size,1000)
        status.text=if(n>=0)"USB accepted $n bytes; check trigger feel" else "USB output failed ($n). Android may block raw HID output."
    }
    private fun sendOff() {
        val c=conn;val h=hid;if(c==null||h==null){status.text="Connect controller first";return}
        val b=ByteArray(48);b[0]=0x02;b[1]=0x0C;b[11]=0x05;b[22]=0x05
        val n=c.controlTransfer(0x21,0x09,0x0202,h.id,b,b.size,1000)
        status.text=if(n>=0)"Trigger reset report sent" else "USB reset failed ($n)"
    }
    override fun onGenericMotionEvent(e:MotionEvent):Boolean {
        if(e.source and InputDevice.SOURCE_JOYSTICK==InputDevice.SOURCE_JOYSTICK && e.action==MotionEvent.ACTION_MOVE){
            arena.sticks(e.getAxisValue(MotionEvent.AXIS_X),e.getAxisValue(MotionEvent.AXIS_Y),e.getAxisValue(MotionEvent.AXIS_Z),e.getAxisValue(MotionEvent.AXIS_RZ));return true
        };return super.onGenericMotionEvent(e)
    }
    override fun onKeyDown(k:Int,e:KeyEvent):Boolean { when(k){KeyEvent.KEYCODE_BUTTON_R2->{arena.fire=true;return true};KeyEvent.KEYCODE_BUTTON_L2->{arena.aim=true;return true};KeyEvent.KEYCODE_BUTTON_A->{arena.dodge();return true}};return super.onKeyDown(k,e) }
    override fun onKeyUp(k:Int,e:KeyEvent):Boolean { when(k){KeyEvent.KEYCODE_BUTTON_R2->{arena.fire=false;return true};KeyEvent.KEYCODE_BUTTON_L2->{arena.aim=false;return true}};return super.onKeyUp(k,e) }
    private fun closeUsb(){try{hid?.let{conn?.releaseInterface(it)}}catch(_:Exception){};conn?.close();conn=null;hid=null}
    override fun onDestroy(){try{unregisterReceiver(receiver)}catch(_:Exception){};closeUsb();super.onDestroy()}
}
private class Arena(c:Context):View(c) {
    private val p=android.graphics.Paint(3);private var x=.5f;private var y=.5f;private var a=0f;private var sx=0f;private var sy=0f;private var ax=0f;private var ay=0f;private var score=0
    var fire=false;var aim=false
    private val zombies=MutableList(7){Pair(Math.random().toFloat(),Math.random().toFloat())}
    fun sticks(x1:Float,y1:Float,x2:Float,y2:Float){sx=x1;sy=y1;ax=x2;ay=y2;invalidate()}
    fun dodge(){x=(x+cos(a)*.13f).coerceIn(.05f,.95f);y=(y+sin(a)*.13f).coerceIn(.05f,.95f);invalidate()}
    override fun onDraw(c:android.graphics.Canvas){
        super.onDraw(c);val w=width.toFloat();val h=height.toFloat();c.drawColor(0xFF182A2C.toInt());p.color=0xFF294044.toInt()
        for(i in 0..20)c.drawLine(i*w/20,0f,i*w/20,h,p);for(i in 0..12)c.drawLine(0f,i*h/12,w,i*h/12,p)
        x=(x-sx*.012f).coerceIn(.04f,.96f);y=(y+sy*.012f).coerceIn(.05f,.95f);if(abs(ax)+abs(ay)>.2f)a=atan2(ay,ax)
        zombies.forEachIndexed{i,z->val zx=z.first*w;val zy=z.second*h;p.color=0xFF8CBA69.toInt();c.drawCircle(zx,zy,14f,p);if(fire&&hypot(zx-x*w,zy-y*h)<105f){score++;zombies[i]=Pair(Math.random().toFloat(),Math.random().toFloat())}}
        p.color=0xFFE5E7EB.toInt();c.drawCircle(x*w,y*h,15f,p);p.color=0xFFEAB308.toInt();p.strokeWidth=7f;c.drawLine(x*w,y*h,x*w+cos(a)*30,y*h+sin(a)*30,p)
        p.color=-1;p.textSize=22f;c.drawText("ZOMBIE SURVIVAL  SCORE $score",18f,30f,p);p.textSize=14f;c.drawText(if(aim)"AIMING" else "SURVIVE THE WAVE",18f,h-16f,p);postInvalidateDelayed(33)
    }
}
