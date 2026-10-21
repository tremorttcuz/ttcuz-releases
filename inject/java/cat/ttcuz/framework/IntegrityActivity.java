package cat.ttcuz.framework;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** A local blocking screen over the original task; success returns without relaunching it. */
public final class IntegrityActivity extends Activity {

    private TextView text;
    private View spinner;
    private Button retry;
    private boolean continued;

    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Skin skin = Skin.tonal(this, Accent.colour());
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(24), dp(24), dp(24), dp(24));
        page.setBackgroundColor(skin.page);
        TextView title = new TextView(this);
        title.setText("ttcuz");
        title.setTextSize(24);
        title.setTextColor(skin.text);
        title.setGravity(Gravity.CENTER);
        page.addView(title);
        spinner = new View(this) {
            protected void onDraw(android.graphics.Canvas canvas) {
                LoadingIndicator.draw(this, canvas);
            }
        };
        LinearLayout.LayoutParams icon = new LinearLayout.LayoutParams(dp(48), dp(48));
        icon.topMargin = dp(24);
        icon.bottomMargin = dp(24);
        page.addView(spinner, icon);
        text = new TextView(this);
        text.setTextSize(16);
        text.setTextColor(skin.text);
        text.setGravity(Gravity.CENTER);
        page.addView(text, new LinearLayout.LayoutParams(-1, -2));
        retry = new Button(this);
        retry.setText("Повторить проверку");
        retry.setOnClickListener(v -> AppIntegrity.retry());
        retry.setVisibility(View.GONE);
        page.addView(retry, new LinearLayout.LayoutParams(-2, dp(48)));
        Button close = new Button(this);
        close.setText("Закрыть");
        close.setOnClickListener(v -> finishAndRemoveTask());
        LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(-2, dp(48));
        button.topMargin = dp(24);
        page.addView(close, button);
        setContentView(page);
        AppIntegrity.shown(this);
    }

    void result(int state) {
        if (isFinishing() || continued) return;
        if (state == 1) {
            continued = true;
            finish();
            return;
        }
        spinner.setVisibility(state == 0 ? View.VISIBLE : View.GONE);
        retry.setVisibility(state == 3 ? View.VISIBLE : View.GONE);
        text.setText(
            state == 0
                ? "Проверка сборки…"
                : state == 2
                  ? "Мы просили вас не делать форк этого мода. Пожалуйста, удалите текущую версию."
                  : "Не удалось проверить сборку. Закройте приложение и попробуйте снова. Если ошибка повторится, переустановите официальную сборку ttcuz."
        );
    }

    protected void onDestroy() {
        AppIntegrity.gone(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (continued) super.onBackPressed();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
