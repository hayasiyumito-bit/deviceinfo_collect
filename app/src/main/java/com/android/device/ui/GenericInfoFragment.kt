package com.android.device.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.device.DeviceInfoParser
import com.android.device.R
import com.android.device.databinding.FragmentListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

/**
 * 通用信息页：按传入的分类集合过滤共享快照，渲染成卡片。
 * 系统 / 硬件 / 网络 / 应用 四个 Tab 共用本 Fragment，仅分类集合不同。
 */
class GenericInfoFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeviceViewModel by activityViewModels()
    private lateinit var adapter: CardAdapter

    private val topKeys: List<String> by lazy {
        arguments?.getStringArray(ARG_TOP_KEYS)?.toList() ?: emptyList()
    }

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
                    val cards = SnapshotExpander.expand(requireContext(), state.data.raw, topKeys)
                    adapter.submit(cards)
                    binding.empty.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
                }
                is DeviceViewModel.State.Error -> {
                    binding.loading.visibility = View.GONE
                    binding.swipe.isRefreshing = false
                    binding.empty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showDetail(row: CardItem.Row) {
        var content = row.fullValue
        if (row.key == "build") {
            content = DeviceInfoParser.formatBuildDetailContent(content)
        } else {
            try {
                val t = content.trim()
                if (t.startsWith("{")) content = DeviceInfoParser.formatJsonForDisplay(JSONObject(content))
                else if (t.startsWith("[")) content = DeviceInfoParser.formatJsonForDisplay(JSONArray(content))
            } catch (_: Exception) {
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(row.label)
            .setMessage(content)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TOP_KEYS = "top_keys"

        fun newInstance(vararg topKeys: String): GenericInfoFragment =
            GenericInfoFragment().apply {
                arguments = Bundle().apply { putStringArray(ARG_TOP_KEYS, topKeys) }
            }
    }
}
