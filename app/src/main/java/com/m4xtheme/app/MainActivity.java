package com.m4xtheme.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_THEME_FILE = 101;
    private static final int BG = Color.rgb(7, 12, 30);
    private static final int CARD = Color.rgb(20, 28, 55);
    private static final int CARD_2 = Color.rgb(27, 37, 70);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(173, 182, 211);
    private static final int PURPLE = Color.rgb(119, 73, 255);
    private static final int BLUE = Color.rgb(55, 139, 255);

    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout bottomNav;
    private SharedPreferences prefs;
    private String selectedFile = "Chưa chọn file";
    private int currentTab = 0;

    private final List<ThemeItem> themes = new ArrayList<>(Arrays.asList(
            new ThemeItem("Hyper Minimal OS", "M4X Studio", "HyperOS 2/3", "Tối giản", 4.9, 12480, 28500, true),
            new ThemeItem("Glass Control Center", "Zan Themes", "HyperOS 2", "Trung tâm điều khiển", 4.8, 9821, 21010, true),
            new ThemeItem("MIUI Classic 14", "Minh Design", "MIUI 13/14", "Hoài cổ", 4.7, 8350, 19020, false),
            new ThemeItem("Dynamic Island Pro", "M4X Studio", "HyperOS 1/2/3", "Màn hình khóa", 4.9, 7712, 18100, true),
            new ThemeItem("Neon Night", "Huy Theme", "MIUI/HyperOS", "Tối", 4.6, 6500, 14320, false),
            new ThemeItem("iOS Clean Lock", "Phu Themes", "HyperOS 2", "Màn hình khóa", 4.8, 5900, 13040, true)
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("m4x", MODE_PRIVATE);
        buildShell();
        showHome();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setPadding(dp(8), dp(8), dp(8), dp(10));
        bottomNav.setBackgroundColor(Color.rgb(10, 16, 36));
        addNav("⌂", "Trang chủ", 0);
        addNav("⌕", "Khám phá", 1);
        addNav("＋", "Đăng theme", 2);
        addNav("●", "Tài khoản", 3);
        root.addView(bottomNav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));
        setContentView(root);
    }

    private void addNav(String icon, String label, int index) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), dp(4), dp(2), dp(2));
        TextView i = text(icon, 21, TEXT, Typeface.BOLD);
        TextView l = text(label, 11, MUTED, Typeface.NORMAL);
        i.setGravity(Gravity.CENTER);
        l.setGravity(Gravity.CENTER);
        item.addView(i);
        item.addView(l);
        item.setOnClickListener(v -> {
            currentTab = index;
            refreshNav();
            if (index == 0) showHome();
            else if (index == 1) showExplore();
            else if (index == 2) showUpload();
            else showProfile();
        });
        bottomNav.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private void refreshNav() {
        for (int n = 0; n < bottomNav.getChildCount(); n++) {
            View v = bottomNav.getChildAt(n);
            v.setBackground(n == currentTab ? rounded(CARD_2, 18) : null);
        }
    }

    private ScrollView page(String title, String subtitle) {
        content.removeAllViews();
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(24));
        TextView h = text(title, 30, TEXT, Typeface.BOLD);
        TextView s = text(subtitle, 15, MUTED, Typeface.NORMAL);
        page.addView(h);
        page.addView(s);
        page.addView(space(18));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        content.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.setTag(page);
        refreshNav();
        return scroll;
    }

    private LinearLayout body(ScrollView scroll) {
        return (LinearLayout) scroll.getTag();
    }

    private void showHome() {
        currentTab = 0;
        ScrollView scroll = page("M4X Theme", "Kho theme Xiaomi dành cho HyperOS và MIUI");
        LinearLayout p = body(scroll);

        LinearLayout search = card();
        TextView searchText = text("⌕  Tìm theme, tác giả, phiên bản...", 15, MUTED, Typeface.NORMAL);
        search.addView(searchText);
        search.setOnClickListener(v -> showExplore());
        p.addView(search);
        p.addView(space(14));

        LinearLayout banner = cardGradient();
        banner.addView(text("THEME NỔI BẬT TUẦN NÀY", 12, Color.WHITE, Typeface.BOLD));
        banner.addView(text("Hyper Minimal OS", 25, Color.WHITE, Typeface.BOLD));
        banner.addView(text("Tối giản • Mượt • Tương thích HyperOS 2/3", 14, Color.WHITE, Typeface.NORMAL));
        banner.addView(space(10));
        Button view = button("Xem ngay");
        view.setOnClickListener(v -> showThemeDetails(themes.get(0)));
        banner.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        p.addView(banner);

        p.addView(sectionTitle("Danh mục nhanh"));
        p.addView(chipRow(new String[]{"HyperOS 3", "HyperOS 2", "MIUI", "Màn hình khóa", "Biểu tượng"}));

        p.addView(sectionTitle("Thống kê cộng đồng"));
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statCard("1.248", "Người dùng"), new LinearLayout.LayoutParams(0, dp(94), 1f));
        stats.addView(spaceH(8));
        stats.addView(statCard("326", "Theme"), new LinearLayout.LayoutParams(0, dp(94), 1f));
        stats.addView(spaceH(8));
        stats.addView(statCard("12.481", "Lượt tải"), new LinearLayout.LayoutParams(0, dp(94), 1f));
        p.addView(stats);

        p.addView(sectionTitle("Theme nổi bật"));
        for (int i = 0; i < 3; i++) p.addView(themeCard(themes.get(i)));

        p.addView(sectionTitle("Mới cập nhật"));
        for (int i = 3; i < themes.size(); i++) p.addView(themeCard(themes.get(i)));
    }

    private void showExplore() {
        currentTab = 1;
        ScrollView scroll = page("Khám phá", "Tìm kiếm, lọc và xem bảng xếp hạng theme");
        LinearLayout p = body(scroll);

        EditText query = new EditText(this);
        query.setHint("Tìm theme hoặc nhà sáng tạo");
        query.setHintTextColor(MUTED);
        query.setTextColor(TEXT);
        query.setSingleLine(true);
        query.setPadding(dp(16), 0, dp(16), 0);
        query.setBackground(rounded(CARD, 18));
        p.addView(query, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        p.addView(space(10));
        p.addView(chipRow(new String[]{"Tất cả", "HyperOS 3", "HyperOS 2", "MIUI 14", "Top tải", "4.8+ sao"}));

        Button searchBtn = button("Tìm kiếm");
        searchBtn.setOnClickListener(v -> {
            String q = query.getText().toString().trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
                Toast.makeText(this, "Nhập từ khóa cần tìm", Toast.LENGTH_SHORT).show();
                return;
            }
            int found = 0;
            for (ThemeItem item : themes) if ((item.name + item.creator + item.os).toLowerCase(Locale.ROOT).contains(q)) found++;
            Toast.makeText(this, "Tìm thấy " + found + " kết quả", Toast.LENGTH_SHORT).show();
        });
        p.addView(searchBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        p.addView(sectionTitle("Top tải nhiều"));
        for (ThemeItem item : themes) p.addView(themeCard(item));
    }

    private void showUpload() {
        currentTab = 2;
        ScrollView scroll = page("Đăng theme", "Gửi theme mới để đội ngũ M4X kiểm tra và xét duyệt");
        LinearLayout p = body(scroll);

        EditText name = input("Tên theme");
        EditText version = input("Phiên bản, ví dụ 1.0.0");
        EditText os = input("Hệ điều hành hỗ trợ: HyperOS/MIUI");
        EditText description = input("Mô tả chi tiết theme");
        description.setMinLines(4);
        description.setGravity(Gravity.TOP);
        p.addView(name); p.addView(space(10));
        p.addView(version); p.addView(space(10));
        p.addView(os); p.addView(space(10));
        p.addView(description); p.addView(space(12));

        LinearLayout fileCard = card();
        TextView file = text("File theme: " + selectedFile, 14, MUTED, Typeface.NORMAL);
        fileCard.addView(text("Chọn file .mtz hoặc .zip", 18, TEXT, Typeface.BOLD));
        fileCard.addView(file);
        Button choose = button("Chọn file");
        choose.setOnClickListener(v -> pickThemeFile());
        fileCard.addView(choose, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        p.addView(fileCard);

        p.addView(sectionTitle("Ảnh xem trước"));
        p.addView(text("Thêm tối thiểu 3 ảnh preview và 1 ảnh banner. Bản demo chưa tải ảnh lên máy chủ.", 14, MUTED, Typeface.NORMAL));
        p.addView(space(12));

        Button submit = button("Gửi theme để xét duyệt");
        submit.setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty() || selectedFile.equals("Chưa chọn file")) {
                Toast.makeText(this, "Bạn cần nhập tên và chọn file theme", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putInt("pending", prefs.getInt("pending", 5) + 1).apply();
            new AlertDialog.Builder(this)
                    .setTitle("Đã gửi theme")
                    .setMessage("Theme đã vào hàng chờ duyệt. Bạn sẽ nhận thông báo khi Admin duyệt hoặc từ chối.")
                    .setPositiveButton("Đã hiểu", null).show();
        });
        p.addView(submit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
    }

    private void showProfile() {
        currentTab = 3;
        ScrollView scroll = page("Tài khoản", "Quản lý hồ sơ, lượt tải, điểm thưởng và thông báo");
        LinearLayout p = body(scroll);

        LinearLayout profile = card();
        TextView avatar = text("M", 28, Color.WHITE, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(rounded(PURPLE, 30));
        profile.addView(avatar, new LinearLayout.LayoutParams(dp(60), dp(60)));
        profile.addView(text("Nguyễn Minh Dân\nNhà sáng tạo cấp 4 • 2.450 điểm", 17, TEXT, Typeface.BOLD));
        p.addView(profile);

        p.addView(sectionTitle("Hoạt động của tôi"));
        p.addView(menu("♡  Theme yêu thích", "4 theme đã lưu", v -> showFavorites()));
        p.addView(menu("⇩  Lịch sử tải", "12 lượt tải gần đây", v -> showDownloadHistory()));
        p.addView(menu("⬆  Theme đã đăng", prefs.getInt("pending", 5) + " theme đang chờ duyệt", v -> showMyThemes()));
        p.addView(menu("🔔  Thông báo", "3 thông báo chưa đọc", v -> showNotifications()));
        p.addView(menu("★  Điểm thưởng & nhiệm vụ", "Hoàn thành nhiệm vụ để nhận điểm", v -> showRewards()));

        p.addView(sectionTitle("Quản trị"));
        p.addView(menu("🛡  Bảng điều khiển Admin", "Duyệt theme, quản lý người dùng và báo cáo", v -> showAdmin()));

        p.addView(sectionTitle("Cài đặt"));
        p.addView(menu("⚙  Cài đặt ứng dụng", "Chế độ tối, ngôn ngữ, bộ nhớ", v -> simpleDialog("Cài đặt", "Chế độ tối: Bật\nNgôn ngữ: Tiếng Việt\nTự động kiểm tra cập nhật: Bật")));
        p.addView(menu("↻  Kiểm tra cập nhật OTA", "Phiên bản nội dung 1.0.0", v -> simpleDialog("OTA", "Bạn đang dùng phiên bản nội dung mới nhất.")));
        p.addView(menu("ⓘ  Giới thiệu M4X Theme", "Phiên bản ứng dụng 2.0.0", v -> simpleDialog("M4X Theme", "Kho theme Xiaomi dành cho cộng đồng HyperOS và MIUI.")));
    }

    private void showThemeDetails(ThemeItem item) {
        ScrollView scroll = page(item.name, item.creator + " • " + item.os);
        LinearLayout p = body(scroll);
        LinearLayout preview = cardGradient();
        TextView icon = text("MT", 42, Color.WHITE, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        preview.addView(icon, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
        p.addView(preview);

        LinearLayout meta = card();
        meta.addView(text(String.format(Locale.getDefault(), "★ %.1f    ⇩ %,d lượt tải    ◉ %,d lượt xem", item.rating, item.downloads, item.views), 15, TEXT, Typeface.BOLD));
        meta.addView(text("Danh mục: " + item.category + "\nPhiên bản: 1.4.2\nDung lượng: 38,6 MB\nCập nhật: Hôm nay", 14, MUTED, Typeface.NORMAL));
        p.addView(meta);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button fav = button(isFavorite(item) ? "♥ Đã thích" : "♡ Yêu thích");
        Button download = button("Tải theme");
        fav.setOnClickListener(v -> {
            toggleFavorite(item);
            fav.setText(isFavorite(item) ? "♥ Đã thích" : "♡ Yêu thích");
        });
        download.setOnClickListener(v -> {
            item.downloads++;
            prefs.edit().putInt("downloads", prefs.getInt("downloads", 12) + 1).apply();
            Toast.makeText(this, "Đang chuẩn bị tải " + item.name, Toast.LENGTH_SHORT).show();
        });
        actions.addView(fav, new LinearLayout.LayoutParams(0, dp(52), 1f));
        actions.addView(spaceH(8));
        actions.addView(download, new LinearLayout.LayoutParams(0, dp(52), 1f));
        p.addView(actions);

        p.addView(sectionTitle("Mô tả"));
        p.addView(text("Gói giao diện được tối ưu cho trải nghiệm mượt mà, có màn hình khóa, biểu tượng, hình nền, trung tâm điều khiển và nhiều thành phần được Việt hóa.", 15, MUTED, Typeface.NORMAL));
        p.addView(sectionTitle("Nhật ký cập nhật"));
        p.addView(text("• Sửa lỗi hiển thị trên HyperOS 3\n• Bổ sung lịch âm Việt Nam\n• Tối ưu hiệu ứng mở khóa\n• Cập nhật biểu tượng ứng dụng", 15, MUTED, Typeface.NORMAL));
        p.addView(sectionTitle("Đánh giá và bình luận"));
        p.addView(comment("Minh Phú", "5★", "Theme đẹp, chạy mượt trên HyperOS 2."));
        p.addView(comment("Hoàng Nam", "4★", "Mong tác giả bổ sung thêm kiểu đồng hồ."));
        Button rate = button("Viết đánh giá");
        rate.setOnClickListener(v -> ratingDialog(item));
        p.addView(rate, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        p.addView(space(10));
        Button report = outlineButton("Báo lỗi hoặc nội dung vi phạm");
        report.setOnClickListener(v -> simpleDialog("Đã gửi báo cáo", "Đội ngũ M4X sẽ kiểm tra báo cáo của bạn."));
        p.addView(report, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
    }

    private void showAdmin() {
        ScrollView scroll = page("Bảng quản trị", "Tổng quan hệ thống và nội dung đang chờ xử lý");
        LinearLayout p = body(scroll);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(statCard("1.248", "Người dùng"), new LinearLayout.LayoutParams(0, dp(94), 1f)); row.addView(spaceH(8));
        row.addView(statCard("326", "Theme"), new LinearLayout.LayoutParams(0, dp(94), 1f)); row.addView(spaceH(8));
        row.addView(statCard(String.valueOf(prefs.getInt("pending", 5)), "Chờ duyệt"), new LinearLayout.LayoutParams(0, dp(94), 1f));
        p.addView(row);
        p.addView(sectionTitle("Hiệu suất 30 ngày"));
        LinearLayout chart = card();
        chart.addView(text("Lượt xem   84.920  ↑ 18%\nLượt tải      12.481  ↑ 11%\nĐánh giá TB   4,8/5\nBáo cáo mới   7", 16, TEXT, Typeface.BOLD));
        p.addView(chart);
        p.addView(sectionTitle("Theme chờ duyệt"));
        p.addView(adminTheme("Hyper Glass 3", "Zan Theme", "38 MB"));
        p.addView(adminTheme("Colorful Lock VN", "M4X User", "24 MB"));
        p.addView(adminTheme("MIUI 14 Classic", "Phú Design", "41 MB"));
        p.addView(sectionTitle("Công cụ quản trị"));
        p.addView(menu("👥 Quản lý người dùng", "Khóa, mở khóa và phân quyền", v -> simpleDialog("Người dùng", "1.248 tài khoản • 3 tài khoản bị khóa")));
        p.addView(menu("⚠ Báo cáo vi phạm", "7 báo cáo cần xử lý", v -> simpleDialog("Báo cáo", "Đã mở danh sách nội dung bị báo cáo.")));
        p.addView(menu("🔔 Gửi thông báo", "Thông báo toàn hệ thống hoặc từng người", v -> notificationDialog()));
        p.addView(menu("✓ Kiểm tra file tự động", "Quét cấu trúc, dung lượng và mã nguy hiểm", v -> simpleDialog("Kiểm tra tự động", "Dịch vụ quét đang hoạt động bình thường.")));
    }

    private View adminTheme(String name, String creator, String size) {
        LinearLayout c = card();
        c.addView(text(name, 18, TEXT, Typeface.BOLD));
        c.addView(text(creator + " • " + size + " • Chờ 2 giờ", 13, MUTED, Typeface.NORMAL));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button reject = outlineButton("Từ chối");
        Button approve = button("Duyệt");
        reject.setOnClickListener(v -> simpleDialog("Đã từ chối", "Đã gửi lý do từ chối cho " + creator));
        approve.setOnClickListener(v -> {
            prefs.edit().putInt("pending", Math.max(0, prefs.getInt("pending", 5) - 1)).apply();
            Toast.makeText(this, "Đã duyệt " + name, Toast.LENGTH_SHORT).show();
            c.setVisibility(View.GONE);
        });
        row.addView(reject, new LinearLayout.LayoutParams(0, dp(48), 1f)); row.addView(spaceH(8));
        row.addView(approve, new LinearLayout.LayoutParams(0, dp(48), 1f));
        c.addView(row);
        return c;
    }

    private void showFavorites() {
        ScrollView scroll = page("Theme yêu thích", "Danh sách theme bạn đã lưu");
        LinearLayout p = body(scroll);
        boolean any = false;
        for (ThemeItem item : themes) if (isFavorite(item) || item.favorite) { p.addView(themeCard(item)); any = true; }
        if (!any) p.addView(text("Bạn chưa yêu thích theme nào.", 16, MUTED, Typeface.NORMAL));
    }

    private void showDownloadHistory() {
        ScrollView scroll = page("Lịch sử tải", "Các theme đã tải trên tài khoản này");
        LinearLayout p = body(scroll);
        for (int i = 0; i < 4; i++) p.addView(themeCard(themes.get(i)));
    }

    private void showMyThemes() {
        ScrollView scroll = page("Theme đã đăng", "Theo dõi trạng thái duyệt và cập nhật theme");
        LinearLayout p = body(scroll);
        p.addView(statusCard("M4X Dynamic Clock", "Đã duyệt", "8.420 lượt tải"));
        p.addView(statusCard("Hyper Center VN", "Chờ duyệt", "Đã gửi 2 giờ trước"));
        p.addView(statusCard("Classic MIUI Lock", "Cần chỉnh sửa", "Thiếu ảnh xem trước"));
    }

    private void showNotifications() {
        ScrollView scroll = page("Thông báo", "Cập nhật từ hệ thống và nhà sáng tạo");
        LinearLayout p = body(scroll);
        p.addView(notification("Theme đã được duyệt", "M4X Dynamic Clock đã xuất hiện trên cửa hàng.", "5 phút trước"));
        p.addView(notification("Có bản cập nhật mới", "Hyper Minimal OS đã cập nhật lên 1.4.2.", "1 giờ trước"));
        p.addView(notification("Nhiệm vụ hoàn thành", "Bạn nhận được 100 điểm thưởng.", "Hôm qua"));
    }

    private void showRewards() {
        ScrollView scroll = page("Điểm thưởng", "Hoàn thành nhiệm vụ và mở khóa quyền lợi");
        LinearLayout p = body(scroll);
        LinearLayout balance = cardGradient();
        balance.addView(text("SỐ DƯ HIỆN TẠI", 12, Color.WHITE, Typeface.BOLD));
        balance.addView(text("2.450 điểm", 32, Color.WHITE, Typeface.BOLD));
        balance.addView(text("Cấp 4 • Còn 550 điểm để lên cấp", 14, Color.WHITE, Typeface.NORMAL));
        p.addView(balance);
        p.addView(sectionTitle("Nhiệm vụ hôm nay"));
        p.addView(task("Đăng nhập hằng ngày", "+20 điểm", true));
        p.addView(task("Đánh giá một theme", "+30 điểm", false));
        p.addView(task("Tải 3 theme", "+50 điểm", false));
        p.addView(task("Mời một người bạn", "+200 điểm", false));
    }

    private View themeCard(ThemeItem item) {
        LinearLayout c = card();
        c.setClickable(true);
        c.addView(text(item.name, 19, TEXT, Typeface.BOLD));
        c.addView(text(item.creator + " • " + item.os, 13, MUTED, Typeface.NORMAL));
        c.addView(text(String.format(Locale.getDefault(), "★ %.1f   ⇩ %,d   ◉ %,d", item.rating, item.downloads, item.views), 14, TEXT, Typeface.BOLD));
        TextView category = text(item.category, 12, Color.WHITE, Typeface.BOLD);
        category.setPadding(dp(10), dp(6), dp(10), dp(6));
        category.setBackground(rounded(PURPLE, 20));
        c.addView(category, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        c.setOnClickListener(v -> showThemeDetails(item));
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(rounded(CARD, 22));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout cardGradient() {
        LinearLayout c = card();
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{PURPLE, BLUE});
        gd.setCornerRadius(dp(24));
        c.setBackground(gd);
        return c;
    }

    private View statCard(String value, String label) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER); c.setPadding(dp(8), dp(10), dp(8), dp(10)); c.setBackground(rounded(CARD, 20));
        c.addView(text(value, 19, TEXT, Typeface.BOLD)); c.addView(text(label, 11, MUTED, Typeface.NORMAL));
        return c;
    }

    private View chipRow(String[] chips) {
        HorizontalScrollView hsv = new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for (String chip : chips) {
            TextView t = text(chip, 13, TEXT, Typeface.BOLD); t.setPadding(dp(14), dp(9), dp(14), dp(9)); t.setBackground(rounded(CARD_2, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.rightMargin = dp(8); row.addView(t, lp);
            t.setOnClickListener(v -> Toast.makeText(this, "Đã chọn: " + chip, Toast.LENGTH_SHORT).show());
        }
        hsv.addView(row); return hsv;
    }

    private TextView sectionTitle(String value) {
        TextView t = text(value, 21, TEXT, Typeface.BOLD);
        t.setPadding(0, dp(20), 0, dp(12)); return t;
    }

    private View menu(String title, String subtitle, View.OnClickListener listener) {
        LinearLayout c = card(); c.addView(text(title, 17, TEXT, Typeface.BOLD)); c.addView(text(subtitle, 13, MUTED, Typeface.NORMAL)); c.setOnClickListener(listener); return c;
    }

    private View comment(String user, String stars, String message) {
        LinearLayout c = card(); c.addView(text(user + "   " + stars, 15, TEXT, Typeface.BOLD)); c.addView(text(message, 14, MUTED, Typeface.NORMAL)); return c;
    }

    private View statusCard(String name, String status, String detail) {
        LinearLayout c = card(); c.addView(text(name, 18, TEXT, Typeface.BOLD)); c.addView(text(status + " • " + detail, 14, status.equals("Đã duyệt") ? Color.rgb(79, 215, 137) : MUTED, Typeface.BOLD)); return c;
    }

    private View notification(String title, String bodyText, String time) {
        LinearLayout c = card(); c.addView(text(title, 17, TEXT, Typeface.BOLD)); c.addView(text(bodyText, 14, MUTED, Typeface.NORMAL)); c.addView(text(time, 12, MUTED, Typeface.NORMAL)); return c;
    }

    private View task(String title, String reward, boolean done) {
        LinearLayout c = card(); c.addView(text((done ? "✓ " : "○ ") + title, 17, TEXT, Typeface.BOLD)); c.addView(text(reward + (done ? " • Đã nhận" : ""), 13, done ? Color.rgb(79, 215, 137) : MUTED, Typeface.BOLD)); return c;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setHintTextColor(MUTED); e.setTextColor(TEXT); e.setTextSize(15); e.setPadding(dp(16), dp(13), dp(16), dp(13)); e.setBackground(rounded(CARD, 18)); e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); return e;
    }

    private Button button(String value) {
        Button b = new Button(this); b.setText(value); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); b.setBackground(gradientButton()); return b;
    }

    private Button outlineButton(String value) {
        Button b = new Button(this); b.setText(value); b.setTextColor(TEXT); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false);
        GradientDrawable gd = rounded(Color.TRANSPARENT, 18); gd.setStroke(dp(1), Color.rgb(87, 104, 160)); b.setBackground(gd); return b;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(sp); t.setTextColor(color); t.setTypeface(Typeface.DEFAULT, style); t.setLineSpacing(0, 1.15f); return t;
    }

    private Space space(int h) { Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); return s; }
    private Space spaceH(int w) { Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1)); return s; }

    private GradientDrawable rounded(int color, int radius) { GradientDrawable gd = new GradientDrawable(); gd.setColor(color); gd.setCornerRadius(dp(radius)); return gd; }
    private GradientDrawable gradientButton() { GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{PURPLE, BLUE}); gd.setCornerRadius(dp(18)); return gd; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void pickThemeFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*"); intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/octet-stream"}); startActivityForResult(intent, PICK_THEME_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_THEME_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData(); selectedFile = uri.getLastPathSegment() == null ? "theme.mtz" : uri.getLastPathSegment(); Toast.makeText(this, "Đã chọn: " + selectedFile, Toast.LENGTH_LONG).show(); showUpload();
        }
    }

    private boolean isFavorite(ThemeItem item) { return prefs.getBoolean("fav_" + item.name, item.favorite); }
    private void toggleFavorite(ThemeItem item) { prefs.edit().putBoolean("fav_" + item.name, !isFavorite(item)).apply(); }

    private void ratingDialog(ThemeItem item) {
        final EditText comment = input("Nhập bình luận của bạn");
        new AlertDialog.Builder(this).setTitle("Đánh giá " + item.name).setSingleChoiceItems(new String[]{"5 sao", "4 sao", "3 sao", "2 sao", "1 sao"}, 0, null).setView(comment).setPositiveButton("Gửi", (d, w) -> Toast.makeText(this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show()).setNegativeButton("Hủy", null).show();
    }

    private void notificationDialog() {
        EditText message = input("Nội dung thông báo");
        new AlertDialog.Builder(this).setTitle("Gửi thông báo").setView(message).setPositiveButton("Gửi", (d, w) -> Toast.makeText(this, "Đã gửi thông báo", Toast.LENGTH_SHORT).show()).setNegativeButton("Hủy", null).show();
    }

    private void simpleDialog(String title, String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Đóng", null).show(); }

    @Override
    public void onBackPressed() {
        if (currentTab != 0) showHome(); else super.onBackPressed();
    }

    static class ThemeItem {
        String name, creator, os, category; double rating; int downloads, views; boolean favorite;
        ThemeItem(String name, String creator, String os, String category, double rating, int downloads, int views, boolean favorite) { this.name = name; this.creator = creator; this.os = os; this.category = category; this.rating = rating; this.downloads = downloads; this.views = views; this.favorite = favorite; }
    }
}
