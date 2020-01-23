package com.khsbs.saycle

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity


// TODO : Service로 이전
class MainActivity : AppCompatActivity() {

    companion object {
        const val GPS_ENABLE_REQUEST_CODE = 2001
        const val PERMISSIONS_REQUEST_CODE = 100
        const val COUNTDOWN_REQUEST = 1001

        const val REQUEST_ENABLE_BT = 3
        const val USER_OK = 4
        const val USER_EMERGENCY = 5
        const val TAG = "Saycle"

        const val LIST_NAME = "NAME"
        const val LIST_UUID = "UUID"
    }

    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mDevices: Set<BluetoothDevice> = emptySet()
    private var mPairedDeviceCount = 0
    lateinit var mDeviceName: String
    lateinit var mDeviceAddress: String
    private var mBluetoothLeService: BluetoothLeService? = null
    private var characteristicTX: BluetoothGattCharacteristic? = null
    private var characteristicRX: BluetoothGattCharacteristic? = null

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            tv_main_remote.text = "OFF"
            mBluetoothLeService?.disconnect()
            mBluetoothLeService?.close()
            mBluetoothLeService = null
        }

        override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
            } else {
                Log.d(TAG, "Initialize Bluetooth")
                mBluetoothLeService!!.connect(mDeviceAddress)
            }
        }
    }

    val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    tv_main_remote.text = "ON"
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED ->
                    tv_main_remote.text = "OFF"
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED ->
                    displayGattServices(mBluetoothLeService?.supportedGattServices)
                BluetoothLeService.ACTION_DATA_AVAILABLE ->
                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)!!)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Glide.with(baseContext)
            .asGif()
            .load(R.raw.loading)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(iv_main_loading)

        // TODO : RxBinding - RxTextView 사용하기
        tv_main_remote.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                when (p0.toString()) {
                    "ON" -> {
                        iv_main_loading.visibility = View.INVISIBLE
                        bg_main.background =
                            ContextCompat.getDrawable(
                                this@MainActivity,
                                R.drawable.bg_main_default
                            )

                    }
                    "OFF" -> {
                        iv_main_loading.visibility = View.INVISIBLE
                        bg_main.background =
                            ContextCompat.getDrawable(
                                this@MainActivity,
                                R.drawable.bg_main_default
                            )
                        switch_main.isChecked = false
                    }
                    "DISCONNECTED" -> {
                        iv_main_loading.visibility = View.VISIBLE
                        bg_main.background = ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.bg_main_disconnected
                        )
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        switch_main.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBluetooth()
                tv_main_remote.text = "DISCONNECTED"
            } else {
                mBluetoothLeService?.disconnect()
                mBluetoothLeService?.close()
                tv_main_remote.text = "OFF"
            }
        }

        iv_main_user.setOnClickListener {
            startActivity<SettingActivity>()
        }

        bindService(
            Intent(this, MainActivity::class.java),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())

        if (mBluetoothLeService != null) {
            if (mBluetoothLeService!!.connect(mDeviceAddress))
                tv_main_remote.text = "ON"
            else
                tv_main_remote.text = "OFF"
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BT -> selectDevice()
            COUNTDOWN_REQUEST ->
                when (resultCode) {
                    USER_EMERGENCY -> {
                        Log.d(TAG, "EMERGENCY")
                        sendMessage()
                    }
                }
        }
    }

    private fun checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter.isEnabled.not()) {
            // 블루투스를 지원하지만 비활성 상태인 경우
            // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(
                enableBtIntent,
                REQUEST_ENABLE_BT
            )
        } else {
            selectDevice()
        }
    }

    private fun selectDevice() {
        // 페어링되었던 기기 목록 획득
        mDevices = mBluetoothAdapter.bondedDevices
        // 페어링되어던 기기 갯수
        mPairedDeviceCount = mDevices.size
        // AlertDialog
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("디바이스 선택")
        builder.setCancelable(false)

        // 페어링 된 블루투스 장치의 이름 목록 작성
        val items_name = ArrayList<String>()
        val items_address = ArrayList<String>()
        for (device in mDevices) {
            items_name.add(device.name)
            items_address.add(device.address)
        }
        items_name.add("취소")
        val itemsWithCharSequence = items_name.toArray(arrayOfNulls<CharSequence>(items_name.size))

        builder.setItems(itemsWithCharSequence) { dialog, pos ->
            if (pos == items_name.size - 1) {
                dialog.dismiss()
                switch_main.isChecked = false
            } else {
                connectToSelectedDevice(items_name[pos] to items_address[pos])
            }
        }

        val alert = builder.create()
        alert.show()
    }

    @SuppressLint("HandlerLeak")
    private fun connectToSelectedDevice(deviceInfo: Pair<String, String>) {
        mDeviceName = deviceInfo.first
        mDeviceAddress = deviceInfo.second
        Log.d("device", "$mDeviceName $mDeviceAddress")
        val gattServiceIntent = Intent(baseContext, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    private fun displayData(str: String) {
        startActivityForResult(Intent(this, CountdownActivity::class.java), COUNTDOWN_REQUEST)
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = "unknown service"
        val gattServiceData = ArrayList<HashMap<String, String>>()


        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = Attr.lookup(uuid, unknownServiceString)

            // If the service exists for HM 10 Serial, say so.
            //if(SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") { isSerial.setText("Yes, serial :-)"); } else {  isSerial.setText("No, serial :-("); }
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX)
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX)
            if (Attr.lookup(uuid, unknownServiceString) === "HM 10 Serial") {
                mBluetoothLeService?.setCharacteristicNotification(characteristicRX, true)
            }
        }
    }

    private fun sendMessage() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.SEND_SMS),
                PERMISSIONS_REQUEST_CODE
            )
        }
        message(
            "1037855236",
            "서울 동작구 상도로 369 숭실대학교에서 사고가 발생하였습니다."
        )
        message(
            "1037855236",
            "이름 : 한도협\n혈액형 : O형\n키 : 190cm\n몸무게 : 69kg"
        )
    }

    private fun getCurrentAddress(latitude: Double, longitude: Double) {

    }

    private fun message(number: String, text: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage("+82$number", null, text, null, null)
    }
}