package com.linx.kahabatteryapp.ui.activity

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.linx.kahabatteryapp.R
import com.linx.kahabatteryapp.base.BaseActivity
import com.linx.kahabatteryapp.bean.DeviceBean
import com.linx.kahabatteryapp.blue.BluetoothScanUtils
import com.linx.kahabatteryapp.constant.Constant
import com.linx.kahabatteryapp.databinding.ActivityMainBinding
import com.linx.kahabatteryapp.ui.battery.BatteryInfoActivity
import com.permissionx.guolindev.PermissionX

/**
 * @author：linxun
 * @exception：扫描蓝牙设备
 * @create on:2022.09.05
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private val mAdapter = MyAdapter(this)

    private var mBackPressed: Long = 0

    private val TIME_EXIT = 2000

    private var activity: ActivityResultLauncher<Intent>? = null

    override fun onBackPressed() {
        mBackPressed = if (mBackPressed + TIME_EXIT > System.currentTimeMillis()) {
            super.onBackPressed()
            return
        } else {
            Toast.makeText(this, "Click again to exit", Toast.LENGTH_SHORT).show()
            System.currentTimeMillis()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        addListener()
        check()
    }

    private fun initView() {
        BluetoothScanUtils.INSTANCE.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.recyclerView.adapter = mAdapter
        //实现列表分割线
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        mAdapter.setOnItemOnclickListener(object : DeviceItemOnClickListener {
            override fun onClick(view: View, position: Int) {
                if (BluetoothScanUtils.INSTANCE.isScanning()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Please stop scanning first",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val intent = Intent(this@MainActivity, BatteryInfoActivity::class.java)
                intent.putExtra(Constant.ADDRESS, mAdapter.data[position].address)
                startActivity(intent)
            }
        })

        activity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                Activity.RESULT_OK -> {
                    check()
                }
                else -> {
                    Toast.makeText(
                        this@MainActivity,
                        "Please turn on bluetooth first",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun addListener() {
        BluetoothScanUtils.INSTANCE.addDeviceCallbackListener(object : BluetoothScanUtils.DeviceCallback {
            override fun onScanResult(device: DeviceBean) {
                if (!isDeviceContains(device.address ?: "")) {
                    mAdapter.setNewData(device)
                }
            }
            override fun onScanStop() {
                invalidateOptionsMenu()
            }

            override fun onError(msg: String) {

            }
        })
    }
    private fun isDeviceContains(address: String): Boolean {
        var result = false
        mAdapter.data.forEachIndexed { index, deviceBean ->
            if (deviceBean.address == address) {
                result = true
            }
        }
        return result
    }


    private fun setScanRole() {
        val name = binding.etFilterName.text.toString()
        val rssi = binding.etFilterRssi.text.toString()
        if (name.isNotEmpty()){
            BluetoothScanUtils.INSTANCE.addFilterName(name)
        }
    }

    private fun check() {
        setScanRole()
        //是否是安卓12.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                .request { allGranted, grantedList, deniedList ->
                    if (!allGranted) {
                        Toast.makeText(
                            this,
                            "Please enable the relevant permissions",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (!BluetoothScanUtils.INSTANCE.isEnabled()) {
                            activity?.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        } else {
                            invalidateOptionsMenu()
                            BluetoothScanUtils.INSTANCE.scanBlueDevice()
                        }
                    }
                }
            return
        }
        //安卓11以及一下的版本
        PermissionX.init(this)
            .permissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .request { allGranted, grantedList, deniedList ->
                if (!allGranted) {
                    Toast.makeText(
                        this,
                        "Please enable the relevant permissions",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    //权限通过   检查蓝牙是否开启
                    if (!BluetoothScanUtils.INSTANCE.isEnabled()) {
                        activity?.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    } else {
                        invalidateOptionsMenu()
                        BluetoothScanUtils.INSTANCE.scanBlueDevice()
                    }
                }
            }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val item = menu.findItem(R.id.menu_search)
        val mSearchView = MenuItemCompat.getActionView(item) as SearchView
        val id: Int =
            mSearchView.context.resources.getIdentifier("android:id/search_src_text", null, null)
        val textView: TextView = mSearchView.findViewById(id)
        textView.setTextColor(Color.WHITE)
        textView.setHintTextColor(Color.parseColor("#CCCCCC"))
        mSearchView.setOnSearchClickListener { v: View? ->
            menu.findItem(R.id.menu_stop).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.findItem(R.id.menu_scan).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.findItem(R.id.menu_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        mSearchView.setOnCloseListener {
            menu.findItem(R.id.menu_stop).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.findItem(R.id.menu_scan).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.findItem(R.id.menu_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            false
        }
        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                return true
            }
        })

        if (BluetoothScanUtils.INSTANCE.isScanning()) {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh)
                .setActionView(R.layout.actionbar_indeterminate_progress)
        } else {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                mAdapter.clea()
                hideKeyboard()
                check()
            }
            R.id.menu_stop -> {
                hideKeyboard()
                BluetoothScanUtils.INSTANCE.stopScan()
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return true
    }


    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    inner class MyAdapter(val context: Context) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        val data = mutableListOf<DeviceBean>()

        private var listener: DeviceItemOnClickListener? = null


        fun setOnItemOnclickListener(listener: DeviceItemOnClickListener) {
            this.listener = listener
        }

        fun setNewData(item: DeviceBean) {
            data.add(item)
            data.sortWith { o1: DeviceBean, o2: DeviceBean -> o2.rssi - o1.rssi }
            notifyDataSetChanged()
        }

        fun clea() {
            if (data.size > 0) {
                data.clear()
                notifyDataSetChanged()
            }
        }
        inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById<TextView>(R.id.tvDeviceName)
            val address: TextView = view.findViewById<TextView>(R.id.tvAddress)
            val rootView: ConstraintLayout = view.findViewById<ConstraintLayout>(R.id.clRootView)
            val riis: TextView = view.findViewById<TextView>(
                R.id.tvRiis
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_bluetooth, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.name.text = data[position].name ?: ""
            holder.address.text = data[position].address ?: ""
            holder.riis.text = "${data[position].rssi}"
            holder.rootView.setOnClickListener {
                listener?.onClick(it, position)
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
    interface DeviceItemOnClickListener {
        fun onClick(view: View, position: Int)
    }
}