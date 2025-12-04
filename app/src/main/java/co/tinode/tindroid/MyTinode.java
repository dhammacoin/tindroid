package co.tinode.tindroid;

import co.tinode.tinodesdk.Storage;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.model.ServerMessage;

import androidx.annotation.Nullable;

public class MyTinode extends Tinode {

    public MyTinode(String ua, String apiKey, Storage storage, @Nullable EventListener listener) {
        // Мы используем EventListener в качестве типа слушателя, как и должен базовый класс Tinode
        super(ua, apiKey, storage, listener);
    }

    // Делаем публичный метод для login("none")
    public PromisedReply<ServerMessage> loginAnon() {
        return super.login("none", null, null);
    }

    // Делаем публичный метод для входа по токену
    public PromisedReply<ServerMessage> loginWithToken(String token) {
        return super.login("token", token, null);
    }

    /**
     * Делает публичный метод для account (анонимная регистрация).
     *
     * Настройки изменены, чтобы отправить {"user":"new"} и исключить {"login":true}
     * для соответствия успешному ручному вводу.
     */
    public <Pu, Pr> PromisedReply<ServerMessage> accountAnon() {
        return super.account(
                "new",          // login: Установлено "new" для генерации {"user":"new"} в пакете acc
                null,           // password
                null,           // credentials
                "anonymous",    // scheme
                null,           // secret
                false,          // background: Установлено false, чтобы исключить {"login":true}
                null,           // tags
                null,           // public
                null            // private
        );
    }
}