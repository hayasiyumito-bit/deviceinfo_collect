package com.android.device.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.device.R
import com.android.device.databinding.FragmentListBinding
import com.android.device.i18n.AppLocale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

/**
 * 风控页（第一个 Tab）：设备风险总览仪表盘 + Root / Hook / 环境 / 修复分区卡片。
 * 这是本应用相较通用设备信息工具的核心差异化。数据源为共享快照的 security 块。
 */
class SecurityFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeviceViewModel by activityViewModels()
    private lateinit var adapter: CardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CardAdapter(::showDetail)
        binding.recycler.adapter = adapter
        binding.swipe.setOnRefreshListener { viewModel.collect() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DeviceViewModel.State.Loading -> {
                    if (!binding.swipe.isRefreshing) binding.loading.visibility = View.VISIBLE
                    binding.empty.visibility = View.GONE
                }
                is DeviceViewModel.State.Success -> {
                    binding.loading.visibility = View.GONE
                    binding.swipe.isRefreshing = false
                    adapter.submit(buildCards(state.data.raw))
                }
                is DeviceViewModel.State.Error -> {
                    binding.loading.visibility = View.GONE
                    binding.swipe.isRefreshing = false
                    binding.empty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun buildCards(raw: JSONObject): List<CardItem> {
        val cards = mutableListOf<CardItem>()
        val security = raw.optJSONObject("security") ?: JSONObject()
        val summary = security.optJSONObject("summary") ?: JSONObject()

        // 应用可调试(FLAG_DEBUGGABLE) 与 ADB 调试不视为真实风控风险，从原因中剔除并据此重算结论。
        val reasonsRaw = raw.optJSONArray("anyRiskReasons")
        val reasons = mutableListOf<String>()
        if (reasonsRaw != null) {
            for (i in 0 until reasonsRaw.length()) {
                val t = reasonsRaw.optString(i)
                if (!isBenignSignal(t)) reasons.add(t)
            }
        }
        val anyRisk = reasons.isNotEmpty()
        val hitCount = reasons.size

        // 1) 总览头卡
        cards.add(
            CardItem.Header(
                verdict = getString(if (anyRisk) R.string.risk_verdict_danger else R.string.risk_verdict_safe),
                sub = if (anyRisk) getString(R.string.risk_hits, hitCount) else getString(R.string.risk_no_hits),
                danger = anyRisk
            )
        )

        // 2) 风险原因
        if (reasons.isNotEmpty()) {
            val rows = reasons.mapIndexed { i, text -> CardItem.Row("${i + 1}", text, text) }
            cards.add(CardItem.Body(getString(R.string.risk_reasons_title), rows))
        }

        // 3) Root
        val root = security.optJSONObject("root") ?: JSONObject()
        val rootRows = mutableListOf<CardItem.Row>()
        rootRows.add(boolRow(summary, "isRooted", AppLocale.tr("是否 Root", "Rooted")))
        rootRows.add(boolRow(summary, "rootAccessGranted", AppLocale.tr("Root 已授权", "Root granted")))
        val accessDetail = root.optString("accessDetail", "")
        if (accessDetail.isNotEmpty()) {
            rootRows.add(CardItem.Row(AppLocale.tr("授权详情", "Access detail"), accessDetail, accessDetail))
        }
        rootRows.add(boolRow(summary, "magiskDetected", "Magisk"))
        rootRows.add(boolRow(summary, "kernelsuDetected", "KernelSU"))
        rootRows.add(boolRow(summary, "apatchDetected", "APatch"))
        rootRows.add(boolRow(summary, "systemSuDetected", AppLocale.tr("系统 su", "System su")))
        rootRows.add(boolRow(summary, "suBinaryFound", AppLocale.tr("su 二进制", "su binary")))
        rootRows.add(boolRow(summary, "busyboxDetected", "BusyBox"))
        rootRows.add(boolRow(summary, "rootHideDetected", AppLocale.tr("Root 隐藏", "Root hiding")))
        rootRows.add(boolRow(summary, "magiskHideSuspected", AppLocale.tr("疑似 MagiskHide", "MagiskHide suspected")))
        rootRows.add(boolRow(summary, "dangerousAppDetected", AppLocale.tr("危险应用", "Risky app")))
        rootRows.add(boolRow(summary, "bootloaderUnlocked", AppLocale.tr("Bootloader 解锁", "Bootloader unlocked")))
        cards.add(CardItem.Body(getString(R.string.sec_root), rootRows))

        // 4) Hook / Xposed
        val hookRows = mutableListOf<CardItem.Row>()
        hookRows.add(boolRow(summary, "hookFrameworkDetected", AppLocale.tr("Hook 框架", "Hook framework")))
        hookRows.add(boolRow(summary, "propertyTampered", AppLocale.tr("属性被篡改", "Property tampered")))
        hookRows.add(boolRow(summary, "anyHookSignal", AppLocale.tr("任一 Hook 信号", "Any hook signal")))
        cards.add(CardItem.Body(getString(R.string.sec_hook), hookRows))

        // 5) 环境 / 模拟器
        val envRows = mutableListOf<CardItem.Row>()
        envRows.add(boolRow(summary, "isEmulator", AppLocale.tr("模拟器", "Emulator")))
        envRows.add(boolRow(summary, "simulatorDetected", AppLocale.tr("模拟器综合判定", "Simulator verdict")))
        envRows.add(boolRow(summary, "isVpn", AppLocale.tr("VPN 已连接", "VPN connected")))
        envRows.add(boolRow(summary, "isDebug", AppLocale.tr("调试模式", "Debug mode")))
        envRows.add(boolRow(summary, "isAdbEnabled", AppLocale.tr("ADB 已开启", "ADB enabled")))
        cards.add(CardItem.Body(getString(R.string.sec_environment), envRows))

        // 6) 修复建议
        val remediation = raw.optJSONObject("remediation")
        if (remediation != null) {
            val remRows = mutableListOf<CardItem.Row>()
            val summ = remediation.optString("summary", "")
            if (summ.isNotEmpty()) {
                remRows.add(CardItem.Row(AppLocale.tr("摘要", "Summary"), summ, summ))
            }
            val hints = raw.optJSONArray("anyRiskFixHints")
            if (hints != null) {
                for (i in 0 until hints.length()) {
                    val h = hints.optString(i)
                    remRows.add(CardItem.Row("${i + 1}", h, h))
                }
            }
            if (remRows.isNotEmpty()) cards.add(CardItem.Body(getString(R.string.sec_remediation), remRows))
        }

        return cards
    }

    /** 应用可调试 / ADB 调试属于开发态，不计入风控风险。 */
    private fun isBenignSignal(reason: String): Boolean {
        val r = reason.lowercase()
        return r.contains("debug") || r.contains("调试") ||
            r.contains("adb") || r.contains("flag_debuggable")
    }

    private fun boolRow(obj: JSONObject, key: String, label: String): CardItem.Row {
        val v = obj.optBoolean(key, false)
        val text = getString(if (v) R.string.val_yes else R.string.val_no)
        return CardItem.Row(label, text, text, key)
    }

    private fun showDetail(row: CardItem.Row) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(row.label)
            .setMessage(row.fullValue)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
