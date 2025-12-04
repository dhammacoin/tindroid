//package co.tinode.tindroid;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.text.TextUtils;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//
//import co.tinode.tinodesdk.PromisedReply;
//import co.tinode.tinodesdk.Tinode;
//
//
///**
// * Обработчик результатов входа (login) и создания аккаунта (account).
// * Этот класс отвечает за сохранение токена и перенаправление пользователя на главный экран.
// */
//public class TinoLoginHandler implements LoginHandler {
//    private static final String TAG = "TinoLoginHandler";
//
//    private final Context mContext;
//    private final Target mTarget;
//    private final boolean mAutoLogin;
//
//    /**
//     * Куда перенаправлять после успешного входа.
//     */
//    public enum Target {
//        // Перенаправить на ChatsActivity, инициировать запрос разрешений.
//        PERMISSIONS,
//        // Остаться на текущей активности (для LoginActivity).
//        CURRENT
//    }
//
//    /**
//     * Создает обработчик для входа/аккаунта.
//     *
//     * @param context Контекст приложения или активности.
//     * @param target  Куда переходить после успеха.
//     * @param autoLogin True, если это автоматический вход по токену или анонимный вход.
//     */
//    public TinoLoginHandler(Context context, Target target, boolean autoLogin) {
//        mContext = context;
//        mTarget = target;
//        mAutoLogin = autoLogin;
//    }
//
//    /**
//     * Обработка успешного входа или создания аккаунта.
//     *
//     * @param token Токен аутентификации, полученный от сервера.
//     */
//    @Override
//    public void onSuccess(String token) {
//        final Tinode tinode = Cache.getTinode();
//
//        // ⬅️ КЛЮЧЕВОЙ ШАГ: Сохранение нового токена, полученного от сервера
//        if (!TextUtils.isEmpty(token)) {
//            SharedPrefs.saveToken(mContext, token);
//        }
//
//        // Если это не анонимный вход, инициируем запрос разрешений (если нужно).
//        if (mTarget == Target.PERMISSIONS) {
//            final Activity activity = (Activity) mContext;
//
//            // Если это LoginActivity, мы его завершаем.
//            if (activity instanceof LoginActivity) {
//                activity.finish();
//            }
//
//            // Переход на главный экран (ChatsActivity)
//            Intent intent = new Intent(mContext, ChatsActivity.class);
//            // Флаги для очистки стека активностей, если вход был ручным
//            if (!mAutoLogin) {
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            }
//            mContext.startActivity(intent);
//
//            // Подключаемся к топикам Me и Fnd
//            PromisedReply<ServerResponse> mePromise = Cache.attachMeTopic(null);
//            PromisedReply<ServerResponse> fndPromise = Cache.attachFndTopic(null);
//
//            // Ожидаем, пока оба топика прикрепятся
//            PromisedReply.all(mePromise, fndPromise).thenApply(new PromisedReply.SuccessListener<ServerResponse[]>() {
//                @Override
//                public ServerResponse[] onSuccess(ServerResponse[] result) {
//                    UiUtils.doPermissionChecks(mContext, activity, false, false);
//                    return result;
//                }
//            });
//        }
//    }
//
//    /**
//     * Обработка ошибки входа или создания аккаунта.
//     *
//     * @param err Ошибка.
//     */
//    @Override
//    public void onFailure(Exception err) {
//        // ⬅️ КЛЮЧЕВОЙ ШАГ: Удаление устаревшего токена при ошибке 401
//        if (err instanceof ServerResponseException) {
//            ServerResponseException sre = (ServerResponseException) err;
//            String scheme = sre.getScheme();
//
//            // Если это ошибка 401 Unauthorized И это был вход по токену
//            if (sre.getCode() == 401 && "token".equals(scheme)) {
//                // Удаляем невалидный токен, чтобы при следующем запуске
//                // Cache.reconnect() попытался создать анонимный аккаунт.
//                SharedPrefs.deleteToken(mContext);
//            }
//        }
//
//        // Если это ручной вход, показываем ошибку пользователю.
//        if (!mAutoLogin) {
//            Toast.makeText(mContext, "Login failed: " + err.getLocalizedMessage(),
//                    Toast.LENGTH_LONG).show();
//        } else {
//            // Если это автоматический вход (токен или аноним), и он не удался,
//            // перенаправляем на LoginActivity для ручного входа
//            if (mTarget == Target.PERMISSIONS) {
//                Intent intent = new Intent(mContext, LoginActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                mContext.startActivity(intent);
//            }
//        }
//    }
//}