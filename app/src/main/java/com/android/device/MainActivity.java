package com.android.device;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 启动即采集并展示本机全量参数（含 Hook / 模拟器 / 环境检测）。
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_WRITE_STORAGE = 1001;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView statusText;
    private ChipGroup chipGroup;
    private FloatingActionButton fabRefresh;
    private DeviceInfoAdapter adapter;
    private List<Object> allItems = new ArrayList<>();
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        chipGroup = findViewById(R.id.chip_group);
        fabRefresh = findViewById(R.id.fab_refresh);

        adapter = new DeviceInfoAdapter(new ArrayList<>(), this::showDetailDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupChipGroup();
        fabRefresh.setOnClickListener(v -> startCollection());

        startCollection();
    }

    private void startCollection() {
        if (needsLegacyStoragePermission()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE
            );
            return;
        }
        collectDeviceInfo();
    }

    private static boolean needsLegacyStoragePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            collectDeviceInfo();
        }
    }

    private void collectDeviceInfo() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText(R.string.status_collecting);
        allItems = new ArrayList<>();
        adapter.updateData(allItems);

        executorService.execute(() -> {
            String jsonData = null;
            Exception error = null;
            try {
                JSONObject snapshot = DeviceSnapshotMerger.collectFull(getApplicationContext());
                jsonData = snapshot.toString();
                Log.i(TAG, "Collected snapshot length=" + jsonData.length());
            } catch (Exception e) {
                error = e;
                Log.e(TAG, "Collection failed", e);
            }

            final String finalJson = jsonData;
            final Exception finalError = error;
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (finalJson != null && !finalJson.isEmpty()) {
                    try {
                        JSONObject jsonObject = new JSONObject(finalJson);
                        allItems = DeviceInfoParser.parse(jsonObject);
                        adapter.updateData(allItems);
                        int itemCount = DeviceInfoParser.countDataItems(allItems);
                        statusText.setText(getString(R.string.status_done, itemCount));
                        updateToolbarTitle(itemCount);
                        chipGroup.check(R.id.chip_all);
                    } catch (JSONException e) {
                        Log.e(TAG, "Parse failed", e);
                        statusText.setText(getString(R.string.status_parse_error, e.getMessage()));
                        Toast.makeText(this, R.string.status_parse_error_toast, Toast.LENGTH_LONG).show();
                    }
                } else {
                    statusText.setText(getString(R.string.status_collect_error,
                            finalError != null ? finalError.getMessage() : "unknown"));
                }
            });
        });
    }

    private void showDetailDialog(DeviceInfoItem item) {
        String content = item.getFullValue();
        if ("build".equals(item.getOriginalKey())) {
            content = DeviceInfoParser.formatBuildDetailContent(content);
        } else {
            try {
                if (content.trim().startsWith("{")) {
                    Object raw = new JSONObject(content);
                    content = DeviceInfoParser.formatJsonForDisplay(raw);
                } else if (content.trim().startsWith("[")) {
                    Object raw = new org.json.JSONArray(content);
                    content = DeviceInfoParser.formatJsonForDisplay(raw);
                }
            } catch (Exception ignored) {
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(item.getTranslatedKey())
                .setMessage(content)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private void setupChipGroup() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipGroup.check(R.id.chip_all);
                return;
            }
            filterDataByCategory(checkedIds.get(0));
        });
        chipGroup.check(R.id.chip_all);
    }

    private void filterDataByCategory(int chipId) {
        if (allItems.isEmpty()) {
            return;
        }
        String targetCategory = getTargetCategory(chipId);
        List<Object> filteredItems = new ArrayList<>();

        if ("全部".equals(targetCategory)) {
            filteredItems = allItems;
        } else {
            boolean inTargetCategory = false;
            for (Object item : allItems) {
                if (item instanceof String) {
                    inTargetCategory = targetCategory.equals(item);
                    if (inTargetCategory) {
                        filteredItems.add(item);
                    }
                } else if (item instanceof DeviceInfoItem && inTargetCategory) {
                    filteredItems.add(item);
                }
            }
        }

        adapter.updateData(filteredItems);
        int itemCount = DeviceInfoParser.countDataItems(filteredItems);
        statusText.setText(getString(R.string.status_filtered, itemCount));
        updateToolbarTitle(itemCount);
    }

    private String getTargetCategory(int chipId) {
        if (chipId == R.id.chip_security) {
            return "安全检测";
        } else if (chipId == R.id.chip_system) {
            return "系统信息";
        } else if (chipId == R.id.chip_hardware) {
            return "硬件信息";
        } else if (chipId == R.id.chip_network) {
            return "网络信息";
        } else if (chipId == R.id.chip_software) {
            return "软件信息";
        } else if (chipId == R.id.chip_storage) {
            return "存储信息";
        } else if (chipId == R.id.chip_sensor) {
            return "传感器信息";
        } else if (chipId == R.id.chip_other) {
            return "其他信息";
        }
        return "全部";
    }

    private void updateToolbarTitle(int count) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_device_info, count));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
