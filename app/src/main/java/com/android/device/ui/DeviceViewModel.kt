package com.android.device.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.device.DeviceSnapshotMerger
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * 全局共享的采集结果：整机只采集一次，各 Tab 页共享同一份快照。
 *
 * 复用现有 Java 采集链（[DeviceSnapshotMerger.collectFull]）；UI 层直接从原始 JSON
 * 按需展开（见 SnapshotExpander / SecurityFragment），不再走旧的 DeviceInfoParser.parse。
 */
class DeviceViewModel(app: Application) : AndroidViewModel(app) {

    /** 一次完整快照的原始 JSON，各页共享。 */
    data class DeviceData(
        val raw: JSONObject,
        val collectedAt: Long
    )

    sealed class State {
        object Loading : State()
        data class Success(val data: DeviceData) : State()
        data class Error(val message: String) : State()
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val _state = MutableLiveData<State>()
    val state: LiveData<State> get() = _state

    /** 首次进入或下拉刷新时调用；重复调用会重新采集。 */
    fun collect() {
        _state.postValue(State.Loading)
        executor.execute {
            try {
                val snapshot = DeviceSnapshotMerger.collectFull(getApplication())
                val json = JSONObject(snapshot.toString())
                Log.i(TAG, "snapshot collected, topKeys=" + json.length())
                _state.postValue(State.Success(DeviceData(json, System.currentTimeMillis())))
            } catch (e: Exception) {
                Log.e(TAG, "collect failed", e)
                _state.postValue(State.Error(e.message ?: "unknown"))
            }
        }
    }

    /** 若尚无数据则采集，避免重建时重复采。 */
    fun collectIfNeeded() {
        if (_state.value == null) collect()
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdownNow()
    }

    private companion object {
        const val TAG = "DeviceViewModel"
    }
}
