package co.tinode.tindroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import co.tinode.tinodesdk.PromisedReply; // ⬅️ Добавлен импорт для PromisedReply, если он отсутствует
import co.tinode.tinodesdk.model.ServerMessage; // ⬅️ Добавлен импорт, если он отсутствует

/**
 * Fragment для анонимного входа в приложение.
 */
public class LoginFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "LoginFragment";

    // URL вашего сервера
    private static final String SERVER_URL = "144.31.121.233:6060";

    // ID кнопки, предполагая, что вы используете anonLogin
    private static final int ID_ANON_LOGIN = R.id.anonLogin;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        AppCompatActivity activity = (AppCompatActivity) requireActivity();

        // Настройка ActionBar
        final ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setTitle(R.string.app_name);
        }

        // Inflate layout и назначаем обработчик на кнопку анонимного входа
        View fragment = inflater.inflate(R.layout.fragment_login, container, false);
        Button anonButton = fragment.findViewById(ID_ANON_LOGIN);
        anonButton.setOnClickListener(this);

        return fragment;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == ID_ANON_LOGIN) {
            doAnonymousLogin();
        }
    }

    private void doAnonymousLogin() {
        final LoginActivity parent = (LoginActivity) requireActivity();
        final Button btnAnon = parent.findViewById(R.id.anonLogin);
        btnAnon.setEnabled(false);

        final MyTinode tinode = (MyTinode) Cache.getTinode(); // Приводим к MyTinode

        // Выполняем подключение и логин в отдельном потоке
        new Thread(() -> {
            try {
                // 1. Подключаемся к серверу
                tinode.connect(SERVER_URL, false, false).getResult();

                try {
                    // 2. Попытка анонимного логина (восстановление сессии)
                    tinode.loginAnon().getResult();
                    Log.d(TAG, "Anonymous login successful (restored)");
                } catch (Exception e) {
                    // Если вход не удался (например, 401 "unknown scheme"),
                    // 3. Пытаемся зарегистрировать новый анонимный аккаунт.
                    Log.i(TAG, "Anon login failed, attempting registration: " + e.getMessage());
                    tinode.accountAnon().getResult();
                    Log.d(TAG, "Anonymous account registered and logged in.");
                }

                // 4. Успех: Выполняем переход на UI-потоке
                parent.runOnUiThread(() -> {
                    UiUtils.onLoginSuccess(parent, btnAnon, tinode.getMyId());
                });

            } catch (Exception e) {
                // 5. Ошибка: Любая другая ошибка (подключение или регистрация)
                parent.runOnUiThread(() -> {
                    Log.e(TAG, "Anonymous login failed completely", e);
                    btnAnon.setEnabled(true);
                    parent.reportError(e, btnAnon, 0, R.string.error_login_failed);
                });
            }
        }).start();
    }
}