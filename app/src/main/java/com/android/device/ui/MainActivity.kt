package com.android.device.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.device.R
import com.android.device.databinding.ActivityMainBinding
import com.android.device.i18n.AppLocale
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 全新主界面：底部导航 5 大分组（风控 / 系统 / 硬件 / 网络 / 应用），第一个为风控。
 * 采集只跑一次，5 个页面共享 [DeviceViewModel]。主题配色与界面语言可在右上角菜单切换。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: DeviceViewModel

    private val fragments = HashMap<Int, Fragment>()
    private var activeId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        AppLocale.applyFromCache(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applySystemBars(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        viewModel = ViewModelProvider(this)[DeviceViewModel::class.java]

        binding.bottomNav.setOnItemSelectedListener { item ->
            switchTo(item.itemId)
            true
        }
        // 首次进入默认选中风控页
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_security
        }

        // 左上角「实时监测」入口
        binding.btnRealtime.setOnClickListener {
            startActivity(android.content.Intent(this, RealtimeActivity::class.java))
        }

        // 重新采集：手动重新扫描全部参数
        binding.fabRefresh.setOnClickListener {
            viewModel.collect()
            Toast.makeText(this, R.string.toast_recollecting, Toast.LENGTH_SHORT).show()
        }

        // BACK 退出确认，避免误触直接退出
        onBackPressedDispatcher.addCallback(this) { showExitDialog() }

        // 进入即采集
        viewModel.collectIfNeeded()
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setPositiveButton(R.string.action_exit) { _, _ -> finish() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun switchTo(id: Int) {
        if (id == activeId && fragments.containsKey(id)) return
        val tx = supportFragmentManager.beginTransaction()
        fragments[activeId]?.let { tx.hide(it) }
        var target = fragments[id]
        if (target == null) {
            target = createFragment(id)
            fragments[id] = target
            tx.add(R.id.fragment_container, target, "tab_$id")
        } else {
            tx.show(target)
        }
        tx.commit()
        activeId = id
    }

    private fun createFragment(id: Int): Fragment = when (id) {
        R.id.nav_security -> SecurityFragment()
        R.id.nav_system -> GenericInfoFragment.newInstance(
            "build", "uname", "time", "collectedAt", "ids", "memThreshold"
        )
        R.id.nav_hardware -> GenericInfoFragment.newInstance(
            "hardware", "batteryInfo", "gpuInfo", "sensor", "inputDevices", "usb", "input"
        )
        R.id.nav_network -> GenericInfoFragment.newInstance(
            "net", "location", "storage"
        )
        R.id.nav_apps -> GenericInfoFragment.newInstance(
            "installedApps", "packageInfo", "service_list", "inputMethods",
            "InputLanguageList", "appsflyerdebuginfo", "media", "library", "fonts", "systemFonts"
        )
        else -> SecurityFragment()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> { showThemeDialog(); true }
            R.id.action_language -> { showLanguageDialog(); true }
            R.id.action_about -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.action_ok, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** 主题配色切换：单选后写入缓存并 recreate 立即生效。 */
    private fun showThemeDialog() {
        val presets = ThemeManager.Preset.entries
        val labels = presets.map { getString(it.nameRes) }.toTypedArray()
        val checked = presets.indexOf(ThemeManager.current(this))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                dialog.dismiss()
                val chosen = presets[which]
                if (chosen != ThemeManager.current(this)) {
                    ThemeManager.setPreset(this, chosen)
                    recreate()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** 界面语言切换：English / 中文，写入 SP 长期缓存，切换后系统重建界面。 */
    private fun showLanguageDialog() {
        val labels = arrayOf(getString(R.string.language_english), getString(R.string.language_chinese))
        val tags = arrayOf(AppLocale.EN, AppLocale.ZH)
        val current = AppLocale.getLanguage(this)
        val checked = if (current.equals(AppLocale.ZH, ignoreCase = true)) 1 else 0
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                dialog.dismiss()
                if (!tags[which].equals(current, ignoreCase = true)) {
                    AppLocale.setLanguage(this, tags[which])
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
