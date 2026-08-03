package com.m4xtheme.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_THEME_FILE = 101;
    private int downloads = 12480;
    private int pendingThemes = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statsText = findViewById(R.id.statsText);
        Button downloadButton = findViewById(R.id.downloadButton);
        Button uploadButton = findViewById(R.id.uploadButton);
        Button adminButton = findViewById(R.id.adminButton);

        updateStats(statsText);

        downloadButton.setOnClickListener(v -> {
            downloads++;
            updateStats(statsText);
            Toast.makeText(this, "Đã ghi nhận lượt tải theme", Toast.LENGTH_SHORT).show();
        });

        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/zip",
                    "application/octet-stream"
            });
            startActivityForResult(intent, PICK_THEME_FILE);
        });

        adminButton.setOnClickListener(v -> {
            pendingThemes = Math.max(0, pendingThemes - 1);
            updateStats(statsText);
            Toast.makeText(this, "Admin demo: đã duyệt 1 theme", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateStats(TextView statsText) {
        statsText.setText(String.format(Locale.getDefault(),
                "• 1.248 người dùng\n• 326 theme\n• %d theme chờ duyệt\n• %,d lượt tải\n• Đánh giá trung bình 4,8/5",
                pendingThemes, downloads));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_THEME_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String message = uri == null ? "Đã chọn file theme" : "Đã chọn: " + uri.getLastPathSegment();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
}
